# v0.0.10.1-cleanup — sweep v0.0.10-sprint review residues

> Baseline: commit `c806f04` / tag `v0.0.10-sprint` (2026-06-09).
> Scope: 全拿下 — A (runtime code) + B (test hardening) + C (archive docs).

## Why

v0.0.10-sprint 引入 Sprint 实体的过程中，Phase 5 Review 留下了 3 条 H 类 + 12 条 M 类未修复项（见 `archive/2026-06-08-sprint/pending-adjustments.md §C/D`）。其中：

- 4 项触及运行代码（DROP 死列、N+1 enrich）
- 3 项是测试断言强化（防 v0.0.9 残留 / 字段集回归 / echo 缺失）
- 3 项是归档文档与 Phase 4 实测的对齐（columnDefinition 没生效 / orphan filter 宽化 / 决策映射）

集中清扫一轮把质量基线收齐，避免技术债跨多个 release 滚雪球。沿用 v0.0.8.1-cleanup 的同款轻量模式。

## What Changes

### A. 运行代码（A1-A2）

- **A1.** NEW `LegacyRequirementIdColumnCleanup` runner — 通过 INFORMATION_SCHEMA 幂等检测 `rainier_story.requirement_id` 是否存在，若存在执行 `ALTER TABLE rainier_story DROP COLUMN requirement_id` + INFO 日志；`@Order(HIGHEST_PRECEDENCE + 2)` 排在 `LegacyStoryToSprintMigration`（改为 `+ 1`）之后，顺带消除 Code-M3 "两个 runner 同 @Order" 隐患。
- **A2.** `StoryService.enrich` / `SprintService.enrich` list 路径批量化 — 每个被 join 的实体类型一次 `findAllById(ids)` + Map 拼接（替代当前 per-row 单查）；`SprintService.storyCount` 改为 native SQL `SELECT sprint_id, COUNT(*) … GROUP BY sprint_id` 一次聚合。**目标**：StoryList(size=20) enrich 阶段 SQL 数 ~80 → ~5。

### B. 测试加固（B1-B3）

- **B1.** `TC-SPR-009` 加 22-字段集 loop-assert（mirror `TC-STR-010` 的写法），字段集为：`[id, code, name, description, goal, status, requirementId, requirementCode, requirementTitle, projectId, projectName, projectCode, ownerUserId, ownerName, ownerLoginName, startDate, endDate, storyCount, createTime, updateTime, createBy, updateBy]`。
- **B2.** `TC-SPR-001` 加 `$.id` / `$.code` / `$.requirementId` / `$.ownerUserId` echo 断言（防 POST 响应回包字段缩水）。
- **B3.** `TC-REQS-SPR-003` 加 `assertFalse(body.has("storyCount"))` 防 v0.0.9 残留偷偷回来。

### C. 归档文档对齐（只动 `archive/2026-06-08-sprint/`，不影响运行）

- **C1.** `design.md` Decision 2 + "Build addendum (2026-06-09)" block — Hibernate 没认 `columnDefinition="BIGINT"` / MySQL `ADD COLUMN NOT NULL` 自动填 `0` / 实际 orphan filter 是 `IS NULL OR =0 OR NOT IN (live sprints)`。
- **C2.** `design.md` 加 proposal 8 决策 ↔ design 14 决策映射表（修 Phase 5 Docs-H1）。
- **C3.** `test-plan.md` TC-MIG + `slices.md` M08 的 native SQL 例子 — orphan filter 从 `IS NULL` 改为宽 filter（修 Docs-H4）。

### 显式不做

- Code-M5 `SprintsPage` 全 CRUD（v0.0.10 user 已确认只读是有意的）
- L 类 typo / ASCII 图 polish
- 不 push 到 remote（user 后续决定）

## Capabilities

### Modified Capabilities

- `entity-story` — Story.requirementId 字段在 v0.0.10 已从代码移除，v0.0.10.1 完成 DB 层 DROP；触发 enrich 批量化。
- `entity-sprint` — enrich 批量化 + storyCount 聚合查询；TC-SPR-001/009 测试加固。
- `entity-requirement` — TC-REQS-SPR-003 加 storyCount 缺席守护。

### New Capabilities

无。

## Impact

**代码层面（~16 文件）**：

- backend NEW: `LegacyRequirementIdColumnCleanup.java` (runner)
- backend MOD: `LegacyStoryToSprintMigration.java` (@Order +1)
- backend MOD: `StoryService.java` / `SprintService.java` (enrich 批量化)
- backend MOD: `StoryRepository.java` (add `countBySprintIdInBatch` 返回 `List<Object[]>` 或 `Map<Long,Long>`)
- backend MOD: 3 test files (B1/B2/B3 断言加固)
- archive DOC: 3 处对齐（design.md / test-plan.md / slices.md）

**配置层面**：

- 无 `application.yml` / `docker-compose.yml` / `.env` 变更

**基础设施**：

- 无新服务 / 新 API endpoint
- DB DDL: 一次 `DROP COLUMN`（幂等，re-run 安全）

## Success Criteria

- [ ] `mvn test` 全绿（≥ 186 backend tests，且新断言生效 → B1/B2/B3 在 mutation 测试下应该红）
- [ ] `npm test` 全绿（41 frontend tests，无 regression）
- [ ] `npm run build` 成功
- [ ] `docker compose up` 后：
  - `DESCRIBE rainier_story` 不再列出 `requirement_id` 列
  - 启动日志含 `LegacyRequirementIdColumnCleanup: dropped legacy requirement_id column` 或同款 no-op 行
  - `LegacyStoryToSprintMigration` 仍执行第一次 boot / no-op 后续
  - 二次重启 `LegacyRequirementIdColumnCleanup` 走 no-op 路径（日志体现）
- [ ] StoryList(size=20) SQL log 显示 enrich 阶段 ≤ 5 条 SELECT（而非 v0.0.10 的 ~80 条）
- [ ] `archive/2026-06-08-sprint/` 内 3 处文档对齐已落地
- [ ] 不 push 到 remote（待 user 后续决定）
