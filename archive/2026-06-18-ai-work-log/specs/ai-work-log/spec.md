# Capability: ai-work-log — v0.0.43 (NEW)

> NEW capability。飞轮层底座：AI 工作日志（提议-证据-裁决状态机）。AI 每个动作 = 一条带 evidence 的 PROPOSED 提议，
> 人类可见/可采纳/可驳回。all-users（token 必需）。新表 rainier_ai_work_log。种子驱动壳。路线图 §3/§4。见 [[frontend-scaffold]]。

## ADDED Requirements

### Requirement: AI 工作日志创建（提议必带证据）

后端 SHALL 提供 `POST /api/ai-work-logs`，必填 `agentType` / `action` / `summary` / `evidence`（任一空 → 400），
可选 `targetType` / `targetId`，创建一条 `status=PROPOSED` 的日志。

#### Scenario: 创建提议

- **WHEN** `POST /api/ai-work-logs` body `{agentType:"STATUS_SYNC", action:"UPDATE_TASK_STATUS", summary:"...", evidence:"commit abc123"}`
- **THEN** SHALL 返回 201
- **AND** body.status SHALL 为 "PROPOSED"

#### Scenario: 缺证据被拒

- **WHEN** `POST /api/ai-work-logs` 缺 `evidence`（或为空）
- **THEN** SHALL 返回 400

### Requirement: AI 工作日志查询

后端 SHALL 提供 `GET /api/ai-work-logs?agentType=&status=&page=&size=`，分页倒序（按创建时间），支持 agentType /
status 过滤；`GET /api/ai-work-logs/{id}` 返回单条或 404。

#### Scenario: 按状态过滤

- **GIVEN** 存在 PROPOSED 与 ACCEPTED 各若干
- **WHEN** `GET /api/ai-work-logs?status=PROPOSED`
- **THEN** body.content SHALL 仅含 status=PROPOSED 的日志

### Requirement: 裁决状态机

后端 SHALL 提供 `POST /api/ai-work-logs/{id}/decision`，body `{decision, reason?}`，`decision` SHALL 限
ACCEPTED/REJECTED；仅 `status=PROPOSED` 可裁决；裁决记 `decidedBy` + `decidedAt`。

#### Scenario: 采纳提议

- **GIVEN** 存在 PROPOSED 日志 L
- **WHEN** `POST /api/ai-work-logs/L/decision` body `{"decision":"ACCEPTED"}`
- **THEN** SHALL 返回 200
- **AND** body.status SHALL 为 "ACCEPTED"
- **AND** body.decidedBy SHALL 非空

#### Scenario: 驳回必带理由

- **GIVEN** 存在 PROPOSED 日志 L
- **WHEN** `POST /api/ai-work-logs/L/decision` body `{"decision":"REJECTED"}`（无 reason）
- **THEN** SHALL 返回 400

#### Scenario: 驳回带理由成功

- **GIVEN** 存在 PROPOSED 日志 L
- **WHEN** `POST /api/ai-work-logs/L/decision` body `{"decision":"REJECTED","reason":"误判"}`
- **THEN** SHALL 返回 200
- **AND** body.status SHALL 为 "REJECTED"
- **AND** body.rejectReason SHALL 为 "误判"

#### Scenario: 重复裁决被拒

- **GIVEN** 日志 L 已 ACCEPTED
- **WHEN** 再次 `POST /api/ai-work-logs/L/decision` body `{"decision":"REJECTED","reason":"x"}`
- **THEN** SHALL 返回 409

#### Scenario: 非法 decision 被拒

- **WHEN** `POST /api/ai-work-logs/{id}/decision` body `{"decision":"MAYBE"}`
- **THEN** SHALL 返回 400

### Requirement: 种子驱动壳

启动时（`app.demo.ai-work-log-seed.enabled=true` 且表为空）后端 SHALL 种入若干 PROPOSED 样例日志；幂等
（非空时不重复种）；test profile（flag=false）SHALL NOT 种入。

#### Scenario: 表空时种入

- **GIVEN** `app.demo.ai-work-log-seed.enabled=true`，rainier_ai_work_log 为空
- **WHEN** 应用启动运行 AiWorkLogSeed
- **THEN** 表中 SHALL 出现 ≥1 条 PROPOSED 日志（均带非空 evidence）

#### Scenario: 已有数据不重复种

- **GIVEN** 表中已有日志
- **WHEN** AiWorkLogSeed 再次运行
- **THEN** SHALL NOT 新增种子（count 不变）
