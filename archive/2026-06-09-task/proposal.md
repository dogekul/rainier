# v0.0.11-task — introduce Task entity as Project execution unit

> Baseline: commit `901eea7` / tag `v0.0.10.1-cleanup` (2026-06-09).
> Project → Requirement → Sprint → Story → **Task** 五层金字塔末端落地。

## Why

工作分解金字塔 Project → Requirement → Sprint → Story 已经完整，但缺最后一公里 —— **真正落到人手上的可执行待办**。Story 是 PO 拆出来的需求纵切；Task 是任何人（用户或未来的 AI agent）都可以创建的执行单元，可独立存在（ad-hoc todo），也可挂 Sprint 或 Story。

引入 Task 实体后：
- PM 角色拿到完整的 "目标 → 计划 → 工作 → 执行" 链路
- 为后续 AI 助理（`archive/B-驱动飞轮.md` 「智能派发」「智能填表」）留出接入点 —— 系统 / Agent 可作为 Task 的 `createBy`
- 用户使用本系统不再需要外挂 Trello / Todoist 之类工具

## What Changes

### A. 新实体（A1-A3）

- **A1.** NEW 表 `rainier_task`：12 业务字段 + 6 审计 + `del_flag` (`@SQLDelete`)
  - `code` NN, `title` NN, `description`, `status` NN, `priority` NN
  - `project_id` NN FK / `sprint_id` 可空 / `story_id` 可空 / `assignee_user_id` 可空
  - `due_date` / `close_reason`
- **A2.** NEW `com.rainier.task.{domain,dto,repository,service,controller}`
  - `TaskStatus` 5 项：`TODO` / `IN_PROGRESS` / `DONE` / `BLOCKED` / `CANCELLED`
  - 5 endpoint CRUD（POST / GET / list / PUT / DELETE）
  - 业务校验：
    - `projectId` 必须 live
    - `sprintId` 非空 → live + `sprint.requirement.projectId == task.projectId`（跨层一致性）
    - `storyId` 非空 → live + `story.sprint.requirement.projectId == task.projectId`
    - `sprintId` 与 `storyId` 都非空时 → `story.sprintId == task.sprintId`
    - `assigneeUserId` 可空；非空时校验 user live
  - enrich 二段+：`projectName/Code`、`sprintCode/Name`、`storyCode/Title`、`assigneeName/LoginName`。沿用 v0.0.10.1 list batch + findById 单查模式。
- **A3.** Project FK 保护：删除 Project 若有未软删 Task → 409 `"project has linked tasks"`。家族 chain：Requirement / UserRole / Task。

### B. 前端（B1-B3）

- **B1.** NEW `api/task.ts`（Task / TaskCreate / TaskUpdate / TaskListParams + 5 fn）
- **B2.** NEW `/pm/tasks` → `TasksPage`（list + filter `projectId / status / priority / assigneeUserId / sprintId / storyId` + paginate）+ `TaskEditDrawer`：
  - Project 必选下拉
  - Sprint 可选下拉（按选中 project 过滤显示）
  - Story 可选下拉（按选中 project 过滤显示）
  - Assignee 可选下拉（含「待分配」空选项）
  - Status / Priority / dueDate / title / code / description 输入
- **B3.** Sider「需求管理」追加「任务」一项 → 6 项：
  `项目 / Sprint / 任务 / 诉求 / 需求 / 诉求-需求关联`
  「任务」排第三：项目 → Sprint → 任务，与「执行靠近」语义一致。

### 显式排除

- Task 评论 / 活动流（v0.0.12+）
- 工时估算 / story points（v0.0.12+ 视实际使用情况再加）
- Task 父子嵌套（子任务）
- Task 与 Demand 直接关联（通过 Requirement 间接表达）
- AI 自动创建路径的具体实现（v0.0.11 只保留「系统可创建」 — `createBy` 字段使用 AuditorAware 默认机制即可）
- 已有 v0.0.10 数据迁移（Task 是新实体，无遗留）
- SprintsPage / StoriesPage 内嵌 TaskListPanel drilldown（推后，避免一次性扩散）

## Capabilities

### Modified Capabilities

- `entity-project` — DELETE FK 检查链追加 Task 引用（home of "project has linked tasks" rule）
- `frontend-scaffold` — Sider 5 项 → 6 项 + `/pm/tasks` 路由 + `TasksPage`

### New Capabilities

- `entity-task` — NEW capability with ~5 Requirements

## Impact

**代码层面（~14 文件新增 + ~3 文件修改）**：

- backend NEW (10): `com.rainier.task.{domain/Task.java, domain/TaskStatus.java, dto/TaskCreateRequest.java, dto/TaskUpdateRequest.java, dto/TaskDetail.java, repository/TaskRepository.java, service/TaskService.java, controller/TaskController.java}`
- backend MOD (1): `ProjectService.delete` 加 `taskRepo.countByProjectId` 检查
- backend tests NEW (3): `TaskControllerCreateTest / QueryTest / DeleteTest`（~22 TCs P0）
- backend tests MOD (1): `ProjectControllerDeleteTest` 加 Task FK Scenario
- frontend NEW (4): `api/task.ts / pages/Task/TasksPage.tsx / TaskEditDrawer.tsx / TasksPage.test.tsx`
- frontend MOD (2): `AppLayout.tsx` (Sider 6 项) / `AppRoutes.tsx` (`/pm/tasks`)
- canonical spec NEW (1): `specs/entity-task/spec.md`
- canonical spec MOD (2): `specs/entity-project/spec.md` + `frontend-scaffold/spec.md`

**配置层面**：

- 无 `application.yml` / `docker-compose.yml` / `.env` 变更

**基础设施**：

- 无新服务 / 新 API 中间件
- DB DDL：`ddl-auto=update` 自动建 `rainier_task` 表（首次启动）
- 不需启动迁移 runner（Task 是新实体无遗留数据）

## Success Criteria

- [ ] `mvn test` 全绿（≥ 191 baseline + ~22 新 = **≥ 213 backend**）
- [ ] `npm test` 全绿（≥ 41 baseline + ~3 新 = **≥ 44 frontend**）
- [ ] `npm run build` 成功
- [ ] `docker compose up` 后：
  - `SHOW TABLES;` = **13**（含 `rainier_task`）
  - `DESCRIBE rainier_task` 列字段集匹配设计
- [ ] curl E2E:
  - 建 Task w/ `projectId+sprintId+storyId` 三层一致 → 201 + 全富化
  - 建 Task w/ `sprintId` 不属 task.projectId → 400 `"sprint not in project"`
  - 建 Task w/ `storyId` 跨 project → 400 `"story not in project"`
  - 建 Task w/ `sprintId+storyId` 不一致 → 400 `"story not in sprint"`
  - 建 Task w/ `assigneeUserId=null` → 201 (允许 unassigned)
  - 删 Project w/ Task → 409 `"project has linked tasks"`
- [ ] 前端 `/pm/tasks` 可建 / 编辑 / 删 Task
- [ ] Sider 第三项是「任务」，点击跳 `/pm/tasks`
- [ ] enrich SQL count 在 size=20 list 锁死 = 6（2 page + 4 batch enrich：user / sprint / story / project — Requirement 不出现在 TaskDetail 字段，无需 batch；revised from initial estimate 7 per PA-1）
- [ ] 不 push 到 remote（待 user 后续决定）
