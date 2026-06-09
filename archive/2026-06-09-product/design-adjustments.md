# Design Adjustments — v0.0.12-product

Phase 3-5 deviations from the Phase-2-locked design.

## A. Spec / Capability adjustments (carry into Phase 6 spec merge)

### A1. **Sider top-level count = 4 groups (not 5)** — wording reconciliation

- Phase 2 docs said "Sider 顶级组 4 → 5". Reality: 4 top-level groups (组织 / 产品 / 需求管理 / 人事配置). The "5" came from miscounting (likely conflating "expanded items per group" with "top-level group count").
- Phase 3 slices.md §陷阱 F flagged the typo; Phase 4 implementation correctly delivered 4 top-level groups; TC-FES-PROD-001 asserts 4.
- **Phase 6 canonical merge target text** for `specs/frontend-scaffold/spec.md` (append as new Requirement block):
  > Sider 顶级 4 个组：组织 → 产品 → 需求管理 → 人事配置. 产品组在 v0.0.12 起位于第 2 位，展开后含 4 项：产品分类 / 产品 / 产品模块 / 功能.
- Do NOT carry "5 顶级组" text into canonical. Archive-local docs (proposal/design/spec) retain the historical "5" wording as a record of the Phase 2 misunderstanding + this addendum.

### A2. 4 NEW entity capabilities (entity-product-category / entity-product / entity-product-module / entity-feature)

- Merge all 4 change-local `specs/entity-*/spec.md` files as canonical `specs/entity-*/spec.md` (no existing canonical files for these).
- Each spec preserves: 4 Requirements (Create / Query / Update / Delete-soft-FK) + 1 perf SCenario.
- Status machines (D2): 2 / 4 / 3 / 3 states as locked in Phase 2.

### A3. frontend-scaffold MODIFIED — append new Requirement for v0.0.12 「产品」 group

- Use A1 wording. Add a changelog entry to the canonical frontend-scaffold spec's `Change log` section:
  > 2026-06-09 (v0.0.12-product) — Sider 顶级 4 组（组织 → 产品 → 需求管理 → 人事配置）. NEW「产品」组位于第 2 位，含 4 路由: /pm/product-categories / /pm/products / /pm/product-modules / /pm/features. NEW 4 EditDrawer w/ cascading parent select (Category→Product / Product→Module 等).

## B. Phase 4 BUILD adjustments

### B1. PA-1 — Java 8 `orElseThrow()` fix
3 sub-agent-written Update tests used Java 10+ no-arg form. Fixed inline to `orElseThrow(IllegalStateException::new)`.

### B2. PA-3 — Scenario count drift
`.stdd.yaml` `scenarios: 46 → 44` after hand-recount. spec / test-plan content unchanged.

### B3. PA-5 — ConflictException FQCN consistency
2 FQCN usages collapsed to short name (already imported).

## C. Phase 5 Step 0 review fixes (this round)

| ID | Source | Fix |
|---|---|---|
| Code-L1 | Code reviewer | FQCN ConflictException in ProductCategoryService — collapsed to short name |
| Code-L2 | Code reviewer | Same for ProductService |
| Docs-M1 | Docs reviewer | `.stdd.yaml scenarios: 46 → 44` |
| Docs-C1 / H1-H4 | Docs reviewer | "5 顶级组" reconciliation policy: Phase 6 canonical uses "4 顶级组"; change-local docs unchanged + addendum block (this file) |

## D. Recorded-but-unfixed Step 0 findings (rationale)

Aggregate after Step 0.3: **C:0 (was 1) / H:0 (was 4) / M:6 / L:15** — within threshold post-policy decision on Docs-C1.

Most M / L items captured in `pending-adjustments.md §PA-6`. Notable:

- **Test-M2 (PA-2)**: TC-FES-PROD-003 missing — defer to v0.0.13+ as tracked follow-up. Runtime behaviour exists, only regression coverage missing.
- **Test-M1**: perf tests share JPA L2 cache region — latent flake risk if test profile enables L2 cache. Not active currently.
- **Test-L1**: TC-XXX-010 immutability tests could also assert response body shows unchanged parent_id, in addition to DB read. Robustness improvement.
- **Code-L3 (sub-agent)**: EditDrawer hard-coded `size: 100` for parent fetch — same scale concern as v0.0.11 TaskEditDrawer. Tracked for v0.0.13+ server-side filter migration.

## E. Phase 6 DELIVER TODO checklist

- [ ] Archive `changes/2026-06-09-product/` → `archive/2026-06-09-product/`
- [ ] Merge 4 NEW entity specs into canonical `specs/entity-*/spec.md` (clean copy + retain header)
- [ ] Merge frontend-scaffold MOD: append Requirement block + changelog line per A1/A3 wording (use 4 顶级组 phrasing, NOT 5)
- [ ] Commit + tag `v0.0.12-product`
- [ ] No push (per pre-auth)
