# Capability: entity-task

> NEW capability from v0.0.11-task (2026-06-09).
> Task 是 Project 下的执行单元（execution unit），与 Story（design unit）正交。
> 强制挂 Project（NN FK）；可选挂 Sprint / Story；assignee 可空 — 允许 unassigned。
> 跨层一致性：sprint/story 若 set，其归属链必须收敛到 task.projectId；sprint+story 都 set 时 story.sprintId == task.sprintId。
> 5-state machine: TODO / IN_PROGRESS / DONE / BLOCKED / CANCELLED。
> code service-级唯一（family pattern）；软删 via `@SQLDelete + del_flag`。

## ADDED Requirements

### Requirement: 创建 Task

后端 SHALL 通过 `POST /api/tasks` 接受 `code` + `title` + `projectId`（必填），其余字段使用默认值或留空；持久化并返回 201。

#### Scenario: 最小 payload 创建 Task + 默认值 + 富化

- **GIVEN** 数据库存在 Project id=1（code="PROJ-1"，name="Apollo"）/ User id=1（loginName="alice"，name="Alice"）
- **WHEN** 客户端 `POST /api/tasks` body `{"code":"TASK-001","title":"修登录页 bug","projectId":1}`
- **THEN** 系统 SHALL 返回 HTTP 201
- **AND** body 默认值 SHALL 为 `status="TODO"` / `priority="MEDIUM"` / `sprintId=null` / `storyId=null` / `assigneeUserId=null`
- **AND** body SHALL 富化 `projectName="Apollo"` / `projectCode="PROJ-1"`；assignee 相关字段 SHALL 为 null
- **AND** `Location` header SHALL 形如 `/api/tasks/\d+`

#### Scenario: 完整 payload 创建 Task w/ Sprint + Story + Assignee

- **GIVEN** Project id=1 / Requirement id=10 (projectId=1) / Sprint id=20 (requirementId=10) / Story id=30 (sprintId=20, projectId=1) / User id=2 (loginName="lili", name="黎立")
- **WHEN** `POST /api/tasks` body `{"code":"TASK-002","title":"实现登录 API","projectId":1,"sprintId":20,"storyId":30,"assigneeUserId":2,"status":"IN_PROGRESS","priority":"HIGH","dueDate":"2026-07-15"}`
- **THEN** 系统 SHALL 返回 HTTP 201
- **AND** body SHALL 富化 `sprintCode` / `sprintName` / `storyCode` / `storyTitle` / `assigneeName="黎立"` / `assigneeLoginName="lili"` / `projectName` / `projectCode`
- **AND** body.dueDate SHALL 为 `"2026-07-15"`

#### Scenario: code 重复被拒（service 级唯一）

- **GIVEN** 数据库已存在 `code="TASK-DUP"` Task
- **WHEN** 再 `POST /api/tasks` 同 `code="TASK-DUP"`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "code already exists"

#### Scenario: projectId 不存在被拒

- **GIVEN** 数据库无 Project id=999
- **WHEN** `POST /api/tasks` body 含 `projectId=999`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "project not found"

#### Scenario: assigneeUserId 不存在被拒

- **GIVEN** 数据库无 User id=999_999；Project id=1 存在
- **WHEN** `POST /api/tasks` body 含 `assigneeUserId=999999`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "assignee user not found"

#### Scenario: 非法 status 被拒

- **GIVEN** 后端已启动；Project id=1 / User id=1 存在
- **WHEN** `POST /api/tasks` body 含 `status="UNKNOWN"`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "invalid status"

#### Scenario: 非法 priority 被拒

- **GIVEN** Project id=1 存在
- **WHEN** `POST /api/tasks` body 含 `priority="EXTREME"`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "invalid priority"

#### Scenario: 缺必填字段被拒

- **GIVEN** 后端已启动
- **WHEN** `POST /api/tasks` body `{"code":"TASK-X"}`（缺 title / projectId）
- **THEN** SHALL 返回 400
- **AND** body.fieldErrors[*].field SHALL 同时含 `"title"` / `"projectId"`

#### Scenario: createBy 自动注入登录 username

- **GIVEN** JWT 当前 username="alice"；Project id=1 存在
- **WHEN** `POST /api/tasks` body 创建 Task
- **THEN** SHALL 返回 201
- **AND** body.createBy SHALL 为 "alice"（由 AuditorAwareImpl 自动注入）

### Requirement: 跨层一致性守护（v0.0.11 核心约束）

后端 SHALL 在 `POST /api/tasks` 校验 `sprintId / storyId / projectId` 跨层一致性：
- 若 `sprintId` 非空：sprint → Requirement → projectId 必须等于 `task.projectId`，否则 400 "sprint not in project"
- 若 `storyId` 非空：story.projectId（v0.0.10 二段继承字段）必须等于 `task.projectId`，否则 400 "story not in project"
- 若 `sprintId` 与 `storyId` 都非空：`story.sprintId` 必须等于 `task.sprintId`，否则 400 "story not in sprint"

#### Scenario: sprint 跨 project 被拒

- **GIVEN** Project id=1 / Project id=2 / Requirement id=10 (projectId=2) / Sprint id=20 (requirementId=10)
- **WHEN** `POST /api/tasks` body `{"code":"TASK-X","title":"x","projectId":1,"sprintId":20}`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "sprint not in project"

#### Scenario: story 跨 project 被拒

- **GIVEN** Project id=1 / Project id=2 / Requirement id=10 (projectId=2) / Sprint id=20 (requirementId=10) / Story id=30 (sprintId=20, projectId=2)
- **WHEN** `POST /api/tasks` body `{"code":"TASK-Y","title":"x","projectId":1,"storyId":30}`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "story not in project"

