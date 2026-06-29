# Spec: gitlab-webhook (capability=gitlab-webhook)

## Scenario 1: Valid token + push payload with RA-<id> ref → Event + AiWorkLog
GIVEN the configured `app.gitlab.webhook-secret=test-secret`
AND a JSON body `{ "object_kind": "push", "after": "abc123", "commits": [{"message": "fix RA-42 done"}] }`
WHEN `POST /api/webhooks/gitlab` is called with header `X-Gitlab-Token: test-secret`
THEN response is 202
AND response body contains `eventId` (numeric) and `processed=1`
AND a new Event is persisted with sourceType=GITLAB, eventKind=COMMIT, sourceId=abc123, processed=true
AND the Event has extractedEntityType=TASK, extractedEntityId=42 (GitLabAdapter ran)

## Scenario 2: Valid token + merge_request payload (action=merge) → eventKind=PR_MERGE + AiWorkLog PROPOSED
GIVEN the configured secret matches
AND a JSON body whose `object_kind=merge_request`, `object_attributes.action=merge`,
    `object_attributes.iid=7`, `object_attributes.title="ship RA-100"`
WHEN `POST /api/webhooks/gitlab` is called with the correct token
THEN response is 202
AND a PROPOSED AiWorkLog is created with agentType=STATUS_SYNC, action=UPDATE_TASK_STATUS,
    targetType=TASK, targetId=100 (StatusSyncService bridged it)

## Scenario 3: Wrong / missing token → 401
GIVEN the configured secret is `test-secret`
WHEN the request carries `X-Gitlab-Token: wrong-secret`
OR the header is omitted entirely
THEN response is 401
AND NO Event row is persisted
