# Design Adjustments — v0.0.8-project

Adjustments captured during Phase 3-5 vs the Phase-2-locked design.

## A. Spec / Capability adjustments (carry into Phase 6 spec merge)

### A1. `entity-requirement` — owner mutability reversal (Decision 6b)
- **Phase 2 lock**: design.md Decision 6b — Requirement.ownerUserId IS now mutable
  (semantic reversal of v0.0.6's "owner immutable" rule). RequirementUpdateRequest
  carries `@NotNull Long ownerUserId`.
- **What changes on merge**: When merging `specs/entity-requirement/spec.md`
  into the canonical capability spec, **remove**:
  - Requirement statement `ownerUserId 不可修改`
  - Scenario "更新 body 含 ownerUserId 静默忽略"
- **Why**: v0.0.6 stated owner is fixed at creation; v0.0.8 (per user
  confirmation "需求的owner也可以改") allows admin transfer. Service validates new
  owner exists; createBy/updateBy capture the change.

### A2. `entity-requirement` — owner enrichment fields on read DTO (post-Step-0 review)
- **What changed**: `RequirementDetail` gained `ownerName` + `ownerLoginName`
  (previously only `projectName` + `projectCode`). `RequirementService.enrich`
  now joins User on read.
- **Spec impact**: minor. The MODIFIED Requirement "needs projectId 校验 + 富化"
  description should be expanded post-merge to also mention owner enrichment
  (already documented in v0.0.8 spec's prose; consider tightening on merge).

### A3. `entity-user-role` — projectId activation + 启动自愈
- **Phase 2 lock**: design.md Decision 6 strict + startup self-heal.
- **What changes on merge**: Replace the v0.0.7 caveat that "projectId is a
  placeholder; service does NOT validate" with the v0.0.8 reality: service
  validates non-null projectId existence; reads can assume no dangling refs
  thanks to `DanglingProjectIdCleanup`; NULL still acceptable (公司级 hat).

### A4. `frontend-scaffold` — Sider menu group ordering
- **What changed**: 「需求管理」 group went from 3 items (诉求 / 需求 / 关联) to 4
  (项目 first, then the original three). `/pm` redirect changed from `/pm/demands`
  to `/pm/projects`.
- **Why**: 项目 is the parent concept; surfacing it first matches the new
  workflow (admin creates Project, then attaches Requirements + assigns
  UserRoles to it).

### A5. NEW capability `entity-project`
- 4 Requirements / 13 Scenarios / 13 TCs (TC-PRJ-001..013).
- Standard 5-endpoint CRUD pattern with FK protection (Requirement +
  UserRole) on delete.
- `owner_user_id NOT NULL` + mutable; service validates user existence on
  both create and update paths.
- Code uniqueness enforced at service layer (no DB UNIQUE — same pattern as
  Requirement / Position / Role / Demand).

## B. Implementation-level adjustments (documented for the reader)

### B1. `DanglingProjectIdCleanup` log granularity
- **Phase 2 design.md note** ambiguous between per-row WARN and per-table count.
- **As built**: per-row WARN `cleaned dangling project_id from <table>.<id>
  (was project_id=<old>)` + per-table summary `cleaned <N> rows from
  <table>.project_id`.
- **Rationale**: per-row makes forensics tractable; per-table summary closes
  the log block. TC-REQP-004 / TC-URLP-004 assert the per-row pattern via
  Logback ListAppender (added Phase 5 Step 0 fix).
- **tasks.md note**: §4.2 should be updated post-deliver to say
  "log WARN per-row with id" — captured but does not block merge.

### B2. Strict reads vs read-tolerance — chosen strict path
- **Phase 1 proposal** initially considered "graceful reads return null
  projectName on dangling ref"; user Gate-1 selected option B "启动自愈"
  (CommandLineRunner). `enrich(...)` keeps `findById(...).orElse(null)`
  as a defense-in-depth fallback (e.g. between a Project hard-delete and a
  concurrent read), but this is no longer the *primary* tolerance mechanism.

## C. Test plan adjustments

- TC count: backend 22 new (PRJ 13 + REQP 6 + URLP 3 shared cleanup) +
  frontend 5 new (FES-P 1..5). test-plan §1.3 row was off-by-one on frontend
  count; corrected in Phase 5 Step 0 fix.
- §1.2 测试原则 referenced TC-PRJ-008 for the owner-mutability principle;
  actual TC is TC-PRJ-009. Corrected.
- §3 execution matrix listed entity-requirement as 4 TCs; actual is 6.
  Corrected.

## D. Capability cross-references unchanged

- v0.0.6 entity-requirement: project_id placeholder activated; no schema change.
- v0.0.7 entity-user-role: project_id placeholder activated; no schema change.
- v0.0.5 entity-organization: untouched.
- v0.0.4 BaseEntity / AuditorAware: untouched.

## E. Phase 6 deliver TODO checklist

- [ ] Merge `entity-project/spec.md` as NEW capability under canonical
      `specs/entity-project/`.
- [ ] Merge change-local `entity-requirement/spec.md` MODIFIED block: append
      new Requirements; remove v0.0.6 "owner 不可改" Requirement + its
      associated Scenario (per A1).
- [ ] Merge change-local `entity-user-role/spec.md` MODIFIED block: update
      the v0.0.7 "projectId placeholder" caveat to reflect v0.0.8 validation
      + 启动自愈 (per A3).
- [ ] Merge change-local `frontend-scaffold/spec.md` MODIFIED block: append
      the Sider 4-item + /pm/projects + ProjectsPage owner default Scenarios.
- [ ] Git tag `v0.0.8-project`.
