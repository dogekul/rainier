# v0.0.11-task 测试方案与详细案例

> 版本：v0.0.11-task
> 创建日期：2026-06-09
> 对应 Phase 2 Spec：`specs/entity-task/spec.md` (NEW) / `specs/entity-project/spec.md` (MOD) / `specs/frontend-scaffold/spec.md` (MOD)
> 基线：tag `v0.0.10.1-cleanup` / commit `901eea7` — 191 backend / 41 frontend tests

## 一、测试策略

### 1.1 测试金字塔

- **单元/集成**：22 新增 P0 backend TCs（CRUD + 跨层一致性 + Project FK chain + perf）+ 2 frontend TCs（Sider + 路由 + Drawer 联动）
- **E2E**：M-Final 段做 curl smoke + `SHOW TABLES` = 13 + `DESCRIBE rainier_task`

### 1.2 测试原则

- 沿用 v0.0.10/v0.0.10.1 已建立的 fixture 模式（seed User/Project/Requirement/Sprint/Story 链 + cleanDb 顺序）
- N+1 perf 用 `Hibernate Statistics.getPrepareStatementCount()` 等号锁死（PA-1 教训）
- 跨层一致性 3 条 guard 各 1 单独 TC，验证错误 message 精确文案
- 24-field GET 详情用 loop assert（沿用 TC-STR-010 / TC-SPR-009-FULL）

### 1.3 已有测试资产（v0.0.10.1 baseline）

| 测试文件 | 用例数 | 类型 | 本次相关性 |
|----------|--------|------|------------|
| `ProjectControllerDeleteTest.java` (v0.0.8) | — | 集成 | 加 TC-PRJ-DEL-TSK Scenario |
| `StoryListSqlCountTest.java` (v0.0.10.1) | 1 | 集成 perf | mirror 模式给 `TaskListSqlCountTest` |
| `SprintListSqlCountTest.java` (v0.0.10.1) | 1 | 集成 perf | 同上 |
| `RequirementControllerQueryTest.java` | 6 | 集成 | TC fixture 模式复用 |

## 二、详细测试案例

### 功能 1：POST /api/tasks 创建（entity-task Requirement 1）

#### TC-TSK-001 — 最小 payload + 默认值 + projectName 富化

| 字段 | 内容 |
|------|------|
| **ID** | TC-TSK-001 |
| **对应 Spec** | entity-task → 最小 payload 创建 Task + 默认值 + 富化 |
| **优先级** | P0 |
| **预置条件** | seed Project id=1 ("Apollo", "PROJ-1") + User id=1 |
| **输入** | POST /api/tasks `{"code":"TASK-001","title":"修登录页 bug","projectId":1}` |
| **预期结果** | 201; status=TODO; priority=MEDIUM; sprintId=null; storyId=null; assigneeUserId=null; projectName="Apollo"; projectCode="PROJ-1"; assignee 字段 null |

#### TC-TSK-002 — 完整 payload + 全富化 + dueDate

| 字段 | 内容 |
|------|------|
| **ID** | TC-TSK-002 |
| **对应 Spec** | entity-task → 完整 payload 创建 Task w/ Sprint + Story + Assignee |
| **优先级** | P0 |
| **预置条件** | seed Project 1 / Requirement 10 / Sprint 20 / Story 30 / User 2 ("黎立", "lili") |
| **输入** | POST w/ 全字段（sprintId=20, storyId=30, assigneeUserId=2, dueDate=2026-07-15） |
| **预期结果** | 201; sprintCode/sprintName/storyCode/storyTitle/assigneeName="黎立"/assigneeLoginName="lili"; dueDate=2026-07-15 |

#### TC-TSK-003 — code 重复 → 409

| 字段 | 内容 |
|------|------|
| **ID** | TC-TSK-003 |
| **对应 Spec** | entity-task → code 重复被拒 |
| **优先级** | P0 |
| **预置条件** | seed Project 1; 已建 Task code="TASK-DUP" |
| **输入** | POST 同 code |
| **预期结果** | 409; body.message starts with "code already exists" |

#### TC-TSK-004 — projectId 不存在 → 400

| 字段 | 内容 |
|------|------|
| **ID** | TC-TSK-004 |
| **对应 Spec** | entity-task → projectId 不存在被拒 |
| **优先级** | P0 |
| **预置条件** | seed User 1; 无 Project id=999 |
| **输入** | POST w/ projectId=999 |
| **预期结果** | 400; "project not found" |

#### TC-TSK-005 — assigneeUserId 不存在 → 400

