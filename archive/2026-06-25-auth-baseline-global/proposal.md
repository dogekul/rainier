# Proposal — auth-baseline-global (B1, v0.0.74)

## 背景

v0.0.27 已经引入 `app.security.require-all-users-token` flag 和 SecurityFilter 全员 token 守门；
prod=true、test=false，AuthBaselineTest 通过 @TestPropertySource 覆盖。逻辑已经落地（401-before-403、
matrix-param 防绕过、whitelist=login+health）。

但白名单常量是 SecurityFilter 内部 `private static final String`，新代码/测试想引用得 hardcode 字符串；
而且作为「安全基线」缺乏一个独立、显式以 `webEnvironment=RANDOM_PORT` 跑完整 servlet stack 的 end-to-end
回归（AuthBaselineTest 用 MockMvc，覆盖足够但不验证真实 servlet 链路 + URL 解析）。

## 目标（B1）

- 把白名单常量化为 `SecurityWhitelistPaths`（POST /api/auth/login + GET /api/health/**）
- SecurityFilter 引用常量类（不改语义，纯重构）
- 新增 `GlobalAuthBaselineTest` @SpringBootTest(webEnvironment=RANDOM_PORT)：
  - 无 token GET /api/me/inbox → 401
  - 无 token POST /api/auth/login → 200/4xx 但非 401-未认证（通过 message 区分）
  - 有 token GET /api/me/inbox → 200
  - matrix-param 绕过 GET /api/me/inbox;x=1 → 401
- flag 默认仍在 prod=true / test=false（不破坏 39+ admin 测试 + 既有 AuthBaselineTest）

## OutOfScope（留给后续 B 批）

- B2 真实 SSO/OAuth
- B3 改密
- B4 细粒度权限

## 验收

- mvn test 全绿
- GlobalAuthBaselineTest 4 个 case 全过
- 既有 SecurityFilterTest / AuthBaselineTest / 全部 admin 测试不变
