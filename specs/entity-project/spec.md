# Capability: entity-project

> NEW capability from `archive/2026-06-05-project` (v0.0.8-project, 2026-06-07).
> Project entity holds ONLY `owner_user_id` — all project-dimensional role hats
> (PM / PMO / TechLead / etc.) live in UserRole M2M. `owner_user_id` is mutable
> (admin transfer); `code` is immutable after creation. FK protection on delete
> (rainier_requirement + rainier_user_role). Service-level code uniqueness
> (no DB UNIQUE — soft-deleted codes can be reused, same family pattern as
> Requirement / Position / Role / Demand).
>
> **Changelog**
> - 2026-06-11 (v0.0.16-project-type) — Project gains an extensible `projectType` field
>   (initial values CASUAL/FORMAL, default CASUAL). 「轻量→正式 conversion」 is a plain `update`
>   of this field — no promote endpoint, no approval, no completeness gate; only enum membership is
>   validated. DB column is **nullable**; legacy rows are backfilled to CASUAL by `ProjectTypeBackfill`
>   at startup, and `ProjectDetail.from` read-coalesces null→CASUAL. create may omit it (→CASUAL);
>   update absent/null **preserves** the current value (no silent downgrade). `GET /api/projects`
>   adds a `projectType` filter. See the ADDED Requirements at the end of this file.
> - 2026-06-12 (v0.0.17-milestone) — `DELETE /api/projects/{id}` now **cascade soft-deletes** the
>   project's milestones (`rainier_milestone`). Milestones are NOT in the FK-block chain — the
>   existing Requirement→UserRole→Task 409 order is unchanged; once those pass, milestones are
>   soft-deleted (same `@Transactional`) before the project. A blocked delete rolls back leaving
>   milestones active. See the ADDED Requirement at the end of this file + capability `entity-milestone`.
> - 2026-06-17 (v0.0.28-scope-substrate) — Project gains a **nullable `organization_id`** column (+
>   Create/Update/Detail) so projects can be tagged to a department/domain/team node for portfolio
>   scoping. ddl-auto safe (nullable); legacy rows NULL. Consumed by `ScopeService` (scope=led) — see
>   capability [[entity-portfolio]]. `ProjectRepository` +`findByOwnerUserId`/`findByOrganizationIdIn`.

## Requirements

### Requirement: 创建项目

后端 SHALL 通过 `POST /api/projects` 接受 `code` + `name` + `ownerUserId`（必填），其余字段使用默认值，持久化并返回 201。

#### Scenario: 最小 payload 创建项目 + 默认值 + 富化

- **GIVEN** 数据库已存在用户 id=1（loginName="alice"，name="Alice"）
- **WHEN** 客户端 `POST /api/projects` body `{"code":"PROJ-001","name":"采购系统改造","ownerUserId":1}`
- **THEN** 系统 SHALL 返回 HTTP 201
- **AND** body.id SHALL 为正整数（JSON number 类型）
- **AND** body SHALL 含 `code="PROJ-001"` / `name="采购系统改造"` / `status="PLANNING"` / `enabled=true` / `ownerUserId=1`
- **AND** body SHALL 富化 `ownerName="Alice"` / `ownerLoginName="alice"`

#### Scenario: code 重复被拒（service 级唯一）

- **GIVEN** 数据库已存在 `code="PROJ-001"` 项目
- **WHEN** 再 `POST /api/projects` 同 `code="PROJ-001"`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "code already exists"

#### Scenario: 缺 ownerUserId 被拒

- **GIVEN** backend 已启动
- **WHEN** `POST /api/projects` body `{"code":"PROJ-002","name":"X"}`（缺 ownerUserId）
- **THEN** SHALL 返回 400
- **AND** body.fieldErrors[*].field SHALL 含 "ownerUserId"

#### Scenario: ownerUserId 不存在被拒

- **GIVEN** 数据库无 id=999_999 的用户
- **WHEN** `POST /api/projects` body 含 `ownerUserId=999999`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "owner user not found"

#### Scenario: 非法 status 被拒

- **GIVEN** backend 已启动
- **WHEN** `POST /api/projects` body 含 `status="UNKNOWN_STATUS"`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "invalid status"

#### Scenario: createBy 自动注入登录 username

- **GIVEN** JWT 当前 username="alice"
- **WHEN** `POST /api/projects` body 创建项目
- **THEN** SHALL 返回 201
- **AND** body.createBy SHALL 为 "alice"（由 AuditorAwareImpl 自动注入）

### Requirement: 查询项目

后端 SHALL 通过 `GET /api/projects/{id}` 返回单项目详情（含富化）；通过 `GET /api/projects?status=&enabled=&search=&page=&size=` 返回 PageResponse。

#### Scenario: GET 详情返完整字段 + 富化 + N+1 容错

- **GIVEN** 项目 id=1 存在 owner_user_id=1，用户 id=1 loginName=alice / name=Alice
- **WHEN** `GET /api/projects/1`
- **THEN** SHALL 返回 200
- **AND** body 字段集 SHALL 等于 [id, code, name, description, status, ownerUserId, ownerName, ownerLoginName, startDate, endDate, enabled, projectType, createTime, updateTime, createBy, updateBy]（v0.0.16 加 `projectType`）
- **AND** body.ownerName SHALL 为 "Alice"
- **AND** body.ownerLoginName SHALL 为 "alice"

