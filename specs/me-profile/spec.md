# Capability: me-profile

> NEW capability (v0.0.40-me-profile, 2026-06-18)。`GET /api/me/profile` 自助个人贡献/能力档案 read-model
> （org 身份 + 岗位 + 直接上级 + 贡献计数）。all-users（token 必需，非 admin 门控）。纯聚合既有
> UserOrganization/Position/Story/Task 数据，零写、零新表。路线图 #9。见 [[frontend-scaffold]]。

## ADDED Requirements

### Requirement: 我的个人档案

后端 SHALL 提供 `GET /api/me/profile`（token 必需，非 admin 门控），返回当前用户的：身份（userId/loginName/name/
positionName/positionCategory）、在岗组织关系列表 `memberships`（organizationId/organizationName/organizationType/
role(HEAD|MEMBER)/isPrimary，`leftAt IS NULL`）、直接上级 `manager`、贡献计数（ownedStoryCount/assignedTaskCount）。
直接上级 = 从 primary org 沿组织树（parentId）上溯取首个**非本人**的在岗 HEAD（深度上限 8 防环）；无则 null。

#### Scenario: 返回身份 + 岗位 + 组织关系

- **GIVEN** 用户 alice（name="Alice"，positionId 指向岗位 "后端工程师"/category="TECH"）在组织 T（TEAM）为 MEMBER（primary，leftAt 为空）
- **WHEN** alice 携带有效 token `GET /api/me/profile`
- **THEN** SHALL 返回 HTTP 200
- **AND** body SHALL 含 `name="Alice"` / `positionName="后端工程师"` / `positionCategory="TECH"`
- **AND** body.memberships SHALL 含一项 `{organizationId: T.id, role:"MEMBER", isPrimary:true}`

#### Scenario: 直接上级 = 上溯首个非本人在岗 HEAD

- **GIVEN** alice 在 TEAM T（primary）为 MEMBER；bob 是 T 的在岗 HEAD
- **WHEN** alice `GET /api/me/profile`
- **THEN** body.manager SHALL 为 `{userId: bob.id, name: bob.name}`

#### Scenario: 团队 HEAD 的上级取父组织 HEAD

- **GIVEN** alice 是 TEAM T 的在岗 HEAD；T 的父组织 DEPT D 的在岗 HEAD 是 carol
- **WHEN** alice `GET /api/me/profile`
- **THEN** body.manager SHALL 为 carol（跳过本人，上溯到父组织 HEAD）

#### Scenario: 无上级时 manager 为空

- **GIVEN** alice 在 primary org 既是唯一 HEAD 且父组织无 HEAD（或无 primary org）
- **WHEN** alice `GET /api/me/profile`
- **THEN** body.manager SHALL 为 null
- **AND** SHALL 返回 200（不报错）

#### Scenario: 贡献计数

- **GIVEN** alice 负责 3 个 Story、被分配 5 个 Task（均未软删）
- **WHEN** alice `GET /api/me/profile`
- **THEN** body.ownedStoryCount SHALL 为 3
- **AND** body.assignedTaskCount SHALL 为 5

#### Scenario: 缺 token 拒绝

- **WHEN** 未携带 token `GET /api/me/profile`
- **THEN** SHALL 返回 HTTP 401

#### Scenario: token 主体无对应用户则降级

- **GIVEN** token 的 sub 为 "system"，数据库无 loginName="system" 的用户
- **WHEN** `GET /api/me/profile`
- **THEN** SHALL 返回 200
- **AND** body.loginName SHALL 为 "system"
- **AND** body.memberships SHALL 为空数组、manager SHALL 为 null、ownedStoryCount/assignedTaskCount SHALL 为 0
