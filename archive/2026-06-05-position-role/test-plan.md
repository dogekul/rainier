# v0.0.7 测试方案与详细案例

> 版本：v0.0.7-position-role
> 创建日期：2026-06-05
> 对应 Phase 2 Spec：
> - changes/2026-06-05-position-role/specs/entity-position/spec.md
> - changes/2026-06-05-position-role/specs/entity-role/spec.md
> - changes/2026-06-05-position-role/specs/entity-user-role/spec.md
> - changes/2026-06-05-position-role/specs/entity-user/spec.md（MODIFIED 块）
> - changes/2026-06-05-position-role/specs/frontend-scaffold/spec.md（MODIFIED 块）

## 一、测试策略

### 1.1 测试金字塔

- **后端集成（占 ~85%）**：`@SpringBootTest @AutoConfigureMockMvc` 覆盖所有 endpoint + DB + service 校验链路（沿用 v0.0.6 ROI 模式）
- **后端单元（占 ~3%）**：仅在涉及 NULL 兜底 / 富化 join 时补
- **前端组件（占 ~12%）**：vitest + @testing-library/react 覆盖 Sider 菜单 + 3 页表头 + UsersPage 编辑抽屉岗位下拉

### 1.2 测试原则

- **NULL 唯一性双覆盖**：service 层 IS NULL 查询 + DB UNIQUE 兜底，TC-UROL-003/004 显式验证两条路径
- **占位字段语义显式**：TC-UROL-007 显式验证 projectId 任意 BIGINT 都接受（占位 + 无校验）
- **富化字段断言**：TC-USR-001/003 + TC-UROL-008 验证 service join 后的 enrichment

### 1.3 已有测试资产（v0.0.6 baseline）

| 测试文件 | 用例数 | 类型 | 本变更影响 |
|---|---|---|---|
| 后端 v0.0.6 全部测试 | 94 | 集成/单元 | 0 修改（UserControllerTest 加 USR-001..004 即可） |
| frontend v0.0.6 全部测试 | 19 | 组件 | 0 修改（AppLayout test 增 1 TC-FES-H01） |
| **新增后端测试** | **≥ 25** | 集成 | 见第二节 |
| **新增前端测试** | **≥ 5** | 组件 | 见第二节 |
| 总计 | ≥ 143 | — | 94 + 25 = ≥ 119 backend；19 + 5 = ≥ 24 frontend |

## 二、详细测试案例

### 功能 1：entity-position — 岗位 CRUD（10 TCs）

| TC-ID | 优先级 | Scenario | 关键断言 | 位置 |
|---|---|---|---|---|
| TC-POS-001 | P0 | 最小 payload + 默认值 | 201 + body.id isNumber + code/name/category 字段 + enabled=true + Location matchesPattern("/api/positions/\\d+") | PositionControllerCreateTest |
| TC-POS-002 | P0 | code 重复 → 409 | 409 + message startsWith("code already exists") | PositionControllerCreateTest |
| TC-POS-003 | P0 | 非法 category → 400 | 400 + message containsString("invalid category") | PositionControllerCreateTest |
| TC-POS-004 | P0 | 缺 name → 400 | 400 + fieldErrors[*].field 含 "name" | PositionControllerCreateTest |
| TC-POS-005 | P0 | GET 详情完整字段集 | 200 + body 字段集 = [id, code, name, description, category, enabled, createTime, updateTime, createBy, updateBy] | PositionControllerQueryTest |
| TC-POS-006 | P0 | 按 category 过滤列表 | total=2 + content 全部 category="TECH" | PositionControllerQueryTest |
| TC-POS-007 | P0 | PUT 更新 name + enabled | 200 + body.name 更新 + body.enabled=false | PositionControllerQueryTest |
| TC-POS-008 | P0 | PUT body 含 code 静默忽略 | 200 + body.code 未变 | PositionControllerQueryTest |
| TC-POS-009 | P0 | DELETE 无 User 引用 → 204 | 204 + 后续 GET 404 | PositionControllerDeleteTest |
| TC-POS-010 | P0 | DELETE 有 User 引用 → 409 | 409 + message 含 "position has assigned users" | PositionControllerDeleteTest |

### 功能 2：entity-role — 角色 CRUD（8 TCs）

