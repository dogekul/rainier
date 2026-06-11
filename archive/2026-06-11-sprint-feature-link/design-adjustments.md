# Design Adjustments — v0.0.14-sprint-feature-link

Phase 4-5 deviations from the Phase 2 locked design, and Step 0 review outcomes.

## A. Build-phase adjustments (from pending-adjustments.md)

| # | Adjustment | Type | Impact |
|---|---|---|---|
| PA-1 | Sprint product-enrich perf budget `≥3∧≤6` (estimated) → `≥4∧≤7` (actual). Existing `SprintListSqlCountTest` left at exactly 6 (null-productId sprints don't trigger the product batch → proves no regression); new `SprintProductEnrichSqlCountTest` asserts ≥4∧≤7 for product-bound sprints. test-plan.md TC-PERF-SPR-PF-001 updated to ≥4∧≤7 in Step 0.3. | perf budget number | minor, no behaviour change |
| PA-2 | v0.0.13 `LegacyProductCategoryCleanupTest` table-count assertion 16 → 17 (+ `rainier_sprint_feature` presence check). | cross-version test maintenance | minor; only test touched outside scope |
| PA-3 | Added TC `post_unknownProductId_returns400` — create-time pre-bind productId that doesn't exist → 400 "product not found". Additive coverage for SprintService.create's new validation. | additive | none (no spec change) |
| PA-4 | Test helper `post()` → `postLink()` (shadowed static `MockMvcRequestBuilders.post`); `Priority` import corrected to `com.rainier.common.domain.Priority`. Fixed pre-run. | build fix | none |

## B. Step 0 review fixes (Step 0.3, this phase)

| Finding | Agent | Action |
|---|---|---|
| H1 — perf budget doc number stale (`≥3∧≤6` vs test's `≥4∧≤7`) | test + docs | FIXED — test-plan.md TC-PERF-SPR-PF-001 → ≥4∧≤7 with PA-1 note. |
| M2 (docs) — `SprintCreate` TS interface omitted `productId`, making backend create-time pre-bind unreachable from the typed client | docs | FIXED — added `productId?: number` to `SprintCreate` in `api/sprint.ts`. |
| M5 (test) — frontend mount-dropdown product-filter (spec "仅显示该产品的 feature") was untested | test | FIXED — added an out-of-product feature (moduleId 99) to the mock; TC-FES-SF-001 now asserts the dropdown contains the in-product feature and excludes both the linked one and the cross-product one. |
| M3 (test) — reverse-query tests asserted `$[0].field.exists()` only | test | FIXED — TC-SF-REV-001/003 now assert `$[*].code`/`$[*].featureId`/`$[*].sprintId` via `containsInAnyOrder` against concrete values. |

## C. Positive deviation (over-delivery, no action)

- **D11 frontend filter** — design.md Decision 11 said the frontend dropdown would "先不严格过滤 null 情形" and rely on backend 400. The implementation (`SprintFeaturePanel.tsx`) actually performs a proper `productId → listProductModules → moduleIds → filter features` client-side filter. Stricter (better) than documented; now also test-covered (M5 fix). The spec's ADDED Requirement ("挂载下拉…仅显示该产品的 feature") is therefore fully honored.

## D. Recorded-but-not-fixed (rationale) — within M-threshold, deferred

| Finding | Agent | Rationale |
|---|---|---|
| Code-M1 — create uses 400 for missing sprint/feature, reverse queries use 404 | code | Intentional & matches DemandRequirementLink precedent (create = input validation → 400; reverse = resource lookup → 404). Documented, not changed. |
| Code-M2 — soft-deleted feature/sprint leaves an un-cleanable orphan link (vanishes from reverse views) | code | Matches DemandRequirementLink precedent (`orElse(null)` skip). Cascade-cleanup on soft-delete is out of v0.0.14 scope → **v0.0.15 candidate**. |
| Code-L1/L2 — `findFeaturesBySprint`/`findSprintsByFeature` do per-link `findById` (N+1), unlike the batched requirement rollup | code | Matches precedent; sprints/features per link are low-cardinality; perf tests pass. Optional future batching. |
| Test-M1/M2/M4/M6 — optional assertion hardening (direct column read, ordered dedup assert, refetch call-count) | test | Current assertions already catch the targeted bugs; deferred as polish. |
| L-tier (method-name drift `is16`, `assertEquals(0,…?1:0)` readability, etc.) | all | Cosmetic; appendix only. |
