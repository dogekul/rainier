# Capability: frontend-scaffold

> MODIFIED by `changes/2026-06-12-requirement-enrich` (v0.0.19, 2026-06-12):
> RequirementsPage 状态下拉改新 6 态(中文)、优先级下拉 5 级、加 expectedDate 输入;
> demand/story/task 页优先级下拉加「最低」(共用 Priority 第 5 级)。既有 Requirements 保留。

## ADDED Requirements (from change 2026-06-12-requirement-enrich / v0.0.19)

### Requirement: RequirementsPage 新状态 / 五级优先级 / 期望交付日期

前端 SHALL 在 RequirementsPage 的状态下拉提供新 6 态(草稿/审批中/分析中/实施中/已交付/已关闭)、优先级下拉提供 5 级(含「最低」)、并提供「期望交付日期」输入(YYYY-MM-DD),提交时透传 `expectedDate`。

#### Scenario: 状态下拉为新 6 态中文

- **GIVEN** 用户打开 RequirementsPage 新建/编辑抽屉
- **WHEN** 抽屉渲染
- **THEN** 状态下拉 SHALL 含「草稿」「审批中」「分析中」「实施中」「已交付」「已关闭」
- **AND** SHALL 不含旧值标签(如「评审中」「已批准」「已废弃」)

#### Scenario: 优先级下拉含「最低」

- **GIVEN** RequirementsPage 抽屉已渲染
- **WHEN** 查看优先级下拉
- **THEN** SHALL 含 5 个选项,包括「最低」

#### Scenario: 提交携带 expectedDate

- **GIVEN** 用户在新建抽屉填妥必填项并填「期望交付日期」= "2026-09-01"
- **WHEN** 点击保存
- **THEN** SHALL 调用 `createRequirement` 且 body 含 `expectedDate: "2026-09-01"`

### Requirement: demand/story/task 优先级含最低

前端 SHALL 在 demand/story/task 页的优先级下拉提供 5 级(含「最低」)。

#### Scenario: TasksPage 优先级下拉含最低

- **GIVEN** 用户打开 TasksPage 新建/编辑抽屉
- **WHEN** 查看优先级下拉
- **THEN** SHALL 含「最低」选项
