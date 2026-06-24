# Proposal: v0.0.50 — 任务状态中文标签

## Why

任务状态枚举（TODO/IN_PROGRESS/DONE/BLOCKED/CANCELLED）在界面上直接显示英文原值，对中文用户不友好。用户要求：给任务状态加中文、并展示中文。

## What Changes

- 前端 `api/task.ts` 新增共享 `TASK_STATUS_LABELS`（待办/进行中/已完成/阻塞/已取消）+ `TASK_STATUS_OPTIONS`。
- 所有任务状态展示处改显中文：TasksPage 列表状态 chip、TaskEditDrawer 状态下拉、工作台（Workbench）任务状态快改下拉、驾驶舱（Cockpit）待办任务状态下拉。
- 枚举的存储值（后端 TaskStatus 常量）不变 —— 仅展示层中文化（与既有 RequirementStatus/Priority/ProjectType 标签同范式：标签在前端）。

## Capabilities

- Modified: `frontend-scaffold`（任务状态展示中文）。
- New: 无。

## Impact

- 代码：前端 `api/task.ts` + `TasksPage.tsx` / `TaskEditDrawer.tsx` / `WorkbenchPage.tsx` / `CockpitPage.tsx`（去各自本地 options 数组，统一用共享常量）+ 测试。
- 后端：无（TaskStatus 常量值不变）。
- 数据：无。

## Success Criteria

- [ ] 任务状态在 列表/编辑/工作台/驾驶舱 均显示中文（如 TODO→待办）。
- [ ] 下拉提交的 value 仍为英文枚举（后端契约不变）。
- [ ] 前端全绿 + tsc/lint clean。
