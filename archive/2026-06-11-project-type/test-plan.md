# v0.0.16-project-type 测试方案与详细案例

> 版本：v0.0.16-project-type
> 创建日期：2026-06-11
> 对应 Phase 2 Spec：specs/entity-project/spec.md (MOD) + specs/frontend-scaffold/spec.md (MOD)

## 一、测试策略

### 1.1 测试金字塔

- **单元/集成(后端，主力)**：`@SpringBootTest`+`MockMvc` 覆盖 create/update/list/detail 的 projectType 行为 + 回填 runner + DTO 读兜底。
- **组件(前端)**：vitest + RTL 覆盖 ProjectsPage 类型下拉/类型列/类型过滤。
- **E2E(关键路径)**：docker compose + curl + MySQL，验证存量回填 + 转化链 + 既有数据不变。

### 1.2 测试原则

- 纯新增字段，最小回归面;既有 Project 行为(create 必填校验/owner/FK 保护/软删)用例保持绿。
- standing：测试不删改存量业务数据;回填仅补 null 类型列，断言其它列不变。
- 字段集 exact-equality 测试同步加 `projectType`(避免 detail 字段集断言假红/假绿)。

### 1.3 已有测试资产

| 测试文件 | 类型 | 覆盖范围 |
|----------|------|----------|
| backend `.../project/...ProjectControllerTest` 等(既有) | 集成 | 既有 create/update/list/detail/delete |
| frontend `pages/Project/ProjectsPage.test.tsx`(既有) | 组件 | 既有 ProjectsPage 渲染/编辑 |
| backend `.../project/bootstrap/DanglingProjectIdCleanup`(参考) | — | 回填 runner 模式参考 |

## 二、详细测试案例

### 功能 1：创建项目带项目类型（entity-project）

#### 案例 1.1 — 省略 projectType 默认 CASUAL

| 字段 | 内容 |
|------|------|
| **ID** | TC-PROJTYPE-001 |
| **对应 Spec** | entity-project → Scenario: 省略 projectType 默认 CASUAL |
| **优先级** | P0 |
| **预置条件** | 用户 id=1 存在 |
| **输入** | `POST /api/projects` `{"code":"PT-001","name":"X","ownerUserId":1}` |
| **预期结果** | 201;body.projectType="CASUAL" |
| **当前状态** | ❌ 测试缺 |

#### 案例 1.2 — 显式 FORMAL 创建

| 字段 | 内容 |
|------|------|
| **ID** | TC-PROJTYPE-002 |
| **对应 Spec** | entity-project → Scenario: 显式 FORMAL 创建正式项目 |
| **优先级** | P0 |
| **预置条件** | 用户 id=1 存在 |
| **输入** | `POST /api/projects` body 含 `"projectType":"FORMAL"` |
| **预期结果** | 201;body.projectType="FORMAL" |
| **当前状态** | ❌ 测试缺 |

#### 案例 1.3 — 非法 projectType → 400

| 字段 | 内容 |
|------|------|
| **ID** | TC-PROJTYPE-003 |
| **对应 Spec** | entity-project → Scenario: 非法 projectType 被拒 |
| **优先级** | P0 |
| **预置条件** | backend 启动 |
| **输入** | `POST /api/projects` body 含 `"projectType":"XXX"` |
| **预期结果** | 400;message 含 "invalid project type" |
| **当前状态** | ❌ 测试缺 |

### 功能 2：更新项目类型（转化，entity-project）

#### 案例 2.1 — CASUAL→FORMAL 转化

| 字段 | 内容 |
|------|------|
| **ID** | TC-PROJTYPE-004 |
| **对应 Spec** | entity-project → Scenario: CASUAL 改 FORMAL 完成转化 |
| **优先级** | P0 |
| **预置条件** | 项目 id=X projectType="CASUAL" |
| **输入** | `PUT /api/projects/X` body 含 `"projectType":"FORMAL"`(+name/status/ownerUserId) |
| **预期结果** | 200;body.projectType="FORMAL" |
| **当前状态** | ❌ 测试缺 |

#### 案例 2.2 — update 省略 projectType 保留原值（防静默降级）