| TC-ID | 优先级 | Scenario | 关键断言 |
|---|---|---|---|
| TC-ROL-001 | P0 | 最小 payload + 默认值 | 201 + body.id isNumber + code/name + enabled=true + Location matchesPattern("/api/roles/\\d+") |
| TC-ROL-002 | P0 | code 重复 → 409 | 409 + message startsWith("code already exists") |
| TC-ROL-003 | P0 | 缺 name → 400 | 400 + fieldErrors[*].field 含 "name" |
| TC-ROL-004 | P0 | GET 详情完整字段集 | body 字段集 = [id, code, name, description, enabled, createTime, updateTime, createBy, updateBy] |
| TC-ROL-005 | P0 | 软删后 GET 404 | DELETE → 204 → 后续 GET → 404 |
| TC-ROL-006 | P0 | PUT 更新 name + description | body.name + body.description 更新 |
| TC-ROL-007 | P0 | DELETE 无 user_role 引用 → 204 | 204 + 后续 GET 404 |
| TC-ROL-008 | P0 | DELETE 有 user_role 引用 → 409 | 409 + message 含 "role has assignments" |

**位置**：RoleControllerCreateTest / QueryTest / DeleteTest（3 文件，与 Position 一致）

### 功能 3：entity-user-role — UserRole M2M + 富化（9 TCs）

| TC-ID | 优先级 | Scenario | 关键断言 | 位置 |
|---|---|---|---|---|
| TC-UROL-001 | P0 | 含 projectId 合法创建 | 201 + body.userId/roleId/projectId 字段 | UserRoleControllerCreateTest |
| TC-UROL-002 | P0 | projectId=null 公司级 hat 创建 | 201 + body.projectId=null | UserRoleControllerCreateTest |
| TC-UROL-003 | P0 | (userId, roleId, projectId=42) 重复 → 409 (DB UNIQUE 路径) | 409 + message 含 "user-role already exists" | UserRoleControllerCreateTest |
| TC-UROL-004 | P0 | (userId, roleId, projectId=null) 重复 → 409 (service NULL 兜底) | 409 + message 含 "user-role already exists" | UserRoleControllerCreateTest |
| TC-UROL-005 | P0 | NULL 与 non-NULL 共存 OK | 第二个 POST 201；后续 GET total=2 | UserRoleControllerCreateTest |
| TC-UROL-006 | P0 | userId 不存在 → 400 | 400 + message 含 "user not found" | UserRoleControllerCreateTest |
| TC-UROL-007 | P0 | projectId 任意 BIGINT 接受（占位） | POST projectId=987654321 → 201 + body.projectId=987654321 | UserRoleControllerCreateTest |
| TC-UROL-008 | P0 | GET 列表富化 userName/roleName | content[0].userName/userLoginName/roleName/roleCode 全部存在且匹配 | UserRoleControllerQueryTest |
| TC-UROL-009 | P0 | DELETE 硬删 | 204 + 后续 GET 404 + repo.count(id=1)=0 | UserRoleControllerDeleteTest |

### 功能 4：entity-user MODIFIED — User + Position（4 TCs）

| TC-ID | 优先级 | Scenario | 关键断言 |
|---|---|---|---|
| TC-USR-001 | P0 | POST 含 positionId 创建 + 富化 | 201 + body.positionId/positionName/positionCategory 全部存在并匹配 |
| TC-USR-002 | P0 | POST positionId 不存在 → 400 | 400 + message 含 "position not found" |
| TC-USR-003 | P0 | PUT 更新 positionId 后富化跟随 | 200 + body.positionId=2 + body.positionName 更新 |
| TC-USR-004 | P0 | PUT positionId=null 清空 | 200 + body.positionId=null + body.positionName=null |

**位置**：扩展现有 `UserControllerTest`（沿用 v0.0.3 测试文件，加 4 个 @Test 方法）

### 功能 5：frontend-scaffold MODIFIED — Sider + Routes + UsersEditDrawer（3 TCs）

