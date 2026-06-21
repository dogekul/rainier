# Capability: admin-compliance — v0.0.41 (NEW)

> NEW capability。管理员合规仪表盘 read-model：审计活动聚合 + 停用-残留权限对账。admin-gated
> （AdminPaths Tier A `/api/compliance`）。纯读聚合 AuditLog/User/UserRole/Role，零写、零新表。路线图 #10。

## ADDED Requirements

### Requirement: 审计活动聚合

后端 SHALL 提供 `GET /api/compliance/audit-summary`（admin-gated），返回 `{ total, byAction:[{label,count}],
byEntityType:[{label,count}], recent:[最近 20 条审计行] }`。byAction/byEntityType SHALL 按 count 降序（最频在前）。

#### Scenario: 总量与按动作（最频在前）

- **GIVEN** 审计表有 3 条 CREATE、1 条 UPDATE、1 条 DELETE（共 5 条）
- **WHEN** admin 调 `GET /api/compliance/audit-summary`
- **THEN** SHALL 返回 200
- **AND** body.total SHALL 为 5
- **AND** body.byAction[0] SHALL 为 `{label:"CREATE", count:3}`（最频在前）
- **AND** body.recent SHALL 含 5 条

#### Scenario: 按实体类型聚合

- **GIVEN** 审计表有 2 条 TASK、1 条 REQUIREMENT
- **WHEN** admin 调 `GET /api/compliance/audit-summary`
- **THEN** body.byEntityType[0] SHALL 为 `{label:"TASK", count:2}`

#### Scenario: 空审计表

- **GIVEN** 审计表为空
- **WHEN** admin 调 `GET /api/compliance/audit-summary`
- **THEN** body.total SHALL 为 0
- **AND** body.byAction / body.recent SHALL 为空数组

### Requirement: 停用-残留权限对账

后端 SHALL 提供 `GET /api/compliance/residual-permissions`（admin-gated），返回 停用（`enabled=false`、未软删）
且仍持有 ≥1 个 UserRole 授权 的用户 `[{userId, name, loginName, roleCount, roleNames[]}]`。启用用户、无角色的停用
用户 SHALL NOT 返回。

#### Scenario: 仅停用且有角色

- **GIVEN** ghost（停用）有 1 角色 DEV；alice（启用）有角色；empty（停用）无角色
- **WHEN** admin 调 `GET /api/compliance/residual-permissions`
- **THEN** 结果 SHALL 仅含 ghost
- **AND** ghost 项 SHALL 含 `roleCount:1` 与 `roleNames:["DEV"]`

#### Scenario: 无残留

- **GIVEN** 无停用且持角色的用户
- **WHEN** admin 调 `GET /api/compliance/residual-permissions`
- **THEN** SHALL 返回空数组

### Requirement: 合规端点 admin 门控

`/api/compliance/**` SHALL 经 AdminPaths Tier A 门控：无 token→401，非管理员→403，管理员→200。

#### Scenario: 无 token 拒绝

- **WHEN** 无 token 调 `GET /api/compliance/audit-summary`（admin-authz 开启）
- **THEN** SHALL 返回 401

#### Scenario: 非管理员拒绝

- **WHEN** 非管理员调 `GET /api/compliance/residual-permissions`（admin-authz 开启）
- **THEN** SHALL 返回 403

#### Scenario: 管理员放行

- **WHEN** 管理员调 `GET /api/compliance/audit-summary`（admin-authz 开启）
- **THEN** SHALL 返回 200
