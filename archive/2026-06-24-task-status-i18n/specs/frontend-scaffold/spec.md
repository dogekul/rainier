# Capability: frontend-scaffold — v0.0.50 task-status-i18n delta (MODIFIED)

> 合并入 canonical `specs/frontend-scaffold/spec.md`（Phase 6）。任务状态展示中文。

## MODIFIED Requirements (from change 2026-06-24-task-status-i18n / v0.0.50)

### Requirement: 任务状态展示中文

`api/task.ts` SHALL 导出共享 `TASK_STATUS_LABELS`（TODO=待办 / IN_PROGRESS=进行中 / DONE=已完成 / BLOCKED=阻塞 /
CANCELLED=已取消）与 `TASK_STATUS_OPTIONS`。TasksPage 列表状态、TaskEditDrawer/工作台/驾驶舱 的任务状态下拉 SHALL 显示中文标签；
下拉提交值 SHALL 仍为英文枚举（后端契约不变）。

#### Scenario: 任务列表显示中文状态

- **GIVEN** 一个 status=TODO 的任务
- **WHEN** TasksPage 渲染
- **THEN** 状态 chip SHALL 显示「待办」，SHALL NOT 显示「TODO」

#### Scenario: 状态下拉显示中文、提交英文值

- **WHEN** 打开任务编辑抽屉的状态下拉
- **THEN** 选项 SHALL 显示中文（待办/进行中/已完成/阻塞/已取消）
- **AND** 选中提交的 value SHALL 仍为对应英文枚举
