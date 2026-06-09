# v0.0.11-task 实现任务清单

## M01 — TaskStatus + Task entity + TaskRepository

- [ ] 1.1 `backend/src/main/java/com/rainier/task/domain/TaskStatus.java` — 5 constants + ALL set (`unmodifiableSet(new HashSet<>(Arrays.asList(...)))` Java 8 模式)
- [ ] 1.2 `backend/src/main/java/com/rainier/task/domain/Task.java` — 12 业务字段 + `@SQLDelete` + `@Where(del_flag=0)` + `@Table(name="rainier_task")`
- [ ] 1.3 `backend/src/main/java/com/rainier/task/repository/TaskRepository.java` — extends JpaRepository + JpaSpecificationExecutor；method: `existsByCode`, `countByProjectId`
- [ ] 1.4 `mvn -q compile` 通过

## M02 — DTOs

- [ ] 2.1 `dto/TaskCreateRequest.java` — `@NotBlank` code/title + `@NotNull` projectId + optional sprintId/storyId/assigneeUserId/status/priority/description/dueDate
- [ ] 2.2 `dto/TaskUpdateRequest.java` — code/title/description/status/priority/assigneeUserId/dueDate/closeReason；**不**含 projectId/sprintId/storyId
- [ ] 2.3 `dto/TaskDetail.java` — 24 字段 + `from(Task)` static factory + setter for enrichment (projectName/Code, sprintCode/Name, storyCode/Title, assigneeName/LoginName)
- [ ] 2.4 `mvn -q compile` 通过

## M03 — TaskService.create + 跨层一致性

- [ ] 3.1 `service/TaskService.java` 框架：注入 5 repo (taskRepo/projectRepo/sprintRepo/storyRepo/requirementRepo/userRepo)；`@Service` + `@Transactional(readOnly=true)`；私有 `enrich(Task)` placeholder
- [ ] 3.2 实现 `create(TaskCreateRequest req)`：
  1. projectRepo.existsById → 400 "project not found"
  2. sprintId 非空 → sprintRepo.findById → 400 "sprint not found"；继续校验
  3. storyId 非空 → storyRepo.findById → 400 "story not found"；继续校验
  4. assigneeUserId 非空 → userRepo.existsById → 400 "assignee user not found"
  5. 跨层一致性 guard 3 条（design.md Decision 2 算法）
  6. existsByCode → 409 "code already exists"
  7. status/priority 校验
  8. `repo.saveAndFlush + enrich`
- [ ] 3.3 跨层一致性 3 guard 实现：
  - `sprint.requirementId → req.projectId == task.projectId` else 400 "sprint not in project"
  - `story.projectId == task.projectId` else 400 "story not in project"
  - `sprintId && storyId 都 set → story.sprintId == task.sprintId` else 400 "story not in sprint"
- [ ] 3.4 `mvn -q compile` 通过；M03 单元测试在 M08 写

## M04 — TaskService.findById + 单行 enrich

- [ ] 4.1 `findById(Long id)` → `enrich(getOrThrow(id))`；`getOrThrow` 抛 404 "task not found"
- [ ] 4.2 private `enrich(Task t)` 单行 5-stage join：
  - userRepo.findById(assigneeUserId) → assigneeName/assigneeLoginName（null-safe）
  - sprintRepo.findById(sprintId) → sprintCode/sprintName（null-safe）
  - storyRepo.findById(storyId) → storyCode/storyTitle（null-safe）
  - projectRepo.findById(projectId) → projectName/projectCode（projectId NN，期望存在）
  - 注意：requirement 不直接出现在 TaskDetail 字段（sprintName/storyTitle 已足够）；故只 5 个 repo 不 6 个

## M05 — TaskService.list batch enrich

- [ ] 5.1 `list(projectId, sprintId, storyId, status, priority, assigneeUserId, PageParams)` 用 Specification 多条件 AND + search across code/title
- [ ] 5.2 sort DESC by createTime
- [ ] 5.3 batch enrich：5 个 `findAllById(setOf(ids))` + Map 拼接 + `.filter(Objects::nonNull)` 防 null FK（v0.0.10.1 Code-M2 防御）
- [ ] 5.4 empty result early return
- [ ] 5.5 `private TaskDetail enrichBatch(Task, Map<Long,User>, Map<Long,Sprint>, Map<Long,Story>, Map<Long,Project>)`
- [ ] 5.6 注意：list 暴露的 `assigneeName / sprintCode / storyTitle` 来自 v0.0.10 Sprint/Story 的二段继承字段（Sprint/Story DTO 已有 projectId — 前端 filter 用）

## M06 — TaskService.update + delete

- [ ] 6.1 `update(Long id, TaskUpdateRequest req)`：
  - getOrThrow
  - status/priority 校验
  - assigneeUserId 非空时 existsById → 400；可改为 null
  - code 变更 → existsByCode 重检
  - 其余字段直接 set
  - projectId/sprintId/storyId 不动
  - `enrich(saveAndFlush(s))`
- [ ] 6.2 `delete(Long id)`：getOrThrow → `repo.delete(s)`（@SQLDelete 自动软删）→ 204

## M07 — TaskController

- [ ] 7.1 `controller/TaskController.java` 5 endpoints
- [ ] 7.2 `@RestController @RequestMapping("/api/tasks")`
- [ ] 7.3 POST 返回 201 + Location header `/api/tasks/{id}`
- [ ] 7.4 GET 单 / GET list（query params）/ PUT / DELETE
- [ ] 7.5 `mvn -q compile && mvn -q test -Dtest='!*Task*'` 确认无既有回归

