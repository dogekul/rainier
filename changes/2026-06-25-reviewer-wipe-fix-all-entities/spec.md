# Spec — reviewer-wipe-fix-all-entities (G2)

## 范围 Matrix

| 实体 | 有 reviewer 字段？ | UpdateRequest 有 set 标志？ | Service.update 走 patch-like？ | 测试 | 本 change 行动 |
|---|---|---|---|---|---|
| Story | 是 (v0.0.39) | 是 (v0.0.81) | 是 (v0.0.81) | StoryUpdateNoReviewerWipeTest | C1 已完成 |
| Task | 是 (v0.0.82) | 是 (v0.0.82) | 是 (v0.0.82) | TaskUpdateNoReviewerWipeTest | C2 已完成 — 本 change 仅再次确认绿 |
| Sprint | 否 | N/A | N/A | N/A | 跳过（无字段无风险） |
| Requirement | 否 | N/A | N/A | N/A | 跳过（无字段无风险） |

## Scenarios（Task — 既有 TaskUpdateNoReviewerWipeTest 覆盖）

### TC-G2-001 — PUT 不带 reviewer 字段 → 保留原值
- Given: Task 已设 reviewerUserId=R1, reviewStatus=PENDING
- When: PUT /api/tasks/{id} body 不含 reviewerUserId 与 reviewStatus 两个 key
- Then: 200 OK；DB 中 reviewerUserId=R1, reviewStatus=PENDING（未被清）

### TC-G2-002 — PUT 显式 null → 清空
- Given: Task 已设 reviewerUserId=R1, reviewStatus=PENDING
- When: PUT body 显式 reviewerUserId=null, reviewStatus=null
- Then: 200 OK；reviewerUserId / reviewStatus 都为 null

### TC-G2-003 — PUT 显式新值 → 替换
- Given: Task 已设 reviewerUserId=R1, reviewStatus=PENDING
- When: PUT body 显式 reviewerUserId=R2（不带 reviewStatus key）
- Then: 200 OK；reviewerUserId=R2，reviewStatus 仍 PENDING（key 缺省 → 保留）

## Patch-like 语义合同（所有有 reviewer 字段的实体共用）

| JSON 写法 | 语义 |
|---|---|
| key 缺省 | 保留 DB 原值 |
| `"reviewerUserId": null` | 清空 |
| `"reviewerUserId": 123` | 替换为 123（先校验用户存在） |

实现要点（Java 8 / Jackson）：
- DTO 字段加 private boolean xxxSet；
- setter 设值时同步 `this.xxxSet = true;`
- Service 仅在 `req.isXxxSet()` 时调用 `entity.setXxx(req.getXxx())`

## 未来扩展守则

若日后给 Sprint / Requirement / 其它实体加 reviewer 字段，**必须同时**：
1. 在 UpdateRequest 加 `reviewerUserIdSet` / `reviewStatusSet`
2. setter 内置 `this.xxxSet = true`
3. Service.update 用 `if (req.isXxxSet())` 守卫
4. 新增同款 `XxxUpdateNoReviewerWipeTest`（3 用例：keep / clear / replace）

## OutOfScope

- 给 Sprint/Requirement 新增 reviewer 字段
- POST create 路径（无歧义）
- 其它非 reviewer 但同样可能被 PUT 误清的字段（如未来引入的 watcher/cc 列表 — 另案处理）
