# Slices — v0.0.21-admin-authz

| # | 优先级 | TC 覆盖 | 实现目标 | 依赖 |
|---|---|---|---|---|
| S01 | P0 | (基座) | `ForbiddenException` + `GlobalExceptionHandler` +403 handler | 无 |
| S02 | P0 | (基座) | `ElevationService.isElevated(username)`（复用 user/userRole/role repo） | 无 |
| S03 | P0 | TC-AUTHZ-* | `AdminPaths`(Tier A/B 常量 + requiresAdmin(path,method)) + `AdminAuthorizationInterceptor`(preHandle: 门控/OPTIONS/token/elevation) | S01,S02 |
| S04 | P0 | TC-AUTHZ-* | `WebMvcConfig` 注册拦截器到 Tier A+B 路径; `application.yml` +admin-authz.enabled=true +bootstrap-admin-role=PMO; `application-test.yml` +admin-authz.enabled=false | S03 |
| S05 | P0 | TC-BOOT-* | `AdminAuthzBootstrap`(CommandLineRunner, enabled 时, 无 admin → 提升 PMO native UPDATE + INFO) | S02 |
| S06 | P0 | TC-AUTHZ-001..011 | `AdminAuthorizationTest`(@TestPropertySource enabled=true; seed admin/非admin 用户角色 + token; 401/403/200 + Tier B GET + 全员 + OPTIONS) | S03,S04 |
| S07 | P0 | TC-BOOT-001..003 | `AdminAuthzBootstrapTest`(enabled=true; 提升/幂等/无PMO安全) | S05 |
| S08 | P0 | TC-AUTHZ-GATE-OFF | 跑全量 354 确认门控关零改动全绿 | S04 |
| S09 | P0 | TC-E2E-AUTHZ-* | E2E docker 重建 + bootstrap + 401/403/200 + Tier B GET + 全员 + 存量(除 seed) | 全部 |

拓扑批次：
- 批次1（并行）：S01 / S02
- 批次2：S03（依 S01,S02）
- 批次3：S04 / S05（依 S03/S02）
- 批次4：S06 / S07（依 S04/S05）
- 批次5：S08 → S09

陷阱预警：
- 拦截器 preHandle 抛异常须经 GlobalExceptionHandler（Spring HandlerExceptionResolver 会处理 HandlerMethod 关联的异常）→ 测试断言 401/403 JSON；若不走则 preHandle 直接 `response.sendError` 兜底。
- Tier B 仅写收口：preHandle 内 `isMutating(method)` 才查 elevation；GET/HEAD/OPTIONS 放行。
- WebMvcConfig 广注册 `/api/**`，分级由 `AdminPaths.requiresAdmin(path, method)` 单一来源决策；preHandle 用 `UrlPathHelper` lookup path（剥 `;...`）防 matrix-param 绕过。`/api/users` 与 `/api/user-roles`/`/api/user-organizations` 经 `matches()` 边界（equals||startsWith(base+"/")）不串味。
- 门控关：preHandle 首行 `if(!enabled) return true;`；bootstrap runner 也门控（enabled 时才跑）→ test profile 不跑不干扰既有。
- bootstrap native UPDATE 绕过 @SQLDelete（同 ProjectTypeBackfill 模式），@Transactional。
- Java 8：无 Set.of → `Collections.unmodifiableSet(new HashSet<>(Arrays.asList(...)))`。
- AdminAuthorizationTest 是独立 context（@TestPropertySource）→ 不污染门控关的共享 context。
