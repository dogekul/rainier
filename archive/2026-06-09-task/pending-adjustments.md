# Pending Design Adjustments — v0.0.11-task

## PA-1. SQL count budget 7 → 6 (Task enrich is 4-stage, not 5)

- **Phase 2 spec/design said**: 2 page + 5 batch enrich (user/sprint/story/requirement/project) = 7.
- **Phase 4 reality**: TaskDetail does NOT expose any Requirement field directly. sprintName + storyTitle cover the "where does this task live" question. Requirement enrich would be a dead query — no field consumes it.
- **Implemented**: 2 page + 4 batch (user/sprint/story/project) = 6.
- **Test asserts**: `assertEquals(6L, stmtCount, ...)` in `TaskListSqlCountTest`.
- **Categorisation**: minor (does not change interface or external semantics); auto-recorded per long-range pre-auth A1.
- **Doc reconcile**: ✅ propagated in Phase 5 Step 0.3 — updated proposal.md / design.md (Decision 6 + 10) / specs/entity-task/spec.md (Req 6 + Scenario) / test-plan.md (TC-PERF-TSK-001 + 测试原则) / slices.md (M10) / tasks.md (2.3 + 9.1) / TaskService.java line comment all 7→6 and "5 batch → 4 batch (drop requirement)".

## PA-2. Field count `22/23` → `24` (mechanical typo across 5 docs)

- **Reality**: TaskDetail field set array has exactly **24** items: 11 business + 9 enrichment + 4 audit.
- **Drift**: test-plan §测试原则 line 20 said "22-field", §详细案例 TC-TSK-010 said "23 字段全有"; slices.md M02 + M09 said "23 字段"; tasks.md 2.3 + 9.1 said "23-field".
- **Fixed** in Phase 5 Step 0.3 by counting the array and replacing the integer in 5 docs.
- **Categorisation**: minor (cosmetic, array contents unchanged); auto-recorded per long-range pre-auth A1.

## PA-3. TaskService cross-layer guard message refinement (Code-M1)

- **Issue**: When parent Requirement of a Sprint is soft-deleted (rare but possible), `findById` returns empty and previously threw `"sprint not in project"`, which is misleading.
- **Fix landed**: throw `"sprint parent requirement missing: sprintId=<id>"` instead. Preserves the alive-Sprint case (still throws `"sprint not in project"` only when projectId mismatch).
- **Categorisation**: minor (defensive — no live bug observed); auto-recorded.
