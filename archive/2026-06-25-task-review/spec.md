# Spec: task-review (v0.0.82)

## Scenario S1 — Task create 带 reviewer + PENDING
**Given** Project P / User reviewer R 存在
**When** `POST /api/tasks` body 含 `reviewerUserId=R, reviewStatus=PENDING`
**Then** 201；返回的 TaskDetail.reviewerUserId == R，reviewStatus == PENDING，
reviewerName 已 enrich。

## Scenario S2 — Task POST /review APPROVED
**Given** Task t reviewerUserId=R, reviewStatus=PENDING
**When** `POST /api/tasks/{id}/review` body `{decision: "APPROVED"}`
**Then** 200；t.reviewStatus == APPROVED；reviewerUserId 保留。

## Scenario S3 — Task POST /review REJECTED 必须带 reason
**Given** Task t reviewerUserId=R, reviewStatus=PENDING
**When** `POST /api/tasks/{id}/review` body `{decision: "REJECTED"}` 无 reason
**Then** 400 "reason required for REJECTED"。
**When** 同请求带 `reason: "重写"` 重试
**Then** 200；t.reviewStatus == REJECTED；t.closeReason == "重写"。

## Scenario S4 — Task POST /review 非法 decision
**Given** Task t 存在
**When** `POST /api/tasks/{id}/review` body `{decision: "MAYBE"}`
**Then** 400 "invalid decision: MAYBE"。

## Scenario S5 — Task PUT patch-like 不清 reviewer
**Given** Task t reviewerUserId=R, reviewStatus=PENDING
**When** `PUT /api/tasks/{id}` body 不含 reviewerUserId / reviewStatus 键
**Then** 200；t.reviewerUserId == R 保留，reviewStatus == PENDING 保留。

## Scenario S6 — GET /api/me/pending-reviews 合并 Story+Task
**Given** Alice 是 1 个 Story 和 2 个 Task 的 reviewer，均 PENDING
**When** Alice 调 `GET /api/me/pending-reviews`
**Then** 200；返回 3 行；Story 行 `kind="STORY"`，Task 行 `kind="TASK"`，taskId 填值。

## TestCase 映射
| ID | Scenario | 类型 | 位置 |
|----|----------|------|------|
| TC-TREV-001 | S1 | @SpringBootTest | TaskReviewTest |
| TC-TREV-002 | S2 | @SpringBootTest | TaskReviewTest |
| TC-TREV-003 | S3 | @SpringBootTest | TaskReviewTest |
| TC-TREV-004 | S4 | @SpringBootTest | TaskReviewTest |
| TC-TREV-005 | S5 | @SpringBootTest | TaskUpdateNoReviewerWipeTest |
| TC-TREV-006 | S6 | @SpringBootTest | MePendingReviewsTaskMergeTest |
