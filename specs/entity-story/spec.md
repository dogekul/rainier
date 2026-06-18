# Capability: entity-story

> Change log:
> - 2026-06-08 (v0.0.9-story) — NEW capability. Story belongs directly to
>   Requirement (NN FK). projectId auto-inherited from the parent Requirement.
> - 2026-06-09 (v0.0.10-sprint) — refactored to Sprint subordinate. Story now
>   belongs to a Sprint (NN FK); the parent Requirement is reached via
>   Sprint.requirementId. projectId auto-inherited via 2-stage join
>   (sprint → requirement → projectId) at creation time. Field set on the
>   GET-detail response grows by `sprintId / sprintCode / sprintName /
>   sprintStatus`. The legacy `requirement_id` column is left on
>   `rainier_story` (loosened to NULL by the migration) and is queued for DROP
>   in v0.0.11+.
> - 2026-06-12 (v0.0.18-workbench) — `GET /api/stories` adds an `ownerUserId` filter
>   param (powers 我的 Story on the workbench); optional, combinable with the existing filters.
>
> Family invariants retained: code service-level unique (soft-delete reuse OK);
> 6-state machine (DRAFT / READY / IN_PROGRESS / DONE / BLOCKED / CANCELLED);
> owner mutable (Decision 6b); soft-deleted via `@SQLDelete + del_flag`. Task
> is a separate entity, not modeled here.

## Requirements

### Requirement: 创建 Story (v0.0.10 — sprint-based)

后端 SHALL 通过 `POST /api/stories` 接受 `code` + `title` + `sprintId` + `ownerUserId`（必填），其余字段使用默认值；Service SHALL 通过 2-stage join 从 Sprint → Requirement → projectId 自动继承填入；持久化并返回 201。`requirementId` 字段在 v0.0.10 创建 payload 中不再存在。

#### Scenario: 最小 payload 创建 Story + 默认值 + 富化 + projectId 二段继承 (v0.0.10)

- **GIVEN** 数据库存在 Project id=1 / Requirement id=1（projectId=1）/ Sprint id=10（requirementId=1）/ User id=1（loginName="alice"，name="Alice"）
- **WHEN** 客户端 `POST /api/stories` body `{"code":"STR-001","title":"用户登录页","sprintId":10,"ownerUserId":1}`
- **THEN** 系统 SHALL 返回 HTTP 201
- **AND** body 默认值 SHALL 为 `status="DRAFT"` / `priority="MEDIUM"` / `complexity=null`
- **AND** body.projectId SHALL 为 1（从 Sprint.Requirement.projectId 二段继承）
- **AND** body SHALL 富化 `sprintId=10` / `sprintCode` / `sprintName` / `sprintStatus` / `requirementCode` / `requirementTitle` / `ownerName="Alice"` / `ownerLoginName="alice"` / `projectName` / `projectCode`

#### Scenario: code 重复被拒（service 级唯一）

- **GIVEN** 数据库已存在 `code="STR-001"` Story
- **WHEN** 再 `POST /api/stories` 同 `code="STR-001"`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "code already exists"

#### Scenario: sprintId 不存在被拒 (v0.0.10 — 替换 v0.0.9 requirementId 校验)

- **GIVEN** 数据库无 Sprint id=999
- **WHEN** `POST /api/stories` body 含 `sprintId=999`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "sprint not found"

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

#### Scenario: 缺必填字段被拒 (v0.0.10 — sprintId 替换 requirementId)

- **GIVEN** 后端已启动
- **WHEN** `POST /api/stories` body `{"code":"STR-X"}`（缺 title / sprintId / ownerUserId）
- **THEN** SHALL 返回 400
- **AND** body.fieldErrors[*].field SHALL 同时含 `"title"` / `"sprintId"` / `"ownerUserId"`

#### Scenario: createBy 自动注入登录 username

- **GIVEN** JWT 当前 username="alice"
- **WHEN** `POST /api/stories` body 创建 Story
- **THEN** SHALL 返回 201
- **AND** body.createBy SHALL 为 "alice"（由 AuditorAwareImpl 自动注入）

### Requirement: 查询 Story (v0.0.10 — sprint-based filter)

后端 SHALL 通过 `GET /api/stories/{id}` 返回单 Story 详情（含 2-stage 富化）；通过 `GET /api/stories?sprintId=&status=&priority=&search=&page=&size=` 返回 PageResponse。`requirementId` query 参数在 v0.0.10 起不再支持（Sprint 是直接父）。

#### Scenario: GET 详情完整字段集 + 富化 (v0.0.10)

- **GIVEN** Story id=1 存在，关联 Sprint id=10（code=SPR-1, name="Phase 1", status="ACTIVE", requirementId=1）/ Requirement id=1（code=REQ-1, title="登录流程"）/ Project id=1 / User id=1
- **WHEN** `GET /api/stories/1`
- **THEN** SHALL 返回 200
- **AND** body 字段集 SHALL 等于 `[id, code, title, description, acceptanceCriteria, status, priority, complexity, sprintId, sprintCode, sprintName, sprintStatus, requirementId, requirementCode, requirementTitle, projectId, projectName, projectCode, ownerUserId, ownerName, ownerLoginName, closeReason, createTime, updateTime, createBy, updateBy]`
- **AND** body.sprintCode SHALL 为 "SPR-1"
- **AND** body.requirementCode SHALL 为 "REQ-1"（来自 sprint→requirement 2-stage join）

#### Scenario: 按 sprintId 过滤列表 (v0.0.10 — 替换 v0.0.9 requirementId 过滤)

