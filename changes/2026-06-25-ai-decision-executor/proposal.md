# F1: AiWorkLog 采纳真正回写 Task + Undo (v0.0.100)

## What
ACCEPTED 决策不再只翻 status —— 通过 DecisionExecutor 真正回写实体（首个：UPDATE_TASK_STATUS → Task.status=DONE），并将执行前快照写入 AiWorkLog.reverseSnapshot；新增 POST /api/ai-work-logs/{id}/reverse 一键 Undo，复活到 PROPOSED 并自动公示 AiError(OPEN)。

## Why
35 个 stub 的总开关：当前用户每采纳一条 AI 建议还要再手动改一次 Task，飞轮空转、KPI（采纳率/驳回率/回滚率）全部失真。让 ACCEPTED 真正落地是闭环的命门。

## Scope
- AiWorkLog +reverseSnapshot @Lob String / reversedAt / reversedBy（均 nullable）
- DecisionExecutor 接口 + UpdateTaskStatusExecutor 实现（按 action 字段匹配）
- AiWorkLogService.decide：ACCEPTED 时找 executor 执行并写 snapshot；REJECTED 行为不变
- POST /api/ai-work-logs/{id}/reverse：必须 ACCEPTED 且 snapshot 非空 → 调 executor.reverse → 翻回 PROPOSED → 自动 record AiError(OPEN)
- Executor 抛异常 → status 仍变 ACCEPTED 但 snapshot=null（不阻断决策）

## OutOfScope
- 其他 action 类型（SUGGEST_ASSIGNEE / FLAG_RISK / DRAFT_WEEKLY）的 executor
- 软删除 AiError；reverse 后再次 ACCEPT 的二次执行
- 前端 Undo 按钮 UI

## Decisions
1. reverseSnapshot 用 @Lob String 存 JSON（{taskId, oldStatus, newStatus}），便于将来扩展其他实体字段。
2. Executor 列表按构造注入 + supports(log) 路由，不做注册中心。
3. reverse 总是自动 record 一条 AiError（errorDesc="user reversed ACCEPTED ai work log"）—— 公开化飞轮 KPI 的关键反向证据。
