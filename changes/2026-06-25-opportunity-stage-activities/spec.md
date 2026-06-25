# Spec — opportunity-stage-activities (v0.0.90)

## Domain
- StageActivity (rainier_stage_activity)
  - opportunityId BIGINT NOT NULL
  - stageCode VARCHAR(32) NOT NULL  (OpportunityStage 之一)
  - activityTitle VARCHAR(200) NOT NULL
  - description TEXT (nullable)
  - assigneeUserId BIGINT (nullable)
  - dueDate DATE (nullable)
  - status VARCHAR(16) NOT NULL DEFAULT 'PENDING'  (PENDING/DONE/SKIPPED)
  - completedAt INSTANT (nullable)
- 继承 BaseEntity: id, createBy, createTime, updateBy, updateTime, delFlag

## Status 状态机
- PENDING → DONE  (markDone：set completedAt=now)
- PENDING → SKIPPED (skip：completedAt=null)
- 终态后再调用 → 400

## Endpoints
| Method | Path | Body | Returns |
|---|---|---|---|
| GET  | /api/opportunities/{id}/stages/{code}/activities | — | List<StageActivityDetail> |
| POST | /api/opportunities/{id}/stages/{code}/activities | StageActivityCreateRequest | StageActivityDetail (201) |
| POST | /api/stage-activities/{aid}/done | — | StageActivityDetail |
| POST | /api/stage-activities/{aid}/skip | — | StageActivityDetail |
| GET  | /api/opportunities/{id}/stages/{code}/dashboard | — | StageDashboardView{ activities, artifacts } |

## Validation
- opportunityId 必须存在 → 404 NotFoundException
- stageCode 必须 ∈ OpportunityStage.ALL → 400
- activityTitle 非空 → 400
- markDone/skip 已是终态 → 400

## Test scenarios
- TC-SA-001: 不存在的 opportunityId → list 404
- TC-SA-002: 无效 stageCode → 400
- TC-SA-003: add + list 顺序 (id asc)
- TC-SA-004: markDone 设置 completedAt
- TC-SA-005: skip 不设置 completedAt
- TC-SA-006: 已 DONE 再 markDone → 400
- TC-SA-007: dashboard 同时返回 activities + 该 stage 的 artifacts
