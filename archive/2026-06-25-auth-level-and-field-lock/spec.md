# spec: auth-level-and-field-lock

capability: auth-level-and-field-lock
version: v0.0.69

## Scenario 1 — 设置 AI 授权级别并回显

GIVEN: 用户 alice 已登录（持有有效 JWT），且尚未显式设置 aiAuthLevel
WHEN: 调用 `GET /api/auth/me`
THEN: 返回 `aiAuthLevel = "BASIC"`（默认值，由 getter 派生）
AND:  调用 `POST /api/me/ai-auth-level` body `{"level":"DEPTH"}`
THEN: 返回 200；User 行被持久化 aiAuthLevel="DEPTH"
AND:  再次 `GET /api/auth/me` 返回 `aiAuthLevel = "DEPTH"`
AND:  非枚举值 `{"level":"GOD"}` 返回 400 BadRequest

## Scenario 2 — 字段锁的幂等登记 / 解除 / 查询

GIVEN: 仓库为空
WHEN: 调用 `FieldLockService.lock("TASK", 42, "status", "USER")`
THEN: 返回新的 FieldLock 行，lockedAt 由 @CreationTimestamp 写入
AND:  对同一 (TASK, 42, status) 再次 `lock(...)` 不抛错，不创建新行（幂等，返回已存在的行）
AND:  `listFor("TASK", 42)` 返回 1 条
AND:  `unlock("TASK", 42, "status")` 后 `listFor` 返回 0 条
AND:  对未登记的 (TASK, 42, status) 再 unlock 不抛错（幂等）

## Scenario 3 — field-locks HTTP 端点（全员）

GIVEN: 已登录用户
WHEN: `POST /api/field-locks` body `{"entityType":"TASK","entityId":1,"fieldName":"title","lockedBy":"USER"}`
THEN: 返回 201，body 含 id + lockedAt
WHEN: `GET /api/field-locks?entityType=TASK&entityId=1`
THEN: 返回数组含上一条记录
WHEN: `DELETE /api/field-locks?entityType=TASK&entityId=1&fieldName=title`
THEN: 返回 204
