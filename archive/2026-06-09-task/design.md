# v0.0.11-task — Technical Design

> Baseline: commit `901eea7` / tag `v0.0.10.1-cleanup` (2026-06-09).
> Goal: 完成工作分解金字塔末端 — Project 下的执行单元 Task。

## Context

v0.0.10.1 之前已建立 Organization / User / Project / Requirement / Sprint / Story / Demand / DemandRequirement / Position / Role / UserRole 共 11 张业务表。Project → Requirement → Sprint → Story 四层是 PM 视角的「计划-需求-纵切」链路，但缺少最终落到执行人手上的「待办」实体。

Family pattern 已经成熟（v0.0.6 → v0.0.10.1 多轮迭代）：
- `BaseEntity` + 自增 `Long` id + 6 审计 + `del_flag` 软删
- `@SQLDelete` + `@Where(del_flag=0)`，code 服务级唯一（无 DB UNIQUE）
- 状态 `VARCHAR(16)` + Java 8 `Collections.unmodifiableSet(new HashSet<>(Arrays.asList(...)))` ALL set
- service-level FK 校验 + 跨层 join 富化
- Owner 可改（v0.0.8 Decision 6b 同款）
- v0.0.10.1 引入 list batch enrich（`findAllById(setOf(ids))` + `Map` 拼接）+ Hibernate Statistics 锁死预算

Task 与 Story 的差异：
- Story 是 PO 在 Sprint 内拆出来的**需求纵切**（垂直切片）
- Task 是任何人都能创建的**执行单元**：可独立、可挂 Sprint、可挂 Story
- Task **必须**挂在 Project 下（用户明确决定 — 强制 NN FK）
- assignee **可空**（unassigned task 合法，与 Story owner 不同）

## Decisions

### 1. Task 三 FK 形状：project NN + sprint/story 可空

**方案**：
- `project_id BIGINT NOT NULL`（FK 到 rainier_project，service 级校验 live）
- `sprint_id BIGINT NULL`（可空；非空时校验 live + 跨层一致性）
- `story_id BIGINT NULL`（可空；非空时校验 live + 跨层一致性）
- 允许 `sprint_id` 与 `story_id` 同时非空（典型场景：task 属于某 Story，Story 又在某 Sprint 内 — 此时需 `story.sprintId == task.sprintId`）

**为什么**：
- 用户明确表态「Task 核心是挂在项目里的，可以关联到 sprint 或 story 上」
- Project 是稳定的、长期存在的容器；Sprint/Story 是阶段性产物 — 任务挂 Project 才能跨阶段被找回
- 留出 AI agent 在没有具体 Sprint/Story 上下文时（如「整理周报」）也能创建 Task 的空间

**备选方案及排除原因**：
- 备选 A — 只挂 Sprint：需要为 ad-hoc todo 创建「杂篮」Sprint，污染 Sprint 语义
- 备选 B — 互斥（sprint XOR story）：阻断「task 属 Story（自动也属 Sprint）」的自然表达
- 备选 C — 只挂 Story：违反用户"可独立存在"的需求

### 2. 跨层一致性校验算法（核心 service 逻辑）

**方案**：`TaskService.create` 与 `update` 在 FK 存在性校验通过后，执行如下跨层一致性 guard：

```java
// pseudo
if (req.getSprintId() != null) {
    Sprint sp = sprintRepo.findById(req.getSprintId()).orElseThrow(BadRequest);
    Requirement r = requirementRepo.findById(sp.getRequirementId()).orElseThrow(BadRequest);
    if (!Objects.equals(r.getProjectId(), req.getProjectId())) {
        throw new BadRequest("sprint not in project");
    }
}
if (req.getStoryId() != null) {
    Story st = storyRepo.findById(req.getStoryId()).orElseThrow(BadRequest);
    // story.projectId 已经在 v0.0.10 二段继承填好（StoryService.create copy from sprint→req）
    if (!Objects.equals(st.getProjectId(), req.getProjectId())) {
        throw new BadRequest("story not in project");
    }
    if (req.getSprintId() != null && !Objects.equals(st.getSprintId(), req.getSprintId())) {
        throw new BadRequest("story not in sprint");
    }
}
```