## M08 — TaskControllerCreateTest

- [ ] 8.1 `test/.../task/controller/TaskControllerCreateTest.java`
- [ ] 8.2 fixture helpers: createUser, createProject, createRequirement, createSprint, createStory（沿用 Story/Sprint 模式）
- [ ] 8.3 cleanDb: taskRepo → storyRepo → sprintRepo → requirementRepo → projectRepo → userRepo（dependency 顺序）
- [ ] 8.4 12 tests: TC-TSK-001..009 + TC-TSK-CONS-001..003
- [ ] 8.5 `mvn -q test -Dtest=TaskControllerCreateTest` 全绿

## M09 — TaskControllerQueryTest + UpdateTest + DeleteTest

- [ ] 9.1 `TaskControllerQueryTest.java` — TC-TSK-010（24-field loop assert）+ TC-TSK-011（projectId+status filter）
- [ ] 9.2 `TaskControllerUpdateTest.java` — TC-TSK-012/013/014
- [ ] 9.3 `TaskControllerDeleteTest.java` — TC-TSK-015
- [ ] 9.4 `mvn -q test -Dtest='TaskController*Test'` 全绿

## M10 — TaskListSqlCountTest

- [ ] 10.1 `test/.../task/perf/TaskListSqlCountTest.java`
- [ ] 10.2 `@SpringBootTest(properties="spring.jpa.properties.hibernate.generate_statistics=true")`
- [ ] 10.3 seed: 4 Project / 5 Requirement / 4 Sprint / 4 Story / 5 User / 20 Task
- [ ] 10.4 `stats.clear()` → mockMvc GET /api/tasks?size=20 → `assertEquals(7L, stats.getPrepareStatementCount(), ...)`
- [ ] 10.5 抽样验证 content[0].projectCode / sprintCode（如有）/ storyCode（如有）/ assigneeName（如有）

## M11 — ProjectService FK chain + ProjectControllerDeleteTest

- [ ] 11.1 `ProjectService.java` 注入 TaskRepository
- [ ] 11.2 `delete(Long id)` FK chain 末尾追加：`if (taskRepo.countByProjectId(id) > 0) throw new ConflictException("project has linked tasks");`
- [ ] 11.3 `ProjectControllerDeleteTest.java` 加 fixture（注入 TaskRepository）+ 2 个 test：
  - TC-PRJ-DEL-TSK-001: 单 Task 引用 → 409 "project has linked tasks"
  - TC-PRJ-DEL-TSK-002: Requirement + Task 双引用 → 409 "project has linked requirements"（优先）
- [ ] 11.4 `mvn -q test -Dtest=ProjectControllerDeleteTest` 全绿

## M12 — 前端

- [ ] 12.1 `frontend/src/api/task.ts` — Task / TaskCreate / TaskUpdate / TaskListParams + 5 fn (listTasks/getTask/createTask/updateTask/deleteTask)
- [ ] 12.2 `pages/Task/TaskEditDrawer.tsx`：
  - Project 必选下拉（listProjects size=100）
  - Sprint 可选下拉（listSprints size=100 + 客户端 filter projectId === selected.project）+ 切 project 时清空
  - Story 可选下拉（listStories size=100 + 客户端 filter projectId === selected.project + sprintId === selected.sprint || null）
  - Assignee 可选下拉（listUsers + 含「待分配」空选项）
  - Status / Priority / dueDate / code / title / description 输入
  - 表单错误 (沿用 v0.0.8.1 Code-M7 form-error pattern)
- [ ] 12.3 `pages/Task/TasksPage.tsx`：list + filter projectId/status/priority + 分页 + 新建按钮 + 行编辑/删除
- [ ] 12.4 `pages/Task/index.tsx` re-export
- [ ] 12.5 `components/AppLayout.tsx`：Sider 「需求管理」 5 项 → 6 项；「任务」 第 3 位
- [ ] 12.6 `AppRoutes.tsx`：加 `<Route path="/pm/tasks" element={<TasksPage />} />`
- [ ] 12.7 `pages/Task/TasksPage.test.tsx` — TC-FES-TSK-001 (Sider) + TC-FES-TSK-002 (route)
- [ ] 12.8 `pages/Task/TaskEditDrawer.test.tsx` — TC-FES-TSK-003 (Sprint/Story 联动清空)
- [ ] 12.9 `npx tsc --noEmit && npx vitest run` 全绿
- [ ] 12.10 检查并改 v0.0.10 既有「5 项」断言 → 6 项（陷阱 E）

## M13 — E2E

- [ ] 13.1 `mvn -q package -DskipTests` 出 jar
- [ ] 13.2 `cd frontend && npx vite build`
- [ ] 13.3 `DOCKER_BUILDKIT=0 docker compose build backend frontend` (BuildKit fetch fix v0.0.10.1 教训)
- [ ] 13.4 `docker compose up -d --no-deps --force-recreate backend frontend`
- [ ] 13.5 验证：
  - `docker exec rainier-mysql mysql -uroot -prainier_root rainier -e "SHOW TABLES;"` 含 `rainier_task`（13 张）
  - `DESCRIBE rainier_task` 字段集匹配
  - curl chain（建 Project → Req → Sprint → Story → 建 Task 三层一致 → 200 富化 / 跨 project sprint → 400 / 跨 project story → 400 / sprint+story 不一致 → 400 / unassigned → 201 / 删 Project w/ Task → 409 "project has linked tasks"）
- [ ] 13.6 自动调用 `stdd-verify`
