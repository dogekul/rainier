# Capability: ai-inference-and-error-board

> NEW capability (v0.0.68, 2026-06-25) — **飞轮 AI 推理统一入口 + AI 错误公示板**。`AiInference` 是飞轮所有 AI 调用的契约；`StubAiInference` 是默认实现（deterministic stub，后续可替换 LLM）。`AiError` 记录 AI 出错的事实，所有用户可见，admin 可标记修复。**A4：0 真实模型；只是壳 + 错误公示**。

## ADDED Requirements

### Requirement: AI 推理统一入口

后端 SHALL 提供 `AiInference` 接口与默认 `StubAiInference` 实现。

#### Scenario: Stub 推理按 outputClass 返回 deterministic stub

- **GIVEN** Spring 容器加载默认 `StubAiInference`
- **WHEN** 调用 `infer("ANY_KIND", "input", String.class)`
- **THEN** SHALL 返回非空字符串（deterministic stub，例如 `"stub:ANY_KIND"`）
- **WHEN** 调用 `infer("ANY_KIND", null, Integer.class)`
- **THEN** SHALL 返回 `0`
- **WHEN** 调用 `infer("ANY_KIND", null, Boolean.class)`
- **THEN** SHALL 返回 `Boolean.FALSE`

### Requirement: AI 错误录入

后端 SHALL 提供 `AiErrorService.record(...)` 持久化一条 `OPEN` 状态错误。

#### Scenario: 录入错误后状态 OPEN

- **WHEN** 调用 `record("SYNC_TASK_STATUS", "状态推理错误", "TASK", 42L, "MODEL", "evidence text")`
- **THEN** SHALL 返回 status=OPEN 的实体
- **AND** occurredAt SHALL 非空

### Requirement: AI 错误标记修复

后端 SHALL 提供 `POST /api/ai/errors/{id}/fix` 由 admin 标记某条错误已修复；body 含 `fixAction`。

#### Scenario: 修复后状态变为 FIXED

- **GIVEN** 存在一条 `OPEN` 的 AiError，id=X
- **WHEN** 调用 `markFixed(X, "调整 prompt v2")`
- **THEN** SHALL 返回 status=FIXED 的实体
- **AND** fixAction SHALL 为 `"调整 prompt v2"`

### Requirement: AI 错误列表

后端 SHALL 提供 `GET /api/ai/errors?status=&page=&size=`，按 occurredAt 倒序分页；status 可空（不过滤）。
