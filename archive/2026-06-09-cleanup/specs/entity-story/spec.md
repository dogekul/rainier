# Capability: entity-story

## MODIFIED Requirements

### Requirement: 启动自愈 — DROP 遗留 `requirement_id` 列（v0.0.10.1）

后端 SHALL 在启动时通过 `LegacyRequirementIdColumnCleanup` CommandLineRunner（`@Order(HIGHEST_PRECEDENCE + 2)`，排在 `LegacyStoryToSprintMigration` 之后）case-insensitive 探测 `rainier_story.requirement_id` 列是否存在：存在则执行 `ALTER TABLE rainier_story DROP COLUMN requirement_id` 并 log INFO `"LegacyRequirementIdColumnCleanup: dropped legacy requirement_id column from rainier_story"`；不存在则 log INFO `"LegacyRequirementIdColumnCleanup: no-op — requirement_id column already absent"` 并立即返回。runner SHALL 幂等（二次启动自然走 no-op 分支）。

#### Scenario: 首次启动 DROP 遗留列 + 输出 INFO 日志

- **GIVEN** `rainier_story` 表当前含 `requirement_id BIGINT NULL` 列（v0.0.10 状态）
- **WHEN** 应用启动且 `LegacyRequirementIdColumnCleanup.run()` 被调用
- **THEN** 启动后 `rainier_story` 表 SHALL **不**含 `requirement_id` 列（INFORMATION_SCHEMA 查询返回 0 行）
- **AND** 应用日志 SHALL 含一行 INFO `"LegacyRequirementIdColumnCleanup: dropped legacy requirement_id column from rainier_story"`
- **AND** 既有 Story 行 SHALL 完整保留（id / code / sprint_id / 其它字段不变）

#### Scenario: 二次启动 no-op + 输出 no-op 日志

- **GIVEN** `rainier_story` 表已无 `requirement_id` 列（首次启动已 DROP）
- **WHEN** 应用再次启动
- **THEN** runner SHALL 走 no-op 分支
- **AND** 应用日志 SHALL 含 INFO `"LegacyRequirementIdColumnCleanup: no-op — requirement_id column already absent"`
- **AND** `ALTER TABLE` SHALL **不**被执行（无 DDL SQL emit）

### Requirement: `StoryService.list` enrich 批量化（v0.0.10.1 性能）

后端在 `StoryService.list` 的 enrich 路径上 SHALL 对 User / Sprint / Requirement / Project 四种 join 实体各执行一次 `findAllById(setOf(ids))` 后构建 `Map<Long, Entity>`，再以 map lookup 完成每行的富化；SHALL **不**对同类型实体做 per-row `findById` 单查。期望 list size=20 的 enrich 阶段 PreparedStatement 计数 = 6（v0.0.10 实测后修正自 Phase 2 估计的 5；详见 pending-adjustments.md PA-1）。

#### Scenario: `GET /api/stories?size=20` 在 enrich 阶段 = 6 个 SELECT

- **GIVEN** 数据库已 seed 20 个 Story（关联 4 个 Sprint、3 个 Requirement、2 个 Project、5 个不同 User）
- **AND** Hibernate Statistics 已 enabled 且 `stats.clear()` 已在 list 调用前执行
- **WHEN** 客户端调用 `GET /api/stories?page=0&size=20`
- **THEN** 系统 SHALL 返回 HTTP 200 + 20 行 Story 富化结果（projectName / projectCode / ownerName / ownerLoginName / sprintCode / sprintName / sprintStatus / requirementCode / requirementTitle 全部正确）
- **AND** `Statistics.getPrepareStatementCount()` 的增量 SHALL 等于 6 — 1 page-data + 1 page-count（Spring Data `findAll(Specification, Pageable)` 强制双查）+ 4 batch enrich queries（user / sprint / requirement / project）
- **AND** 富化结果 SHALL 与逐行单查的 v0.0.10 实现完全一致（同 seed 同 page 同输出）
