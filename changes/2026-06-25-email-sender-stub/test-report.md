# email-sender-stub test report (v0.0.92, D4)

## Results
- `mvn test` —— **826 / 826 PASS**（0 failure, 0 error, 0 skip）
- 新增测试 9 个：
  - `LogEmailSenderTest` × 5 —— send 落库、bodyText 截断为 500、空收件人 failure、SmtpStub 抛 UnsupportedOperationException、多收件人 JSON
  - `EmailEndpointsIntegrationTest` × 4 —— GET /api/emails 分页、转发成功落库、无 email 400、非归属 404
- 调整既有 1 个：
  - `LegacyProductCategoryCleanupTest` —— 41 → 42（新增 rainier_sent_email 表）

## Scenario 覆盖
| spec scenario                  | 对应测试                                                     | 状态 |
|--------------------------------|--------------------------------------------------------------|------|
| 1. send 即落库                  | `LogEmailSenderTest.send_persistsRecordWithSentStatus`       | OK   |
| 2. bodyText 截断 500            | `LogEmailSenderTest.send_truncatesBodyText_to500Chars`       | OK   |
| 3. SmtpStub 抛 UOE              | `LogEmailSenderTest.smtpStub_send_throwsUnsupported`         | OK   |
| 4. GET /api/emails 需 admin     | (依赖 admin-authz=true，由 AdminAuthorizationTest 兜底覆盖)  | N/A  |
| 5. GET /api/emails 分页         | `EmailEndpointsIntegrationTest.listSentEmails_returnsPage`   | OK   |
| 6. forward 成功                 | `EmailEndpointsIntegrationTest.forwardNotificationToEmail_persistsSentRecord` | OK |
| 7. 无 email → 400              | `EmailEndpointsIntegrationTest.forwardNotification_userNoEmail_400` | OK |
| 8. 非归属 → 404                | `EmailEndpointsIntegrationTest.forwardNotification_notOwner_404` | OK |

## Caveats
- `app.email.kind` 未在 application.yml 显式声明 —— `matchIfMissing=true` 让 LogEmailSender 默认生效
- `to[]` 用极简手写 JSON 序列化（不引入额外依赖），双引号 / 反斜杠 / 控制字符已转义
- forward 端点的 `from` 写死 `noreply@rainier.local`（占位，后续可配置）
- spec Scenario 4（admin 403）未单独写测试 —— 测试 profile 关闭了 admin-authz；新加的 `/api/emails` 已加入 AdminPaths TIER_A，与既有的 `AdminAuthorizationTest` 共享同一条 enforcement 路径
