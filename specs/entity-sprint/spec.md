# Capability: entity-sprint

> NEW capability from `archive/2026-06-08-sprint` (v0.0.10-sprint, 2026-06-09).
> Sprint is the management-layer decomposition between Requirement and Story:
> Requirement → Sprint → Story. **Hierarchical only — NOT an agile time-box**;
> the service does NOT enforce `start_date ≤ end_date` (TC-SPR-008 explicitly
> verifies `end < start` still returns 201). `start_date` / `end_date` / `goal`
> are reference metadata. `ownerUserId` mutable (sibling of v0.0.8 Decision 6b).
> `requirementId` immutable after creation. Code uniqueness is service-level
> (no DB UNIQUE — family pattern). FK protection on delete (Story references).
> 4-state machine (PLANNING / ACTIVE / COMPLETED / CANCELLED). Soft-deleted.
>
> v0.0.10 also ships `LegacyStoryToSprintMigration` — a CommandLineRunner that
> on first v0.0.10 boot creates a default Sprint per legacy v0.0.9 Requirement
> with orphan Stories, migrates `Story.sprint_id`, then ALTERs the column to
> NOT NULL. Idempotent on second boot.

## Requirements

### Requirement: 创建 Sprint

后端 SHALL 通过 `POST /api/sprints` 接受 `code` + `name` + `requirementId` + `ownerUserId`（必填），其余字段使用默认值；持久化并返回 201。

#### Scenario: 最小 payload 创建 Sprint + 默认值 + 富化

- **GIVEN** 数据库存在 Project id=1 / Requirement id=1 (owner=alice, projectId=1) / User id=1 (loginName="alice", name="Alice")
- **WHEN** 客户端 `POST /api/sprints` body `{"code":"SPR-001","name":"Phase 1","requirementId":1,"ownerUserId":1}`
- **THEN** 系统 SHALL 返回 HTTP 201
- **AND** body 默认值 SHALL 为 `status="PLANNING"` / `startDate=null` / `endDate=null` / `goal=null`
- **AND** body SHALL 富化 `ownerName="Alice"` / `ownerLoginName="alice"` / `requirementCode="REQ-1"` / `requirementTitle="登录流程"` / `projectName` / `projectCode`
- **AND** body.storyCount SHALL 为 0（新 Sprint 无 Story）

#### Scenario: code 重复被拒（service 级唯一）

- **GIVEN** 数据库已存在 code="SPR-001" Sprint
- **WHEN** 再 `POST /api/sprints` 同 code="SPR-001"
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "code already exists"

#### Scenario: requirementId 不存在被拒

- **GIVEN** 数据库无 Requirement id=999
- **WHEN** `POST /api/sprints` body 含 `requirementId=999`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "requirement not found"

#### Scenario: ownerUserId 不存在被拒

- **GIVEN** 数据库无 User id=999_999
- **WHEN** `POST /api/sprints` body 含 `ownerUserId=999999`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "owner user not found"

#### Scenario: 非法 status 被拒

- **GIVEN** 后端已启动
- **WHEN** `POST /api/sprints` body 含 `status="UNKNOWN"`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "invalid status"

#### Scenario: 缺必填字段被拒

- **GIVEN** 后端已启动
- **WHEN** `POST /api/sprints` body `{"code":"SPR-X"}`（缺 name / requirementId / ownerUserId）
- **THEN** SHALL 返回 400
- **AND** body.fieldErrors[*].field SHALL 同时含 `"name"` / `"requirementId"` / `"ownerUserId"`

#### Scenario: createBy 自动注入登录 username

- **GIVEN** JWT 当前 username="alice"
- **WHEN** `POST /api/sprints` body 创建 Sprint
- **THEN** SHALL 返回 201
- **AND** body.createBy SHALL 非空（由 AuditorAwareImpl 自动注入）

#### Scenario: 时间字段不做一致性校验

- **GIVEN** 后端已启动
- **WHEN** `POST /api/sprints` body 含 `startDate="2026-12-01", endDate="2026-01-01"`（end 早于 start）
- **THEN** SHALL 返回 201（service 不做时间一致性校验，dates 是参考元数据）
- **AND** body.startDate / body.endDate SHALL 原样返回

### Requirement: 查询 Sprint

后端 SHALL 通过 `GET /api/sprints/{id}` 返回单 Sprint 详情（含富化）；通过 `GET /api/sprints?requirementId=&status=&search=&page=&size=` 返回 PageResponse。

#### Scenario: GET 详情完整字段集 + 富化

