# Spec: B7 residual-permission-recycle

## Entities
NONE — pure cleanup writes against `rainier_user` and `rainier_user_role`.

## Endpoints

### POST /api/compliance/users/{id}/revoke-roles
- Auth: admin (Tier A, `/api/compliance` already)
- Body: none
- Preconditions:
  - user must exist (else 404)
  - user.enabled MUST be false (else 400 — only disabled users qualify for residual recycle)
- Action:
  - load all UserRole rows by userId
  - hard-delete every row (`userRoleRepo.deleteAll(list)`)
  - write 1 AuditLog row: actor=current, entityType=USER, entityId=userId, action=REVOKE_RESIDUAL_ROLES, summary="REVOKE_RESIDUAL_ROLES USER#<id> roleIds=[..]"
- Response: 200 `{ ok:true, revokedCount: <int> }`
- Errors: 401/403 (authz), 404 (user not found), 400 (user still enabled)

### POST /api/compliance/disable-user/{id}
- Auth: admin (Tier A)
- Body: none
- Action:
  - load user (404 if missing)
  - if enabled=true → set enabled=false + saveAndFlush + audit DISABLE_USER
  - then ALWAYS run the same revoke logic as above (audit REVOKE_RESIDUAL_ROLES if any rows deleted)
  - idempotent — if already disabled with no roles, returns ok with revokedCount=0
- Response: 200 `{ ok:true, revokedCount: <int>, alreadyDisabled: <bool> }`
- Errors: 401/403, 404

## AdminPaths
`/api/compliance` is already Tier A → no change required. Recorded here so future AdminPaths refactors keep it.

## AuditLog
- Two new action constants used: `REVOKE_RESIDUAL_ROLES`, `DISABLE_USER`. They are free-form VARCHAR(16) strings; no new enum required (AuditAction.* is `String` constants only). Both fit ≤16.

## Test Cases
### Backend (ResidualPermissionServiceTest + Controller)
- TC-RPR-001: disabled user with 2 roles → POST revoke-roles → 200, revokedCount=2, UserRole rows gone, AuditLog has 1 REVOKE_RESIDUAL_ROLES row
- TC-RPR-002: enabled user → POST revoke-roles → 400, no UserRole touched
- TC-RPR-003: missing user → POST revoke-roles → 404
- TC-RPR-004: disabled user with 0 roles → POST revoke-roles → 200, revokedCount=0, audit NOT written (nothing to revoke)
- TC-RPR-005: POST disable-user on enabled user with 1 role → 200, user.enabled=false, role gone, 2 audit rows
- TC-RPR-006: POST disable-user on already-disabled user with 1 residual role → 200, alreadyDisabled=true, role gone, 1 audit row (REVOKE only)
- TC-RPR-007: POST disable-user on missing user → 404

### Frontend (CompliancePage)
- TC-RPRP-01: residual row shows 「一键回收」 button; click triggers revokeResidualRoles + reload (mocked)
