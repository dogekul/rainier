# Test Report: C5 capability-tags

## Run
- `cd backend && mvn test`
- Result: **BUILD SUCCESS** — 752 tests, 0 failures, 0 errors, 0 skipped (was 740 → +12 new).

## NEW tests
- `CapabilityServiceTest` (10): TC-CAP-001..010 — tag create (happy / dup / bad cat), user upsert (insert/update/level-bounds/source-validation/missing-tag), join shape, categorize buckets.
- `CapabilityTagSeedTest` (2): TC-CAP-SEED-001..002 — seeds ≥10 rows covering TECH/PRODUCT/SOFT; second `run()` is idempotent (count unchanged).

## Touched tests
- `LegacyProductCategoryCleanupTest.schema_tableCount_withoutProductCategory` — schema table count 35 → 37（新增 `rainier_capability_tag` + `rainier_user_capability`），注释也加了 v0.0.85 一行。

## 风险/Caveats
- 未加新的 controller-layer mockMvc 鉴权测试：admin-write 的 Tier A 拦截、`/api/me/capabilities` 401、`/api/users/{id}/capabilities` 403/404 走的是已经被大量集成测试覆盖的同款 `AdminPaths` + `AuthController.ATTR_USERNAME` + `MeProfileService.isDirectManagerOf` 路径，无新分支。如后续要做 endpoint-level 黑盒，复用 `UserProfileControllerTest`（C3）的样式即可。
- 既有 `MeProfileService` 的所有调用方（profileOf / profileOfUserId）现在多吃一个空 capabilities 列表 —— 默认 test profile 下 capability-seed 关闭、`UserCapabilityRepository.findByUserId` 返回空，对既有断言无影响（被 752 全绿验证）。
- 前端 / UI 未动（OutOfScope）。
- `app.demo.capability-seed.enabled` 默认 true（prod yml），test profile 显式置 false。
