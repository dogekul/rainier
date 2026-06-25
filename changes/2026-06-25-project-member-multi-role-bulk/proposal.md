# Proposal — C8 项目成员多角色 + bulk add (v0.0.88)

## 背景
v0.0.64 `ProjectMember` 一行一个 `role`：一人在一个项目里只能挂一个项目内角色（DEV/QA/...）。
现实场景：同一个人在一个项目里常常兼任多角色（PD+DEV、DEV+QA）。

## 目标
1. **多角色**：一个 ProjectMember 可挂多个 projectRole（M2M，按 (memberId, role) 唯一）。
2. **bulk add**：一次 API 把多个人加入项目，每人可携带多个 role。
3. **平滑迁移**：保留 `ProjectMember.role`（向后兼容，作为「首选 role」），新表 `rainier_project_member_role` 承载多角色。
4. **启动迁移**：flag-gated 把存量 `ProjectMember.role` 复刻一条到 `rainier_project_member_role`（idempotent）。

## 范围
- NEW entity `ProjectMemberRoleAssignment`（表 `rainier_project_member_role`）
  - 字段：`projectMemberId` NOT NULL, `projectRole` String(16) NOT NULL
  - UniqueConstraint(`projectMemberId`, `projectRole`)
- NEW endpoints：
  - `POST /api/projects/{id}/members/bulk` body `{memberUserIds:[...], projectRoles:[...]}`
  - `POST /api/project-members/{id}/roles` body `{projectRole}`
  - `DELETE /api/project-members/{id}/roles/{role}`
  - `GET /api/project-members/{id}/roles`
- `ProjectMemberDetail` 增 `roles[]` 富化字段
- 启动迁移 `ProjectMemberRoleBackfill`（flag: `app.migration.project-member-role.enabled`，默认 true）

## OutOfScope
- 项目内角色权限差异（仍仅身份描述）
- 角色定义页（仍接受任意 String，service 用 `ProjectMemberRole` 常量集校验）

## 兼容
- 现有 `ProjectMember.role` 字段保留；`POST /api/projects/{id}/members`（单加）行为不变
- `update` / `delete` 单成员行为不变
- 新增 `roles[]` 字段在 list 接口里返回；旧前端可忽略