- **GIVEN** Sprint id=10 下有 3 个 Story；Sprint id=20 下有 2 个
- **WHEN** `GET /api/stories?sprintId=10`
- **THEN** body.total SHALL 为 3
- **AND** body.content[*].sprintId SHALL 全为 10

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

## ADDED Requirements (from change 2026-06-12-workbench / v0.0.18)

### Requirement: 按 ownerUserId 过滤 Story 列表

后端 SHALL 在 `GET /api/stories?ownerUserId=` 按 `ownerUserId` 精确过滤，可与既有 projectId/sprintId/status/priority 组合；省略则不过滤。

#### Scenario: 按 ownerUserId 过滤仅返回匹配项

- **GIVEN** owner=1 有 2 个 Story，owner=2 有 1 个 Story
- **WHEN** `GET /api/stories?ownerUserId=1`
- **THEN** body.total SHALL 为 2
- **AND** body.content 全部 `ownerUserId=1`

## ADDED Requirements (from change 2026-06-18-review-queue / v0.0.39)

> 路线图 #7。Story 新增 reviewer 字段（可空列，ddl-auto 自动建，存量行 review 字段为 null → 不进任何评审队列）。
> reviewStatus 取值 `PENDING`/`APPROVED`/`REJECTED`（{@code ReviewStatus}），null = 无评审需求。

### Requirement: Story 评审字段（reviewerUserId / reviewStatus）

后端 SHALL 在 `rainier_story` 加可空列 `reviewer_user_id`(BIGINT) 与 `review_status`(VARCHAR 16)。`POST /api/stories`
与 `PUT /api/stories/{id}` SHALL 接受可选 `reviewerUserId`（非空则校验用户存在，否则 400）与可选 `reviewStatus`
（非空则校验 ∈ ReviewStatus.ALL，否则 400）；`GET` 响应 `StoryDetail` SHALL 富化 `reviewerUserId` / `reviewStatus`
/ `reviewerName`。PUT 对评审字段采用全量替换语义（不带则置空）。

#### Scenario: 创建 Story 带评审人与 PENDING

- **GIVEN** 存在用户 anna 及一条 Sprint
- **WHEN** `POST /api/stories` body 含 `reviewerUserId=anna.id` 且 `reviewStatus="PENDING"`
- **THEN** SHALL 返回 201
- **AND** body.reviewerUserId SHALL 为 anna.id
- **AND** body.reviewStatus SHALL 为 "PENDING"
- **AND** body.reviewerName SHALL 为 anna 的 name（service 富化）

#### Scenario: 评审人不存在被拒

- **GIVEN** 数据库无 id=999999 的用户
- **WHEN** `POST /api/stories` body 含 `reviewerUserId=999999`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "reviewer user not found"

#### Scenario: 非法 reviewStatus 被拒

- **GIVEN** 一条合法 Sprint 与 owner
- **WHEN** `POST /api/stories` body 含 `reviewStatus="MAYBE"`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "invalid reviewStatus"

#### Scenario: 创建不带评审字段则为空

- **WHEN** `POST /api/stories` 不含 reviewerUserId / reviewStatus
- **THEN** SHALL 返回 201
- **AND** body.reviewerUserId SHALL 为 null
- **AND** body.reviewStatus SHALL 为 null

### Requirement: 评审决定端点

后端 SHALL 提供 `POST /api/stories/{id}/review`，body `{decision}`，`decision` SHALL 限 `APPROVED`/`REJECTED`，
将该 Story 的 `reviewStatus` 置为 decision 并保留 `reviewerUserId`，返回更新后的 `StoryDetail`。

#### Scenario: 通过评审

- **GIVEN** 存在 Story id=S，reviewerUserId=anna、reviewStatus=PENDING
- **WHEN** `POST /api/stories/S/review` body `{"decision":"APPROVED"}`
- **THEN** SHALL 返回 200
- **AND** body.reviewStatus SHALL 为 "APPROVED"
- **AND** body.reviewerUserId SHALL 不变（仍为 anna）

#### Scenario: 非法 decision 被拒

- **GIVEN** 存在 Story id=S
- **WHEN** `POST /api/stories/S/review` body `{"decision":"MEH"}`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "invalid decision"

#### Scenario: Story 不存在

- **WHEN** `POST /api/stories/999999/review` body `{"decision":"APPROVED"}`
- **THEN** SHALL 返回 404

### Requirement: 我的待评审队列

后端 SHALL 提供 `GET /api/me/pending-reviews`（all-users，token 必需，非 admin 门控），返回 `reviewerUserId = 当前
用户` 且 `reviewStatus = PENDING` 的未软删 Story（富化 code/title/status/priority/projectName/sprintName/ownerName），
按优先级高→低、再按创建时间升序（最久未评在前）排序。

#### Scenario: 只返回我的 PENDING

- **GIVEN** anna 为 reviewer 的 PENDING Story 一条、bob 为 reviewer 的 PENDING Story 一条、anna 的另一 Story 已 APPROVED
- **WHEN** anna 携带有效 token `GET /api/me/pending-reviews`
- **THEN** SHALL 返回 HTTP 200
- **AND** 结果 SHALL 仅含 anna 的那条 PENDING Story
- **AND** 结果 SHALL 不含 bob 的 Story，也不含已 APPROVED 的 Story

#### Scenario: 按优先级排序

- **GIVEN** anna 为 reviewer 的两条 PENDING Story：一条 priority=LOW、一条 priority=URGENT
- **WHEN** anna `GET /api/me/pending-reviews`
- **THEN** 结果首条 SHALL 为 URGENT 那条

#### Scenario: 缺 token 拒绝

- **WHEN** 未携带 token `GET /api/me/pending-reviews`
- **THEN** SHALL 返回 HTTP 401
