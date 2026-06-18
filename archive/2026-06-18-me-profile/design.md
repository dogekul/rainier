# Design — v0.0.40-me-profile

> Baseline: tag `v0.0.39-review-queue` / commit 6e7d049. Gate 1 decisions P1–P4 locked.

## Context

`GET /api/auth/me` (MeResponse) carries id/username/name/roles[]/projects[] — project-level only, NO
org membership / position / manager / contribution. The audit (#9) names this the team-member growth
gap. All the data exists: UserOrganization (membership + HEAD relation, hard-delete via `leftAt`),
Position (name/category), Story.ownerUserId, Task.assigneeUserId. The `/api/me/*` self-scoped substrate
(MeTeamService / PortfolioController) is the pattern to mirror — token-gated, all-users.

## Decisions

### D1 — NEW capability `me-profile`; self-scoped read-model
`GET /api/me/profile` (all-users). New `MeProfileController` (`@RequestMapping("/api/me")`,
`currentUsername(request)` from `AuthController.ATTR_USERNAME`, 401 when missing) + `MeProfileService`.
Mirrors entity-portfolio precedent (a /api/me read-model gets its own capability). P1: self-only; a
subordinate view (`/api/users/{id}/profile` gated 本人+直接上级) is a documented follow-up.

### D2 — `ProfileResponse` shape (nested DTOs)
```
ProfileResponse { userId, loginName, name, positionName, positionCategory,
                  memberships: [Membership{organizationId, organizationName, organizationType, role, isPrimary}],
                  manager: Manager{userId, name, loginName} | null,
                  ownedStoryCount, assignedTaskCount }
```
`Membership` / `Manager` are static nested classes on `ProfileResponse` (Jackson-serialized). Position
enriched via `positionRepo.findById` when `user.positionId != null` (UserService.enrich pattern).

### D3 — memberships = active assignments
`UserOrganizationRepository.findByUserIdAndLeftAtIsNull(userId)` (NEW) → all active (leftAt null)
memberships. Orgs batch-loaded via `orgRepo.findAllById(orgIds)` (no N+1); soft-deleted orgs drop out
(`@Where`). `role` serialized as the enum name (HEAD/MEMBER); `isPrimary` from the row.

### D4 — manager = walk up org tree for first non-self active HEAD (P3)
Start at the user's primary org (`isPrimary=true` membership; fall back to first membership if none).
At each level: `findByOrganizationIdAndLeftAtIsNull(orgId)` → filter `role==HEAD && userId != me` →
first such is the manager. If none, climb to `org.parentId` and repeat (depth-capped at 8 to bound the
walk). Load that user → `Manager`. Returns null if no non-self HEAD is found up to the root.
- *Why walk up*: a team HEAD's 直接上级 is the parent-org (department/domain) HEAD, not themselves.

### D5 — contribution = two lean counts (P4)
`ownedStoryCount = storyRepo.countByOwnerUserId(me.id)` (exists); `assignedTaskCount =
taskRepo.countByAssigneeUserId(me.id)` (NEW repo method). Both honor `@Where(del_flag=0)`. Richer
breakdowns (by-status, this-week) are a follow-up.

### D6 — degrade when token subject has no User
If `findByLoginName(username)` is empty (e.g. sub="system"), return a ProfileResponse with
loginName=username, name=null, empty memberships, null manager, counts 0 — never 500 (mirrors MeService
degrade + MeTeamService empty-list behavior).

### D7 — frontend dedicated「我的档案」page (P2)
`api/profile.ts:getMyProfile()` → `client.get('/me/profile')`. `ProfilePage` at `/profile`: identity
`DashboardCard` + contribution `StatTiles` + memberships list (org + type + role `StatusChip` + primary
mark) + manager `OwnerChip` + `EmptyState` for no-org. Nav item「我的档案」(icon `badge`, end:true) in the
all-users 工作台 group; route in AppRoutes; NOT in `isAdminPath` (navGuardConsistency auto-pins).

## Architecture / Data flow

```
GET /api/me/profile → MeProfileService.profileOf(username)
  ├─ user = findByLoginName(username)  (null → degraded profile, D6)
  ├─ position: positionRepo.findById(user.positionId)?  → name/category
  ├─ memberships: userOrgRepo.findByUserIdAndLeftAtIsNull(uid) + orgRepo.findAllById (batch)
  ├─ manager: walk up from primary org → first non-self active HEAD (D4)
  └─ counts: storyRepo.countByOwnerUserId + taskRepo.countByAssigneeUserId
ProfilePage /profile → getMyProfile() → board-kit
```

## Risks / Trade-offs

| Risk | Mitigation |
|---|---|
| manager walk infinite loop (cyclic parentId) | depth cap 8 + visited not needed (tree); bounded |
| N+1 on memberships | orgs batch findAllById; manager is a bounded up-walk (≤8 single finds) |
| no primary org | fall back to first active membership as walk start; else manager=null |
| token sub w/o User | D6 degrade, never 500 |
| 存量数据 | read-only aggregation; zero writes; standing 约束 trivially honored |
| Java 8 | no Set.of / no-arg orElseThrow / var; Docker temurin-8 gate |
