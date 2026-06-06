# Capability: entity-requirement

## ADDED Requirements

### Requirement: 创建需求

后端 SHALL 通过 `POST /api/requirements` 接受 `code` + `title` + `description` + `ownerUserId`，其余字段使用默认值，持久化并返回 201。

#### Scenario: 最小 payload 创建需求 + 默认值

- **GIVEN** 数据库存在 `rainier_user` id=1
- **WHEN** `POST /api/requirements` body `{"code":"REQ-001","title":"加速采购下单","description":"...","ownerUserId":1}`
- **THEN** SHALL 返回 201
- **AND** body.id SHALL 为正整数
- **AND** body 默认值 SHALL 为 `status="DRAFT"` / `priority="MEDIUM"` / `complexity=null` / `projectId=null`
- **AND** `Location` header SHALL 形如 `/api/requirements/\d+`

#### Scenario: code 全局唯一性冲突

- **GIVEN** 数据库已存在 `code="REQ-001"` 需求
- **WHEN** 再 `POST /api/requirements` 同 `code="REQ-001"`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "code already exists"

#### Scenario: ownerUser 不存在被拒

- **GIVEN** 数据库无 id=999_999 的用户
- **WHEN** `POST /api/requirements` body 含 `ownerUserId=999999`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "owner user not found"

### Requirement: 查询需求

后端 SHALL 通过 `GET /api/requirements/{id}` 返回单需求详情；通过 `GET /api/requirements?status=&priority=&projectId=&search=&page=&size=` 返回 PageResponse。

#### Scenario: 按 id 查询返回完整详情

- **GIVEN** 数据库存在需求 id=1
- **WHEN** `GET /api/requirements/1`
- **THEN** SHALL 返回 200
- **AND** body 字段集 SHALL 等于 [id, code, title, description, ownerUserId, status, priority, complexity, projectId, closeReason, createTime, updateTime, createBy, updateBy]

#### Scenario: 按 projectId 筛选（占位字段查询）

- **GIVEN** 数据库 1 个需求 projectId=null，1 个 projectId=42（手动 update 测试数据）
- **WHEN** `GET /api/requirements?projectId=42`
- **THEN** body.total SHALL 为 1
- **AND** body.content[0].projectId SHALL 为 42

### Requirement: 更新需求

后端 SHALL 通过 `PUT /api/requirements/{id}` 修改 code / title / description / status / priority / complexity / projectId / closeReason；code 变更须重检唯一性；ownerUserId 不可修改。

#### Scenario: 更新状态

- **GIVEN** 需求 id=1，status="DRAFT"
- **WHEN** `PUT /api/requirements/1` body `{"code":"REQ-001","title":"X","description":"X","status":"APPROVED","priority":"HIGH"}`
- **THEN** SHALL 返回 200
- **AND** body.status SHALL 为 "APPROVED"

#### Scenario: 更新 body 含 ownerUserId 静默忽略

- **GIVEN** 需求 id=1，ownerUserId=1
- **WHEN** `PUT /api/requirements/1` body 含 `ownerUserId=2`
- **THEN** SHALL 返回 200
- **AND** body.ownerUserId SHALL 仍为 1（service 不接受该字段变更）

### Requirement: 软删需求（FK 保护）

后端 SHALL 通过 `DELETE /api/requirements/{id}` 标记 `del_flag=1`；若有未删除的 demand_requirement 链接 → 409。

#### Scenario: 无关联软删成功

- **GIVEN** 需求 id=1，无 demand_requirement 行
- **WHEN** `DELETE /api/requirements/1`
- **THEN** SHALL 返回 204
- **AND** 后续 `GET /api/requirements/1` SHALL 返回 404

#### Scenario: 有关联软删被拒

- **GIVEN** 需求 id=1 在 `rainier_demand_requirement` 中有 ≥ 1 行
- **WHEN** `DELETE /api/requirements/1`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "requirement has linked demands"
