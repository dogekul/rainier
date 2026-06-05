# Capability: entity-user

## ADDED Requirements

### Requirement: 创建用户

后端 SHALL 接受 `POST /api/users`，含 `login_name` / `name` / 可选 `code` / 可选 `email_address` / `is_internal`(默认 true) / `enabled`(默认 true)。

#### Scenario: 最小 payload 创建成功

- **GIVEN** backend 已启动
- **WHEN** `POST /api/users` body `{"loginName":"alice","name":"Alice"}`
- **THEN** SHALL 返回 201
- **AND** body SHALL 含 id（UUID hex）、loginName="alice"、name="Alice"、code=null、emailAddress=null、isInternal=true、enabled=true、delFlag=false、createTime
- **AND** Location header SHALL 为 `/api/users/{id}`

#### Scenario: login_name 唯一性冲突

- **GIVEN** 已存在用户 `loginName="alice"`（未软删）
- **WHEN** `POST /api/users` body `{"loginName":"alice","name":"Other Alice"}`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "loginName already exists"

#### Scenario: code 唯一性冲突

- **GIVEN** 已存在用户 `code="E1001"`（未软删）
- **WHEN** `POST /api/users` body `{"loginName":"bob","name":"Bob","code":"E1001"}`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "code already exists"

#### Scenario: email 格式非法

- **GIVEN** backend 已启动
- **WHEN** `POST /api/users` body `{"loginName":"x","name":"X","emailAddress":"not-email"}`
- **THEN** SHALL 返回 400
- **AND** body.fieldErrors[*].field SHALL 含 "emailAddress"

### Requirement: 查询用户详情

后端 SHALL 通过 `GET /api/users/{id}` 返回单用户信息或 404。

#### Scenario: id 存在

- **GIVEN** 数据库存在用户 id=`u1`（未软删）
- **WHEN** `GET /api/users/u1`
- **THEN** SHALL 返回 200
- **AND** body SHALL 含 id、loginName、name、code、emailAddress、isInternal、enabled、delFlag=false、createTime、updateTime

#### Scenario: id 已软删

- **GIVEN** 用户 id=`u1` 的 `del_flag=1`
- **WHEN** `GET /api/users/u1`
- **THEN** SHALL 返回 404

### Requirement: 用户列表分页搜索

后端 SHALL 通过 `GET /api/users?search=&isInternal=&enabled=&page=&size=` 返回 PageResponse。

#### Scenario: search 跨字段匹配

- **GIVEN** 数据库存在 loginName="alice"、name="Alice Wong"、code="E1001"、emailAddress="alice@x.com"，与 loginName="bob"、code="E2001"
- **WHEN** `GET /api/users?search=alice`
- **THEN** body.total SHALL 为 1
- **AND** body.content[0].loginName SHALL 为 "alice"

#### Scenario: isInternal=false 筛选

- **GIVEN** 数据库 3 内部 + 2 外部用户
- **WHEN** `GET /api/users?isInternal=false`
- **THEN** body.total SHALL 为 2
- **AND** body.content 全部 isInternal=false

### Requirement: 更新用户

后端 SHALL 通过 `PUT /api/users/{id}` 修改 name / code / email_address / is_internal / enabled。login_name 一旦创建不可改。

#### Scenario: 修改 name + enabled

- **GIVEN** 用户 id=`u1`，原 name="Alice"、enabled=true
- **WHEN** `PUT /api/users/u1` body `{"name":"Alice Wang","enabled":false}`
- **THEN** SHALL 返回 200
- **AND** body.name SHALL 为 "Alice Wang"
- **AND** body.enabled SHALL 为 false
- **AND** body.loginName SHALL 不变
- **AND** body.updateTime SHALL 晚于 createTime

### Requirement: 软删用户（FK 保护）

后端 SHALL 通过 `DELETE /api/users/{id}` 标记 `del_flag=1`；若用户在 user_organization 中有 `left_at IS NULL` 行 → 409。

#### Scenario: 无组织归属 → 软删成功

- **GIVEN** 用户 id=`u1`，user_organization 中无 left_at=null 行
- **WHEN** `DELETE /api/users/u1`
- **THEN** SHALL 返回 204
- **AND** 后续 `GET /api/users/u1` SHALL 返回 404

#### Scenario: 有在岗组织归属 → 拒绝软删

- **GIVEN** 用户 id=`u1` 在 user_organization 表中有 ≥1 行 `user_id=u1 AND left_at IS NULL`
- **WHEN** `DELETE /api/users/u1`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "has active organization assignments"



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
