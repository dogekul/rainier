# Capability: entity-user-role

## ADDED Requirements

### Requirement: 创建用户角色关联

后端 SHALL 通过 `POST /api/user-roles` 接受 `userId` + `roleId` + 可选 `projectId`（占位字段，无存在性校验），硬持久化（无 del_flag 软删语义）；同 `(userId, roleId, projectId)` 重复 → 409。

#### Scenario: 含 projectId 的合法关联创建

- **GIVEN** 数据库存在 user id=1 与 role id=2
- **WHEN** `POST /api/user-roles` body `{"userId":1,"roleId":2,"projectId":42}`
- **THEN** SHALL 返回 HTTP 201
- **AND** body.id SHALL 为正整数
- **AND** body SHALL 含 `userId=1` / `roleId=2` / `projectId=42`

#### Scenario: projectId 为 null 的公司级 hat 创建

- **GIVEN** 数据库存在 user id=1 与 role id=2
- **WHEN** `POST /api/user-roles` body `{"userId":1,"roleId":2,"projectId":null}`
- **THEN** SHALL 返回 201
- **AND** body.projectId SHALL 为 null

#### Scenario: 同 (userId, roleId, projectId) 重复 → 409 (non-NULL)

- **GIVEN** 已存在关联 `(userId=1, roleId=2, projectId=42)`
- **WHEN** 再 POST 同 `(userId=1, roleId=2, projectId=42)`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "user-role already exists"

#### Scenario: 同 (userId, roleId) 公司级重复 → 409 (NULL 兜底)

- **GIVEN** 已存在关联 `(userId=1, roleId=2, projectId=null)`
- **WHEN** 再 POST 同 `(userId=1, roleId=2, projectId=null)`
- **THEN** SHALL 返回 409（service 层 IS NULL 查询补 MySQL UNIQUE 的 NULL 漏洞）
- **AND** body.message SHALL 含 "user-role already exists"

#### Scenario: 公司级 hat 与 项目级 hat 可共存

- **GIVEN** 已存在关联 `(userId=1, roleId=2, projectId=null)`
- **WHEN** POST `(userId=1, roleId=2, projectId=42)`
- **THEN** SHALL 返回 201（不同 projectId 视为不同关联）
- **AND** 后续 `GET /api/user-roles?userId=1&roleId=2` 列表 total SHALL 为 2

#### Scenario: userId 不存在被拒

- **GIVEN** 数据库无 user id=999_999
- **WHEN** `POST /api/user-roles` body `{"userId":999999,"roleId":2}`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "user not found"

#### Scenario: projectId 任意 BIGINT 不校验（占位语义）

- **GIVEN** 数据库无 project 实体（v0.0.7 阶段）
- **WHEN** `POST /api/user-roles` body `{"userId":1,"roleId":2,"projectId":987654321}`
- **THEN** SHALL 返回 201（占位字段位不做存在性校验）
- **AND** body.projectId SHALL 为 987654321

### Requirement: 查询用户角色关联（含富化）

后端 SHALL 通过 `GET /api/user-roles?userId=&roleId=&projectId=&page=&size=` 返回 PageResponse；返回每行 SHALL 富化 `userName` / `userLoginName` / `roleName` / `roleCode`。

#### Scenario: 列表富化用户与角色字段

- **GIVEN** 存在 user (id=1, name="Alice", loginName="alice") 与 role (id=2, name="PMO", code="PMO")，关联 (userId=1, roleId=2, projectId=null)
- **WHEN** `GET /api/user-roles?userId=1`
- **THEN** body.content[0] SHALL 含 `userName="Alice"` / `userLoginName="alice"` / `roleName="PMO"` / `roleCode="PMO"`
- **AND** body.content[0] SHALL 同时含 `userId=1` / `roleId=2` / `projectId=null`

### Requirement: 硬删用户角色关联

后端 SHALL 通过 `DELETE /api/user-roles/{id}` 物理删除（无 del_flag）；删除后 `GET` 返 404。

#### Scenario: 硬删成功

- **GIVEN** 关联 id=1
- **WHEN** `DELETE /api/user-roles/1`
- **THEN** SHALL 返回 204
- **AND** 后续 `GET /api/user-roles/1` SHALL 返回 404
- **AND** DB `SELECT COUNT(*) FROM rainier_user_role WHERE id=1` SHALL 为 0
