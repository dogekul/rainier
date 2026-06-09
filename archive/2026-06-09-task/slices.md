# v0.0.11-task 切片执行计划

> 13 切片，全 P0。后端链 M01→M11 + 前端 M12 + E2E M13。

## 切片表

| # | 优先级 | TC 覆盖 | 实现目标 | 依赖 |
|---|--------|---------|----------|------|
| M01 | P0 | — | `TaskStatus.java` (5 constants + ALL set Java 8 兼容) + `Task.java` entity (@SQLDelete + @Where + columns) + `TaskRepository.java` (existsByCode / countByProjectId) | 无 |
| M02 | P0 | — | `TaskCreateRequest.java` (@NotBlank code/title + @NotNull projectId + optional sprintId/storyId/assigneeUserId/dueDate) + `TaskUpdateRequest.java` (无 projectId/sprintId/storyId — 父级 immutable) + `TaskDetail.java` (24 字段 + enrichment setters) | M01 |
| M03 | P0 | TC-TSK-001..009 + TC-TSK-CONS-001..003 (12) | `TaskService.create()`: project/sprint/story/user 存在性校验 + 跨层一致性 3 guard + code 唯一 + status/priority 校验 + 单行 enrich | M02 |
| M04 | P0 | TC-TSK-010 (1) | `TaskService.findById()` + 单行 `enrich(Task)` 5-stage join (user/sprint/story/requirement/project + 二段 join story→sprint→requirement) | M03 (复用 enrich 私有方法) |
| M05 | P0 | TC-TSK-011 (1) | `TaskService.list()` + Specification filter (projectId/sprintId/storyId/status/priority/assigneeUserId/search) + batch enrich (5 findAllById + Map 拼接 + null-filter family pattern) + PageResponse | M04 |
| M06 | P0 | TC-TSK-012..015 (4) | `TaskService.update()` (status/priority/code/title/desc/assignee/dueDate/closeReason 可改；projectId/sprintId/storyId 不接受) + assigneeUserId null 允许 + 唯一性 re-check + `delete()` 软删 | M05 |
| M07 | P0 | — | `TaskController.java` 5 endpoints (POST /api/tasks → 201 + Location; GET /api/tasks/{id} → 200; GET /api/tasks?... → PageResponse; PUT /api/tasks/{id} → 200; DELETE /api/tasks/{id} → 204) | M06 |
| M08 | P0 | TC-TSK-001..009 + TC-TSK-CONS-001..003 (12) | `TaskControllerCreateTest.java` — fixture chain (User/Project/Requirement/Sprint/Story) + 12 tests，含跨层一致性 3 单测 | M07 |
| M09 | P0 | TC-TSK-010/011/012/013/014/015 (6) | `TaskControllerQueryTest.java` (TC-010/011) + `TaskControllerUpdateTest.java` (TC-012/013/014) + `TaskControllerDeleteTest.java` (TC-015)。24-field loop assert on TC-010 (沿用 TC-STR-010 / TC-SPR-009-FULL 写法) | M07 |
| M10 | P0 | TC-PERF-TSK-001 (1) | `TaskListSqlCountTest.java` — `@SpringBootTest(properties="spring.jpa.properties.hibernate.generate_statistics=true")` + seed 20 Task / 4 Project / 5 Req / 4 Sprint / 4 Story / 5 User + `assertEquals(6L, stmtCount, ...)` 锁死（**Phase 4 PA-1 修订自 7** — Requirement 不在 TaskDetail 字段集，无需 batch） | M07 |
| M11 | P0 | TC-PRJ-DEL-TSK-001/002 (2) | `ProjectService.delete()` FK chain 末尾追加 `taskRepo.countByProjectId(id) > 0 → 409 "project has linked tasks"` + 注入 TaskRepository + `ProjectControllerDeleteTest.java` 加 2 个新 Scenario (TC-PRJ-DEL-TSK-001 单 Task / TC-PRJ-DEL-TSK-002 Requirement+Task 双引用优先 Requirement) | M07 |
| M12 | P0 | TC-FES-TSK-001/002/003 (3) | 前端：`api/task.ts` (Task type + 5 fn) + `pages/Task/TasksPage.tsx` (list + filter + 分页 + 行操作) + `TaskEditDrawer.tsx` (Project/Sprint/Story/Assignee 联动级联 + status/priority/dueDate 输入 + form-error 模式) + `pages/Task/index.tsx` + `AppLayout.tsx` 加 "任务" 第 3 位 + `AppRoutes.tsx` 加 /pm/tasks + `TasksPage.test.tsx` + `TaskEditDrawer.test.tsx` | M07 (backend endpoint shape 稳定) |
| M13 | P0 | — | E2E 验证：`mvn -q package -DskipTests` + `npx vite build` + `DOCKER_BUILDKIT=0 docker compose build backend frontend` + `up -d --force-recreate`：(a) `SHOW TABLES` = 13 含 rainier_task (b) `DESCRIBE rainier_task` 字段集 (c) curl: build Project → Req → Sprint → Story → 建 Task 三层一致 → 富化 / 跨 project sprint → 400 / 跨 project story → 400 / sprint+story 不一致 → 400 / assigneeUserId null → 201 / 删 Project w/ Task → 409 | M01-M12 |

