# Capability: event-pipeline

> NEW capability (v0.0.65-event-pipeline, 2026-06-25) — **飞轮层事件管线**。统一收集 GITLAB / 钉钉 / 飞书 / 邮件 / 禅道
> 等外部系统的原始事件，先持久化（`rainier_event`），再走 `EventExtractor` pipeline 尝试抽取到内部业务实体
> （TASK / STORY / REQUIREMENT）。all-users（token-optional）。**A1：0 真实集成；只有壳 + 抽取契约**。后续
> A2 加 stub adapter，A3 才挂状态同步。

## ADDED Requirements

### Requirement: 事件持久化（webhook 入口）

后端 SHALL 提供 `POST /api/events`，必填 `sourceType` / `eventKind` / `occurredAt`，可选 `sourceId` / `payload`，
持久化为一条 `processed=false` 的事件。

#### Scenario: 收到事件被持久化

- **WHEN** `POST /api/events` body `{sourceType:"GITLAB", sourceId:"mr-42", eventKind:"PR_OPEN", payload:"{...}", occurredAt:"2026-06-25T10:00:00"}`
- **THEN** SHALL 返回 201
- **AND** body.processed SHALL 为 false
- **AND** body.receivedAt SHALL 非空

### Requirement: 事件查询

后端 SHALL 提供 `GET /api/events?sourceType=&page=&size=` 分页列表（按 occurredAt 倒序），并 SHALL 提供
`GET /api/events/pending` 返回未处理事件（按 occurredAt 升序）。

#### Scenario: 仅未处理事件

- **GIVEN** 存在 processed=true 与 processed=false 各若干
- **WHEN** `GET /api/events/pending`
- **THEN** body SHALL 仅含 processed=false 的事件

### Requirement: 抽取 pipeline

后端 SHALL 提供 `EventService.process(maxBatch)`：遍历未处理事件，找到第一个 `supports(event)=true` 的
`EventExtractor` 调用 `extract`，将返回的 `extractedEntityType` / `extractedEntityId` 写回事件，并置
`processed=true`。即使没有任何 extractor 命中，processed 也 SHALL 置为 true 以避免重复处理。

#### Scenario: 命中 extractor 后回写实体引用

- **GIVEN** 存在一条 GITLAB 未处理事件 E，且某 extractor `supports(E)=true` 返回 `{TASK, 123, "UPDATE"}`
- **WHEN** 调用 `EventService.process(10)`
- **THEN** E.processed SHALL 为 true
- **AND** E.extractedEntityType SHALL 为 "TASK"
- **AND** E.extractedEntityId SHALL 为 123

#### Scenario: 无 extractor 命中也标记已处理

- **GIVEN** 存在一条未处理事件 E，无任何 extractor `supports(E)=true`
- **WHEN** 调用 `EventService.process(10)`
- **THEN** E.processed SHALL 为 true
- **AND** E.extractedEntityType SHALL 为 null
