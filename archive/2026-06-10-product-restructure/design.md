# v0.0.13-product-restructure — 技术设计

## Context

**Baseline**: tag `v0.0.12-product` / commit `fdb82e0` (含 A1 + A2 patch)。

**当前架构（v0.0.12）**:
```
ProductCategory → Product → ProductModule → Feature
   (id, code,     (id, code,    (id, code,       (id, code,
    name, status, name, status, name, status,    name, status,
    ownerUserId)  categoryId,   productId,       moduleId,
                  ownerUserId)  ownerUserId)     ownerUserId)
```
- 4 个 backend 包：`com.rainier.{productcategory, product, productmodule, feature}`，每个含 entity/status/repo/3 DTO/service/controller (8 src/包) + 测试 (5 test/包)。
- 4 张表：`rainier_product_category` (2-state), `rainier_product` (4-state), `rainier_product_module` (3-state), `rainier_feature` (3-state)。
- 4 个前端 page + drawer + index = 12 文件 + 4 个 api/*.ts。
- Sider 顶级 4 组中第 2 组为「产品」，含 4 项子菜单。
- 共 263+2(A2) = 265 backend tests / 49 frontend tests / E2E green。

**约束**:
- v0.0.12 才交付 1 天，无生产数据。
- BaseEntity 模式：Long id auto-increment + 4 audit + del_flag（soft delete via @SQLDelete + @Where）。
- Java 8 兼容（`Set.of` 不能用，须 `Collections.unmodifiableSet(new HashSet<>(Arrays.asList(...)))`）。
- v0.0.10.1 起 list 端点强制 batch enrich（≤ 5 SQL/page）。
- Hibernate ddl-auto=update（dev/test 自动创建表；生产部署需手动迁移）。

**v0.0.13 目标**:
- 删 ProductCategory 整层 + Product.categoryId 字段。
- ProductModule 加 parent_id 自引用 + 深度配置 + (parent_id, code) 复合唯一 + reparent 可变 + 环检测。

---

## Decisions

### 1. 数据迁移：用 `LegacyProductCategoryCleanupTest` 在 `@PostConstruct` 执行 DROP

**方案**：新建 bootstrap 组件 `com.rainier.product.bootstrap.LegacyProductCategoryCleanup`（`CommandLineRunner`，跟随 domain 包 — 与 `com.rainier.sprint.bootstrap` 家族模式一致，见 PA-1），在应用启动时执行：
```sql
DROP TABLE rainier_product_category;            -- INFORMATION_SCHEMA 探测后执行
ALTER TABLE rainier_product DROP COLUMN category_id;  -- 同上
```
配套 `LegacyProductCategoryCleanupTest` 断言 `SHOW TABLES` 不含目标表、`DESCRIBE rainier_product` 不含 `category_id` 列。

**为什么**:
- 与 v0.0.10.1 的 `LegacyStoryToSprintMigration` + `LegacyRequirementIdColumnCleanup` 同模式（已验证有效）。
- 启动期执行避开测试 race（Hibernate ddl-auto=update 重建表前先清理遗留）。
- 测试断言 schema 验证 idempotent。

**备选**:
- Flyway 正式迁移：项目尚未引入 Flyway，引入需更大基建工作，超本变更范围。
- 仅靠 ddl-auto：Hibernate 不会自动 drop 不再 mapped 的表/列，必须显式 DROP。

### 2. ProductModule `parent_id` 列设计

**方案**：在 `ProductModule` 实体加 `private Long parentId`（nullable），DB 列 `parent_id BIGINT NULL`，加索引 `idx_product_module_parent_id`，加复合唯一 `uk_product_module_parent_code(parent_id, code)`。NULL parentId 视作「顶层 scope」。

**为什么**:
- MySQL 在唯一约束中允许多个 NULL 行，与「同一 productId 内多个顶层 module」语义不冲突——但需在 Service 加额外检查：当 parentId=null 且 productId=P 时，code 在 P 内顶层唯一（应用层校验）。
- 复合唯一允许跨产品/跨子树的 code 重名（提案 Q3 决策）。
- 单列索引加速 `countByParentId` 删除前置检查 + 树组装 `findByProductIdOrderByParentIdAsc` 列表查询。

**备选**:
- 全局 service-level code unique（v0.0.12 原状）：与 Q3 决策相悖。
- 路径式 ltree/jsonb：MySQL 不原生支持，需引入 mariadb extension 或自维护字符串路径。

### 3. NULL parentId 顶层语义 + 应用层顶层 code 唯一

**方案**：因 MySQL UNIQUE 跳过 NULL 行，应用层 Service 在创建/更新前检查：
```java
if (parentId == null) {
    // 顶层 scope: 同 productId 内 (parentId IS NULL, code) 不重复
    if (repo.existsByProductIdAndParentIdIsNullAndCode(productId, code))
        throw new ConflictException("code already exists in top-level: " + code);
} else {
    // 子 scope: (parentId, code) 由 DB UQ 兜底，但 Service 仍做友好检查
    if (repo.existsByParentIdAndCode(parentId, code))
        throw new ConflictException("code already exists under parent: " + code);
}
```

**为什么**:
- 应用层校验给出友好错误信息（无 raw SQL exception 暴露）。
- DB UQ 作为兜底防并发竞态。

**备选**:
- 「顶层用 parentId=0 占位」: 0 是合法 FK，触发 FK 检查失败；放弃。
- 「不约束顶层 code」: 同 product 顶层可同名 → 路径无法唯一识别 → UI 崩溃。

### 4. 深度上限算法 — walk-parent-chain

**方案**：Service 注入 `@Value("${rainier.product-module.depth.max:3}")` int maxDepth。创建/更新时：
```java
int computeDepth(Long parentId) {
    int depth = 1;  // 自身算 1 层
    Long cur = parentId;
    while (cur != null) {
        depth++;
        if (depth > maxDepth) throw new BadRequestException(
            "max module depth exceeded: " + depth + " > " + maxDepth);
        cur = repo.findParentIdById(cur);  // 1 SELECT/层
    }
    return depth;
}
```
最坏 `maxDepth` 次 SELECT（默认 3 次），不会发散。

**为什么**:
- 简单 / 易测 / 性能可控。
- 配置化允许调整（如未来想放宽到 5 层）。

**备选**:
- 在 DB 用递归 CTE：MySQL 8+ 支持，但更脆弱且测试 setup 麻烦。
- 缓存 depth 列：写时算好存表 → 需 reparent 时维护整棵子树 depth 更新，复杂度高。

### 5. 跨产品父级拒绝

**方案**：Service 在 create/update 时，如果 parentId 非 null：
```java
ProductModule parent = repo.findById(parentId).orElseThrow(...);
if (!parent.getProductId().equals(this.productId))
    throw new BadRequestException("parent module must belong to the same product");
```

**为什么**:
- 产品边界是硬约束：Module 不能挂到别的产品的 Module 下。
- 单次 SELECT 即可决断，性能友好。

**备选**:
- DB 约束：MySQL FK 无法表达 productId 一致性，需 trigger，可读性差。

### 6. 环检测 — DFS 子树扫描

**方案**：update parentId 时，从当前 module 出发 DFS 收集所有子孙 ID，若新 parentId 在该集合中（含自己）→ 抛 400 cycle。代码：
```java
Set<Long> descendants = new HashSet<>();
collectDescendants(id, descendants);  // BFS/DFS via repo.findByParentId
if (descendants.contains(newParentId) || newParentId.equals(id))
    throw new BadRequestException("cycle detected: would create ancestor loop");
```
最坏 O(树大小) SELECT，但子树通常 <100 节点，可接受。

**为什么**:
- 环只有在 reparent 时才可能产生；create 时永远不会（新 parent 不可能在尚未存在的新节点的子树中）。
- DFS 比每次单步链 walk 更直观。

**备选**:
- 「向上 walk 新 parent 的祖先链」：检查祖先链是否含自己。算法也对，节点数相近。两者皆可，选 DFS 是因为后续若需 cascade 操作（如「移走整棵子树」）可复用。

### 7. Reparent 验证顺序：cross-product → cycle → depth

**方案**：Update Service 校验顺序固定为：
1. 解析新 parentId（null OK / 找不到抛 400）。
2. 检查 cross-product（同 productId）。
3. 检查 cycle（DFS 子树）。
4. 检查 depth（从新 parent 起向上 walk + 加该节点子树最大深度）。

**为什么**:
- 失败时给最具体的错误信息（cross-product > cycle > depth 的语义紧度）。
- 早期 short-circuit 避免无用计算。

**备选**:
- 任意顺序：错误信息可能误导用户。

### 8. productId 仍然创建后不可改

**方案**：UpdateRequest DTO 不含 `productId` 字段（Jackson 静默丢弃）。Service Update 方法不 set productId。

**为什么**:
- 继承 v0.0.12 Decision 11。
- 允许 reparent 但禁止跨产品 == 「productId 锁定 / parentId 可变（在产品内）」。

**备选**:
- 开放 productId：与新增 cross-product 防御冲突；不一致。

### 9. 状态机不受层级影响

**方案**：ProductModule 仍 3 态 (PLANNING/ACTIVE/DEPRECATED)。子 module 状态独立于父 module 状态——父 DEPRECATED 时子仍可 ACTIVE（业务可能反对，但本版不强制）。

**为什么**:
- 状态联动逻辑复杂，且需求场景不清晰（例如「父 ACTIVE 子 PLANNING」可能合法 = 父已在用，子刚规划）。
- 推到 v0.0.14+ 决策。

**备选**:
- 父 DEPRECATED 时子必须 DEPRECATED：业务过于刚性，砍掉。

### 10. 删除链：双向对称 — countFeatures || countSubModules → 409

**方案**：Service `delete(id)` 改为：
```java
if (featureRepo.countByModuleId(id) > 0)
    throw new ConflictException("module has linked features");
if (repo.countByParentId(id) > 0)
    throw new ConflictException("module has linked sub-modules");
repo.delete(module);
```
两条 check 顺序固定（先 Feature，因 v0.0.12 已有；后子 module）。

**为什么**:
- 对称语义：删 Product 检查 Module；删 Module 检查 Feature + 子 Module。
- 错误信息明确区分两种阻塞原因。

**备选**:
- 合并消息「module has dependents」: 用户无法定位哪个表挡道。

### 11. Tree assembly 策略 — 客户端组装（嵌套 UL 完全替换 Table，见 PA-5）

**方案**：后端仍返回 flat list（`GET /api/product-modules?productId=X`）。前端 `ProductModulesPage` 拉取后用 JS 算法组装 tree：
```ts
function buildTree(flat: ProductModule[]): TreeNode[] {
  const map = new Map<number, TreeNode>();
  flat.forEach(m => map.set(m.id, { ...m, children: [] }));
  const roots: TreeNode[] = [];
  flat.forEach(m => {
    if (m.parentId == null) roots.push(map.get(m.id)!);
    else map.get(m.parentId)?.children.push(map.get(m.id)!);
  });
  return roots;
}
```
渲染：递归 UL，depth 通过 padding-left。

**为什么**:
- 简单、O(n)，无后端 schema 变更。
- 对前端 search / status filter 透明（filter 后重组 tree）。

**备选**:
- 后端递归 SQL 返回 nested JSON：测试复杂、序列化 cost 高。

### 12. Module path display "A / B / C" — 后端 enrich

**方案**：`ProductModuleDetail` 加 `pathName: String`（如 "支付平台 / 钱包 / 余额"）和 `pathCodes: String`（如 "PROD-PAY / MOD-WALLET / MOD-BALANCE"）。Service enrich 时 walk parent chain 拼接。

**为什么**:
- 前端 EditDrawer / Feature 下拉直接渲染，无需再做 parent map lookup。
- 与既有 `productCode/productName` enrich 风格一致。

**备选**:
- 前端组装 path：需要 sticky 整棵树在前端缓存，drawer 切换 product 时重拉，复杂度高。

### 13. 后端包清理顺序

**方案**：清理 `com.rainier.productcategory` 按依赖反向：
1. 先改 `ProductService`（去 categoryRepo 依赖、enrich Category 部分、list categoryId 参数）→ 编译通过。
2. 改 `ProductCategoryService` 等不再被引用。
3. 最后 `git rm -r backend/src/main/java/com/rainier/productcategory backend/src/test/java/com/rainier/productcategory`。
4. 同步删 `frontend/src/api/productCategory.ts` + `frontend/src/pages/ProductCategory/`。
5. 改 `AppRoutes.tsx` 删 import 与路由项；改 `AppLayout.tsx` 删 nav item；改 `AppLayout.test.tsx` 改断言。

**为什么**：保证每一步编译通过，避免「跨步死锁」。

### 14. AppLayout 测试更新与「3 项产品组」断言

**方案**：现有 `AppLayout.test.tsx` 的 TC-FES-PROD-001 断言「产品组 4 项」，改为「产品组 3 项」，删除 `产品分类` 文本断言。所有其他 9 个 navGroups 测试断言保持不变。

**为什么**：单点修改避免连带影响其他 nav 组测试。

---

## Architecture

### 后端模块视图

```
┌─────────────────────────────────────────────────────────┐
│ com.rainier.product.bootstrap (NEW subpkg, v0.0.13)     │
│   └ LegacyProductCategoryCleanup (CommandLineRunner)    │
│   └ LegacyProductCategoryCleanupTest (test side)        │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ com.rainier.product (MODIFIED)                          │
│   ├ Product (DROP categoryId)                           │
│   ├ ProductRepository (DROP existsByCategoryId etc.)    │
│   ├ Product*Request (DROP categoryId fields)            │
│   ├ ProductDetail (DROP category enrich fields)         │
│   ├ ProductService (DROP categoryRepo dep + enrich)     │
│   └ ProductController (DROP categoryId query param)     │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ com.rainier.productcategory (DELETED)                   │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ com.rainier.productmodule (MODIFIED)                    │
│   ├ ProductModule (ADD parentId Long?)                  │
│   ├ ProductModuleRepository                             │
│   │   + countByParentId(Long)                           │
│   │   + findParentIdById(Long)                          │
│   │   + existsByProductIdAndParentIdIsNullAndCode       │
│   │   + existsByParentIdAndCode                         │
│   ├ ProductModuleCreateRequest (ADD parentId? optional) │
│   ├ ProductModuleUpdateRequest (ADD parentId? optional) │
│   ├ ProductModuleDetail (ADD parentId, parentCode,      │
│   │                       parentName, pathName, pathCodes)│
│   ├ ProductModuleService                                │
│   │   • walkParentChainDepth                            │
│   │   • crossProductReject                              │
│   │   • cycleDfsCheck                                   │
│   │   • parentEnrich + pathChainBuild                   │
│   │   • delete: feature OR child-module → 409           │
│   └ ProductModuleController (parentId query param)      │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ com.rainier.feature (UNCHANGED — wired via moduleId)    │
└─────────────────────────────────────────────────────────┘
```

### 前端组件视图

```
AppLayout.navGroups
  └ product (CHANGED 4 → 3 items)
      • products
      • product-modules   (NEW: tree view + parent select)
      • features          (UNCHANGED, drawer module path display)

AppRoutes (CHANGED 4 → 3 routes — drop /pm/product-categories)

pages/Product/
  ├ ProductsPage (DROP Category column + Category filter)
  └ ProductEditDrawer (DROP Category select)

pages/ProductModule/
  ├ ProductModulesPage (REWRITE: tree assembly + UL indent)
  ├ ProductModuleEditDrawer (REWRITE cascade: Product → parentModule)
  └ ProductModuleTreeView (NEW component: recursive UL render)

pages/Feature/
  └ FeatureEditDrawer (MINOR: module option label uses pathName)

pages/ProductCategory/ (DELETED)
api/productCategory.ts (DELETED)
```

### 数据流：创建子 Module

```
Frontend EditDrawer
  └ user picks Product P + parentModule M (depth=1)
       ↓
POST /api/product-modules {code, name, productId=P.id, parentId=M.id}
       ↓
ProductModuleService.create
  1. validate productId existence
  2. validate parentId (if not null):
       a. fetch parent → exists ?
       b. parent.productId == req.productId ? (cross-product check)
       c. computeDepth(parent) + 1 ≤ maxDepth ?
  3. code uniqueness check (composite: (parentId, code))
  4. ownerUserId / status validation (unchanged)
  5. save → enrich → return
       ↓
Response: ProductModuleDetail with pathName "P.name / M.name / new.name"
```

### 数据流：reparent

```
PUT /api/product-modules/{id}  {code, name, ..., parentId=newP}
  1. fetch existing module → existingProductId
  2. if newParentId != null:
       a. fetch newParent → exists ?
       b. newParent.productId == existingProductId ? (cross-product)
       c. cycle check: collectDescendants(id) ∋ newParentId ?
       d. computeDepth(newParent) + maxChildDepth(id) ≤ maxDepth ?
  3. code uniqueness (if parentId or code changed)
  4. set parentId = newParentId
  5. save → enrich → return
```

---

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|----------|
| LegacyCleanup DROP TABLE/COLUMN 不可逆 | 在 README 醒目位置标注；v0.0.12 -> v0.0.13 跳跃属破坏式版本；无生产数据。 |
| Composite UQ + 应用层顶层 code 唯一双层校验易脱节 | 写并发测试 TC-PMOD-RACE 模拟两线程同时创建顶层同 code，断言一方 409。 |
| Depth maxDepth 配置如被改大（如 10），现有 walk 算法仍 O(maxDepth) 不爆炸 | 算法本身上限受控；改配置时 LegacyCleanup 启动检查现有最大 depth 不超新 max（v0.0.13 不做、留 v0.0.14）。 |
| Reparent 算法 3 道校验顺序若开发者改动可能误导错误信息 | design.md Decision 7 明示顺序；single test class `ProductModuleControllerReparentTest` 单独覆盖。 |
| Tree assembly 客户端 — 大量 module（>500）渲染卡顿 | v0.0.13 限制 size=100 默认；若 product 内 >100 module 触发服务器分页 list（已有）。 |
| Path 字段后端拼接 N 层时 N 次 SELECT 拖慢 list 端点 | enrich 改 batch：先收集所有需要的 parentId, 一次 findAllByIdInBatch → Map 解析 + 在 Service 端递归求 path；性能测试覆盖 ≤ 5 SQL/page。 |
| 删 ProductCategory 后 Product 列表 React 旧缓存可能出现"category 字段 undefined" | 在 v0.0.13 release 后强制刷新前端 build（vite 自动 hash），无 localStorage 残留风险（auth.ts 不存 product 数据）。 |
| AppLayout.test.tsx 改 4 → 3 项后可能漏改其他 nav 计数断言 | grep 全文件搜「4 个顶级菜单组」「4 项」校对。 |
| Feature 下拉 pathName 太长 (e.g. "A / B / C / D / E" 超 60 字) | UI 用 CSS `text-overflow: ellipsis` + `title=` 完整 path 鼠标 hover；TC 覆盖。 |
