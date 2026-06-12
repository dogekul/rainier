# Capability: frontend-scaffold

> MODIFIED by `changes/2026-06-12-milestone` (v0.0.17-milestone, 2026-06-12):
> ProjectsPage 每行加「里程碑」按钮 → 行下内联展开 `MilestonesPanel`(照搬 SprintFeaturePanel 内联模式)，
> 面板内对该项目里程碑做 列表 / 新建 / 编辑 / 删除。新 `api/milestone.ts`。无新增页面/路由/Sider 组。
> 既有 v0.0.8–v0.0.16 Requirements 全部保留;此文件仅列本次 ADDED Requirements。

## ADDED Requirements (from change 2026-06-12-milestone / v0.0.17)

### Requirement: ProjectsPage 里程碑按钮 + 内联面板

前端 SHALL 在 ProjectsPage 每个项目行的操作区提供「里程碑」按钮；点击 SHALL 在该行下方内联展开/收起 `MilestonesPanel`(`milestones-panel-${projectId}`)。

#### Scenario: 点击里程碑按钮展开内联面板

- **GIVEN** ProjectsPage 已渲染，含项目行 id=7
- **WHEN** 用户点击该行的「里程碑」按钮
- **THEN** 页面 SHALL 渲染 `milestones-panel-7` 面板
- **AND** 面板 SHALL 调用 `listMilestones` 且 params 含 `projectId: 7`

### Requirement: MilestonesPanel 内联 CRUD

前端 SHALL 在 `MilestonesPanel` 列出该项目里程碑(按 sortOrder)，并提供内联新建/编辑表单(code/name/targetDate/status/actualDate/sortOrder)与删除。

#### Scenario: 面板列出该项目里程碑

- **GIVEN** `listMilestones({projectId:7})` 返回 2 个里程碑
- **WHEN** `MilestonesPanel` 渲染完成
- **THEN** 面板 SHALL 显示这 2 个里程碑的 name 与 status

#### Scenario: 面板新建里程碑携带 projectId

- **GIVEN** 项目 id=7 的 `MilestonesPanel` 已展开
- **WHEN** 用户填妥必填项（code/name/targetDate）并点击「新建里程碑」
- **THEN** SHALL 调用 `createMilestone` 且 body 含 `projectId: 7`

#### Scenario: 面板删除里程碑

- **GIVEN** 项目 id=7 的面板列出里程碑 id=11
- **WHEN** 用户点击该里程碑的删除并确认
- **THEN** SHALL 调用 `deleteMilestone` 且参数为 `11`
