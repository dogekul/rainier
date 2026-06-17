# Capability: auth-placeholder

> Change log:
> - 2026-06-12 (v0.0.18-workbench) — `GET /api/auth/me` expanded from `{username}` to the full
>   current-user context `{id, username, name, roles[], projects[]}` (resolves the token subject to a
>   User, joins UserRole + Role + Project; degrades to id=null / empty arrays when the subject has no
>   matching User). The "携带有效 token 查询成功" scenario's `username` assertion still holds (subset).
> - 2026-06-15 (v0.0.20-role-nav) — each `roles[]` element gains `adminAccess` (reads
>   `Role.adminAccess`, coalesced NULL→false; never null in the response). Drives the frontend
>   `isElevated` role-scoped navigation + admin route guards.
> - 2026-06-17 (v0.0.24-me-self-scope) — `SecurityFilter` now resolves the Bearer token →
>   `rainier.username` request attribute for `/api/me/*` too (not just `/api/auth/me`), so the new
>   self-scoped endpoints (`/api/me/led-teams`, `/api/me/team-members`, `/api/me/portfolio`) get
>   identity; no token → controller 401. See [[entity-user-organization]] / [[entity-portfolio]].
> - 2026-06-17 (v0.0.27-auth-baseline-polish) — `SecurityFilter` resolves identity for ANY path and,
>   when `app.security.require-all-users-token.enabled=true`, gates EVERY `/api/**`: missing/invalid
>   token → uniform JSON 401 before any controller (whitelist: `POST /api/auth/login` + `GET
>   /api/health`; matrix-param safe via UrlPathHelper). Runs BEFORE `AdminAuthorizationInterceptor`,
>   so 401 (identity) precedes 403 (authz). Flag true in prod, false in the test profile. See
>   [[backend-authz]].
> - 2026-06-17 (v0.0.38-real-auth) — `POST /api/auth/login` gains REAL BCrypt credential verification
>   (flag `app.security.real-auth.enabled`, true prod / false test). When on: resolves the login via
>   `findByLoginName`, requires `enabled=true` + a non-null `password_hash`, and `PasswordEncoder.matches`
>   the supplied password; unknown user / disabled / wrong password all → uniform **401** ("invalid
>   username or password", no leak of which login names exist). When off: legacy mock issuer (any
>   non-blank creds) — so the test profile and all legacy login tests stay green. Closes the CRITICAL
>   self-asserted-identity / impersonation hole that previously let any caller mint a token for any
>   subject. Password storage + backfill live in [[entity-user]]. `SecurityPostureWarning` loudly WARNs
>   on startup when the posture is the insecure dev default (real-auth off / shared default password /
>   dev JWT secret) — fails OPEN with a loud signal, not silently.

## ADDED Requirements

### Requirement: 用户登录（mock JWT）

后端 SHALL 接受任意非空 `username` 和 `password`，返回 mock JWT 与用户信息。
注：本能力为占位实现，不接真实身份源；secret 与算法见 `design.md §4`。

#### Scenario: 凭证非空时登录成功

- **GIVEN** 后端处于运行状态
- **WHEN** 客户端发起 `POST /api/auth/login`，body 为 `{"username":"alice","password":"any"}`
- **THEN** 系统 SHALL 返回 HTTP 200
- **AND** 响应 body SHALL 包含字段 `token`（非空字符串、HS256 JWT 三段式）
- **AND** 响应 body SHALL 包含 `user.username` 等于 `"alice"`
- **AND** 返回的 JWT `sub` claim SHALL 等于 `"alice"`，`exp` SHALL 为签发时间 + 24h

#### Scenario: 缺少必填字段时拒绝

- **GIVEN** 后端处于运行状态
- **WHEN** 客户端发起 `POST /api/auth/login` 且 body 中 `username` 为空字符串或缺失
- **THEN** 系统 SHALL 返回 HTTP 400
- **AND** 响应 body SHALL 为 JSON 且包含字段 `message`

### Requirement: 凭 token 查询当前用户

后端 SHALL 在 `GET /api/auth/me` 接口验证 `Authorization: Bearer <token>`，有效则返回当前用户信息。

#### Scenario: 携带有效 token 查询成功

- **GIVEN** 客户端已通过 `POST /api/auth/login` 取得有效 token `T`，用户名为 `alice`
- **WHEN** 客户端发起 `GET /api/auth/me`，请求头携带 `Authorization: Bearer T`
- **THEN** 系统 SHALL 返回 HTTP 200
- **AND** 响应 body SHALL 包含 `username` 等于 `"alice"`

#### Scenario: 缺失或非法 token 时拒绝

- **GIVEN** 后端处于运行状态
- **WHEN** 客户端发起 `GET /api/auth/me`，未携带 `Authorization` 头或携带非法 token（格式错误 / 已过期 / 签名不匹配）
- **THEN** 系统 SHALL 返回 HTTP 401
- **AND** 响应 body SHALL 为 JSON 且包含字段 `message`
- **AND** 响应 SHALL 不包含任何 stack trace 或服务器内部细节

## ADDED Requirements (from change 2026-06-12-workbench / v0.0.18)

