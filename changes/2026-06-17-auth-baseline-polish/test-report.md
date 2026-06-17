# Test Report — structural v1.0 (v0.0.22 → v0.0.27)

> Whole-v1.0 adversarial review (4 parallel dimension reviewers → verify each Critical/High) + per-cycle gates.

## Quality gates (final)
- Backend: **384** tests green (348 baseline → +6 me-self-scope +7 auth-baseline; +others), checkstyle clean.
- Frontend: **146** tests green (106 baseline → +40 across board-kit/cockpit/ryg/team-lead/demand-lite/nav-guard), tsc + eslint clean.
- E2E (real Tomcat, auth baseline ON): no-token `/api/projects` → 401; with token → 200; `/api/health` + login
  whitelisted; **matrix-param `/api/projects;x=1` → 401** (bypass closed); `/api/me/led-teams` reachable;
  existing data intact (19 tables, 2 users, 6 projects, 2 roles).

## Adversarial review — 15 findings; 2 adversarially CONFIRMED serious

### CONFIRMED-CRITICAL (pre-existing, NOT a v1.0 regression) — surfaced + follow-up spawned
**Credential-less login = self-asserted identity.** `POST /api/auth/login` issues a valid HS256 token for ANY
non-blank username with NO password/user check (`AuthController.login` → `AuthService.issueToken`, the v0
mock placeholder). Because `SecurityFilter` trusts the token `sub`, anyone can mint a token as a known org
HEAD / admin loginName and pass every server-side re-check (MeTeamService self-scope, AdminAuthorizationInterceptor).
→ The v0.0.24 HEAD re-check and v0.0.21 admin gate are only as trustworthy as the `sub`, which login does not
verify. This is the pre-existing mock auth (explicitly marked "replaced by a real identity provider later"), out
of structural-v1.0 scope. **Action: surfaced to the user as the #1 limitation; follow-up task spawned** (real
credential verification: password hash store + login lookup, gate the mock behind a non-prod profile). Also
related HIGH: `login_name` has no DB unique constraint under ddl-auto (Flyway disabled) — fold into the same
hardening track.

### CONFIRMED-HIGH (v1.0 bug) — FIXED
**Cockpit project-switch stale-response race.** `CockpitPage` load effect had no cleanup; a slow earlier load
could overwrite a newer project selection. **Fixed**: `fetchData` now returns data and the effect applies it
behind an `active` flag (mirrors the TeamLead guard); `reload` re-checks the current projectId before applying.

### Dispositioned (medium/low — accepted or tidied)
- TeamLead member load = a member's TOTAL open work (not team-scoped) — **intended** (a lead cares about a
  member's whole load); documented.
- DemandSubmit TC-DL-01 vacuous `queryByText(/来源/)` — **tidied** to `getAllByRole('combobox').length===1`
  (proves no submitter/status/source picker).
- AuthBaseline flag-OFF "legacy unchanged" — proven by the 384 green run under the flag-off test profile (the
  positive proof is that the whole suite passes with the gate code present but disabled).
- Cockpit MISSED-milestone counted as overdue / BLOCKED tasks only in overdue panel / two same-tier
  distribution segments adjacent — **accepted** (defensible: MISSED is a risk; legend disambiguates colors);
  candidates for a future polish pass.
- Several test-quality notes were confirm-only (MeTeam 403 re-check genuine; cockpit 2020/2099 dates robust).

## Conclusion
v1.0 structural layer is functionally complete and consistent (board-kit-unified, all-users panels, nav-guard
green). One confirmed v1.0 bug fixed (cockpit race). The dominant CRITICAL is the pre-existing mock-auth
placeholder — flagged as the next major track, NOT silently patched. No push (autonomous run stops at Gate 3).
