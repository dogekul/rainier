# push-channel proposal (v0.0.72, A8)

## What
新增**站内通知中心** stub —— `Notification` 实体 + repo/service/controller，三个 `/api/me/notifications*` 端点；`RiskService.runAll` 在产出 `CRIT` finding 时旁路推送一条站内通知给当前用户。

## Why
"被动看板" → "主动推送" 是飞轮的关键一步。本版只接入站内通知；邮件 / IM 走 stub，后续可替换。

## Scope
- NEW entity `Notification`（rainier_notification: userId / title / body @Lob / level INFO|WARN|CRIT / entityType / entityId / createdAt / readAt）
- NEW `NotificationRepository`，`NotificationService.send / listFor(onlyUnread) / markRead / markAllRead`
- NEW `MeNotificationsController`：`GET /api/me/notifications`、`POST /api/me/notifications/{id}/read`、`POST /api/me/notifications/read-all`，全员 token-gated
- 修改 `RiskService.runAll`：对每条 `LEVEL_CRIT` 调用 `NotificationService.send`（每次 runAll 都可能重复发送 —— OutOfScope 收敛）

## OutOfScope
- 邮件 / IM 真实推送通道
- 推送频率去重 / 抑制窗口
- 通知偏好设置

## Decisions
- level 用 `String`（INFO/WARN/CRIT），对齐 RiskFinding 现有约定，不引入 enum
- 列表分页用 `PageParams` + `PageResponse`，与 audit-log 同形态
- `readAt = LocalDateTime`（nullable），已读用 timestamp 表达；markAllRead 返回更新行数
- 仅当前用户访问自己的通知；通过 `SecurityFilter` 注入的 username 解析 userId
- `RiskService` 既有 controller 调用路径每次都旁路推送 —— 接受重复，符合 "stub" 定义
