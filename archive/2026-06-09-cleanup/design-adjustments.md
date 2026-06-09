# Design Adjustments — v0.0.10.1-cleanup

Phase 3-5 deviations from the Phase-2-locked design.

## A. Phase 4 BUILD adjustments

### A1. PA-1: SQL count budget 5 → 6 (propagated this round)

- **Phase 2 assumption**: `findAll(Specification, Pageable)` issues **1** statement per page request → batch enrich budget is `≤ 5` (1 page + 4 batches).
- **Phase 4 reality**: Spring Data JPA issues **2** statements per page request — data + count. New baselines:
  - StoryService.list: 2 (page) + 4 (batch) = **6**
  - SprintService.list: 2 (page) + 3 (batch) + 1 (storyCount aggregate) = **6**
- **Fixed in this round** per Docs-H1 finding:
  - `specs/entity-story/spec.md` + `specs/entity-sprint/spec.md` Scenarios: `≤ 5` → `= 6` with breakdown.
  - `design.md` Decision 3 prose + Decision 5 code sample (`isLessThanOrEqualTo(5L)` → `isEqualTo(6L)`).
  - `test-plan.md` TC-PERF-STR-001 / TC-PERF-SPR-001 期望结果 column: `≤ 5` → `= 6`.

## B. Phase 5 Step 0 review fixes (this round)

| ID | Source | Fix |
|---|---|---|
| Code-M2 | Code reviewer | `StoryService.list` + `SprintService.list` 在 collect id 时加 `.filter(Objects::nonNull)`，避免单个 legacy 行违反 NN 不变性时把整页 list 接口炸成 500。 |
| Test-M1 / Code-L2 | Test + Code reviewers | `StoryListSqlCountTest` + `SprintListSqlCountTest` 删除 tautological `Math.min(stmtCount, 6L)` 断言，改为 `assertEquals(6L, stmtCount, ...)` 锁死预期值（catches both regression >6 AND silent over-tightening <6）。 |
| Test-L2 | Test reviewer | `LegacyRequirementIdColumnCleanupTest.run_columnPresent_dropsItAndLogsInfo` 加 `sprint_id` 列存在性断言 — 拦截 "DROP COLUMN sprint_id"（误删错列）型 mutation；同样的 colCount==0 不再能掩盖问题。 |
| Docs-H1 | Docs reviewer | PA-1（≤5 → ≤6）反向传播完成。Spec / design / test-plan 三处文本统一为 = 6 并标注 "PA-1 修正"。 |

## C. Recorded-but-unfixed Step 0 findings (rationale)

Aggregate after Step 0.3: **C:0 H:0 M:6 L:9** — all within threshold (H ≤ 3, M ≤ 10).

| Severity | ID | Description | Reason recorded only |
|---|---|---|---|
| M | Code-M1 | LegacyRequirementIdColumnCleanupTest `@Transactional` 边界 — H2 下 DDL 跨 tx 可见性可能引发偶发 false-positive no-op | 实测 191 次绿；TC-CLN-001/002/003 都跑过多轮 deploy + unit test，未观察到 flake。Code-L1 同理（DDL inside @Transactional misleading on MySQL — auto-commit 不会 rollback）。 |
| M | Test-M2 | SqlCountTest `@BeforeEach @Transactional` 误导（seed commits regardless） | 实际运作正确；改善 only cosmetic。 |
| M | Test-L3 / Docs-M3 | 没有单独的 detail GET（findById）路径 N+1 测试 | 出 scope；可以作为 v0.0.11 spawn task。findById 路径本身用单查，无 N+1。 |
| M | Docs-M1 | archive/2026-06-08-sprint test-plan.md 24 计数 vs 实际 26 — v0.0.10 既有 bug | 不属 v0.0.10.1 修复范围。 |
| M | Docs-M2 | archive/2026-06-08-sprint test-plan.md line 49 TC-SPR-009 "22 字段全有" 与实际只断 4 字段冲突 | v0.0.10 的不一致；v0.0.10.1 通过 TC-SPR-009-FULL 改写已实质对齐，不动 archive 文字。 |
| M | Docs-M3 | C3 widening 已在 v0.0.10 Phase 5 落地 vs proposal 标 TODO 的细节 | 文字层面已对齐，git blame 体现 C3 由 v0.0.10.1 Phase 4 S08 执行；不冲突。 |
| L | various (9 项) | typo / 注释缺失 / 顺序差异 | 信息性，不阻塞。 |

## D. Phase 6 deliver TODO checklist

- [ ] 归档 `changes/2026-06-09-cleanup/` → `archive/2026-06-09-cleanup/`；标记 `.stdd.yaml` archived。
- [ ] **不**合并 specs/ 主分支（cleanup 的所有 Scenario 都是 v0.0.10 的加固，不引入新的 capability requirement）— 保留 v0.0.10 specs 不变即可；本次 spec 仅用于 PR 内部规格追溯。
- [ ] commit message 明确："cleanup sweep — DROP requirement_id col + N+1 batch enrich + test hardening + archive doc addendum"。
- [ ] 打 tag `v0.0.10.1-cleanup`，不 push。

## E. Recorded Step-0 unresolved (not blocking)

参 §C — 6 M + 9 L 项已记录到 test-report 附录。不阻塞交付。
