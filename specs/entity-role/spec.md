# Capability: entity-role

> Change log:
> - 2026-06-15 (v0.0.20-role-nav) — `Role` gains an `adminAccess` boolean (nullable column
>   `admin_access`, Java default FALSE, getter read-coalesces NULL→false so legacy rows read as
>   non-admin). Surfaced through `RoleCreateRequest`/`RoleUpdateRequest`/`RoleDetail`; create defaults
>   false, update changes it only when non-null. Drives the v0.0.20 role-scoped navigation.

## ADDED Requirements

### Requirement: 创建角色

后端 SHALL 通过 `POST /api/roles` 接受 `code` + `name`（含可选 `description` / `enabled`），持久化并返回 201。

#### Scenario: 最小 payload 创建角色 + 默认值

- **GIVEN** 数据库无角色
- **WHEN** `POST /api/roles` body `{"code":"PMO","name":"PMO"}`
- **THEN** SHALL 返回 HTTP 201
- **AND** body.id SHALL 为正整数
- **AND** body SHALL 含 `code="PMO"` / `name="PMO"` / `enabled=true`
- **AND** `Location` header SHALL 形如 `/api/roles/\d+`

#### Scenario: code 重复被拒（service 级唯一）

- **GIVEN** 数据库已存在 `code="PMO"` 角色
- **WHEN** 再 `POST /api/roles` 同 `code="PMO"`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "code already exists"

#### Scenario: 必填字段缺失被拒

- **GIVEN** backend 已启动
- **WHEN** `POST /api/roles` body 缺 `name`
- **THEN** SHALL 返回 400
- **AND** body.fieldErrors[*].field SHALL 含 "name"

### Requirement: 查询角色

后端 SHALL 通过 `GET /api/roles/{id}` 返回单角色详情；通过 `GET /api/roles?enabled=&search=&page=&size=` 返回 PageResponse。

#### Scenario: 按 id 查询返回完整详情

- **GIVEN** 数据库存在角色 id=1
- **WHEN** `GET /api/roles/1`
- **THEN** SHALL 返回 200
- **AND** body 字段集 SHALL 等于 [id, code, name, description, enabled, createTime, updateTime, createBy, updateBy]

#### Scenario: 软删后查询返 404

- **GIVEN** 角色 id=1 已被软删
- **WHEN** `GET /api/roles/1`
- **THEN** SHALL 返回 404

### Requirement: 更新角色

后端 SHALL 通过 `PUT /api/roles/{id}` 修改 `name` / `description` / `enabled`；不允许改 `code`。

#### Scenario: 更新 name 与 description

- **GIVEN** 角色 id=1，name="PMO"
- **WHEN** `PUT /api/roles/1` body `{"name":"Project Management Office","description":"项目管理办公室"}`
- **THEN** SHALL 返回 200
- **AND** body.name SHALL 为 "Project Management Office"
- **AND** body.description SHALL 为 "项目管理办公室"

### Requirement: 软删角色（FK 保护）

后端 SHALL 通过 `DELETE /api/roles/{id}` 标记 `del_flag=1`；若有任意 user_role 引用 → 409。

#### Scenario: 无 user_role 引用软删成功

- **GIVEN** 角色 id=1，无 user_role 行
- **WHEN** `DELETE /api/roles/1`
- **THEN** SHALL 返回 204
- **AND** 后续 `GET /api/roles/1` SHALL 返回 404

#### Scenario: 有 user_role 引用被拒

- **GIVEN** 角色 id=1 在 `rainier_user_role` 表中有 ≥ 1 行
- **WHEN** `DELETE /api/roles/1`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "role has assignments"

### Requirement: Role 携带 adminAccess 标记 (v0.0.20)

`Role` SHALL 含 `adminAccess` 布尔标记（可空列 `admin_access`，Java 默认 FALSE，读时 NULL→false 兜底），经
`POST/PUT /api/roles` 维护，`RoleDetail` 透出。

#### Scenario: 创建不带 adminAccess 默认 false

- **GIVEN** `POST /api/roles` body 含 code/name 不含 `adminAccess`
- **WHEN** 创建角色
- **THEN** 响应 `adminAccess` SHALL 为 `false`
- **AND** 持久化行 SHALL 读 `adminAccess = false`

#### Scenario: 创建带 adminAccess=true 持久为 true

- **GIVEN** `POST /api/roles` body 含 `adminAccess: true`
- **WHEN** 创建角色
- **THEN** 响应 `adminAccess` SHALL 为 `true`

#### Scenario: 更新切换 adminAccess

- **GIVEN** 既有角色 `adminAccess = false`
- **WHEN** `PUT /api/roles/{id}` body 设 `adminAccess: true`
- **THEN** 更新后 `adminAccess` SHALL 为 `true`

#### Scenario: 存量 NULL admin_access 读为 false

- **GIVEN** 既有 `rainier_role` 行 `admin_access` 列为 NULL（v0.0.20 之前）
- **WHEN** `GET /api/roles/{id}` 读取
- **THEN** 响应 `adminAccess` SHALL 为 `false`
- **AND** 该读取 SHALL NOT 改写既有列值（仍为 NULL）
