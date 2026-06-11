# Pending Adjustments — v0.0.14-sprint-feature-link (Phase 4 BUILD)

## PA-1. Sprint product-enrich perf budget: test-plan ≥3∧≤6 → actual ≥4∧≤7

- **Discovered**: M07. The existing `SprintListSqlCountTest` locks to *exactly 6* (2 page + user/req/project batch + storyCount). Adding the product batch enrich makes it 7 when sprints have a non-null productId.
- **Resolution**: kept the existing test untouched (it seeds null-productId sprints → product batch never fires → still exactly 6, proving no regression). Added a NEW perf test `SprintProductEnrichSqlCountTest` seeding product-bound sprints with range assert ≥4∧≤7 (TC-PERF-SPR-PF-001). The test-plan's ≥3∧≤6 figure was an estimate; 7 is correct given the established 6-statement baseline + 1 product batch.
- **Impact**: minor (perf budget number); no behaviour change.

## PA-2. v0.0.13 LegacyProductCategoryCleanupTest table-count 16 → 17

- **Discovered**: M07 full-suite run — the v0.0.13 schema test asserted exactly 16 rainier_* tables.
- **Resolution**: v0.0.14 adds `rainier_sprint_feature` → updated assertion to 17 + added `assertTrue(contains("rainier_sprint_feature"))`. Expected cross-version test maintenance.
- **Impact**: minor; the only test touched outside the change scope.

## PA-3. Extra create-time productId validation TC

- **Discovered**: M07. Beyond the 4 planned TC-SPR-PF cases, added `post_unknownProductId_returns400` (pre-bind productId that doesn't exist → 400 "product not found") since SprintService.create now validates the optional productId.
- **Impact**: additive coverage, no spec change.

## PA-4. Helper-method name collision (build-time, fixed)

- `SprintFeatureLinkControllerCreateTest` initially named a helper `post(...)` which shadowed the static `MockMvcRequestBuilders.post` import → renamed to `postLink`. Also corrected `Priority` import to `com.rainier.common.domain.Priority` (it is not in `requirement.domain`). Both fixed before the suite ran; no residual impact.
