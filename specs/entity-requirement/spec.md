# Capability: entity-requirement

> Change log:
> - 2026-06-05 (v0.0.6-demand-requirement) — capability introduced; `project_id` BIGINT NULL placeholder column added without validation.
> - 2026-06-07 (v0.0.8-project) — `projectId` activated (strict create/update validation + projectName/projectCode enrichment + startup self-heal). `ownerUserId` mutability reversed: now mutable, service validates new owner exists.
> - 2026-06-08 (v0.0.9-story) — DELETE FK protection extended with Story-reference check (ordered after demand_requirement check); `storyCount` enrichment added to GET single + list paths.
> - 2026-06-09 (v0.0.10-sprint) — Story moved under Sprint, so the Requirement-level FK now blocks on Sprint references (message "requirement has linked sprints"); `storyCount` removed from RequirementDetail and replaced by `sprintCount`. The demand_requirement → sprint check ordering is preserved (demands first).
> - 2026-06-12 (decision — Requirement = Epic, gap B5) — **Requirement IS the agile "Epic"**: the top decomposition unit under Project, broken down into Sprint → Story → Task. The role-card's "Epic" (§卡4 "拆 Epic → Story → Task"; §2 "诉求 → 需求 → Story") maps to this entity — **no separate Epic entity/layer is added**. Where docs list attachment/关联 targets as "项目 / Epic / Story / Task", that means 项目 / 需求(Requirement) / Story / Task. No schema or behavior change; clarification only.
> - 2026-06-15 (v0.0.19-requirement-enrich) — status 6-state set adjusted to DRAFT/IN_APPROVAL/IN_ANALYSIS/IN_PROGRESS/DELIVERED/CLOSED (草稿/审批中/分析中/实施中/已交付/已关闭), freely changeable (no enforced transitions). Legacy statuses remapped at startup by `RequirementStatusBackfill` (IN_REVIEW→IN_APPROVAL, APPROVED→IN_ANALYSIS, IN_DEV→IN_PROGRESS, DEPRECATED→CLOSED). Shared `Priority` gains a 5th level LOWEST. New nullable `expectedDate` (期望交付日期) on create/update/detail (PUT full-replace, same as projectId). See the ADDED Requirements at the end.

## Requirements

### Requirement: 创建需求

后端 SHALL 通过 `POST /api/requirements` 接受 `code` + `title` + `description` + `ownerUserId`，其余字段使用默认值，持久化并返回 201。

#### Scenario: 最小 payload 创建需求 + 默认值

- **GIVEN** 数据库存在 `rainier_user` id=1
- **WHEN** `POST /api/requirements` body `{"code":"REQ-001","title":"加速采购下单","description":"...","ownerUserId":1}`
- **THEN** SHALL 返回 201
- **AND** body.id SHALL 为正整数
- **AND** body 默认值 SHALL 为 `status="DRAFT"` / `priority="MEDIUM"` / `complexity=null` / `projectId=null`
- **AND** `Location` header SHALL 形如 `/api/requirements/\d+`

#### Scenario: code 全局唯一性冲突

- **GIVEN** 数据库已存在 `code="REQ-001"` 需求
- **WHEN** 再 `POST /api/requirements` 同 `code="REQ-001"`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "code already exists"

#### Scenario: ownerUser 不存在被拒

- **GIVEN** 数据库无 id=999_999 的用户
- **WHEN** `POST /api/requirements` body 含 `ownerUserId=999999`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "owner user not found"

### Requirement: 查询需求

后端 SHALL 通过 `GET /api/requirements/{id}` 返回单需求详情；通过 `GET /api/requirements?status=&priority=&projectId=&search=&page=&size=` 返回 PageResponse。

#### Scenario: 按 id 查询返回完整详情（v0.0.8 加入 owner + project 富化）

- **GIVEN** 数据库存在需求 id=1，ownerUserId=1（loginName="alice"），projectId=5（"Apollo"）
- **WHEN** `GET /api/requirements/1`
- **THEN** SHALL 返回 200
- **AND** body 字段集 SHALL 等于 [id, code, title, description, ownerUserId, ownerName, ownerLoginName, status, priority, complexity, projectId, projectName, projectCode, closeReason, createTime, updateTime, createBy, updateBy]
- **AND** body.ownerName / ownerLoginName SHALL 由 service join User 注入
- **AND** body.projectName / projectCode SHALL 由 service join Project 注入（projectId=null 时该两字段为 null）

#### Scenario: 按 projectId 筛选（v0.0.8 起 projectId 强校验存在）

- **GIVEN** 数据库存在项目 id=5；2 个需求 projectId=5，1 个 projectId=null
- **WHEN** `GET /api/requirements?projectId=5`
- **THEN** body.total SHALL 为 2
- **AND** body.content[*].projectId SHALL 全为 5

