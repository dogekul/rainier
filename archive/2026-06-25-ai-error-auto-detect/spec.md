# Spec: ai-error-auto-detect (capability=ai-error-auto-detect)

## Scenario 1: 自动公示 — 撤销已采纳的 AI 提议生成 AiError(OPEN)
GIVEN 一条 AiWorkLog id=L 状态 ACCEPTED，reverseSnapshot 非空，已挂接 executor
WHEN 用户 POST /api/ai-work-logs/L/reverse
THEN 应在 rainier_ai_error 表新增一行
AND 该行 status=OPEN, aiAction=log.action, evidence=reverseSnapshot 原文
AND errorDesc 包含 "reversed" 字样以表明根因为用户撤销

（Note: 该场景已由 F1 ai-decision-executor 行为覆盖；F5 仅追加断言式 coverage 在
AiWorkLogReverseControllerTest 中。）

## Scenario 2: countOverdueOpen 只数早于阈值的 OPEN
GIVEN 数据库中 5 条 AiError：
  - 2 条 OPEN 且 occurredAt = now - 30h (超 24h)
  - 2 条 OPEN 且 occurredAt = now - 2h (新鲜)
  - 1 条 FIXED 且 occurredAt = now - 100h
WHEN 调用 service.countOverdueOpen(24)
THEN 返回 2
AND GET /api/ai/errors/overdue-count?hours=24 返回 {"count":2,"thresholdHours":24}
AND GET /api/ai/errors/overdue-count (省略 hours) 也返回 {"count":2,"thresholdHours":24}

## Scenario 3: 前端 Banner 仅在 count>0 时出现
GIVEN AppLayout 已渲染
WHEN GET /api/ai/errors/overdue-count 返回 {"count":2}
THEN 顶部出现红色 banner 含 "2" 与 "查看公示板" 链接 (href=/ai/errors)
WHEN 接口返回 {"count":0}
THEN banner 不渲染（DOM 中无 data-testid="ai-error-overdue-banner"）
