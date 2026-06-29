# Test Report — F1 ai-decision-executor (v0.0.100)

## Backend (mvn test)
- Total: 860 tests, 0 failures, 0 errors, 0 skipped (was 845 → +15)
- New tests:
  - `UpdateTaskStatusExecutorTest` (6) — supports/execute/reverse + error paths
  - `AiWorkLogServiceDecisionWithExecutorTest` (4) — ACCEPTED runs executor, REJECTED no-op, unknown action flips status without snapshot, executor exception swallowed
  - `AiWorkLogReverseControllerTest` (5) — happy path + 400 (non-ACCEPTED, no snapshot, second reverse) + 404
- Pre-existing AiWorkLog suites still green: `AiWorkLogControllerTest` (9), `AiWorkLogSeedTest` (2)

## Scope verification vs spec.md
- Scenario 1 (ACCEPTED → executor writes Task.status=DONE + snapshot): covered by
  `accept_updateTaskStatus_runsExecutor_andStoresSnapshot` + executor `execute_setsDone_andSnapshotsOldStatus`.
- Scenario 2 (REJECTED leaves entity untouched): covered by `reject_doesNotMutateEntity_noSnapshot`.
- Scenario 3 (reverse restores + AiError OPEN + idempotent): covered by
  `reverse_restoresTask_revivesLog_andRecordsAiError` + `reverse_twice_secondReturns400` +
  `reverse_nonAcceptedLog_returns400` + `reverse_acceptedButNoSnapshot_returns400`.

## Caveats
- Frontend Undo button UI is OutOfScope; only the API + new DTO fields ship in this batch.
- Executor failure swallowing keeps the status flip atomic with decision audit; the trade-off is
  the log becomes non-reversible (reverseSnapshot=null) — by design.
