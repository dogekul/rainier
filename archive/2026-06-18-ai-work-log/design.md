# Design — v0.0.43-ai-work-log

> Baseline: tag `v0.0.42-po-inbox` / commit d08cefb. The first flywheel-layer slice — seed-driven shell.

## Context

The flywheel (AI/events/integration) is gated on external integration, but its structural base — a
human-reviewable log of AI proposals with evidence + an accept/reject state machine — can be built now,
seed-driven. Mirrors AuditLog (append-style log + paginated read) + StoryService.review (decision
endpoint) + the gated idempotent CommandLineRunner seed pattern.

## Decisions

### D1 — NEW capability `ai-work-log`; new table rainier_ai_work_log
`AiWorkLog extends BaseEntity` (ddl-auto=update auto-creates the table; @EnableJpaAuditing fills
createTime). No @SQLDelete (we don't delete AI logs; status mutates via decision). Table count 20→21
(update LegacyProductCategoryCleanupTest assertion).

### D2 — entity shape + evidence invariant
Fields: agentType(32), action(64), targetType(64,null), targetId(null), summary(512), **evidence(2000,
NN)**, status(16, default PROPOSED), decidedBy(32,null), decidedAt(Instant,null), rejectReason(512,null).
`evidence` is NOT NULL at the column + @NotBlank on the create request — an AI proposal MUST cite evidence
(the flywheel's trust contract). `AiWorkLogStatus{PROPOSED,ACCEPTED,REJECTED}` + `ALL` + `DECISIONS={ACCEPTED,REJECTED}`.

### D3 — decision state machine
`POST /api/ai-work-logs/{id}/decision {decision, reason?}` → `AiWorkLogService.decide`:
- decision ∉ DECISIONS → 400.
- log.status != PROPOSED → 409 (already decided; idempotency/anti-double-decide).
- decision == REJECTED && blank reason → 400 (reject reason is the KPI signal).
- set status, decidedBy (token username, else "system"), decidedAt, rejectReason; save; return detail.

### D4 — read + create
`GET /api/ai-work-logs?agentType=&status=&page=&size=` — Specification filter + PageResponse, sort
DESC createTime, id (AuditLog pattern). `GET /{id}`. `POST /api/ai-work-logs` — create a PROPOSED
proposal (agentType/action/summary/evidence @NotBlank); this is how the AI agent will propose (now also
used to seed/manually add). All-users (D6).

### D5 — seed-driven shell
`AiWorkLogSeed` (@Component @Order(HIGHEST_PRECEDENCE) CommandLineRunner, @Transactional), gated on
`app.demo.ai-work-log-seed.enabled` (true prod/dev, **false in application-test.yml** → no test
pollution). Idempotent: seeds ~4 varied PROPOSED entries only when `repo.count()==0`. Gives the page
real data without a real AI.

### D6 — authz all-users; new AI nav group
`/api/ai-work-logs/**` is NOT in AdminPaths → all-users (token-gated). Fine-grained 分级授权 (who may
accept/reject which agent's proposals) is a later flywheel step. Frontend: new top-level「AI」navGroup
(no requiresAdmin → all-users); `/ai/work-logs` NOT in ADMIN_PATH_PREFIXES → navGuardConsistency auto-pins
all-users.

## Architecture / Data flow

```
seed (startup, flag on, table empty) → N PROPOSED AiWorkLog rows
AI proposes (future) → POST /api/ai-work-logs (evidence required) → PROPOSED
human reviews → GET /api/ai-work-logs?status=PROPOSED → AiWorkLogsPage
human decides → POST /api/ai-work-logs/{id}/decision → ACCEPTED | REJECTED(+reason) [only from PROPOSED]
```

## Risks / Trade-offs

| Risk | Mitigation |
|---|---|
| seed pollutes tests | gated flag, false in application-test.yml (like AdminAuthzBootstrap / RealAuthPasswordBackfill) |
| double-decide race | status!=PROPOSED → 409; decision is one-way from PROPOSED |
| evidence empty (black-box AI) | NN column + @NotBlank — proposals must cite evidence |
| all-users decisions | accepted for the shell; 分级授权 is a later flywheel step |
| new table | ddl-auto auto-creates; table-count test bumped 20→21 |
| Java 8 | constants via unmodifiableSet(new HashSet<>(Arrays.asList())); no Set.of/var/no-arg orElseThrow; temurin-8 gate |
