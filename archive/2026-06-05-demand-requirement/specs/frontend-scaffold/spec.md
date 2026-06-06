# Capability: frontend-scaffold

## MODIFIED Requirements (from change 2026-06-05-demand-requirement)

### Requirement: Sider 含「需求管理」菜单组

前端 SHALL 在登录后页面（AppLayout 下）的左侧 Sider 中新增菜单组「需求管理」，位于「组织」之后；展开后含 3 项：诉求 / 需求 / 诉求-需求关联。

#### Scenario: Sider 含「需求管理」3 路由

- **GIVEN** 用户已登录访问 `/`
- **WHEN** 页面渲染完成
- **THEN** 左侧 Sider SHALL 渲染
- **AND** Sider SHALL 含菜单组 `"需求管理"`
- **AND** 该组展开后 SHALL 含 3 项：`"诉求"`、`"需求"`、`"诉求-需求关联"`
- **AND** 点击 `"诉求"` SHALL 跳转 `/pm/demands`

### Requirement: /pm/* 路由全部注册

前端 SHALL 在 router 中注册 `/pm/demands`、`/pm/requirements`、`/pm/demand-requirements` 三条路由；访问 `/pm` SHALL 重定向至 `/pm/demands`。

#### Scenario: 路由直接访问

- **GIVEN** 用户已登录
- **WHEN** 浏览器直接访问 `/pm/demands`
- **THEN** SHALL 渲染 DemandsPage 组件
- **AND** `grep -c "/pm/demands" frontend/src/AppRoutes.tsx` SHALL ≥ 1（防止 linter 静默回退）

### Requirement: requirement 编辑抽屉支持「源诉求」多选

前端 SHALL 在 RequirementsPage 的编辑抽屉中提供「源诉求」分区，渲染分页 + 搜索 demand 列表 + 复选；保存时把 checked id 收集为 `sourceDemandIds: number[]` 并传给 `POST /api/requirements`（新建）或 `PUT` 后调用关联 API（编辑）。

#### Scenario: 新建需求时多选源诉求

- **GIVEN** 抽屉打开（mock `listDemands` 返回 2 条 demand id=10、id=20）
- **WHEN** 用户勾选 id=10 + id=20，填写 code/title/description，点保存
- **THEN** mock `createRequirement` SHALL 被调用且参数 body.sourceDemandIds SHALL 等于 `[10, 20]`
