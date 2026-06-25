# Proposal: AI 推理接口 + AI 错误公示板 (A4, v0.0.68)

## What
- 新增 `AiInference` 接口（飞轮 AI 调用契约）+ `StubAiInference` 默认实现（deterministic stub）。
- 新增 `AiError` 实体（rainier_ai_error）+ Repository / Service / Controller，提供 AI 错误录入、公示、修复关闭。

## Why
飞轮（AI proposes → human decides）需要一个统一 AI 调用入口（后续接 LLM/模型时只换实现）；同时把 AI 出错的事实公开化（错误公示板）是飞轮"信任契约"的反面证据，便于事后回溯与改进。

## Scope
- backend/ai: AiInference, StubAiInference, AiError entity + repo/service/controller, DTOs。
- AdminPaths: 把 `POST /api/ai/errors/{id}/fix` 加入 Tier B（写 admin、读 all-users）。
- spec.md + 测试（StubAiInferenceTest, AiErrorServiceTest）。

## OutOfScope
- 真实 LLM 调用（StubAiInference 是 placeholder）。
- 错误自动检测（手动 record，本版不自动）。
- 前端页面（A9 才做 UI）。

## Decisions
- `infer(taskKind, input, outputClass)`：按 outputClass 默认构造一个对象返回；String/Integer/Long/Boolean 有 deterministic stub 值。
- AiError.status 默认 OPEN；markFixed 强制要求 fixAction 非空。
- 错误列表所有用户可见（GET / 所有用户）；只有 admin 可点击"已修复"（POST /{id}/fix）。