### Requirement: projectId 强校验 + 启动自愈（v0.0.8 激活）

后端 SHALL 在 `POST /api/requirements` 与 `PUT /api/requirements/{id}` 验证 `projectId` 存在（null 允许；非 null 必须命中 live Project）；后端 SHALL 在每次启动时由 `DanglingProjectIdCleanup` CommandLineRunner 把 dangling project_id（指向已软删 / 已硬删 Project 的引用）原地 SET NULL，并 log WARN `cleaned dangling project_id from rainier_requirement.<id> (was project_id=<old>)`。

#### Scenario: POST projectId 不存在被拒

- **GIVEN** 数据库无 Project id=999
- **WHEN** `POST /api/requirements` body 含 `projectId=999`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "project not found"

#### Scenario: 启动自愈 dangling project_id

- **GIVEN** rainier_requirement 表存在一行 projectId=999；数据库无 Project id=999
- **WHEN** 后端重启
- **THEN** 启动后该行 projectId SHALL 为 null
- **AND** 应用日志 SHALL 含 WARN 行 `"cleaned dangling project_id from rainier_requirement.<id>"`

### Requirement: 更新需求（含 owner 可改 — v0.0.8 反转）

后端 SHALL 通过 `PUT /api/requirements/{id}` 修改 code / title / description / status / priority / complexity / projectId / `ownerUserId`（v0.0.8 起可改） / closeReason；code 变更须重检唯一性；新 `ownerUserId` 不存在 → 400。

> 历史注：v0.0.6 规定 "ownerUserId 不可修改 / silent ignore"；v0.0.8 (archive/2026-06-05-project Decision 6b) 反转为可改 — admin 可转移负责人，service 校验新 owner 存在。createBy/updateBy 审计字段记录变更人。

#### Scenario: 更新状态

- **GIVEN** 需求 id=1，status="DRAFT"
- **WHEN** `PUT /api/requirements/1` body `{"code":"REQ-001","title":"X","description":"X","ownerUserId":1,"status":"APPROVED","priority":"HIGH"}`
- **THEN** SHALL 返回 200
- **AND** body.status SHALL 为 "APPROVED"

#### Scenario: PUT 改 ownerUserId 转移负责人（v0.0.8）

- **GIVEN** 需求 id=1，ownerUserId=1；用户 id=2 loginName="lili" / name="黎立" 存在
- **WHEN** `PUT /api/requirements/1` body 含 `ownerUserId=2`
- **THEN** SHALL 返回 200
- **AND** body.ownerUserId SHALL 为 2
- **AND** body.ownerName SHALL 为 "黎立"（富化跟随）
- **AND** body.ownerLoginName SHALL 为 "lili"

#### Scenario: PUT 新 ownerUserId 不存在被拒（v0.0.8）

- **GIVEN** 需求 id=1 存在；用户 id=999_999 不存在
- **WHEN** `PUT /api/requirements/1` body 含 `ownerUserId=999999`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "owner user not found"

### Requirement: 软删需求（FK 保护）

后端 SHALL 通过 `DELETE /api/requirements/{id}` 标记 `del_flag=1`；若有未删除的 demand_requirement 链接 → 409。

#### Scenario: 无关联软删成功

- **GIVEN** 需求 id=1，无 demand_requirement 行
- **WHEN** `DELETE /api/requirements/1`
- **THEN** SHALL 返回 204
- **AND** 后续 `GET /api/requirements/1` SHALL 返回 404

#### Scenario: 有关联软删被拒

- **GIVEN** 需求 id=1 在 `rainier_demand_requirement` 中有 ≥ 1 行
- **WHEN** `DELETE /api/requirements/1`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "requirement has linked demands"

#### Scenario: v0.0.10 — 有 Sprint 引用被拒 (替换 v0.0.9 Story FK 检查)

- **GIVEN** 需求 id=1 在 `rainier_sprint` 中有 ≥ 1 行 `requirement_id=1 AND del_flag=0`；无 demand_requirement 引用
- **WHEN** `DELETE /api/requirements/1`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "requirement has linked sprints"

#### Scenario: v0.0.10 — 同时有 Demand 和 Sprint 引用时优先返 demand 错误

- **GIVEN** 需求 id=1 同时有 ≥ 1 demand_requirement 行 AND ≥ 1 sprint 行
- **WHEN** `DELETE /api/requirements/1`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "requirement has linked demands"（先检查 demand 引用，与 v0.0.8 Project FK 检查顺序家族一致）

### Requirement: sprintCount 富化（v0.0.10 — 替换 v0.0.9 storyCount）

