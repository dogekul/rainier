# Capability: entity-requirement

> MODIFIED by `changes/2026-06-12-requirement-enrich` (v0.0.19, 2026-06-12):
> status 6 态调整为 DRAFT/IN_APPROVAL/IN_ANALYSIS/IN_PROGRESS/DELIVERED/CLOSED(不强制转换;存量 remap);
> 共用 Priority 加第 5 级 LOWEST;Requirement 加 expectedDate(LocalDate, 可空)。既有 Requirements 保留。

## ADDED Requirements (from change 2026-06-12-requirement-enrich / v0.0.19)

### Requirement: 需求新状态集（6 态）

后端 SHALL 校验 Requirement status ∈ {DRAFT, IN_APPROVAL, IN_ANALYSIS, IN_PROGRESS, DELIVERED, CLOSED};旧值不再合法。create 省略默认 DRAFT。不强制转换。

#### Scenario: 新状态创建成功

- **GIVEN** 用户 id=1 存在
- **WHEN** `POST /api/requirements` body 含 `status="IN_ANALYSIS"`（+必填 code/title/ownerUserId）
- **THEN** 系统 SHALL 返回 201
- **AND** body.status SHALL 为 `"IN_ANALYSIS"`

#### Scenario: 旧状态值被拒

- **GIVEN** 用户 id=1 存在
- **WHEN** `POST /api/requirements` body 含 `status="APPROVED"`（旧值）
- **THEN** 系统 SHALL 返回 400
- **AND** body.message SHALL 含 `"invalid status"`

### Requirement: 存量状态 remap（启动迁移）

系统 SHALL 在启动时把存量 Requirement 的旧状态 remap 为新值:IN_REVIEW→IN_APPROVAL、APPROVED→IN_ANALYSIS、IN_DEV→IN_PROGRESS、DEPRECATED→CLOSED;DRAFT/DELIVERED 不变。

#### Scenario: 启动 remap 旧状态

- **GIVEN** rainier_requirement 有行 status 分别为 IN_REVIEW / APPROVED / IN_DEV / DEPRECATED / DRAFT / DELIVERED
- **WHEN** 应用启动，`RequirementStatusBackfill` 运行
- **THEN** 这些行 status SHALL 变为 IN_APPROVAL / IN_ANALYSIS / IN_PROGRESS / CLOSED / DRAFT / DELIVERED
- **AND** 各行其它字段 SHALL 不变

### Requirement: 优先级接受 LOWEST（共用 Priority 五级）

后端 SHALL 接受 Priority `LOWEST`（共用枚举第 5 级）;Requirement(及 demand/story/task) create priority=LOWEST 合法。

#### Scenario: requirement 优先级 LOWEST 创建成功

- **GIVEN** 用户 id=1 存在
- **WHEN** `POST /api/requirements` body 含 `priority="LOWEST"`
- **THEN** 系统 SHALL 返回 201
- **AND** body.priority SHALL 为 `"LOWEST"`

### Requirement: 期望交付日期

后端 SHALL 在 Requirement create/update 接受 `expectedDate`(LocalDate, 可空),并在 Detail 返回。

#### Scenario: create + detail 含 expectedDate

- **GIVEN** 用户 id=1 存在
- **WHEN** `POST /api/requirements` body 含 `expectedDate="2026-09-01"`
- **THEN** 系统 SHALL 返回 201
- **AND** body.expectedDate SHALL 为 `"2026-09-01"`

#### Scenario: update 改 expectedDate

- **GIVEN** 需求 id=X 存在
- **WHEN** `PUT /api/requirements/X` body 含 `expectedDate="2026-10-01"`
- **THEN** 系统 SHALL 返回 200
- **AND** body.expectedDate SHALL 为 `"2026-10-01"`
