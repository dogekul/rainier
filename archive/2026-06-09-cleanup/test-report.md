# v0.0.10.1-cleanup Test Report

> Change: `changes/2026-06-09-cleanup/`
> Phase: 5 VERIFY
> Mode: long-range full_auto
> Baseline: tag `v0.0.10-sprint` / commit `c806f04`

## 1. 总体概况

| 维度 | 数值 |
|---|---|
| **Backend tests** | **191 / 191 passed (100.0%)** — 0 failures, 0 errors, 0 skipped |
| **Frontend tests** | **41 / 41 passed (100.0%)** — no frontend changes this round |
| **Backend lint** (spotless:apply) | ✅ clean |
| **Frontend tsc + vite build** | ✅ clean (no frontend changes) |
| **E2E** (docker compose, no down -v) | ✅ DROP column verified + cleanup runner two-boot log canonical + curl smoke green |
| **Quality thresholds** (C=0 H≤3 M≤10) | ✅ post-fix: C:0 H:0 M:6 L:9 |

### 1.1 覆盖率诊断（变更文件）

| 变更文件 | 测试覆盖 | 备注 |
|---|---|---|
| `LegacyRequirementIdColumnCleanup.java` | High | TC-CLN-001/002/003 — log capture + INFORMATION_SCHEMA + @Order + 反向列保护 |
| `LegacyStoryToSprintMigration.java` (@Order tweak) | High | 既有 `LegacyStoryToSprintMigrationTest` 不受影响 + TC-CLN-003 反射断言新顺序 |
| `StoryService.java` (batch enrich) | High | 16 既有 StoryController 测试断言富化正确性 + TC-PERF-STR-001 锁死 6 SQL |
| `SprintService.java` (batch enrich + GROUP BY) | High | 17 既有 SprintController 测试 + TC-PERF-SPR-001 锁死 6 SQL |
| `SprintControllerQueryTest.java` (22-field guard) | High | mutation-test 漏一字段就红 |
| `SprintControllerCreateTest.java` (echo guard) | High | $.id/$.code/$.requirementId/$.ownerUserId echo 缩水即红 |
| `RequirementControllerQueryTest.java` (storyCount absent) | High | v0.0.9 残留偷溜回来即红 |

## 2. 按模块统计

| 测试类 | 用例数 | 状态 | 类别 |
|---|---|---|---|
| baseline v0.0.10 (186 tests) | 186 | ✅ all pass — 0 regressions | inherited |
| `LegacyRequirementIdColumnCleanupTest` (NEW) | 3 | ✅ TC-CLN-001/002/003 (with sprint_id survival check) | NEW |
| `StoryListSqlCountTest` (NEW) | 1 | ✅ TC-PERF-STR-001 — 锁死 6 SQL | NEW perf |
| `SprintListSqlCountTest` (NEW) | 1 | ✅ TC-PERF-SPR-001 — 锁死 6 SQL | NEW perf |
| `SprintControllerQueryTest.TC-SPR-009` | (inline 加固) | ✅ 22-field loop + 既有 4 字段 | hardened |
| `SprintControllerCreateTest.TC-SPR-001` | (inline 加固) | ✅ 4 echo asserts + 既有富化 | hardened |
| `RequirementControllerQueryTest.TC-REQS-SPR-003` | (inline 加固) | ✅ storyCount 缺席 + 既有 sprintCount | hardened |
| **Total** | **191** | ✅ | |

## 3. E2E 测试结果（docker compose, no down -v）

| 关键路径 | 期望 | 实测 |
|---|---|---|
| `DESCRIBE rainier_story` 不含 `requirement_id` 列 | column DROPPED | ✅ |
| 首次启动日志 `dropped legacy requirement_id column from rainier_story` | INFO | ✅ |
| 二次重启日志 `no-op — requirement_id column already absent` | INFO | ✅ (幂等) |
| `LegacyStoryToSprintMigration` 依旧执行（@Order tweak 不破坏） | 既有行为保留 | ✅ |
| `DanglingProjectIdCleanup` 序列在最前（HIGHEST_PRECEDENCE） | unchanged | ✅ |
| curl `GET /api/sprints?size=5` 22-field SprintDetail | enrichment 全字段 | ✅ |
| curl `GET /api/requirements/1` sprintCount=2 + storyCount ABSENT | 契约稳定 | ✅ |
| curl `GET /api/stories?size=5` sprintCode/requirementCode/projectCode 富化正确 | 2-stage join | ✅ |

## 4. 失败项详细分析

无失败项。

## 5. 功能 / 测试覆盖对照（spec → tc → impl → test）

