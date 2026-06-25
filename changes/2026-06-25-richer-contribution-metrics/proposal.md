# Proposal: C4 更丰富贡献指标（按状态/本周/4 周趋势）

## Problem
`GET /api/me/profile` 当前只返回 `ownedStoryCount` / `assignedTaskCount` 两个总计。
对「我的档案」与「下属档案」(C3) 来说，这两个数字看不出活跃度、看不出本周进展、看不出
质量分布（多少在 BLOCKED）。运营 + 团队负责人需要更细的贡献画像。

## Decision
扩展 `ProfileResponse` 的贡献区，新增一个嵌套 `contribution` 对象（保留顶层
`ownedStoryCount` / `assignedTaskCount` 不动以保前向兼容）。新增字段：

- `contribution.tasksByStatus`: `{TODO, IN_PROGRESS, DONE, BLOCKED, CANCELLED}` → 计数
- `contribution.storiesByStatus`: `{DRAFT, READY, IN_PROGRESS, DONE, BLOCKED, CANCELLED}` → 计数
- `contribution.tasksThisWeek`: 本周（按 ISO week，周一 00:00 UTC 起算）assigneeUserId=me
  的 task `createTime >= weekStart` 计数
- `contribution.tasksDoneThisWeek`: 本周 status=DONE 且 `updateTime >= weekStart` 的 task 计数
- `contribution.weeklyTrend`: 长度 4 的数组（最近 4 个 ISO 周，按时间升序），元素
  `{week: "2026-W26", tasksDone: n, storiesDone: n}` — task 用 assigneeUserId=me
  filter，story 用 ownerUserId=me filter，时间过滤 `updateTime >= weekStart && updateTime < weekEnd`

> ⚠️ status 键名采用既有常量（TaskStatus.IN_PROGRESS / StoryStatus.IN_PROGRESS），而非 plan
> 里写的 "DOING"，避免与系统其余部分割裂。

## Service 重构
- 新增 `ContributionMetricsService`（包 `com.rainier.me.service`），消费 `TaskRepository` /
  `StoryRepository` 计数方法。`MeProfileService.aggregate(User)` 末尾调用它装填
  `ProfileResponse.contribution`。
- 仍保留 `ownedStoryCount` / `assignedTaskCount` 字段（前端老代码不会断）。

## Repo 新增
- TaskRepository
  - `long countByAssigneeUserIdAndStatus(Long, String)`
  - `long countByAssigneeUserIdAndCreateTimeGreaterThanEqual(Long, Instant)`
  - `long countByAssigneeUserIdAndStatusAndUpdateTimeGreaterThanEqual(Long, String, Instant)`
  - `long countByAssigneeUserIdAndStatusAndUpdateTimeBetween(Long, String, Instant, Instant)`
- StoryRepository
  - `long countByOwnerUserIdAndStatus(Long, String)`
  - `long countByOwnerUserIdAndStatusAndUpdateTimeBetween(Long, String, Instant, Instant)`

## Non-Goals
- 项目维度细分（按 projectId 分组）
- 团队对比 / 排行榜
- 时区可配置（统一 UTC，ISO-8601）
- 写端点
- 历史 4 周以前的趋势

## Compatibility
- 既有 `ownedStoryCount` / `assignedTaskCount` 字段保留
- 既有 `MeProfileControllerTest` 全部 8 用例必须全绿
- C3 `UserProfileController` 自动获得 contribution（因走同一 `profileOfUserId`）
- 前端目前不依赖 contribution 字段，本次后端 only
