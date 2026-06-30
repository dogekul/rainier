# 下属面板入口 (H4)

## Why
团队/小组负责人需要一个集中入口查看自己的直接下属（作为组织 HEAD 管辖的成员），从这里跳转到下属档案做 1-on-1 / 绩效讨论。现状要求他们要么从 MyTeam（按项目导向）绕路、要么直接拼 `/users/{id}/profile` URL — 不友好。

## What
1. 新增 `GET /api/me/subordinates`：返回当前 user 作为 HEAD 的所有 org（任意层）下、`leftAt is null` 的成员（排除自己）；每条带上 `loginName / displayName / primaryOrgName / contributionSummary`（本周 task done 数 + 任务总数，复用 ContributionMetricsService）。
2. 新增页面 `/me/subordinates`（SubordinatesPage）：表格展示，行末「查看档案」按钮跳 `/users/{id}/profile`。
3. AppLayout 工作台分组新增「我的下属」入口，仅当 `me().leadTeams.length > 0` 时显示。
4. AppRoutes 注册 `/me/subordinates`。

## Out of scope
- 跨级下属递归（仅一级直管已覆盖 80% 场景）— 复用 `subordinatesOf(userId, includeIndirect=false)`。
- 批量调动 / 绩效分配 UI。
- 下属列表分页 / 搜索（后续按数据量决定）。