| Spec Capability | Spec Requirements | Spec Scenarios | TCs | Tests | 状态 |
|---|---|---|---|---|---|
| entity-story (MODIFIED) | 2 | 3 | TC-CLN-001/002/003 + TC-PERF-STR-001 | 4 backend | ✅ 1:1 |
| entity-sprint (MODIFIED) | 2 | 3 | TC-PERF-SPR-001 + TC-SPR-009-FULL + TC-SPR-001-ECHO | 3 backend (1 new + 2 hardened) | ✅ |
| entity-requirement (MODIFIED) | 1 | 1 | TC-REQS-SPR-003-NEG | 1 backend (hardened) | ✅ |
| **Total** | **5** | **7 + 1 (Sprint POST echo Scenario)** = **8** | **8** | **8** | ✅ 100% Scenario coverage |

> Scenario count adjusted to 8 in §1 metrics — entity-sprint Requirement 2 has 2 Scenarios (22-field guard + POST echo).

## 6. 设计调整说明

参 `design-adjustments.md`:
- **PA-1**: SQL count budget 5 → 6（Spring Data findAll(spec, pageable) 双查事实）
- **4 Step 0 fixes** landed inline: Code-M2 null-filter / Test-M1 tautology kill / Test-L2 sprint_id survival / Docs-H1 PA-1 propagation

## 7. Phase 5 Review fixes

### Step 0 — 3 parallel agents

| Agent | Initial findings | Auto-fixed |
|---|---|---|
| Code quality | C:0 H:0 M:2 L:3 | M2 (null filter) ✅ / L2 (Math.min tautology — same as Test-M1) ✅ / M1+L1+L3 recorded |
| Test / config | C:0 H:0 M:2 L:4 | M1 (perf test tautology) ✅ / L2 (sprint_id survival) ✅ / M2+L1+L3+L4 recorded |
| Docs / specs | C:0 H:1 M:4 L:3 | H1 (PA-1 propagation) ✅ / M1+M2+M3+M4 recorded; L1+L2+L3 recorded |

### Final classification after Step 0.3

| Severity | Initial | Post-fix | Threshold | Disposition |
|---|---|---|---|---|
| Critical | 0 | 0 | =0 | ✅ pass |
| High | 1 | 0 | ≤3 | ✅ all closed |
| Medium | 8 | 6 | ≤10 | ✅ within threshold |
| Low | 10 | 9 | informational | recorded |

### Step 1-3 — quality checks

- **Step 1** mvn test + spotless + (frontend untouched) + E2E：✅ 全绿
- **Step 2** Diff 审查：✅ ~10 backend modified/added files all within proposal §A/§B; 3 archive files within §C
- **Step 3** 11-failure-mode check:
  - (a) Hallucination: ✅ no fabricated APIs
  - (b) Scope creep: ✅ none — strictly within proposal §A+§B+§C
  - (c) Cascade error: ✅ null-filter added (Code-M2 fix); empty-set early returns preserved
  - (d) Context loss: ✅ PA-1 propagation reconciled spec ↔ test ↔ design
  - (e) Tool misuse: ✅ Edit/Write/Bash 正确分工
  - (f) Runtime behavior: ✅ E2E confirmed cleanup runner fires + DROP COLUMN succeeds + log canonical
  - (g) Pipeline break: ✅ mvn → docker build (DOCKER_BUILDKIT=0 workaround for transient registry EOF) → curl 完整链证
  - (h) Content quality: ✅ 22-field SprintDetail / sprintCount absence 契约 enforce
  - (i) Instruction decay: ✅ long-range pre_auth 全程遵守
  - (j) Coverage vacuum: ✅ 8 Scenarios → 8 TCs，全自动化覆盖
  - (k) Contract break: ✅ TC-REQS-SPR-003-NEG fence 住 storyCount 不能回来；TC-SPR-009-FULL fence 住 SprintDetail 22 字段

## 8. 结论

| 信号 | 状态 |
|---|---|
| Backend tests | ✅ 191/191 |
| Frontend tests | ✅ 41/41 (no changes) |
| Lint + build | ✅ clean |
| E2E (DROP + 两-boot 日志 + curl 全链) | ✅ |
| Critical findings | ✅ 0 |
| High findings | ✅ 0 (initial H1 fixed inline) |
| Medium findings | ✅ 6 (within ≤10) |
| Spec → TC → Test coverage | ✅ 100% (8/8 Scenarios) |
| PA-1 propagation | ✅ spec / design / test-plan / test code 四方一致 = 6 |

**部署建议**: Ship.

- Tag `v0.0.10.1-cleanup` after Gate 3 confirmation.
- Phase 6 deliver：**不**合并 specs/ 主分支（cleanup 不引入新 capability requirement，只加固 v0.0.10 已有契约）。
- v0.0.11+ candidates: `findById` 路径 SQL count 测试 / SprintsPage 全 CRUD / Hibernate Statistics 推广到其他服务。
