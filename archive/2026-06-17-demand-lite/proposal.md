# v0.0.26-demand-lite — 业务方 lite 入口「提个诉求」

> Baseline: tag `v0.0.25-team-lead`. Structural v1.0 cycle #5 (effort S). 飞书项目 quick-create analog.

## Why

A non-technical business stakeholder shouldn't face the full admin-ish DemandsPage (table, filters,
status/source/closeReason). They just want to file a 诉求 in one screen. This is the business-side front door.

## What Changes (frontend; 0 backend — reuses POST /api/demands)

- New `pages/Demand/DemandSubmitPage.tsx` (single form, no usePaginated/Table/Drawer) + route
  `/demand-submit` + `提个诉求` in the all-users 工作台 nav group.
- Only 3 fields: 主题 (required) + 描述 (optional) + 优先级 (default 中). Submitter auto-defaults to the
  logged-in user, shown read-only (`提交人：<我>`) — no picker, no status/source fields.
- Submit reuses `createDemand` ({title, submitterUserId, priority, description?}); service fills
  status=PENDING/source=WEB. Success swaps the form for a green confirmation Card (title + #id + 再提一个 /
  查看我提交的). Guards null user.id (message, no submit). Submit disabled until 主题 is non-empty.

## Capabilities

- Modified: `frontend-scaffold` (new lite page + route + nav).

## Impact

- Frontend: new `pages/Demand/DemandSubmitPage.tsx` (+test), `AppRoutes`/`AppLayout` (1 route + 1 nav item).
- All-users — `/demand-submit` NOT in `isAdminPath` (nav-guard consistent).

## Success Criteria

- [ ] Minimal form: read-only submitter, default 优先级=中, no submitter/status/source fields; submit disabled when 主题 empty.
- [ ] Submit sends {title, submitterUserId=store id, priority} (description omitted when blank); success → confirmation with title + #id.
- [ ] 再提一个 resets; null user.id → message + no submit; create failure → inline error, fields kept.
- [ ] tsc + eslint + full vitest green (139→146).
