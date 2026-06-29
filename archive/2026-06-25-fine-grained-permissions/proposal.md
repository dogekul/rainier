# Proposal — fine-grained-permissions (B4, v0.0.77)

## 背景

v0.0.21 通过 `AdminPaths` Tier A/B + `AdminAuthorizationInterceptor` 实现了"路径前缀 + 提升角色（adminAccess=true）"
的粗粒度门禁。在管理面进一步分化（审计、合规、用户管理、角色管理、项目管理）后，需要在「提升角色」之上叠加
**功能点级**的细粒度授权，使得：

- 不同管理员可被授予 AUDIT_VIEW、COMPLIANCE_VIEW、USER_MANAGE 等独立权限点；
- 控制器方法上通过声明式注解 `@RequiresPermission(POINT)` 表达；
- 路径级 `AdminPaths` 兜底仍然存在（必须先是 admin / Tier A 才能进到方法解析阶段）。

## 范围

1. `enum PermissionPoint` — USER_MANAGE / ROLE_MANAGE / AUDIT_VIEW / COMPLIANCE_VIEW / PROJECT_ADMIN（带 description）
2. `@RequiresPermission(value, level=ANY|ALL)` — 方法注解，支持单点或多点
3. `RolePermission` JPA 实体（rainier_role_permission，唯一约束 role_id + permission_point）
4. `RolePermissionRepository.findByRoleIdIn(...)`
5. `PermissionService.hasPermission(userId, point)` — user → roles → role permissions
6. `PermissionInterceptor` — 反射方法注解，未授权抛 ForbiddenException(403)
7. WebMvcConfig 注册顺序：AdminAuthorizationInterceptor 之后
8. 给 `AuditLogController` + `ComplianceController` 加 `@RequiresPermission(AUDIT_VIEW)` 演示
9. NEW admin 端点：GET/POST/DELETE `/api/admin/roles/{id}/permissions`

## OutOfScope

- 用户级直接授权（绕过角色）
- 资源级 / 行级 ACL
- 前端权限点隐藏菜单（后续做）

## commit

`feat(fine-grained-permissions): B4 PermissionPoint + @RequiresPermission (v0.0.77)`
