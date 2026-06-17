# v0.0.24-me-self-scope — 自助域团队端点 (/api/me/*)

> Baseline: tag `v0.0.23-pm-cockpit`. Structural v1.0 cycle #3 — the ONLY new backend in v1.0,
> isolated for its authz subtlety. Unblocks the team-lead panel (v0.0.25).

## Why

The team-lead panel needs "who is on my team". A non-admin org HEAD canNOT reuse `/api/user-organizations`
(Tier-A admin-gated since v0.0.21). So add two **token-scoped** (not admin) self-service endpoints that
let a HEAD read ONLY the teams they lead — with a server-side HEAD re-check so the panel can't read
arbitrary teams.

## What Changes (backend; 0 tables, 2 read endpoints; + a thin frontend api client)

- `SecurityFilter` now resolves the Bearer token → `rainier.username` attr for `/api/me/*` too (same
  identity path as `/api/auth/me`). No token → controller throws 401.
- `GET /api/me/led-teams` → `[{organizationId, organizationName, organizationType}]` for the caller's
  **active HEAD** memberships (role=HEAD AND leftAt IS NULL). Degrades to `[]` when the subject has no User.
- `GET /api/me/team-members?organizationId=` → active members `[{userId, name, loginName}]` of a team the
  caller HEADs; **403** (ForbiddenException) otherwise. v1 = direct members only (no recursive subtree).
- `UserOrganizationRepository` +3 derived queries (findByUserIdAndRoleAndLeftAtIsNull /
  existsByUserIdAndOrganizationIdAndRoleAndLeftAtIsNull / findByOrganizationIdAndLeftAtIsNull).
- Frontend `api/teamLead.ts` (LedTeam/TeamMember types + listLedTeams/listTeamMembers) — typed surface
  the v0.0.25 panel consumes. No page yet.

## Capabilities

- New: `backend-authz` is unchanged; this is a NEW small capability `entity-user-organization` MOD
  (self-scoped reads) + `auth-placeholder` MOD (SecurityFilter identity extended). No new tables.

## Impact

- Backend: new `com.rainier.me.{controller,service,dto}` (MeTeamController/MeTeamService/LedTeam/TeamMember),
  `SecurityFilter` (identity for /api/me/*), `UserOrganizationRepository` (+3 queries). `MeTeamControllerTest` (6).
- Frontend: new `api/teamLead.ts` (unused until v0.0.25).
- Not admin-gated (not in AdminPaths) — a plain HEAD reaches it; the HEAD re-check is the guard.

## Success Criteria

- [ ] HEAD of a team → led-teams returns it (id/name/type); HEAD of nothing → `[]`; MEMBER role not counted.
- [ ] team-members as HEAD returns active members, excludes left (leftAt set) ones.
- [ ] team-members by a non-HEAD → 403; no token → 401.
- [ ] backend suite green (371→377); checkstyle clean. Frontend tsc/eslint clean (teamLead.ts).
