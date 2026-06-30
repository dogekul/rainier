# Proposal — password-reset-real-email (G3, v0.0.107)

## 命门
B3 留下的 `PasswordService.issueResetToken` 只在 INFO log 打印 token —
flow 名义闭环但用户拿不到 token。D4 提供了 `EmailSender` 抽象 + `LogEmailSender` 默认实现 +
`SmtpEmailSenderStub`（仅抛 `UnsupportedOperationException`）。G3 把两边接起来：
1. `PasswordService` 注入 `EmailSender`，token 生成后真正调一次 send（log/smtp 都行）；
2. 真正实现 `SmtpEmailSender`（覆盖 D4 stub），通过 `spring-boot-starter-mail` 的 `JavaMailSender` 发邮件，
   并复用 LogEmailSender 的落库逻辑（抽到 `EmailRecorder` helper）。

## 范围
1. **pom.xml**：加 `spring-boot-starter-mail` 依赖
2. **NEW `EmailRecorder` helper**（package-private）：抽出 `SentEmailRecord` 落库逻辑 +
   `toJsonArray` / `snippet` 静态工具，Log 与 Smtp 实现共用，避免重复
3. **REWRITE `LogEmailSender`**：改用 `EmailRecorder.record(...)`，行为 100% 兼容
4. **REPLACE `SmtpEmailSenderStub` → `SmtpEmailSender`**：
   - `@Component @ConditionalOnProperty(app.email.kind=smtp)`
   - 注入 `JavaMailSender`
   - 构造 `SimpleMailMessage` (text only) 调 `mailSender.send(...)`；失败时记录 `STATUS_FAILED` 并返回
     `SendResult.failure`（不抛）
   - 成功 / 失败均通过 `EmailRecorder` 落库
   - bodyHtml 字段 D4 已定义但 G3 仍按纯文本发送（HTML 模板留待后续）
5. **修改 `PasswordService.issueResetToken`**：
   - inject `EmailSender`
   - token 写库后构造 `EmailMessage`：subject=「Rainier 密码重置」，bodyText 含
     `http://{frontend-base}/reset-password?token={token}` + 「1 小时内有效」
   - `user.emailAddress` 为空 → 跳过 send（仅 INFO log token，原 dev 行为保留）
   - send 抛异常 → log warn + 继续返回（不让邮件挂导致 200 变 500）
   - 注入 `@Value("${app.frontend.base-url:http://localhost}")` 作为 link 前缀
6. **application.yml**：
   - `app.email.kind: log`（与 matchIfMissing=true 对齐，显式注释）
   - `app.frontend.base-url: http://localhost`
   - 注释一段 `spring.mail.host / port / username / password` 模板，dev 默认不启用
7. **D4 archive spec 更新**：注明 G3 已用 `SmtpEmailSender` 替换 stub（保留历史 Scenario 文本不删，
   追加「v0.0.107 G3 update」block）

## OutOfScope
- 真实 SMTP server 联调
- HTML 邮件模板系统
- 退订 / 邮件偏好
- 多语言

## 测试
- `PasswordServiceEmailIntegrationTest` @SpringBootTest test profile：
  - mock `EmailSender` bean，断言 forgotPassword 成功时 send 被调一次 + bodyText 含 reset URL + token
  - emailAddress 空 → send 不被调用 + 仍写 PasswordResetToken
  - EmailSender 抛 → forgotPassword 仍返回正常
- `SmtpEmailSenderTest` 单测：mock `JavaMailSender`，验证 SimpleMailMessage 内容 + 失败路径不抛
- 既有 `PasswordServiceTest` (15 cases) + `LogEmailSenderTest` (5 cases) 保持不变

## commit
`feat(password-reset-real-email): G3 forgot-password 真发邮件 + SmtpEmailSender (v0.0.107)`
