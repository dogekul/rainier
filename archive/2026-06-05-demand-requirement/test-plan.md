# v0.0.6 测试方案与详细案例

> 版本：v0.0.6-demand-requirement
> 创建日期：2026-06-05
> 对应 Phase 2 Spec：
> - changes/2026-06-05-demand-requirement/specs/entity-demand/spec.md
> - changes/2026-06-05-demand-requirement/specs/entity-requirement/spec.md
> - changes/2026-06-05-demand-requirement/specs/entity-demand-requirement/spec.md
> - changes/2026-06-05-demand-requirement/specs/workflow-demand-conversion/spec.md
> - changes/2026-06-05-demand-requirement/specs/frontend-scaffold/spec.md

## 一、测试策略

### 1.1 测试金字塔

- **后端集成（占 80%）**：`@SpringBootTest @AutoConfigureMockMvc` 覆盖所有 endpoint 的 HTTP 行为 + DB 持久化 + service 校验链路。复用 v0.0.3-0.0.5 已证明的 ROI 最高模式。
- **后端单元（占 5%）**：仅在涉及事务回滚验证时补（workflow-demand-conversion 的回滚测试可考虑用 @Transactional 集成测试或 RestTemplate + DB 直查验证）
- **前端组件（占 15%）**：vitest + @testing-library/react 覆盖 Sider 菜单、3 页表头与新建路径、requirement EditDrawer 的 sourceDemandIds 多选

### 1.2 测试原则

