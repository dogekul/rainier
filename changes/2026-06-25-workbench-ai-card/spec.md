# Spec: workbench-ai-card (capability=workbench-ai-card)

## Scenario 1: 卡片渲染 PROPOSED 提议（最多 3 条）
GIVEN 后端 `GET /api/ai-work-logs?status=PROPOSED&size=3` 返回 3 条 AiWorkLog（含 agentType / action / summary / evidence JSON）
WHEN WorkbenchPage 挂载
THEN 右栏顶部出现「AI 给我提的建议」卡片
AND 渲染 3 个 `data-testid=ai-suggest-row-{id}` 行
AND 每行显示 agentType+action chip、summary 文本
AND 若 evidence JSON 含 `eventId`，则附加 `事件 #{eventId} ({source})` 文案

## Scenario 2: 采纳后可在 5 秒内撤销
GIVEN AiSuggestionCard 已加载 1 条 PROPOSED 提议
WHEN 点击「采纳」按钮
THEN 调用 `POST /api/ai-work-logs/{id}/decision { decision: ACCEPTED }`
AND 该行状态变为「已采纳」并显示「撤销」按钮
AND 点击「撤销」调用 `POST /api/ai-work-logs/{id}/reverse` 并从卡片移除该行（或刷新列表）

## Scenario 3: 驳回需要内联输入原因
GIVEN AiSuggestionCard 已加载 1 条 PROPOSED 提议
WHEN 点击「驳回」按钮
THEN 该行展开 textarea 与「确认驳回」按钮
WHEN 输入「误判」并点击「确认驳回」
THEN 调用 `POST /api/ai-work-logs/{id}/decision { decision: REJECTED, reason: "误判" }`
AND 该行从卡片移除（刷新列表）
