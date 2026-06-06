# Pending Design Adjustments — v0.0.8-project

Long-range full_auto / Phase 5 VERIFY Step 0 Review captures items recorded for the
Gate 3 packet. None block delivery for this change; all are scoped follow-ups.

## Code-review High items — recorded, no in-change fix

### CR-H1. `DanglingProjectIdCleanup` runs on every startup, with native UPDATE running
inside the Spring context lifecycle.

- **Source**: Code review agent, v0.0.8-Step-0
- **Spec reference**: design.md Decision 6 (启动自愈) + spec/entity-requirement /
  spec/entity-user-role `Scenario: 启动自愈` (proposal Success Criteria explicitly
  asserts the WARN log line).
- **Reality**: The runner executes BEFORE DispatcherServlet starts taking traffic in
  the default Spring Boot 2.7 lifecycle (`CommandLineRunner` is invoked from
  `SpringApplication.callRunners` after the context refresh completes but before
  the embedded Tomcat connector exits its startup latch); the M11 E2E confirmed
  the cleanup-then-traffic ordering empirically (`docker logs` shows
  `cleaned dangling … rainier_user_role.2` BEFORE the first request landed).
- **Theoretical concern**: If a future change re-orders bean creation, a race
  could open. We accept this as the Decision-6 design intent (startup-only,
  non-recurring, batch).
- **Mitigation already in place**: `@Transactional` boundary on `run(...)` + the
  cleanup is idempotent (the second run is a no-op once the WHERE-clause returns
  empty).
- **Follow-up (not in scope)**: v0.0.9+ — consider a Flyway repeatable migration
  or a Liquibase changeset so cleanup runs strictly pre-context (before the
  application can accept traffic). Track as separate change if v0.0.9 introduces
  PMO board query that depends on consistent reads at boot.

### CR-H2. Project soft-delete + dangling FKs: silent NULL on next boot.

- **Source**: Code review agent, v0.0.8-Step-0
- **Spec reference**: design.md Decision 6 explicitly chose the self-heal strategy
  over per-read tolerance after the user Gate-2 confirmation "启动自愈".
- **Reality**: This is by design. `ProjectService.delete` checks both
  `requirementRepo.countByProjectId` and `userRoleRepo.countByProjectId`; only
  out-of-band soft-deletes (direct repo.delete, DB-level UPDATE) can produce
  dangling refs that the next-boot cleanup NULLs out. The behavior is
  documented in proposal.md "Decision 6" prose.
- **Mitigation**: A persistent audit trail of cleanup events would be useful
  but is not required by the spec. The WARN log lines provide one boot's worth
  of forensic visibility.
- **Follow-up (not in scope)**: When `audit_log` table arrives (planned v0.1.x),
  pipe cleanup events into it for retroactive search.

### CR-H3. `existsByCode` respects `@Where(del_flag=0)` — code reuse after soft delete.

- **Source**: Code review agent, v0.0.8-Step-0
- **Reality**: This is the **same** pattern used by `Requirement.code`,
  `Position.code`, `Role.code`, `Demand.code` in v0.0.6 / v0.0.7 — soft-deleted
  rows free their code for reuse. Inherited deliberately to keep the family
  consistent; not a v0.0.8 regression.
- **Follow-up (not in scope)**: A platform-wide audit could decide whether
  soft-deleted codes should remain reserved. Out of scope for v0.0.8 (which
  promises pattern parity, not reform).

## Test/Code Medium items — fixed in this change

| Finding | Action | Verification |
|---|---|---|
| Code-M1: `RequirementDetail` missing ownerName/ownerLoginName despite frontend reading them | Added the two fields + setters to `RequirementDetail`; updated `RequirementService.enrich` to populate from `userRepo.findById` | Backend tests still 147/147 |
| Test-M1: DanglingProjectIdCleanupTest had no log assertion | Added Logback `ListAppender` capture; both tests now assert WARN line `cleaned dangling project_id from rainier_<table>.<id>` + `was project_id=<N>` | Test added |
| Test-M3: AppRoutes.test missing grep guard for `/pm/projects` literal | Added a vitest case that reads `AppRoutes.tsx` via `fs.readFileSync` and asserts ≥1 occurrence | Frontend tests 31/31 |
| Test-M4: TC-FES-P02/P03/P04 JSDoc labels were off-by-one vs test-plan | Renumbered to match test-plan TC-IDs | n/a — comment only |
| Test-L4 (TC-PRJ-008): assertion only checked `total=2`, not row status | Added `everyItem(is("ACTIVE"))` on `$.content[*].status` | 147/147 |
| Test-L5 (TC-REQP-005): missing owner enrichment follow-through assertion | Added `.ownerName="黎立"` + `.ownerLoginName="lili"` to mirror TC-PRJ-009 rigor | 147/147 |
| Docs-C1 / Docs-H1: `.stdd.yaml` stale `dirty_data_strategy` + stale `explicitly_excluded` entry | Rewrote `dirty_data_strategy` block to match Decision 6 + 6b; removed `dirty-placeholder-auto-cleanup` from `explicitly_excluded` | yaml updated |
| Docs-H2: test-plan §1.2 wrong TC-PRJ-008 reference + stale "owner mutable differentiator" claim | Replaced with TC-PRJ-009 + TC-REQP-005 references + reframed as v0.0.8 synchronized reversal | test-plan updated |
| Docs-H3: execution matrix under-counted entity-requirement TCs (4 → 6) | Updated to 6 + classified breakdown | test-plan updated |

## Medium items recorded but not fixed (within M:≤10 threshold)

- **Code-M2**: SELECT-then-UPDATE window in cleanup is racy if a writer inserts a new
  Project between the two statements. Cosmetic for log accuracy; cleanup is itself
  idempotent. Defer.
- **Code-M3**: `ProjectService.update` inconsistent partial-update semantics
  (description/enabled null-guarded, others full-replace). Behavioral but not
  spec-violating. Defer to platform-wide PUT consistency pass.
- **Code-M5**: `countByProjectId` respecting `@Where(del_flag=0)` on Requirement —
  documented above (CR-H2 family).
- **Code-M6**: ProjectsPage cannot clear a populated description. Edge case.
- **Code-M7**: `if (!ownerUserId) return;` silently no-ops on id 0. Practical risk
  is nil (BIGINT IDENTITY starts at 1).
- **Docs-M4**: After Phase 6 deliver merges entity-requirement spec, the canonical
  `specs/entity-requirement/spec.md` will need its v0.0.6 "ownerUserId 不可修改"
  Requirement + "PUT body 静默忽略" Scenario explicitly excised. Deliver-phase
  TODO.
- **Docs-M5**: proposal.md says 6 Requirement改造 files in §B, §Impact says 5.
  Cosmetic — actual diff produced files matching both interpretations depending
  on whether `RequirementDetail` enrichment fix counts as separate. Defer.
- **Docs-M6**: frontend-scaffold spec missing dedicated RequirementEditDrawer改造
  Scenario. Coverage is in tasks M09 + manual E2E (M11). Defer.

## Low items — informational only, not addressed

See test-report.md §7 for the full L-tier list (15 items). Standard pattern-
consistency notes that do not affect this change's correctness or coverage.
