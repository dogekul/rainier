# Proposal: B2 IdentityProvider 接口 + LDAP/OAuth stub

## Problem
`AuthController.login` 当前直接 `userRepo` + `passwordEncoder`，将认证后端硬绑到 LocalDb。
未来要接 LDAP / OAuth / SAML 时无插拔点。

## Decision
引入 `IdentityProvider` SPI：
- 链式调用、按 `@Order` 试、第一个非空胜出
- `LocalDbIdentityProvider` 为 `@Primary`，承载现有 BCrypt
- `LdapIdentityProviderStub` / `OAuthIdentityProviderStub` 仅在 feature flag 开启时进入 context，目前永远返回空

## Non-Goals
- 真实 LDAP/OAuth 协议代码
- SSO 跳转
- Provider 之间用户自动合并

## Compatibility
- `app.security.real-auth.enabled=false` 路径不变（仍走 mock token）
- `RealAuthLoginTest` 6 用例零修改通过
