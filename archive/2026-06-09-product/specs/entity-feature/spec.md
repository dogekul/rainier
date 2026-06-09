# Capability: entity-feature

> NEW capability from v0.0.12-product (2026-06-09).
> Feature 属一个 ProductModule（NN FK）。3-state machine (PLANNING/ACTIVE/DEPRECATED).
> moduleId 创建后 immutable.
> 无下游 FK 保护（叶子实体）；可直接软删。

## ADDED Requirements

### Requirement: 创建功能

后端 SHALL 通过 `POST /api/features` 接受 `code` + `name` + `moduleId` + `ownerUserId`（必填）；持久化并返回 201。

#### Scenario: 最小 payload + moduleName 富化

- **GIVEN** Module id=1 (code=MOD-WALLET, name="钱包") + User id=1 ("Alice")
- **WHEN** `POST /api/features` body `{"code":"FEAT-RECHARGE","name":"充值","moduleId":1,"ownerUserId":1}`
- **THEN** SHALL 返回 201
- **AND** body.status SHALL 为 "PLANNING"
- **AND** body SHALL 富化 moduleCode="MOD-WALLET" / moduleName="钱包" / ownerName="Alice"

#### Scenario: moduleId 不存在 → 400

- **GIVEN** 数据库无 Module id=999
- **WHEN** POST body 含 moduleId=999
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "module not found"

#### Scenario: code 重复 → 409

- **WHEN** 重复 POST 同 code
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "code already exists"

#### Scenario: 非法 status → 400

- **WHEN** POST body 含 status="UNKNOWN"
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "invalid status"

#### Scenario: 缺必填字段 → 400

- **WHEN** POST body `{"code":"FEAT-X"}`（缺 name/moduleId/ownerUserId）
- **THEN** SHALL 返回 400
- **AND** body.fieldErrors[*].field SHALL 同时含 `"name"` / `"moduleId"` / `"ownerUserId"`

### Requirement: 查询功能

后端 SHALL 通过 `GET /api/features/{id}` 返回单 Feature 详情；通过 `GET /api/features?moduleId=&status=&search=&page=&size=` 返回 PageResponse。

#### Scenario: GET 详情完整字段集

- **GIVEN** Feature id=1 关联 Module 1 + User 1
- **WHEN** `GET /api/features/1`
- **THEN** SHALL 返回 200
- **AND** body 字段集 SHALL 含 `[id, code, name, description, status, moduleId, moduleCode, moduleName, ownerUserId, ownerName, ownerLoginName, createTime, updateTime, createBy, updateBy]`

#### Scenario: 按 moduleId 过滤列表

- **GIVEN** Module A 下 2 Feature, Module B 下 1 Feature
- **WHEN** `GET /api/features?moduleId={A.id}`
- **THEN** body.total SHALL 为 2
- **AND** body.content[*].moduleId SHALL 全为 A.id

### Requirement: 更新功能（含 owner 可改；moduleId 不可改）

后端 SHALL 通过 `PUT /api/features/{id}` 修改 code / name / description / status / ownerUserId；moduleId 不可改。

#### Scenario: 更新 status + owner 转移

- **GIVEN** Feature id=1 status=PLANNING; User id=2 存在
- **WHEN** PUT body 含 status="ACTIVE", ownerUserId=2
- **THEN** SHALL 返回 200
- **AND** body.status SHALL 为 "ACTIVE"
- **AND** body.ownerName SHALL 为 User id=2 的 name

### Requirement: 软删功能

后端 SHALL 通过 `DELETE /api/features/{id}` 标记 `del_flag=1`，无下游 FK 保护（Feature 是叶子）。

#### Scenario: 软删成功 + 后续 GET 404

- **GIVEN** Feature id=1 存在
- **WHEN** `DELETE /api/features/1`
- **THEN** SHALL 返回 204
- **AND** 后续 `GET /api/features/1` SHALL 返回 404
