# Design Adjustments — v0.0.10-sprint

Phase 3-5 deviations from the Phase-2-locked design.

## A. Spec / Capability adjustments (carry into Phase 6 spec merge)

### A1. entity-sprint (NEW) — full capability merge
- Merge `specs/entity-sprint/spec.md` (5 Requirements / 17 Scenarios) as canonical
  `specs/entity-sprint/spec.md`.
- Status machine 4 items locked in proposal §I row 6.

### A2. entity-story — refactor (drop requirementId, add sprintId)
- Replace canonical entity-story Scenarios that reference `requirementId` with sprint-based ones.
- Field-set grows by 4 (sprintId / sprintCode / sprintName / sprintStatus) and the DB column for `requirementId` is documented as a v0.0.11+ DROP target.

### A3. entity-requirement — DELETE FK swap + sprintCount enrichment
- Merge change-local block: DELETE FK now blocks on Sprint references; `storyCount` removed from RequirementDetail, replaced by `sprintCount`. v0.0.9 TC-REQS-001b ordering test (demand check fires before story check) is preserved with story → sprint substitution.

### A4. frontend-scaffold — Sprint menu + page + drilldown reshape
- Sider 「需求管理」: 4 items → 5 items (insert Sprint between 项目 and 诉求).
- New /pm/sprints route + SprintsPage (read-only browser; CRUD via Requirement drilldown).
- RequirementsPage drilldown: StoryListPanel → SprintListPanel; "Story 数" column → "Sprint 数".
- SprintsPage row-expand → StoryListPanel (reusing v0.0.9 component, now keyed by sprintId).
- StoryEditDrawer locked-display swap: requirement-only → sprint+requirement two-line locked panel.

## B. Implementation-level adjustments

### B1. PA-1 / Decision 2 Build addendum: Hibernate did not honor `columnDefinition="BIGINT"`
- **Phase 2 expectation**: Entity `@Column(name="sprint_id", nullable=false, columnDefinition="BIGINT")` would cause Hibernate to emit `ADD COLUMN sprint_id BIGINT` (nullable). Migration would then ALTER to NOT NULL after data fill.
- **Reality (M13 E2E)**: Hibernate 5.6 emitted `ADD COLUMN sprint_id BIGINT NOT NULL` regardless. MySQL auto-filled existing `rainier_story` rows with `sprint_id = 0`.
- **Fix landed in Phase 4**: orphan detection broadened to `sprint_id IS NULL OR sprint_id = 0 OR sprint_id NOT IN (SELECT id FROM rainier_sprint WHERE del_flag = 0)`. This handles both the (unlikely) NULL case and the (actual) MySQL `=0` auto-default uniformly.
- **Code-H2 follow-up landed in Phase 5**: ALTER to NOT NULL no longer gated on `storiesMigrated > 0`; instead probes INFORMATION_SCHEMA for current nullability and runs ALTER if still nullable. Decouples DB-invariant restoration from migration counts so a partial-success first boot will heal on the next.

### B2. PA-2: MySQL INFORMATION_SCHEMA case sensitivity
- Production MySQL on Linux stores `TABLE_NAME` lowercase; uppercase comparisons silently miss.
- Migration uses `LOWER(TABLE_NAME) = 'rainier_story'` AND `LOWER(COLUMN_NAME) = '<col>'`.

### B3. PA-3: H2/MySQL ALTER syntax fallback
- Migration tries MySQL `MODIFY COLUMN` first; falls back to H2 `ALTER COLUMN … SET NOT NULL` on exception. Identical pattern for relax (NULL) and upgrade (NOT NULL).
- Phase 5 Code-H3 hardened: fallback wraps the primary failure via `addSuppressed` so root cause survives in stack traces.

### B4. Phase 5 Code-H1: default-Sprint INSERT race hardened
- `SELECT id FROM rainier_sprint WHERE code = ?` was vulnerable to `NonUniqueResultException` if a prior soft-deleted Sprint shared the code. Fixed by adding `AND del_flag = 0 ORDER BY id DESC` + `setMaxResults(1)`.

### B5. Phase 5 Test-C1 + H1 + H2: migration test strengthened
- TC-SPR-MIG-001 now asserts (a) Story.sprintId migrated, (b) DB `IS_NULLABLE = 'NO'`, (c) per-row INFO log line, (d) summary INFO log with canonical wording.
- TC-SPR-MIG-002 now actually exercises the legacy migration path (seeds orphan, runs migration twice), asserts no duplicate Sprint, no summary log on 2nd run, column stays NOT NULL.

### B6. Phase 5 Test-H3: new component tests
- `SprintEditDrawer.test.tsx`: 3 cases (default owner, owner change in edit, form error on missing owner).
- `SprintsPage.test.tsx`: 1 case (row expand renders StoryListPanel keyed by sprintId + stories-new-btn).

## C. Documentation adjustments

- Code-M4 (RequirementsPage ConfirmDialog message): "Story" → "Sprint" wording updated.
- Recorded but unaddressed: design.md Decision 2 prose still describes pre-empirical-finding state; .stdd.yaml `phase.build.notable_fixes` captures the truth. v0.0.11 cleanup will harmonize.

## D. Phase 6 deliver TODO checklist

- [ ] Move `changes/2026-06-08-sprint/` → `archive/2026-06-08-sprint/`; mark `.stdd.yaml` phase archived.
- [ ] Merge `entity-sprint/spec.md` as NEW capability under `specs/`.
- [ ] Merge change-local `entity-story/spec.md` block: replace `requirementId` Scenarios with sprintId; remove v0.0.9 "Story → Requirement direct" Scenarios.
- [ ] Merge change-local `entity-requirement/spec.md` block: replace `storyCount` with `sprintCount`; replace TC-REQS-001 Scenarios (Sprint FK).
- [ ] Merge change-local `frontend-scaffold/spec.md` block: Sider 5-item; /pm/sprints route; StoryEditDrawer Sprint locked display.
- [ ] Git commit + tag `v0.0.10-sprint`.

## E. Recorded Step-0 unresolved findings

Captured in `pending-adjustments.md §C` — 1 H (Code-H3 root-cause masking, now mitigated via addSuppressed), 12 M, 13 L. None block delivery.
