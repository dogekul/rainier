# Test Report — Role-Based Default Landing (H6, v0.0.113)

## RED

- Backend first run failed on all 6 scenarios with `No value at JSON path "$.defaultLandingPath"`.
- Frontend first run failed because Login still called `navigate("/", { replace: true })`.

## GREEN

- Backend command:
  - `cd backend && mvn -q -Dtest=AuthMeDefaultLandingTest test`
  - Result: 6 tests passed.
- Frontend command:
  - `cd frontend && npm test -- --run src/pages/Login/Login.test.tsx src/components/ProtectedRoute.test.tsx`
  - Result: 2 files passed, 5 tests passed.

## Coverage

- `GET /api/auth/me` now returns `defaultLandingPath`.
- Resolution order:
  - admin role -> `/sys/compliance`
  - PMO role -> `/pmo`
  - ARCHITECT role -> `/architect`
  - project owner -> `/pm/cockpit`
  - requirement owner -> `/inbox`
  - fallback -> `/`
- Login hydrates `me()` after token issuance and redirects to `defaultLandingPath`.
- `ProtectedRoute` redirects cold `/` entry to `defaultLandingPath` after hydration.

## Caveats

- `defaultLandingPath` is deterministic only; there is no user-customized landing page.
- The rule is intentionally backend-owned so role/path priorities stay consistent across clients.
