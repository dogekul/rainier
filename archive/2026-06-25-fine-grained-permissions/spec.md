# Spec — fine-grained-permissions (B4, v0.0.77)

> 在 v0.0.21 路径前缀 + 提升角色门禁之上叠加方法级 `@RequiresPermission(POINT)` 声明式细粒度授权。

## ADDED Requirements

### Requirement: PermissionPoint 枚举

后端 SHALL 提供 `com.rainier.authz.PermissionPoint` 枚举，列出受控功能点：
USER_MANAGE / ROLE_MANAGE / AUDIT_VIEW / COMPLIANCE_VIEW / PROJECT_ADMIN，每个枚举值带 description。

#### Scenario: 枚举完整

- **GIVEN** 应用启动后
- **WHEN** 调用 `PermissionPoint.values()`
- **THEN** SHALL 包含上述 5 个常量
- **AND** 每个 `description()` 非空

### Requirement: @RequiresPermission 注解

后端 SHALL 提供 `com.rainier.authz.RequiresPermission` 方法注解，支持 `value PermissionPoint[]`
与 `level()` 取值 `ANY`/`ALL`（缺省 ANY）。

### Requirement: RolePermission 实体 + 仓储

后端 SHALL 提供 JPA 实体 `rainier_role_permission(id, role_id NOT NULL, permission_point NOT NULL)`，
带唯一约束 `(role_id, permission_point)`；以及 `RolePermissionRepository.findByRoleIdIn(roleIds)`。

### Requirement: PermissionService 解析用户权限

后端 SHALL 提供 `PermissionService.hasPermission(Long userId, PermissionPoint point)`：
按 user → user_role(role_id) → role_permission 解析；用户不存在或无任何匹配 → false。

#### Scenario: 用户角色拥有该权限点

- **GIVEN** alice 关联 role ADMIN 且 ADMIN 拥有 AUDIT_VIEW
- **WHEN** `permissionService.hasPermission(aliceId, AUDIT_VIEW)`
- **THEN** SHALL 返回 true

#### Scenario: 用户无该权限点

- **GIVEN** bob 关联 role DEV，DEV 无任何 permission
- **WHEN** `permissionService.hasPermission(bobId, AUDIT_VIEW)`
- **THEN** SHALL 返回 false

### Requirement: PermissionInterceptor 强制注解

后端 SHALL 提供 `PermissionInterceptor extends HandlerInterceptor`，注册在 `AdminAuthorizationInterceptor`
之后。SHALL 反射读取 handler method 上 `@RequiresPermission`：
- 未声明 → 放行
- 已声明 & 无 token → 401（UnauthorizedException）
- 已声明 & token 用户无对应权限点 → 403（ForbiddenException）
- 已声明 & token 用户权限满足 → 放行

`level=ANY` 表示数组中任一权限点命中即通过；`level=ALL` 表示全部需要命中。

#### Scenario: 已声明且无权限 → 403

- **GIVEN** 注解 `@RequiresPermission(AUDIT_VIEW)` 已挂在 `/api/audit-logs` GET
- **AND** bob 是 admin（adminAccess=true）但其 role 无 AUDIT_VIEW
- **WHEN** GET /api/audit-logs Authorization: Bearer <bob>
- **THEN** SHALL 返回 403

#### Scenario: 已声明且具备权限 → 通过

- **GIVEN** alice 的 role 拥有 AUDIT_VIEW
- **WHEN** GET /api/audit-logs Authorization: Bearer <alice>
- **THEN** SHALL 返回 200

### Requirement: 角色权限 admin 维护端点

后端 SHALL 在 `/api/admin/roles/{id}/permissions` 提供：
- GET：列出某角色当前权限点集合
- POST body `{"permissionPoint":"AUDIT_VIEW"}`：授予权限（已存在视作幂等成功）
- DELETE `/api/admin/roles/{id}/permissions/{permissionPoint}`：撤销权限

路径在 `AdminPaths` Tier A `/api/admin` 兜底之下，仍需 admin 身份。

## OutOfScope

- 用户级直接授权（先经角色）
- 资源级 ACL
- 前端基于权限点的菜单隐藏
