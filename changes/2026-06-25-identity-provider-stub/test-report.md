# test-report — identity-provider-stub (v0.0.75 B2)

## 命令
`cd backend && mvn test`

## 结果
- Tests run: **653**, Failures: 0, Errors: 0, Skipped: 0
- BUILD SUCCESS (13.2s)

## 新增覆盖
| 用例 | Test | Status |
|---|---|---|
| TC-IDP-001 | `LocalDbIdentityProviderTest#authenticate_correctPassword_returnsIdentity` | PASS |
| TC-IDP-002a | `authenticate_wrongPassword_returnsEmpty` | PASS |
| TC-IDP-002b | `authenticate_unknownUser_returnsEmpty` | PASS |
| TC-IDP-002c | `authenticate_disabledUser_returnsEmpty` | PASS |
| TC-IDP-002d | `authenticate_nullInputs_returnsEmpty` | PASS |
| TC-IDP-003 | `AuthServiceProviderChainTest#chain_secondProviderSucceeds_returns200` | PASS |
| TC-IDP-004 | `chain_allEmpty_returns401` | PASS |
| TC-IDP-006 | `RealAuthLoginTest`（6 用例零修改） | PASS |

## 回归
- `AuthControllerLoginTest` (3) — PASS
- `RealAuthLoginTest` (6, BCrypt 真实校验路径) — PASS
- 全量 653 用例 — PASS

## Caveats
- LDAP / OAuth stub 默认 `@ConditionalOnProperty` 未启用，因此未直接装入 ApplicationContext；TC-IDP-005 通过手写 `@TestConfiguration` 内嵌 stub 等效验证链式行为。
- 实现仍使用本机 JDK 25 编译；Docker temurin-8 Java-8 gate 需在 CI 验收时再跑一次。
