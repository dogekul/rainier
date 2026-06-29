# Test Report — event-adapters (A2, v0.0.66)

## 新增类

- `backend/src/main/java/com/rainier/event/adapter/GitLabAdapter.java`
- `backend/src/main/java/com/rainier/event/adapter/DingTalkAdapter.java`
- `backend/src/main/java/com/rainier/event/adapter/FeishuAdapter.java`
- `backend/src/main/java/com/rainier/event/adapter/EmailAdapter.java`
- `backend/src/main/java/com/rainier/event/adapter/ZentaoAdapter.java`
- `backend/src/main/java/com/rainier/event/bootstrap/EventSeed.java`

## 新增测试

- `backend/src/test/java/com/rainier/event/adapter/GitLabAdapterTest.java` (4 cases)
- `backend/src/test/java/com/rainier/event/adapter/ZentaoAdapterTest.java` (4 cases)
- `backend/src/test/java/com/rainier/event/adapter/DingTalkAdapterTest.java` (2 cases)
- `backend/src/test/java/com/rainier/event/adapter/FeishuAdapterTest.java` (2 cases)
- `backend/src/test/java/com/rainier/event/adapter/EmailAdapterTest.java` (2 cases)
- `backend/src/test/java/com/rainier/event/service/EventServiceProcessIntegrationTest.java` (1 case
  端到端 — 5 events 经全 5 adapter 后 processed=true，GitLab/Zentao 含 ref)

## 配置变更

- `backend/src/main/resources/application.yml` — `app.demo.event-seed.enabled: true`
- `backend/src/test/resources/application-test.yml` — `app.demo.event-seed.enabled: false`

## 数据库 DDL

无（沿用 v0.0.65 `rainier_event` 表，未加字段）

## 测试结果

```
Tests run: 589, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

较 A1 (574) 新增 15 tests，全部通过。

## Caveats

- 钉钉/飞书/邮件 adapter 仅做 supports 标记，不抽取实体；A3+ 引入真实集成时需补 extract 逻辑
- GitLab/Zentao 仅匹配 payload 文本，不解析 JSON 结构，也不处理多次匹配（取第一个）
- 没有 webhook controller — 事件入库仍走 `POST /api/events` (A1) 或 `EventSeed`
- `EventSeed` 写入 5 条样例事件后不会自动触发 `process()`；调用方需自行 `POST /api/events/process` (A1)
  或后续 A3 引入定时任务
