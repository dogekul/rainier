# Capability: entity-project — v0.0.48 project-types delta (MODIFIED)

> 合并入 canonical `specs/entity-project/spec.md`（Phase 6）。项目类型由 轻量/正式 拓展为 轻量 + 三正式子类。见 [[opportunity]] / [[frontend-scaffold]]。

## MODIFIED Requirements (from change 2026-06-23-project-types / v0.0.48)

### Requirement: 项目类型拓展为 轻量 + 三正式子类

`ProjectType.ALL` SHALL = {`CASUAL`(轻量), `CORE_FEATURE`(主业-功能建设), `CORE_TECH`(主业-技术改造), `EXTERNAL_DELIVERY`(对外-交付)}。
create 未指定类型 SHALL 默认 `CASUAL`；create/update 指定非法类型 SHALL 返回 400。`FORMAL` 退役（不在 ALL）。

#### Scenario: 创建对外-交付项目

- **WHEN** `POST /api/projects` body `projectType="EXTERNAL_DELIVERY"`
- **THEN** SHALL 返回 201，projectType SHALL 为 EXTERNAL_DELIVERY

#### Scenario: 非法类型被拒

- **WHEN** `POST /api/projects` body `projectType="FOO"`
- **THEN** SHALL 返回 400

#### Scenario: 默认仍为轻量

- **WHEN** `POST /api/projects` 不带 projectType
- **THEN** SHALL 返回 201，projectType SHALL 为 CASUAL

### Requirement: 既有 FORMAL 项目迁移为主业-功能建设

启动 backfill SHALL 将 `project_type = 'FORMAL'` 的行改为 `'CORE_FEATURE'`，并保留既有 `NULL → 'CASUAL'`；两者幂等、仅改类型列、不动其它业务字段。

#### Scenario: FORMAL → CORE_FEATURE

- **GIVEN** 一条 `project_type='FORMAL'` 的项目
- **WHEN** 应用启动执行 backfill
- **THEN** 该行 `project_type` SHALL 为 `CORE_FEATURE`
- **AND** 其它字段（code/name/owner/status）SHALL 不变

#### Scenario: NULL → CASUAL 保留且幂等

- **GIVEN** 一条 `project_type=NULL` 的项目与一条已是 `CORE_TECH` 的项目
- **WHEN** backfill 运行
- **THEN** NULL 行 SHALL 变 `CASUAL`，`CORE_TECH` 行 SHALL 不变
