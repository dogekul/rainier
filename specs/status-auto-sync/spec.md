# Capability: status-auto-sync

> NEW capability (v0.0.67-status-auto-sync, 2026-06-25) — **Event → AiWorkLog PROPOSED 自动同步**。
> 在 v0.0.65/0.0.66 事件管线之上，把 GitLab PR_MERGE + TASK 抽取结果转成一条 AI 提议
> （`AiWorkLog`, status=PROPOSED），由人工 accept/reject。**不直接改业务实体**——所有状态变化
> 必须经 `/api/ai-work-logs/{id}/decision` 端点确认（沿用既有 v0.0.43 决策机制，本版不动）。

## ADDED Requirements

### Requirement: PR_MERGE + TASK 抽取 → 提议改 Task 为 DONE

`StatusSyncService.applyExtraction(event)` SHALL 在 event 满足以下全部条件时创建一条
`AiWorkLog`（agentType=STATUS_SYNC, action=UPDATE_TASK_STATUS, status=PROPOSED, targetType=TASK,
targetId=event.extractedEntityId, evidence 含 eventId / sourceType / sourceId / eventKind）：

- `event.processed == true`
- `event.eventKind == "PR_MERGE"`
- `event.extractedEntityType == "TASK"`
- `event.extractedEntityId != null`

否则 SHALL 为 no-op（不创建任何 AiWorkLog）。

#### Scenario: PR_MERGE + TASK 42 → 1 条 PROPOSED 写入

- **GIVEN** event `{sourceType:"GITLAB", sourceId:"mr-1", eventKind:"PR_MERGE", processed:true, extractedEntityType:"TASK", extractedEntityId:42}`
- **WHEN** `StatusSyncService.applyExtraction(event)`
- **THEN** SHALL 在 `rainier_ai_work_log` 表写入 1 行
- **AND** 该行 `status="PROPOSED"`, `agentType="STATUS_SYNC"`, `action="UPDATE_TASK_STATUS"`
- **AND** `targetType="TASK"`, `targetId=42`
- **AND** `evidence` JSON 字符串含 `"eventId"` 与 `"PR_MERGE"`
- **AND** Task #42 实体本身 SHALL 未被修改（等人工 accept）

#### Scenario: 非 PR_MERGE 事件 → 不产生提议

- **GIVEN** event `{eventKind:"PR_OPEN", processed:true, extractedEntityType:"TASK", extractedEntityId:42}`
- **WHEN** `StatusSyncService.applyExtraction(event)`
- **THEN** SHALL 不创建任何 AiWorkLog（表行数不变）

#### Scenario: 非 TASK 抽取 → 不产生提议

- **GIVEN** event `{eventKind:"PR_MERGE", processed:true, extractedEntityType:"STORY", extractedEntityId:7}`
- **WHEN** `StatusSyncService.applyExtraction(event)`
- **THEN** SHALL 不创建任何 AiWorkLog（本版只处理 TASK，STORY 留后续）

### Requirement: EventService.process 钩入 StatusSync

`EventService.process(maxBatch)` SHALL 在每条 event 完成 extractor 调用并标 `processed=true` 后，
立即调用 `StatusSyncService.applyExtraction(event)`。两者运行于同一事务——若 StatusSync 抛异常，
event 标记同回滚。

#### Scenario: process() 集成 status sync

- **GIVEN** 1 条未处理 GITLAB PR_MERGE 事件，payload="fix login RA-100 done"
- **WHEN** `EventService.process(10)`
- **THEN** 该 event SHALL `processed=true`, `extractedEntityType="TASK"`, `extractedEntityId=100`
- **AND** `rainier_ai_work_log` SHALL 多出一行 `STATUS_SYNC` PROPOSED 对应 task 100
