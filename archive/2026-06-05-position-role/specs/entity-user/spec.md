# Capability: entity-user

## MODIFIED Requirements (from change 2026-06-05-position-role)

### Requirement: User 关联岗位字段位

后端 SHALL 在 `rainier_user` 表加 `position_id BIGINT NULL` 列（FK rainier_position(id)，可空 = 未定级）；`POST` / `PUT /api/users/{id}` 接受 `positionId` 字段（可空 → null；非空 → 校验 Position 存在）；`GET` 响应 body 富化 `positionName` / `positionCategory`。

#### Scenario: POST 含 positionId 创建用户

- **GIVEN** 数据库存在岗位 id=1（name="Backend Engineer"，category="TECH"）
- **WHEN** `POST /api/users` body `{"loginName":"alice","name":"Alice","positionId":1}`
- **THEN** SHALL 返回 HTTP 201
- **AND** body.positionId SHALL 为 1
- **AND** body.positionName SHALL 为 "Backend Engineer"（service 富化）
- **AND** body.positionCategory SHALL 为 "TECH"

#### Scenario: POST positionId 不存在被拒

- **GIVEN** 数据库无岗位 id=999_999
- **WHEN** `POST /api/users` body `{"loginName":"bob","name":"Bob","positionId":999999}`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "position not found"

#### Scenario: PUT 更新 positionId

- **GIVEN** 用户 id=1 当前 positionId=1；岗位 id=2 存在
- **WHEN** `PUT /api/users/1` body 含 `positionId=2`
- **THEN** SHALL 返回 200
- **AND** body.positionId SHALL 为 2
- **AND** body.positionName SHALL 为新岗位的 name

#### Scenario: PUT positionId 设为 null 清空岗位

- **GIVEN** 用户 id=1 当前 positionId=1
- **WHEN** `PUT /api/users/1` body 含 `positionId=null`
- **THEN** SHALL 返回 200
- **AND** body.positionId SHALL 为 null
- **AND** body.positionName SHALL 为 null
- **AND** body.positionCategory SHALL 为 null
