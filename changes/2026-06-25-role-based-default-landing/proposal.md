# H6 — 登录默认落地按角色分流

## 问题
登录后所有用户都跳 `/`（我的工作台）。但 PMO/架构师/项目负责人/需求负责人各自的核心日常入口不同。要求登录后按角色直接落到主要工作页，减少一跳。

## 范围
1. 后端 `GET /api/auth/me` response 增加 `defaultLandingPath` 字段。
2. 解析顺序（首条命中即返回）：
   - admin（任意 role.adminAccess=true） → `/sys/compliance`
   - role.code == `PMO` → `/pmo`
   - role.code == `ARCHITECT` → `/architect`
   - 拥有 owner 项目（`project.ownerUserId = me`，count > 0） → `/pm/cockpit`
   - 拥有 owner 需求（`requirement.ownerUserId = me`，count > 0） → `/inbox`
   - 默认 → `/`
3. 前端 `LoginPage` 成功登录后立即 `me()`，按 `defaultLandingPath` 跳转。
4. 前端 `ProtectedRoute`：首次进入 `/` 且 hydrated 后若 `defaultLandingPath != /` → `Navigate replace`。
5. 前端 `AppLayout` 顶栏加「返回工作台」链接（当 `pathname !== '/'` 时显示）。

## OutOfScope
- 用户自定义首页 / admin 配置默认首页。
- 重定向链 / 循环防护（admin 用户若 admin 路径被禁会被守卫弹回 `/`，下一次仍重定向；MVP 接受）。
- 仅根据「记得上次访问页」恢复。

## 验收
- 7 个 seed 账户登录后落地路径正确（alice=admin→`/sys/compliance` 等）。
- 单元测试覆盖：admin、PMO、ARCHITECT、owner-project、owner-requirement、default 6 条路径。
