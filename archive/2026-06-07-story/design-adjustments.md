# Design Adjustments — v0.0.9-story

Phase 3-5 deviations from the Phase-2-locked design.

## A. Spec / Capability adjustments (carry into Phase 6 spec merge)

### A1. entity-story (NEW) — full capability merge
- Merge `specs/entity-story/spec.md` (4 Requirements / 16 Scenarios) as the canonical
  `specs/entity-story/spec.md`.
- All Gate-1 decisions (8 items) preserved verbatim; no Phase 4-5 reversal.

### A2. entity-requirement — DELETE FK + storyCount enrichment
- Merge change-local `specs/entity-requirement/spec.md` MODIFIED block:
  - DELETE FK protection adds Story-reference check after demand_requirement check (intentional ordering).
  - `storyCount` enrichment field on GET single + list paths.
- Phase 5 Test-H1 added TC-REQS-001b to lock the FK-check ordering (`demand_requirement` first, then `story`).

### A3. frontend-scaffold — drilldown
- Merge change-local `specs/frontend-scaffold/spec.md` MODIFIED block:
  - RequirementsPage drilldown + Story 数 column.
  - StoryEditDrawer + StoryListPanel components.
  - Owner-mutability + form-error parity (sibling of v0.0.8.1 Code-M7 pattern).

## B. Implementation-level adjustments

### B1. PA-1: RequirementsPage `onStoryCountChange` memoization
- See `pending-adjustments.md` PA-1. Inline `onCountChange={() => void list.refetch()}`
  caused infinite render loop; fixed via `useCallback(() => void
  refetchRequirements(), [refetchRequirements])`.
- No spec impact — implementation detail not specified at scenario granularity.

### B2. Phase 5 Code-H1: StoryEditDrawer remount key
- Code review found a stale-state leak between edit→close→reopen-for-new cycles.
- Fixed by keying the drawer with `editing?.id ?? 'new'` in StoryListPanel — forces
  React remount when the editing target flips, triggering the useEffect-driven state
  reset from a clean slate.
- No spec impact.

### B3. Table component — added expandable row support
- Added optional `isExpanded` + `renderExpanded` props to the shared Table component to
  support RequirementsPage drilldown.
- This is a UI primitive extension, not a spec change. Other pages can opt in by
  passing the new props.

### B4. Phase 5 Test-H1 fix — added TC-REQS-001b
- Combined demand_requirement + Story FK case now explicitly tested. Anchors the
  intentional ordering of `RequirementService.delete` (demand-link → story).
- Scenario count: spec stays at 22; TC count rises from 22 → 23 (TC-REQS-001b is an
  extra TC for an existing Scenario "Requirement DELETE FK 保护").

### B5. Phase 5 Test-M2 fix — TC-STR-010 enrichment value assertions
- Added concrete value asserts for `projectName="Apollo"`, `projectCode="PROJ-Q1"`,
  `ownerLoginName="alice"` to TC-STR-010, ensuring GET-detail enrichment short-circuit
  regressions cannot pass undetected.

## C. Documentation adjustments

- Phase 5 Docs-H1: fixed scenario count (`21 → 22`) in `.stdd.yaml` and test-plan §六
  frontend-scaffold row (`3 → 4`).
- Phase 5 Docs-H2: corrected backend test count line in test-plan §一.三
  (`TC-REQS-FK ×1 → TC-REQS-001b`).

## D. Phase 6 deliver TODO checklist

- [ ] Move `changes/2026-06-07-story/` → `archive/2026-06-07-story/`; set
      `.stdd.yaml` phase to archived.
- [ ] Merge `entity-story/spec.md` as NEW capability under canonical `specs/`.
- [ ] Merge change-local `entity-requirement/spec.md` block: append the DELETE Story
      FK Requirement + storyCount enrichment Requirement; keep v0.0.8 Decision-6b
      owner-mutability intact.
- [ ] Merge change-local `frontend-scaffold/spec.md` block: append the
      RequirementsPage drilldown + StoryEditDrawer Requirements.
- [ ] Git commit + tag `v0.0.9-story`.

## E. Phase 5 Step-0 Review unresolved Mediums (recorded, not fixed)

These M-level findings are recorded for transparency. None block delivery; they are
either documented trade-offs, family-pattern parity, or low-impact UX warts.

| Source | ID | Description | Reason recorded only |
|---|---|---|---|
| Code | M1 | `existsByCode` race vs concurrent POST | Family parity — same as Project / Requirement / Demand / Role |
| Code | M2 | `enrich` 3-call N+1 on list path | Design Decision 10 trade-off; v0 scale acceptable |
| Code | M3 | Empty-string status validation message wart | Sibling-consistent with v0.0.6 RequirementCreateRequest |
| Code | M4 | Soft-deleted Requirement test missing | Edge case; not required by spec |
| Code | M5 | StoryEditDrawer listUsers race on rapid reopen | Low likelihood; B2 (key remount) mitigates partially |
| Test | M1 | Helper duplication across 3 Story test classes | Refactor opportunity; not a defect |
| Test | M3 | Brittle `getByText('3')` in RequirementsPage test | Single-cell column; low collision risk |
| Test | M4 | Dead `listUsers` mock in RequirementsPage test | Drawer stays closed; mock is harmless |
| Docs | M1 | design.md Decision 4 leaves both old + corrected nullability statements | Cosmetic; correction is unambiguous to readers |
| Docs | M2 | `13~14` field count vs actual 17~18 in proposal §Success Criteria | Cosmetic; E2E asserts schema via mysql DESCRIBE not count |
| Docs | M3 | StoryDetail field enumeration not in design.md (only spec) | Spec is the contract; design.md need not duplicate |
| Docs | M4 | M-IDs not back-mapped to proposal §A-G regions | Traceable via slices.md M01-M09 ↔ proposal §A files |

12 of the 13 Step-0 Mediums are recorded; M2 (TC-STR-010 enrichment value asserts) was
fixed and is counted as B5 above.
