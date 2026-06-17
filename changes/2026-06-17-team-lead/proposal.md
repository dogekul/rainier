# v0.0.25-team-lead — 团队负责人面板

> Baseline: tag `v0.0.24-me-self-scope`. Structural v1.0 cycle #4. Consumes the v0.0.24 `/api/me/*`
> endpoints + board-kit. 飞书项目「工作量 by 负责人 + 项目红黄绿」analog; all-users (a lead ≠ admin).

## Why

A frontline 我的工作台 shows "my" work; an org HEAD needs a supervisory rollup of their team's load and
their projects' health — without admin screens. This panel sits between the two: member load + a
rule-based project red/yellow/green ranking, pure entity aggregation (no AI).

## What Changes (frontend; 0 backend — reuses v0.0.24 + list endpoints)

- Pure `src/utils/ryg.ts` (unit-tested): `ryg({openCount,overdueCount,anyBlocked})` (GRAY no-open / RED
  blocked-or-overdueRatio>0.3 / YELLOW some-overdue / GREEN clean), `loadTier(openTasks)` (<=3 绿 / 4-7 黄 /
  >7 红), `isOpenTaskStatus`, named-constant thresholds, `RYG_ORDER`.
- New `pages/TeamLead/TeamLeadPage.tsx` + route `/team` + a `团队负责人面板` item in the all-users 工作台
  nav group (after 我的工作台). Top 团队 selector auto-selects the sole led team (0 clicks) else defaults [0].
- Two board-kit cards: **成员负载** (per-member open-task StatusBar threshold-colored + open-story count, row
  → `/pm/tasks?assigneeUserId=`), **项目红黄绿** (lead's projects sorted 红→黄→绿→灰 by `ryg`, row →
  `/pm/tasks?projectId=`). EmptyState '你当前不是任何团队的负责人' when led-teams is [].

## Capabilities

- Modified: `frontend-scaffold` (new panel + route + nav + ryg util).

## Impact

- Frontend: new `utils/ryg.ts` (+test), `pages/TeamLead/*` (+test), `AppRoutes`/`AppLayout` (1 route + 1
  nav item). Consumes `api/teamLead.ts` (v0.0.24) + reused `listTasks`/`listStories`.
- `/team` is all-users — NOT added to `isAdminPath`, so navGuardConsistency stays green.

## Success Criteria

- [ ] Sole led-team auto-selected (no dropdown); multiple → selector defaulting [0]; led-none → empty state.
- [ ] Member open-task count colored by threshold (5→黄, 1→绿); each member row drills to assignee-filtered tasks.
- [ ] Project RYG: overdueRatio 0.4 → 红 sorted first; no-open → 灰 last; row drills to project-filtered tasks.
- [ ] tsc + eslint + full vitest green (126→139). `/team` all-users (nav-guard consistent).
