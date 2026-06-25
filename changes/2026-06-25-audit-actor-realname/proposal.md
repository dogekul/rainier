# B6 审计 actor 改为真实用户名

## 背景
- `AuditAspect` 依赖 `AuditorAware<String>` 读取 actor。
- 现状 `AuditorAwareImpl` 仅从 HttpServletRequest 读取 `rainier.username` 属性。在没有 HTTP 上下文（异步任务、scheduled job、line-level service 调用）时降级为 "system"。
- B6 要求：actor 一律为真实 loginName（通过 JWT 解析获得）；无上下文时降级 "system"。

## 范围
1. 新增 `com.rainier.auth.RequestUserContext` ThreadLocal helper：
   - `get()` / `set(String)` / `clear()`
   - 不依赖 HTTP / Spring，纯静态。
2. `SecurityFilter` 在解析出 token 后 `set`，filter end `finally` 中 `clear`。
3. `AuditAspect.record(...)` 优先用 `RequestUserContext.get()`，否则回落到既有 `AuditorAware`，再回落 "system"。

## OutOfScope
- 不改 `actor` 字段类型/字段名。
- 不改既有审计行。
- 不改 `AuditorAwareImpl`（保留 BaseEntity.createdBy/updatedBy 行为）。

## commit
- "feat(audit-actor-realname): B6 审计 actor 改真实用户名 (v0.0.79)"
