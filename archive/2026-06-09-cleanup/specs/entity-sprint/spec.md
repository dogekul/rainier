# Capability: entity-sprint

## MODIFIED Requirements

### Requirement: `SprintService.list` enrich + storyCount 批量化（v0.0.10.1 性能）

后端在 `SprintService.list` 的 enrich 路径上 SHALL 对 User / Requirement / Project 三种 join 实体各执行一次 `findAllById(setOf(ids))` 后用 `Map<Long, Entity>` 拼接；SHALL 把每行的 `storyCount` 富化替换为**一次** native SQL `SELECT sprint_id, COUNT(*) FROM rainier_story WHERE del_flag = 0 AND sprint_id IN (?) GROUP BY sprint_id` 聚合查询后用 `Map<Long, Long>` 查表。期望 list size=20 的 enrich 阶段 PreparedStatement 计数 = 6（v0.0.10 实测后修正自 Phase 2 估计的 5；详见 pending-adjustments.md PA-1）。

#### Scenario: `GET /api/sprints?size=20` 在 enrich 阶段 = 6 个 SELECT

- **GIVEN** 数据库已 seed 20 个 Sprint（关联 5 个 Requirement、2 个 Project、3 个不同 User，每个 Sprint 含若干 Story）
- **AND** Hibernate Statistics 已 enabled 且 `stats.clear()` 已在 list 调用前执行
- **WHEN** 客户端调用 `GET /api/sprints?page=0&size=20`
- **THEN** 系统 SHALL 返回 HTTP 200 + 20 行 Sprint 富化结果（requirementCode / requirementTitle / projectName / projectCode / ownerName / ownerLoginName / storyCount 全部正确）
- **AND** `Statistics.getPrepareStatementCount()` 的增量 SHALL 等于 6 — 1 page-data + 1 page-count + 3 batch enrich queries (user/req/project) + 1 storyCount aggregate
- **AND** 每行 `storyCount` SHALL 等于该 Sprint 下 `del_flag=0` 的 Story 行数（含所有状态）

### Requirement: SprintDetail GET 响应字段集守护（v0.0.10.1 测试加固）

后端的 `SprintDetail` 响应体 SHALL 在 `GET /api/sprints/{id}` 上含且仅含以下 22 个字段：`id` / `code` / `name` / `description` / `goal` / `status` / `requirementId` / `requirementCode` / `requirementTitle` / `projectId` / `projectName` / `projectCode` / `ownerUserId` / `ownerName` / `ownerLoginName` / `startDate` / `endDate` / `storyCount` / `createTime` / `updateTime` / `createBy` / `updateBy`。同时 `POST /api/sprints` 响应体 SHALL echo 回 `id` / `code` / `requirementId` / `ownerUserId` 用于前端 fence 防止字段悄悄缩水。

#### Scenario: GET 详情 22 字段全有（loop assert）

- **GIVEN** Sprint id=1 存在，关联 Requirement id=1（code=REQ-1, title="登录流程"）/ Project id=1 / User id=1
- **WHEN** 客户端 `GET /api/sprints/1`
- **THEN** 系统 SHALL 返回 HTTP 200
- **AND** 响应体 SHALL 含且仅含以下字段（逐项断言 `body.has(field)`）：`id, code, name, description, goal, status, requirementId, requirementCode, requirementTitle, projectId, projectName, projectCode, ownerUserId, ownerName, ownerLoginName, startDate, endDate, storyCount, createTime, updateTime, createBy, updateBy`

#### Scenario: POST 响应回包 echo 关键字段

- **GIVEN** 数据库已 seed Project / Requirement / User（reqId=1, userId=1）
- **WHEN** 客户端 `POST /api/sprints` body `{"code":"SPR-ECHO","name":"Phase 1","requirementId":1,"ownerUserId":1}`
- **THEN** 系统 SHALL 返回 HTTP 201
- **AND** `body.id` SHALL 为正整数
- **AND** `body.code` SHALL 为 `"SPR-ECHO"`
- **AND** `body.requirementId` SHALL 为 `1`
- **AND** `body.ownerUserId` SHALL 为 `1`
