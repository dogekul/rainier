# Spec: audit-actor-realname (B6)

## Scenario AUDIT-ACTOR-REAL-001 — 带 token 的写操作审计 actor = loginName
- Given `SecurityFilter` 解析出 token 的 subject = "alice"
- When 调用任一 `*Service.create/update/delete`
- Then 新审计行 actor = "alice"

## Scenario AUDIT-ACTOR-REAL-002 — 无 token / 无 HTTP 上下文降级 system
- Given 没有 token、没有 request attribute
- When `*Service.create` 执行（如后台任务 / TransactionTemplate 内）
- Then 新审计行 actor = "system"

## Scenario AUDIT-ACTOR-REAL-003 — RequestUserContext.clear() 在 filter 末执行
- Given 一次 HTTP 请求 set("alice")
- When 请求结束
- Then `RequestUserContext.get()` 返回 null（不污染线程池后续请求）

## Out-of-scope
- 不改 audit_log schema 或字段名。
- 不动 AuditorAwareImpl 的 BaseEntity 行为。
