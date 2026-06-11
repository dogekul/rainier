# v0.0.13-product-restructure — 删除「产品分类」+ ProductModule 自引用树（2~3 层可配）

## Why

v0.0.12 引入 ProductCategory→Product→ProductModule→Feature 4 层架构，但**「分类」和「产品」的层次区分太薄**——分类只是给产品挂个标签，没有承担真正的组织职责。**真正缺的是 Module 内部的「大模块 → 小模块」结构**：一个产品下的功能集合在自然演化中会出现"中台→子能力→具体功能"的多层归类。把分类层去掉、让 Module 自身承载层级，结构更贴近实际使用。

## What Changes

- **C1.** **DELETE 整个 `entity-product-category` capability**：移除 backend `com.rainier.productcategory.*`（8 src + 5 test = 13 文件）+ 前端 `pages/ProductCategory/`（4 文件）+ `api/productCategory.ts` + `specs/entity-product-category/`。
- **C2.** **`Product` 去掉 `categoryId` 字段**：实体/3 DTO/Repo/Service/Controller/5 test 全部移除 category 字段、参数、enrich 调用；前端 `api/product.ts` 类型 + `ProductsPage` + `ProductEditDrawer` 移除 Category select 与过滤；test 同步。
- **C3.** **`ProductModule` 加 `parent_id BIGINT NULL` 自引用 FK**：实体加字段 + 关联；DB 唯一约束 `uk_product_module_parent_code(parent_id, code)` 替换原 service-level code 唯一；null parentId 视为「顶层 scope」与同 product 内顶层 code 唯一。
- **C4.** **`ProductModuleService` 三大算法**：(a) **深度上限校验**——create/update 时 walk parent chain，超过配置 `product-module.depth.max`（默认 3，可改）抛 400 `"max module depth exceeded: <n> > <max>"`；(b) **跨产品父级拒绝**——新 parent 必须同 productId，否则 400 `"parent module must belong to the same product"`；(c) **环检测**——update parentId 时 DFS 子树扫描，新 parent 不能在当前节点的子孙集合中，否则 400 `"cycle detected: would create ancestor loop"`。
- **C5.** **`parentId` 可变**：UpdateRequest 含 `parentId: Long?`；service 在 C4 三检通过后允许换 parent。Code 仍服务级（在新 (parentId, code) 唯一约束下）唯一性校验；productId 仍创建后锁死（继承 v0.0.12 Decision 11）。
- **C6.** **Module 删除策略升级 — 双向 FK 链**：原来只检查 child Feature；现在同时检查 child Module（`countByParentId`），任一非零都 409。两条消息明确区分：`"module has linked features"` / `"module has linked sub-modules"`。Feature delete 链不变。
- **C7.** **数据迁移（不可逆）**：新增 `LegacyProductCategoryCleanupTest`，在 `@BeforeAll` 执行 `DROP TABLE IF EXISTS rainier_product_category` + `ALTER TABLE rainier_product DROP COLUMN IF EXISTS category_id`；并在测试体内断言两者已不存在（`SHOW TABLES` 不含 `rainier_product_category`、`DESCRIBE rainier_product` 不含 `category_id` 列）。v0.0.12 才交付 1 天，无生产数据需保留。
- **C8.** **前端 `ProductModuleEditDrawer` 改 cascade**：原来 Category→Product 二级；改为 **Product→（可选）parentModule** 二级 cascade。parentModule 下拉用 `listProductModules({productId, parentDepthLt: max-1})` 服务器侧过滤（A2 同款模式）——只展示「子还能挂得下」的候选父节点。
- **C9.** **前端 `ProductModulesPage` 改为树形显示**：客户端拉 flat list 后组装 tree，UI 渲染为 `UL` 缩进列表 + depth indent；保留 search + status 过滤；hierarchical context 在 EditDrawer 的 parentModule 显示中体现为「父链路径：A / B / C」。
- **C10.** **前端 `FeatureEditDrawer` 模块下拉适配**：原来 Module 选项是 flat list；改为按 tree path 显示（"A / B / C"）便于在多层场景下定位，但仍是单选；productId 切换时仍重新拉取 Modules（A2 服务器侧过滤）。
- **C11.** **Sider 调整**：「产品」组从 4 项减为 3 项（产品 / 产品模块 / 功能），删「产品分类」入口；`AppLayout.test.tsx` 同步改断言；`AppRoutes.tsx` 删 `/pm/product-categories` 路由。

### 显式排除（推到 v0.0.14+）

- 跨产品 reparent（必须同 productId，跨产品先删后建）
- 拖拽改父级 UI（API 支持，UI 不做）
- Module 依赖关系图 / 横向引用
- Demand/Requirement ↔ Feature 关联（B3 推到 v0.0.14）
- Feature 增加 `productId` 直接字段（依然通过 Module 间接拿）
- Module 多人协作 / 权限
- Module 状态自动联动（child status → parent status 推断）
- 历史层级变更审计日志

## Capabilities

### Deleted Capabilities

