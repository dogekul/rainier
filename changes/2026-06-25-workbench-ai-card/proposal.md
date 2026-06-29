# Proposal: workbench-ai-card (F4)

## What
工作台右栏新增 “AI 给我提的建议” 卡片：拉取最新 ≤3 条 PROPOSED AiWorkLog，提供 采纳 / 驳回（内联输入原因） / 撤销（采纳后短时间内可点）。evidence 含 eventId 时显示 `事件 #N (GITLAB)` 反查信息（仅文案，不跳转）。

## Why
A 批 9 个 sub-change 的 AI 提议除了 /ai/work-logs 列表外，普通用户在工作台没有任何入口。F1 已让“采纳真改 Task”可用，本批把这能力带到首页，让飞轮第一次对普通用户可见。

## Scope
- frontend: `api/aiWorkLog.ts` 新增 `listMyProposals` / `acceptWorkLog` / `rejectWorkLog` / `reverseWorkLog`
- frontend: 新增 `components/AiSuggestionCard.tsx` + `.css`
- frontend: `WorkbenchPage` 在右栏顶部插入 `<AiSuggestionCard />`
- 测试: 新增 `AiSuggestionCard.test.tsx`；扩展 `WorkbenchPage.test.tsx` 验证卡片可见

## OutOfScope
- 后端按用户过滤（AiWorkLog 暂无 targetOwnerUserId 字段）：本批先做 list-all-PROPOSED size=3
- evidence schema 规范化（按现有 JSON 拼字段）
- Undo 倒计时动画 / 真硬倒计时禁用：本批显示按钮，5 秒内点即可，过期由后端校验

## Decisions
- 后端 `GET /api/ai-work-logs?status=PROPOSED&size=3` 现有，无需新端点
- 撤销复用 F1 `/api/ai-work-logs/{id}/reverse`
- 驳回内联输入复用 E1 已经在 AiWorkLogsPage 用的形态（本卡片简化版：点驳回展开 textarea）
- 采纳成功后该条变为「已采纳」并保留 5 秒供撤销，5 秒后从卡片中消失（刷新即拉新一批）
