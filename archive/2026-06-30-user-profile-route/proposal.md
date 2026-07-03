# user-profile-route

## Why

团队负责人在「我的下属」页面点击「查看档案」时，前端链接已经指向 `/users/{id}/profile`，但路由树没有注册该路径，点击后会落到兜底路由再回到 Workbench。后端 C3 的 `GET /api/users/{id}/profile` 已经完成档案聚合和上下级鉴权，前端缺少最后一段路由与页面消费，导致下属档案钻取体验断开。

## What Changes

- 新增前端 API 方法 `getUserProfile(userId)`，调用 `GET /api/users/{id}/profile`。
- 新增指定用户档案页面，展示后端返回的 `ProfileResponse`。
- 抽取档案展示组件，让 `/profile` 和 `/users/:id/profile` 复用同一套 UI。
- 在 `AppRoutes` 注册受保护路由 `/users/:id/profile`。
- 增加前端测试覆盖路由注册、指定用户 API 调用和页面渲染。

## Capabilities

### Modified Capabilities

- `me-profile`：复用现有档案展示结构，从“我的档案”扩展到“指定用户档案”读取。
- `subordinates-nav-entry`：补齐「查看档案」链接的目标页面。
- `frontend-scaffold`：新增受 `ProtectedRoute` 保护的 `/users/:id/profile` 路由。

### New Capabilities

- 无后端新能力；本次只补齐已有后端能力的前端入口。

## Impact

**代码层面**：
- 前端小范围改动，涉及 `frontend/src/api/profile.ts`、`frontend/src/pages/Profile/*`、`frontend/src/AppRoutes.tsx` 及对应 Vitest 测试。

**配置层面**：
- 无配置变更。

**基础设施**：
- 无新服务、无数据库变更、无新后端 API。

## Success Criteria

- [x] 访问 `/profile` 仍调用 `/api/me/profile` 并展示当前用户档案。
- [x] 访问 `/users/42/profile` 会调用 `/api/users/42/profile`。
- [x] `/users/:id/profile` 在 `AppRoutes` 中注册且受 `ProtectedRoute` 保护。
- [x] 「我的下属」页面的「查看档案」链接点击后能进入真实档案页，不再落到 Workbench。
- [x] 前端测试覆盖新增路由和指定用户档案渲染。
- [x] 后端不改动，既有 C3 鉴权规则保持由服务端兜底。
