# spec: push-channel

capability: push-channel
version: v0.0.72

## Scenario 1 — NotificationService.send 写入一条 INFO 通知

GIVEN: user alice (userId=1)
WHEN:  调用 `notificationService.send(1L, "标题", "正文", "INFO", "TASK", 42L)`
THEN:  数据库 `rainier_notification` 新增 1 行
AND:   返回实体 readAt=null, createdAt!=null, level=INFO

## Scenario 2 — listFor onlyUnread 只返回未读

GIVEN: alice 有 3 条通知（n1/n2/n3），其中 n2 已 markRead
WHEN:  `listFor(1L, PageRequest.of(0,20), true)`
THEN:  返回 content size=2，含 n1/n3；total=2

## Scenario 3 — markAllRead 把当前用户全部未读置已读

GIVEN: alice 有 2 条未读 + 1 条 bob 的未读
WHEN:  `markAllRead(1L)`
THEN:  alice 的 2 条 readAt 非空；bob 的 1 条 readAt 仍为 null

## Scenario 4 — RiskService.runAll 对每条 CRIT finding 旁路推送

GIVEN: alice 在 P1 有 1 个 BLOCKED story（命中 BlockedStoryRule → CRIT）
WHEN:  `riskService.runAll("alice", "mine")`
THEN:  返回 finding 列表（包含 CRIT）
AND:   alice 的 notification 表新增 ≥1 条 level=CRIT 的 row（title 含 "风险"）

## Scenario 5 — GET /api/me/notifications 端点 token-gated

GIVEN: alice 有 1 条通知，且 alice 已登录拿到 token
WHEN:  `GET /api/me/notifications?onlyUnread=true` 带 token
THEN:  HTTP 200，body 是 PageResponse，content 非空
AND:   无 token 调用同端点 → HTTP 401
