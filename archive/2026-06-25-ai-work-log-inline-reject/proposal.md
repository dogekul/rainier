# Proposal: AI 工作日志驳回原因内联输入 (v0.0.97)

## 背景
AiWorkLogsPage 当前用 Drawer 收集驳回理由（v0.0.60 从 window.prompt 升级而来）。Drawer 仍然遮住列表上下文 — 用户看不到自己正在驳回哪一条 evidence。

## 目标
把"驳回"交互改为**内联**：点 `驳回` 在该行下展开 textarea + 确认/取消两按钮；确认才调 decision API，取消折回。

## 范围
- AiWorkLogsPage.tsx：移除 Drawer，用 row-展开式 inline form 替换
- 用项目现有 Button + native textarea（项目无 Mantine；保留 v0.0.60 的"必填校验"语义）
- 同时刻只能展开一行 reject 表单
- 调整 vitest 测试：模拟「点驳回 → 出现 textarea → 输入 → 提交 → 调 decideAiWorkLog(id, 'REJECTED', reason)」

## OutOfScope
- 驳回原因模板/快捷选项
- 后端 API 改动
