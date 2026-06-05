# Capability: entity-user-organization

## MODIFIED Requirements (from change 2026-06-05-v1-id-migration)

### Requirement: UserOrganization id / userId / organizationId 为数字

后端 SHALL 接受 `POST /api/user-organizations` 请求体中 `userId` / `organizationId` 为 JSON 数字，并以数字形式返回 `id` / `userId` / `organizationId`。

#### Scenario: 请求体接收数字 FK

- **GIVEN** 已存在用户 id=U（数字）与组织 id=O（数字）
- **WHEN** 客户端发起 `POST /api/user-organizations` body `{"userId":U,"organizationId":O}`
- **THEN** 系统 SHALL 返回 HTTP 201
- **AND** body.userId SHALL 等于 U（数字）
- **AND** body.organizationId SHALL 等于 O（数字）
- **AND** body.id SHALL 为正整数

### Requirement: FK 不存在错误 message 含数字 id

后端 SHALL 在 FK 校验失败时把传入的数字 id 直接拼到错误消息里。

#### Scenario: organizationId 不存在

- **GIVEN** 数据库无 organization id=999999
- **WHEN** 客户端发起 `POST /api/user-organizations` body `{"userId":1,"organizationId":999999}`
- **THEN** 系统 SHALL 返回 HTTP 400
- **AND** body.message SHALL 包含字符串 `"999999"`
- **AND** body.message SHALL 不包含 32 字符 hex 占位
