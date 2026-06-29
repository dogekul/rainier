# A3: status-auto-sync (Event 抽取后自动写 AiWorkLog PROPOSED)

## What
为 v0.0.65/0.0.66 飞轮事件管线挂上"状态自动同步"：当 `EventService.process` 处理完一条事件
（`processed=true` 且 `extractedEntityType=TASK` 且 `eventKind=PR_MERGE`）后，自动创建一条
`AiWorkLog` PROPOSED，提议把对应 Task 标记为 DONE。

## Why
A1 持久化事件 + A2 抽取实体 ref；但事件不会真正影响业务实体。A3 把"事件 → 提议"环节闭环——
利用既有 `AiWorkLog` PROPOSED 状态机（accept/reject 由人工决策，沿用 v0.0.43 的
`/api/ai-work-logs/{id}/decision` 端点）。**不直接改 Task 状态**，所有变更必须经人工确认。

## Scope
- NEW `com.rainier.event.sync.StatusSyncService` (`@Service`)
- 修改 `EventService.process`：处理完每条 event 后调用 `statusSyncService.applyExtraction(event)`
- NEW spec.md（capability=status-auto-sync，1 Scenario）
- 测试：`StatusSyncServiceTest`（@SpringBootTest，验证 PR_MERGE+TASK 产生 1 条 PROPOSED）

## OutOfScope
- accept AiWorkLog 时真正改 Task 状态（沿用既有 decision endpoint，本 sub-change 不动）
- 其他事件类型 → 状态变更规则（仅 PR_MERGE→TASK 本版处理）
- Story / Requirement 自动同步
- 前端 toast / 推送
- 不新增 AiWorkLog 字段（既有 agentType/action/targetType/targetId/summary/evidence 足够）

## Decisions
- agentType = "STATUS_SYNC"，action = "UPDATE_TASK_STATUS"
- targetType="TASK", targetId=event.extractedEntityId
- summary = "AI 提议：基于 <sourceType> <eventKind> 将任务 #<id> 标记为 DONE"
- evidence = JSON `{"eventId":<id>,"sourceType":<...>,"sourceId":<...>,"eventKind":<...>}`
- 调用时机：`EventService.process` 内的循环里，**在 `e.setProcessed(true)` 之后** 调用一次
- StatusSyncService 自身 `@Transactional`（同 tx，失败回滚 event 标记）
