# spec: risk-radar

capability: risk-radar
version: v0.0.70

## Scenario 1 — OverdueTaskRule 命中过期未完成 task

GIVEN: project P1 下存在 task T (status=IN_PROGRESS, dueDate=今天-1, projectId=P1)
AND:   project P1 下存在 task T2 (status=DONE, dueDate=今天-5)
WHEN:  以 RiskContext(userId=u, projectIds=[P1], now=now) 调用 OverdueTaskRule.evaluate
THEN:  返回 1 个 RiskFinding（level=WARN, entityType=TASK, entityId=T.id, ruleName=OverdueTaskRule）

## Scenario 2 — BlockedStoryRule 命中 BLOCKED story

GIVEN: project P1 下存在 story S (status=BLOCKED, projectId=P1)
AND:   project P2 下存在 story S2 (status=BLOCKED)
WHEN:  ctx.projectIds=[P1] 调用 BlockedStoryRule.evaluate
THEN:  返回 1 个 finding（level=CRIT, entityType=STORY, entityId=S.id）；S2 不在范围内不返回

## Scenario 3 — SprintEndingNoDoneRule 三天内结束且无 DONE task

GIVEN: sprint SP (endDate=今天+2, requirement属于 P1)
AND:   SP 下 0 个 task 为 DONE
WHEN:  ctx.projectIds=[P1] 调用 SprintEndingNoDoneRule.evaluate
THEN:  返回 1 个 finding（level=WARN, entityType=SPRINT, entityId=SP.id）

## Scenario 4 — RiskService.runAll 聚合所有规则 + 端点

GIVEN: 命中 Scenario 1/2/3 的所有数据 (P1 范围)
WHEN:  调用 RiskService.runAll(userId=u, scope="mine")
THEN:  返回 finding 列表 size ≥ 3，包含 OverdueTaskRule / BlockedStoryRule / SprintEndingNoDoneRule 各一条
AND:   `GET /api/me/risks?scope=mine` 返回 200，body 是同结构 JSON 数组
