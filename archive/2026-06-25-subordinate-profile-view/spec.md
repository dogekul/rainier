# Spec: C3 subordinate-profile-view

## Endpoint (NEW)

### GET /api/users/{id}/profile

- Auth: token-gated（identity baseline）
- Path var `id`: 目标 user 的 id
- 响应体: 复用 `ProfileResponse`（与 /api/me/profile 完全一致）
- 鉴权矩阵：

| 条件 | HTTP |
|---|---|
| 缺 token / token 非法 | 401 |
| target id 在 `rainier_user` 找不到 | 404 |
| `caller.id == target.id` | 200 |
| caller 是 target 的「直接上级」（见下） | 200 |
| 其他 | 403 |

「直接上级」判定（caller 是否管 target）：
1. 取 target 的在岗 memberships（`leftAt IS NULL`），找 `isPrimary=true` 那条，没有则取列表第一条。无在岗 membership → 直接 403。
2. 记 `primaryOrg`。在 `primaryOrg` 的在岗 HEAD（排除 target 本人）中若有 `caller.id` → true。
3. 否则取 `primaryOrg.parentId`，若为 null → false；否则在父 org 的在岗 HEAD（排除 target 本人）中若有 `caller.id` → true。
4. 其他 → false。

只看 1 层 parent（OutOfScope: 多级上级递归）。

## Service 重构

`MeProfileService`：
- 新增 `public ProfileResponse profileOfUserId(Long userId)`，承载现有聚合逻辑。
- `profileOf(String username)` 改为：lookup user → 调 `profileOfUserId(user.id)`；找不到 user 时保留原 degraded 行为（loginName-only）。
- 新增 `public boolean isDirectManagerOf(Long callerUserId, Long targetUserId)`，封装上面的「直接上级」判定。

## Controller (NEW)

`UserProfileController`（`@RestController`，无类级 `@RequestMapping`）：
- `@GetMapping("/api/users/{id}/profile")` 
- 流程：
  1. `currentUsername(request)` → null/empty 抛 `UnauthorizedException`
  2. `User caller = userRepo.findByLoginName(username).orElse(null)`；caller==null → `ForbiddenException`（token 有效但无对应 user，不应能看别人档案）
  3. `User target = userRepo.findById(id).orElseThrow(() -> new NotFoundException(...))`
  4. 如 `caller.id == target.id` → 直接返回 `service.profileOfUserId(id)`
  5. 否则若 `service.isDirectManagerOf(caller.id, target.id)` → 返回 `service.profileOfUserId(id)`
  6. 否则抛 `ForbiddenException`

## Test Cases

- TC-SUBPROF-001: caller == target → 200 + 完整 profile（含 manager、贡献计数）
- TC-SUBPROF-002: caller 是 target 同 team 的 HEAD → 200 + body.userId == target.id
- TC-SUBPROF-003: caller 是 target primary org 父 org 的 HEAD → 200
- TC-SUBPROF-004: caller 与 target 同 team 但 caller 是 MEMBER → 403
- TC-SUBPROF-005: caller 与 target 在不同 team / 无上下级关系 → 403
- TC-SUBPROF-006: target 不存在 → 404
- TC-SUBPROF-007: 无 token → 401
- TC-SUBPROF-008: token sub 在 user 表无匹配 → 403（与 /api/me/profile 的 degraded 200 不同：不能用 ghost token 读他人档案）
- TC-SUBPROF-009: target 无在岗 membership → 仅 self/admin 可读；非 self → 403

## Out of Scope
- 多级上级（>1 层）
- HR / Admin 直通
- 前端页面
- 写操作
