# Capability: entity-user

## MODIFIED Requirements (from change 2026-06-05-v1-id-migration)

### Requirement: User id 为数字

后端 SHALL 在 `POST /api/users` 返回 body 中 `id` 为 JSON 数字。

#### Scenario: 新建用户返回数字 id

- **GIVEN** 数据库为空
- **WHEN** 客户端发起 `POST /api/users` body `{"loginName":"alice","name":"A"}`
- **THEN** 系统 SHALL 返回 HTTP 201
- **AND** body.id SHALL 为正整数（JSON number）
- **AND** `Location` header SHALL 形如 `/api/users/\d+`
- **AND** body.id SHALL 不匹配 32 字符 hex 正则
