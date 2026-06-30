# Spec — password-reset-real-email (G3, v0.0.107)

## Scenario 1：forgotPassword 命中用户 → EmailSender.send 被调用
- Given 用户 alice，emailAddress=`alice@example.com`，PasswordResetToken 表空
- And mock `EmailSender` 已替换默认 bean
- When 调 `service.issueResetToken("alice", "alice@example.com")`
- Then `EmailSender.send` 被调用 exactly 1 次
- And 捕获的 `EmailMessage`：
  - `to` 含 `alice@example.com`
  - `subject` == `Rainier 密码重置`
  - `bodyText` 包含 `http://localhost/reset-password?token=<32hex>`
  - `bodyText` 包含「1 小时内有效」
- And PasswordResetToken 表新增 1 行

## Scenario 2：用户无 emailAddress → 不发邮件但 token 仍写库
- Given 用户 bob，emailAddress=null，PasswordResetToken 表空
- When 调 `service.issueResetToken("bob", "")`  → 因 email 空走 issueResetToken 的「email 必填」分支抛 400
- (修正) Given 用户 bob，emailAddress=null
- When admin 路径不存在；issueResetToken 校验 (loginName,email) 必须匹配，bob 没 email 故任何 email 都 mismatch → 静默无 token
- 因此本 Scenario 改为：用户 carol，emailAddress="" 空字符串：issueResetToken 走 email mismatch 静默路径，
  不写 token，也不调 EmailSender.send
- Then `EmailSender.send` 调用次数 = 0
- And PasswordResetToken 表行数 = 0

## Scenario 3：EmailSender 抛异常 → forgotPassword 仍正常返回
- Given mock `EmailSender.send` 抛 `RuntimeException("smtp down")`
- And 用户 alice email 匹配
- When 调 `service.issueResetToken("alice", "alice@example.com")`
- Then 不抛任何异常（fault-tolerant）
- And PasswordResetToken 仍新增 1 行（先写 token 后发邮件的顺序，邮件挂了 token 仍可用）
- And 日志中含 warn 级 `[password-reset] email send failed`

## Scenario 4：SmtpEmailSender 真调 JavaMailSender.send
- Given mock `JavaMailSender`，注入 `SmtpEmailSender`
- And 输入 EmailMessage(from=noreply@rainier, to=[a@x.com], subject=hi, bodyText=hello)
- When 调 `sender.send(msg)`
- Then `JavaMailSender.send(SimpleMailMessage)` 被调 1 次，参数：
  - `getFrom()=="noreply@rainier"`
  - `getTo()[0]=="a@x.com"`
  - `getSubject()=="hi"`
  - `getText()=="hello"`
- And 返回 `SendResult.isSuccess()==true`，providerId 前缀 `smtp:`
- And `rainier_sent_email` 新增 1 行 status=SENT

## Scenario 5：SmtpEmailSender JavaMailSender 抛 MailException → 容错落库失败行
- Given mock `JavaMailSender.send(...)` 抛 `MailSendException("relay down")`
- When 调 `sender.send(msg)`
- Then 不抛
- And 返回 `SendResult.isSuccess()==false`，errorMessage 含 `relay down`
- And `rainier_sent_email` 新增 1 行 status=FAILED, failReason 非空

## Scenario 6：既有 PasswordService 测试不被破坏
- Given 默认 test profile + 默认 `LogEmailSender` bean 装配
- When `PasswordServiceTest` 全部 case 运行
- Then 全部通过（forgotThenReset_happyPath 现在也会触发一次 log email，不影响 token 行为）
