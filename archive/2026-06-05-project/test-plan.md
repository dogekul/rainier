# v0.0.8 测试方案与详细案例

> 版本：v0.0.8-project
> 创建日期：2026-06-05
> 对应 Phase 2 Spec：
> - changes/2026-06-05-project/specs/entity-project/spec.md
> - changes/2026-06-05-project/specs/entity-requirement/spec.md（MODIFIED 块）
> - changes/2026-06-05-project/specs/entity-user-role/spec.md（MODIFIED 块）
> - changes/2026-06-05-project/specs/frontend-scaffold/spec.md（MODIFIED 块）

## 一、测试策略

### 1.1 测试金字塔

- **后端集成（~80%）**：`@SpringBootTest @AutoConfigureMockMvc` 覆盖 Project 5 endpoint + Requirement / UserRole 改造分支（沿用 v0.0.7 ROI 模式）
- **后端单元（~5%）**：仅在需要时补
- **前端组件（~15%）**：vitest 覆盖 Sider 「项目」+ /pm/projects 路由 + ProjectsPage 默认 owner + projectId 控件改造

### 1.2 测试原则

- **启动自愈而非容错读**：脏 project_id 由 `DanglingProjectIdCleanup` CommandLineRunner 在启动阶段 NULL 掉，TC-REQP-004 / TC-URLP-004 显式（reads 假定无 dangling）
- **owner 可改是 v0.0.8 的关键决策（Project 与 Requirement 同步反转 v0.0.6 immutable）**：TC-PRJ-009 验证 Project PUT 改 owner 富化跟随；TC-REQP-005 验证 Requirement PUT 改 owner
- **AuditorAwareImpl 自动注入 createBy**：TC-PRJ-006 显式验证（防止未来配置漂移）
- **默认 owner 解析依赖前端 listUsers + loginName 匹配**：TC-FES-P03 显式 mock 验证

### 1.3 已有测试资产（v0.0.7 baseline）

| 测试文件 | 用例数 | 类型 | 本变更影响 |
|---|---|---|---|
| 后端 v0.0.7 全部测试 | 125 | 集成/单元 | RequirementControllerCreateTest / QueryTest + UserRoleControllerCreateTest 各加 ≥1 case 验证激活 + 富化 |
| frontend v0.0.7 全部测试 | 25 | 组件 | AppLayout.test 加 TC-FES-P01；AppRoutes.test 加 P02；新增 ProjectsPage.test |
| **新增后端测试** | **≥ 15** | 集成 | 见第二节（实际 22：TC-PRJ ×13 + TC-REQP ×6 + TC-URLP ×3，TC-REQP-004/TC-URLP-004 共享 DanglingProjectIdCleanupTest） |
| **新增前端测试** | **≥ 5** | 组件 | 见第二节（TC-FES-P01..P05；AppLayout.test 扩 P01；AppRoutes.test 扩 P02；ProjectsPage.test 新建 P03/P04；UserRolesPage.test 新建 P05） |
| 总计 | ≥ 170 | — | 125 + ≥15 = ≥ 140 backend；25 + ≥5 = ≥ 30 frontend |

## 二、详细测试案例

### 功能 1：entity-project — Project CRUD（13 TCs）

| TC-ID | 优先级 | Scenario | 关键断言 | 位置 |
|---|---|---|---|---|
| TC-PRJ-001 | P0 | 最小 payload + 默认值 + 富化 | 201 + body.id isNumber + status=PLANNING + enabled=true + ownerName="Alice" + ownerLoginName="alice" | ProjectControllerCreateTest |
| TC-PRJ-002 | P0 | code 重复 → 409 | 409 + message startsWith("code already exists") | ProjectControllerCreateTest |
| TC-PRJ-003 | P0 | 缺 ownerUserId → 400 | 400 + fieldErrors[*].field="ownerUserId" | ProjectControllerCreateTest |
| TC-PRJ-004 | P0 | ownerUserId 不存在 → 400 | 400 + message containsString("owner user not found") | ProjectControllerCreateTest |
| TC-PRJ-005 | P0 | 非法 status → 400 | 400 + message containsString("invalid status") | ProjectControllerCreateTest |
| TC-PRJ-006 | P0 | createBy 自动注入 | 创建后 body.createBy = "alice"（v0 测试环境 JWT 默认 username=test 或类似；可用 anonymous 检查非 null） | ProjectControllerCreateTest |
| TC-PRJ-007 | P0 | GET 详情完整字段 + 富化 | body 字段集 = [id, code, name, description, status, ownerUserId, ownerName, ownerLoginName, startDate, endDate, enabled, createTime, updateTime, createBy, updateBy] | ProjectControllerQueryTest |
| TC-PRJ-008 | P0 | 按 status 过滤列表 | total=2 + content 全部 status="ACTIVE" | ProjectControllerQueryTest |
| TC-PRJ-009 | P0 | PUT 转移 owner 成功 | 200 + body.ownerUserId 改变 + body.ownerLoginName 富化跟随 | ProjectControllerQueryTest |
| TC-PRJ-010 | P0 | PUT 新 ownerUserId 不存在 → 400 | 400 + message 含 "owner user not found" | ProjectControllerQueryTest |
| TC-PRJ-011 | P0 | DELETE 无引用 → 204 | 204 + 后续 GET 404 | ProjectControllerDeleteTest |
| TC-PRJ-012 | P0 | DELETE 被 Requirement 引用 → 409 | 409 + message 含 "project has linked requirements" | ProjectControllerDeleteTest |
| TC-PRJ-013 | P0 | DELETE 被 UserRole 引用 → 409 | 409 + message 含 "project has assigned user-roles" | ProjectControllerDeleteTest |

