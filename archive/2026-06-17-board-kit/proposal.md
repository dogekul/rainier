# v0.0.22-board-kit — 看板 UI 基座 (shared dashboard vocabulary)

> Baseline: tag `v0.0.21-admin-authz` / commit e00a522. Part of the **structural v1.0** build
> (设计蓝图 cycle #1). Reference 飞书项目, extreme ease-of-use ([[rainier-v1-design-language]]).

## Why

v1.0 adds three 飞书项目-style 仪表盘 panels (PM 项目驾驶舱 / 团队负责人面板 / 业务方 lite). If each
re-invents its bars/colors/empty-states, v1.0 reads as three divergent pages and the later "polish" cycle
becomes a real refactor. Building the shared vocabulary **first** makes every panel pure composition
(~250 LOC) and visually identical — the single biggest lever for "one coherent,极致好用 product".

## What Changes (frontend-only; 0 backend / 0 endpoints / 0 tables / 0 authz)

- **4-tier status color tokens** in `tokens.ts` + `global.css`: `--rainier-status-{red,yellow,green,gray}`
  (+ `-bg` tints). 红=逾期/BLOCKED/MISSED, 黄=进行中/待办/PLANNING, 绿=DONE/DELIVERED/REACHED, 灰=草稿/CANCELLED/CLOSED.
- **Pure utils** `src/utils/board.ts`: `statusColor(status)→tier`, `groupByStatus(items,getStatus)`,
  `isOverdue(dateStr,today)`, `todayISO(date?)`. Unit-tested in isolation.
- **Components** `src/components/board/*`: `StatusBar` (plain div %-width stacked distribution / ratio bar,
  no chart lib), `DashboardCard` (Card shell + standard title + extra slot), `StatusChip` (status→tinted chip),
  `OwnerChip` (avatar-initial + name), `EmptyState` (friendly line + single primary CTA). All with
  `data-testid` hooks + Vitest specs.

## Capabilities

- Modified: `frontend-scaffold` (adds the board UI kit + status tokens + board utils).

## Impact

- Frontend: `tokens.ts`, `global.css`, `tokens.test.tsx` (+1 assertion), new `utils/board.ts` (+test),
  new `components/board/{StatusBar,DashboardCard,StatusChip,OwnerChip,EmptyState,index}.tsx` (+css, +tests).
- No page/route/nav yet — this cycle ships the kit + specs only (consumed by v0.0.23+).

## Success Criteria

- [ ] 4 status color tokens (+bg) exposed on `:root`; a token test asserts them.
- [ ] `statusColor` maps every entity status to the right tier (default gray); `groupByStatus` counts in
      first-seen order; `isOverdue` is pure date compare (null→false).
- [ ] `StatusBar` renders one fill per segment with width = count/denominator, colored by tier; empty when total 0.
- [ ] `DashboardCard`/`StatusChip`/`OwnerChip`/`EmptyState` render with testids; EmptyState fires its CTA.
- [ ] tsc + eslint + full vitest green (77+106... baseline 106 + new board specs).
