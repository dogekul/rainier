# Test Report — auth-baseline-global (B1, v0.0.74)

## 命令

```
cd backend && mvn test
```

## 结果

- Tests run: 646, Failures: 0, Errors: 0, Skipped: 0
- BUILD SUCCESS

## 关键覆盖

新增 `GlobalAuthBaselineTest`（`@SpringBootTest(webEnvironment=RANDOM_PORT)` + 真实 TestRestTemplate
+ `app.security.require-all-users-token.enabled=true`）：

| Case | 端点 | 期望 | 结果 |
|------|------|------|------|
| inbox_noToken_returns401 | GET /api/me/inbox 无 token | 401 + "Missing or invalid token" | PASS |
| inbox_validToken_returns200 | GET /api/me/inbox + Bearer alice | 200 | PASS |
| login_noToken_notBlockedBySecurityFilter | POST /api/auth/login 无 token | 非 SecurityFilter-401 | PASS |
| inbox_matrixParam_noToken_returns401 | GET /api/me/inbox;x=1 无 token | 401（PATH_HELPER 剥离） | PASS |

既有 `AuthBaselineTest`（7 case MockMvc 版）仍全绿；39+ admin 控制器测试不动；
`MeInboxControllerTest` 不动；`SecurityFilter` 重构（白名单常量化到 `SecurityWhitelistPaths`）零回归。

## 遗留

无。后续 B2-B4（SSO、改密、细粒度权限）按规划在独立 sub-change 内实施。
