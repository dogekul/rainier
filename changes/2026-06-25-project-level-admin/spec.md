# Spec — project-level-admin (B5, v0.0.78)

## 数据

### ProjectMember 新增字段

```java
@Column(name = "is_project_admin", nullable = true)
private Boolean isProjectAdmin;

public Boolean getIsProjectAdmin() {
  return Boolean.TRUE.equals(isProjectAdmin);  // null→false
}
public void setIsProjectAdmin(Boolean v) { this.isProjectAdmin = v; }
```

DDL：`ddl-auto=update`，nullable 列，老行默认 NULL（=false 等价）。

### Repository 新查询

```java
List<ProjectMember> findByProjectIdAndIsProjectAdminTrue(Long projectId);
List<ProjectMember> findByUserIdAndIsProjectAdminTrue(Long userId);
```

## Service

### `ProjectAdminService`（新）

- `isProjectAdmin(userId, projectId): boolean`
  - findByProjectIdAndUserId + getIsProjectAdmin()==true
- `listProjectAdmins(projectId): List<Long>` — userIds
- `listAdminProjects(userId): List<Long>` — projectIds
- `grant(projectId, userId, byUsername)` — 找不到 ProjectMember 则建（role=OTHER，joinedBy=byUsername）；存在则置 isProjectAdmin=true
- `revoke(projectId, userId)` — 存在则置 false；不存在静默通过（200）

写方法用 grant/revoke 名字以避开 AuditAspect 的 create/update/delete 匹配？检查既有 AuditAspect 规则后决定；若仍要 audit，方法名按 update 命名。**决定**：用 `update` 前缀让 AuditAspect 自动记录（`updateGrant` / `updateRevoke`）。

### ProjectService.update / delete 增加校验

新增方法 `requireProjectWritePermission(currentUserId, project)`：
- isAdmin(currentUserId) → OK
- userId == project.ownerUserId → OK
- userId == project.pmoUserId → OK
- projectAdminService.isProjectAdmin(currentUserId, project.id) → OK
- 其它 → ForbiddenException

把校验插在 ProjectController.update/delete 进入 service 之前，或者 service 内取 HttpServletRequest（前者更清晰）。**决定**：放 Controller，注入 AuthzService + ProjectAdminService。

## API

### POST /api/projects/{id}/admins/{userId}（global admin only）

- 200 + 当前管理员列表（List<Long> userIds）
- 401/403 by SecurityFilter + AuthzService.isAdmin

### DELETE /api/projects/{id}/admins/{userId}（global admin only）

- 200（幂等）

### GET /api/auth/me 增加 `adminProjectIds`

```json
{
  "id": 1,
  "username": "alice",
  "roles": [...],
  "projects": [...],
  "aiAuthLevel": "BASIC",
  "adminProjectIds": [10, 12]
}
```

MeResponse 加字段 + MeService 用 projectMemberRepo.findByUserIdAndIsProjectAdminTrue 填充。

## 测试

### ProjectAdminServiceTest（unit，DataJpaTest 或 SpringBootTest）

- TC-PADMIN-001: grant 创建新 ProjectMember 行（user 不在 ProjectMember 中）
- TC-PADMIN-002: grant 已是 ProjectMember 行（仅置 flag）
- TC-PADMIN-003: revoke 已 admin → flag=false，ProjectMember 行保留
- TC-PADMIN-004: isProjectAdmin true / false / 不存在
- TC-PADMIN-005: listProjectAdmins / listAdminProjects

### ProjectControllerProjectAdminTest（SpringBootTest + MockMvc）

- TC-PCTRL-PA-001: 项目管理员 PUT own project → 200
- TC-PCTRL-PA-002: 项目管理员 PUT other project → 403
- TC-PCTRL-PA-003: 非 admin/owner/pmo/项目管理员 PUT → 403
- TC-PCTRL-PA-004: global admin PUT 任意 project → 200
- TC-PCTRL-PA-005: project owner PUT own → 200（不破坏既有契约）
- TC-PCTRL-PA-006: POST /api/projects/{id}/admins/{uid} 非 admin → 403
- TC-PCTRL-PA-007: POST 由 global admin 调 → 200，listProjectAdmins 包含 uid
- TC-PCTRL-PA-008: DELETE 撤销 → flag false

### MeServiceTest 新增

- TC-ME-ADMINPROJECTS-001: 用户是 2 个项目的项目管理员 → adminProjectIds.size()==2

## Java 8 注意

- 不使用 List.of / Map.of / Optional.orElseThrow() 无参 / Stream.toList / var
- 用 `new ArrayList<>()` + Stream.collect(Collectors.toList())
