# Spec — H5 架构师角色落地页

## Scenarios

### TC-ARCHSTATS-001: 统计计数仅看到我自己
**Given** alice 是 2 个 PENDING Story 的 reviewer，bob 是另一个 PENDING Story 的 reviewer
**When** alice 调用 `GET /api/me/review-stats`
**Then** 返回 `pendingStoryCount = 2`（不包含 bob 的）

### TC-ARCHSTATS-002: pendingTaskCount 单独计数
**Given** alice 是 1 个 PENDING Story 和 3 个 PENDING Task 的 reviewer
**When** alice 调用 `GET /api/me/review-stats`
**Then** 返回 `pendingStoryCount = 1`、`pendingTaskCount = 3`

### TC-ARCHSTATS-003: 本周通过/打回计数
**Given** alice 本周已经 review 了 2 个 Story（APPROVED）+ 1 个 Task（REJECTED）
**When** alice 调用 `GET /api/me/review-stats`
**Then** 返回 `approvedThisWeek = 2`、`rejectedThisWeek = 1`

### TC-ARCHSTATS-004: 无 token → 401
**Given** 未带 Authorization header
**When** 调用 `GET /api/me/review-stats`
**Then** 返回 401

### TC-ARCHUI-001: 落地页渲染 4 张统计卡
**Given** 已登录用户访问 `/architect`
**When** 页面挂载，`/api/me/review-stats` 返回 `{pendingStoryCount: 3, pendingTaskCount: 5,
approvedThisWeek: 7, rejectedThisWeek: 2}`
**Then** 页面渲染 4 张数字卡片：3 / 5 / 7 / 2

### TC-ARCHUI-002: 落地页含 Story/Task tabs（占位测试）
**Given** 已登录用户访问 `/architect`
**When** stats + pending-reviews 都返回
**Then** 页面渲染 `architect-tab-story` / `architect-tab-task` 两个 tab 按钮

## OutOfScope
- 真·`reviewedAt` 持久化
- 架构师角色权限校验（暂不存在 architect 角色）
