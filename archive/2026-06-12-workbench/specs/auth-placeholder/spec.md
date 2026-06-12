# Capability: auth-placeholder

> MODIFIED by `changes/2026-06-12-workbench` (v0.0.18-workbench, 2026-06-12):
> `GET /api/auth/me` 由 `{username}` 扩成返回**完整当前用户上下文** {id, username, name, roles[], projects[]}。
> 既有 login + 无-token-401 行为保留;此文件仅列本次 ADDED Requirements。

## ADDED Requirements (from change 2026-06-12-workbench / v0.0.18)

### Requirement: me 返回当前用户上下文

后端 SHALL 在 `GET /api/auth/me` 由 token 的 username 解析当前 User，返回 `{id, username, name, roles[], projects[]}`；roles 来自该用户的 user-role 行（含 roleCode/roleName + projectId/projectName/projectCode），projects 为其参与项目的去重列表。

#### Scenario: 已知用户返回 id/name/roles/projects

- **GIVEN** 用户 alice（id=U，name="Alice"）有 user-role（role code="PMO" name="PMO"，projectId=P，project code="PRJ-1"）
- **WHEN** 客户端携 alice 的 token `GET /api/auth/me`
- **THEN** 系统 SHALL 返回 200
- **AND** body SHALL 含 `id=U` / `username="alice"` / `name="Alice"`
- **AND** body.roles SHALL 含一项 `roleCode="PMO"` 且 `projectId=P`/`projectName` 富化
- **AND** body.projects SHALL 含 `{id:P, code:"PRJ-1"}`

#### Scenario: 组织级角色（projectId 为 null）也返回

- **GIVEN** 用户 alice 有一条 user-role `projectId=null`（组织级角色）
- **WHEN** `GET /api/auth/me`
- **THEN** body.roles SHALL 含该角色且其 `projectId` 为 null
- **AND** body.projects SHALL **不**因该 null 行新增项目

#### Scenario: username 无对应 User 时降级

- **GIVEN** token 的 sub 为 "system"，数据库无 loginName="system" 的 User
- **WHEN** `GET /api/auth/me`
- **THEN** 系统 SHALL 返回 200
- **AND** body SHALL 含 `username="system"` / `id=null` / `roles=[]` / `projects=[]`（降级，不报错）

### Requirement: me 仍要求有效 token

后端 SHALL 在缺失/无效 token 时 `GET /api/auth/me` 返回 401（行为不变）。

#### Scenario: 无 token 被拒

- **GIVEN** 请求未携带有效 Bearer token
- **WHEN** `GET /api/auth/me`
- **THEN** 系统 SHALL 返回 401