| 字段 | 内容 |
|------|------|
| **ID** | TC-PROJTYPE-005 |
| **对应 Spec** | entity-project → Scenario: update 省略 projectType 保留原值 |
| **优先级** | P0 |
| **预置条件** | 项目 id=X projectType="FORMAL" |
| **输入** | `PUT /api/projects/X` body 不含 projectType(其它字段齐) |
| **预期结果** | 200;body.projectType 仍="FORMAL"(未降级) |
| **当前状态** | ❌ 测试缺 |

#### 案例 2.3 — update 非法 projectType → 400

| 字段 | 内容 |
|------|------|
| **ID** | TC-PROJTYPE-006 |
| **对应 Spec** | entity-project → Scenario: update 非法 projectType 被拒 |
| **优先级** | P0 |
| **预置条件** | 项目 id=X 存在 |
| **输入** | `PUT /api/projects/X` body 含 `"projectType":"XXX"` |
| **预期结果** | 400;message 含 "invalid project type" |
| **当前状态** | ❌ 测试缺 |

### 功能 3：列表过滤 + 详情字段（entity-project）

#### 案例 3.1 — 按 projectType 过滤

| 字段 | 内容 |
|------|------|
| **ID** | TC-PROJTYPE-007 |
| **对应 Spec** | entity-project → Scenario: 按 projectType 过滤仅返回匹配项 |
| **优先级** | P0 |
| **预置条件** | 2 个 FORMAL + 1 个 CASUAL |
| **输入** | `GET /api/projects?projectType=FORMAL` |
| **预期结果** | total=2;content 全部 projectType="FORMAL" |
| **当前状态** | ❌ 测试缺 |

#### 案例 3.2 — 详情字段集含 projectType

| 字段 | 内容 |
|------|------|
| **ID** | TC-PROJTYPE-008 |
| **对应 Spec** | entity-project → Scenario: 详情字段集含 projectType |
| **优先级** | P0 |
| **预置条件** | 项目 id=X projectType="FORMAL" |
| **输入** | `GET /api/projects/X` |
| **预期结果** | 200;body 含 projectType="FORMAL";既有「字段集 exact-equality」测试同步加 projectType 后仍绿 |
| **当前状态** | ❌ 测试缺(含既有字段集测试同步) |

### 功能 4：存量回填 + 读兜底（entity-project）

#### 案例 4.1 — 回填 runner: NULL → CASUAL

| 字段 | 内容 |
|------|------|
| **ID** | TC-PROJTYPE-009 |
| **对应 Spec** | entity-project → Scenario: 启动回填 NULL 行为 CASUAL |
| **优先级** | P0 |
| **预置条件** | 注入一行 project_type=NULL(native UPDATE set null) + 记录其它列值 |
| **输入** | 调用 `ProjectTypeBackfill.run()` |
| **预期结果** | 该行 project_type="CASUAL";code/name/status/owner/dates/enabled 不变 |
| **当前状态** | ❌ 测试缺 |

#### 案例 4.2 — 读路径 null→CASUAL 兜底（DTO）

| 字段 | 内容 |
|------|------|
| **ID** | TC-PROJTYPE-010 |
| **对应 Spec** | entity-project → Scenario: 回填前读路径 null→CASUAL 兜底 |
| **优先级** | P0 |
| **预置条件** | 一行 project_type=NULL 未回填 |
| **输入** | `ProjectDetail.from(project)`(或 GET 详情，回填未跑) |
| **预期结果** | dto.projectType="CASUAL"(非 null) |
| **当前状态** | ❌ 测试缺 |

### 功能 5：前端类型 UI（frontend-scaffold）

#### 案例 5.1 — 新建抽屉含类型下拉 + 默认轻量

| 字段 | 内容 |
|------|------|
| **ID** | TC-FES-PROJTYPE-001 |
| **对应 Spec** | frontend-scaffold → Scenario: 新建抽屉含项目类型下拉且默认轻量 |
| **优先级** | P0 |
| **预置条件** | ProjectsPage mock listUsers + listProjects |
| **输入** | 点击「新建项目」打开抽屉 |
| **预期结果** | 含 `projects-type-select`;选项有「轻量」「正式」;默认值 CASUAL |
| **当前状态** | ❌ 测试缺 |

#### 案例 5.2 — 表格类型列中文

| 字段 | 内容 |
|------|------|
| **ID** | TC-FES-PROJTYPE-002 |
| **对应 Spec** | frontend-scaffold → Scenario: 表格渲染类型列中文 |
| **优先级** | P0 |
| **预置条件** | listProjects mock 返回一行 projectType="FORMAL" |
| **输入** | 渲染 ProjectsPage |
| **预期结果** | 表格含「类型」列;该行显示「正式」 |
| **当前状态** | ❌ 测试缺 |

