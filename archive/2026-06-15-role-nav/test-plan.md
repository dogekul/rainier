# Test Plan — v0.0.20-role-nav

> Baseline: backend 348 / frontend 77 green, 19 表. Scope: 3 MOD capabilities, 0 new tables.

## 1. 测试策略

- 金字塔：后端 MockMvc 集成（role + me）/ 前端 vitest 组件 + 路由树 / E2E docker 全链。
- 原则：不删改存量数据；adminAccess 为可空列读兜底；守卫只在 me() 注水后生效。
- 已有资产：`AuthMeContextTest`（me seed 模式）、`RoleControllerCreateTest`（role POST 模式）、
  `AppLayout.test`（Sider 组断言）、`ProtectedRoute.test`（路由树）、`WorkbenchPage.test`（me mock）。

## 2. 详细测试案例

### entity-role (后端)

| TC-ID | 场景 | Arrange | Act | Assert |
|---|---|---|---|---|
| TC-ROLE-ADM-001 | create 默认 false | body code/name 无 adminAccess | POST /api/roles | 201, `$.adminAccess=false` |
| TC-ROLE-ADM-002 | create true 持久 | body adminAccess:true | POST /api/roles | 201, `$.adminAccess=true` |
| TC-ROLE-ADM-003 | update 切换 | seed role false | PUT adminAccess:true | `$.adminAccess=true` |
| TC-ROLE-ADM-004 | 存量 NULL 读 false | native INSERT 一行 admin_access NULL（或 save 后 native UPDATE set NULL） | GET /api/roles/{id} | `$.adminAccess=false`，列值未被写动 |

### auth-placeholder (后端)

| TC-ID | 场景 | Arrange | Act | Assert |
|---|---|---|---|---|
| TC-ME-ADM-001 | me 带 adminAccess=true | seed role adminAccess=true + link user | GET /api/auth/me | `$.roles[0].adminAccess=true` |
| TC-ME-ADM-002 | me adminAccess=false 非 null | seed role adminAccess=false + link | GET /api/auth/me | `$.roles[0].adminAccess=false`（非 null） |

### frontend-scaffold (前端)

| TC-ID | 场景 | Arrange | Act | Assert |
|---|---|---|---|---|
| TC-FES-RN-001 | 普通用户只见 2 组 | store user roles 全 adminAccess:false | render AppLayout | 见 工作台/需求管理；无 组织/产品/人事配置/系统 |
| TC-FES-RN-002 | 管理员见全 6 组 | store user 一角色 adminAccess:true | render AppLayout | 6 组全在（系统/审计日志可见） |
| TC-FES-RN-003 | 非管理员敲 admin 路由跳回 / | token+store 非 admin，me mock 非 admin | MemoryRouter /hr/roles → AppRoutes | 落在 workbench（greeting 在；RolesPage 不在） |
| TC-FES-RN-004 | 管理员敲 /hr/roles 渲染 | store admin，me mock admin | MemoryRouter /hr/roles | RolesPage 渲染（新建角色按钮在） |
| TC-FES-RN-005 | pm 路由全员开放 | 非 admin | MemoryRouter /pm/projects | ProjectsPage 渲染（不被守卫踢） |
| TC-FES-RN-006 | ProtectedRoute 注水 me() 一次 | token，store user 无 roles，me mock | render AppRoutes / | me 被调用 1 次，store.user.roles 被写 |
| TC-FES-RN-007 | isElevated 助手 | user roles [{false},{true}] / [{false}] | isElevated(user) | true / false；user null→false |
| TC-FES-RN-008 | RolesPage adminAccess 复选框 | listRoles mock 空 | 勾选「管理员权限」+ 保存（新建） | createRole body.adminAccess=true |
| TC-FES-RN-009 | WorkbenchPage 读 store | store user 含 id/roles，listTasks/Stories mock | render WorkbenchPage | greeting/roles 来自 store；listTasks assigneeUserId=store.id |

### E2E (docker)

| TC-ID | 场景 | 步骤 | 预期 |
|---|---|---|---|
| TC-E2E-RN-001 | PMO 未勾选=普通视图 | 重建栈；GET /api/roles 看 PMO adminAccess=false；GET /api/auth/me（alice）roles[].adminAccess=false | me 返回 adminAccess=false（前端将只显示 2 组） |
| TC-E2E-RN-002 | 勾选 PMO → 全控制台 | PUT /api/roles/{pmoId} adminAccess:true；再 GET /api/auth/me | roles[].adminAccess=true（前端将显示全 6 组）；存量 19 表不变；其它存量数据未改 |

## 3. 测试执行矩阵

| 功能 | 后端集成 | 前端组件 | 前端路由树 | E2E |
|---|---|---|---|---|
| Role.adminAccess CRUD | TC-ROLE-ADM-001..004 | — | — | TC-E2E-RN-001/002 |
| me().adminAccess | TC-ME-ADM-001/002 | — | — | TC-E2E-RN-001/002 |
| 导航裁剪 | — | TC-FES-RN-001/002 | — | (经 me) |
| 路由守卫 | — | — | TC-FES-RN-003/004/005 | — |
| me() 注水 | — | — | TC-FES-RN-006 | — |
| isElevated | — | TC-FES-RN-007 | — | — |
| RolesPage 复选框 | — | TC-FES-RN-008 | — | — |
| WorkbenchPage store | — | TC-FES-RN-009 | — | — |

## 4. 回归风险矩阵

| 区域 | 风险 | 缓解 |
|---|---|---|
| `rainier_role` 加列 | 🔴 ddl-auto NOT NULL 失败 | 可空列 + 读兜底（TC-ROLE-ADM-004） |
| 既有 AppLayout.test（全 6 组无条件渲染）| 🟡 现有用例 seed 的 user 无 admin 角色 → 组会消失 | 既有用例改为 seed admin user（保持 6 组断言）或显式 elevated |
| ProtectedRoute.test（me mock） | 🟡 注水逻辑改变渲染时序 | me mock 返回 roles；await 注水 |
| WorkbenchPage.test（自调 me） | 🟡 改读 store | seed store.user + 保留 me fallback |
| product 组 `/pm/product*` 与 pm 组 `/pm/*` 前缀重叠 | 🟡 守卫误伤 pm | 精确 admin 前缀集（TC-FES-RN-005 验证 pm 不被踢） |
| 存量 PMO 变普通视图 | 🟡 预期 | E2E 明示（TC-E2E-RN-001/002） |

## 5. 建议补充顺序

P0：TC-ROLE-ADM-001..004 → TC-ME-ADM-001/002 → TC-FES-RN-001..009 → TC-E2E-RN-001/002（全 P0，本版无 P1/P2）。
