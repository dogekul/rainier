# Test Report — status-auto-sync (A3, v0.0.67)

## New / Modified Classes

| Path | Type | Notes |
| ---- | ---- | ----- |
| `backend/src/main/java/com/rainier/event/sync/StatusSyncService.java` | NEW @Service | Event → AiWorkLog PROPOSED bridge |
| `backend/src/main/java/com/rainier/event/service/EventService.java` | MODIFIED | Constructor +1 param (StatusSyncService); `process()` now calls `statusSync.applyExtraction(e)` per drained event |
| `backend/src/test/java/com/rainier/event/sync/StatusSyncServiceTest.java` | NEW @SpringBootTest | 5 unit tests covering happy path + 3 no-op guards + e2e via `EventService.process` |
| `backend/src/test/java/com/rainier/event/service/EventServiceTest.java` | MODIFIED | Update 5 constructor calls to pass `null` statusSync (unit tests don't need the bridge) |

## DDL / Schema

No new tables, no new columns. Reuses existing `rainier_ai_work_log` (v0.0.43) end-to-end:

- agentType="STATUS_SYNC", action="UPDATE_TASK_STATUS"
- targetType="TASK", targetId=event.extractedEntityId
- evidence JSON: `{"eventId":..,"sourceType":..,"sourceId":..,"eventKind":..}`
- status starts at PROPOSED (decided via existing `POST /api/ai-work-logs/{id}/decision`)

## Test Results

- Backend: **594 tests pass, 0 failures, 0 errors, 0 skipped** (`mvn test`)
  - New: 5 `StatusSyncServiceTest` tests
  - Pre-existing: 589 still green; in particular `EventServiceProcessIntegrationTest` (now also writes 1 PROPOSED for the GitLab event in its mix) still passes since it never asserted on `rainier_ai_work_log`
- Frontend: not touched (out of scope)

## Caveats / Follow-ups

- `accept` of a PROPOSED `STATUS_SYNC` log does NOT yet mutate the target Task — out of scope per A3 brief. Sub-change in batch A/D will need a `DecisionExecutor` that, on ACCEPT of action=UPDATE_TASK_STATUS, sets `task.status=DONE`.
- Only GitLab `PR_MERGE → TASK` is wired. Future rules (`DOC_CHANGE → REQUIREMENT`, `MESSAGE → COMMENT`, etc.) will land as more guards in `StatusSyncService.applyExtraction`.
- `EventService` constructor signature changed — any out-of-tree callers (none currently) would need to pass the new `StatusSyncService` arg.