**为什么**：
- 防止"挂错项目"的脏数据 — 一旦 `task.projectId=A` 但 `task.sprintId=B`（B 属 project A'），UI 显示会错乱（ProjectsPage 看不到这个 Task，但 Sprint drilldown 又能看到）
- v0.0.10 Story 已经通过 `projectId` 二段继承解决了同类问题；Task 沿用并对 Sprint 也做同款 guard

**备选方案及排除原因**：
- 备选 A — 不校验跨层一致性，由 UI 阻止错误选择：UI 总有 race / 直接调 API 的场景，最终一致性必须 server-side
- 备选 B — 自动继承 projectId（用户填 sprintId 自动推 projectId）：失去用户显式声明意图，调试时不清楚错误在哪层

### 3. 5-state 状态机

**方案**：`TaskStatus = {TODO, IN_PROGRESS, DONE, BLOCKED, CANCELLED}`，VARCHAR(16) 列，service 校验。

```java
public final class TaskStatus {
  public static final String TODO = "TODO";
  public static final String IN_PROGRESS = "IN_PROGRESS";
  public static final String DONE = "DONE";
  public static final String BLOCKED = "BLOCKED";
  public static final String CANCELLED = "CANCELLED";
  public static final Set<String> ALL =
      Collections.unmodifiableSet(new HashSet<>(
          Arrays.asList(TODO, IN_PROGRESS, DONE, BLOCKED, CANCELLED)));
}
```

**为什么**：
- 比 Story 的 6 态（DRAFT/READY/IN_PROGRESS/DONE/BLOCKED/CANCELLED）少一态 — Task 不需要 DRAFT/READY 的双阶段（执行单元一旦建好就是 TODO）
- 与业内 Trello/Jira 简化模板一致，降低用户学习成本

**备选方案及排除原因**：
- 备选 A — 沿用 Story 6 态：复杂度溢出（Task 不需要"草稿→就绪"分离）
- 备选 B — 3 态（TODO/DOING/DONE）：丢失 BLOCKED（"卡住"是真实 PM 信号）和 CANCELLED（避免硬删历史）

### 4. assigneeUserId 可空 + 非空时校验

**方案**：`assignee_user_id BIGINT NULL`，对应 `TaskCreateRequest.assigneeUserId : Long?` 不加 `@NotNull`。
- create / update 时若非空：`userRepo.existsById(assigneeUserId)` 不存在 → 400 "assignee user not found"
- 可改：与 Story owner 同款 sibling — 但允许改成 `null`（unassign）

**为什么**：
- 用户明确「unassigned task 合法」 — AI agent 创建 task 时可能尚未指派
- 与 Story.ownerUserId NN 形成对比 — Story 是设计产物必须有 PO；Task 是执行单元可暂时悬空

**备选方案及排除原因**：
- 备选 A — 必填模仿 Story owner：违反 unassigned 语义
- 备选 B — 必填 + 默认 = 当前登录用户：隐藏意图，AI 创建场景下当前登录无意义

### 5. code 必填 + 用户/系统都传 + service 级唯一

**方案**：`code VARCHAR(64) NOT NULL` + `@NotBlank`；service 内 `taskRepo.existsByCode(code)` 命中 → 409 "code already exists"。AI / 系统创建场景调用方自带 code（如 `"TASK-AUTO-{epoch}"`）。

**为什么**：
- Family pattern 一致（Project / Requirement / Sprint / Story 都 code NN）
- 不自动生成避免 race + 简化测试（用户能预测 code）

**备选方案及排除原因**：
- 备选 A — code 可选 + 后端自动生成：增加 race / 唯一性碰撞处理负担