## 依赖图

```
M01 → M02 → M03 → M04 → M05 → M06 → M07 ──┬── M08
                                            ├── M09
                                            ├── M10
                                            ├── M11
                                            └── M12 ──── M13
```

## 隐藏陷阱记录

- **陷阱 A — Story.projectId 二段继承在 v0.0.10 已落地**：M03 跨层一致性 guard 直接读 `story.getProjectId()`（不需重新追溯 sprint→requirement），避免重复 query。TC-TSK-CONS-002 setup 时需要 Story 实际通过正常 StoryService.create 流程，而不是直接 entity new — 否则 projectId 字段不会被填。
- **陷阱 B — TaskUpdateRequest 没有 projectId/sprintId/storyId 字段**：Jackson 反序列化对 unknown property 默认会报错（如果 @JsonIgnoreProperties 没配置），但项目使用 Spring Boot 默认 `FAIL_ON_UNKNOWN_PROPERTIES=false`（v0.0.6 已禁用），所以 client 传 projectId 也只是被静默忽略。Update 路径 service 也不读这些字段。M06 实现时核对一遍 application.yml 的 Jackson 配置。
- **陷阱 C — 跨层一致性 message 顺序**：M03 实现时 guard 顺序固定为 sprint check → story check → story-in-sprint check。多 guard 触发时返回先碰到的 message（与 ProjectService.delete FK chain 同款 pattern）。
- **陷阱 D — TC-PERF-TSK-001 等号锁死 = 7**：v0.0.10.1 教训 — Spring Data `findAll(spec, pageable)` 双下发 + 5 batch = 7。如果实际跑出 8（多一个 SELECT），最常见原因是 enrich 内某个 path 漏走 batch Map，回去 per-row findById。
- **陷阱 E — Sider 6 项与既有 v0.0.10 TC-FES-SPR-01 测试断言「5 路由」冲突**：M12 修改 AppLayout 后会破坏 `RequirementsPage.test.tsx` / `AppLayout.test.tsx` 中可能存在的"5 项"断言。需检查并改为 6 项 + 顺序断言"任务" 第 3 位。
- **陷阱 F — TaskEditDrawer 客户端 filter 依赖 v0.0.10 enrich**：sprint/story payload 必须含 projectId（v0.0.10 已实现）。M12 测试 mock listSprints/listStories 返回值时 SprintDetail / StoryDetail 要含 projectId 字段。
- **陷阱 G — Spring Bean 构造**：M11 ProjectService 注入 TaskRepository 时若引发循环依赖（不太可能），可用 `@Lazy`。先尝试普通注入。