| 字段 | 内容 |
|------|------|
| **ID** | TC-TSK-005 |
| **优先级** | P0 |
| **输入** | POST w/ assigneeUserId=999999 |
| **预期结果** | 400; "assignee user not found" |

#### TC-TSK-006 — 非法 status → 400

| 字段 | 内容 |
|------|------|
| **ID** | TC-TSK-006 |
| **优先级** | P0 |
| **输入** | POST status="UNKNOWN" |
| **预期结果** | 400; "invalid status" |

#### TC-TSK-007 — 非法 priority → 400

| 字段 | 内容 |
|------|------|
| **ID** | TC-TSK-007 |
| **优先级** | P0 |
| **输入** | POST priority="EXTREME" |
| **预期结果** | 400; "invalid priority" |

#### TC-TSK-008 — 缺必填字段 → 400 fieldErrors

| 字段 | 内容 |
|------|------|
| **ID** | TC-TSK-008 |
| **优先级** | P0 |
| **输入** | POST `{"code":"TASK-X"}` |
| **预期结果** | 400; body.fieldErrors[*].field 含 "title", "projectId" |

#### TC-TSK-009 — createBy 自动注入

| 字段 | 内容 |
|------|------|
| **ID** | TC-TSK-009 |
| **优先级** | P0 |
| **输入** | POST minimal payload |
| **预期结果** | 201; body.createBy 存在（AuditorAware） |

### 功能 2：跨层一致性守护（entity-task Requirement 2 — 核心约束）

#### TC-TSK-CONS-001 — sprint 跨 project → 400 "sprint not in project"

| 字段 | 内容 |
|------|------|
| **ID** | TC-TSK-CONS-001 |
| **对应 Spec** | entity-task → sprint 跨 project 被拒 |
| **优先级** | P0 |
| **预置条件** | Project 1 + Project 2 + Requirement (proj=2) + Sprint id=20 (req→proj=2) |
| **输入** | POST w/ projectId=1, sprintId=20 |
| **预期结果** | 400; "sprint not in project" |

#### TC-TSK-CONS-002 — story 跨 project → 400 "story not in project"

| 字段 | 内容 |
|------|------|
| **ID** | TC-TSK-CONS-002 |
| **优先级** | P0 |
| **预置条件** | Story id=30 projectId=2 |
| **输入** | POST w/ projectId=1, storyId=30 |
| **预期结果** | 400; "story not in project" |

#### TC-TSK-CONS-003 — sprint+story 都 set 但 story 不属该 sprint → 400 "story not in sprint"

| 字段 | 内容 |
|------|------|
| **ID** | TC-TSK-CONS-003 |
| **优先级** | P0 |
| **预置条件** | Project 1 / Req 10 / Sprint 20 + Sprint 21（都属 Req 10）/ Story 30 (sprintId=21) |
| **输入** | POST w/ projectId=1, sprintId=20, storyId=30 |
| **预期结果** | 400; "story not in sprint" |

### 功能 3：GET /api/tasks 查询（entity-task Requirement 3）

#### TC-TSK-010 — GET 详情 24 字段全有

| 字段 | 内容 |
|------|------|
| **ID** | TC-TSK-010 |
| **对应 Spec** | entity-task → GET 详情完整字段集 + 富化 |
| **优先级** | P0 |
| **预置条件** | Task id=1 关联 Project 1 / Sprint 20 / Story 30 / User 1 |
| **输入** | GET /api/tasks/1 |
| **预期结果** | 200; body.has(f) for each of 24 fields: `[id, code, title, description, status, priority, projectId, projectName, projectCode, sprintId, sprintCode, sprintName, storyId, storyCode, storyTitle, assigneeUserId, assigneeName, assigneeLoginName, dueDate, closeReason, createTime, updateTime, createBy, updateBy]` |

#### TC-TSK-011 — 按 projectId + status 联合过滤

| 字段 | 内容 |
|------|------|
| **ID** | TC-TSK-011 |
| **对应 Spec** | entity-task → 按 projectId + status 联合过滤列表 |
| **优先级** | P0 |
| **预置条件** | Project A 3 task (TODO/IN_PROGRESS/DONE) + Project B 1 task (TODO) |
| **输入** | GET ?projectId=A&status=TODO |
| **预期结果** | total=1; content[0].projectId=A.id; content[0].status=TODO |

### 功能 4：PUT /api/tasks 更新（entity-task Requirement 4）

#### TC-TSK-012 — 更新 status + assigneeUserId 转移 + 富化跟随