### 6. enrich 5-stage join + list batch + findById 单查（v0.0.10.1 模式）

**方案**：`TaskService.list` 用 batch enrich：

```java
Map<Long, User>        userMap        = batchById(userRepo.findAllById(userIds), User::getId);
Map<Long, Sprint>      sprintMap      = batchById(sprintRepo.findAllById(sprintIds), Sprint::getId);
Map<Long, Story>       storyMap       = batchById(storyRepo.findAllById(storyIds), Story::getId);
Map<Long, Requirement> requirementMap = batchById(requirementRepo.findAllById(requirementIds), Requirement::getId);
Map<Long, Project>     projectMap     = batchById(projectRepo.findAllById(projectIds), Project::getId);
```

Statistics 总数：`2 page (data + count) + 5 batch = 7`。

`findById` 单查路径走 `enrich(Task)` 一行直查（5 个单独 findById + null-safe）。

**为什么**：
- 每个被 join 的实体集合独立 — Task 既有 Story 又有 Sprint 又有 Project，比 Sprint enrich（3 join）多 1 类
- v0.0.10.1 PA-1 已证明 Spring Data `findAll(spec, pageable)` 双下发
- **Phase 4 实测**：Requirement 不出现在 TaskDetail 字段集，无需 batch，实际是 4-stage 而非 5-stage；预算修订为 6（2 page + 4 batch），见 pending-adjustments.md PA-1

**备选方案及排除原因**：
- 备选 A — 单条 JPQL 多 join：跨 4 个 module 的实体；可读性差且每个新富化字段都要改 query
- 备选 B — Hibernate `@EntityGraph`：要在 entity 上加关联（破坏当前"全 service 级 FK"约定）

### 7. ProjectService.delete FK 链追加 Task 守护

**方案**：在现有 `ProjectService.delete(id)` 的 FK 检查链末尾追加：

```java
if (taskRepo.countByProjectId(id) > 0) {
    throw new ConflictException("project has linked tasks");
}
```

**家族 chain 顺序**（FK 检查由先到后）：
1. `requirementRepo.countByProjectId(id) > 0` → "project has linked requirements"
2. `userRoleRepo.countByProjectId(id) > 0` → "project has linked user roles"
3. **NEW** `taskRepo.countByProjectId(id) > 0` → "project has linked tasks"

**为什么**：
- 末尾追加最小破坏既有测试 — 多重引用时 message 仍旧报最先碰到的（既有 Requirement 引用优先）
- 沿用 v0.0.10 Sprint FK 末尾追加的模式（Code-M3 加固后顺序确定）

**备选方案及排除原因**：
- 备选 A — Task 在最前：会破坏 v0.0.8 既有 TC-PRJ-DEL-001/002 的报错消息断言
- 备选 B — 不加 FK 保护：Project 删除后 Task 变成孤儿（projectId 指向 ghost）— 与 v0.0.8 引入 dangling-fk 防御不一致

### 8. Sider「任务」排第 3 位

**方案**：Sider「需求管理」组扩到 **6 项**，顺序：

```
项目 → Sprint → 任务 → 诉求 → 需求 → 诉求-需求关联
```

「任务」位于「Sprint」之后、「诉求」之前。

**为什么**：
- 三层是 "PM 视角" 区域（容器 → 阶段 → 执行）：项目 / Sprint / 任务
- 后三层是 "BA 视角" 区域（流转）：诉求 / 需求 / 关联
- 用户日常进入系统优先做 "执行" 操作 — 把任务靠前更接近"打开就用"的 UX

**备选方案及排除原因**：
- 备选 A — 末尾追加（第 6 位）：执行类用户每次都要往下翻
- 备选 B — 顶级独立菜单组「执行管理」：v0 数据量不够，建独立组语义浪费

### 9. TaskEditDrawer Sprint/Story 联动 select（前端）