#### Scenario: story+sprint 都 set 但 story 不属该 sprint 被拒

- **GIVEN** Project id=1 / Requirement id=10 (projectId=1) / Sprint id=20 + Sprint id=21（都属 Req 10）/ Story id=30 (sprintId=21, projectId=1)
- **WHEN** `POST /api/tasks` body `{"code":"TASK-Z","title":"x","projectId":1,"sprintId":20,"storyId":30}`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "story not in sprint"

### Requirement: 查询 Task

后端 SHALL 通过 `GET /api/tasks/{id}` 返回单 Task 详情（含富化）；通过 `GET /api/tasks?projectId=&sprintId=&storyId=&status=&priority=&assigneeUserId=&search=&page=&size=` 返回 PageResponse。

#### Scenario: GET 详情完整字段集 + 富化

- **GIVEN** Task id=1 关联 Project 1（"Apollo"，code="PROJ-1"）/ Sprint 20 / Story 30 / Assignee User 1（"Alice"）
- **WHEN** `GET /api/tasks/1`
- **THEN** SHALL 返回 200
- **AND** body 字段集 SHALL 等于 `[id, code, title, description, status, priority, projectId, projectName, projectCode, sprintId, sprintCode, sprintName, storyId, storyCode, storyTitle, assigneeUserId, assigneeName, assigneeLoginName, dueDate, closeReason, createTime, updateTime, createBy, updateBy]`
- **AND** body.projectName SHALL 为 "Apollo"

#### Scenario: 按 projectId + status 联合过滤列表

- **GIVEN** Project A 下 3 个 Task (TODO / IN_PROGRESS / DONE)，Project B 下 1 个 Task (TODO)
- **WHEN** `GET /api/tasks?projectId={A.id}&status=TODO`
- **THEN** body.total SHALL 为 1
- **AND** body.content[0].projectId SHALL 等于 A.id
- **AND** body.content[0].status SHALL 为 "TODO"

### Requirement: 更新 Task（assignee/status/priority 可改；父级 immutable）

后端 SHALL 通过 `PUT /api/tasks/{id}` 修改 `code` / `title` / `description` / `status` / `priority` / `assigneeUserId` / `dueDate` / `closeReason`；`projectId / sprintId / storyId` 创建后不可改（请求体不接受）。code 变更须重检唯一性；新 `assigneeUserId` 非空时必须存在。

#### Scenario: 更新 status + assigneeUserId 转移

- **GIVEN** Task id=1，status="TODO"，assigneeUserId=null；User id=2 存在
- **WHEN** `PUT /api/tasks/1` body `{"code":"TASK-001","title":"x","status":"IN_PROGRESS","priority":"HIGH","assigneeUserId":2}`
- **THEN** SHALL 返回 200
- **AND** body.status SHALL 为 "IN_PROGRESS"
- **AND** body.assigneeUserId SHALL 为 2
- **AND** body.assigneeName SHALL 富化为新 user 的 name（跟随）

#### Scenario: 改 assigneeUserId 至 null（unassign）

- **GIVEN** Task id=1 assigneeUserId=2
- **WHEN** `PUT /api/tasks/1` body 含 `assigneeUserId=null`
- **THEN** SHALL 返回 200
- **AND** body.assigneeUserId SHALL 为 null
- **AND** body.assigneeName / assigneeLoginName SHALL 为 null

#### Scenario: PUT 新 assigneeUserId 不存在被拒

- **GIVEN** Task id=1 存在；User id=999_999 不存在
- **WHEN** `PUT /api/tasks/1` body 含 `assigneeUserId=999999`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "assignee user not found"

### Requirement: 软删 Task

后端 SHALL 通过 `DELETE /api/tasks/{id}` 标记 `del_flag=1`，无下游 FK 保护（Task 是叶子实体）。

#### Scenario: 软删成功 + 后续 GET 404

- **GIVEN** Task id=1 存在
- **WHEN** `DELETE /api/tasks/1`
- **THEN** SHALL 返回 204
- **AND** 后续 `GET /api/tasks/1` SHALL 返回 404

### Requirement: list enrich SQL count 锁死 = 6（v0.0.11 perf，PA-1 修订自 7）

后端在 `TaskService.list` 的 enrich 路径上 SHALL 对 User / Sprint / Story / Project 四种 join 实体各执行一次 `findAllById(setOf(ids))` 后用 `Map<Long, Entity>` 拼接。Requirement 不出现在 TaskDetail 字段集中，因此**不**做 batch。期望 list size=20 的 enrich 阶段 PreparedStatement 计数 = 6（2 page + 4 batch）。

#### Scenario: GET /api/tasks?size=20 在 enrich 阶段 = 6 个 SELECT

- **GIVEN** 数据库已 seed 20 个 Task（跨 4 Project / 5 Requirement / 4 Sprint / 4 Story / 5 User）
- **AND** Hibernate Statistics 已 enabled 且 `stats.clear()` 已在 list 调用前执行
- **WHEN** 客户端调用 `GET /api/tasks?page=0&size=20`
- **THEN** 系统 SHALL 返回 HTTP 200 + 20 行 Task 富化结果（projectName/Code、sprintCode/Name（如有）、storyCode/Title（如有）、assigneeName/LoginName（如有）正确）
- **AND** `Statistics.getPrepareStatementCount()` SHALL 等于 6 — 1 page-data + 1 page-count + 4 batch enrich (user/sprint/story/project)
