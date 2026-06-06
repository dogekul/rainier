# Capability: entity-user-role

> Change log:
> - 2026-06-05 (v0.0.7-position-role) — capability introduced; `project_id` BIGINT NULL placeholder added without validation.
> - 2026-06-07 (v0.0.8-project) — `projectId` activated (strict create validation + projectName/projectCode enrichment + startup self-heal NULLs dangling refs). NULL still permitted (公司级 hat / 未指派).

## Requirements

### Requirement: 创建用户角色关联

后端 SHALL 通过 `POST /api/user-roles` 接受 `userId` + `roleId` + 可选 `projectId`（v0.0.8 起非 null 时强校验 Project 存在；NULL 仍允许表示公司级 hat），硬持久化（无 del_flag 软删语义）；同 `(userId, roleId, projectId)` 重复 → 409。

#### Scenario: 含 projectId 的合法关联创建

- **GIVEN** 数据库存在 user id=1 与 role id=2
- **WHEN** `POST /api/user-roles` body `{"userId":1,"roleId":2,"projectId":42}`
- **THEN** SHALL 返回 HTTP 201
- **AND** body.id SHALL 为正整数
- **AND** body SHALL 含 `userId=1` / `roleId=2` / `projectId=42`

#### Scenario: projectId 为 null 的公司级 hat 创建

- **GIVEN** 数据库存在 user id=1 与 role id=2
- **WHEN** `POST /api/user-roles` body `{"userId":1,"roleId":2,"projectId":null}`
- **THEN** SHALL 返回 201
- **AND** body.projectId SHALL 为 null

#### Scenario: 同 (userId, roleId, projectId) 重复 → 409 (non-NULL)

- **GIVEN** 已存在关联 `(userId=1, roleId=2, projectId=42)`
- **WHEN** 再 POST 同 `(userId=1, roleId=2, projectId=42)`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "user-role already exists"

#### Scenario: 同 (userId, roleId) 公司级重复 → 409 (NULL 兜底)

- **GIVEN** 已存在关联 `(userId=1, roleId=2, projectId=null)`
- **WHEN** 再 POST 同 `(userId=1, roleId=2, projectId=null)`
- **THEN** SHALL 返回 409（service 层 IS NULL 查询补 MySQL UNIQUE 的 NULL 漏洞）
- **AND** body.message SHALL 含 "user-role already exists"

#### Scenario: 公司级 hat 与 项目级 hat 可共存

- **GIVEN** 已存在关联 `(userId=1, roleId=2, projectId=null)`
- **WHEN** POST `(userId=1, roleId=2, projectId=42)`
- **THEN** SHALL 返回 201（不同 projectId 视为不同关联）
- **AND** 后续 `GET /api/user-roles?userId=1&roleId=2` 列表 total SHALL 为 2

#### Scenario: userId 不存在被拒

- **GIVEN** 数据库无 user id=999_999
- **WHEN** `POST /api/user-roles` body `{"userId":999999,"roleId":2}`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "user not found"

#### Scenario: v0.0.8 起 projectId 不存在被拒（取代 v0.0.7 占位语义）

- **GIVEN** 数据库无 Project id=999
- **WHEN** `POST /api/user-roles` body `{"userId":1,"roleId":2,"projectId":999}`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "project not found"

> v0.0.7 历史注：本 capability 引入时 `projectId` 是占位字段，写入任意 BIGINT 不校验。v0.0.8-project 起强校验存在性；启动自愈把历史脏值 (如 user_role.id=2 projectId=42) NULL 掉。

### Requirement: 查询用户角色关联（含富化）

后端 SHALL 通过 `GET /api/user-roles?userId=&roleId=&projectId=&page=&size=` 返回 PageResponse；返回每行 SHALL 富化 `userName` / `userLoginName` / `roleName` / `roleCode`；v0.0.8 起当 `projectId` 非 null SHALL 同时富化 `projectName` / `projectCode`。

#### Scenario: 列表富化用户与角色字段（projectId=null）

- **GIVEN** 存在 user (id=1, name="Alice", loginName="alice") 与 role (id=2, name="PMO", code="PMO")，关联 (userId=1, roleId=2, projectId=null)
- **WHEN** `GET /api/user-roles?userId=1`
- **THEN** body.content[0] SHALL 含 `userName="Alice"` / `userLoginName="alice"` / `roleName="PMO"` / `roleCode="PMO"`
- **AND** body.content[0] SHALL 同时含 `userId=1` / `roleId=2` / `projectId=null` / `projectName=null` / `projectCode=null`

#### Scenario: v0.0.8 列表 projectId 非 null 时富化 project 字段

- **GIVEN** 存在 Project id=5 (code="PROJ-5", name="Apollo")；关联 (userId=1, roleId=2, projectId=5)
- **WHEN** `GET /api/user-roles?userId=1`
- **THEN** body.content[0] SHALL 含 `projectName="Apollo"` / `projectCode="PROJ-5"`

### Requirement: 启动自愈 dangling project_id（v0.0.8）

后端 SHALL 在每次启动时由 `DanglingProjectIdCleanup` CommandLineRunner 把 dangling project_id（指向已软删 / 已硬删 Project 的引用）原地 SET NULL，并 log WARN 每行清理事件。

#### Scenario: 启动自愈把脏 projectId NULL 掉

- **GIVEN** rainier_user_role 表存在一行 projectId=999；数据库无 Project id=999
- **WHEN** 后端重启
- **THEN** 启动后该行 projectId SHALL 为 null
- **AND** 应用日志 SHALL 含 WARN 行 `"cleaned dangling project_id from rainier_user_role.<id>"` 含原 projectId=999

### Requirement: 硬删用户角色关联

后端 SHALL 通过 `DELETE /api/user-roles/{id}` 物理删除（无 del_flag）；删除后 `GET` 返 404。

#### Scenario: 硬删成功

- **GIVEN** 关联 id=1
- **WHEN** `DELETE /api/user-roles/1`
- **THEN** SHALL 返回 204
- **AND** 后续 `GET /api/user-roles/1` SHALL 返回 404
- **AND** DB `SELECT COUNT(*) FROM rainier_user_role WHERE id=1` SHALL 为 0
