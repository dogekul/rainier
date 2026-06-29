# Architect Landing Page (H5)

## 背景
C-路线图：架构师 0% 卡在「评审关系数据层不存在」；C1/C2 已建 Story+Task review
字段、me/pending-reviews 合并队列端点。差临门一脚：架构师角色级落地页。

## 范围
1. `GET /api/me/review-stats` → 返回 `{pendingStoryCount, pendingTaskCount,
   approvedThisWeek, rejectedThisWeek}`。本周指基于 `updateTime` 的「从本周一 00:00
   UTC 起」窗口。
2. NEW frontend page `/architect`（ArchitectDashboardPage）：4 张统计卡 + 待评审清单
   （内嵌 `ReviewsPage` 风格 Story/Task tabs）+「最近决定」List（最多 10 条已决定
   的 Story+Task）。
3. AppRoutes 注册 `/architect`，AppLayout 在「工作台」组下加 nav 项「架构师」（全用户可见）。
4. 测试覆盖：`MeReviewStatsControllerTest`（4 用例：身份隔离 / 待评审计数 / 本周计数
   / 401）+ `ArchitectDashboardPage.test.tsx`（render + stats 显示）。

## OutOfScope
- 架构 ADR 文档管理
- 评审历史导出
- review SLA / 提醒
- 真·`reviewedAt` 字段（用 `updateTime` 近似；要更精确需 Story/Task schema 加列，
  下个迭代）

## Commit
`feat(architect-landing-page): H5 架构师角色落地页 (v0.0.112)`
