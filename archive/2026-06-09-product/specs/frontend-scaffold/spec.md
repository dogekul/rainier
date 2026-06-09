# Capability: frontend-scaffold

## MODIFIED Requirements (v0.0.12)

### Requirement: Sider 顶级菜单组 5 项（v0.0.12 起加「产品」组）

前端 SHALL 在 Sider 渲染 5 个顶级菜单组：**组织 → 产品 → 需求管理 → 人事配置**（v0.0.11 4 组 + v0.0.12 新「产品」组，位于「组织」与「需求管理」之间）；「产品」组展开后 SHALL 含 4 项：**产品分类 / 产品 / 产品模块 / 功能**，对应 `/pm/product-categories` / `/pm/products` / `/pm/product-modules` / `/pm/features` 路由。

#### Scenario: Sider 顶级 5 组 + 产品组 4 路由（v0.0.12）

- **GIVEN** 用户已登录访问 `/`
- **WHEN** 页面渲染完成
- **THEN** 左侧 Sider SHALL 含 5 个顶级菜单组：「组织」/「产品」/「需求管理」/「人事配置」
- **AND** 「产品」 组 SHALL 位于「组织」之后、「需求管理」之前
- **AND** 「产品」 组展开后 SHALL 含 4 项：`"产品分类"` / `"产品"` / `"产品模块"` / `"功能"`
- **AND** 点击 `"产品分类"` SHALL 跳转 `/pm/product-categories`
- **AND** 点击 `"功能"` SHALL 跳转 `/pm/features`

### Requirement: /pm/product-* 4 路由注册 + 4 CRUD 页（v0.0.12）

前端 SHALL 在 router 中注册 4 条新路由：`/pm/product-categories` → `ProductCategoriesPage`、`/pm/products` → `ProductsPage`、`/pm/product-modules` → `ProductModulesPage`、`/pm/features` → `FeaturesPage`。每个页面 SHALL 提供 list（含 filter 与分页）+ 新建按钮 + 行编辑/删除。

#### Scenario: /pm/products 路由直接访问 + grep guard

- **GIVEN** 用户已登录
- **WHEN** 浏览器直接访问 `/pm/products`
- **THEN** SHALL 渲染 `ProductsPage` 组件
- **AND** `grep -c "/pm/products" frontend/src/AppRoutes.tsx` SHALL ≥ 1（防止 linter 静默回退）

#### Scenario: ProductEditDrawer Category 联动 — 切 Category 后 list 自动过滤

- **GIVEN** ProductEditDrawer 打开为新建模式；mock listCategories 返回 2 行
- **WHEN** 用户选 Category id=1
- **THEN** `categoryId` 字段 SHALL 等于 1
- **AND** 后续创建提交 body.categoryId SHALL 为 1

#### Scenario: FeatureEditDrawer Module 联动 — 切 Product 后 Module 选项自动过滤

- **GIVEN** FeatureEditDrawer 打开为新建模式；mock listProducts 返回 2 行 (id=1 categoryId=1 / id=2 categoryId=2)；mock listModules 返回 3 行 (id=10 productId=1 / id=20 productId=1 / id=30 productId=2)；初始选 Product=1
- **WHEN** Module 下拉选项渲染
- **THEN** Module 选项 SHALL 仅含 id=10 与 id=20（productId=1 的两个）
- **AND** id=30 SHALL **不**出现在 Module 选项
