# Capability: user-profile-route

<!-- Added from change 2026-06-30-user-profile-route (v0.0.118) -->

## ADDED Requirements

### Requirement: 指定用户档案前端路由

系统 SHALL 提供受保护的 `/users/:id/profile` 前端路由，展示后端授权后的指定用户档案。

#### Scenario: 下属档案路由已注册

- **GIVEN** 用户已登录且前端应用加载完整路由树
- **WHEN** 用户访问 `/users/42/profile`
- **THEN** 系统 SHALL 渲染指定用户档案页面而不是兜底回 Workbench
- **AND** 系统 SHALL 保持该路由位于 `ProtectedRoute` 保护范围内

#### Scenario: 指定用户档案调用正确 API

- **GIVEN** 用户已登录并访问 `/users/42/profile`
- **WHEN** 指定用户档案页面挂载
- **THEN** 系统 SHALL 调用 `GET /api/users/42/profile`
- **AND** 系统 SHALL 使用返回的 `ProfileResponse` 渲染身份、贡献、能力标签和组织身份

#### Scenario: 我的档案行为不变

- **GIVEN** 用户已登录并访问 `/profile`
- **WHEN** 我的档案页面挂载
- **THEN** 系统 SHALL 继续调用 `GET /api/me/profile`
- **AND** 系统 SHALL 继续使用既有档案展示结构

### Requirement: 下属查看档案链路闭环

系统 SHALL 让「我的下属」页面的「查看档案」链接进入真实档案页。

#### Scenario: 下属链接进入真实档案页

- **GIVEN** `/me/subordinates` 表格中存在用户 `id=42`
- **WHEN** 用户点击该行的「查看档案」链接
- **THEN** 系统 SHALL 导航到 `/users/42/profile`
- **AND** 系统 SHALL 由 `/users/:id/profile` 路由渲染档案页面

## OutOfScope

- 不新增后端接口。
- 不改变 C3 服务端鉴权矩阵。
- 不新增 HR/Admin 直通规则。
- 不做多级组织权限推断。
