# Proposal: gitlab-webhook (F2)

## What
NEW `POST /api/webhooks/gitlab` — real GitLab webhook ingress that accepts raw GitLab push/MR payloads,
verifies the `X-Gitlab-Token` header, records an `Event` (sourceType=GITLAB) and immediately drains
the pipeline via `EventService.process`, so the downstream GitLabAdapter → StatusSyncService →
AiWorkLog PROPOSED chain (already shipped) is exercised end-to-end.

## Why
F1 (DecisionExecutor) made AI decisions actually mutate entities; until now though, the only event
source was the generic `POST /api/events` test endpoint. F2 adds the first real external source so
the closed flywheel loop is reachable from a real GitLab instance.

## Scope
- WebhookController in `com.rainier.event.webhook` with `POST /gitlab`.
- Reads headers `X-Gitlab-Token`, `X-Gitlab-Event`; raw JSON body.
- Token compared in constant time against `app.gitlab.webhook-secret` (default `changeme`).
- Parses `object_kind` (push → COMMIT, merge_request → PR_MERGE) and picks `sourceId` from
  `after` sha (push) or `object_attributes.iid` (MR).
- Records Event, runs `EventService.process(1)`, returns `{eventId, processed}` with 202.
- Whitelist `/api/webhooks/**` in `SecurityWhitelistPaths` (token-less; auth is via X-Gitlab-Token).
- `application-test.yml`: `app.gitlab.webhook-secret=test-secret`.

## OutOfScope
- HMAC SHA-256 signature (GitLab's secret-token model is plain string equality).
- Real GitLab API callbacks; tests use canned payload strings.
- Other sources (DingTalk / Feishu / Email / Zentao) — separate sub-changes.
- Replay / dedupe by delivery id — left to a future hardening pass.

## Decisions
- Constant-time compare via `MessageDigest.isEqual` to avoid timing leaks.
- Auth model: header-token only (NO Bearer needed); the path is added to the security whitelist so
  the baseline `require-all-users-token` gate does NOT 401 it.
- We synchronously call `process(1)` so the caller sees the eventId AND knows it was processed —
  fine for v1 since payloads are small and adapters are cheap.