- **GIVEN** Sprint id=1 存在，关联 Requirement id=1 (code=REQ-1, title="登录流程") / Project id=1 (code=PROJ-1, name="Apollo") / User id=1，其下有 2 个 Story
- **WHEN** `GET /api/sprints/1`
- **THEN** SHALL 返回 200
- **AND** body 字段集 SHALL 等于 `[id, code, name, description, goal, status, startDate, endDate, requirementId, requirementCode, requirementTitle, projectId, projectName, projectCode, ownerUserId, ownerName, ownerLoginName, storyCount, createTime, updateTime, createBy, updateBy]`
- **AND** body.storyCount SHALL 为 2

#### Scenario: 按 requirementId 过滤列表

- **GIVEN** Requirement id=1 下有 3 个 Sprint；Requirement id=2 下有 2 个
- **WHEN** `GET /api/sprints?requirementId=1`
- **THEN** body.total SHALL 为 3
- **AND** body.content[*].requirementId SHALL 全为 1

#### Scenario: 按 status 过滤列表

- **GIVEN** 数据库 2 个 ACTIVE + 1 个 COMPLETED Sprint
- **WHEN** `GET /api/sprints?status=ACTIVE`
- **THEN** body.total SHALL 为 2
- **AND** body.content[*].status SHALL 全为 "ACTIVE"

### Requirement: 更新 Sprint（含 owner 可改）

后端 SHALL 通过 `PUT /api/sprints/{id}` 修改 code（重检唯一） / name / description / goal / status / `ownerUserId`（可改）/ startDate / endDate；`requirementId` 不可改（payload 含则静默忽略）；新 `ownerUserId` 不存在 → 400。

#### Scenario: 更新 status + goal

- **GIVEN** Sprint id=1，status="PLANNING"，goal=null
- **WHEN** `PUT /api/sprints/1` body 含 `{"code":"SPR-1","name":"X","ownerUserId":1,"status":"ACTIVE","goal":"完成登录核心流程"}`
- **THEN** SHALL 返回 200
- **AND** body.status SHALL 为 "ACTIVE"
- **AND** body.goal SHALL 为 "完成登录核心流程"

#### Scenario: PUT 改 ownerUserId 转移负责人 + 富化跟随

- **GIVEN** Sprint id=1，ownerUserId=1；用户 id=2 loginName="lili" / name="黎立" 存在
- **WHEN** `PUT /api/sprints/1` body 含 `ownerUserId=2`
- **THEN** SHALL 返回 200
- **AND** body.ownerUserId SHALL 为 2
- **AND** body.ownerName SHALL 为 "黎立"
- **AND** body.ownerLoginName SHALL 为 "lili"

#### Scenario: PUT 新 ownerUserId 不存在被拒

- **GIVEN** Sprint id=1 存在；用户 id=999_999 不存在
- **WHEN** `PUT /api/sprints/1` body 含 `ownerUserId=999999`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "owner user not found"

### Requirement: 软删 Sprint（FK 保护）

后端 SHALL 通过 `DELETE /api/sprints/{id}` 标记 `del_flag=1`；若有 Story 引用 → 409 "sprint has linked stories"。

#### Scenario: 无引用软删成功

- **GIVEN** Sprint id=1，无 Story 引用
- **WHEN** `DELETE /api/sprints/1`
- **THEN** SHALL 返回 204
- **AND** 后续 `GET /api/sprints/1` SHALL 返回 404

#### Scenario: 有 Story 引用 → 409

- **GIVEN** Sprint id=1 在 `rainier_story` 表中有 ≥ 1 行 `sprint_id=1 AND del_flag=0`
- **WHEN** `DELETE /api/sprints/1`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "sprint has linked stories"

### Requirement: 启动自愈迁移 v0.0.9 旧 Story + 收尾 ALTER 升级为 DB NN

后端 SHALL 在每次启动时由 `LegacyStoryToSprintMigration` CommandLineRunner 扫描 `rainier_story.sprint_id IS NULL AND del_flag = 0` 的行；按其 `requirement_id` 分组；为每组创建 `code="SPRINT-DEFAULT-{requirement.code}"` / `name="默认 Sprint"` / `status="ACTIVE"` / `owner=requirement.owner` 的 Sprint；将该组 Story 的 `sprint_id` 更新为新 Sprint id；log INFO per Requirement + summary per run。迁移完成后 SHALL 执行 native `ALTER TABLE rainier_story MODIFY COLUMN sprint_id BIGINT NOT NULL`，把列约束从 NULL 升级为 NOT NULL。Subsequent boots（无 sprint_id IS NULL 行时）SHALL be no-op（早退，不重复执行 ALTER）。

#### Scenario: 首次启动迁移旧 Story + 收尾 ALTER

