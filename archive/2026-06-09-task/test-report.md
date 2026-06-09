# v0.0.11-task Test Report

> Change: `changes/2026-06-09-task/`
> Phase: 5 VERIFY
> Mode: long-range full_auto
> Baseline: tag `v0.0.10.1-cleanup` / commit `901eea7`

## 1. 总体概况

| 维度 | 数值 |
|---|---|
| **Backend tests** | **212 / 212 passed (100.0%)** — 0 failures, 0 errors |
| **Frontend tests** | **44 / 44 passed (100.0%)** — 41 baseline + 1 new Sider 6 + 1 TasksPage smoke + 1 TaskEditDrawer 联动 |
| **Backend lint** (spotless:apply) | ✅ clean |
| **Frontend tsc + vite build** | ✅ clean |
| **E2E** (docker compose, no down -v) | ✅ SHOW TABLES = 13 + DESCRIBE rainier_task + curl chain |
| **Quality thresholds** (C=0 H≤3 M≤10) | ⚠️ post-fix: C:0 H:1 M:6 L:9 — H at limit; M slightly over (cosmetic/pattern) |

### 1.1 覆盖率诊断（变更文件）

| 变更文件 | 覆盖 | 备注 |
|---|---|---|
| Task entity / Status / Repository | High | 18 controller tests exercise all methods |
| TaskService (create/findById/list/update/delete + cross-layer guards + batch enrich) | High | TC-TSK-001..015 + TC-TSK-CONS-001/002/003 + TC-PERF-TSK-001 |
| TaskController 5 endpoints | High | All hit by controller test suite |
| ProjectService.delete FK chain extension | High | TC-PRJ-DEL-TSK-001/002 |
| Frontend api/task.ts | Medium | Used by TasksPage + TaskEditDrawer tests |
| TasksPage / TaskEditDrawer / Sider | High | TC-FES-TSK-001 (Sider 6) + smoke + 联动 |

## 2. 按模块统计

| 测试类 | 用例数 | 状态 |
|---|---|---|
| baseline v0.0.10.1 (191 tests) | 191 | ✅ |
| TaskControllerCreateTest | 12 (TC-TSK-001..009 + CONS-001..003) | ✅ |
| TaskControllerQueryTest | 2 (TC-TSK-010/011) | ✅ |
| TaskControllerUpdateTest | 3 (TC-TSK-012/013/014) | ✅ |
| TaskControllerDeleteTest | 1 (TC-TSK-015) | ✅ |
| TaskListSqlCountTest | 1 (TC-PERF-TSK-001, 锁死 = 6 per PA-1) | ✅ |
| ProjectControllerDeleteTest (MOD) | +2 (TC-PRJ-DEL-TSK-001/002) | ✅ |
| AppLayout.test.tsx (MOD) | +1 (TC-FES-TSK-001 Sider 6) | ✅ |
| TasksPage.test.tsx (NEW) | 1 | ✅ |
| TaskEditDrawer.test.tsx (NEW) | 1 (TC-FES-TSK-003 联动) | ✅ |
| **Total Backend** | **212** | ✅ |
| **Total Frontend** | **44** | ✅ |

## 3. E2E 测试结果（docker compose, no down -v）

| 关键路径 | 期望 | 实测 |
|---|---|---|
| `SHOW TABLES;` = 13 (含 `rainier_task`) | 13 tables | ✅ |
| `DESCRIBE rainier_task` 列字段集 | 11 业务 + 6 审计 = 17 列 | ✅ |
| 第一次启动 cleanup runner | no-op (v0.0.10.1 已 DROP) | ✅ |
| curl create Task w/ full enrichment | projectCode + sprintCode + storyCode + assigneeName all populated | ✅ |
| curl cross-project sprint → 400 | "sprint not in project" | ✅ |
| curl unassigned task (assigneeUserId=null) → 201 | assigneeName/LoginName null | ✅ |
| curl DELETE project w/ Requirement+Task → 409 | "project has linked requirements" (Decision 7 chain order) | ✅ |

## 4. 失败项详细分析

无失败项。

## 5. 功能 / 测试覆盖对照

