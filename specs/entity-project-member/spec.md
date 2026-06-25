# Capability: entity-project-member

## ADDED Requirements

### Requirement: 项目成员关系表 + 项目内角色

系统 SHALL 提供 `rainier_project_member` 表承载 (project, user) 多对多成员关系，含 role 字段(枚举 PD/DEV/QA/DESIGN/BIZ/OPS/OTHER)，UNIQUE(project_id, user_id, del_flag) 一人一项目一行。

#### Scenario: 添加成员

- **GIVEN** 项目 id=3 (KG 平台) 存在，owner=lina (id=4)
- **AND** 当前用户是 owner (lina)
- **WHEN** `POST /api/projects/3/members` body `{userId: 6, role: "DEV"}`
- **THEN** 系统 SHALL 返回 201
- **AND** 返回体 SHALL 含 userId=6 / role="DEV" / userName="陈敏"
- **AND** `rainier_project_member` SHALL 出现该行 del_flag=0 / joined_by="lina"

#### Scenario: 非法 role 拒绝

- **GIVEN** 项目 id=3 / 当前用户是 owner
- **WHEN** `POST /api/projects/3/members` body `{userId: 6, role: "FAKE"}`
- **THEN** 系统 SHALL 返回 400
- **AND** 错误消息 SHALL 含 "invalid role"

#### Scenario: 重复添加同一成员 409

- **GIVEN** 项目 3 已有 user 6 / role=DEV
- **WHEN** 再次 `POST /api/projects/3/members` body `{userId: 6, role: "QA"}`
- **THEN** 系统 SHALL 返回 409
- **AND** 错误消息 SHALL 含 "已是项目成员"

### Requirement: owner 不可作为成员添加

系统 SHALL 拒绝将项目 owner 作为成员添加（owner 通过 UNION 自动出现在成员列表中）。

#### Scenario: 添加 owner 为成员

- **GIVEN** 项目 3 owner=lina (id=4)
- **WHEN** `POST /api/projects/3/members` body `{userId: 4, role: "PD"}`
- **THEN** 系统 SHALL 返回 400
- **AND** 错误消息 SHALL 含 "该用户已是项目负责人"

### Requirement: 成员管理权限

系统 SHALL 仅允许 project.ownerUserId、project.pmoUserId、adminAccess=true 的用户调用成员管理接口 (add/update/delete)。

#### Scenario: 非授权用户被拒

- **GIVEN** 项目 3 owner=lina / pmo=黎立 / 当前用户=陈敏 (普通用户)
- **WHEN** `POST /api/projects/3/members` body `{userId: 7, role: "DEV"}`
- **THEN** 系统 SHALL 返回 403

#### Scenario: 项目 PMO 可加成员

- **GIVEN** 项目 3 owner=lina / pmo=黎立 / 当前用户=黎立
- **WHEN** `POST /api/projects/3/members` body `{userId: 7, role: "QA"}`
- **THEN** 系统 SHALL 返回 201

### Requirement: 成员 role 可更新

系统 SHALL 提供 `PUT /api/projects/{id}/members/{userId}` 仅修改 role 字段，权限同 add。

#### Scenario: 改 role 成功

- **GIVEN** 项目 3 已有 user 6 / role=DEV
- **AND** 当前用户是 owner
- **WHEN** `PUT /api/projects/3/members/6` body `{role: "QA"}`
- **THEN** 系统 SHALL 返回 200
- **AND** 返回体 SHALL 含 role="QA"

### Requirement: 成员可删除（owner 不可）

系统 SHALL 提供 `DELETE /api/projects/{id}/members/{userId}` 软删除成员；删除 owner 时 SHALL 返回 400。

#### Scenario: 删普通成员成功

- **GIVEN** 项目 3 已有 user 6 / role=DEV
- **AND** 当前用户是 owner
- **WHEN** `DELETE /api/projects/3/members/6`
- **THEN** 系统 SHALL 返回 200
- **AND** `rainier_project_member` 该行 SHALL del_flag=1

#### Scenario: 删 owner 拒绝

- **GIVEN** 项目 3 owner=4 / 当前用户=owner
- **WHEN** `DELETE /api/projects/3/members/4`
- **THEN** 系统 SHALL 返回 400
- **AND** 错误消息 SHALL 含 "不可移除负责人"

### Requirement: 成员列表 UNION 合成 owner + pmo

系统 SHALL 提供 `GET /api/projects/{id}/members` 返回列表：第一行合成 owner (role="OWNER")；第二行合成 pmo (role="PMO", 仅当 pmo_user_id 非空且 != ownerUserId)；后续为真实 project_member 行按 joined_at DESC。

#### Scenario: 列表含合成行 + 真实行

- **GIVEN** 项目 3 owner=lina (id=4) / pmo=黎立 (id=2)
- **AND** project_member 已有 user 6 (role=DEV) joined 2 天前 + user 7 (role=QA) joined 1 天前
- **WHEN** `GET /api/projects/3/members`
- **THEN** 系统 SHALL 返回 200 含 4 行
- **AND** 第 1 行 SHALL userId=4 role="OWNER" displayLabel="负责人"
- **AND** 第 2 行 SHALL userId=2 role="PMO" displayLabel="项目PMO"
- **AND** 第 3 行 SHALL userId=7 role="QA" (新到)
- **AND** 第 4 行 SHALL userId=6 role="DEV" (旧)

#### Scenario: pmo == owner 时不重复

- **GIVEN** 项目 3 owner=lina / pmo=lina (同人)
- **AND** 无其他 member
- **WHEN** `GET /api/projects/3/members`
- **THEN** 系统 SHALL 返回 1 行
- **AND** 该行 role="OWNER" (不重复合成 PMO 行)
