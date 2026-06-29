# Test Report — project-level-admin (B5, v0.0.78)

## 命令

```bash
cd backend && mvn test
```

## 结果

- **Tests run: 701, Failures: 0, Errors: 0, Skipped: 0** ✅
- BUILD SUCCESS, total time 14.528s

## 新增测试

### ProjectAdminServiceTest (8)
- TC-PADMIN-001 grant 新建 ProjectMember 行（role=OTHER）
- TC-PADMIN-002 grant 已有 ProjectMember 行（保留 role，只置 flag）
- TC-PADMIN-003 revoke 保留行，置 flag=false
- TC-PADMIN-003b revoke 对不存在行幂等
- TC-PADMIN-004 isProjectAdmin truth table (true / false / 不存在 / null inputs)
- TC-PADMIN-005 listProjectAdmins + listAdminProjects
- grant unknown project → 404
- grant unknown user → 404

### ProjectControllerProjectAdminTest (10)
- TC-PCTRL-PA-001 项目管理员 PUT own → 200
- TC-PCTRL-PA-002 项目管理员 PUT other → 403
- TC-PCTRL-PA-003 random 用户 PUT → 403
- TC-PCTRL-PA-004 global admin PUT 任意 → 200
- TC-PCTRL-PA-005 owner PUT own → 200
- TC-PCTRL-PA-005b pmo PUT own → 200
- TC-PCTRL-PA-006 非 admin grant → 403
- TC-PCTRL-PA-007 global admin grant → 200 + list
- TC-PCTRL-PA-008 global admin revoke → 200 + empty
- anonymous PUT → 200（test profile / 无 token 保留旧契约）

### AuthMeAdminProjectsTest (2)
- TC-ME-ADMINPROJECTS-001 me 包含 adminProjectIds（2 项）
- me empty 时为空 list 而非 null

## 不破坏既有

- ProjectMemberControllerTest 12 条全过（v0.0.64）
- ProjectController*Test (create/delete/query/update) 全过
- 39+ admin 控制器测试全过
- SecurityFilterTest / AuthBaselineTest / AuthMeContextTest 全过
- 总数 681 → 701 (+20)

## Caveats

- `requireProjectWritePermission` 对 anonymous（uid==null）放行 —— 因为 SecurityFilter（`app.security.require-all-users-token=true`）只在 prod 启用，test 配置 false。等价做法是要求 test 也开 token；当前选择不破坏 39+ 既有测试。Prod 行为：SecurityFilter 先挡 401 → 控制器永远拿到 uid != null。
- ProjectAdminController 路径 `/api/projects/{id}/admins/{userId}` 故意挂在 /api/projects 下而非 /api/admin —— 后者会被 AdminPaths Tier A 拦截（401-before-403 顺序保持），但本接口需要 owner-视角的语义路径；显式调 AuthzService.isCurrentUserAdmin 替代 AdminPaths gating。
- B6+ 后续：完整项目级 RBAC（PermissionPoint × projectId）；本期只引入 isProjectAdmin 单标志位作为地基。
