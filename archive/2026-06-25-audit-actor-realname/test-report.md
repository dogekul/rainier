# Test Report — audit-actor-realname (B6) v0.0.79

## Backend
- mvn test: **704/704 passed**, 0 failures, 0 errors, 0 skipped
- New: `AuditActorRealnameTest` (3 tests, all green)
  - `withValidBearerToken_actorIsRealLoginName` — token subject "bob" → audit row actor = "bob"
  - `noHttpContext_actorDegradesToSystem` — TransactionTemplate path (no HTTP) → actor = "system"
  - `afterRequest_threadLocalIsCleared` — SecurityFilter finally clears RequestUserContext
- Regression: `AuditAspectIntegrationTest` (9/9) + `AuthBaselineTest` + `GlobalAuthBaselineTest` + `AdminAuthz*` all green.

## Lint / Java 8
- No List.of / Map.of / var / Optional.orElseThrow() / Stream.toList() / switch-表达式 introduced.

## OutOfScope (untouched)
- audit_log schema / column names unchanged.
- AuditorAwareImpl unchanged (BaseEntity.createdBy/updatedBy preserved as fallback).
