# Capability: entity-story

## MODIFIED Requirements (from change 2026-06-08-sprint)

### Requirement: Story 改挂 Sprint（v0.0.9 requirementId 反转 → sprintId NN）

v0.0.10 起，Story 的父实体从 Requirement 改为 Sprint。`Story.requirementId` 列在 DB 层面遗留为死列（ddl-auto=update 不删列），**代码层面**：`Story` entity / DTO / Service / Repository 不再读取该列。新增 `Story.sprintId BIGINT NN`（DB 列层面 nullable 由 LegacyStoryToSprintMigration 在 NN 约束生效前填好；Java 层 `@Column(nullable=false)` + DTO `@NotNull` 阻止新写入 NULL）。Service create 时校验 sprintId 存在；从 sprintId → sprint.requirementId → requirement.projectId 二段链路继承 projectId。

#### Scenario: POST Story 含 sprintId 创建 + 二段富化

- **GIVEN** 数据库存在 Project id=1 (code="PROJ-1", name="Apollo") / Requirement id=1 (code="REQ-1", title="登录流程") / Sprint id=10 (code="SPR-A", name="Phase 1", requirement_id=1) / User id=1 (loginName="alice")
- **WHEN** 客户端 `POST /api/stories` body `{"code":"STR-A1","title":"S1","sprintId":10,"ownerUserId":1}`
- **THEN** 系统 SHALL 返回 HTTP 201
- **AND** body.sprintId SHALL 为 10
- **AND** body.projectId SHALL 为 1（从 sprint.requirement.projectId 继承）
- **AND** body SHALL 富化 `sprintCode="SPR-A"` / `sprintName="Phase 1"`
- **AND** body SHALL 富化 `requirementCode="REQ-1"` / `requirementTitle="登录流程"`（来自 sprint 二段 join requirement）
- **AND** body SHALL 富化 `projectName="Apollo"` / `projectCode="PROJ-1"` / `ownerName="Alice"` / `ownerLoginName="alice"`

#### Scenario: POST Story sprintId 不存在被拒

- **GIVEN** 数据库无 Sprint id=999
- **WHEN** `POST /api/stories` body 含 `sprintId=999`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "sprint not found"

#### Scenario: POST Story 缺 sprintId 被拒

- **GIVEN** 后端已启动
- **WHEN** `POST /api/stories` body `{"code":"STR-X","title":"X","ownerUserId":1}`（缺 sprintId）
- **THEN** SHALL 返回 400
- **AND** body.fieldErrors[*].field SHALL 含 `"sprintId"`

#### Scenario: GET Story 详情含 sprintCode/sprintName + requirementCode/requirementTitle

- **GIVEN** Story id=1 存在，关联 Sprint id=10 (code="SPR-A", name="Phase 1") / Requirement id=1 (code="REQ-1", title="登录流程")
- **WHEN** `GET /api/stories/1`
- **THEN** SHALL 返回 200
- **AND** body 字段集 SHALL 等于 `[id, code, title, description, acceptanceCriteria, status, priority, complexity, sprintId, sprintCode, sprintName, requirementCode, requirementTitle, projectId, projectName, projectCode, ownerUserId, ownerName, ownerLoginName, closeReason, createTime, updateTime, createBy, updateBy]`
- **AND** body SHALL **不**含 `requirementId` 字段（v0.0.10 移除）

#### Scenario: 按 sprintId 过滤列表

- **GIVEN** Sprint id=10 下有 3 个 Story；Sprint id=20 下有 2 个
- **WHEN** `GET /api/stories?sprintId=10`
- **THEN** body.total SHALL 为 3
- **AND** body.content[*].sprintId SHALL 全为 10
