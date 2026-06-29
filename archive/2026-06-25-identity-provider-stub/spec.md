# identity-provider-stub spec (B2)

## Why
将认证后端抽象为 `IdentityProvider` 链，为后续真实 LDAP/OAuth/SAML 接入留出零侵入扩展点。
当前唯一真实实现仍是 LocalDb BCrypt；LDAP/OAuth 仅占位（`Optional.empty()`），通过 feature flag 默认关闭。

## Scope
- NEW `com.rainier.auth.idp.IdentityProvider` 接口
  - `String name()`
  - `Optional<UserIdentity> authenticate(String loginName, String password)`
- NEW value object `com.rainier.auth.idp.UserIdentity`
  - 字段：externalId / loginName / displayName / email / groups(List<String>)
- NEW `LocalDbIdentityProvider` `@Component` `@Primary` `@Order(Ordered.HIGHEST_PRECEDENCE)`
  - 从 `AuthController` 搬迁 BCrypt 校验逻辑（保留 `realAuthEnabled` 行为不变）
- NEW `LdapIdentityProviderStub` `@Component` `@ConditionalOnProperty(app.auth.ldap.enabled=true)`（默认 false）
- NEW `OAuthIdentityProviderStub` `@Component` `@ConditionalOnProperty(app.auth.oauth.enabled=true)`（默认 false）
- `AuthController` 改为注入 `List<IdentityProvider>`，按 `@Order` 顺序串行 try；第一个返回非空者胜出
- 关键不变量：`realAuthEnabled=false` 时仍走 mock 直发 token（保留旧 dev 体验）

## OutOfScope
- 真实 LDAP/OAuth/SAML 协议接入（需外部服务，本切片仅占位）
- SSO 跳转 / 回调流程
- 多 provider 用户合并 / 自动建账

## Scenarios
- **TC-IDP-001** LocalDb provider：正确密码返回 `UserIdentity`
- **TC-IDP-002** LocalDb provider：错误密码 / 未知用户 / disabled → `Optional.empty()`
- **TC-IDP-003** Provider chain：第一个 provider empty、第二个返回 identity → 登录成功 + 颁 token
- **TC-IDP-004** 全部 provider 返回 empty → 401
- **TC-IDP-005** LdapStub / OAuthStub 默认未启用（context 中无 bean）；启用后 `authenticate` 始终返回 empty
- **TC-IDP-006** 既有 `RealAuthLoginTest` 全部 6 用例不破

## Compatibility
- `realAuthEnabled=false`（dev profile）行为 100% 保留
- 现有 `AuthService.issueToken / parseUsername` 接口零改动
