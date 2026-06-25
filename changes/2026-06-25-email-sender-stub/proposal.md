# email-sender-stub proposal (v0.0.92, D4)

## What
新增**邮件发送抽象层** —— `EmailSender` 接口 + 两个实现（`LogEmailSender` @Primary 默认装配；`SmtpEmailSenderStub` 仅占位），落地 `SentEmailRecord` 表用于追踪发送记录，并暴露一个 admin-only 查询端点 + 一个示例 forward 端点（将站内通知转邮件）。

## Why
飞轮"主动推送"的第二条通道：站内通知（A8/D3）→ 邮件。本版只搭基建：抽象 + log impl + 持久化，**不真发邮件**。后续可在不改调用方的前提下接 SMTP / SendGrid / 阿里云邮件。

## Scope
- NEW package `com.rainier.email`
- NEW interface `EmailSender { SendResult send(EmailMessage msg); }`
- NEW value class `EmailMessage`（from / to[] / cc[] / subject / bodyText / bodyHtml）
- NEW value class `SendResult`（success / providerId / errorMessage）
- NEW `LogEmailSender` `@Primary @ConditionalOnProperty(app.email.kind=log, matchIfMissing=true)` —— `logger.info` 记录 + 持久化 `SentEmailRecord`（status=SENT）
- NEW `SmtpEmailSenderStub` `@ConditionalOnProperty(app.email.kind=smtp)` —— 抛 `UnsupportedOperationException`
- NEW entity `SentEmailRecord`（rainier_sent_email: fromAddr / toAddrsJson @Lob / subject / bodyTextSnippet / sentAt / status / failReason）
- NEW `SentEmailRecordRepository`
- NEW `GET /api/emails` —— admin only（AdminPaths TIER_A 加 `/api/emails`），分页查发送记录
- NEW `POST /api/me/notifications/{id}/email` —— 把站内通知通过 `EmailSender` 转发；收件人取 `User.emailAddress`（已存在，nullable —— 缺失则 400）

## OutOfScope
- 真实 SMTP / SendGrid / 阿里云邮件 SDK 接入
- 邮件模板系统
- 退订 / 订阅管理
- 重试 / 队列

## Decisions
- `LogEmailSender` `@Primary` + `matchIfMissing=true` —— dev/test 默认装配
- `to[]` 用 JSON 字符串持久化（`@Lob`），保持灵活，单值场景也走数组
- `bodyTextSnippet` 取前 500 char，避免 LOB 列；完整正文不持久化（log 即可）
- `status` `String`（SENT / FAILED），不引入 enum，对齐既有约定
- `/api/emails` 走 TIER_A —— GET/POST 都需 admin
- forward 端点 token-gated；只允许转发自己的通知（与 `MeNotificationsController.markRead` 同样的归属校验）
- `User.emailAddress` 已存在，**不新增字段**
