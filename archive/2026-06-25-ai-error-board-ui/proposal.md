# Proposal — AI 错误公示板前端 (A9, v0.0.73)

## What
为 A4 已上线的后端 `/api/ai/errors`（AI 错误公示板）补一个前端页面。新增 `frontend/src/api/aiErrors.ts` + `frontend/src/pages/AiError/AiErrorsPage.tsx`，并在侧边栏「AI」分组追加「错误公示板」入口（`/ai/errors`，all-users 可见）。

## Why
飞轮的「信任契约」要求把 AI 的失误公开展示给所有用户；admin 才能登记修复。后端 A4 已完成，缺前端就是「黑箱」，与产品价值不符。

## Scope
- NEW `frontend/src/api/aiErrors.ts`：`listAiErrors`、`fixAiError`，类型 `AiError` / `AiErrorStatus`。
- NEW `frontend/src/pages/AiError/AiErrorsPage.tsx`（+ `index.ts`）：列表 + 状态过滤 + 「标记修复」按钮（仅 admin 用 `isElevated` 判定）。
- 修改 `frontend/src/components/AppLayout.tsx`：「AI」组追加「错误公示板」`/ai/errors`。
- 修改 `frontend/src/AppRoutes.tsx`：注册路由。
- NEW vitest：mock api，渲染并断言标题、行、修复按钮可见性。

## OutOfScope
- 错误详情页（列表字段已经够；后端 `GET /{id}` 暂不接）。
- 推送通知（用户主动看，无需 push channel）。
- 创建错误的 UI（仅由后端业务流程录入，A4 OutOfScope 已确认）。

## Decisions
- 复用 `DashboardCard` / `StatTiles` / `StatusChip` / `EmptyState` 风格，与 AiWorkLogsPage 一致。
- 「标记修复」走 Drawer 输入 `fixAction`（必填，后端 `@NotBlank`），与 AiWorkLog 驳回理由同形态。
- 列表入口对 all-users 可见（后端 GET 是 all-users），但 admin 才看得到「标记修复」按钮（前端用 `isElevated`，后端 `AdminPaths` TIER_B 兜底）。
