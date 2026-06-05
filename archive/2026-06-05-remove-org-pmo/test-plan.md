# v0.0.5 测试方案与详细案例

> 版本：v0.0.5-remove-org-pmo
> 创建日期：2026-06-05
> 对应 Phase 2 Spec：
> - changes/2026-06-05-remove-org-pmo/specs/entity-organization/spec.md
> - changes/2026-06-05-remove-org-pmo/specs/frontend-scaffold/spec.md

## 一、测试策略

### 1.1 测试金字塔

本变更是 **纯字段移除**，测试策略重点在 **negative assertion + 契约 verification**：

- **后端单元/集成**（占比 60%）：复用 v0.0.4 已有 ≥ 62 个测试基线；额外补 2 个 negative MockMvc 测试覆盖 "doesNotExist" 与 "PUT 容错忽略 isPmo"；删除 1 个 isPmo 正向断言
- **前端组件级**（占比 25%）：新增 1 个 EditDrawer 测试 + 1 个 OrganizationsPage 测试，断言 PMO 控件不渲染
- **E2E 契约**（占比 15%）：down -v + DESCRIBE 校验 schema 不含 is_pmo

### 1.2 测试原则

- **行为锚点优先**：每个 v0.0.4 已通过的 controller/repo 测试在 isPmo 删除后必须仍绿（行为锚点 = 删除前后该测试通过 = 移除是纯减法）
- **doesNotExist 是核心 RED 信号**：未来回归（有人不小心 add back isPmo）必然让 doesNotExist 失败
- **E2E DESCRIBE 是 schema 契约兜底**：防止"代码删了但 DB 残留列"

### 1.3 已有测试资产（v0.0.4 baseline）

| 测试文件 | 用例数 | 类型 | 覆盖范围 | 本变更影响 |
|---|---|---|---|---|
| OrganizationControllerCreateTest.java | 5 | 集成 | POST 端点 5 个场景 | 1 处删除 + 1 处新增（doesNotExist） |
| OrganizationControllerQueryTest.java | 11 | 集成 | GET / 列表 / 树 / id-migration | +2 新增（TC-RMP-002 + TC-RMP-003） |
| OrganizationDeleteFkTest.java | 1 | 集成 | DELETE FK | 0 修改 |
| OrganizationRepositoryTest.java | 3 | repo | 工厂 + native query | 0 修改 |
| UserControllerTest.java | 10 | 集成 | User CRUD | 0 修改 |
| UserOrganizationControllerTest.java | 10 | 集成 | UO CRUD + demote | 0 修改 |
| GlobalExceptionHandlerTest.java | 5 | 单元 | 异常体系 | 0 修改 |
| PathVariableTypeMismatchTest.java | 1 | 集成 | 400 兜底 | 0 修改 |
| BaseEntityReflectionTest.java | 1 | 单元 | id 类型 | 0 修改 |
| 其他 v0 测试（health/auth/login/me/cors/page） | 18 | — | — | 0 修改 |
| **后端合计** | **64** (含本变更新增) | — | — | 1 替换（line 67）+ 2 新增 |
| frontend Table.test.tsx | 2 | 组件 | 列表渲染 | 0 修改 |
| frontend TreeSelect.test.tsx | 1 | 组件 | 树选择 | 0 修改 |
| frontend AppLayout.test.tsx | 1 | 组件 | Sider | 0 修改 |
| frontend v0 测试（App / ProtectedRoute / Login / tokens / auth） | 7 | 组件/单元 | v0 | 0 修改 |
| **frontend 合计** | **13** (含本变更新增) | — | — | 2 新增 |

## 二、详细测试案例

### 功能 1：API 响应 body 不含 isPmo

> 对应 spec entity-organization Requirement 1 — Organization 实体与 API 契约不再含 isPmo 字段

#### 案例 1.1 — POST 响应 doesNotExist 断言

| 字段 | 内容 |
|---|---|
| **ID** | TC-RMP-001 |
| **对应 Spec** | entity-organization/spec.md → Scenario: POST /api/organizations 响应不含 isPmo |
| **优先级** | P0 |
| **预置条件** | 数据库为空，backend MockMvc 集成测试环境就绪 |
| **输入** | MockMvc `POST /api/organizations` body `{"type":"COMPANY","code":"HQ","name":"X"}` |
| **预期结果** | 201；`jsonPath("$.id").isNumber()`；`jsonPath("$.isPmo").doesNotExist()`；`jsonPath("$.name").value("X")` |
| **当前状态** | ❌ 待添加（增量改造 OrganizationControllerCreateTest 中已有 create 测试） |
| **位置** | OrganizationControllerCreateTest.java（替换 line 67 的正向 isPmo 断言为 doesNotExist） |

