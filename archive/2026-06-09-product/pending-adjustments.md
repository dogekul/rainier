# Pending Design Adjustments — v0.0.12-product

## PA-1. Java 8 `orElseThrow()` no-arg form (Build-time fix, propagated)

- **Discovered**: M03/M05/M07 sub-agent wrote tests using `repo.findById(id).orElseThrow()` (Java 10+ no-arg form).
- **Fixed**: changed 3 occurrences across `ProductControllerUpdateTest`, `ProductModuleControllerUpdateTest`, `FeatureControllerUpdateTest` to `orElseThrow(IllegalStateException::new)`.
- **Categorisation**: minor — pre-test-run fix, no semantic change.

## PA-2. TC-FES-PROD-003 (ProductEditDrawer Category select) not implemented

- **Discovered**: Phase 4 M11 frontend agent brief omitted TC-FES-PROD-003 from the per-test breakdown — agent literal-followed and produced only 3 of 4 frontend TCs.
- **Spec coverage**: spec entity-product-category/spec.md does NOT define this test. spec frontend-scaffold/spec.md Requirement 2 Scenario "ProductEditDrawer Category 联动" defines the behaviour. test-plan TC-FES-PROD-003 documents it.
- **Categorisation**: minor — runtime behaviour exists (Drawer wires categoryId correctly via cascading select code path), only missing dedicated regression test.
- **Decision**: defer to v0.0.13+ as a tracked follow-up. v0.0.12 ships with this acknowledged gap.

## PA-3. Spec Scenario count drift — 46 → 44

- **Discovered**: docs-review re-counted Scenarios:
  - entity-product-category: 11
  - entity-product: 10
  - entity-product-module: 10
  - entity-feature: 9
  - frontend-scaffold: 4
  - Total: 44 (not 46 as recorded in `.stdd.yaml`)
- **Fixed**: `.stdd.yaml:30` corrected from `46` to `44`.

## PA-4. "5 顶级组" vs "4 顶级组" doc drift (Docs-C1, multiple docs)

- **Background**: Phase 2 docs confused "Sider 顶级 group 数" (实际 4) 与 "群展开后的子项总数" (实际 14)。Phase 3 slices.md 陷阱 F caught the bug and Phase 4 implementation correctly delivered **4 顶级组**: 组织 / 产品 / 需求管理 / 人事配置. TC-FES-PROD-001 asserts 4 顶级组. proposal/design/frontend-scaffold spec文本仍是 "5"。
- **Decision (this round)**: instead of mass-rewriting 10 instances across change-local docs, Phase 5 design-adjustments §A1 documents the canonical correction. Phase 6 canonical merge of `specs/frontend-scaffold/spec.md` uses corrected wording (`4 顶级组 + 产品组 4 子项`). Change-local docs in `archive/2026-06-09-product/` keep historical "5" + an addendum.
- **Categorisation**: docs only, code/test/runtime all consistent on 4.

## PA-5. FK chain message ConflictException FQCN inconsistency (Code-L1/L2)

- **Discovered**: ProductCategoryService and ProductService threw `com.rainier.common.exception.ConflictException` (FQCN) in M09 FK chain lines, while same files import `ConflictException` short name.
- **Fixed**: replaced 2 FQCN with short name. ProductModuleService already used short form (consistent baseline).

## PA-6. Recorded-but-unfixed Step 0 findings (rationale)

| Severity | ID | Description | Reason recorded only |
|---|---|---|---|
| L | Code-L3 | TaskEditDrawer / ProductModule/Feature EditDrawer 客户端 filter size=100 cap (v0.0.11 同款) | v0.0.13+ 服务器端 filter；v0 scale acceptable |
| M | Test-M1 | Perf tests share L2 cache region, latent flake if profile enables L2 | L2 cache disabled in test profile; not active |
| L | Test-L1..L8 | minor improvements (PUT-response immutable assertion, etc.) | scope creep |
| M | Docs-M1..M6 | minor count/arithmetic drift in test-plan summary lines | cosmetic |
| H | Docs-H1..H4 | "5 顶级组" wording in design.md / proposal Risk rows | covered by PA-4 |
| L | Docs-L1..L6 | re-verifications, no action needed | informational |
