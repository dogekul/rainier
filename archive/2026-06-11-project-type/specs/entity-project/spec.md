# Capability: entity-project

> MODIFIED by `changes/2026-06-11-project-type` (v0.0.16-project-type, 2026-06-11):
> Project 实体新增可扩展 `projectType` 字段(初始 `CASUAL`/`FORMAL`，默认 `CASUAL`)。
> 「轻量→正式 的转化」= 普通 update 改 `projectType`，无审批、无完整性校验门，仅枚举合法性校验。
> DB 列 nullable + 启动 `ProjectTypeBackfill` 回填存量为 `CASUAL` + 读路径 null→CASUAL 兜底。
> 既有 v0.0.8/v0.0.11 Requirements 全部保留不变;此文件仅列本次 ADDED Requirements。

## ADDED Requirements (from change 2026-06-11-project-type / v0.0.16)

### Requirement: 创建项目带项目类型（默认 CASUAL）

后端 SHALL 在 `POST /api/projects` 接受可选 `projectType`；省略则默认 `CASUAL`，显式传须 ∈ {CASUAL, FORMAL}，否则 400。

#### Scenario: 省略 projectType 默认 CASUAL

- **GIVEN** 数据库已存在用户 id=1
- **WHEN** 客户端 `POST /api/projects` body `{"code":"PT-001","name":"X","ownerUserId":1}`（无 projectType）
- **THEN** 系统 SHALL 返回 201
- **AND** body.projectType SHALL 为 `"CASUAL"`

#### Scenario: 显式 FORMAL 创建正式项目

- **GIVEN** 数据库已存在用户 id=1
- **WHEN** `POST /api/projects` body 含 `"projectType":"FORMAL"`
- **THEN** 系统 SHALL 返回 201
- **AND** body.projectType SHALL 为 `"FORMAL"`

#### Scenario: 非法 projectType 被拒

- **GIVEN** backend 已启动
- **WHEN** `POST /api/projects` body 含 `"projectType":"XXX"`
- **THEN** 系统 SHALL 返回 400
- **AND** body.message SHALL 含 `"invalid project type"`

### Requirement: 更新项目类型（轻量→正式转化，无审批/无校验门）

后端 SHALL 在 `PUT /api/projects/{id}` 接受 `projectType`：非空且合法则替换(即转化)；非法则 400；**省略/null 则保留原值**(不静默降级)。转化无需审批、无完整性校验门。

#### Scenario: CASUAL 改 FORMAL 完成转化

- **GIVEN** 项目 id=1 当前 `projectType="CASUAL"`
- **WHEN** `PUT /api/projects/1` body `{"name":"X","status":"ACTIVE","ownerUserId":1,"projectType":"FORMAL"}`
- **THEN** 系统 SHALL 返回 200
- **AND** body.projectType SHALL 为 `"FORMAL"`

#### Scenario: update 省略 projectType 保留原值（防静默降级）

- **GIVEN** 项目 id=1 当前 `projectType="FORMAL"`
- **WHEN** `PUT /api/projects/1` body 含 name/status/ownerUserId 但**不含** projectType
- **THEN** 系统 SHALL 返回 200
- **AND** body.projectType SHALL 仍为 `"FORMAL"`（保留，不被清空或降级为 CASUAL）

#### Scenario: update 非法 projectType 被拒

- **GIVEN** 项目 id=1 存在
- **WHEN** `PUT /api/projects/1` body 含 `"projectType":"XXX"`
- **THEN** 系统 SHALL 返回 400
- **AND** body.message SHALL 含 `"invalid project type"`

### Requirement: 按项目类型过滤列表

后端 SHALL 在 `GET /api/projects?projectType=` 按 `projectType` 精确过滤。

#### Scenario: 按 projectType 过滤仅返回匹配项

- **GIVEN** 数据库 2 个 `FORMAL` + 1 个 `CASUAL` 项目
- **WHEN** `GET /api/projects?projectType=FORMAL`
- **THEN** body.total SHALL 为 2
- **AND** body.content 全部 `projectType="FORMAL"`

### Requirement: 项目详情含项目类型

后端 SHALL 在 `GET /api/projects/{id}` 与列表项的 `ProjectDetail` 响应中包含 `projectType` 字段。

#### Scenario: 详情字段集含 projectType

- **GIVEN** 项目 id=1 `projectType="FORMAL"`
- **WHEN** `GET /api/projects/1`
- **THEN** 系统 SHALL 返回 200
- **AND** body SHALL 含字段 `projectType="FORMAL"`
- **AND**（合并到既有「GET 详情返完整字段」scenario 时，字段集 SHALL 追加 `projectType`）

### Requirement: 存量项目类型回填（启动自愈 + 读兜底）

系统 SHALL 在启动时将 `rainier_project` 中 `project_type IS NULL` 的存量行回填为 `CASUAL`；读路径 SHALL 对 null `projectType` 兜底返回 `CASUAL`。回填 SHALL 不改动该行其它列。

#### Scenario: 启动回填 NULL 行为 CASUAL

- **GIVEN** `rainier_project` 有一行 `project_type IS NULL`（存量），其 code/name/owner 等字段已知
- **WHEN** 应用启动，`ProjectTypeBackfill` 运行
- **THEN** 该行 `project_type` SHALL 变为 `"CASUAL"`
- **AND** 该行 code/name/status/ownerUserId/startDate/endDate/enabled SHALL 一字未改

#### Scenario: 回填前读路径 null→CASUAL 兜底

- **GIVEN** 某行 `project_type IS NULL` 尚未被回填
- **WHEN** `ProjectDetail.from` 富化该行
- **THEN** 返回的 `projectType` SHALL 为 `"CASUAL"`（DTO 兜底，不返回 null）
