# v0.0.30-portfolio-map — 项目地图 (scoped portfolio landing page)

> Baseline: tag `v0.0.29-portfolio-consumers`. 结构层第二波 cycle #3 — one page, many roles.

## Why
The audit's PMO(0%)/高管(0%)/领域(不可能)/部门(不可能) all need a portfolio landing. One scoped page over
the v0.0.28 substrate serves them all via a scope toggle — flips four near-0 roles to "has a view".

## What Changes (frontend only)
- New `pages/Portfolio/PortfolioPage.tsx` + route `/portfolio` + new all-users nav group 「数据看板」→「项目地图」.
- Scope toggle 我的项目(mine) / 我带的团队(led, org-subtree footprint) / 全公司(all). getPortfolio(scope) →
  健康分布 StatusBar (红/黄/绿/灰 counts) + 项目列表(红→绿排序, RYG chip + open/overdue/blocked/逾期里程碑 +
  drill to /pm/tasks?projectId=). Empty state per scope.

## Success Criteria
- [ ] 项目地图 page renders rows worst-first + summary; scope toggle refetches; empty state per scope.
- [ ] /portfolio all-users (nav-guard consistent). tsc/eslint/vitest green (149→153).