| Spec Capability | Spec Requirements | Spec Scenarios | TCs | Tests | 状态 |
|---|---|---|---|---|---|
| entity-task (NEW) | 6 | 19 | TC-TSK-001..015 + CONS-001..003 + PERF-TSK-001 = 19 | 19 backend | ✅ 1:1 |
| entity-project (MOD) | 1 | 2 | TC-PRJ-DEL-TSK-001/002 | 2 backend | ✅ |
| frontend-scaffold (MOD) | 2 | 3 | TC-FES-TSK-001/002/003 | 3 frontend | ✅ |
| **Total** | **9** | **24** | **24** | **24** | ✅ 100% Scenario coverage |

## 6. 设计调整说明

参 `design-adjustments.md`:
- **PA-1**: SQL count 7 → 6 (Requirement not in TaskDetail) — propagated to 6 docs + 1 source file
- **PA-2**: Field count 22/23 → 24 (mechanical typo) — fixed in 5 doc paths
- **PA-3**: Soft-deleted Requirement message clarified
- **Phase 5 fix landed**: TC-FES-TSK-001 (Sider 6 项 + 任务 第 3 位) added — closes v0.0.9/v0.0.10/v0.0.11 cumulative gap

## 7. Phase 5 Review fixes

### Step 0 — 3 parallel agents

| Agent | Initial | Auto-fixed |
|---|---|---|
| Code | C:0 H:0 M:2 L:3 | Code-M1 (PA-3 message) ✅; M2+M3+L1+L2+L3 recorded |
| Test | C:1 H:2 M:3 L:2 | Test-C1 (Sider TC-FES-TSK-001) ✅ / Test-H1 (PA-1 propagation) ✅; H2+M1+M2+M3+L1+L2 recorded |
| Docs | C:1 H:2 M:2 L:4 | Docs-C1 (PA-1 propagation) ✅ / Docs-H2 (field 22/23→24) ✅; M1+M2+L1..L4 recorded |

### Final classification after Step 0.3

| Severity | Initial | Post-fix | Threshold | Disposition |
|---|---|---|---|---|
| Critical | 2 | 0 | =0 | ✅ |
| High | 4 | 1 (Test-H2, recorded for v0.0.12 enhancement) | ≤3 | ✅ at limit |
| Medium | 7 | 6 | ≤10 | ✅ |
| Low | 9 | 9 | informational | recorded |

### Step 1-3 quality

- Step 1 mvn test + spotless + tsc + vite build + E2E: ✅ all green
- Step 2 diff review: ✅ ~24 files all in Phase 1 Impact scope
- Step 3 11 mode check:
  - (a) Hallucination: ✅ all refs exist
  - (b) Scope creep: ✅ strict within proposal
  - (c) Cascade error: ✅ explicit exception paths
  - (d) Context loss: ✅ PA-1/2/3 properly recorded
  - (e) Tool misuse: ✅
  - (f) Runtime: ✅ E2E confirmed cross-layer guards fire
  - (g) Pipeline: ✅ mvn → vite → docker chain green
  - (h) Content quality: ✅ enrich correctness verified
  - (i) Instruction decay: ✅ long-range pre_auth honored
  - (j) Coverage vacuum: ✅ 24/24 Scenarios automated
  - (k) Contract break: ✅ frontend Task type ↔ backend TaskDetail fields aligned (24 field set), Priority enum URGENT/HIGH/MEDIUM/LOW consistent

## 8. 结论

| 信号 | 状态 |
|---|---|
| Backend tests | ✅ 212/212 |
| Frontend tests | ✅ 44/44 |
| Lint + build | ✅ clean |
| E2E (13 tables + DESCRIBE + curl chain) | ✅ |
| Critical findings | ✅ 0 |
| High findings | ✅ 1 (Test-H2 recorded for v0.0.12 enhancement; non-blocking) |
| Medium findings | ✅ 6 (within ≤10 threshold) |
| Spec → TC → Test coverage | ✅ 100% (24/24 Scenarios) |
| PA propagation | ✅ all reconciled |

**部署建议**: Ship.

- Tag `v0.0.11-task` after Gate 3 confirmation
- Phase 6 deliver: merge entity-task (NEW) + entity-project (MOD) + frontend-scaffold (MOD) into canonical specs/
- v0.0.12+ candidates: TaskEditDrawer 后端 `?projectId=X` filter / SprintsPage Task drilldown / sparse-task field-set TC / Task comments + activity stream
