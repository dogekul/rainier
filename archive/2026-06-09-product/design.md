# v0.0.12-product — Technical Design

> Baseline: commit `31c9721` / tag `v0.0.11-task` (2026-06-09).
> 4 new entities: ProductCategory → Product → ProductModule → Feature.
> Family pattern reuse from Project/Requirement/Sprint/Story/Task (v0.0.8-v0.0.11).

## Context

Rainier 已建立成熟的"项目视角"金字塔（Project → Requirement → Sprint → Story → Task）+ Demand 与 Organization/User/Role 的基础设施。Family pattern 经过 7 个版本迭代已经稳定：

- `BaseEntity` + auto-increment `Long` id + 6 审计 + `del_flag` 软删
- `@SQLDelete` + `@Where(del_flag=0)`，code 服务级唯一（无 DB UNIQUE）
- 状态 `VARCHAR(16)` + Java 8 `Collections.unmodifiableSet(new HashSet<>(Arrays.asList(...)))` ALL set
- service-level FK 校验 + cross-layer guard + 跨层 join 富化
- v0.0.10.1 起 list 路径 batch enrich + Hibernate Statistics 锁死 SQL count
- v0.0.11 起 parent FK 创建后 immutable（与 Sprint.requirementId / Story.sprintId / Task.projectId 同款）

v0.0.12 引入"产品视角"4 实体链路：

```
ProductCategory (顶层, flat)
    └─ Product (FK NN)
        └─ ProductModule (FK NN)
            └─ Feature (FK NN)
```

与 Project/Sprint 等"项目视角"实体互补，互不直接关联（Demand/Requirement ↔ Feature 关联推到 v0.0.13+）。

## Decisions

### 1. 4-entity 严格父子 FK chain

**方案**：
- `rainier_product_category` — 顶层，无 parent_id（flat — 不做 Organization 式树）
- `rainier_product` — `category_id BIGINT NOT NULL` FK
- `rainier_product_module` — `product_id BIGINT NOT NULL` FK
- `rainier_feature` — `module_id BIGINT NOT NULL` FK
- 父级 id 创建后 immutable（v0.0.11 Decision 11 sibling）— update DTO 不接受

**为什么**：
- 用户明确锁定 D1（ProductCategory flat）— 简化模型，避免 v0 阶段陷入 Organization tree 的循环检测复杂度
- 4 层严格父子是产品架构语义的标准表达（行业通用）
- immutable parent_id 避免跨父级移动的语义混乱（业务上 = 删旧建新）

**备选方案及排除原因**：
- 备选 A — ProductCategory 做树：v0 数据量不够，增 ~6 Scenario（cycle detection / move parent 等）
- 备选 B — 可改 parent_id：每次 update 重跑 parent 校验，复杂度溢出

### 2. 4 个不同的状态机（用户 D2 锁定）

**方案**：
- `ProductCategoryStatus`: `ACTIVE / ARCHIVED` (2)
- `ProductStatus`: `PLANNING / ACTIVE / SUNSET / ARCHIVED` (4)
- `ProductModuleStatus`: `PLANNING / ACTIVE / DEPRECATED` (3)
- `FeatureStatus`: `PLANNING / ACTIVE / DEPRECATED` (3)
- 全部 VARCHAR(16) + Java 8 `Collections.unmodifiableSet` ALL set 模式
- 默认值：Category=`ACTIVE` / Product=`PLANNING` / Module=`PLANNING` / Feature=`PLANNING`

**为什么**：
- 各层语义不同 — Category 是组织维度（仅 active/archived 二值即可表达）；Product 有完整产品生命周期（含 SUNSET 表"已退役但保留历史"）；Module/Feature 用 DEPRECATED 表"软淘汰"（Module/Feature 不存在"退役"概念）
- 不统一为 4 项 — 强行让 Category 多 2 个无意义状态会让前端下拉变臃肿

**备选方案及排除原因**：
- 备选 A — 统一 4 项 ACTIVE/INACTIVE/ARCHIVED/PLANNING：语义模糊
- 备选 B — 复用 ProjectStatus / RequirementStatus：跨能力域复用状态机违反 SRP

