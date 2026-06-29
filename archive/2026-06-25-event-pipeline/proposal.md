# Proposal: event-pipeline (A1)

## What
新建 `event` 模块：`Event` 实体 + Repository + `EventExtractor` 接口 + `EventService` + `EventController`，承载 GITLAB/钉钉/飞书/邮件/禅道 等外部事件 webhook 收集与抽取 pipeline 的基础壳。

## Why
飞轮层关键基础：所有“AI 提议 / 状态自动同步 / 风险雷达”等高阶能力都依赖一条统一的事件流。先把"事件如何进来、如何被处理、抽取结果挂到哪个业务实体"的契约固化下来，后续 A2 才能加 stub adapter，A3 才能挂状态同步。

## Scope
- entity `Event`（rainier_event）+ `EventRepository`
- 接口 `EventExtractor` + DTO `ExtractionResult`
- `EventService.record()` 持久化原始事件；`EventService.process(maxBatch)` 遍历未处理事件并调 extractor
- `EventController`：`POST /api/events` webhook 入口；`GET /api/events`、`GET /api/events/pending`
- 后端测试：Repository（findByProcessedFalse / findBySourceTypeAndSourceId）+ Service（record / process）

## OutOfScope
- 真实 GitLab / 钉钉 / 飞书 集成（A2 才做 stub adapter）
- 状态自动同步实现（A3）
- 前端列表页（暂无）
- 分级鉴权（A5；现阶段 all-users token-optional）

## Decisions
- `payload` 用 `@Lob String`，存原始 JSON，不在本层 parse
- `occurredAt` 由外部系统提供（NOT NULL），`receivedAt` 由 `@CreationTimestamp` 自动填
- `processed=false` + `extractedEntityType/Id=null` 为初始状态；processed 后即便 extractor 返回 empty 也置 true
- 不继承 `BaseEntity`：Event 是流水/日志，不参与 audit/soft-delete；用 Hibernate `@CreationTimestamp` 处理 receivedAt