#### 案例 1.2 — GET 详情响应 doesNotExist 断言

| 字段 | 内容 |
|---|---|
| **ID** | TC-RMP-002 |
| **对应 Spec** | entity-organization/spec.md → Scenario: GET /api/organizations/{id} 响应不含 isPmo |
| **优先级** | P0 |
| **预置条件** | 数据库存在 1 个节点 id=1（通过 POST 准备） |
| **输入** | MockMvc `GET /api/organizations/1` |
| **预期结果** | 200；`jsonPath("$.isPmo").doesNotExist()`；`jsonPath("$.id").value(1)` |
| **当前状态** | ❌ 待添加（OrganizationControllerQueryTest 内增） |
| **位置** | OrganizationControllerQueryTest.java（新增 1 test） |

#### 案例 1.3 — PUT 容错忽略客户端发送的 isPmo

| 字段 | 内容 |
|---|---|
| **ID** | TC-RMP-003 |
| **对应 Spec** | entity-organization/spec.md → Scenario: PUT /api/organizations/{id} body 带 isPmo 静默忽略 |
| **优先级** | P0 |
| **预置条件** | 数据库存在节点 id=1，code="HQ"，name="X" |
| **输入** | MockMvc `PUT /api/organizations/1` body `{"code":"HQ","name":"Y","isPmo":true}` |
| **预期结果** | 200（非 400，验证 Jackson 默认配置生效）；`jsonPath("$.name").value("Y")`；`jsonPath("$.isPmo").doesNotExist()` |
| **当前状态** | ❌ 待添加 |
| **位置** | OrganizationControllerQueryTest.java（新增 1 test） 或新建文件 |

### 功能 2：DB schema 不含 is_pmo 列

> 对应 spec entity-organization Requirement 2 — schema 验证

#### 案例 2.1 — DESCRIBE rainier_organization 无 is_pmo 列

| 字段 | 内容 |
|---|---|
| **ID** | TC-RMP-E2E-001 |
| **对应 Spec** | entity-organization/spec.md → Scenario: DESCRIBE 表结构无 is_pmo 列 |
| **优先级** | P0 |
| **预置条件** | 执行 `docker compose down -v && docker compose up -d --build`；所有服务 healthy |
| **输入** | Bash: `docker exec rainier-mysql mysql -urainier -prainier rainier -e "DESCRIBE rainier_organization"` |
| **预期结果** | stdout 不含 `is_pmo` 字符串（`grep -c is_pmo` 返回 0） |
| **当前状态** | ❌ 待添加（Phase 5 verify 手动执行） |
| **位置** | test-report.md 的 E2E 章节 |

### 功能 3：前端 UI 不渲染 PMO 控件

> 对应 spec frontend-scaffold Requirement 1

#### 案例 3.1 — EditDrawer 无 PMO 复选框

| 字段 | 内容 |
|---|---|
| **ID** | TC-RMP-FE-001 |
| **对应 Spec** | frontend-scaffold/spec.md → Scenario: EditDrawer 渲染时无 PMO 复选框 |
| **优先级** | P0 |
| **预置条件** | vitest + @testing-library/react；mock `getOrganizationTree` 返回 `[]` |
| **输入** | mount `<OrganizationEditDrawer open={true} editing={null} onClose={vi.fn()} onSubmit={vi.fn()} />` |
| **预期结果** | `screen.queryByLabelText('PMO 团队')` === null；`screen.queryByText('PMO 团队')` === null |
| **当前状态** | ❌ 待添加（新建 EditDrawer.test.tsx） |
| **位置** | frontend/src/pages/Organization/EditDrawer.test.tsx（新文件） |

#### 案例 3.2 — OrganizationsPage 列表表头无 PMO 列

| 字段 | 内容 |
|---|---|
| **ID** | TC-RMP-FE-002 |
| **对应 Spec** | frontend-scaffold/spec.md → Scenario: OrganizationsPage 列表表头无 PMO 列 |
| **优先级** | P0 |
| **预置条件** | vitest + @testing-library/react；mock `listOrganizations` 返回 PaginatedResult |
| **输入** | mount `<OrganizationsPage />` |
| **预期结果** | `screen.queryAllByRole('columnheader').map(h => h.textContent)` 数组不含 'PMO'；含 ['编码','名称','类型','全路径','操作'] |
| **当前状态** | ❌ 待添加（新建 OrganizationsPage.test.tsx） |
| **位置** | frontend/src/pages/Organization/OrganizationsPage.test.tsx（新文件） |