### 3. 父级仅校验存在性，**不**校验状态

**方案**：create / update 时 service 仅检查父级 `existsById`（且 `del_flag=0`，由 `@Where` 自动过滤），**不**校验父级 status。

例如：
- 创建 Product 时只检查 `categoryRepo.existsById(categoryId)`，**不**检查 `category.status == ACTIVE`
- 创建 Feature 时只检查 `moduleRepo.existsById(moduleId)`，**不**检查 `module.status != DEPRECATED`

**为什么**：
- 状态级联在 v0 是反业务的 — 用户在 ARCHIVED Category 下补建一个 Product 来表达"这个老分类下其实还有这款产品"是合法用例
- 简化代码 + 测试规模（避免 4 × N 个 status guard TC）
- v0.0.13+ 若引入"路线图"功能再视用户反馈加 status guard

**备选方案及排除原因**：
- 备选 A — 父级必须 ACTIVE/非 ARCHIVED：增 4-12 Scenario；产品现实使用中"老分类下补漏"需求频繁
- 备选 B — 父级 status 改时级联子级：违反 owner mutability 家族原则（v0.0.8 Decision 6b）— 子级 status 是子级自主决策

### 4. Owner mutable 跨 4 个实体（family Decision 6b）

**方案**：4 个实体的 `owner_user_id BIGINT NOT NULL`；update 允许改（service 校验新 user 存在）。

**为什么**：
- 与 v0.0.8 Project / v0.0.10 Sprint / v0.0.11 Task 一致（Decision 6b family）
- 产品 owner 转移是真实场景（PM 离职/调岗）

### 5. code service-level 唯一，4 个实体独立 namespace

**方案**：4 个实体各自的 `code` 在 service.create / update 通过 `repo.existsByCode(code)` 校验唯一；4 个实体的 code 互不冲突（不同表，不同唯一性 namespace）。

**为什么**：
- Family pattern 沿用
- 不同实体共享 code namespace 会强制用户给 Category 加 `CAT-` 前缀 — 用户体验差

### 6. FK chain on delete — 统一 message format `"X has linked Ys"`

**方案**：
- `CategoryService.delete(id)` → `productRepo.countByCategoryId(id) > 0` → 409 `"category has linked products"`
- `ProductService.delete(id)` → `moduleRepo.countByProductId(id) > 0` → 409 `"product has linked modules"`
- `ModuleService.delete(id)` → `featureRepo.countByModuleId(id) > 0` → 409 `"module has linked features"`
- `FeatureService.delete(id)` → 直接软删（Feature 无下游）

**为什么**：
- 4 条 message 格式一致 → 前端只需一段 generic "X has linked Ys" 错误处理
- 与 v0.0.8 Project "project has linked requirements" + v0.0.10 Requirement "requirement has linked sprints" + v0.0.11 "project has linked tasks" 同款 message 模式

### 7. list batch enrich：每个 list endpoint 锁死 = 5 个 SELECT

**方案**：4 个实体的 `*Service.list` 都用 v0.0.10.1 family pattern：

```java
// pseudo
Set<Long> userIds   = entities.stream().map(E::getOwnerUserId).filter(Objects::nonNull).collect(toSet());
Set<Long> parentIds = entities.stream().map(E::getParentId).filter(Objects::nonNull).collect(toSet());  // Category 路径下 parentIds=空集合
Map<Long, User> userMap     = userRepo.findAllById(userIds) → Map
Map<Long, Parent> parentMap = parentRepo.findAllById(parentIds) → Map (Category 跳过此步)
// enrich Map lookup
```

SQL count breakdown:
- Category list: 2 page + 1 batch user = 3 (无 parent)
- Product list: 2 page + 1 user + 1 category = 4
- Module list: 2 page + 1 user + 1 product = 4
- Feature list: 2 page + 1 user + 1 module = 4

但 PA-style 锁定到 **= 5** 作为统一上限（留余地，v0.0.11 PA-1 教训：等号锁死 6 不留余地容易因小重构破坏）。Hibernate Statistics 测试用 `assertTrue(stmtCount <= 5L)` + `assertTrue(stmtCount >= 3L)`（范围断言）。

