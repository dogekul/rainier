# Capability: frontend-scaffold

> MODIFIED by `changes/2026-06-11-project-type` (v0.0.16-project-type, 2026-06-11):
> ProjectsPage 新建/编辑抽屉加「项目类型」下拉(默认 轻量)、表格加「类型」列(中文 轻量/正式)、
> 表格上方加「类型」过滤下拉。无新增页面/路由/Sider 组。
> 既有 v0.0.8–v0.0.15 Requirements 全部保留不变;此文件仅列本次 ADDED Requirements。

## ADDED Requirements (from change 2026-06-11-project-type / v0.0.16)

### Requirement: ProjectsPage 项目类型下拉（新建/编辑抽屉）

前端 SHALL 在 ProjectsPage 的新建/编辑抽屉提供「项目类型」下拉(`projects-type-select`)，选项为 轻量(CASUAL) / 正式(FORMAL)，新建时默认选中 轻量，编辑时回显该项目的 projectType。

#### Scenario: 新建抽屉含项目类型下拉且默认轻量

- **GIVEN** 用户在 ProjectsPage 点击「新建项目」打开抽屉
- **WHEN** 抽屉渲染完成
- **THEN** 抽屉 SHALL 含「项目类型」下拉(`projects-type-select`)
- **AND** 下拉选项 SHALL 含「轻量」与「正式」
- **AND** 新建时默认值 SHALL 为「轻量」(CASUAL)

#### Scenario: 提交携带 projectType

- **GIVEN** 用户打开新建抽屉并把「项目类型」选为「正式」
- **WHEN** 用户填妥必填项并点击「保存」
- **THEN** SHALL 调用 `createProject` 且 body 含 `projectType: "FORMAL"`

### Requirement: ProjectsPage 表格类型列

前端 SHALL 在 ProjectsPage 表格展示「类型」列，按 projectType 显示中文 轻量/正式。

#### Scenario: 表格渲染类型列中文

- **GIVEN** `listProjects` 返回一行 `projectType="FORMAL"`
- **WHEN** ProjectsPage 渲染完成
- **THEN** 表格 SHALL 含「类型」列
- **AND** 该行类型列 SHALL 显示「正式」

### Requirement: ProjectsPage 按类型过滤

前端 SHALL 在 ProjectsPage 表格上方提供「类型」过滤下拉(`projects-type-filter`，含「全部」)；选择某类型 SHALL 以 `projectType` 参数重新查询列表。

#### Scenario: 选择类型过滤触发带参查询

- **GIVEN** ProjectsPage 已渲染
- **WHEN** 用户在「类型」过滤下拉选择「正式」
- **THEN** SHALL 调用 `listProjects` 且 params 含 `projectType: "FORMAL"`