### 功能 2：entity-requirement MODIFIED — projectId 激活 + owner 可改（6 TCs）

| TC-ID | 优先级 | Scenario | 关键断言 | 位置 |
|---|---|---|---|---|
| TC-REQP-001 | P0 | POST 含 projectId 存在 → 富化 | 201 + body.projectName + body.projectCode | RequirementControllerCreateTest（扩展） |
| TC-REQP-002 | P0 | POST 含 projectId 不存在 → 400 | 400 + message 含 "project not found" | RequirementControllerCreateTest（扩展） |
| TC-REQP-003 | P0 | POST 含 projectId=null → 兼容 | 201 + body.projectName=null + body.projectCode=null | RequirementControllerCreateTest（扩展） |
| TC-REQP-004 | P0 | 启动自愈：requirement 表 dangling project_id 被 NULL 化 | 启动后 GET dangling 行 → projectId=null + projectName=null + 日志含 WARN | DanglingProjectIdCleanupTest（新建） |
| TC-REQP-005 | P0 | PUT 改 ownerUserId 转移负责人（v0.0.6 不可改语义反转） | 200 + body.ownerUserId 更新 | RequirementControllerQueryTest（扩展） |
| TC-REQP-006 | P0 | PUT 新 ownerUserId 不存在 → 400 | 400 + message 含 "owner user not found" | RequirementControllerQueryTest（扩展） |

### 功能 3：entity-user-role MODIFIED — projectId 激活校验 + 富化（4 TCs）

| TC-ID | 优先级 | Scenario | 关键断言 | 位置 |
|---|---|---|---|---|
| TC-URLP-001 | P0 | POST 含 projectId 存在 → 富化 | 201 + body.projectName + body.projectCode | UserRoleControllerCreateTest（扩展） |
| TC-URLP-002 | P0 | POST 含 projectId 不存在 → 400 | 400 + message 含 "project not found" | UserRoleControllerCreateTest（扩展） |
| TC-URLP-003 | P0 | POST 含 projectId=null → 公司级 hat 保留 | 201 + body.projectId=null | UserRoleControllerCreateTest（扩展） |
| TC-URLP-004 | P0 | 启动自愈：v0.0.7 测试遗留 id=2 projectId=42 被 NULL 化 | 启动后 GET /api/user-roles/2 → projectId=null + projectName=null + userName/roleName 仍富化 + 日志含 WARN | DanglingProjectIdCleanupTest（新建，同 TC-REQP-004） |

### 功能 4：frontend-scaffold MODIFIED — Sider 项目 + 路由 + 默认 owner + 控件改造（5 TCs）

| TC-ID | 优先级 | Scenario | 关键断言 | 位置 |
|---|---|---|---|---|
| TC-FES-P01 | P0 | Sider「需求管理」组含「项目」位于「诉求」之前 | screen.getByText("项目") + DOM 顺序在「诉求」之前 + 点击跳 /pm/projects | AppLayout.test.tsx（扩展） |
| TC-FES-P02 | P0 | /pm/projects 路由直接访问 + grep 校验 | MemoryRouter at /pm/projects → ProjectsPage 渲染 + Bash grep -c "/pm/projects" AppRoutes.tsx >= 1 | AppRoutes.test.tsx（扩展） |
| TC-FES-P03 | P0 | ProjectsPage 新建抽屉默认 owner = 当前登录 user | auth store user.username="alice" + mock listUsers 含 {id:1, loginName:"alice"} → 打开抽屉 → 「负责人」select value === "1" | ProjectsPage.test.tsx（新建） |
| TC-FES-P04 | P0 | ProjectsPage 编辑抽屉 owner 可改 | 编辑模式 ownerUserId=1，listUsers 含 lili(id=2) → 切换到 2 → 保存 → mock updateProject 收到 body.ownerUserId=2 + 下拉控件 disabled 属性为 false | ProjectsPage.test.tsx |
| TC-FES-P05 | P0 | UserRolesPage 项目留白 → projectId 传 null | mock listProjects 返 1 条 + 用户/角色已选 + 项目下拉留白 → 保存 → mock createUserRole 收到 body.projectId === null | UserRolesPage.test.tsx（新建或扩展） |

## 三、测试执行矩阵

