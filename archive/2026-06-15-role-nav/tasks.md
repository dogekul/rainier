# Tasks — v0.0.20-role-nav

## entity-role (backend)
- [x] S01 `Role.adminAccess` 可空列 + getter 读兜底 null→false + setter
- [x] S01 `RoleCreateRequest`/`RoleUpdateRequest` +adminAccess
- [x] S01 `RoleDetail` +adminAccess（from() 透传）
- [x] S01 `RoleService.create`（默认 false）/`update`（可改）set adminAccess
- [x] S03 `RoleControllerAdminAccessTest`：TC-ROLE-ADM-001..004

## auth-placeholder (backend)
- [x] S02 `MeResponse.MeRole` +adminAccess 字段/getter/ctor
- [x] S02 `MeService` 组装 MeRole 读 role.adminAccess 兜底 false
- [x] S03 `AuthMeAdminAccessTest`：TC-ME-ADM-001/002

## frontend-scaffold (frontend)
- [x] S04 `api/auth.ts` MeRole +adminAccess
- [x] S04 `api/role.ts` Role/RoleCreate/RoleUpdate +adminAccess
- [x] S04 `store/auth.ts` `isElevated(user)` 助手导出
- [x] S05 `AppLayout` NavGroup +requiresAdmin + 过滤渲染（org/product/hr/sys=admin）
- [x] S06 `ProtectedRoute` me() 注水 store + admin 前缀守卫（hydrated 门控）
- [x] S07 `RolesPage` adminAccess 复选框 + create/update body
- [x] S08 `WorkbenchPage` 改读 store（不再调 me()）
- [x] S09 前端测试：AppLayout RN-001/002（+修既有 6 组用例 seed admin）、ProtectedRoute RN-003..006、isElevated RN-007、RolesPage RN-008、WorkbenchPage RN-009（+修既有）

## E2E
- [x] S10 docker 重建 + TC-E2E-RN-001/002 + 存量 19 表/数据不变
