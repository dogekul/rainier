# Spec: B3 password-management

## Entities (NEW)

### PasswordResetToken — `rainier_password_reset_token`
| field | type | nullable | notes |
|---|---|---|---|
| id | BIGINT PK | no | inherits from BaseEntity |
| user_id | BIGINT | no | FK target rainier_user(id), not enforced via JPA relation (loose) |
| token | VARCHAR(64) | no | UUID hex; service layer guarantees uniqueness on insert |
| expires_at | TIMESTAMP | no | createdAt + 1h |
| used_at | TIMESTAMP | yes | NULL until used; non-NULL → token cannot be reused |

Soft delete: NOT applied (tokens are short-lived; mark `used_at` instead).

## Endpoints

### POST /api/me/password
- Auth: token-gated (identity baseline)
- Body: `{currentPassword, newPassword}`
- Validates: both non-blank; newPassword.length ≥ 8; currentPassword matches stored BCrypt
- On success: 200 `{ok:true}`; passwordHash overwritten
- Errors: 400 (validation), 401 (no token), 401 (currentPassword mismatch — keep generic)

### POST /api/admin/users/{id}/reset-password
- Auth: admin (Tier A via AdminPaths /api/admin)
- Body: `{newPassword}` (≥ 8 chars)
- On success: 200 `{ok:true}`; passwordHash overwritten regardless of old value
- Errors: 400 (validation), 401/403 (authz), 404 (user not found)

### POST /api/auth/forgot-password
- Auth: NONE (whitelist; alongside /api/auth/login)
- Body: `{loginName, email}`
- Behaviour: if user exists AND emailAddress matches (case-insensitive trim) → insert PasswordResetToken (UUID, 1h TTL). Always log "[password-reset] issued token=…". If not matching → silently no-op.
- Always returns 200 `{ok:true}` (anti-enumeration). 400 only for blank body fields.

### POST /api/auth/reset-password
- Auth: NONE (whitelist)
- Body: `{token, newPassword}` (newPassword ≥ 8)
- Validates: token exists, not expired (`expires_at > now`), not used (`used_at IS NULL`)
- On success: overwrites passwordHash; marks `used_at = now`; returns 200 `{ok:true}`
- Errors: 400 (validation / token invalid)

## AdminPaths
Add new Tier A base: `/api/admin` (covers `/api/admin/users/{id}/reset-password` and any future /api/admin/* endpoint).

## SecurityWhitelistPaths
Add: `POST /api/auth/forgot-password`, `POST /api/auth/reset-password`.

## Test Cases
- TC-PWD-001: change own password — correct current → 200 + login with new
- TC-PWD-002: change own password — wrong current → 401
- TC-PWD-003: change own password — newPassword too short → 400
- TC-PWD-004: admin reset → 200 + login with new
- TC-PWD-005: admin reset — user not found → 404
- TC-PWD-006: forgot + reset happy path — 200, 200; login with new
- TC-PWD-007: forgot — wrong email → 200 (silent), no token row created
- TC-PWD-008: reset — invalid token → 400
- TC-PWD-009: reset — expired token → 400
- TC-PWD-010: reset — token re-use → 400
