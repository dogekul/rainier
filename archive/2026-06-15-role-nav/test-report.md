# Test Report — v0.0.20-role-nav

> Baseline: tag `v0.0.19-requirement-enrich` / commit d706864 (backend 348 / frontend 77 / 19 表).

## 1. 总体概况

| 维度 | 总数 | 通过 | 失败 | 跳过 | 通过率 |
|---|---|---|---|---|---|
| Backend (mvn test) | 354 | 354 | 0 | 0 | 100% |
| Frontend (vitest) | 106 | 106 | 0 | 0 | 100% |
| tsc -b | — | ✅ clean | — | — | — |
| eslint --max-warnings 0 | — | ✅ clean | — | — | — |
| checkstyle (mvn build) | — | ✅ BUILD SUCCESS | — | — | — |
| E2E (docker compose) | — | ✅ green | — | — | — |

新增测试：backend +6（RoleControllerAdminAccessTest 4 + AuthMeAdminAccessTest 2），frontend +29
（AppLayout RN-001/002 = 2、ProtectedRoute.guards RN-003/003b/004/005/006 = 5、isElevated RN-007 = 3、
RolesPage RN-008/008b = 2、navGuardConsistency = 17）。

### 1.1 覆盖率诊断（变更文件）
变更集中在 Role 实体/DTO/Service、MeService/MeResponse、前端 store/AppLayout/ProtectedRoute/RolesPage/WorkbenchPage。
每个新增/改动单元均有对应 TC（见 §5），无零覆盖 capability。

## 2. 按模块统计
- `RoleControllerAdminAccessTest` 4/4（create 默认 false + 持久列、create true + 持久列、update 切换、legacy NULL 读 false + 不被写动）。
- `AuthMeAdminAccessTest` 2/2（me adminAccess true / false-非null）。
- `auth.test.ts` isElevated 3/3 + 既有 setAuth/logout 2/2。
- `AppLayout.test.tsx` 既有 10 + RN-001/002 = 12/12（beforeEach 改 seed admin 保既有断言）。
- `ProtectedRoute.guards.test.tsx` 5/5（RN-003 跳转 / RN-003b /pm/products 跳转 / RN-004 admin 留 / RN-005 /pm 留 / RN-006 注水一次）。
- `ProtectedRoute.test.tsx` 既有 2/2（未受影响）。
- `navGuardConsistency.test.tsx` 17/17（nav↔guard 机械一致性）。
- `RolesPage.test.tsx` 2/2（RN-008 create / RN-008b update）。
- `WorkbenchPage.test.tsx` 3/3（改读 store，RN-009 等）。

## 3. E2E 测试结果（docker compose，真实 MySQL，卷保留）
- SHOW TABLES = **19**（无新表；`admin_access` 为 `rainier_role` 上的 `bit(1)` NULLABLE 列）。✅
- 存量角色 PMO(id=1)/YFM(id=2) 保留，`admin_access=NULL`（legacy 行）。✅
- **TC-E2E-RN-001**：`GET /api/roles` → PMO/YFM `adminAccess=false`（NULL 读兜底）；alice `me()` roles `adminAccess=false` → 普通视图。✅
- **TC-E2E-RN-002**：`PUT /api/roles/1 {adminAccess:true}` → `me()` `adminAccess=true` → 全控制台。✅
- 测后将 PMO.admin_access **还原为 NULL**（精确原始态）；最终行数 users=2/roles=2/projects=6/user_roles=2 与测前一致 → 存量业务数据未删改（standing 约束）。✅

## 4. 失败项详细分析
无失败项。

## 5. 功能/测试覆盖对照
| Spec Scenario | 实现 | 测试 |
|---|---|---|
| Role create 默认 false / true 持久 / update 切换 / legacy NULL 读 false | Role.adminAccess + RoleService + RoleDetail | TC-ROLE-ADM-001..004 + E2E |
| me roles[].adminAccess true/false 非 null | MeService + MeResponse.MeRole | TC-ME-ADM-001/002 + E2E |
| 普通用户只见 2 组 / 管理员见 6 组 | AppLayout requiresAdmin + isElevated 过滤 | TC-FES-RN-001/002 + E2E |
| 非管理员 admin 路由跳回 / / admin 留 / pm 留 | ProtectedRoute isAdminPath 守卫 | TC-FES-RN-003/003b/004/005 |
| ProtectedRoute me() 注水一次 | ProtectedRoute useEffect | TC-FES-RN-006 |
| isElevated 助手 | store/auth.ts | TC-FES-RN-007 |
| RolesPage adminAccess 复选框 create/update | RolesPage checkbox | TC-FES-RN-008/008b |
| WorkbenchPage 读 store | WorkbenchPage useAuthStore | TC-FES-RN-009 |
| nav↔guard 一致性（防漂移） | 导出 navGroups + isAdminPath | navGuardConsistency 17 |

## 6. 设计调整说明
见 `design-adjustments.md`（AD-1..AD-5，均来自 Step 0 评审，纯增强/纠正，不改接口语义）。

## 7. 修复确认记录（Step 0 三代理评审）
- 评审结果：代码 C:0 H:0 / 测试 C:0 H:0 / 文档 C:0 H:0；M 共 2、L 多。阈值 C=0、H≤3、M≤10 全满足。
- 自动修复：PA-1（nav↔guard 单一真相 + 一致性测试 + /pm/products 守卫测试）、PA-2（移重言断言）、
  PA-3（ADM-001/002 持久列断言）、PA-4（RolesPage update 测试）、PA-5（文档 me() fallback 纠正）。
- 未修（记录可接受）：code-L1 字段初始值 cosmetic、code-L2 me 失败 fail-safe、test-L3 store 跨文件隔离 benign。

## 8. 十一类失败模式检查 (a–k)
- (a) 幻觉：无。所有引用文件/字段/API 真实（admin_access 列、getAdminAccess、isAdminPath、navGroups 均存在）。
- (b) 范围蔓延：diff 限于 proposal Impact 列出的文件 + 对应测试；RolesPage 加「管理员」只读列为 scope-positive 小增强。
- (c) 级联错误：ProtectedRoute me() catch 仅吞 401（拦截器已处理）+ 非 401 标记 hydrated 避免挂起，fail-closed；WorkbenchPage list catch 仅边界。
- (d) 上下文丢失：实现匹配 D1-D6（见 docs 评审逐条 PASS）。
- (e) 工具误用：用 Edit/Write 改文件、专用测试工具；无 shell 改源码。
- (f) 运行时行为偏差：守卫 hydrated 门控经 guard 测试验证（首帧不误踢，注水后生效）；E2E 验证 me() 真实返回 adminAccess。
- (g) 管线断链：Role.adminAccess → me() → isElevated → nav/guard 全链 E2E + 单测打通。
- (h) 内容质量偏差：无跨页数据不一致；中文标签一致。
- (i) 指令衰减：standing 约束「不删改存量数据」严格执行（E2E 测后还原 NULL + 行数核对）。
- (j) 覆盖真空：无零覆盖 capability；三 MOD capability 均有自动化 TC（含前端守卫/导航）。
- (k) 契约断层：后端 `adminAccess`(camelCase JSON) ↔ 前端 `MeRole.adminAccess`/`Role.adminAccess` 字段名一致；E2E 交叉验证。

## 9. 结论
全量测试 100% 通过；E2E 全链绿；lint/type/checkstyle clean；存量业务数据零删改（测后精确还原）。
0 新表、0 新端点、0 新依赖（仅 `rainier_role.admin_access` 新增可空列）。质量信号全绿，建议进入 Gate 3。
