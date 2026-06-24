# Capability: frontend-scaffold — v0.0.48 project-types delta (MODIFIED)

> 合并入 canonical `specs/frontend-scaffold/spec.md`（Phase 6）。ProjectsPage 四类型 + DeliveryFlow 立项「创建或关联对外-交付」。见 [[entity-project]] / [[opportunity]]。

## MODIFIED Requirements (from change 2026-06-23-project-types / v0.0.48)

### Requirement: ProjectsPage 四种项目类型 + 共享类型常量

`api/project.ts` SHALL 导出共享 `PROJECT_TYPE_OPTIONS`(=[CASUAL,CORE_FEATURE,CORE_TECH,EXTERNAL_DELIVERY]) 与 `PROJECT_TYPE_LABELS`
(轻量/主业-功能建设/主业-技术改造/对外-交付)。ProjectsPage 新建/编辑类型下拉与列表标签 SHALL 用该共享常量、呈现 4 个中文选项。

#### Scenario: 类型下拉显示四类

- **WHEN** 打开 ProjectsPage 新建抽屉
- **THEN** `projects-type-select` SHALL 含 轻量/主业-功能建设/主业-技术改造/对外-交付 四个选项

### Requirement: DeliveryFlow 立项「关联或新建对外-交付项目」

DeliveryFlow 立项抽屉 SHALL **默认「新建」模式**（立项主流动作 = 为本次赢单新建一个交付项目；该默认 SHALL NOT 依赖既有项目数量
——真实使用中对外-交付项目会很多，按数量判断会长期埋没「新建」）。「关联已有」SHALL 作为次要选项一键可达。关联模式 SHALL 只列
`EXTERNAL_DELIVERY` 项目；新建模式 SHALL 收 code/name + **项目负责人**（默认当前用户/商机 PM，类型固定显示「对外-交付」）。提交 SHALL 调
`initiateOpportunity`（关联传 projectId / 新建传 projectCode+projectName+projectOwnerUserId）；失败 SHALL 展示后端 message。

#### Scenario: 立项关联已有对外-交付项目

- **GIVEN** 一个 INITIATION/WON 商机与一个 EXTERNAL_DELIVERY 项目
- **WHEN** 在立项抽屉选「关联已有」、选该项目、点移交
- **THEN** SHALL 以 `{projectId, decision:'PASS'}` 调 `initiateOpportunity`

#### Scenario: 立项新建对外-交付项目

- **GIVEN** 一个 INITIATION/WON 商机
- **WHEN** 切到「新建」、填 code/name、点移交
- **THEN** SHALL 以 `{projectCode, projectName, decision:'PASS'}` 调 `initiateOpportunity`

#### Scenario: 默认进入新建模式（与项目数量无关）

- **GIVEN** 任意数量的 EXTERNAL_DELIVERY 项目（含 0 个或很多个）
- **WHEN** 打开立项抽屉
- **THEN** SHALL 直接呈现新建表单（code/name/负责人），无需额外切换点击
- **AND** 「关联已有」SHALL 作为次要选项一键可切

#### Scenario: 立项失败展示后端原因

- **GIVEN** 新建项目编号已存在
- **WHEN** 提交立项
- **THEN** SHALL 展示后端返回的 message（而非通用错误串）
