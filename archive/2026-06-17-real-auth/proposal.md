# v0.0.38-real-auth — 真实凭证校验登录(替换 mock）

> Baseline: tag `v0.0.37-all-pages-polish` / commit 626275b. 关闭 2026-06-17 评审发现的 CRITICAL:
> mock 登录无凭证校验 → 任意用户名+任意密码都发 token → 可冒充任意管理员/负责人,架空 v0.0.21/24 所有授权。

## Why

`AuthController.login` 当前只校验 username/password 非空就 `issueToken(username)`——不查用户、不验密码。因为
`SecurityFilter` 信任 token 的 `sub`,任何人都能签一个"我是 alice"的 token。这是系统的头号安全洞。本版加真实
BCrypt 凭证校验,并补 `login_name` 唯一约束(否则 `findByLoginName` 歧义)。

## What Changes (backend; flag-gated)

- 新依赖 `spring-security-crypto`(仅 BCrypt,非全 Spring Security)+ `BCryptPasswordEncoder` bean。
- `User` 加 `password_hash` 可空列。`login_name` 唯一性:**仅 app 层**(`existsByLoginName`,尊重
  `@Where del_flag=0`)。**设计调整**:原计划加 DB unique 约束,但与软删冲突——`@SQLDelete` 让 deleteAll 只置
  del_flag=1,普通 unique 索引仍见软删行,会阻止"软删后重建同名登录"且拖垮测试。软删模型下 app 层唯一性是正确
  做法;findByLoginName 经 @Where 只返回 active 用户 → 无歧义。
- `AuthController.login`:当 `app.security.real-auth.enabled=true` → `findByLoginName` + `encoder.matches`
  校验;未知用户/错密码 → **401**;非空校验(400)不变;通过则发 token。flag 关时保留 mock(任意凭证)。
- `UserService.create`:set passwordHash = encode(req.password ?? 默认密码);`UserCreateRequest` +可选 `password`。
- `RealAuthPasswordBackfill`(@Order HIGHEST CommandLineRunner,gated on real-auth.enabled):启动给所有
  `password_hash` 为空的用户回填 `app.security.default-password`(默认 `rainier123`,BCrypt)→ 存量/demo 用户照常能登。
- 配置:`application.yml`(real-auth.enabled: true, default-password: rainier123);`application-test.yml`
  (real-auth.enabled: false)→ 既有 login 测试(任意密码)零改动。

## Capabilities

- Modified: `auth-placeholder`(真实凭证校验 + password_hash) + `entity-user`(password_hash + login_name unique)。

## Impact

- 后端:pom + `User`/`UserCreateRequest`/`UserService` + `AuthController` + 新 `PasswordConfig` /
  `RealAuthPasswordBackfill` + application(-test).yml。`RealAuthLoginTest`(flag on)+ backfill test。
- **demo 行为变更**:登录不再"任意密码"——存量用户用默认密码 `rainier123`(可配)。
- 显式排除:改密/重置端点、找回、SSO/真实身份源、token 刷新(后续)。

## Success Criteria

- [ ] flag on:`findByLoginName`+BCrypt 校验;错密码/未知用户 → 401;正确 → 200 发 token。
- [ ] flag off(test):login 行为与今天一致(既有测试全绿)。
- [ ] 启动回填给无密码用户设默认密码(BCrypt);幂等。
- [ ] `login_name` DB unique;create 重名仍 409。
- [ ] backend 全绿 + 新测试;E2E:错密码 401 / 默认密码 200 / 存量数据除密码哈希外不变。
