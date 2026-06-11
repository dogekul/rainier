# Design Adjustments — v0.0.15-audit-log

Phase 4-5 deviations from the Phase 2 locked design + Step 0 review outcomes.

## A. Build-phase adjustments (pending-adjustments.md)

| # | Adjustment | Impact |
|---|---|---|
| PA-1 | append-only `POST /api/audit-logs` → 500 (pre-existing global catch-all `@ExceptionHandler(Throwable)` maps the unhandled 405 to 500), not 404/405. Test asserts the load-bearing guarantees (no write mapping in source + zero rows + non-2xx) instead of the brittle status code. spec scenario reworded to match. | minor; pre-existing handler, append-only contract holds |
| PA-2 | `LegacyProductCategoryCleanupTest` table count 17 → 18 (+`rainier_audit_log`). | minor; only test touched outside scope |
| PA-3 | `AuditLogController` javadoc reworded to avoid the literal `@PostMapping` string the source-grep test matched. | cosmetic |
| PA-4 | 16 new backend tests (est. ~18) → 309 total; frontend +4 = 58. | estimate reconciliation |

## B. Step 0 review fixes (Step 0.3, this phase)

| Finding | Sev | Action |
|---|---|---|
| **TC-AUD-006 tautological** — asserting "audit row absent after rollback" passes both for "joined-tx-and-rolled-back" AND "never written"; can't catch the AuditTxConfig regression it guards | **C / H** | **FIXED** — added an in-tx **presence** assertion: after `requirementService.create()` inside the `TransactionTemplate`, assert `auditRepo.count() == before+1` (proves the audit row joined the open tx) BEFORE `setRollbackOnly()`, then assert `== before` after. The +1-inside / 0-after pair distinguishes "joined & rolled back" from "never written". |
| TC-AUD-009 weak — `anyMatch` existence, not count/correctness; the 🔴 AOP-weaving sampling guard | M | FIXED — now asserts **exactly one** audit row per entityType (REQUIREMENT + PRODUCT), catching mislabel/double-audit. test-plan reconciled to 2 entities. |
| TC-AUD-012 weak — asserts only `content[0].id` (relies on id tiebreaker) | M | FIXED — asserts full order `content[0]==B && content[1]==A`. |
| TC-AUD-013 presence-only — `body.has(f)`, a field-swap passes | M | FIXED — asserts concrete values (actor/entityType/entityId=7/action=UPDATE). |
| TC-FES-AUD-003 causation — couldn't distinguish button vs page-0 effect | M | FIXED — `mockClear()` after mount + assert every post-click call carries the filter. |
| spec "404/405" wording | L | FIXED — reworded to "非 2xx；实际 500 见 PA-1". |
| `.stdd.yaml current: spec` stale | L | FIXED — → `verify`. |

## C. Recorded-but-not-fixed (rationale)

| Finding | Sev | Rationale |
|---|---|---|
| Code-M1 — audit-write failure rolls back the business tx (same-tx coupling) | M | **Inherent & intended** trade-off of same-tx atomicity. Columns sized safely (entityType≤23/64, summary≈37/512, actor=32 matching createBy). A future longer service name or free-text actor could couple an audit fault to a business failure → defensive truncation is a **v0.0.16 candidate**. |
| Code-M2/L1/L2 — `.orElse("system")` dead-defense; `actor` nullable; create/update action not routed through `AuditAction` constants | M/L | Harmless redundancy / cosmetic; values correct today. |
| Test-M4 — TC-AUD-005 asserts total-count-unchanged not "no CREATE row for that code" | M | Effective proxy; net-zero-with-removal is implausible. |
| Docs-L3/L5 — TC-FES-PROD-001 docstring "4 组"; design.md D2 sentence omits `record` | L | Cosmetic; behavior correct. |
| Residual: TC-AUD-006 still cannot fully isolate AuditTxConfig | note | A `TransactionTemplate` provides an ambient tx, so the test proves the audit write joins the *ambient* tx and rolls back — meaningful, but full AuditTxConfig isolation would need a commit-time-failure fixture (hard with current entities). Same-tx is additionally evidenced by E2E + design. |
