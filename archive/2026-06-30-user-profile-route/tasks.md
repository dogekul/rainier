# v0.0.118-user-profile-route 任务清单

## 1. user-profile-route（P0）

- [x] 1.1 写 RED 测试：`AppRoutes` 挂载 `/users/:id/profile`
- [x] 1.2 写 RED 测试：`UserProfilePage` 调 `getUserProfile(id)` 并渲染 profile
- [x] 1.3 写 RED/回归测试：`/profile` 仍调用 `getMyProfile()`
- [x] 1.4 新增 `getUserProfile(userId)` API 方法
- [x] 1.5 抽取 `ProfileView` 共享展示组件
- [x] 1.6 新增 `UserProfilePage` 并注册路由

## 2. 测试与验证（P0）

- [x] 2.1 运行聚焦 Vitest：Profile + AppRoutes
- [x] 2.2 运行前端全量 Vitest
- [x] 2.3 运行 TypeScript build 或 `tsc --noEmit`
- [x] 2.4 生成 Phase 5 `test-report.md`

## 3. P6 前本地预览修复（P0）

- [x] 3.1 复现 `127.0.0.1:5174` 登录 403 CORS 问题
- [x] 3.2 修复 Vite dev proxy Origin 转发
- [x] 3.3 浏览器验证 `alice / rainier123` 登录进入工作台