| 字段 | 内容 |
|------|------|
| **ID** | TC-TSK-012 |
| **对应 Spec** | entity-task → 更新 status + assigneeUserId 转移 |
| **优先级** | P0 |
| **预置条件** | Task TODO, assigneeUserId=null; User 2 存在 |
| **输入** | PUT body w/ status=IN_PROGRESS, priority=HIGH, assigneeUserId=2 |
| **预期结果** | 200; body.assigneeUserId=2; assigneeName 跟随；status=IN_PROGRESS |

#### TC-TSK-013 — unassign（assigneeUserId 设为 null）

| 字段 | 内容 |
|------|------|
| **ID** | TC-TSK-013 |
| **对应 Spec** | entity-task → 改 assigneeUserId 至 null（unassign） |
| **优先级** | P0 |
| **预置条件** | Task assigneeUserId=2 |
| **输入** | PUT body 含 assigneeUserId=null |
| **预期结果** | 200; body.assigneeUserId=null; assigneeName/assigneeLoginName=null |

#### TC-TSK-014 — PUT 新 assigneeUserId 不存在 → 400

| 字段 | 内容 |
|------|------|
| **ID** | TC-TSK-014 |
| **对应 Spec** | entity-task → PUT 新 assigneeUserId 不存在被拒 |
| **优先级** | P0 |
| **输入** | PUT assigneeUserId=999999 |
| **预期结果** | 400; "assignee user not found" |

### 功能 5：DELETE /api/tasks（entity-task Requirement 5）

#### TC-TSK-015 — 软删 + GET 404

| 字段 | 内容 |
|------|------|
| **ID** | TC-TSK-015 |
| **对应 Spec** | entity-task → 软删成功 + 后续 GET 404 |
| **优先级** | P0 |
| **输入** | DELETE /api/tasks/{id} |
| **预期结果** | 204; 后续 GET → 404 |

### 功能 6：list enrich SQL count 锁定（entity-task Requirement 6）

#### TC-PERF-TSK-001 — list size=20 PreparedStatement = 6（PA-1 修订自 7）

| 字段 | 内容 |
|------|------|
| **ID** | TC-PERF-TSK-001 |
| **对应 Spec** | entity-task → GET /api/tasks?size=20 在 enrich 阶段 = 6 个 SELECT |
| **优先级** | P0 |
| **预置条件** | seed 4 Project / 5 Req / 4 Sprint / 4 Story / 5 User / 20 Task；`hibernate.generate_statistics=true`；`stats.clear()` |
| **输入** | GET /api/tasks?size=20 |
| **预期结果** | 200; total=20; 抽样验证富化；`stats.getPrepareStatementCount() == 6` (1 page-data + 1 page-count + 4 batch — user/sprint/story/project；Requirement 不在 TaskDetail 字段集所以不 batch) |

### 功能 7：Project FK 守护扩展（entity-project Requirement 1）

#### TC-PRJ-DEL-TSK-001 — 有 Task 引用 → 409 "project has linked tasks"

| 字段 | 内容 |
|------|------|
| **ID** | TC-PRJ-DEL-TSK-001 |
| **对应 Spec** | entity-project → v0.0.11 — 有 Task 引用被拒 |
| **优先级** | P0 |
| **预置条件** | Project 1 + Task projectId=1 del_flag=0；无 Requirement/UserRole 引用 |
| **输入** | DELETE /api/projects/1 |
| **预期结果** | 409; "project has linked tasks" |

#### TC-PRJ-DEL-TSK-002 — Requirement + Task 双引用时优先返 requirement

| 字段 | 内容 |
|------|------|
| **ID** | TC-PRJ-DEL-TSK-002 |
| **对应 Spec** | entity-project → v0.0.11 — 同时有 Requirement + Task 引用时优先返 requirement 错误 |
| **优先级** | P0 |
| **预置条件** | Project 1 + Requirement + Task |
| **输入** | DELETE /api/projects/1 |
| **预期结果** | 409; "project has linked requirements"（家族 chain 顺序优先） |

### 功能 8：前端 Sider + 路由 + Drawer 联动（frontend-scaffold MOD）

#### TC-FES-TSK-001 — Sider 6 项含「任务」排第 3

| 字段 | 内容 |
|------|------|
| **ID** | TC-FES-TSK-001 |
| **对应 Spec** | frontend-scaffold → Sider 含「需求管理」6 路由 |
| **优先级** | P0 |
| **预置条件** | render `<AppLayout>` w/ logged-in store |
| **输入** | 页面渲染 |
| **预期结果** | Sider 含 6 项 "项目"/"Sprint"/"任务"/"诉求"/"需求"/"诉求-需求关联"；"任务" 位于 "Sprint" 与 "诉求" 之间；点击跳 /pm/tasks |

