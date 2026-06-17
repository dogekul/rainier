# v0.0.21-admin-authz — 后端 admin 端点鉴权收口（B）

> Baseline: tag `v0.0.20-role-nav` / commit a15ef66. backend 354 + frontend 106 测试 green, 19 表.
> 来源: v0.0.20 的显式后续 B —— 前端已做 UX 收口（隐藏菜单 + 路由守卫），但后端 API 仍对任何调用者开放。

## Why

v0.0.20 给了角色分级，但只在**前端**生效：菜单隐藏、路由守卫都是客户端的。后端现状更糟 —— `SecurityFilter`
**只校验 `/api/auth/me` 的 token，其余所有 CRUD 端点完全不鉴权**（连 token 都不要）。任何人都能直接
`POST /api/roles`、`DELETE /api/organizations/1`、`GET /api/audit-logs`。本版把 admin 端点的真正鉴权收口：
未带有效 token → 401；已登录但非管理员 → 403。提升态复用 v0.0.20 的 `Role.adminAccess`（任一所属角色 admin）。

## What Changes

- 新增 `AdminAuthorizationInterceptor`（`HandlerInterceptor`，经 `WebMvcConfigurer` 注册到 admin 路径），
  对受管端点校验：解析 Bearer token（无/失效 → 401）→ 查提升态（非管理员 → 403）。
- 新增 `ElevationService.isElevated(username)`：解析 User → 任一 `UserRole`→`Role.adminAccess=true`（exists 查询）。
- 新增 `ForbiddenException` + `GlobalExceptionHandler` 映射为 **403**。
- 端点分级（D2，Gate 1 选定 A/B）：
  - **Tier A（全方法仅管理员）**：`/api/organizations`、`/api/user-organizations`、`/api/positions`、
    `/api/roles`、`/api/user-roles`、`/api/audit-logs`、`/api/products`。
  - **Tier B（写仅管理员，GET 放开给已认证用户）**：`/api/users`、`/api/features`、`/api/product-modules`
    （全员页面的 owner/assignee 下拉 + SprintFeaturePanel 需读这三个）。
- 开关门控：`app.security.admin-authz.enabled`（生产 `application.yml` = true；`application-test.yml` = false），
  使 ~39 个既有 admin 控制器测试零改动，鉴权由专用测试 + E2E 验证（D5，Gate 1 选定）。
- **防自锁引导**（bootstrap）：新增启动 runner，当**无任何角色 adminAccess=true** 时，把
  `app.security.bootstrap-admin-role`（默认 `PMO`）的角色提升为 admin（幂等、有日志、仅门控开启时跑）。
  否则授权一开，"给第一个管理员"这件事本身（`PUT /api/roles` 改 adminAccess）也需要 admin → 死锁。

## Capabilities

### New Capabilities

- `backend-authz`：admin 端点的 token + 提升态鉴权（拦截器 + ElevationService + 403 + bootstrap）。

### Modified Capabilities

- 无实体/DTO/前端改动（提升态复用 v0.0.20 `Role.adminAccess`，前端 v0.0.20 已 role-based 一致）。

## Impact

**代码层面**（后端 only）：
- 新增：`com.rainier.authz` 包 —— `ElevationService`、`AdminAuthorizationInterceptor`、`AdminPaths`（Tier A/B 常量）、
  `AdminAuthzBootstrap`（runner）；`common/exception/ForbiddenException`；`config/WebMvcConfig`(注册拦截器)。
- 改动：`GlobalExceptionHandler`（+ForbiddenException→403）；`application.yml`（+admin-authz.enabled / +bootstrap-admin-role）；
  `application-test.yml`（admin-authz.enabled=false）。
- **前端**：0 改动（v0.0.21 纯后端；v0.0.20 的 isElevated 已 role-based，与后端提升态同源）。
- **配置/基础设施**：0 新表、0 新列、0 新依赖。

## 关键决策（待 Gate 1 确认）

- **D1 机制**：`HandlerInterceptor` + `WebMvcConfigurer`（路径模式匹配清晰、可注入 repo 查提升态）。备选：扩展
  `SecurityFilter`（servlet 层，路径匹配较糙）。倾向 interceptor。
- **D2 分级**：✅ Tier A/B（Gate 1 已选）。GET 放开仅 `users/features/product-modules`（全员依赖），其余全方法收口。
- **D3 状态码**：无/失效 token → **401**（`UnauthorizedException`）；已认证非管理员 → **403**（新 `ForbiddenException`）；
  `OPTIONS` 预检放行（不拦 CORS preflight）。
- **D4 提升态来源**：复用 `Role.adminAccess`（role-based，与前端同源），新 `ElevationService.isElevated(username)`。
  **不**改 me() / 不加 user 级 admin 配置（保持单一来源）。
- **D5 测试策略**：✅ 开关门控（Gate 1 已选）。生产 true / test false；新 `AdminAuthorizationTest`
  （`@TestPropertySource` 置 true）验证 401/403/200 + Tier B GET 放行 + 全员端点不被误伤；E2E 真实 profile 验证。
- **D6 引导（bootstrap）**：无 admin 时启动提升 `PMO`（可配）为 admin，防死锁/防永久自锁。**会一次性把 PMO.admin_access
  由 NULL→true**（这是「第一个管理员」的合法 seed，非测试改数据；有日志、幂等）。← 请重点确认是否接受此自动引导，
  或改为「手动 SQL 设首个 admin（不自动改数据）」。

## 显式排除（往后）

- 细粒度权限点 / 资源级 ACL / 按项目的 admin —— 仍是单 `adminAccess` 布尔。
- 「所有 /api/** 都要求 token」的全局认证基线（本版只收 admin 端点；全员端点维持现状 token-optional）—— 可作后续 C。
- admin 写操作的审计 actor 由 "system" 改为真实用户名（拦截器已解析 username，可顺带 set 但本版**不动**审计语义，留后续）。
- 真实身份源替换 mock JWT（既有 v0 占位，独立大改）。

## Success Criteria

- [ ] 无 token `POST /api/roles` → **401**；无 token `GET /api/audit-logs` → **401**。
- [ ] 非管理员 token `POST /api/roles` → **403**；`DELETE /api/organizations/1` → **403**。
- [ ] 管理员 token `POST /api/roles` → 正常（201/200）。
- [ ] 非管理员 token `GET /api/users` / `GET /api/features` / `GET /api/product-modules` → **200**（Tier B 读放行）；
      其 `POST/PUT/DELETE` → **403**。
- [ ] 全员端点（`/api/projects`、`/api/tasks`、`/api/auth/me` 等）行为不变（不被拦截器误伤）。
- [ ] `app.security.admin-authz.enabled=false`（test profile）时既有 ~39 个 admin 控制器测试零改动全绿。
- [ ] bootstrap：全新/无 admin 时启动后 `PMO` 成为 admin（日志可见）；已有 admin 时启动 no-op（幂等）。
- [ ] 全量回归 green（backend ≥354 + 新增 AdminAuthorizationTest；frontend 106 不变）；0 新表/列。
- [ ] E2E：真实 profile 下 401/403/200 三态成立；bootstrap 生效；存量数据除 bootstrap seed 外不变。
