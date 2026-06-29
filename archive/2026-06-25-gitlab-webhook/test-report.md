# Test Report: gitlab-webhook (F2, v0.0.101)

## Summary
- Backend: `mvn test` → **864 tests, 0 failures, 0 errors, 0 skipped** (was 860; +4 new).
- Frontend: untouched in this sub-change; not run.

## New tests
`backend/src/test/java/com/rainier/event/webhook/GitLabWebhookControllerTest.java` (4 cases):
1. `validToken_pushPayload_recordsEventAndExtractsTaskRef` — push payload with `RA-42` ref
   yields 202, persists an Event (sourceType=GITLAB, eventKind=COMMIT, sourceId=`abc123`),
   processed=true, GitLabAdapter extracts TASK/42.
2. `validToken_mergeRequestPayload_createsProposedAiWorkLog` — merge_request action=merge
   with iid=7 and `RA-100` title yields 202, eventKind=PR_MERGE; StatusSyncService writes a
   PROPOSED AiWorkLog (agentType=STATUS_SYNC, action=UPDATE_TASK_STATUS, targetId=100).
3. `wrongToken_returns401_andPersistsNothing` — mismatched `X-Gitlab-Token` → 401, no rows.
4. `missingToken_returns401` — header absent → 401, no rows.

## Regressions checked
- AuthBaselineTest (7 tests, identity-baseline gate) still green; the new whitelist entry
  for `/api/webhooks/**` does not break the require-all-users-token enforcement.
- Full event package suite (EventService / adapters / sync) untouched and green.

## Coverage notes
- Constant-time token compare via `MessageDigest.isEqual` — covered indirectly by the
  pass/fail token cases. Timing-attack resistance is a property test we don't run here.
- JSON parse error fallback (`eventKind=OTHER`) is reachable but not currently asserted;
  flagged as a follow-up if/when a real GitLab fuzz harness lands.