**方案**：
- Project 必选下拉（已加载所有 Projects 一次）
- Sprint 下拉：选定 Project 后用 `listSprints({size: 100})` 拉全量再客户端 `filter(s => s.projectId === selectedProject.id)`（SprintDetail 已含 projectId 富化字段，v0.0.10 enrich）
- Story 下拉：选定 Project 后用 `listStories({size: 100})` 同样客户端 filter（StoryDetail.projectId 已富化）
- Project 切换时清空 Sprint/Story 选择
- Sprint 选定后 Story 选项进一步过滤为 `filter(s => s.sprintId === selectedSprintId)`（StoryDetail.sprintId 已富化）
- Sprint/Story 都允许保留 "未选" 空选项（产生 null 提交）

**为什么**：
- 后端**不**需新增 `?projectId=X` query — 复用现有 `?size=N` + 客户端 filter，简单且 v0 size cap=100 足够
- v0.0.10 enrich 字段已经把 `projectId` / `sprintId` 暴露到 list payload，无新 endpoint

**备选方案及排除原因**：
- 备选 A — 后端加 `/api/sprints?projectId=X`：要加 join + spec，工作量翻倍
- 备选 B — 不联动（用户任意选）：破坏跨层一致性，最终 server 报 400 体验差

### 10. perf 预算锁定 = 6（list size=20；Phase 4 PA-1 修订自 7）

**方案**：`TaskListSqlCountTest` 用 Hibernate Statistics + `@SpringBootTest(properties="spring.jpa.properties.hibernate.generate_statistics=true")` 锁死：

```java
assertEquals(6L, stats.getPrepareStatementCount(), ...);
```

Breakdown: 1 page-data + 1 page-count + 4 batch enrich (user/sprint/story/project) = 6。Requirement 不在 TaskDetail 字段集中，无需 batch。

**为什么**：
- v0.0.10.1 已建立 perf 测试家族 pattern，每个 list endpoint 都该有 SQL count guard
- 等号锁死防止 both 上溢（N+1 回归）和下溢（意外重构导致少 join）

**备选方案及排除原因**：
- 备选 A — `≤ 8` 不锁死：v0.0.10.1 PA-1 教训表明等号断言更有用

### 11. update 路径：projectId / sprintId / storyId 创建后不可改（immutable）

**方案**：`TaskUpdateRequest` **不**含 `projectId / sprintId / storyId` 字段。可改字段：`code / title / description / status / priority / assigneeUserId / dueDate / closeReason`。

**为什么**：
- 父级关系变化在业务上 = 删除重建（"把这个 task 从 Sprint A 转到 Sprint B" 应该是新建一个 Task，旧的 CANCELLED 软删）
- 与 Story 同款（Story 也是 sprintId 创建后不可改）
- 简化跨层一致性 — 只在 create 时校验一次

**备选方案及排除原因**：
- 备选 A — 允许改：要在 update 路径重跑全部跨层一致性 guard，且违反"父级是身份"的语义

### 12. 显式不做的事项

清单（与 proposal 一致）：
- Task 评论 / 活动流
- 工时估算 / story points / 复杂度
- Task 父子嵌套（subtask tree）
- Task ↔ Demand 直接关联（通过 Requirement 间接）
- AI 自动创建 endpoint（v0.0.11 只保留通用 POST，AI 调用方负责 code 生成）
- 数据迁移（Task 是新实体）
- SprintsPage / StoriesPage 内嵌 TaskListPanel drilldown

## Architecture

### Task 创建数据流