| 功能模块 | 单元 | 集成 | 组件 | E2E | 状态 |
|---|---|---|---|---|---|
| entity-project | — | 13 TCs | — | E2E POST + DESCRIBE | 🟢 充分 |
| entity-requirement MODIFIED | — | 6 TCs（projectId 校验 3 + 启动自愈 1 + owner 可改 2） | — | E2E POST w/ projectId + PUT 改 owner | 🟢 充分 |
| entity-user-role MODIFIED | — | 4 TCs（projectId 校验 3 + 启动自愈 1 含脏 id=2） | — | E2E POST + 富化 + 脏 id=2 验证 | 🟢 充分 |
| frontend-scaffold MODIFIED | — | — | 5 TCs | 浏览器手测 | 🟢 充分 |

## 四、回归风险矩阵

| 风险区域 | v0.0.8 改动 | 已有回归保护 | 风险等级 |
|---|---|---|---|
| v0.0.7 baseline（position/role/user_role/user） | 0 entity 改动；UserRoleService.enrich + create 改造 | TC-USR-001..004 + TC-UROL-001..009 全部既有 | 🟡 中：service 改造但保持向后兼容 |
| v0.0.6 baseline（demand/requirement/link） | 0 entity 改动；RequirementService.create/update + Detail 字段加 | TC-DMD-001..011 + TC-REQ-001..009 + TC-DRL-001..007 全部既有 | 🟡 中：DTO 加字段 + service 加校验 |
| Sider 菜单 | 「需求管理」组追加 1 项 | AppLayout.test 扩展 TC-FES-P01 + v0.0.6 TC-FES-D01 + v0.0.7 TC-FES-H01 | 🟢 低 |
| AppRoutes | 加 1 路由 `/pm/projects` | TC-FES-P02 grep + 路由 mount | 🟡 中（v0.0.3 历史曾有 linter 回退） |
| 新 Project entity + 5 endpoint | 新建 ~10 文件 | 13 集成 TC | 🟢 低 |
| Requirement/UserRole Detail 加字段 + service enrich 改造 | 改 entity Detail + service join + 富化容错 | TC-REQP/URLP 各 4 TCs | 🟡 中（影响 baseline 现有 reads） |
| 现有脏数据 `user_role.id=2 projectId=42` | reads 容错 → projectName=null | TC-URLP-004 显式验证 | 🟢 低（容错路径已覆盖） |
| 数据策略（不 down -v） | Hibernate ddl-auto=update 只 CREATE 新表 | E2E 验证：所有 v0.0.7 数据保留 + 新 rainier_project 表 | 🟡 中：实际执行时 verify 阶段确认 |
| owner 可改 — 与 v0.0.6 Requirement.owner 不可改不一致 | Project ≠ Requirement，这是有意的设计 | TC-PRJ-008/009 显式覆盖 | 🟢 低（已在 design.md 决策 2 说明） |
| 前端 default owner 解析依赖 listUsers size 100 | 池 > 100 默认会错 | KL 记录，v0 用户数远低于 100 | 🟢 低 |

## 五、建议补充顺序

### 第一优先（P0 — 部署前必补）

**Backend**：
1. TC-PRJ-001..013（13）
2. TC-REQP-001..003 + TC-REQP-005..006（5）— 扩展 v0.0.6 RequirementControllerCreateTest + QueryTest（含 owner 可改对内修订）
3. TC-URLP-001..003（3）— 扩展 v0.0.7 UserRoleControllerCreateTest
4. TC-REQP-004 + TC-URLP-004 启动自愈（2，共享 DanglingProjectIdCleanupTest 文件）— 新建 1 文件
合计 23 TC（spec 要求 15+，超额覆盖）

**Frontend**：
4. TC-FES-P01..P05（5）— 扩展 AppLayout.test + AppRoutes.test + 新建 ProjectsPage.test + UserRolesPage.test

**E2E（test-report 阶段执行）**：
5. docker exec mysql SHOW TABLES = 10 张
6. DESCRIBE rainier_project
7. 现有 v0.0.7 9 张表数据完整保留
8. curl POST /api/projects + POST Requirement w/ projectId + POST UserRole w/ projectId 流程
9. 现有脏 user_role.id=2 GET 返 projectName=null（验证容错）
10. grep `is_pmo|isPmo` / `BaseAutoIdEntity` 仍 0 行

### 第二优先（P1 — 部署后尽快）

无（本变更范围聚焦 v0.0.8 必需）

### 第三优先（P2）

无

## 六、TC 编号对照表

| TC-ID | Spec Scenario | 文件 |
|---|---|---|
| TC-PRJ-001..013 | entity-project 13 scenarios | ProjectController{Create,Query,Delete}Test |
| TC-REQP-001..003 + TC-REQP-005..006 | entity-requirement MODIFIED 5 scenarios | RequirementControllerCreateTest / QueryTest（扩展） |
| TC-REQP-004 + TC-URLP-004 | 启动自愈（共 2 scenarios） | DanglingProjectIdCleanupTest（新建） |
| TC-URLP-001..003 | entity-user-role MODIFIED 3 scenarios | UserRoleControllerCreateTest（扩展） |
| TC-FES-P01..P05 | frontend-scaffold MODIFIED 5 scenarios | AppLayout.test + AppRoutes.test + ProjectsPage.test（新）+ UserRolesPage.test（扩展或新） |

**总计**：28 P0 TCs；覆盖 10 Requirements / 28 Scenarios（1:1 映射）。
