# Spec: ai-decision-executor (capability=ai-decision-executor)

## Scenario 1: ACCEPTED with UPDATE_TASK_STATUS executes and snapshots
GIVEN an AiWorkLog PROPOSED with agentType=STATUS_SYNC, action=UPDATE_TASK_STATUS, evidence references task #42 (status=DOING)
WHEN POST /api/ai-work-logs/{id}/decision { decision: ACCEPTED }
THEN the AiWorkLog.status becomes ACCEPTED
AND Task #42.status is updated to DONE
AND AiWorkLog.reverseSnapshot is non-null JSON containing {"taskId":42, "oldStatus":"DOING", "newStatus":"DONE"}

## Scenario 2: REJECTED does NOT touch the entity
GIVEN an AiWorkLog PROPOSED for UPDATE_TASK_STATUS on task #42 (status=DOING)
WHEN POST /api/ai-work-logs/{id}/decision { decision: REJECTED, reason: "误判" }
THEN AiWorkLog.status=REJECTED, rejectReason="误判"
AND Task #42.status remains DOING
AND AiWorkLog.reverseSnapshot is null

## Scenario 3: Reverse restores the entity and publishes an AiError
GIVEN an AiWorkLog with status=ACCEPTED, action=UPDATE_TASK_STATUS, reverseSnapshot present, Task #42.status=DONE
WHEN POST /api/ai-work-logs/{id}/reverse
THEN AiWorkLog.status flips back to PROPOSED, reversedAt/reversedBy set, reverseSnapshot cleared
AND Task #42.status is restored to its pre-execution value (DOING)
AND a new AiError row is recorded with status=OPEN and errorDesc citing the reversal
AND a second reverse call on the same log returns 400 (no longer ACCEPTED / snapshot empty)
