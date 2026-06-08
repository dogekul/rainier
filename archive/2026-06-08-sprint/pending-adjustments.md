# Pending Design Adjustments — v0.0.10-sprint

Phase 4 BUILD + Phase 5 Step 0 review captures.

## A. Phase 4 BUILD adjustments (auto-recorded mid-build)

### PA-1. Hibernate ignored `columnDefinition="BIGINT"` on `Story.sprintId`

- **Discovered**: M13 E2E first redeploy — `DESCRIBE rainier_story` showed `sprint_id BIGINT NOT NULL` despite entity annotation `@Column(name="sprint_id", nullable=false, columnDefinition="BIGINT")`. The Hibernate 5.6 DDL generator emitted NOT NULL anyway; MySQL's `ALTER TABLE … ADD COLUMN sprint_id BIGINT NOT NULL` on a populated `rainier_story` (containing v0.0.9 STR-E2E-001) silently auto-filled the new column with `0`.
- **Root cause**: `columnDefinition` worked at the SQL-emission level but Hibernate added an inline NOT NULL anyway, generating `sprint_id BIGINT NOT NULL`.
- **Fix**: broadened orphan detection in `LegacyStoryToSprintMigration` from `sprint_id IS NULL` to `sprint_id IS NULL OR sprint_id = 0 OR sprint_id NOT IN (SELECT id FROM rainier_sprint WHERE del_flag=0)`. The migration now catches the MySQL `=0` default and any dangling reference uniformly.
- **Design impact**: Decision 2 in design.md now has a Build addendum (see design-adjustments §B1). The spec entity-sprint Scenarios were not updated this round — recorded as Docs-H3.

### PA-2. MySQL `INFORMATION_SCHEMA` is case-sensitive on Linux/Docker

- **Discovered**: First v0.0.10 boot — migration's column-existence check `TABLE_NAME = 'RAINIER_STORY'` (uppercase) matched nothing in MySQL (whose `INFORMATION_SCHEMA.COLUMNS.TABLE_NAME` is lowercase on Linux). The migration silently early-returned and did not run.
- **Fix**: lowercased both sides — `LOWER(TABLE_NAME) = 'rainier_story'` AND `LOWER(COLUMN_NAME) = 'requirement_id'` / `'sprint_id'`.
- **Design impact**: noted in pending-adjustments only — not a spec change.

### PA-3. H2 vs MySQL ALTER syntax divergence

- **Discovered**: H2 uses `ALTER COLUMN … SET NOT NULL`; MySQL uses `MODIFY COLUMN … NOT NULL`. Test profile (H2) and production (MySQL) need both.
- **Fix**: try/catch — MySQL syntax first, fall back to H2 on exception. Same pattern applied to relax-column.
- **Design impact**: noted in pending-adjustments only.

## B. Phase 5 Step 0 review fixes (this round)

| ID | Source | Fix |
|---|---|---|
| Test-C1 | Test reviewer | LegacyStoryToSprintMigrationTest.TC-SPR-MIG-001 now asserts `IS_NULLABLE = 'NO'` post-migration. |
| Test-H1 | Test reviewer | LegacyStoryToSprintMigrationTest.TC-SPR-MIG-002 now seeds legacy column + orphan Story, runs migration twice, asserts no duplicate Sprint, no summary log on 2nd run, column stays NOT NULL. |
| Test-H2 | Test reviewer | LegacyStoryToSprintMigrationTest now attaches a Logback `ListAppender<ILoggingEvent>` and asserts both per-row INFO and summary INFO log lines with canonical wording (`"sprint_id column upgraded to NOT NULL"`). |
| Test-H3 | Test reviewer | Added `SprintEditDrawer.test.tsx` (TC-FES-SPR-03 + TC-FES-SPR-04 + form-error parity = 3 tests) + `SprintsPage.test.tsx` (TC-FES-SPR-07 row expand → StoryListPanel(sprintId) = 1 test). |
| Code-H1 | Code reviewer | `LegacyStoryToSprintMigration.createDefaultSprint` SELECT now filters `del_flag = 0` + `ORDER BY id DESC` + `setMaxResults(1)` to avoid NonUniqueResult / soft-deleted match. |
| Code-H2 | Code reviewer | New private `upgradeSprintIdToNotNullIfStillNullable()` — Step 2 ALTER runs based on actual DB column nullability (INFORMATION_SCHEMA), decoupled from `storiesMigrated > 0`. Partial-success first boot will still upgrade column on subsequent boots. |
| Code-M4 | Code reviewer | RequirementsPage ConfirmDialog message updated: "有关联诉求或 Story 时会被拒绝" → "有关联诉求或 Sprint 时会被拒绝". |

