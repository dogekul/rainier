# Capability: entity-story

## ADDED Requirements

### Requirement: 创建 Story

后端 SHALL 通过 `POST /api/stories` 接受 `code` + `title` + `requirementId` + `ownerUserId`（必填），其余字段使用默认值；Service SHALL 自动从父 Requirement 复制 `projectId` 填入；持久化并返回 201。

#### Scenario: 最小 payload 创建 Story + 默认值 + 富化 + projectId 继承

- **GIVEN** 数据库存在 Project id=1 / Requirement id=1（owner=alice，projectId=1）/ User id=1（loginName="alice"，name="Alice"）
- **WHEN** 客户端 `POST /api/stories` body `{"code":"STR-001","title":"用户登录页","requirementId":1,"ownerUserId":1}`
- **THEN** 系统 SHALL 返回 HTTP 201
- **AND** body 默认值 SHALL 为 `status="DRAFT"` / `priority="MEDIUM"` / `complexity=null`
- **AND** body.projectId SHALL 为 1（从 Requirement 自动继承）
- **AND** body SHALL 富化 `ownerName="Alice"` / `ownerLoginName="alice"` / `requirementCode` / `requirementTitle` / `projectName` / `projectCode`

#### Scenario: code 重复被拒（service 级唯一）

- **GIVEN** 数据库已存在 `code="STR-001"` Story
- **WHEN** 再 `POST /api/stories` 同 `code="STR-001"`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "code already exists"

#### Scenario: requirementId 不存在被拒

- **GIVEN** 数据库无 Requirement id=999
- **WHEN** `POST /api/stories` body 含 `requirementId=999`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "requirement not found"

#### Scenario: ownerUserId 不存在被拒

- **GIVEN** 数据库无 User id=999_999
- **WHEN** `POST /api/stories` body 含 `ownerUserId=999999`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "owner user not found"

#### Scenario: 非法 status 被拒

- **GIVEN** 后端已启动
- **WHEN** `POST /api/stories` body 含 `status="UNKNOWN"`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "invalid status"

#### Scenario: 非法 priority 被拒

- **GIVEN** 后端已启动
- **WHEN** `POST /api/stories` body 含 `priority="EXTREME"`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "invalid priority"

#### Scenario: 非法 complexity 被拒

- **GIVEN** 后端已启动
- **WHEN** `POST /api/stories` body 含 `complexity="XXL"`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "invalid complexity"

#### Scenario: 缺必填字段被拒

- **GIVEN** 后端已启动
- **WHEN** `POST /api/stories` body `{"code":"STR-X"}`（缺 title / requirementId / ownerUserId）
- **THEN** SHALL 返回 400
- **AND** body.fieldErrors[*].field SHALL 同时含 `"title"` / `"requirementId"` / `"ownerUserId"`

#### Scenario: createBy 自动注入登录 username

- **GIVEN** JWT 当前 username="alice"
- **WHEN** `POST /api/stories` body 创建 Story
- **THEN** SHALL 返回 201
- **AND** body.createBy SHALL 为 "alice"（由 AuditorAwareImpl 自动注入）

### Requirement: 查询 Story

后端 SHALL 通过 `GET /api/stories/{id}` 返回单 Story 详情（含富化）；通过 `GET /api/stories?requirementId=&status=&priority=&search=&page=&size=` 返回 PageResponse。

#### Scenario: GET 详情完整字段集 + 富化

- **GIVEN** Story id=1 存在，关联 Requirement id=1（code=REQ-1, title="登录流程"）/ Project id=1 / User id=1
- **WHEN** `GET /api/stories/1`
- **THEN** SHALL 返回 200
- **AND** body 字段集 SHALL 等于 `[id, code, title, description, acceptanceCriteria, status, priority, complexity, requirementId, requirementCode, requirementTitle, projectId, projectName, projectCode, ownerUserId, ownerName, ownerLoginName, closeReason, createTime, updateTime, createBy, updateBy]`
- **AND** body.requirementCode SHALL 为 "REQ-1"
- **AND** body.requirementTitle SHALL 为 "登录流程"

#### Scenario: 按 requirementId 过滤列表

- **GIVEN** Requirement id=1 下有 3 个 Story；Requirement id=2 下有 2 个
- **WHEN** `GET /api/stories?requirementId=1`
- **THEN** body.total SHALL 为 3
- **AND** body.content[*].requirementId SHALL 全为 1

#### Scenario: 按 status 过滤列表

- **GIVEN** 数据库 2 个 IN_PROGRESS + 1 个 DONE Story
- **WHEN** `GET /api/stories?status=IN_PROGRESS`
- **THEN** body.total SHALL 为 2
- **AND** body.content[*].status SHALL 全为 "IN_PROGRESS"

### Requirement: 更新 Story（含 owner 可改）

后端 SHALL 通过 `PUT /api/stories/{id}` 修改 code（重检唯一） / title / description / acceptanceCriteria / status / priority / complexity / `ownerUserId`（可改） / closeReason；`requirementId` 与 `projectId` 不可改（payload 中若含静默忽略）；新 `ownerUserId` 不存在 → 400。

#### Scenario: 更新 status + priority + acceptanceCriteria

- **GIVEN** Story id=1，status="DRAFT"，acceptanceCriteria=null
- **WHEN** `PUT /api/stories/1` body 含 `{"code":"STR-1","title":"X","ownerUserId":1,"status":"IN_PROGRESS","priority":"HIGH","acceptanceCriteria":"用户能登录成功；错误密码弹提示"}`
- **THEN** SHALL 返回 200
- **AND** body.status SHALL 为 "IN_PROGRESS"
- **AND** body.priority SHALL 为 "HIGH"
- **AND** body.acceptanceCriteria SHALL 为 "用户能登录成功；错误密码弹提示"

#### Scenario: PUT 改 ownerUserId 转移负责人 + 富化跟随

- **GIVEN** Story id=1，ownerUserId=1；用户 id=2 loginName="lili" / name="黎立" 存在
- **WHEN** `PUT /api/stories/1` body 含 `ownerUserId=2`
- **THEN** SHALL 返回 200
- **AND** body.ownerUserId SHALL 为 2
- **AND** body.ownerName SHALL 为 "黎立"
- **AND** body.ownerLoginName SHALL 为 "lili"

#### Scenario: PUT 新 ownerUserId 不存在被拒

- **GIVEN** Story id=1 存在；用户 id=999_999 不存在
- **WHEN** `PUT /api/stories/1` body 含 `ownerUserId=999999`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "owner user not found"

### Requirement: 软删 Story

后端 SHALL 通过 `DELETE /api/stories/{id}` 标记 `del_flag=1`；删除后 `GET /api/stories/{id}` 返 404。

#### Scenario: 软删成功 + 后续 GET 404

- **GIVEN** Story id=1 存在
- **WHEN** `DELETE /api/stories/1`
- **THEN** SHALL 返回 204
- **AND** 后续 `GET /api/stories/1` SHALL 返回 404
- **AND** DB `SELECT del_flag FROM rainier_story WHERE id=1` SHALL 为 1