后端 SHALL 在 `GET /api/requirements/{id}` 与 `GET /api/requirements?...` 列表的每个返回项中加入 `sprintCount` 字段，值为该 Requirement 关联的、`del_flag=0` 的 Sprint 行数（含所有状态）。`storyCount` 字段在 v0.0.10 起不再出现于 RequirementDetail（Story 通过 Sprint 间接归属，跨层计数另谋）。

#### Scenario: GET 详情 + list 项含 sprintCount

- **GIVEN** Requirement id=1 下有 3 个 Sprint（status: PLANNING、ACTIVE、COMPLETED，del_flag 都为 0）
- **WHEN** `GET /api/requirements/1`
- **THEN** SHALL 返回 200
- **AND** body.sprintCount SHALL 为 3
- **AND** body SHALL **不**含 `storyCount` 字段
- **AND** `GET /api/requirements?page=0&size=20` 返回的列表项 SHALL 也含 `sprintCount = 3`

## MODIFIED Requirements (from change 2026-06-11-sprint-feature-link / v0.0.14)

### Requirement: 反查 Requirement 触达的功能（2 跳汇总）

后端 SHALL 提供 `GET /api/requirements/{id}/features`，返回该需求经其所有 sprint 触达的去重 Feature 列表。

#### Scenario: 跨多个 sprint 汇总去重

- **GIVEN** Requirement R 下有 Sprint S1、S2
- **AND** S1 挂 Feature F1、F2，S2 挂 Feature F2、F3
- **WHEN** `GET /api/requirements/{R}/features`
- **THEN** SHALL 返回 200
- **AND** 返回数组 SHALL 含且仅含 F1、F2、F3（F2 去重为 1 项）
- **AND** 每项 SHALL 含 featureId / code / name

#### Scenario: 需求下的 sprint 均未挂功能 → 空数组

- **GIVEN** Requirement R 下有 Sprint 但无任何 sprint-feature 链接
- **WHEN** `GET /api/requirements/{R}/features`
- **THEN** SHALL 返回 200
- **AND** 返回数组 SHALL 为空

#### Scenario: requirement 不存在 → 404

- **GIVEN** 数据库无 Requirement id=999999
- **WHEN** `GET /api/requirements/999999/features`
- **THEN** SHALL 返回 404
- **AND** body.message SHALL 含 "requirement not found"

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
- **WHEN** `POST /api/requirements` body 含 `status="APPROVED"`（旧值，及 IN_REVIEW/IN_DEV/DEPRECATED）
- **THEN** 系统 SHALL 返回 400
- **AND** body.message SHALL 含 `"invalid status"`

### Requirement: 存量状态 remap（启动迁移）

系统 SHALL 在启动时把存量 Requirement 的旧状态 remap 为新值:IN_REVIEW→IN_APPROVAL、APPROVED→IN_ANALYSIS、IN_DEV→IN_PROGRESS、DEPRECATED→CLOSED;DRAFT/DELIVERED 不变;其它字段不变。

#### Scenario: 启动 remap 旧状态

- **GIVEN** rainier_requirement 有行 status 分别为 IN_REVIEW / APPROVED / IN_DEV / DEPRECATED / DRAFT / DELIVERED
- **WHEN** 应用启动，`RequirementStatusBackfill` 运行
- **THEN** 这些行 status SHALL 变为 IN_APPROVAL / IN_ANALYSIS / IN_PROGRESS / CLOSED / DRAFT / DELIVERED
- **AND** 各行其它字段 SHALL 不变

### Requirement: 优先级接受 LOWEST（共用 Priority 五级）

后端 SHALL 接受共用 Priority 第 5 级 `LOWEST`;Requirement 及 demand/story/task create priority=LOWEST 合法（4 实体同校验 `Priority.ALL`）。

#### Scenario: requirement 优先级 LOWEST 创建成功

- **GIVEN** 用户 id=1 存在
- **WHEN** `POST /api/requirements` body 含 `priority="LOWEST"`
- **THEN** 系统 SHALL 返回 201
- **AND** body.priority SHALL 为 `"LOWEST"`

### Requirement: 期望交付日期

后端 SHALL 在 Requirement create/update 接受 `expectedDate`(LocalDate, 可空, PUT 全量替换),并在 Detail 返回。

#### Scenario: create + detail 含 expectedDate

- **GIVEN** 用户 id=1 存在
- **WHEN** `POST /api/requirements` body 含 `expectedDate="2026-09-01"`
- **THEN** 系统 SHALL 返回 201
- **AND** body.expectedDate SHALL 为 `"2026-09-01"`

#### Scenario: update 省略 expectedDate 即清空

- **GIVEN** 需求 id=X 当前 expectedDate="2026-09-01"
- **WHEN** `PUT /api/requirements/X` body 不含 expectedDate
- **THEN** 系统 SHALL 返回 200
- **AND** body.expectedDate SHALL 为 null（全量替换语义,同 projectId）
