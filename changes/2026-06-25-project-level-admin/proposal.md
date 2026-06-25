# Proposal — project-level-admin (B5, v0.0.78)

## 背景

- 当前 `Role.adminAccess` 是全局开关（ElevationService.isElevated → AdminAuthzInterceptor）
- v0.0.64 引入了 ProjectMember（archive/2026-06-25-project-team-and-members），但项目内只有 PD/DEV/QA/... 这种"职能 role"，没有"项目管理员"标志
- ProjectController.update/delete 当前只挡 Tier A admin 路径之外的开放写入；任意登录用户可改任意项目（不合理）

## 目标（B5）

按"单标志位"先打地基（不引入完整项目级 RBAC）：

1. ProjectMember 表追加 `is_project_admin BOOLEAN`（nullable，getter null→false）
2. NEW `ProjectAdminService`
   - `isProjectAdmin(userId, projectId)` — 查 ProjectMember.isProjectAdmin=true
   - `listProjectAdmins(projectId)` — 项目的所有项目管理员 userId 列表
   - `listAdminProjects(userId)` — 该用户作为项目管理员的 projectIds
3. NEW endpoint（global admin only）
   - `POST /api/projects/{id}/admins/{userId}` → 该用户在该项目的 ProjectMember.isProjectAdmin=true
     - 若 user 还不是 ProjectMember：先建一行（role=OTHER）
   - `DELETE /api/projects/{id}/admins/{userId}` → 置 false（不删 ProjectMember 行）
4. `GET /api/auth/me` 返回多加字段 `adminProjectIds: List<Long>` —— 该用户作为项目管理员的所有 projectId
5. ProjectController.update / delete 加校验：
   - 全局 admin（AdminPaths Tier B 不挡 projects；这里 Service 层显式判）OR
   - 项目 owner OR project.pmoUserId OR ProjectAdminService.isProjectAdmin
   - 否则 403

## OutOfScope

- 项目级角色完整体系（仅 isProjectAdmin 单标志）
- 项目级权限点（B4 PermissionPoint 的项目维度版本，后续 B6+）
- 前端项目管理员管理 UI（仅暴露 API）

## 验收

- `mvn test` 全绿（39+ admin 测试 + ProjectMemberControllerTest 12 条 + 既有 ProjectController*Test 不破坏）
- 新增 ProjectAdminServiceTest（isProjectAdmin / listProjectAdmins / listAdminProjects 各 ≥1 case）
- 新增 ProjectControllerProjectAdminTest（项目管理员可改 own，非 admin/owner/pmo/项目管理员不可改 other → 403）
- me() 包含 adminProjectIds 字段（已断言）

## commit

`feat(project-level-admin): B5 项目级 admin 分级 (v0.0.78)`
