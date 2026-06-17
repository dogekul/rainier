# Design — v0.0.21-admin-authz

> Baseline: tag `v0.0.20-role-nav` / commit a15ef66. backend 354 + frontend 106 green, 19 表.

## Context

`SecurityFilter` 今天只在 `/api/auth/me` 校验 token；其余 20 个 `/api/*` 端点零鉴权（连 token 都不要）。
v0.0.20 的角色分级只在前端生效。本版加后端 admin 端点鉴权（token + 提升态），提升态复用 `Role.adminAccess`。
约束：Java 8 / Spring Boot 2.7；既有 ~39 个 admin 控制器测试无 token 直接写（开关门控避免改动）；mock JWT 占位。

## Decisions

### D1 — 机制：HandlerInterceptor + WebMvcConfigurer
- **方案**：`AdminAuthorizationInterceptor implements HandlerInterceptor`，`preHandle` 做鉴权；`WebMvcConfig
  implements WebMvcConfigurer` 用 `addInterceptors().addPathPatterns("/api/**")` **广注册**，分级判定全部交给
  `AdminPaths.requiresAdmin(path, method)` 单一来源（避免路径列表在配置与 AdminPaths 两处重复）。
  preHandle 用 `UrlPathHelper.getLookupPathForRequest`（与 DispatcherServlet 路由同一 lookup path，剥 `;...`）匹配，
  防 matrix-param 绕过。
- **为什么**：路径模式匹配清晰；拦截器是 Spring bean，可注入 `ElevationService`/`AuthService`；与既有
  `SecurityFilter`（servlet 层）解耦，不动 me() 的过滤逻辑。
- **备选（排除）**：扩展 `SecurityFilter`（servlet filter，路径判断要手写、与 me() 逻辑耦合）；Spring Security
  （引入完整框架，超本版范围）。

### D2 — 端点分级 Tier A / Tier B（Gate 1 选定）
- **Tier A（全方法仅管理员）**：`/api/organizations`、`/api/user-organizations`、`/api/positions`、`/api/roles`、
  `/api/user-roles`、`/api/audit-logs`、`/api/products`。分级由 `AdminPaths.TIER_A`/`TIER_B` 常量 + `requiresAdmin`
  决策（非在 WebMvcConfig 逐路径注册）。
- **Tier B（写仅管理员，GET 放行）**：`/api/users`、`/api/features`、`/api/product-modules`。拦截器内对这三个前缀
  仅当方法 ∈ {POST,PUT,PATCH,DELETE} 才要求提升态；GET/HEAD/OPTIONS 放行。
- **依据**：探明全员页面读取 `users`(6 处 owner/assignee 下拉) / `features`+`product-modules`(SprintFeaturePanel)。
- **OPTIONS** 一律放行（CORS 预检）。

### D3 — 鉴权流程与状态码
- `preHandle`：
  1. `OPTIONS` → 放行。
  2. 判定该 (path, method) 是否需要 admin（Tier A 全方法 / Tier B 仅写）；不需要 → 放行。
  3. 取 `Authorization: Bearer`；无 / 解析失败 → 抛 `UnauthorizedException` → **401**。
  4. `ElevationService.isElevated(username)` 为 false → 抛 `ForbiddenException` → **403**。
  5. 通过 → 放行（可选 set username 请求属性，本版不动审计语义，故**不 set**）。
- 抛异常经 `GlobalExceptionHandler`（拦截器抛的异常会被 `@ExceptionHandler` 捕获，输出统一 JSON）。
- 新 `ForbiddenException` + handler `body(HttpStatus.FORBIDDEN, msg, null)`。

### D4 — ElevationService（提升态单一来源）
- **方案**：`isElevated(String username)`：`userRepo.findByLoginName` → null 则 false；否则
  `userRoleRepo.findByUserId` 的 roleIds → `roleRepo.findAllById` 任一 `getAdminAccess()==true`。
  纯读、`@Transactional(readOnly=true)`。复用既有 repo（与 MeService 同款 join，但只判 boolean）。
- **为什么**：与前端 `isElevated`（role-based）同源；不引入 user 级 admin 配置 / 不改 me()。

### D5 — 开关门控 + 测试策略（Gate 1 选定）
- `app.security.admin-authz.enabled`：`application.yml` = `true`；`application-test.yml` = `false`。
- 拦截器 `preHandle` 首行：`if (!enabled) return true;`（门控关 → 整个拦截器 no-op）。
- 既有 ~39 个 admin 控制器测试在 test profile（false）下零改动全绿。
- 新 `AdminAuthorizationTest`：`@SpringBootTest @AutoConfigureMockMvc @TestPropertySource(properties=
  "app.security.admin-authz.enabled=true")`（独立 context），seed admin 用户/角色 + 非 admin 用户/角色，
  issueToken，断言 401/403/200 + Tier B GET 放行 + 全员端点放行。

### D6 — Bootstrap 防自锁（Gate 1 选定）
- **方案**：`AdminAuthzBootstrap implements CommandLineRunner`（`@Order(HIGHEST_PRECEDENCE)`，仅当
  `admin-authz.enabled=true` 时生效）：若 `roleRepo` 无任一 `adminAccess=true`，找 `code =
  app.security.bootstrap-admin-role`（默认 `PMO`）的角色，native UPDATE `admin_access=true`（绕过 @SQLDelete）+ INFO 日志。
- **为什么**：授权一开，「设第一个 admin」(`PUT /api/roles`) 本身要 admin → 死锁；且若误把所有角色降为非 admin
  会永久自锁。该 runner 幂等（仅无 admin 时动）、防永久锁、有日志。
- **数据影响**：首启把 PMO.admin_access NULL→true（合法首管理员 seed）。test profile 门控关 → runner 不跑（不干扰既有测试）。

## Architecture

```
请求 → AdminAuthorizationInterceptor.preHandle
  ├─ !enabled → 放行
  ├─ OPTIONS → 放行
  ├─ requiresAdmin(path, method)? 否 → 放行（全员端点 / Tier B 的 GET）
  ├─ 解析 Bearer token：无/失效 → 401
  ├─ ElevationService.isElevated(username) == false → 403
  └─ 放行 → Controller
启动: AdminAuthzBootstrap（enabled 时）→ 无 admin 角色则提升 PMO
```

## Risks / Trade-offs

| 风险 | 等级 | 缓解 |
|---|---|---|
| 授权死锁（无人能设首个 admin） | 🔴 | D6 bootstrap runner 自动提升 PMO |
| 误伤全员端点（/pm、/api/users 下拉） | 🔴 | Tier B GET 放行 + AdminAuthorizationTest 显式验证全员端点/Tier B GET 200 |
| 39 个既有测试因鉴权全红 | 🔴 | D5 开关门控（test profile false），鉴权由专用测试 + E2E 覆盖 |
| 门控关 → 鉴权零真实测试覆盖（覆盖真空 j） | 🟡 | AdminAuthorizationTest 置 enabled=true 跑真实拦截器 + E2E 真实 profile |
| 拦截器抛异常不被 GlobalExceptionHandler 捕获 | 🟡 | 验证：Spring 中拦截器 preHandle 抛的异常会进 @ExceptionHandler（HandlerExceptionResolver）；测试断言 401/403 JSON |
| bootstrap 改存量 PMO 数据 | 🟡（有意）| 仅无 admin 时、幂等、日志、Gate 1 已确认；E2E 说明 |
| Tier B 前缀匹配误判（/api/users vs /api/user-roles/user-organizations） | 🟡 | 精确前缀 + 边界（`/api/users` 与 `/api/user-roles` 是不同 path，addPathPatterns 用精确串 + `/**`）|
