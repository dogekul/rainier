# Test Report — B7 residual-permission-recycle (v0.0.80)

## Backend
- Command: `cd backend && mvn test`
- Result: **713 passed, 0 failed, 0 errors, 0 skipped**
- New tests (in `ResidualPermissionServiceTest`, 9 cases — 7 spec TCs + 2 HTTP smoke):
  - TC-RPR-001 disabled+2 roles → revoke clears, 1 audit row ✓
  - TC-RPR-002 enabled user → revoke 400, no change ✓
  - TC-RPR-003 missing user → 404 ✓
  - TC-RPR-004 disabled+0 roles → 200, no audit ✓
  - TC-RPR-005 disable-user on enabled+role → 2 audits, role gone, enabled=false ✓
  - TC-RPR-006 disable-user on already-disabled+role → alreadyDisabled=true, only REVOKE audit ✓
  - TC-RPR-007 disable-user missing → 404 ✓
  - HTTP POST /api/compliance/users/{id}/revoke-roles returns ok JSON ✓
  - HTTP POST /api/compliance/disable-user/{id} returns ok JSON ✓
- Existing compliance suites still pass: ComplianceControllerTest (5/5), ComplianceAuthzTest (6/6) — confirms /api/compliance Tier A gating still covers the new POSTs.

## Frontend
- Command: `cd frontend && npm test -- --run`
- Result: **270 passed, 0 failed**
- New test: TC-RPRP-01 — 「一键回收」 button calls `revokeResidualRoles(userId)` and reloads (the second `getResidualPermissions` resolves to `[]`, asserting the residual row disappears).

## Caveats
- `/api/compliance/disable-user/**` does NOT have a dedicated authz unit test (still gated by the existing `/api/compliance` Tier A prefix). If a future refactor of AdminPaths breaks that prefix, the new endpoint would silently go un-gated. Mitigation noted inline in AdminPaths.
- Audit action codes `REVOKE_RESIDUAL` (15) and `DISABLE_USER` (12) fit AuditLog.action `length=16`. Do not rename longer without widening the column.
