# F5: AI 错误自动检测 + 24h 必上板 Banner

## What
- 复用 F1 已有的 reverse→AiError 自动公示链路（无需再改 service）。
- 新增 `AiErrorService.countOverdueOpen(thresholdHours)`，通过 repository 查询 status=OPEN 且
  occurredAt 早于 (now - thresholdHours) 的数量。
- 新增 `GET /api/ai/errors/overdue-count?hours=24` 端点（all-users readable，AdminPaths 已对
  `/api/ai/errors` GET 放行）。
- 新增前端 `AiErrorOverdueBanner` 组件，AppLayout 顶部 30s 轮询，count>0 即显示红条
  「N 个 AI 错误已超过 24h 未处理 [查看公示板]」，点击跳 `/ai/errors`。

## Why
飞轮"信任契约"的反面证据必须可见 — 当 AI 错误超 24h 仍 OPEN 时，所有用户都应当
被动看到提醒，强迫线下/线上处理；admin 一旦 markFixed 即降为 0，banner 消失。

## Scope
- Backend: AiErrorRepository.countByStatusAndOccurredAtBefore + Service.countOverdueOpen + Controller endpoint。
- Frontend: api/aiErrors.ts 加 fetchOverdueCount + 新组件 AiErrorOverdueBanner + AppLayout 接入。
- Tests: 3 个测试 (Service overdueCount、Reverse→AiError 已由 F1 覆盖、Banner 渲染)。

## OutOfScope
- AiError 状态过期自动 close（沉到表底即可，admin 手动 fix）。
- 多级告警（仅 24h 阈值）。
- IM 推送告警（站内 banner 足够）。

## Decisions
- 阈值通过 query 参数 hours 传入，默认 24，避免 magic constant 后续要再改。
- Banner 失败静默（与 NotificationBell 同策略）—— 不阻挡主流程。
- 不复用 NotificationService（这是计数广播，不是个人未读）。
