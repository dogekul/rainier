# Capability: frontend-scaffold

## MODIFIED Requirements (from change 2026-06-05-position-role)

### Requirement: Sider 含「人事配置」菜单组

前端 SHALL 在 AppLayout 左侧 Sider 新增菜单组「人事配置」，位于「需求管理」之后；展开后含 3 项：岗位 / 角色 / 用户角色。

#### Scenario: Sider 含「人事配置」3 路由

- **GIVEN** 用户已登录访问 `/`
- **WHEN** 页面渲染完成
- **THEN** 左侧 Sider SHALL 渲染
- **AND** Sider SHALL 含菜单组 `"人事配置"`
- **AND** 该组展开后 SHALL 含 3 项：`"岗位"`、`"角色"`、`"用户角色"`
- **AND** 点击 `"岗位"` SHALL 跳转 `/hr/positions`

### Requirement: /hr/* 路由全部注册

前端 SHALL 在 `AppRoutes.tsx` 注册 `/hr/positions`、`/hr/roles`、`/hr/user-roles` 三条路由；访问 `/hr` SHALL 重定向至 `/hr/positions`。

#### Scenario: 路由直接访问

- **GIVEN** 用户已登录
- **WHEN** 浏览器直接访问 `/hr/positions`
- **THEN** SHALL 渲染 PositionsPage 组件
- **AND** `grep -c "/hr/positions" frontend/src/AppRoutes.tsx` SHALL ≥ 1（防止 linter 静默回退）

### Requirement: UsersPage 编辑抽屉新增「岗位」下拉

前端 SHALL 在 `UsersPage` 编辑抽屉新增「岗位」下拉，异步加载岗位池；保存时 POST/PUT 包含 `positionId`；列表新增「岗位」列展示 `positionName + category`。

#### Scenario: 新建用户时选择岗位

- **GIVEN** 抽屉打开（mock `listPositions` 返回 2 条岗位 id=1/2）
- **WHEN** 用户从下拉选择 id=1，填写 loginName + name，点保存
- **THEN** mock `createUser` SHALL 被调用且参数 body.positionId SHALL 等于 1