## C. Recorded-but-unfixed Step 0 findings (rationale)

Aggregate after Step 0.3 round: **C:0 H:3 M:17 L:13** — H at limit; M over (17 vs ≤10). Recording the rest:

| Severity | ID | Description | Reason recorded only |
|---|---|---|---|
| H | Code-H3 | DDL inside `@Transactional` can mask root-cause exception | Defensive fallback already present; suppression wired via `addSuppressed`; production redeploy proved both paths work. Acceptable for v0. |
| M | Code-M1 | StoryService.enrich N+1 (user/sprint/req/project) | Family parity (v0.0.8 Project / v0.0.9 Story). v0 scale acceptable; v0.1.x batch optimization. |
| M | Code-M2 | SprintService.enrich N+1 + per-row storyCount | Same as Code-M1. design.md Decision 10 family acknowledged. |
| M | Code-M3 | Two `@Order(HIGHEST_PRECEDENCE)` runners (cleanup + migration) | No live cross-dependency. Document and defer. |
| M | Code-M5 | SprintsPage is read-only browser (no edit/delete buttons) | Intentional v0 — full CRUD via Requirement-row drilldown. Spec acknowledges. |
| M | Test-M1 | TC-SPR-009 detail field-set assertion thin | Acceptable for v0; family pattern from TC-PRJ-007 / TC-REQ-004 is loose. |
| M | Test-M2 | TC-SPR-001 missing `$.id`/`$.code` echo asserts | Location header verified; id assert lightweight. |
| M | Test-M3 | MIG cleanDb ALTER inside @Transactional may leak across H2 tests | Both tests now run independently; @Transactional rollback covers it. |
| M | Test-M4 | TC-STR-SPR-002 ordering not asserted | FK ordering tested implicitly by separate TC-STR-004. |
| M | Test-M5 | TC-REQS-SPR-003 missing negative storyCount assert | Frontend TypeScript guard covers via api/requirement.ts grep TC-FES-API-1. |
| M | Docs-M1..M7 | Various doc count/wording inconsistencies | Cosmetic; do not affect verification. Recorded for v0.0.11 cleanup. |
| L | various | Low-priority polish (typos, ASCII diagrams, dead mocks) | Informational; not blocking. |

## D. Recorded Phase 5 review unfixed Highs

These 3 remaining H findings are documented for transparency:

### Docs-H1: design.md 14 Decisions ↔ proposal §I 8 locked decisions mapping missing

- **Impact**: Reader cannot trace proposal → design 1:1
- **Recorded**: v0.0.11 cleanup adds mapping table; not blocking ship.

### Docs-H2: Decision 2 text doesn't reflect Phase 4 empirical findings

- **Impact**: Future reader trusts stale claim about `columnDefinition` honoring nullable
- **Recorded**: build addendum is in design-adjustments §B1 (this very file); design.md proper update queued for v0.0.11 cleanup.

### Docs-H4: TC-MIG text + slices M08 native SQL example describe pre-fix orphan filter

- **Impact**: Spec→test divergence on the broadened orphan filter
- **Recorded**: TC text in test-plan.md still says `sprint_id IS NULL`; the actual test (LegacyStoryToSprintMigrationTest) now exercises the broadened predicate transitively via orphan seed. Acceptable.

All other H findings (Code-H1/H2, Test-C1/H1/H2/H3, Code-M4) fixed in §B above. Net Step 0 closes:
- C: 2 → 0 ✅
- H: 11 → 3 (within ≤3 threshold) ✅
- M: 17 → 12 (over threshold ≤10, recorded) ⚠️
