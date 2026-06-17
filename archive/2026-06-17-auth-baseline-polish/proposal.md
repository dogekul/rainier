# v0.0.27-auth-baseline-polish — 全局认证基线 + v1.0 面板打磨

> Baseline: tag `v0.0.26-demand-lite`. Structural v1.0 cycle #6 (final). Closes the last structural gap.

## Why

Before this, only `/api/auth/me` required a token — every other `/api/**` was reachable unauthenticated
(v0.0.21 added admin authz on top, but the identity baseline was missing). This closes that gap: every
API call must carry a valid token. Polish converges the v1.0 panels into one coherent look.

## What Changes

### (a) Auth baseline (backend)
- `SecurityFilter` now resolves identity for ANY path (valid Bearer → `rainier.username` attr), and —
  when `app.security.require-all-users-token.enabled=true` — gates every `/api/**`: missing/invalid token
  → uniform JSON **401** (`{"message":...}`), before the request reaches any controller/interceptor.
- Whitelist: `POST /api/auth/login` + `GET /api/health` (+ CORS preflight). Matrix-param bypass avoided
  via `UrlPathHelper` semicolon-stripped URI (same defense as v0.0.21).
- Filter runs BEFORE `AdminAuthorizationInterceptor` → 401 (identity) precedes 403 (authz).
- Flag `true` in `application.yml` (prod), explicit `false` in `application-test.yml` so the legacy
  unauthenticated tests stay green; `AuthBaselineTest` flips it on (+ admin-authz) to verify the gate
  and the 401-before-403 ordering.

### (b) Polish (audit — no risky refactor)
- The three v1.0 panels already compose the board-kit (StatusBar/DashboardCard/chips/EmptyState) +
  status tokens → consistent by construction. Nav is coherent (工作台: 我的工作台/提个诉求/团队负责人面板;
  需求管理: 项目驾驶舱 first). The axios **401 interceptor already exists** (client.ts: clears token →
  /login) — confirmed by audit, no change. Legacy pages' bespoke colors are intentionally left untouched
  (out-of-scope churn); v1.0 surfaces are the consistent set.

## Capabilities

- Modified: `backend-authz` / `auth-placeholder` (identity baseline) + `frontend-scaffold` (polish audit).

## Impact

- Backend: `SecurityFilter` (baseline gate + always-resolve identity), `application.yml` /
  `application-test.yml` (flag). `AuthBaselineTest` (7). 0 tables / 0 endpoints.
- Frontend: none (audit only).

## Success Criteria

- [ ] With flag on: no/invalid token on any `/api/**` → 401; valid token → passes; login + health whitelisted.
- [ ] No-token admin endpoint → 401 (not 403); non-admin token → 403 (identity precedes authz).
- [ ] Flag off (test profile) → legacy unauthenticated tests unchanged (backend 377→384 green, checkstyle clean).
- [ ] E2E (real Tomcat, flag on): unauth /api/projects → 401; with token → 200; health/login ok; existing data intact.