| TC-ID | 优先级 | Scenario | 关键断言 | 位置 |
|---|---|---|---|---|
| TC-FES-H01 | P0 | Sider 含「人事配置」3 项 | screen.getByText("人事配置") + 3 子项 + 点击岗位跳 /hr/positions | AppLayout.test.tsx（扩展） |
| TC-FES-H02 | P0 | /hr/* 路由直接访问 + grep 校验 | Memory Router at /hr/positions → PositionsPage 渲染 + Bash grep -c "/hr/positions" frontend/src/AppRoutes.tsx >= 1 | AppRoutes.test.tsx（扩展） |
| TC-FES-H03 | P0 | UsersPage 编辑抽屉岗位下拉 | mock listPositions 返回 2 条 → 选 id=1 → 保存 → mock createUser 收到 body.positionId=1 | UsersPage.test.tsx（新建） |

## 三、测试执行矩阵

| 功能模块 | 单元 | 集成 | 组件 | E2E | 状态 |
|---|---|---|---|---|---|
| entity-position | — | 10 TCs | — | E2E POST + DESCRIBE | 🟢 充分 |
| entity-role | — | 8 TCs | — | E2E POST + DESCRIBE | 🟢 充分 |
| entity-user-role | — | 9 TCs（含 NULL 双路径） | — | E2E POST 流程 | 🟢 充分 |
| entity-user MODIFIED | — | 4 TCs | — | E2E POST user w/ positionId | 🟢 充分 |
| frontend-scaffold MODIFIED | — | — | 3 TCs | 浏览器手测 3 页 | 🟢 充分 |

## 四、回归风险矩阵

| 风险区域 | v0.0.7 改动 | 已有回归保护 | 风险等级 |
|---|---|---|---|
| v0.0.6 baseline（demand/req/link） | 0 改动 | 30 backend + 8 frontend 测试 | 🟢 低 |
| v0.0.3 User CRUD | 加 positionId 字段 + 富化 | 10 既有 + 4 新 TC-USR | 🟡 中（涉及 entity / DTO / service 改造，但向后兼容） |
| AppLayout Sider | 增菜单组 | AppLayout.test 增 TC-FES-H01 | 🟡 中 |
| AppRoutes | 新 4 路由 (`/hr` 重定向 + 3 页) | TC-FES-H02 grep + 路由 mount | 🟡 中（v0.0.3 历史曾有 linter 回退） |
| 新 3 entity + service + controller | 新建 30+ 文件 | 27 集成 TC（POS 10 + ROL 8 + UROL 9） | 🟢 低（独立 service，无 v0.0.6 影响面） |
| UserRole NULL 唯一性 | service 层 IS NULL 兜底 | TC-UROL-003/004/005 显式三路径 | 🟡 中 |
| project_id 无校验占位 | 沿用 v0.0.6 Requirement.project_id 模式 | TC-UROL-007 显式验证 | 🟢 低（既定模式） |
| frontend `client.ts` axios | 0 改动 | v0 baseline auth.test | 🟢 低 |

## 五、建议补充顺序

### 第一优先（P0 — 部署前必补）

**Backend**：
1. TC-POS-001..010（10）
2. TC-ROL-001..008（8）
3. TC-UROL-001..009（9）
4. TC-USR-001..004（4）
合计 31 TC（spec 要求 25+，超额覆盖）

**Frontend**：
5. TC-FES-H01..H03（3）

**E2E（test-report 阶段执行）**：
6. docker compose down -v + up 后 SHOW TABLES = 9 张
7. DESCRIBE rainier_position / rainier_role / rainier_user_role
8. DESCRIBE rainier_user 验证 position_id BIGINT NULL 列
9. curl 端到端流程（建岗位 → 建角色 → 给用户挂岗位 + 公司级 hat + 项目级 hat）

### 第二优先（P1 — 部署后尽快）

无（本变更范围聚焦 v0 必需）

### 第三优先（P2）

无

## 六、TC 编号对照表

| TC-ID | Spec Scenario | 文件 |
|---|---|---|
| TC-POS-001..010 | entity-position 10 scenarios | PositionController{Create,Query,Delete}Test |
| TC-ROL-001..008 | entity-role 8 scenarios | RoleController{Create,Query,Delete}Test |
| TC-UROL-001..009 | entity-user-role 9 scenarios | UserRoleController{Create,Query,Delete}Test |
| TC-USR-001..004 | entity-user MODIFIED 4 scenarios | UserControllerTest（扩展） |
| TC-FES-H01..H03 | frontend-scaffold MODIFIED 3 scenarios | AppLayout.test (扩展) + AppRoutes.test (扩展) + UsersPage.test (新建) |

**总计**：34 P0 TCs；覆盖 15 Requirements / 34 Scenarios（1:1 映射）。

> 注：spec Position Create R 含 4 个 Scenario，但 entity-role Create R 含 3 个 Scenario 而非对称的 4 个。这是有意的：Role 无 enum 字段（category），因此无"非法 enum"分支。对应 TC 数 TC-POS (10) > TC-ROL (8) 即两个分支差。