- **GIVEN** rainier_story 表存在一行 sprint_id=NULL requirement_id=1；DB 列 sprint_id 当前为 BIGINT NULL（Hibernate ADD COLUMN 默认）；rainier_requirement id=1 (code="REQ-1", owner_user_id=1) 存在；rainier_sprint 表无任何 code="SPRINT-DEFAULT-REQ-1" 的行
- **WHEN** 后端启动 → LegacyStoryToSprintMigration.run() 执行
- **THEN** 启动后 `rainier_sprint` 表 SHALL 新增 1 行 code="SPRINT-DEFAULT-REQ-1" / name="默认 Sprint" / status="ACTIVE" / requirement_id=1 / owner_user_id=1
- **AND** 该旧 Story 的 sprint_id SHALL 等于新 Sprint id（非 NULL）
- **AND** 启动后 DB 层 SHALL 显示 `DESCRIBE rainier_story` 中 sprint_id 行 Null="NO"（ALTER 已升级为 NOT NULL）
- **AND** 应用日志 SHALL 含 INFO 行 `"legacy story migrated to default sprint: requirement_id=1 → sprint_id=<新 id>, 1 stories"`
- **AND** 应用日志 SHALL 含 INFO 行 `"LegacyStoryToSprintMigration: created 1 default sprints, migrated 1 stories; sprint_id column upgraded to NOT NULL"`

#### Scenario: 二次启动幂等无操作（无 NULL 行早退，不重复 ALTER）

- **GIVEN** 所有 rainier_story.sprint_id 均非 NULL（首次启动已迁移完）；DB 列 sprint_id 已是 NOT NULL
- **WHEN** 后端再次重启 → LegacyStoryToSprintMigration.run() 执行
- **THEN** 应用日志 SHALL **不**含 `"LegacyStoryToSprintMigration: created"` summary 行（migration early return）
- **AND** 不 SHALL 新建任何 Sprint 行
- **AND** DB 列 sprint_id SHALL 保持 NOT NULL 不变

## MODIFIED Requirements (from change 2026-06-11-sprint-feature-link / v0.0.14)

### Requirement: Sprint 持有可选 productId（惰性建立、不可变）

后端 SHALL 在 `rainier_sprint` 提供 nullable `productId`；`SprintCreateRequest` 接受可选 `productId`（建时预绑）；`SprintUpdateRequest` 不接受 productId（不可变）；productId 非空后不可改。

#### Scenario: 创建 Sprint 不传 productId → productId 为 null

- **GIVEN** Requirement R 与 User U 存在
- **WHEN** `POST /api/sprints` body 含 `requirementId=R, ownerUserId=U`，不含 productId
- **THEN** SHALL 返回 201
- **AND** body.productId SHALL 为 null

#### Scenario: 创建 Sprint 预绑 productId

- **GIVEN** Requirement R、User U、Product P 存在
- **WHEN** `POST /api/sprints` body 含 `requirementId=R, ownerUserId=U, productId=P`
- **THEN** SHALL 返回 201
- **AND** body.productId SHALL 为 P
- **AND** body.productName SHALL 为 P 的名称

#### Scenario: 更新 Sprint 携带 productId 被静默忽略

- **GIVEN** Sprint S 的 productId 已为 P
- **WHEN** `PUT /api/sprints/{S}` body 含 `productId=Q`（另一产品）
- **THEN** SHALL 返回 200
- **AND** Sprint S 的 productId SHALL 仍为 P（Update 不接受改 productId）

### Requirement: 查询 Sprint 详情含 product 富化

后端 SHALL 在 `GET /api/sprints/{id}` 与列表中返回 `productId` + `productName`（productId 为 null 时 productName 为 null）。

#### Scenario: GET 详情含 productId 与 productName

- **GIVEN** Sprint S 的 productId 为 Product P
- **WHEN** `GET /api/sprints/{S}`
- **THEN** SHALL 返回 200
- **AND** body 字段集 SHALL 含 `productId` 与 `productName`
- **AND** body.productName SHALL 为 P 的名称

### Requirement: 反查 Sprint 下的功能

后端 SHALL 提供 `GET /api/sprints/{id}/features`，返回该 sprint 已挂的 Feature 列表（含 feature 富化字段）。

#### Scenario: 列出 sprint 的功能

- **GIVEN** Sprint S 挂了 Feature F1、F2
- **WHEN** `GET /api/sprints/{S}/features`
- **THEN** SHALL 返回 200
- **AND** 返回数组长度 SHALL 为 2
- **AND** 每项 SHALL 含 featureId / code / name / moduleId

#### Scenario: sprint 不存在 → 404

- **GIVEN** 数据库无 Sprint id=999999
- **WHEN** `GET /api/sprints/999999/features`
- **THEN** SHALL 返回 404
- **AND** body.message SHALL 含 "sprint not found"
