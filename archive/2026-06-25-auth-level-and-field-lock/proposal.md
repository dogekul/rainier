# A5 AI 分级授权 + 字段锁 (v0.0.69)

## What
- User 新增 `aiAuthLevel` 列（nullable，getter null→"BASIC"），三档：BASIC / INTERMEDIATE / DEPTH。
- 新增 entity `rainier_field_lock`（entityType + entityId + fieldName + lockedBy + lockedAt），唯一约束 (entityType, entityId, fieldName)。
- 新增 `POST /api/me/ai-auth-level` 持久化当前用户授权级别；`MeResponse` 加 `aiAuthLevel` 字段。
- 新增 `GET/POST/DELETE /api/field-locks`（all-users），登记/查询/解除字段锁。

## Why
飞轮契约的第三层：用户必须能配置「让 AI 改到哪一层」，并对单字段加锁（即使授权 DEPTH 也不能改某字段）。这是后续 A6/A7 AI Agent 实际执行前的硬约束基座。本版仅落库 +查询，不在写路径强制（下一批 A6/A7 再查锁拦截）。

## Scope
- backend: User entity +column, FieldLock entity+repo+service, controllers, MeResponse 字段, AdminPaths（无变更，全员端点）。
- 测试: FieldLockServiceTest (幂等 lock / unlock / listFor), AuthLevelTest (持久化 + me() 回显)。

## OutOfScope
- 实际拦截 AI 改字段（A6/A7）。
- 字段锁的前端管理 UI。
- aiAuthLevel 默认值的 DB 默认（依赖 getter null→BASIC）。

## Decisions
- aiAuthLevel 用 String（不是 enum），与现有 status/linkType 风格一致；常量集中在 `AiAuthLevel.java`。
- field-locks 端点全员，每个用户可锁定任何字段（不区分所有者），简化模型；后续若需所有者校验再加。
- FieldLock 不软删——unlock 直接物理 delete（锁本身是瞬态状态，不需要审计）。
