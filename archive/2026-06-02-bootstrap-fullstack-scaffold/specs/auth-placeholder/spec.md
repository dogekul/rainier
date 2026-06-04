# Capability: auth-placeholder

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
