# Test Report — password-reset-real-email (G3, v0.0.107)

## Backend
- `mvn test` 全绿：**Tests run: 879, Failures: 0, Errors: 0, Skipped: 0**（耗时 ≈17.9s）

### 新增
- `PasswordServiceEmailIntegrationTest`（3）
  - `forgotPassword_sendsEmail_withTokenInBody` — happy path：`EmailSender.send` 被调一次，
    body 含 `http://localhost/reset-password?token=<32hex>` 和「1 小时内有效」，HTML 含 href
  - `forgotPassword_emailMismatch_skipsEmail_andNoToken` — carol email 为 null → 无 token、无 send 调用
  - `forgotPassword_emailSenderThrows_isSwallowed_andTokenPersisted` — mock send 抛 RuntimeException → 不传播，token 仍写库
- `SmtpEmailSenderTest`（3 单测，不开 Spring context）
  - `send_happyPath_callsMailSenderAndReturnsSuccess` — 验证 `SimpleMailMessage` 字段（from/to/subject/text）+ providerId=`smtp:42`
  - `send_mailExceptionThrown_isSwallowed_andFailedRowPersisted` — `MailSendException("relay down")` → 返回 failure，落 STATUS_FAILED 行，不抛
  - `send_emptyRecipients_returnsFailureWithoutTouchingMailSender` — 空 to → 直接返回 failure，never 调 mailSender

### 调整
- `LogEmailSenderTest`：删除已不存在的 `smtpStub_send_throwsUnsupported`（Scenario 4），保留剩余 4 用例
- `PasswordServiceTest`（既有 15 用例）：不动，全绿 — `forgotThenReset_happyPath` 现在会附带触发一次 `LogEmailSender.send`，对 token/PasswordResetToken 行为无影响

## Frontend
- 本切片仅后端，未触前端代码与测试。

## 遗留 / Caveats
- 没有连真 SMTP relay 联调；`application.yml` 的 `spring.mail.*` 块仍是被注释的占位模板，需要部署方在启用 smtp 前补
- `bodyHtml` 字段已在 `EmailMessage` 中设置，但 `SmtpEmailSender` 走 `SimpleMailMessage` 只发纯文本 — HTML 邮件需后续切换到 `MimeMessageHelper`
- D4 `archive/2026-06-25-email-sender-stub/spec.md` 的 Scenario 3（`UnsupportedOperationException`）已加更新 block 标注作废，但 Scenario 文本本体保留作历史记录
