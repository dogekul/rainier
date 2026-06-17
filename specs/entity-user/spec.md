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



## MODIFIED Requirements (from change 2026-06-17-real-auth / v0.0.38)

### Requirement: User 密码哈希字段位

后端 SHALL 在 `rainier_user` 表加 `password_hash VARCHAR(100) NULL` 列，存储 BCrypt 哈希（不存明文，响应中
SHALL 不序列化该字段）。`POST /api/users` 接受可选 `password`（≤100 字符）：非空 → 以 BCrypt 编码后写入；空 →
落 `app.security.default-password`（默认 `rainier123`）的 BCrypt 哈希。启动时（`app.security.real-auth.enabled=true`）
`RealAuthPasswordBackfill` SHALL 给所有 `password_hash` 为空的用户回填默认密码哈希，幂等。

#### Scenario: 创建用户不带 password 落默认密码哈希

- **GIVEN** `app.security.default-password=rainier123`
- **WHEN** `POST /api/users` body `{"loginName":"alice","name":"Alice"}`（无 password）
- **THEN** SHALL 返回 201
- **AND** 该用户 `password_hash` SHALL 非空且为 BCrypt（`$2a$` 前缀）
- **AND** `password_hash` SHALL `matches("rainier123")`
- **AND** 响应 body SHALL 不含 `password` 或 `passwordHash` 字段

#### Scenario: 启动回填给无密码用户设默认密码

- **GIVEN** `app.security.real-auth.enabled=true`，存在 `password_hash` 为空的存量用户 legacy
- **WHEN** `RealAuthPasswordBackfill` 在启动时运行
- **THEN** legacy 的 `password_hash` SHALL 变为非空 BCrypt
- **AND** 其 `password_hash` SHALL `matches("rainier123")`
- **AND** 再次运行回填 SHALL 不改动已有非空哈希（幂等）

### Requirement: login_name 唯一性为 app 层（软删兼容）

后端 SHALL 在 service 层经 `existsByLoginName` 保证活跃用户 `login_name` 唯一，重名创建返回 409。**设计调整**：
不在 `rainier_user.login_name` 加 DB unique 约束 —— `@SQLDelete` 软删模型下普通 unique 索引仍可见
`del_flag=1` 行，会错误地阻止「软删后重建同名登录」并破坏 `deleteAll` 重播。`@Where(del_flag=0)` 使
`existsByLoginName`/`findByLoginName` 只见活跃行，故 app 层唯一性无歧义。

#### Scenario: 软删后可重建同名登录

- **GIVEN** 用户 `loginName="alice"` 已被软删（`del_flag=1`）
- **WHEN** `POST /api/users` body `{"loginName":"alice","name":"New Alice"}`
- **THEN** SHALL 返回 201（不被软删行的残留索引阻塞）
- **AND** `findByLoginName("alice")` SHALL 解析到新建的活跃用户
