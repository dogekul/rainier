# Capability: entity-user-organization

## ADDED Requirements

### Requirement: 创建用户↔组织归属

后端 SHALL 接受 `POST /api/user-organizations`，含 `user_id` / `organization_id` / `role`(默认 MEMBER) / `is_primary`(默认 false) / `joined_at`。

#### Scenario: 合法 payload 创建归属

- **GIVEN** user id=`u1` 与 organization id=`org1`（type=TEAM）存在
- **WHEN** `POST /api/user-organizations` body `{"userId":"u1","organizationId":"org1","role":"MEMBER","isPrimary":true}`
- **THEN** SHALL 返回 201
- **AND** body SHALL 含 id、userId、organizationId、role="MEMBER"、isPrimary=true、joinedAt、leftAt=null
- **AND** Location header SHALL 为 `/api/user-organizations/{id}`

#### Scenario: 同 (user_id, organization_id) 重复

- **GIVEN** 已存在归属 `user_id="u1", organization_id="org1"`（left_at IS NULL）
- **WHEN** 再 POST 同 user + 同 org
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "user already assigned"

#### Scenario: user 或 org 不存在

- **GIVEN** 数据库无 user id="ghost"
- **WHEN** POST body `{"userId":"ghost","organizationId":"org1"}`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "user not found"

#### Scenario: 设新 primary 自动 demote 旧 primary

- **GIVEN** 用户 u1 已有 1 行 user_organization `is_primary=true`（organizationId=A）
- **WHEN** `POST /api/user-organizations` body `{"userId":"u1","organizationId":"B","isPrimary":true}`
- **THEN** SHALL 返回 201
- **AND** 新创建行 isPrimary=true
- **AND** 旧 A 行 SHALL 被 UPDATE 为 isPrimary=false
- **AND** DB 查询 `WHERE user_id='u1' AND is_primary=true` SHALL 仅 1 行

### Requirement: 查询归属列表

后端 SHALL 通过 `GET /api/user-organizations?userId=&organizationId=&role=&page=&size=` 返回 PageResponse。

#### Scenario: 按 user_id 筛选

- **GIVEN** user u1 关联 3 个 org，其他 user 关联 5 个
- **WHEN** `GET /api/user-organizations?userId=u1`
- **THEN** body.total SHALL 为 3
- **AND** body.content 全部 userId=u1

#### Scenario: 按 organization_id 筛选

- **GIVEN** org "team1" 关联 4 个 user，其他 org 关联 6 个
- **WHEN** `GET /api/user-organizations?organizationId=team1`
- **THEN** body.total SHALL 为 4

#### Scenario: 列表 enrichment

- **GIVEN** 1 行 user_organization：userId=u1, organizationId=org1, role=HEAD
- **WHEN** `GET /api/user-organizations?userId=u1`
- **THEN** 返回项 SHALL 含 `user.loginName`、`user.name`、`organization.name`、`organization.type`（避免前端二次查询）

### Requirement: 更新归属

后端 SHALL 通过 `PUT /api/user-organizations/{id}` 修改 role / is_primary / left_at。user_id / organization_id 不可改（要变就删旧建新）。

#### Scenario: 设 left_at 表示离职

- **GIVEN** 归属 id=`uo1`，left_at=null
- **WHEN** `PUT /api/user-organizations/uo1` body `{"leftAt":"2026-12-31T23:59:59Z"}`
- **THEN** SHALL 返回 200
- **AND** body.leftAt SHALL 为 "2026-12-31T23:59:59Z"
- **AND** 后续查询 `GET /api/user-organizations?userId=...&active=true` 不返回此行

#### Scenario: role MEMBER → HEAD

- **GIVEN** 归属 id=`uo1`，role=MEMBER
- **WHEN** `PUT /api/user-organizations/uo1` body `{"role":"HEAD"}`
- **THEN** SHALL 返回 200
- **AND** body.role SHALL 为 "HEAD"

### Requirement: 删除归属

后端 SHALL 通过 `DELETE /api/user-organizations/{id}` 硬删该行（不软删 —— 归属记录的"软删"语义由 `left_at` 已覆盖）。

#### Scenario: 删除归属

- **GIVEN** 归属 id=`uo1`
- **WHEN** `DELETE /api/user-organizations/uo1`
- **THEN** SHALL 返回 204
- **AND** DB 中该行 SHALL 物理消失
- **AND** 后续 `GET /api/user-organizations/uo1` SHALL 返回 404