### Requirement: me 返回当前用户上下文

后端 SHALL 在 `GET /api/auth/me` 由 token 的 username 解析当前 User，返回 `{id, username, name, roles[], projects[]}`；roles 来自该用户的 user-role 行（含 roleCode/roleName + projectId/projectName/projectCode），projects 为其参与项目的去重列表。

#### Scenario: 已知用户返回 id/name/roles/projects

- **GIVEN** 用户 alice（id=U，name="Alice"）有 user-role（role code="PMO"，projectId=P，project code="PRJ-1"）
- **WHEN** 客户端携 alice 的 token `GET /api/auth/me`
- **THEN** 系统 SHALL 返回 200
- **AND** body SHALL 含 `id=U` / `username="alice"` / `name="Alice"`
- **AND** body.roles SHALL 含一项 `roleCode="PMO"` 且 `projectId=P`
- **AND** body.projects SHALL 含 `{id:P, code:"PRJ-1"}`

#### Scenario: 组织级角色（projectId 为 null）也返回

- **GIVEN** 用户 alice 有一条 user-role `projectId=null`（组织级角色）
- **WHEN** `GET /api/auth/me`
- **THEN** body.roles SHALL 含该角色且其 `projectId` 为 null
- **AND** body.projects SHALL 不因该 null 行新增项目

#### Scenario: username 无对应 User 时降级

- **GIVEN** token 的 sub 为 "system"，数据库无 loginName="system" 的 User
- **WHEN** `GET /api/auth/me`
- **THEN** 系统 SHALL 返回 200
- **AND** body SHALL 含 `username="system"` / `id=null` / `roles=[]` / `projects=[]`

### Requirement: me() 角色暴露 adminAccess (v0.0.20)

`GET /api/auth/me` 的每个 `roles[]` 元素 SHALL 含 `adminAccess`（读 `Role.adminAccess` 兜底 NULL→false，响应中永不为 null）。

#### Scenario: adminAccess=true 角色在 me() 中可见

- **GIVEN** 用户被指派一个 `adminAccess` 为 true 的角色
- **WHEN** 携该用户 token `GET /api/auth/me`
- **THEN** 对应 `roles[].adminAccess` SHALL 为 `true`

#### Scenario: adminAccess=false 或 NULL 角色读为 false

- **GIVEN** 用户被指派一个 `adminAccess` 为 false 或 legacy NULL 的角色
- **WHEN** `GET /api/auth/me`
- **THEN** 对应 `roles[].adminAccess` SHALL 为 `false`
- **AND** 该字段 SHALL 永不为 null

## MODIFIED Requirements (from change 2026-06-17-real-auth / v0.0.38)

### Requirement: 用户登录（真实凭证校验，flag-gated）

当 `app.security.real-auth.enabled=true` 时，后端 SHALL 对 `POST /api/auth/login` 做真实凭证校验：按
`login_name` 解析用户，要求 `enabled=true` 且 `password_hash` 非空，并以 BCrypt `matches` 校验密码；任一不满足
SHALL 返回 401，且对「未知用户 / 已禁用 / 密码错误」返回**相同**的错误消息（不泄露登录名是否存在）。`username`/
`password` 非空校验（400）先于凭证校验。当 flag 关闭时保留 mock 行为（任意非空凭证发 token），用于测试 profile。
本需求取代「用户登录（mock JWT）」中「接受任意非空 username 和 password」在 flag-on 下的语义。

#### Scenario: flag 开启 + 正确密码登录成功

- **GIVEN** `app.security.real-auth.enabled=true`，存在 `enabled=true` 用户 alice，其 `password_hash` 为 `s3cret` 的 BCrypt
- **WHEN** 客户端 `POST /api/auth/login` body `{"username":"alice","password":"s3cret"}`
- **THEN** 系统 SHALL 返回 HTTP 200
- **AND** 响应 body SHALL 含非空 `token` 与 `user.username="alice"`

#### Scenario: flag 开启 + 错误密码拒绝

- **GIVEN** `app.security.real-auth.enabled=true`，存在用户 alice（`password_hash` 为 `s3cret`）
- **WHEN** 客户端 `POST /api/auth/login` body `{"username":"alice","password":"nope"}`
- **THEN** 系统 SHALL 返回 HTTP 401

#### Scenario: flag 开启 + 未知用户拒绝（不泄露存在性）

- **GIVEN** `app.security.real-auth.enabled=true`，数据库无 `login_name="ghost"` 的活跃用户
- **WHEN** 客户端 `POST /api/auth/login` body `{"username":"ghost","password":"whatever"}`
- **THEN** 系统 SHALL 返回 HTTP 401
- **AND** 错误消息 SHALL 与「错误密码」场景一致（不区分用户是否存在）

#### Scenario: flag 开启 + 禁用用户即使密码正确也拒绝

- **GIVEN** `app.security.real-auth.enabled=true`，用户 bob `enabled=false`，`password_hash` 为 `s3cret`
- **WHEN** 客户端 `POST /api/auth/login` body `{"username":"bob","password":"s3cret"}`
- **THEN** 系统 SHALL 返回 HTTP 401