**为什么**：
- 4 个 entity 的 enrich 都很轻（只 owner + parent 2 类 join），不需复杂 batch
- 统一上限 = 5 让测试 fixture 简单 + 阅读者一眼看到 budget
- 范围断言（≤5 ∧ ≥3）避免 v0.0.11 PA-1 教训（等号被实际 budget 推翻）

**备选方案及排除原因**：
- 备选 A — 等号锁死各自精确值（3/4/4/4）：脆，v0.0.11 PA-1 已证；前端 SQL 一打补丁就破
- 备选 B — 不写 perf 测试：放弃 v0.0.10.1 起建立的 perf guard 家族 — 退步

### 8. Sider 顶级组「产品」位于「组织」与「需求管理」之间

**方案**：
```
组织 → 产品 → 需求管理 → 人事配置  (4 → 5 顶级组)
       │
       ├─ 产品分类  → /pm/product-categories
       ├─ 产品      → /pm/products
       ├─ 产品模块  → /pm/product-modules
       └─ 功能      → /pm/features
```

> 注：路由前缀仍是 `/pm/...`（项目管理 namespace），因为产品也属 PM 范畴；不另开 `/product/...` namespace（避免 url tree 跨度过大）。

**为什么**：
- 用户锁定 D5 — 产品是组织+人事之上的业务基础，「产品」靠前更接近"长期参考"语义
- 「组织 → 产品 → 需求管理」叙事顺序：先有谁（人）、后有什么（产品）、再做什么（项目/需求）

### 9. Frontend cascading select：客户端 filter（v0.0.11 同款模式）

**方案**：
- `ProductEditDrawer` 加 Category 下拉（必选）— 列出所有 Category
- `ModuleEditDrawer` 加 Product 下拉（必选）— 列出所有 Product；Category 下拉过滤（可选）— 选 Category 后 Product 选项 `filter(p => p.categoryId === selected.id)`
- `FeatureEditDrawer` 加 Module 下拉（必选）— Product 下拉过滤选项 → Module 下拉过滤选项
- 切换上级时清空下级选择（v0.0.11 同款）

**为什么**：
- 沿用 v0.0.11 TaskEditDrawer 联动模式 — 团队已熟悉
- v0 size cap=100 足够（产品架构通常 < 100 节点）

### 10. immutable parent_id after create

**方案**：4 个 `*UpdateRequest` 不含 parent_id 字段：
- `ProductUpdateRequest` 不含 `categoryId`
- `ModuleUpdateRequest` 不含 `productId`
- `FeatureUpdateRequest` 不含 `moduleId`

**为什么**：
- 与 v0.0.11 Task / v0.0.10 Story / Sprint 一致
- 移到新父 = 删旧建新（不允许"我把这个 Module 从 Product A 转到 Product B"，避免跨父级混乱）

### 11. 全 4 实体都遵循 family base shape

**方案**：每个实体相同字段集（除 parent_id 差异）：

| 字段 | 类型 | 约束 |
|---|---|---|
| id | BIGINT | auto_increment |
| code | VARCHAR(64) | NN, service unique |
| name | VARCHAR(200) | NN |
| description | VARCHAR(4000) | NULL |
| status | VARCHAR(16) | NN, 各自 ALL 集合 |
| owner_user_id | BIGINT | NN, FK to rainier_user |
| (parent_id) | BIGINT | NN, FK to parent table (Category 没有) |
| createBy/createTime/updateBy/updateTime | audit | from BaseEntity |
| del_flag | bit | softdelete from BaseEntity |

**为什么**：
- 简化 review + 减少 specs 重复内容
- 用户后续读 4 个实体 source code 也只需看一份"骨架 + 差异"

### 12. 错误处理 + AuditorAware

**方案**：
- 所有业务校验抛 `BadRequestException` (400) / `ConflictException` (409) / `NotFoundException` (404)
- 4 个 controller 用既有 `GlobalExceptionHandler` 处理
- `createBy` / `updateBy` 通过 `AuditorAwareImpl` 自动注入登录 username

**为什么**：沿用既有家族 pattern

### 13. 测试基线：每实体 ~15 TCs