#### 案例 5.3 — 类型过滤触发带参查询

| 字段 | 内容 |
|------|------|
| **ID** | TC-FES-PROJTYPE-003 |
| **对应 Spec** | frontend-scaffold → Scenario: 选择类型过滤触发带参查询 |
| **优先级** | P0 |
| **预置条件** | ProjectsPage 已渲染;mockClear listProjects |
| **输入** | 在 `projects-type-filter` 选「正式」 |
| **预期结果** | listProjects 被调且 params 含 projectType="FORMAL" |
| **当前状态** | ❌ 测试缺 |

#### 案例 5.4 — 提交携带 projectType

| 字段 | 内容 |
|------|------|
| **ID** | TC-FES-PROJTYPE-004 |
| **对应 Spec** | frontend-scaffold → Scenario: 提交携带 projectType |
| **优先级** | P0 |
| **预置条件** | ProjectsPage 打开新建抽屉，类型选「正式」，必填项填妥 |
| **输入** | 点击「保存」 |
| **预期结果** | createProject 被调且 body 含 projectType="FORMAL" |
| **当前状态** | ❌ 测试缺 |

### 功能 6：E2E（关键路径）

#### 案例 6.1 — 存量回填 + 转化链 + 既有数据不变

| 字段 | 内容 |
|------|------|
| **ID** | TC-E2E-PROJTYPE-001 |
| **对应 Spec** | entity-project(回填/转化/过滤) + standing 约束 |
| **优先级** | P0 |
| **预置条件** | docker compose 重建;现有 project 行(启动前 project_type 未设) |
| **输入** | 启动后查现有 project.projectType;`POST` 一个 FORMAL 项目;`PUT` 把某 CASUAL 改 FORMAL;`GET ?projectType=FORMAL` |
| **预期结果** | 现有 project 全 CASUAL 且其它字段不变;新建 FORMAL 成功;转化成功;过滤命中;`SHOW TABLES`=18(无新表);审计表含对应 UPDATE/CREATE PROJECT 行(v0.0.15 白拿) |
| **当前状态** | ❌ 测试缺 |

## 三、测试执行矩阵

| 功能模块 | 单元/集成 | 组件 | E2E | 状态 |
|----------|---------|------|-----|------|
| create projectType(默认/显式/非法) | TC-PROJTYPE-001/002/003 | — | TC-E2E-PROJTYPE-001 | 🔴→待补 |
| update projectType(转化/保留/非法) | TC-PROJTYPE-004/005/006 | — | TC-E2E-PROJTYPE-001 | 🔴→待补 |
| list 过滤 + detail 字段 | TC-PROJTYPE-007/008 | — | TC-E2E-PROJTYPE-001 | 🔴→待补 |
| 回填 runner + 读兜底 | TC-PROJTYPE-009/010 | — | TC-E2E-PROJTYPE-001 | 🔴→待补 |
| 前端 类型下拉/列/过滤/提交 | — | TC-FES-PROJTYPE-001/002/003/004 | — | 🔴→待补 |

## 四、回归风险矩阵

| 风险区域 | v0.0.16 改动 | 已有回归保护 | 风险等级 |
|----------|-------------|-------------|---------|
| Project create/update 既有字段 | 加 projectType 分支(其它字段逻辑不动) | 既有 ProjectControllerTest 全套 | 🟢低 |
| ProjectDetail 字段集 exact-equality 测试 | 加 projectType 字段 | 既有字段集断言测试(需同步) | 🟡中(必须同步改测试) |
| 存量 MySQL 加列 | nullable 列 + 回填 | E2E 既有数据不变断言 | 🟡中 |
| 前端 ProjectsPage 既有列/抽屉 | 加类型列/下拉/过滤 | 既有 ProjectsPage.test | 🟢低 |
| list filter 链路 | 加 projectType predicate | 既有 status filter 测试 | 🟢低 |

## 五、建议补充顺序

1. **第一优先(部署前必补，全 P0)**：TC-PROJTYPE-001..010 + TC-FES-PROJTYPE-001..003 + TC-E2E-PROJTYPE-001
2. **第二优先(P1)**：无
3. **第三优先(P2)**：无
