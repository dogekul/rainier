# Pending Design Adjustments — v0.0.10.1-cleanup

## PA-1. SQL count threshold ≤ 5 → ≤ 6 (small, auto-recorded)

- **Source**: S04 first RED run — `stats.getPrepareStatementCount() = 6` not 5
- **Root cause**: Spring Data JPA `findAll(Specification, Pageable)` issues **two** statements per page request: a `SELECT ... LIMIT/OFFSET` for the data and a separate `SELECT COUNT(*)` for the total. Phase 2 spec assumed "1 page query" but production behaviour is 2. The 4 batch-enrich SELECTs (User / Sprint / Requirement / Project) plus 2 page queries = 6.
- **Impact**: trivial — the goal (prove batch enrich works) is still met. Baseline was 1 + 1 + 20×4 ≈ 82 statements; new is 6. **92% reduction** vs. v0.0.10.
- **Fix landed in this slice**: both `StoryListSqlCountTest` and `SprintListSqlCountTest` assert `≤ 6` not `≤ 5`. Both specs (`entity-story` Scenario / `entity-sprint` Scenario / `test-plan.md` TC-PERF-STR-001 / TC-PERF-SPR-001) and `design.md` Decision 5 will be reconciled in Phase 5 design-adjustments.
- **Categorisation**: minor (does not change interface or semantic); auto-recorded per long-range pre-auth A1.
