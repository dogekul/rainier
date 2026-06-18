# Capability: entity-story — v0.0.39 review-queue delta (MODIFIED)

> 合并入 canonical `specs/entity-story/spec.md`（Phase 6）。仅新增以下 Requirements，不动既有。

## ADDED Requirements (from change 2026-06-18-review-queue / v0.0.39)

### Requirement: Story 评审字段（reviewerUserId / reviewStatus）

后端 SHALL 在 `rainier_story` 加可空列 `reviewer_user_id`(BIGINT) 与 `review_status`(VARCHAR 16)。
`reviewStatus` 取值 SHALL 限 `PENDING`/`APPROVED`/`REJECTED`（null = 无评审需求）。`POST /api/stories` 与
`PUT /api/stories/{id}` SHALL 接受可选 `reviewerUserId`（非空则校验用户存在）与可选 `reviewStatus`（非空则校验合法）；
`GET` 响应 `StoryDetail` SHALL 富化 `reviewerUserId` / `reviewStatus` / `reviewerName`。

#### Scenario: 创建 Story 带评审人与 PENDING

- **GIVEN** 存在用户 alice 及一条 Sprint
- **WHEN** `POST /api/stories` body 含 `reviewerUserId=alice.id` 且 `reviewStatus="PENDING"`
- **THEN** SHALL 返回 201
- **AND** body.reviewerUserId SHALL 为 alice.id
- **AND** body.reviewStatus SHALL 为 "PENDING"
- **AND** body.reviewerName SHALL 为 alice 的 name（service 富化）

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

- **GIVEN** 存在 Story id=S，reviewerUserId=alice、reviewStatus=PENDING
- **WHEN** `POST /api/stories/S/review` body `{"decision":"APPROVED"}`
- **THEN** SHALL 返回 200
- **AND** body.reviewStatus SHALL 为 "APPROVED"
- **AND** body.reviewerUserId SHALL 不变（仍为 alice）

#### Scenario: 非法 decision 被拒

- **GIVEN** 存在 Story id=S
- **WHEN** `POST /api/stories/S/review` body `{"decision":"MEH"}`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "invalid decision"

#### Scenario: Story 不存在

- **WHEN** `POST /api/stories/999999/review` body `{"decision":"APPROVED"}`
- **THEN** SHALL 返回 404

### Requirement: 我的待评审队列

后端 SHALL 提供 `GET /api/me/pending-reviews`（all-users，token 必需），返回 `reviewerUserId = 当前用户`
且 `reviewStatus = PENDING` 的未软删 Story（富化 code/title/status/priority/projectName/sprintName/ownerName），
按优先级高→低、再按创建时间升序（最久未评在前）排序。

#### Scenario: 只返回我的 PENDING

- **GIVEN** alice 为 reviewer 的 Story（PENDING）一条、bob 为 reviewer 的 Story（PENDING）一条、alice 的另一 Story 已 APPROVED
- **WHEN** alice 携带有效 token `GET /api/me/pending-reviews`
- **THEN** SHALL 返回 HTTP 200
- **AND** 结果 SHALL 仅含 alice 的那条 PENDING Story
- **AND** 结果 SHALL 不含 bob 的 Story，也不含已 APPROVED 的 Story

#### Scenario: 按优先级排序

- **GIVEN** alice 为 reviewer 的两条 PENDING Story：一条 priority=LOW、一条 priority=URGENT
- **WHEN** alice `GET /api/me/pending-reviews`
- **THEN** 结果首条 SHALL 为 URGENT 那条

#### Scenario: 缺 token 拒绝

- **WHEN** 未携带 token `GET /api/me/pending-reviews`
- **THEN** SHALL 返回 HTTP 401
