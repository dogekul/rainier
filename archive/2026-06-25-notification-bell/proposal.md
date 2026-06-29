# Proposal: NotificationBell + /notifications 列表页 (F3, v0.0.102)

## What
- 新建 `frontend/src/api/notification.ts` —— list / markRead / markAllRead 三端点 + Notification 类型。
- 新建 `frontend/src/components/NotificationBell.tsx` —— 顶栏右侧铃铛 + 未读 badge + Drawer 最新 5 条 + 全部已读/查看全部。
- 新建 `frontend/src/pages/Notifications/NotificationsPage.tsx` —— 路由 `/notifications`，tabs 全部/未读/已读，每行标记已读 + 分页。
- AppLayout 顶栏在 username 之前挂载 `<NotificationBell />`；AppRoutes 注册 `/notifications`（all-users）。

## Why
A8 后端 `GET /api/me/notifications`、`POST /{id}/read`、`POST /read-all` 早已 ship，但前端零消费 —— RiskService 在 CRIT 时自动写通知却完全没人看到。F3 让飞轮的最后一英里（通知触达用户）真正闭环。

## Scope
- 站内通知（与既有 `/api/me/notifications` 同栈）。
- 30s 轮询拿未读条数；点击 bell 展开右侧 Drawer 显示最新 5 条；按 level (INFO/WARN/CRIT) 染色。
- 列表页 tabs / 分页 / 每行单独「标记已读」按钮（已读行 disable）。

## OutOfScope
- 真推送（仍是 polling）；邮件推送 D4 已有但本批不接入。
- 点击单条通知跳详情页（行内显示 entityType+id 文本即可，不带链接路由）。
- 退订 / 通知偏好设置。

## Decisions
1. 复用既有 `usePaginated` hook + `Pagination` 组件 + `Drawer` UI primitive，零新依赖。
2. NotificationBell 用 `useEffect` + `setInterval(30_000)` 轮询；卸载清理。badge 超 99 显示 "99+"。
3. /notifications 走 all-users（不放在 `isAdminPath` 列表），与后端 token-gated 对齐。
4. level → 颜色映射: INFO=gray, WARN=orange, CRIT=red。
5. AppLayout 在 `.rainier-shell-user` 之前插入 bell；保持既有 username span 不动。
