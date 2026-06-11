# Capability: frontend-scaffold

> MODIFIED in v0.0.13-product-restructure (2026-06-10).
> 替换 v0.0.12 引入的两条「产品」相关 Requirement（Sider 4 项 / 4 路由）为 v0.0.13 的 3 项 / 3 路由版本。
> 新增 Requirement：ProductModulesPage 树形显示 + ProductModuleEditDrawer parent 级联。
> v0.0.10–v0.0.12 的其他 Requirement 全部保持不变。

## MODIFIED Requirements

### Requirement: Sider 顶级菜单组「产品」（v0.0.13 改为 3 项）

前端 SHALL 在 Sider 渲染 4 个顶级菜单组 — **组织 → 产品 → 需求管理 → 人事配置**. 「产品」组保持第 2 位（v0.0.12 起加），展开后含 **3 项**：**产品 / 产品模块 / 功能**, 对应 `/pm/products` / `/pm/product-modules` / `/pm/features` 路由. 不再含 v0.0.12 的「产品分类」入口。

#### Scenario: Sider 顶级 4 组 + 产品组 3 路由（v0.0.13）

- **GIVEN** 用户已登录访问 `/`
- **WHEN** 页面渲染完成
- **THEN** 左侧 Sider SHALL 含 4 个顶级菜单组：「组织」/「产品」/「需求管理」/「人事配置」
- **AND** 「产品」 组 SHALL 位于「组织」之后、「需求管理」之前
- **AND** 「产品」 组展开后 SHALL 含 3 项：`"产品"` / `"产品模块"` / `"功能"`
- **AND** 「产品」组 SHALL **不含**「产品分类」项
- **AND** 点击 `"产品"` SHALL 跳转 `/pm/products`
- **AND** 点击 `"功能"` SHALL 跳转 `/pm/features`

### Requirement: /pm/product-* 3 路由注册（v0.0.13 去 categories）

前端 SHALL 在 router 中注册 **3** 条路由：`/pm/products` → `ProductsPage`, `/pm/product-modules` → `ProductModulesPage`, `/pm/features` → `FeaturesPage`. `/pm/product-categories` 路由 SHALL 删除（访问返回 404 / 路由 fallback）。

#### Scenario: /pm/products 路由直接访问 + grep guard

- **GIVEN** 用户已登录
- **WHEN** 浏览器直接访问 `/pm/products`
- **THEN** SHALL 渲染 `ProductsPage` 组件
- **AND** `grep -c "/pm/products" frontend/src/AppRoutes.tsx` SHALL ≥ 1

#### Scenario: /pm/product-categories 路由已删除

- **GIVEN** 用户已登录
- **WHEN** 浏览器直接访问 `/pm/product-categories`
- **THEN** SHALL **不**渲染 `ProductCategoriesPage`（页面已删）
- **AND** `grep -c "/pm/product-categories" frontend/src/AppRoutes.tsx` SHALL 为 0

---

## ADDED Requirements

### Requirement: ProductModulesPage 树形列表显示

前端 SHALL 在 `/pm/product-modules` 页面将模块列表渲染为树形结构 — 顶层模块（parentId 为 null）作为根节点，子模块通过 UL 缩进显示；保留 search + status filter。

#### Scenario: 树形渲染含 ≥ 2 层缩进

- **GIVEN** 后端返回 Module 列表：M1 (id=1, parentId=null) → M2 (id=2, parentId=1) → M3 (id=3, parentId=2)
- **WHEN** `/pm/product-modules` 页面渲染完成
- **THEN** SHALL 渲染嵌套 UL/LI 结构
- **AND** M2 SHALL 在 M1 的 `<ul>` 子元素内
- **AND** M3 SHALL 在 M2 的 `<ul>` 子元素内
- **AND** M2 SHALL 比 M1 多一级缩进（CSS padding-left 或 indent class）

#### Scenario: 顶层 module（parentId=null）作为树根渲染

- **GIVEN** 后端返回 3 个顶层 Module（parentId 均为 null）+ 2 个子 module
- **WHEN** 渲染完成
- **THEN** SHALL 在最外层 UL 含 3 个 LI（根级）
- **AND** 每个根 LI SHALL 可独立展开 / 折叠子树（可选 UX，至少不报错）

### Requirement: ProductModuleEditDrawer Product + parentModule 二级 cascade

前端 SHALL 在 `ProductModuleEditDrawer` 中替换原 Category→Product 二级 cascade 为 **Product → 可选 parentModule** 二级 cascade. 选 Product 后通过服务器侧过滤 `listProductModules({productId})` 拉取候选父模块；切换 Product 时清空 parentModule. 创建时 parentModule 默认空（顶层）.

#### Scenario: Product 切换触发 parentModule 候选刷新

- **GIVEN** Product A 下有 3 个 Module, Product B 下有 2 个 Module
- **WHEN** 用户在 EditDrawer 切换 Product A → Product B
- **THEN** parentModule 下拉 SHALL 清空当前选项
- **AND** SHALL 触发 `listProductModules({productId: B.id})` API 调用
- **AND** parentModule 下拉 SHALL 显示 Product B 的 2 个 module 选项

#### Scenario: parentModule 留空 → 创建顶层

- **GIVEN** 已选 Product P
- **WHEN** 用户不选 parentModule 直接点保存
- **THEN** SHALL 提交 `POST /api/product-modules` body 不含 parentId（或显式 null）
- **AND** 创建结果 SHALL 为顶层 module

### Requirement: FeatureEditDrawer 模块下拉显示父链 pathName

前端 SHALL 在 `FeatureEditDrawer` 的模块下拉中使用后端返回的 `pathName` 字段渲染选项标签（如 `"钱包 / 余额"`），让用户能区分多层结构下同名子模块。仍是单选，productId 切换时重拉。

#### Scenario: 多层模块下拉显示完整 pathName

- **GIVEN** Product 1 下有 Module 树：根 M1 (id=10, name="钱包") → M2 (id=20, name="余额")
- **WHEN** 用户在 FeatureEditDrawer 选 Product 1
- **THEN** 模块下拉 SHALL 含 2 个选项
- **AND** M1 选项的可见文本 SHALL 含 `"钱包"`
- **AND** M2 选项的可见文本 SHALL 含 `"钱包 / 余额"`（pathName）