**方案**：
- 每实体 backend tests：~12 (Create 8 + Query 2 + Delete 1 + Update 1) — 简化 v0.0.11 Task 18 TC 模板
- 4 entities × 12 ≈ 48 backend TCs
- Frontend：8 TCs (Sider 5 顶级 + 4 路由 + 3 cascading select)
- Perf：4 TCs (1 per list endpoint)
- Total ~60 P0 TCs

**为什么**：
- Phase 2 时间预算 — Phase 4 build 时 ~50 TCs 已经覆盖核心契约
- v0.0.13+ 引入 Demand/Req-Feature 链接时再扩 TCs

### 14. 显式不做事项（v0.0.13+ 候选）

清单：
- Demand / Requirement / Story / Task ↔ Feature 关联字段
- ProductCategory 多级树 / parent_id
- 路线图视图 / 产品看板 / 燃尽图
- 产品成员制
- Feature 优先级 / 估算 / dependsOn
- 模块依赖图
- AI 助理 auto-create
- v0.0.13+ 引入"父级 status guard"开关

## Architecture

### 4-entity data model

```
ProductCategory (flat)
  id, code, name, description, status (ACTIVE|ARCHIVED), ownerUserId
            │
            │ 1..N
            ▼
        Product
          id, code, name, description, status (PLANNING|ACTIVE|SUNSET|ARCHIVED),
          ownerUserId, categoryId NN
            │
            │ 1..N
            ▼
       ProductModule
          id, code, name, description, status (PLANNING|ACTIVE|DEPRECATED),
          ownerUserId, productId NN
            │
            │ 1..N
            ▼
        Feature
          id, code, name, description, status (PLANNING|ACTIVE|DEPRECATED),
          ownerUserId, moduleId NN
```

### Sider menu evolution

```
v0.0.6 起：组织 → 需求管理 → 人事配置                       (3 groups)
v0.0.12 起：组织 → 【产品】 → 需求管理 → 人事配置  ←─ THIS  (4 groups, 5 with hr expand state)
```

### list batch enrich (Product example)

```
GET /api/products?categoryId=&status=&search=&page=&size=
       │
       ▼
  repo.findAll(spec, pageRequest) → Page<Product>
       │   ← 2 page-level SELECTs (data + count)
       │
       ├─ extract ownerUserIds → userRepo.findAllById(ids)
       │       ← 1 batch enrich SELECT
       │
       └─ extract categoryIds → categoryRepo.findAllById(ids)
               ← 1 batch enrich SELECT
       │
       ▼  total ≤ 5 statements per list call (sometimes 4)
  PageResponse<ProductDetail>
```

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|----------|
| 4 实体 × 8 文件 = 32 backend 类，review 工作量大 | family pattern 减少 review 难度（看一份会其他 3 份）；统一 message format（D6）减少前端处理分支 |
| Sider 顶级组从 4 → 5，已有 v0.0.11 AppLayout.test 断言"5 项"会破坏 | 同步更新该测试 + 增 v0.0.12 6-顶级组 TC（Phase 4 同期处理）|
| 4 entity 共 60 TCs，Phase 4 BUILD 耗时是 v0.0.11 的 ~2x | 长程模式跑顺畅；slices.md 拆 16 切片 P0 同优先级 |
| ProductCategory flat 限制未来想做 Org-like 树时需迁移 | 仅在 entity 加 parent_id（NULL = top level）+ 1 cycle 检测，不破坏现有 ProductCategory CRUD；不在 v0.0.12 scope |
| `ddl-auto=update` 一次性建 4 张表，部分表创建失败可能让其他 3 张表也回退 | 实测 Hibernate update 是 per-table 顺序的，单表失败不影响其它；v0.0.10/v0.0.11 同款无问题 |
| 用户在 production 误删 Category 时连锁 409 不能批量推数据 | v0 由 admin 手动逐层 ARCHIVE → softdelete；v0.1 可加 cascade-archive 操作 |
| Sider 6 items in `产品` 顶级组若搭配「需求管理」6 项导致视觉拥挤 | 设计 token 支持垂直布局；6 vs 4 在 typical 1024px 高分屏不溢出 |
