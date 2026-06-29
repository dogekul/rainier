# email-sender-stub spec (v0.0.92, D4)

## Scenarios

### Scenario 1 — LogEmailSender 发送即落库
**Given** `app.email.kind` 未设置（matchIfMissing=true）  
**When** 调用 `emailSender.send(msg)`（to=["a@x.com"], subject="hi", bodyText="hello world"）  
**Then** 返回 `SendResult.success=true`；且 `rainier_sent_email` 多出一行：to_addrs_json 含 `a@x.com`、subject="hi"、body_text_snippet="hello world"、status="SENT"、sentAt 非空、failReason 为 null

### Scenario 2 — bodyText 截断为前 500 char
**Given** msg.bodyText 长度 = 800  
**When** send  
**Then** 持久化的 body_text_snippet 长度 = 500，且 == 原 bodyText 前 500 字符

### Scenario 3 — SmtpEmailSenderStub 抛 UnsupportedOperationException
**Given** `app.email.kind=smtp`  
**When** 调用 send  
**Then** 抛 `UnsupportedOperationException`

> **v0.0.107 G3 update** — `SmtpEmailSenderStub` 已被替换为真实 `SmtpEmailSender`（走 `spring-boot-starter-mail` 的 `JavaMailSender`，发 `SimpleMailMessage` 纯文本邮件）。本 Scenario 在 v0.0.107 起不再适用；对应行为请见 `changes/2026-06-25-password-reset-real-email/spec.md` Scenarios 4–5（真发 + MailException 容错落 STATUS_FAILED 行）。

### Scenario 4 — GET /api/emails 需 admin（非 admin 403）
**Given** 非 admin token  
**When** GET /api/emails  
**Then** 403

### Scenario 5 — GET /api/emails 分页返回最新在前
**Given** admin token、库中 3 行  
**When** GET /api/emails?page=0&size=10  
**Then** 200；content 长度 = 3；按 sentAt 倒序

### Scenario 6 — POST /api/me/notifications/{id}/email 成功转发
**Given** user.emailAddress="alice@x.com"，alice 名下通知 id=N  
**When** POST /api/me/notifications/N/email（alice token）  
**Then** 200；rainier_sent_email 多出一行：to_addrs_json 含 alice@x.com、subject 含通知 title

### Scenario 7 — 用户无 emailAddress → 400
**Given** user.emailAddress=null  
**When** POST forward  
**Then** 400

### Scenario 8 — 转发别人的通知 → 404
**Given** 通知归属 BOB  
**When** alice POST /api/me/notifications/N/email  
**Then** 404
