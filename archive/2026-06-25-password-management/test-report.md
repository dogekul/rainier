# Test Report — B3 password-management (v0.0.76)

## Backend
`cd backend && mvn test`

- **Tests run: 668, Failures: 0, Errors: 0, Skipped: 0** → BUILD SUCCESS
- New: `com.rainier.password.PasswordServiceTest` — 15 tests, all green
  - Service: change own (3), admin reset (2), forgot/reset happy + silent + token edge (5)
  - HTTP: anonymous forgot/reset whitelist (2), authenticated `/api/me/password` (1), admin `/api/admin/users/{id}/reset-password` (1) — admin-authz OFF in default test profile, AdminAuthorizationTest still green with Tier-A `/api/admin` gating
- Touched: bumped `LegacyProductCategoryCleanupTest` rainier_* table count 33 → 34 for the new `rainier_password_reset_token` table

## Frontend
None — no UI in this slice.

## Caveats
- forgot-password DOES NOT send email — token only logged at INFO ("[password-reset] issued token=…"). Production must replace with mailer.
- No rate-limiting on forgot/reset (anti-enumeration handled via silent 200, but a fast attacker can still issue many tokens).
- `/api/admin` Tier-A gating means **all future** `/api/admin/**` endpoints will auto-require admin. Anyone adding a new admin route under that prefix gets the gate for free; conversely, non-admin endpoints must NOT be added under `/api/admin`.
