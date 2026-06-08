# Capability: entity-requirement

> Change log:
> - 2026-06-05 (v0.0.6-demand-requirement) — capability introduced; `project_id` BIGINT NULL placeholder column added without validation.
> - 2026-06-07 (v0.0.8-project) — `projectId` activated (strict create/update validation + projectName/projectCode enrichment + startup self-heal). `ownerUserId` mutability reversed: now mutable, service validates new owner exists.
> - 2026-06-08 (v0.0.9-story) — DELETE FK protection extended with Story-reference check (ordered after demand_requirement check); `storyCount` enrichment added to GET single + list paths.

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

#### Scenario: v0.0.9 加 — 有 Story 引用被拒

- **GIVEN** 需求 id=1 在 `rainier_story` 中有 ≥ 1 行 `requirement_id=1 AND del_flag=0`；无 demand_requirement 引用
- **WHEN** `DELETE /api/requirements/1`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "requirement has linked stories"

#### Scenario: v0.0.9 加 — 同时有 Demand 和 Story 引用时优先返 demand 错误

- **GIVEN** 需求 id=1 同时有 ≥ 1 demand_requirement 行 AND ≥ 1 story 行
- **WHEN** `DELETE /api/requirements/1`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "requirement has linked demands"（先检查 demand 引用，与 v0.0.8 Project FK 检查顺序家族一致）

### Requirement: storyCount 富化（v0.0.9 加）

后端 SHALL 在 `GET /api/requirements/{id}` 与 `GET /api/requirements?...` 列表的每个返回项中加入 `storyCount` 字段，值为该 Requirement 关联的、`del_flag=0` 的 Story 行数（含所有状态）。

#### Scenario: GET 详情 + list 项含 storyCount

- **GIVEN** Requirement id=1 下有 3 个 Story（status: DRAFT、IN_PROGRESS、DONE，del_flag 都为 0）
- **WHEN** `GET /api/requirements/1`
- **THEN** SHALL 返回 200
- **AND** body.storyCount SHALL 为 3
- **AND** `GET /api/requirements?page=0&size=20` 返回的列表项 SHALL 也含 `storyCount = 3`
