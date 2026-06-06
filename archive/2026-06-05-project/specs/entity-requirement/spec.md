# Capability: entity-requirement

## MODIFIED Requirements (from change 2026-06-05-project)

### Requirement: 需求的 projectId 校验 + 富化

后端 SHALL 在 `POST /api/requirements` / `PUT /api/requirements/{id}` 时，若 body 含 `projectId` 且非空，校验对应 Project 必须存在；若 NULL 则跳过校验（保持兼容）。响应 body SHALL 富化 `projectName` + `projectCode`。

应用启动时 `DanglingProjectIdCleanup` `CommandLineRunner` SHALL 扫描 `rainier_requirement.project_id` 所有非空值；对不存在 Project 的引用 native UPDATE SET NULL + log WARN。启动后 reads 不再有 dangling 状态，富化逻辑直接 `findById` 即可。

#### Scenario: POST 含 projectId 存在 → 富化

- **GIVEN** 用户 id=1 与项目 id=1（code="PROJ-001"，name="采购系统改造"）存在
- **WHEN** `POST /api/requirements` body `{"code":"REQ-001","title":"X","description":"X","ownerUserId":1,"projectId":1}`
- **THEN** SHALL 返回 201
- **AND** body.projectId SHALL 为 1
- **AND** body.projectName SHALL 为 "采购系统改造"
- **AND** body.projectCode SHALL 为 "PROJ-001"

#### Scenario: POST 含 projectId 不存在 → 400

- **GIVEN** 数据库无 project id=999_999
- **WHEN** `POST /api/requirements` body 含 `projectId=999999`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "project not found"

#### Scenario: POST 含 projectId=null → 兼容

- **GIVEN** 用户 id=1 存在
- **WHEN** `POST /api/requirements` body 含 `projectId=null` 或缺该字段
- **THEN** SHALL 返回 201
- **AND** body.projectId SHALL 为 null
- **AND** body.projectName SHALL 为 null
- **AND** body.projectCode SHALL 为 null

#### Scenario: 启动自愈 — 现有 dangling project_id 被 NULL 化

- **GIVEN** 启动前 `rainier_requirement` 表存在 1 行 projectId=999（无对应 Project）
- **WHEN** Spring 应用启动，`DanglingProjectIdCleanup` 执行
- **THEN** 该行 projectId SHALL 被 SET NULL
- **AND** 应用日志 SHALL 含 WARN 行 `"cleaned dangling project_id from rainier_requirement.<id>"`
- **AND** 后续 `GET /api/requirements/<id>` SHALL 返回 200 + body.projectId=null + body.projectName=null

### Requirement: 需求 owner 可改（v0.0.6 不可改决策的对内修订）

后端 SHALL 在 `PUT /api/requirements/{id}` body 含 `ownerUserId` 且与原值不同时，校验新 user 存在 → 更新。`RequirementUpdateRequest` SHALL 含 `ownerUserId` 字段（必填）。

> 注：本 Requirement 是对 v0.0.6 既有 Scenario "PUT body ownerUserId 静默忽略" 的语义反转 — v0.0.8 起 owner 可改。

#### Scenario: PUT 改 ownerUserId 转移负责人

- **GIVEN** 需求 id=1 当前 ownerUserId=1；用户 id=2 loginName="lili" 存在
- **WHEN** `PUT /api/requirements/1` body `{"code":"REQ-001","title":"X","description":"X","ownerUserId":2}`
- **THEN** SHALL 返回 200
- **AND** body.ownerUserId SHALL 为 2

#### Scenario: PUT 新 ownerUserId 不存在 → 400

- **GIVEN** 需求 id=1 存在；用户 id=999_999 不存在
- **WHEN** `PUT /api/requirements/1` body 含 `ownerUserId=999999`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "owner user not found"