```
POST /api/tasks
  body { code, title, projectId, sprintId?, storyId?, assigneeUserId?, ... }
       │
       ▼
  TaskService.create:
       │
       ├─ projectRepo.existsById(projectId)             ─ 400 if missing
       ├─ if (sprintId)  sprintRepo.findById   ──┐      ─ 400 if missing
       ├─ if (storyId)   storyRepo.findById    ──┤      ─ 400 if missing
       ├─ if (assigneeUserId) userRepo.existsById        ─ 400 if missing
       │                                          │
       ├─ 跨层一致性 guard (Decision 2):           │
       │   sprint.req.proj  == task.proj  ?      │       ─ else 400 sprint not in project
       │   story.proj       == task.proj  ?      │       ─ else 400 story not in project
       │   story.sprintId   == task.sprintId ?  │       ─ else 400 story not in sprint
       │                                          │
       ├─ repo.existsByCode(code)                        ─ 409 if dup
       ├─ status / priority valid?                       ─ 400 if invalid
       │
       ▼
  enrich(saveAndFlush(task))  ── single-row enrich
       │
       ▼
  HTTP 201 Location: /api/tasks/{id} + TaskDetail body
```

### list batch enrich (Decision 6)

```
GET /api/tasks?projectId=&sprintId=&storyId=&status=&priority=&assigneeUserId=&search=&page=&size=
       │
       ▼
  Spring Data repo.findAll(spec, pageRequest)
       │   ← 1 SELECT (data) + 1 SELECT (count)
       ▼
  extract ids: userIds, sprintIds, storyIds, projectIds, requirementIds
       │
       ▼
  batch fetch (5 SELECTs):
    userRepo.findAllById   → Map<Long, User>
    sprintRepo.findAllById → Map<Long, Sprint>
    storyRepo.findAllById  → Map<Long, Story>
    requirementRepo.findAllById  → Map<Long, Requirement>
    projectRepo.findAllById      → Map<Long, Project>
       │
       ▼
  stream.map(t -> enrichBatch(t, maps)) → List<TaskDetail>
       │
       ▼  Total: 2 + 5 = 7 statements
  PageResponse<TaskDetail>
```

### Sider menu 演进

```
v0.0.6: 诉求 / 需求 / 关联                        (3)
v0.0.8: 项目 / 诉求 / 需求 / 关联                  (4)
v0.0.10: 项目 / Sprint / 诉求 / 需求 / 关联         (5)
v0.0.11: 项目 / Sprint / 任务 / 诉求 / 需求 / 关联  (6)  ← THIS CHANGE
```

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|----------|
| 跨层一致性 guard 漏 / 错 — task 数据可能跨项目 / 跨 sprint 错配 | TC-TSK-CONS-001/002/003 三条单测，对应每一条 guard。v0.0.12 可加 `DanglingTaskFkCleanup` runner 兜底脏数据。 |
| 前端客户端 filter sprint/story 列表，size 超 100 时不全 | v0 单 project 下 sprint 数远小于 100；TasksPage 在 TaskEditDrawer 打开时拉全量。v0.1 可改为 `?projectId=X` 后端 filter。 |
| ProjectService.delete FK 链顺序破坏既有 TC-PRJ 报错断言 | Decision 7 明确"末尾追加"，对应 TC-PRJ-DEL-001/002 不动；新加 TC-PRJ-DEL-TSK 只覆盖 task-only case + task+other 混合 case（既有 requirement 优先报）。 |
| AI agent 通过 POST /api/tasks 注入大量 task → 触发 `code already exists` 大量 409 | v0 不限速；v0.1 引入 rate limit。code 唯一性是软删跨范围（与 family pattern 一致），AI 调用方应使用 timestamp 前缀。 |
| 5-stage enrich 加 Hibernate Statistics 测试在 CI 上偶发受其他 session 干扰 | v0.0.10.1 已验证 `stats.clear()` 后 `getPrepareStatementCount` 隔离稳定；沿用同款 `@SpringBootTest(properties=...)` 启用 statistics。 |
| Task.projectId 与 Story.projectId 重复存储 — 数据冗余 | 二段继承在 v0.0.10 已经引入此 trade-off；查询性能 > 范式化。projectId 是创建-only immutable，无更新一致性风险。 |
| Sider 6 项是否拥挤 | 设计 token 已支持垂直排列，6 项在中等屏幕上不溢出；v0.1 可考虑分组折叠。 |
