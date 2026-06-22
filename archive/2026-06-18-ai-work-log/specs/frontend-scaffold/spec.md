# Capability: frontend-scaffold — v0.0.43 ai-work-log delta (MODIFIED)

> 合并入 canonical `specs/frontend-scaffold/spec.md`（Phase 6）。仅新增以下 Requirements。

## ADDED Requirements (from change 2026-06-18-ai-work-log / v0.0.43)

### Requirement: 「AI 工作日志」落地页

前端 SHALL 在 `/ai/work-logs` 提供「AI 工作日志」页（all-users），消费 `GET /api/ai-work-logs`：渲染 StatTiles
（待裁决/已采纳/已驳回）+ 状态过滤 + 日志列表（agentType + action + summary + evidence + 状态 chip；PROPOSED 行带
采纳/驳回 按钮，调 `POST /api/ai-work-logs/{id}/decision` 后刷新）+ EmptyState。

#### Scenario: 渲染日志与裁决

- **GIVEN** `GET /api/ai-work-logs` 返回 1 条 PROPOSED 日志
- **WHEN** 用户打开 `/ai/work-logs`
- **THEN** SHALL 显示该日志（agentType/summary/evidence）
- **AND** SHALL 显示采纳/驳回按钮

#### Scenario: 采纳后刷新

- **GIVEN** 列表含 1 条 PROPOSED 日志
- **WHEN** 用户点击「采纳」
- **THEN** SHALL 调用 decideAiWorkLog(id, "ACCEPTED")
- **AND** SHALL 重新拉取列表

#### Scenario: 空列表

- **GIVEN** `GET /api/ai-work-logs` 返回空
- **WHEN** 用户打开 `/ai/work-logs`
- **THEN** SHALL 显示 EmptyState

### Requirement: AI 导航组（all-users）

前端 SHALL 在 AppLayout 新增「AI」顶级导航组（all-users），含「AI 工作日志」指向 `/ai/work-logs`；
`/ai/work-logs` SHALL NOT 被 `isAdminPath` 门控。`AppRoutes` SHALL 注册 `/ai/work-logs` 路由。

#### Scenario: /ai/work-logs 为 all-users

- **WHEN** 检查 `isAdminPath('/ai/work-logs')`
- **THEN** SHALL 返回 false

#### Scenario: 路由已注册

- **WHEN** 在 `/ai/work-logs` 挂载 AppRoutes
- **THEN** SHALL 渲染「AI 工作日志」页（ai-work-logs 容器可见）