- `entity-product-category` — 整个 capability 删除（包含 spec + backend + frontend + DB 表）。

### Modified Capabilities

- `entity-product` — 移除 `categoryId` 字段、Category 校验、enrich Category 部分；Update 不变（categoryId 已不在 DTO 里）。
- `entity-product-module` — 新增 4 个 Requirements：「parentId 自引用」「深度上限」「跨产品父级拒绝」「环检测」；改写「软删 FK 保护」为双向链；改写「`code` 唯一」为 `(parentId, code)` 组合唯一。
- `frontend-scaffold` — Sider「产品」组从 4 项改 3 项；路由从 4 个减为 3 个；新加 ProductModule 树形列表 + parent select Requirement。

### New Capabilities

- 无（纯重构）

## Impact

**代码层面**：
- **删除**：13 后端文件（productcategory 包）+ 4 前端文件（pages/ProductCategory + api/productCategory.ts）+ 1 spec 目录（entity-product-category）= 18 文件 + 1 目录。
- **修改**：
  - Backend `com.rainier.product.*`（5 src + 5 test = 10 文件）— 去 categoryId。
  - Backend `com.rainier.productmodule.*`（5 src + 6 test = 11 文件）— 加 parentId/depth/cycle/reparent + 双向 FK 删除。
  - Backend `com.rainier.feature.*` — 无变化（间接受益）。
  - Frontend `api/product.ts`、`api/productModule.ts`、`pages/Product/*`、`pages/ProductModule/*`、`pages/Feature/FeatureEditDrawer.*`、`AppLayout.*`、`AppRoutes.tsx`（约 12 文件）。
  - Specs（4 文件）。
- **新增**：
  - `LegacyProductCategoryCleanupTest.java`（DB schema 迁移测试）。
  - 新前端组件：`ProductModuleTreeView.tsx`（客户端组装 tree + UL 缩进渲染）。
  - 配置项：`product-module.depth.max` in `application.yml`（默认 3）。
- **总计**：~30 修改 + ~3 新增 + ~18 删除 = **51 文件变动**。

**配置层面**：
- 新增 `application.yml` 的 `rainier.product-module.depth.max: 3`（可改）。
- `LegacyProductCategoryCleanupTest` 跑后 DB schema 永久变动（不可回滚）。

**基础设施**：
- 17 张表 → **16 张表**（删 `rainier_product_category`）。
- `rainier_product` 表 `category_id` 列 DROP。
- `rainier_product_module` 表新增 `parent_id` 列 + `idx_product_module_parent_id` + `uk_product_module_parent_code(parent_id, code)`（替换原 service-only code 唯一）。

## Success Criteria

- [ ] `SHOW TABLES` 结果**不再包含** `rainier_product_category`，且共 16 张表。
- [ ] `DESCRIBE rainier_product` **不再包含** `category_id` 列。
- [ ] `DESCRIBE rainier_product_module` **包含** `parent_id` 列（BIGINT，可空）。
- [ ] `LegacyProductCategoryCleanupTest` 单测通过，断言 schema 清理完成。
- [ ] 后端测试：删除 ProductCategory 4 个测试类（~22 tests）；ProductModule 测试新增至少 5 个（parent 校验 + depth + cycle + reparent + sub-module-409）；总数 ≈ 265 - 22 + 5 = 248±。
- [ ] 后端 `POST /api/product-modules` 父级不同 productId → 400 `"parent module must belong to the same product"`。
- [ ] 后端 `POST /api/product-modules` 当 chain depth > max → 400 `"max module depth exceeded"`。
- [ ] 后端 `PUT /api/product-modules/{id}` 设 parentId 指向自己的子孙 → 400 `"cycle detected"`。
- [ ] 后端 `DELETE /api/product-modules/{id}` 当有子 Module → 409 `"module has linked sub-modules"`。
- [ ] 后端 `DELETE /api/product-modules/{id}` 当有 Feature → 409 `"module has linked features"`（保持 v0.0.12）。
- [ ] 前端 `/pm/product-categories` 路由**返回 404**（已删）；Sider「产品」组只剩 3 项。
- [ ] 前端 `ProductsPage` 列表**不再含** Category 列、`ProductEditDrawer` **不再含** Category select。
- [ ] 前端 `ProductModulesPage` 渲染**树形结构**（含至少 2 层缩进的快照测试）。
- [ ] 前端 `ProductModuleEditDrawer` 含 parentModule 下拉，调用 `listProductModules({productId})` 时按 productId 过滤；选超深父级时控件不可选。
- [ ] 前端测试：删除 ProductCategory 测试（~1 个）；新增 ProductModule 树形渲染 + parent select 测试（≥ 2 个）。
- [ ] E2E `curl chain`：建 Product → 顶层 Module A → 子 Module A.1 → A.1 下 Feature → 跨产品 reparent 拒绝 → 环检测拒绝 → 深度上限拒绝 → 删 A 时 409 sub-modules。
- [ ] 整套 backend + frontend 测试套件 green，迁移测试 green。
