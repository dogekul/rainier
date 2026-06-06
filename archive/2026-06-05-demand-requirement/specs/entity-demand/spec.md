# Capability: entity-demand

## ADDED Requirements

### Requirement: 创建诉求

后端 SHALL 通过 `POST /api/demands` 接受最小 payload（`title` + `description` + `submitterUserId`），其余字段使用默认值，持久化并返回 201。

#### Scenario: 最小 payload 创建诉求 + 默认值

- **GIVEN** 数据库已存在 `rainier_user` id=1
- **WHEN** 客户端发起 `POST /api/demands` body `{"title":"采购系统反应慢","description":"...","submitterUserId":1}`
- **THEN** 系统 SHALL 返回 HTTP 201
- **AND** body.id SHALL 为正整数（JSON number 类型）
- **AND** body 默认值 SHALL 为 `status="PENDING"` / `priority="MEDIUM"` / `source="WEB"`
- **AND** `Location` header SHALL 形如 `/api/demands/\d+`
- **AND** body SHALL 含 `aiClassification=null` 与 `aiDuplicateHint=null`（AI 字段位预留）

#### Scenario: 必填字段缺失被拒

- **GIVEN** backend 已启动
- **WHEN** `POST /api/demands` body 缺 `title`
- **THEN** SHALL 返回 400
- **AND** body SHALL 含 `message="Validation failed"` 与 `fieldErrors[*].field="title"`

#### Scenario: submitter 不存在被拒

- **GIVEN** 数据库无 id=999_999 的用户
- **WHEN** `POST /api/demands` body 含 `submitterUserId=999999`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "submitter user not found"

#### Scenario: 非法 status 字符串被拒

- **GIVEN** backend 已启动
- **WHEN** `POST /api/demands` body 含 `status="UNKNOWN_STATUS"`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "invalid status"

### Requirement: 查询诉求

后端 SHALL 通过 `GET /api/demands/{id}` 返回单诉求详情；通过 `GET /api/demands?status=&priority=&search=&page=&size=` 返回 PageResponse。

#### Scenario: 按 id 查询返回完整详情

- **GIVEN** 数据库存在诉求 id=1
- **WHEN** `GET /api/demands/1`
- **THEN** SHALL 返回 200
- **AND** body 字段集 SHALL 等于 [id, title, description, submitterUserId, status, priority, source, aiClassification, aiDuplicateHint, closeReason, createTime, updateTime, createBy, updateBy]

#### Scenario: 软删除诉求查询返 404

- **GIVEN** 诉求 id=1 已被软删（`del_flag=1`）
- **WHEN** `GET /api/demands/1`
- **THEN** SHALL 返回 404
- **AND** body SHALL 含 `message`

#### Scenario: 按 status 筛选列表

- **GIVEN** 数据库 2 个 PENDING + 1 个 IN_REVIEW
- **WHEN** `GET /api/demands?status=PENDING`
- **THEN** body.total SHALL 为 2
- **AND** body.content 全部 `status="PENDING"`

### Requirement: 更新诉求

后端 SHALL 通过 `PUT /api/demands/{id}` 修改可写字段（title / description / status / priority / source / closeReason）；AI 字段位 SHALL 不接受客户端输入。

#### Scenario: 更新状态

- **GIVEN** 诉求 id=1，status="PENDING"
- **WHEN** `PUT /api/demands/1` body `{"title":"X","description":"X","status":"IN_REVIEW","priority":"HIGH","source":"WEB"}`
- **THEN** SHALL 返回 200
- **AND** body.status SHALL 为 "IN_REVIEW"
- **AND** body.priority SHALL 为 "HIGH"

#### Scenario: 更新 body 中含 aiClassification 静默忽略

- **GIVEN** 诉求 id=1
- **WHEN** `PUT /api/demands/1` body 含 `"aiClassification":"hack"`
- **THEN** SHALL 返回 200（Jackson 忽略未知字段）
- **AND** body.aiClassification SHALL 仍为更新前的值（service 不接受该字段）

### Requirement: 软删诉求（FK 保护）

后端 SHALL 通过 `DELETE /api/demands/{id}` 标记 `del_flag=1`；若有未删除的 demand_requirement 链接 → 409。

#### Scenario: 无关联软删成功

- **GIVEN** 诉求 id=1，无 demand_requirement 行
- **WHEN** `DELETE /api/demands/1`
- **THEN** SHALL 返回 204
- **AND** 后续 `GET /api/demands/1` SHALL 返回 404

#### Scenario: 有关联软删被拒

- **GIVEN** 诉求 id=1 在 `rainier_demand_requirement` 中有 ≥ 1 行
- **WHEN** `DELETE /api/demands/1`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "demand has linked requirements"