#### TC-FES-TSK-002 — /pm/tasks 路由 + grep guard

| 字段 | 内容 |
|------|------|
| **ID** | TC-FES-TSK-002 |
| **对应 Spec** | frontend-scaffold → /pm/tasks 路由直接访问 |
| **优先级** | P0 |
| **输入** | 路由 /pm/tasks |
| **预期结果** | 渲染 TasksPage；grep "/pm/tasks" AppRoutes.tsx ≥ 1 |

#### TC-FES-TSK-003 — TaskEditDrawer Sprint/Story 联动清空

| 字段 | 内容 |
|------|------|
| **ID** | TC-FES-TSK-003 |
| **对应 Spec** | frontend-scaffold → TaskEditDrawer Sprint/Story 联动 |
| **优先级** | P0 |
| **预置条件** | mock listProjects / listSprints / listStories；Sprint 20 (projectId=A) 已选 |
| **输入** | 用户切换 Project: A → B |
| **预期结果** | Sprint 当前选中清空；Story 当前选中清空；Sprint 下拉项过滤为 `s.projectId === B.id` |

## 三、测试执行矩阵

| 功能模块 | 单元 | 集成 | E2E | 状态 |
|----------|------|------|-----|------|
| TaskService.create + 业务校验 | — | TC-TSK-001..009 (9) | curl chain | 🟢 |
| 跨层一致性 guard | — | TC-TSK-CONS-001/002/003 (3) | curl chain | 🟢 |
| TaskService.list / findById | — | TC-TSK-010/011 (2) | curl chain | 🟢 |
| TaskService.update (immutable parents) | — | TC-TSK-012/013/014 (3) | — | 🟢 |
| TaskService.delete | — | TC-TSK-015 (1) | curl chain | 🟢 |
| List enrich perf | — | TC-PERF-TSK-001 (1) | — | 🟢 |
| ProjectService FK chain 扩展 | — | TC-PRJ-DEL-TSK-001/002 (2) | curl chain | 🟢 |
| Frontend Sider + 路由 + Drawer | — | TC-FES-TSK-001/002/003 (3) vitest | manual smoke | 🟢 |

## 四、回归风险矩阵

| 风险区域 | v0.0.11 改动 | 已有回归保护 | 风险等级 |
|----------|---------------|-------------|---------|
| 新表 rainier_task | ddl-auto=update 自动建 | TC-TSK-001..015 全覆盖；E2E DESCRIBE | 🟢 |
| `ProjectService.delete` FK chain 顺序 | 末尾追加 Task 检查（不动 1/2 顺序） | v0.0.8 TC-PRJ-DEL-001/002（既有 Requirement/UserRole）+ 新 TC-PRJ-DEL-TSK-001/002 | 🟢 |
| 跨层一致性正确性 | 3 条独立 guard | 3 条独立 TC + curl E2E + Story.projectId 二段继承已稳定（v0.0.10） | 🟢 |
| enrich N+1 回归 | 5 batch（比 Sprint 多 2 类） | TC-PERF-TSK-001 等号锁死 = 7 | 🟢 |
| Frontend Sider 6 项不溢出 | CSS 已支持 | 视觉手动验 + TC-FES-TSK-001 测试条目存在 | 🟢 |
| TaskEditDrawer 联动客户端 filter 不全 | size=100 cap，单 project 远小于 | TC-FES-TSK-003 测试联动行为；v0.1 可改后端 filter | 🟡 |
| AI 调用方填同 code 引发 409 风暴 | 唯一性是 service 级（无 DB UNIQUE） | 不限速 v0；调用方应 timestamp 前缀 | 🟡 |

## 五、建议补充顺序

1. **第一优先**（部署前必补 — P0 全部）：
   - Backend: TC-TSK-001..015 (15) + TC-TSK-CONS-001/002/003 (3) + TC-PERF-TSK-001 (1) + TC-PRJ-DEL-TSK-001/002 (2) = 21
   - Frontend: TC-FES-TSK-001/002/003 (3)
   - 总计 **24 个 P0 TCs**
2. **第二优先**（P1）：无（本变更未引入 P1 边界）
3. **第三优先**（P2 / v0.0.12+）：
   - SprintsPage / StoriesPage Task drilldown
   - Task 评论 / 活动流
   - Task 估算字段
   - 后端 /api/sprints?projectId=X filter（避免客户端 filter）
