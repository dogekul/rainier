# Capability: backend-authz

> Change log:
> - 2026-06-16 (v0.0.21-admin-authz) — NEW. Admin-endpoint authorization: an `AdminAuthorizationInterceptor`
>   (registered on `/api/**`, matching on the Spring lookup path so matrix-params can't bypass it) gates
>   admin operations — missing/invalid token → 401, authenticated non-admin → 403. Tier A
>   (organizations/user-organizations/positions/roles/user-roles/audit-logs/products) is all-methods;
>   Tier B (users/features/product-modules) gates writes only (all-users pages read them). Elevation =
>   any of the caller's roles has `adminAccess=true` (`ElevationService`, reuses v0.0.20 `Role.adminAccess`;
>   disabled users excluded). Gated by `app.security.admin-authz.enabled` (true prod / false test profile).
>   `AdminAuthzBootstrap` elevates `app.security.bootstrap-admin-role` (default PMO) at startup when no
>   admin role exists (anti-lockout).

> Admin-endpoint authorization: token + elevation (any role adminAccess=true) gate admin operations.
> Gated by `app.security.admin-authz.enabled`. Reuses v0.0.20 `Role.adminAccess`.

## Requirement: Tier A admin endpoints require an elevated caller for all methods

适用于 `/api/organizations`、`/api/user-organizations`、`/api/positions`、`/api/roles`、`/api/user-roles`、
`/api/audit-logs`、`/api/products`（含 `/{id}` 子路径）的所有方法。

### Scenario: 无 token 调用 Tier A 端点被拒

- **GIVEN** `app.security.admin-authz.enabled=true` 且请求不带 `Authorization` 头
- **WHEN** 调用 `POST /api/roles`（或 `GET /api/audit-logs`）
- **THEN** 系统 SHALL 返回 **401**
- **AND** 不 SHALL 执行控制器业务

### Scenario: 非管理员 token 调用 Tier A 端点被拒

- **GIVEN** 用户 bob 所有角色 `adminAccess=false`，持其有效 token
- **WHEN** 调用 `POST /api/roles` 或 `DELETE /api/organizations/1`
- **THEN** 系统 SHALL 返回 **403**

### Scenario: 管理员 token 调用 Tier A 端点放行

- **GIVEN** 用户 alice 有一角色 `adminAccess=true`，持其有效 token
- **WHEN** 调用 `POST /api/roles` 合法 body
- **THEN** 系统 SHALL NOT 因鉴权拒绝（SHALL 返回 2xx 业务结果）

## Requirement: Tier B endpoints gate writes only, leave GET open

适用于 `/api/users`、`/api/features`、`/api/product-modules`（全员页面下拉/面板需读）。

### Scenario: 非管理员可读 Tier B 端点

- **GIVEN** 用户 bob `adminAccess=false`，持有效 token
- **WHEN** `GET /api/users`（或 `/api/features`、`/api/product-modules`）
- **THEN** 系统 SHALL 返回 **200**（读放行）

### Scenario: 非管理员写 Tier B 端点被拒

- **GIVEN** 用户 bob `adminAccess=false`，持有效 token
- **WHEN** `POST /api/users` 或 `PUT /api/features/1`
- **THEN** 系统 SHALL 返回 **403**

## Requirement: all-users endpoints are not gated

适用于 `/api/projects`、`/api/sprints`、`/api/tasks`、`/api/demands`、`/api/requirements`、
`/api/demand-requirements`、`/api/stories`、`/api/milestones`、`/api/sprint-features`、`/api/auth/*`。

### Scenario: 全员端点不受鉴权影响

- **GIVEN** `admin-authz.enabled=true`，非管理员 token（或既有无 token 行为）
- **WHEN** `GET /api/projects`
- **THEN** 系统 SHALL NOT 被 admin 拦截器拒绝（行为与本版前一致）

### Scenario: OPTIONS 预检放行

- **GIVEN** `admin-authz.enabled=true`
- **WHEN** 对 `/api/roles` 发 `OPTIONS` 预检
- **THEN** 系统 SHALL NOT 返回 401/403（放行 CORS 预检）

## Requirement: authorization is flag-gated

### Scenario: 门控关闭时拦截器 no-op

- **GIVEN** `app.security.admin-authz.enabled=false`（test profile 默认）
- **WHEN** 无 token 调用 `POST /api/roles`
- **THEN** 拦截器 SHALL NOT 拒绝（既有控制器测试行为不变）

## Requirement: bootstrap prevents admin lockout

### Scenario: 无 admin 角色时启动提升 bootstrap 角色

- **GIVEN** `admin-authz.enabled=true` 且数据库无任一角色 `adminAccess=true`，但存在 code=`PMO` 的角色
- **WHEN** 应用启动
- **THEN** 系统 SHALL 把该 `PMO` 角色 `adminAccess` 置 true
- **AND** SHALL 输出 INFO 日志说明已引导首个管理员

### Scenario: 已有 admin 角色时启动 no-op

- **GIVEN** 已存在至少一个 `adminAccess=true` 的角色
- **WHEN** 应用启动
- **THEN** bootstrap SHALL NOT 改动任何角色（幂等）
