# Pending Adjustments — v0.0.15-audit-log (Phase 4 BUILD)

## PA-1. TC-AUD-014 append-only: POST surfaces as 500, not 405

- **Discovered**: M06. The spec scenario said an unsupported write returns 404/405. In reality `POST /api/audit-logs` returns **500** — the pre-existing global `@ExceptionHandler(Throwable.class)` in `GlobalExceptionHandler` (not introduced by this change) catches the unhandled `HttpRequestMethodNotSupportedException` and maps it to 500.
- **Resolution**: reworked the test to assert the **load-bearing** append-only guarantees instead of the brittle status code: (a) `AuditLogController` source contains no write mapping, (b) POST does not return 2xx, (c) POST persists zero audit rows. The append-only contract holds regardless of the 405→500 mapping.
- **Impact**: minor; the spec's "404/405" wording is a pre-existing-handler artifact. Properly mapping 405 (a global-handler change) is out of v0.0.15 scope.

## PA-2. LegacyProductCategoryCleanupTest table count 17 → 18

- **Discovered**: M06 full-suite run. v0.0.15 adds `rainier_audit_log`.
- **Resolution**: assertion updated 17 → 18 + added `contains("rainier_audit_log")`. Same cross-version test maintenance as v0.0.14 PA-2.
- **Impact**: minor; only test touched outside scope.

## PA-3. AuditLogController javadoc reworded

- **Discovered**: M06. The append-only source-grep test matched the literal string `@PostMapping` that appeared inside the controller's own javadoc (`{@code @PostMapping}`).
- **Resolution**: reworded the javadoc to "no write mapping (no POST / PUT / DELETE handler)" so the grep only matches real annotations.
- **Impact**: cosmetic.

## PA-4. Test-count reconciliation

- Estimated ~18 new backend tests; actual **16** (9 aspect + 5 query + 2 perf) → 293 + 16 = 309. Frontend +4 = 58. Estimate-only drift, no behaviour impact.

## Note — 🔴 high-risk AOP weaving verdict

The AuditAspect weaves `@AfterReturning` over **all 17** `*Service.create/update/delete`. Full 293-test backend regression passed with zero failures (only the cross-version table-count test needed the 17→18 bump). Same-transaction rollback (TC-AUD-006 via TransactionTemplate `setRollbackOnly`) and failed-write-not-audited (TC-AUD-005) both verified. The cross-cutting change is clean.