#### Scenario: 按 status 过滤列表

- **GIVEN** 数据库 2 个 ACTIVE + 1 个 ARCHIVED 项目
- **WHEN** `GET /api/projects?status=ACTIVE`
- **THEN** body.total SHALL 为 2
- **AND** body.content 全部 `status="ACTIVE"`

### Requirement: 更新项目（含 owner 可改）

后端 SHALL 通过 `PUT /api/projects/{id}` 修改 code（重检唯一） / name / description / status / ownerUserId（可改） / startDate / endDate / enabled。

#### Scenario: 更新 ownerUserId 转移负责人

- **GIVEN** 项目 id=1，owner_user_id=1；用户 id=2 loginName="lili" 存在
- **WHEN** `PUT /api/projects/1` body `{"code":"PROJ-001","name":"X","ownerUserId":2,"status":"ACTIVE"}`
- **THEN** SHALL 返回 200
- **AND** body.ownerUserId SHALL 为 2
- **AND** body.ownerLoginName SHALL 为 "lili"
- **AND** body.status SHALL 为 "ACTIVE"

#### Scenario: PUT 新 ownerUserId 不存在被拒

- **GIVEN** 项目 id=1 存在；用户 id=999_999 不存在
- **WHEN** `PUT /api/projects/1` body 含 `ownerUserId=999999`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "owner user not found"

### Requirement: 软删项目（FK 保护）

后端 SHALL 通过 `DELETE /api/projects/{id}` 标记 `del_flag=1`；若有 Requirement.project_id 或 UserRole.project_id 或 **Task.project_id**（v0.0.11 加）引用 → 409，错误 message 指明哪个表。家族 chain 顺序（由先到后）：Requirement → UserRole → **Task**。

#### Scenario: 无引用软删成功

- **GIVEN** 项目 id=1，无 Requirement / UserRole 引用
- **WHEN** `DELETE /api/projects/1`
- **THEN** SHALL 返回 204
- **AND** 后续 `GET /api/projects/1` SHALL 返回 404

#### Scenario: 被 Requirement 引用 → 409

- **GIVEN** 项目 id=1 在 `rainier_requirement` 表中有 ≥ 1 行（projectId=1）
- **WHEN** `DELETE /api/projects/1`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "project has linked requirements"

#### Scenario: 被 UserRole 引用 → 409

- **GIVEN** 项目 id=1 在 `rainier_user_role` 表中有 ≥ 1 行（projectId=1）
- **WHEN** `DELETE /api/projects/1`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "project has assigned user-roles"

#### Scenario: v0.0.11 — 被 Task 引用 → 409

- **GIVEN** 项目 id=1 在 `rainier_task` 表中有 ≥ 1 行 `project_id=1 AND del_flag=0`；无 Requirement / UserRole 引用
- **WHEN** `DELETE /api/projects/1`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "project has linked tasks"

#### Scenario: v0.0.11 — 同时有 Requirement + Task 引用时优先返 requirement 错误

- **GIVEN** 项目 id=1 同时有 ≥ 1 Requirement 行 AND ≥ 1 Task 行
- **WHEN** `DELETE /api/projects/1`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "project has linked requirements"（家族 chain 顺序：Requirement → UserRole → Task）

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

后端 SHALL 在 `GET /api/projects/{id}` 与列表项的 `ProjectDetail` 响应中包含 `projectType` 字段（见上「查询项目」的字段集 scenario，已含 projectType）。

#### Scenario: 详情字段集含 projectType

- **GIVEN** 项目 id=1 `projectType="FORMAL"`
- **WHEN** `GET /api/projects/1`
- **THEN** 系统 SHALL 返回 200
- **AND** body SHALL 含字段 `projectType="FORMAL"`

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

## ADDED Requirements (from change 2026-06-12-milestone / v0.0.17)

### Requirement: 删除项目级联软删里程碑

后端 SHALL 在 `DELETE /api/projects/{id}` 成功删除项目时，**级联软删**该项目下的全部里程碑(`del_flag=1`)。里程碑的存在**不**阻断项目删除;但既有的 Requirement / UserRole / Task 引用仍优先以 409 阻断（顺序不变），此时项目与其里程碑均不被删除（同事务回滚）。

#### Scenario: 删除有里程碑且无其它引用的项目级联软删里程碑

- **GIVEN** 项目 id=1 有 2 个里程碑，且无 Requirement / UserRole / Task 引用
- **WHEN** `DELETE /api/projects/1`
- **THEN** 系统 SHALL 返回 204
- **AND** 该项目的 2 个里程碑 SHALL 全部 `del_flag=1`（级联软删）
- **AND** 后续 `GET /api/milestones?projectId=1` 的 total SHALL 为 0

#### Scenario: 被 Requirement 引用的项目仍 409 且里程碑不被删

- **GIVEN** 项目 id=1 有 1 个里程碑，且在 `rainier_requirement` 有 ≥1 行 projectId=1
- **WHEN** `DELETE /api/projects/1`
- **THEN** 系统 SHALL 返回 409
- **AND** body.message SHALL 含 `"project has linked requirements"`
- **AND** 该项目的里程碑 SHALL 仍 `del_flag=0`（事务回滚，级联未执行）
