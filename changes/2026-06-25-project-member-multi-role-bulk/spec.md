# Spec — C8 项目成员多角色 + bulk add (v0.0.88)

## 数据模型

### 新表 `rainier_project_member_role`
| 字段 | 类型 | 备注 |
|---|---|---|
| id | BIGINT PK | BaseEntity |
| project_member_id | BIGINT NOT NULL | FK 概念，指向 rainier_project_member.id |
| project_role | VARCHAR(16) NOT NULL | ∈ ProjectMemberRole.ALL |
| create_by/time, update_by/time, del_flag | — | BaseEntity 标配 |

UniqueConstraint `uk_pmr_member_role` (project_member_id, project_role)。
软删除：`@SQLDelete + @Where`。

### 既有 `ProjectMember.role`
保留；read 时若 `roles[]` 非空，约定 `role` ∈ `roles[]`（用作向后兼容的「首选 role」）。

## API

### `POST /api/projects/{projectId}/members/bulk`
鉴权：`canManageProjectMembers`。

Body:
```json
{ "memberUserIds": [10, 11, 12], "projectRoles": ["DEV", "QA"] }
```

行为：
- 笛卡尔积 NOT 展开 —— 每个 userId 都获得 *projectRoles 全集*
- 已是 owner 的 userId → 跳过（不 409）
- 已是 member 的 userId → 在其现有 ProjectMember 上 merge 新 roles（idempotent）
- 新 member → 创建 ProjectMember（`role` 取 projectRoles[0]） + 为每个 role 写一条 `ProjectMemberRoleAssignment`
- 非法 role → 400
- 空 memberUserIds 或空 projectRoles → 400

返回：`List<ProjectMemberDetail>`（每个 userId 一行）。

### `GET /api/project-members/{id}/roles`
返回 `[{id, projectRole}]`。鉴权：任何登录用户（read）。

### `POST /api/project-members/{id}/roles`
Body: `{ "projectRole": "QA" }`。鉴权：`canManageProjectMembers`（基于 member 所在 project）。

行为：
- 非法 role → 400
- 已存在 (memberId, role) → 409
- OK → 201 返回 `{id, projectMemberId, projectRole}`

### `DELETE /api/project-members/{id}/roles/{role}`
鉴权：`canManageProjectMembers`。

行为：
- 删除指定 role assignment（软删）
- 若删后 roles 为空：保留 ProjectMember.role 不变（不级联删 member；显式 DELETE 成员走原 `/api/projects/{pid}/members/{uid}`）

## DTO 富化
`ProjectMemberDetail` 新增：
```java
private List<String> roles; // sorted, distinct
```
合成 OWNER/PMO 行：`roles = [role]`。

## 启动迁移 `ProjectMemberRoleBackfill`
- `@ConditionalOnProperty("app.migration.project-member-role.enabled", havingValue="true", matchIfMissing=true)`
- 在测试 profile 关闭
- 逻辑：遍历所有 ProjectMember，若其 role 非空且 `rainier_project_member_role` 无 (member.id, member.role) 记录 → 插入一条
- idempotent：第二次启动 0 改动

## 测试
- `BulkAddMembersTest`：
  - bulk add 3 人 × 2 role → DB 6 条 assignment + 3 条 ProjectMember
  - 重复 bulk → merge 不抛错
  - owner 被跳过
- `ProjectMemberRoleAssignmentControllerTest`：
  - add/list/remove role
  - 非法 role → 400
  - 重复 → 409
  - 非授权 → 403
- `ProjectMemberRoleBackfillTest`：
  - 存量 PM 启动后 assignment 表自动同步
  - 二次启动 idempotent
