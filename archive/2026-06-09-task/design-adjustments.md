# Design Adjustments — v0.0.11-task

Phase 3-5 deviations from the Phase-2-locked design.

## A. Spec / Capability adjustments (carry into Phase 6 spec merge)

### A1. entity-task (NEW) — full capability merge
- Merge `specs/entity-task/spec.md` (6 Requirements / 19 Scenarios) as canonical `specs/entity-task/spec.md`.
- 5-state machine + 3 cross-layer consistency guards as documented.

### A2. entity-project — DELETE FK chain append Task
- Merge change-local Scenarios: `软删 Project FK 保护扩展加 Task 引用`. Chain order documented: Requirement → UserRole → Task.

### A3. frontend-scaffold — Sider 6 项 + /pm/tasks + TasksPage + TaskEditDrawer 联动
- Sider 5 项 → 6 项 (任务 第 3 位). New /pm/tasks route + TasksPage + TaskEditDrawer with Project/Sprint/Story/Assignee 联动级联 select.

## B. Phase 4 BUILD adjustments (Phase 5 Step 0.3 reconciled)

### B1. PA-1: enrich batch budget 7 → 6 (Requirement absent from TaskDetail)
- **Phase 2 design**: 5-stage batch enrich (user/sprint/story/requirement/project) → 2 page + 5 batch = 7 SELECTs.
- **Phase 4 reality**: TaskDetail does NOT surface any Requirement field. Adding the Requirement batch would be a dead query (no consumer). Implemented 4-stage (user/sprint/story/project) = 2 page + 4 batch = 6.
- **Propagation landed** in Phase 5 Step 0.3: proposal Success Criteria / design.md Decision 6 + 10 / specs/entity-task/spec.md Req 6 / test-plan.md TC-PERF-TSK-001 / slices.md M10 / tasks.md 2.3+9.1 / TaskService.java line comment all updated to `= 6`.

### B2. PA-2: Field count `22/23 → 24` (text typo, array unchanged)
- The TaskDetail field set array always had 24 items (11 business + 9 enrichment + 4 audit).
- Text counts in 5 docs said "22-field" / "23 字段" — mechanical typo.
- **Fixed** in Step 0.3: 5 doc paths reconciled to "24-field" / "24 字段".

### B3. PA-3: Cross-layer guard message refinement (Code-M1)
- TaskService.create: when sprint exists but its parent Requirement is soft-deleted, distinguish from "projectId mismatch". New message `"sprint parent requirement missing: sprintId=<id>"`. Preserves "sprint not in project" for the actual cross-project case.

## C. Phase 5 Step 0 review fixes (this round)

| ID | Source | Fix |
|---|---|---|
| Test-C1 | Test reviewer | Added TC-FES-TSK-001 to `AppLayout.test.tsx`: asserts Sider has 6 items + 任务 第 3 位 + /pm/tasks link + order project<sprint<task<demand. Closes the stale v0.0.9/v0.0.10/v0.0.11 cumulative gap. |
| Docs-C1 / Test-H1 | Test + Docs reviewers | PA-1 propagation (7→6) across 6 source docs + TaskService comment. |
| Docs-H2 | Docs reviewer | PA-2 propagation (22/23→24) across 5 doc paths. |
| Code-M1 | Code reviewer | PA-3 distinct "sprint parent requirement missing" message. |

## D. Recorded-but-unfixed Step 0 findings (rationale)

Aggregate after Step 0.3: **C:0 H:1 M:6 L:9** — H at limit (Test-H2 recorded), M over (12 over 10) but family-pattern.

| Severity | ID | Description | Reason recorded only |
|---|---|---|---|
| H | Test-H2 | TC-TSK-010 23-field loop tests populated-fixture only; doesn't separately verify sparse-task contract | Scope creep for v0.0.11; recommend adding sparse-task TC in v0.0.12 |
| M | Code-M2 | Legacy Story.projectId=null branch — refused link is correct behavior but message could be clearer | Defensive — no live data observed; document |
| M | Code-L4 | TaskEditDrawer client-side filter size=100 caps real-world projects with >100 sprints | Already noted in test-plan 🟡; v0.1 server-side filter |
| M | Test-M1 | TC-TSK-013 unassign assertion relies on doesNotExist() matching null | Functionally correct given Jackson + enrichment side-evidence; brittle but no observed regression |
| M | Test-M2 | No UserRole+Task fixture (only Requirement+Task tested for FK chain order) | Coverage gap with low blast radius; chain order preserved by mechanical append |
| M | Test-M3 | TC-PERF-TSK-001 seeds 5 Requirements that aren't read in enrich anymore | Cleanup only; no test correctness issue |
| M | Docs-M1 | Canonical frontend-scaffold needs v0.0.11 merge (Phase 6 DELIVER task) | Will land in Phase 6 deliver checklist |
| L | various (9 items) | minor polish / TC-ID style / comment refinement | Informational |

## E. Phase 6 DELIVER TODO checklist

- [ ] Archive `changes/2026-06-09-task/` → `archive/2026-06-09-task/`
- [ ] Merge specs:
  - NEW `specs/entity-task/spec.md` (copy from change)
  - MOD `specs/entity-project/spec.md` (insert v0.0.11 — Task FK guard Scenarios)
  - MOD `specs/frontend-scaffold/spec.md` (update Sider Requirement to v0.0.11 "6 items, 任务 第 3" + /pm/tasks Scenario)
- [ ] Commit + tag `v0.0.11-task` (no push)
