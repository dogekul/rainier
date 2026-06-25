# Test Report — event-pipeline (A1)

## 新增类

- `backend/src/main/java/com/rainier/event/domain/Event.java`
- `backend/src/main/java/com/rainier/event/repository/EventRepository.java`
- `backend/src/main/java/com/rainier/event/extractor/EventExtractor.java`
- `backend/src/main/java/com/rainier/event/extractor/ExtractionResult.java`
- `backend/src/main/java/com/rainier/event/dto/EventCreateRequest.java`
- `backend/src/main/java/com/rainier/event/dto/EventDetail.java`
- `backend/src/main/java/com/rainier/event/service/EventService.java`
- `backend/src/main/java/com/rainier/event/controller/EventController.java`
- `backend/src/test/java/com/rainier/event/repository/EventRepositoryTest.java`
- `backend/src/test/java/com/rainier/event/service/EventServiceTest.java`

## 修改类

- `backend/src/test/java/com/rainier/product/bootstrap/LegacyProductCategoryCleanupTest.java`
  — 表数 28 → 29（新增 `rainier_event`），并新增 `contains("rainier_event")` 断言。

## 新增表

`rainier_event`（DDL via JPA ddl-auto=update）:

| column | type | nullable | note |
|---|---|---|---|
| id | BIGINT PK AUTO | NO | identity |
| source_type | VARCHAR(32) | NO | GITLAB/DINGTALK/FEISHU/EMAIL/ZENTAO/MANUAL |
| source_id | VARCHAR(128) | YES | 外部系统 id |
| event_kind | VARCHAR(32) | NO | COMMIT/PR_OPEN/PR_MERGE/MESSAGE/DOC_CHANGE/OTHER |
| payload | LOB(TEXT) | YES | 原始 JSON |
| occurred_at | TIMESTAMP | NO | 外部提供 |
| received_at | TIMESTAMP | NO (auto) | @CreationTimestamp |
| processed | BOOLEAN | NO (default false) | |
| extracted_entity_type | VARCHAR(64) | YES | TASK/STORY/REQUIREMENT |
| extracted_entity_id | BIGINT | YES | |

## 测试通过数

- Backend: **574 tests pass** (0 failure / 0 error / 0 skipped)
  - 本切片新增 8 个：`EventRepositoryTest` 3 + `EventServiceTest` 5
- Frontend: 未改动前端，未跑

## Caveats

- 仍 0 真实集成（GitLab / 钉钉 / 飞书 webhook 未接入）—— 留给 A2 stub adapter。
- 没有任何 `EventExtractor` 实现（生产 bean 列表为空），`process()` 调用相当于"标记所有 pending 为 processed"——A2 才会加 stub extractor。
- `EventController` 端点为 all-users token-optional（沿用现状），分级鉴权留给 A5。
- 没加 seed 数据（事件流是 webhook 驱动，不需要 demo 种子）。