- **每个 Scenario 至少 1 TC，可拆分多个**（如 create + verify 关联是 2 个断言点 → 同一 TC 内）
- **错误路径必须真正抛错**：BadRequest / Conflict 用 `andExpect(status().is4xxClientError())` + `jsonPath("$.message", containsString("..."))`
- **DB 副作用必须直查验证**：M2M 链接 + 软删 / 硬删的 row count 通过 repository 或 native query 显式校验
- **前端测试只测组件行为**：mock api/* 模块，不真请求后端

### 1.3 已有测试资产（v0.0.5 baseline）

| 测试文件 | 用例数 | 类型 | 本变更影响 |
|---|---|---|---|
| 后端 v0.0.5 全部测试 | 64 | 集成/单元 | 0 修改 |
| frontend 全部 v0.0.5 测试 | 13 | 组件 | 0 修改（AppLayout test 不动；新菜单组渲染会被 Sider 新 test 覆盖） |
| **新增后端测试** | **≥ 30** | 集成 | 见第二节 |
| **新增前端测试** | **≥ 6** | 组件 | 见第二节 |
| 总计 | ≥ 113 | — | 64 backend + ≥ 30 new = ≥ 94 backend；13 frontend + ≥ 6 new = ≥ 19 frontend |

## 二、详细测试案例

### 功能 1：entity-demand — 诉求 CRUD（11 TCs）

#### 案例 1.1 — POST 最小 payload + 默认值

| 字段 | 内容 |
|---|---|
| **ID** | TC-DMD-001 |
| **对应 Spec** | entity-demand/spec.md → Scenario: 最小 payload 创建诉求 + 默认值 |
| **优先级** | P0 |
| **预置** | DB 含 user id=1 |
| **输入** | MockMvc POST `/api/demands` body `{"title":"X","description":"X","submitterUserId":1}` |
| **预期** | 201 / body.id isNumber / status="PENDING" / priority="MEDIUM" / source="WEB" / aiClassification=null / aiDuplicateHint=null / Location matchesPattern("/api/demands/\\d+") |
| **位置** | DemandControllerCreateTest.java |

#### 案例 1.2 — POST 缺 title 返 400

| 字段 | 内容 |
|---|---|
| **ID** | TC-DMD-002 |
| **对应 Spec** | entity-demand → 必填字段缺失被拒 |
| **优先级** | P0 |
| **输入** | POST body 缺 `title` |
| **预期** | 400 + fieldErrors[*].field="title" |
| **位置** | DemandControllerCreateTest |

#### 案例 1.3 — POST submitter 不存在返 400

| TC-DMD-003 | P0 | POST body `submitterUserId=999999` → 400 + message 含 "submitter user not found" | DemandControllerCreateTest |
|---|---|---|---|

#### 案例 1.4 — POST 非法 status 返 400

| TC-DMD-004 | P0 | POST body `status="UNKNOWN"` → 400 + message 含 "invalid status" | DemandControllerCreateTest |
|---|---|---|---|

#### 案例 1.5 — GET 详情返完整字段集

| TC-DMD-005 | P0 | DB 有 demand id=1 → GET `/api/demands/1` → 200 + body 字段集 = [id, title, description, submitterUserId, status, priority, source, aiClassification, aiDuplicateHint, closeReason, createTime, updateTime, createBy, updateBy] | DemandControllerQueryTest |
|---|---|---|---|

#### 案例 1.6 — GET 软删返 404

| TC-DMD-006 | P0 | DB demand id=1 del_flag=1 → GET → 404 | DemandControllerQueryTest |
|---|---|---|---|

#### 案例 1.7 — 列表 status 过滤

| TC-DMD-007 | P0 | DB 2 PENDING + 1 IN_REVIEW → GET `?status=PENDING` → total=2 / content[*].status="PENDING" | DemandControllerQueryTest |
|---|---|---|---|

#### 案例 1.8 — PUT 更新状态

| TC-DMD-008 | P0 | demand id=1 → PUT body `status="IN_REVIEW", priority="HIGH"` → body.status=IN_REVIEW / priority=HIGH | DemandControllerQueryTest |
|---|---|---|---|

#### 案例 1.9 — PUT 含 aiClassification 静默忽略

| TC-DMD-009 | P0 | PUT body 含 `aiClassification="hack"` → 200 + body.aiClassification 未变 | DemandControllerQueryTest |
|---|---|---|---|

#### 案例 1.10 — DELETE 无 link 成功

| TC-DMD-010 | P0 | demand id=1 无 link → DELETE → 204 + 后续 GET 404 | DemandControllerDeleteTest |
|---|---|---|---|

#### 案例 1.11 — DELETE 有 link 返 409

| TC-DMD-011 | P0 | demand id=1 有 link → DELETE → 409 + message 含 "demand has linked requirements" | DemandControllerDeleteTest |
|---|---|---|---|

### 功能 2：entity-requirement — 需求 CRUD（9 TCs）

| TC-ID | 优先级 | Scenario | 关键断言 |
|---|---|---|---|
| TC-REQ-001 | P0 | 最小 payload 创建 + 默认值 | 201 / status="DRAFT" / priority="MEDIUM" / complexity=null / projectId=null / Location matchesPattern("/api/requirements/\\d+") |
| TC-REQ-002 | P0 | code 全局唯一性冲突 | 409 + message 含 "code already exists" |
| TC-REQ-003 | P0 | ownerUser 不存在 | 400 + message 含 "owner user not found" |
| TC-REQ-004 | P0 | GET 详情完整字段集 | body 字段集 = [id, code, title, description, ownerUserId, status, priority, complexity, projectId, closeReason, createTime, updateTime, createBy, updateBy] |
| TC-REQ-005 | P0 | 按 projectId 筛选 | DB native update 给一条设 projectId=42 → GET `?projectId=42` → total=1 |
| TC-REQ-006 | P0 | PUT 状态更新 | body.status="APPROVED" |
| TC-REQ-007 | P0 | PUT body ownerUserId 静默忽略 | 200 + body.ownerUserId 未变 |
| TC-REQ-008 | P0 | DELETE 无 link → 204 | 204 + 后续 GET 404 |
| TC-REQ-009 | P0 | DELETE 有 link → 409 | 409 + message 含 "requirement has linked demands" |

**位置**：RequirementControllerCreateTest / QueryTest / DeleteTest（拆 3 文件，与 demand 一致）

### 功能 3：entity-demand-requirement — M2M CRUD + 辅助查询（7 TCs）

| TC-ID | 优先级 | Scenario | 关键断言 |
|---|---|---|---|
| TC-DRL-001 | P0 | 合法链接创建 | 201 + body 字段集 + DB row count=1 |
| TC-DRL-002 | P0 | 唯一性冲突 (demand_id, requirement_id) | 409 + message 含 "link already exists" |
| TC-DRL-003 | P0 | demandId 不存在 | 400 + message 含 "demand not found" |
| TC-DRL-004 | P0 | 按 demandId 过滤列表 | total=2 + content 全部 demandId=1 |
| TC-DRL-005 | P0 | 硬删 | 204 + 后续 GET 404 + repo.count(id=1)=0 |
| TC-DRL-006 | P0 | GET /requirements/{id}/source-demands | 数组长度=2 + 每项 demand 字段 + linkType + linkId |
| TC-DRL-007 | P0 | GET /demands/{id}/derived-requirements | 长度=1 + requirement 字段 + linkType="DERIVED" |

**位置**：DemandRequirementControllerTest（4 case） + RequirementSourceDemandsTest（DRL-006） + DemandDerivedRequirementsTest（DRL-007）

### 功能 4：workflow-demand-conversion — 原子转化（3 TCs）

| TC-ID | 优先级 | Scenario | 关键断言 |
|---|---|---|---|
| TC-DRC-001 | P0 | 含 sourceDemandIds=[10,20] 成功转化 | 201 + body 不含 sourceDemandIds + DB demand_requirement +2 行 + GET source-demands 长度=2 |
| TC-DRC-002 | P0 | sourceDemandIds 含 999999 → 整体回滚 | 400 + DB 不存在该 code requirement + DB demand_requirement 行数未变 |
| TC-DRC-003 | P0 | 空数组或缺字段 = 普通创建 | 201 + DB demand_requirement 无新行 |

**位置**：RequirementConversionTest（独立文件，含 @Transactional 验证回滚）

### 功能 5：frontend-scaffold MODIFIED — Sider 菜单 + 路由 + EditDrawer（3 TCs）

| TC-ID | 优先级 | Scenario | 关键断言 |
|---|---|---|---|
| TC-FES-D01 | P0 | Sider 含「需求管理」3 项 | screen.getByText("需求管理") + 展开后 3 项 + 点击诉求跳 /pm/demands |
| TC-FES-D02 | P0 | /pm/* 路由直接访问 + grep 校验 | render `<App />` at `/pm/demands` → DemandsPage 组件渲染 + Bash `grep -c "/pm/demands" frontend/src/AppRoutes.tsx` >= 1 |
| TC-FES-D03 | P0 | requirement EditDrawer 多选源诉求 | mock listDemands 返回 2 条 → 勾选 2 条 + 保存 → mock createRequirement 收到 body.sourceDemandIds=[10,20] |

**位置**：AppLayout.test.tsx（增 TC-FES-D01 项）、AppRoutes.test.tsx（新建，TC-FES-D02）、RequirementEditDrawer.test.tsx（新建，TC-FES-D03）

## 三、测试执行矩阵

| 功能模块 | 单元 | 集成 | 组件 | E2E | 状态 |
|---|---|---|---|---|---|
| entity-demand | — | 11 TCs | — | E2E POST + DESCRIBE | 🟢 充分 |
| entity-requirement | — | 9 TCs | — | E2E POST + DESCRIBE | 🟢 充分 |
| entity-demand-requirement | — | 7 TCs | — | E2E POST + DESCRIBE | 🟢 充分 |
| workflow-demand-conversion | — | 3 TCs（含回滚） | — | E2E POST w/ sourceDemandIds | 🟢 充分 |
| frontend-scaffold MODIFIED | — | — | 3 TCs | 浏览器手测 3 页 | 🟢 充分 |

## 四、回归风险矩阵

| 风险区域 | v0.0.6 改动 | 已有回归保护 | 风险等级 |
|---|---|---|---|
| v0.0.5 baseline（org/user/uo CRUD） | 0 改动 | 64 backend 测试 | 🟢 低 |
| BaseEntity + 异常体系 | 0 改动 | BaseEntityReflectionTest + GlobalExceptionHandlerTest | 🟢 低 |
| AppLayout Sider | 新增菜单组 | AppLayout.test 改造 + 新 TC-FES-D01 | 🟡 中 |
| AppRoutes.tsx | 新增 4 路由 | TC-FES-D02 grep + 路由 mount | 🟡 中（v0.0.3 历史上有 linter 回退） |
| 新 demand_requirement 表 + FK 链 | 新建 3 entity + service | 7 + 3 = 10 集成 TC 覆盖 | 🟢 低 |
| workflow 事务回滚 | 新 service 方法 + @Transactional | TC-DRC-002 显式验证 | 🟡 中 |
| 前端 `client.ts` axios 实例 | 0 改动（复用） | v0 auth.test | 🟢 低 |
| docker-compose / application.yml | 0 改动 | docker E2E | 🟢 低 |

## 五、建议补充顺序

### 第一优先（P0 — 部署前必补）

**Backend**：
1. TC-DMD-001..011（11）
2. TC-REQ-001..009（9）
3. TC-DRL-001..007（7）
4. TC-DRC-001..003（3）
合计 30 TC

**Frontend**：
5. TC-FES-D01..D03（3）

**E2E（test-report 阶段执行）**：
6. docker compose down -v + up 后 DESCRIBE 三张新表（schema 验证）
7. curl POST 实地确认

### 第二优先（P1 — 部署后尽快）

无（本变更范围已聚焦 v0 必需）

### 第三优先（P2）

无

## 六、TC 编号对照表

| TC-ID | Spec Scenario | 文件 |
|---|---|---|
| TC-DMD-001..011 | entity-demand 11 scenarios | DemandController{Create,Query,Delete}Test |
| TC-REQ-001..009 | entity-requirement 9 scenarios | RequirementController{Create,Query,Delete}Test |
| TC-DRL-001..007 | entity-demand-requirement 7 scenarios | DemandRequirementControllerTest + 2 辅助 test |
| TC-DRC-001..003 | workflow-demand-conversion 3 scenarios | RequirementConversionTest |
| TC-FES-D01..D03 | frontend-scaffold MODIFIED 3 scenarios | AppLayout.test + AppRoutes.test（新） + RequirementEditDrawer.test（新） |

**总计**：33 P0 TCs；覆盖 16 Requirements / 33 Scenarios（1:1 映射）。