#### 案例 3.3 — TypeScript 类型契约校验

| 字段 | 内容 |
|---|---|
| **ID** | TC-RMP-FE-003 |
| **对应 Spec** | frontend-scaffold/spec.md → Scenario: TypeScript 类型契约 — Organization 类型无 isPmo |
| **优先级** | P0 |
| **预置条件** | frontend/ 已 npm ci |
| **输入** | `npm run build` |
| **预期结果** | tsc -b 退出码 0；`grep -rn 'isPmo' frontend/src` 返回 0 行 |
| **当前状态** | ❌ 待添加（Phase 5 verify 手动执行） |
| **位置** | test-report.md 的 lint 章节 |

## 三、测试执行矩阵

| 功能模块 | 单元测试 | 集成测试 | E2E | 状态 |
|---|---|---|---|---|
| Organization API 不含 isPmo | — | TC-RMP-001/002/003 | TC-RMP-E2E-001 | 🟢 计划充分 |
| Frontend UI 无 PMO 控件 | TC-RMP-FE-001/002/003 | — | (浏览器手测) | 🟢 计划充分 |
| 回归保护（v0.0.4 baseline） | — | 65 既有测试需保持绿 | TC-RMP-E2E-001 配套手测 | 🟢 复用已有 |

## 四、回归风险矩阵

| 风险区域 | v0.0.5 改动 | 已有回归保护 | 风险等级 |
|---|---|---|---|
| Organization POST/PUT/GET | 删除 isPmo 字段、新增 doesNotExist 断言 | 5 CreateTest + 11 QueryTest + 1 DeleteFkTest = 17 测试 | 🟡 中：删除断言可能漏改某行 |
| Organization 树 cascade（name/path/move/delete） | 0 直接改动 | 11 QueryTest 含 cascade 用例 | 🟢 低：不动 cascade 逻辑 |
| User / UserOrganization 实体 | 0 改动 | 10 + 10 测试 | 🟢 低：完全不动 |
| 前端 OrganizationsPage onSubmit 链路 | 删除 isPmo prop，不动 update/move 调用 | 0（v0.0.4 是无 unit test 的）+ 浏览器手测 | 🟡 中：建议补 OrganizationsPage.test |
| 前端 EditDrawer 表单 | 删除 PMO 复选框 + isPmo state | 0（v0.0.4 无 unit test）+ 浏览器手测 | 🟡 中：本变更补 EditDrawer.test |
| DB schema 残留 is_pmo 列 | 依赖 down -v 干净重生 | TC-RMP-E2E-001 DESCRIBE 校验 | 🟢 低：契约级兜底已就位 |
| Jackson 反序列化策略变更 | 依赖默认 ignore-unknown=true | TC-RMP-003 显式验证 | 🟢 低：测试覆盖 |

## 五、建议补充顺序

### 第一优先（P0 — 部署前必补）

1. TC-RMP-001（POST doesNotExist）
2. TC-RMP-002（GET doesNotExist）
3. TC-RMP-003（PUT 容错忽略）
4. TC-RMP-FE-001（EditDrawer 无 PMO 复选框）
5. TC-RMP-FE-002（OrganizationsPage 无 PMO 列）
6. TC-RMP-FE-003（tsc + grep 校验）
7. TC-RMP-E2E-001（DESCRIBE 无 is_pmo）

### 第二优先（P1 — 部署后尽快）

无（本变更范围极小）

### 第三优先（P2）

无

## 六、TC 编号对照表

| TC-ID | Spec Scenario | 类型 | 文件 |
|---|---|---|---|
| TC-RMP-001 | POST 响应不含 isPmo | 集成 | OrganizationControllerCreateTest.java |
| TC-RMP-002 | GET 响应不含 isPmo | 集成 | OrganizationControllerQueryTest.java |
| TC-RMP-003 | PUT body 带 isPmo 静默忽略 | 集成 | OrganizationControllerQueryTest.java |
| TC-RMP-FE-001 | EditDrawer 无 PMO 复选框 | 组件 | EditDrawer.test.tsx (新) |
| TC-RMP-FE-002 | OrganizationsPage 无 PMO 列 | 组件 | OrganizationsPage.test.tsx (新) |
| TC-RMP-FE-003 | TS 类型契约 | E2E-lint | test-report.md |
| TC-RMP-E2E-001 | DESCRIBE 表结构 | E2E | test-report.md |
