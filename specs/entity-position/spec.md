# Capability: entity-position

## ADDED Requirements

### Requirement: 创建岗位

后端 SHALL 通过 `POST /api/positions` 接受 `code` + `name` + `category`，其余字段使用默认值，持久化并返回 201。

#### Scenario: 最小 payload 创建岗位 + 默认值

- **GIVEN** backend 已启动，数据库无岗位
- **WHEN** 客户端发起 `POST /api/positions` body `{"code":"BE_ENG","name":"Backend Engineer","category":"TECH"}`
- **THEN** 系统 SHALL 返回 HTTP 201
- **AND** body.id SHALL 为正整数（JSON number 类型）
- **AND** body SHALL 含 `code="BE_ENG"` / `name="Backend Engineer"` / `category="TECH"` / `enabled=true`
- **AND** `Location` header SHALL 形如 `/api/positions/\d+`

#### Scenario: code 重复被拒（service 级唯一）

- **GIVEN** 数据库已存在 `code="BE_ENG"` 岗位
- **WHEN** 再 `POST /api/positions` 同 `code="BE_ENG"`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "code already exists"

#### Scenario: 非法 category 被拒

- **GIVEN** backend 已启动
- **WHEN** `POST /api/positions` body 含 `category="UNKNOWN_CAT"`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "invalid category"

#### Scenario: 必填字段缺失被拒

- **GIVEN** backend 已启动
- **WHEN** `POST /api/positions` body 缺 `name`
- **THEN** SHALL 返回 400
- **AND** body.fieldErrors[*].field SHALL 含 "name"

### Requirement: 查询岗位

后端 SHALL 通过 `GET /api/positions/{id}` 返回单岗位详情；通过 `GET /api/positions?category=&enabled=&search=&page=&size=` 返回 PageResponse。

#### Scenario: 按 id 查询返回完整详情

- **GIVEN** 数据库存在岗位 id=1
- **WHEN** `GET /api/positions/1`
- **THEN** SHALL 返回 200
- **AND** body 字段集 SHALL 等于 [id, code, name, description, category, enabled, createTime, updateTime, createBy, updateBy]

#### Scenario: 按 category 筛选列表

- **GIVEN** 数据库 2 个 TECH + 1 个 PM 岗位
- **WHEN** `GET /api/positions?category=TECH`
- **THEN** body.total SHALL 为 2
- **AND** body.content 全部 `category="TECH"`

### Requirement: 更新岗位

后端 SHALL 通过 `PUT /api/positions/{id}` 修改 `name` / `description` / `category` / `enabled`；不允许改 `code`。

#### Scenario: 更新 name 与 enabled

- **GIVEN** 岗位 id=1，name="Backend Engineer"，enabled=true
- **WHEN** `PUT /api/positions/1` body `{"name":"Senior Backend Engineer","category":"TECH","enabled":false}`
- **THEN** SHALL 返回 200
- **AND** body.name SHALL 为 "Senior Backend Engineer"
- **AND** body.enabled SHALL 为 false

#### Scenario: PUT body 含 code 静默忽略

- **GIVEN** 岗位 id=1，code="BE_ENG"
- **WHEN** `PUT /api/positions/1` body 含 `code="BE_ENG_V2"`
- **THEN** SHALL 返回 200（Jackson 忽略未知字段；DTO 无 code 字段）
- **AND** body.code SHALL 仍为 "BE_ENG"（service 不接受改 code）

### Requirement: 软删岗位（FK 保护）

后端 SHALL 通过 `DELETE /api/positions/{id}` 标记 `del_flag=1`；若有 User 引用该 position_id → 409。

#### Scenario: 无 User 引用软删成功

- **GIVEN** 岗位 id=1，无任何 user.position_id 引用
- **WHEN** `DELETE /api/positions/1`
- **THEN** SHALL 返回 204
- **AND** 后续 `GET /api/positions/1` SHALL 返回 404

#### Scenario: 有 User 引用被拒

- **GIVEN** 岗位 id=1 被 User id=10 引用（position_id=1）
- **WHEN** `DELETE /api/positions/1`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "position has assigned users"
