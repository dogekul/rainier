# Design — v0.0.42-po-inbox

> Baseline: tag `v0.0.41-admin-compliance` / commit 55bf6a8.

## Context

PO is the weakest-covered role (six-segment chain, only "我的 Story" works). Data exists: Demand
(soft-delete, status PENDING/IN_REVIEW/CONVERTED/DONE/CLOSED), DemandRequirementLink (demandId↔
requirementId, hard-delete), Requirement (ownerUserId, status, expectedDate, soft-delete). The
`/api/me/*` self-scoped substrate is the pattern to mirror.

## Decisions

### D1 — NEW capability `me-inbox`; GET /api/me/inbox (all-users)
`MeInboxController @RequestMapping("/api/me")` + `MeInboxService`. Mirrors PortfolioController/MeProfile
(token-gated, not admin). `me == null` (token sub has no User) → empty InboxResponse (both lists empty),
consistent with the other me-services' degrade.

### D2 — InboxResponse shape
`InboxResponse{ unconvertedDemands:[InboxDemand], myRequirements:[InboxRequirement] }`.
`InboxDemand{id,title,priority,status,createTime}` (Demand has no enrichment today — keep lean).
`InboxRequirement{id,code,title,status,priority,expectedDate,projectId,projectName}` (projectName batch-enriched).

### D3 — unconverted demands = no link AND not terminal
`linkRepo.findAll()` → Set of linked demandIds; `demandRepo.findAll()` (honors `@Where del_flag=0`) →
filter `!linked.contains(id)` AND `status ∉ {DONE, CLOSED}`. App-scale data → in-memory filter is fine
(no fragile empty-IN-clause). Sorted by priority rank (URGENT→LOWEST), then createTime asc.
- *Why exclude terminal*: a DONE/CLOSED demand never converted is dead, not a triage item. CONVERTED
  demands normally carry a link so the link filter already drops them.
- *Why global (not submitter-scoped)*: a PO triages ALL incoming demands, not just their own. Finer
  per-product PO scoping is a follow-up.

### D4 — my requirements = ownerUserId filter
`RequirementRepository.findByOwnerUserId(meId)` (NEW derived query, honors `@Where`). projectName
enriched via one batch `projectRepo.findAllById(projectIds)`. Sorted by priority rank.

### D5 — frontend「需求收件箱」(all-users)
`api/inbox.ts:getInbox()`. `InboxPage` at `/inbox`: `StatTiles`(待处理诉求数 / 我的需求数) + two
`DashboardCard`s (待处理诉求 → rows link to `/pm/demands`; 我的需求 → rows link to `/pm/requirements`)
each with `EmptyState`. Reuse PRIORITY_LABELS / REQUIREMENT_STATUS_LABELS. Nav「需求收件箱」(icon
`inbox`) in the all-users 工作台 group; route in AppRoutes; NOT in isAdminPath (navGuardConsistency auto).

## Architecture / Data flow

```
GET /api/me/inbox → MeInboxService.inbox(username)
  ├─ me = findByLoginName (null → empty both)
  ├─ unconvertedDemands: linkRepo.findAll() demandIds; demandRepo.findAll() filter !linked && !terminal; sort
  └─ myRequirements: requirementRepo.findByOwnerUserId(me.id); batch projectName; sort
InboxPage /inbox (工作台 all-users) → getInbox() → board-kit
```

## Risks / Trade-offs

| Risk | Mitigation |
|---|---|
| demandRepo.findAll() unbounded | app-scale (tens); follow-up: paginate/Specification NOT-IN if it grows |
| me==null | empty both, never 500 |
| 存量数据 | read-only aggregation; zero writes; standing 约束 honored |
| Java 8 | Collectors.toList/toSet, explicit types, no Set.of/var/no-arg orElseThrow; temurin-8 gate |
