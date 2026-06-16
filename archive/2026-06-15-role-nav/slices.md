# Slices — v0.0.20-role-nav

| # | 优先级 | TC 覆盖 | 实现目标 | 依赖 |
|---|---|---|---|---|
| S01 | P0 | TC-ROLE-ADM-001..004 | backend Role.adminAccess（实体可空列+读兜底）+ RoleCreate/Update/Detail +adminAccess + RoleService create/update set | 无 |
| S02 | P0 | TC-ME-ADM-001/002 | backend MeResponse.MeRole +adminAccess + MeService 组装兜底 | S01 |
| S03 | P0 | TC-ROLE-ADM-001..004, TC-ME-ADM-001/002 | backend 测试：RoleControllerAdminAccessTest(4) + AuthMeAdminAccessTest(2) | S01,S02 |
| S04 | P0 | (前端基座) | frontend api/auth.ts MeRole +adminAccess + api/role.ts +adminAccess + store/auth.ts isElevated 助手 | 无 |
| S05 | P0 | TC-FES-RN-001/002 | frontend AppLayout NavGroup +requiresAdmin + isElevated 过滤 | S04 |
| S06 | P0 | TC-FES-RN-003/004/005/006 | frontend ProtectedRoute me() 注水 + admin 前缀守卫 | S04 |
| S07 | P0 | TC-FES-RN-008 | frontend RolesPage adminAccess 复选框 | S04 |
| S08 | P0 | TC-FES-RN-009 | frontend WorkbenchPage 改读 store（不再调 me()） | S04 |
| S09 | P0 | TC-FES-RN-001..009 | frontend 测试：AppLayout(RN-001/002 + 修既有)、ProtectedRoute(RN-003..006)、isElevated(RN-007)、RolesPage(RN-008)、WorkbenchPage(RN-009 + 修既有) | S05-S08 |
| S10 | P0 | TC-E2E-RN-001/002 | E2E docker 重建 + PMO 未勾选=普通(me adminAccess false) + PUT 勾选 → me adminAccess true + 存量 19 表不变 | 全部 |

拓扑批次：
- 批次1（并行）：S01 / S04
- 批次2：S02（依 S01）/ S05,S06,S07,S08（依 S04，可并行）
- 批次3：S03（依 S01,S02）/ S09（依 S05-S08）
- 批次4：S10（依全部）

陷阱预警：
- 陷阱 A（Java8）：Boolean 默认值 `Boolean.FALSE`；getter 兜底 `adminAccess == null ? Boolean.FALSE : adminAccess`；无 Set.of。
- 陷阱 D1：`@Column(name="admin_access")` **不加 nullable=false**（ddl-auto=update 加 NOT NULL 列对存量行失败）。
- 陷阱守卫：admin 前缀集必须精确含 `/pm/products`/`/pm/product-modules`/`/pm/features`（product 组），不能裸 `/pm`（会误伤 pm 组全员路由 TC-FES-RN-005）。
- 陷阱注水时序：ProtectedRoute hydrated 门控——me() resolve/fail 前不守卫（管理员首帧不被误踢）。
- 陷阱既有测试：AppLayout.test 既有 6 组断言用例 seed 的 user 无 admin 角色→组会消失；必须给那些用例 seed admin user。WorkbenchPage.test 既有用 me mock；改读 store 后需 seed store.user。ProtectedRoute.test 既有 me mock（roles []）→非 admin，但它测的是 `/` 不受守卫。
- vi.mock 工厂不能引用顶层变量（内联 mock 对象）。
