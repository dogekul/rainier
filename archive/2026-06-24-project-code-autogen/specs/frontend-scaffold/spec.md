# Capability: frontend-scaffold — v0.0.49 project-code-autogen delta (MODIFIED)

> 合并入 canonical `specs/frontend-scaffold/spec.md`（Phase 6）。创建/立项表单去掉项目编号输入（编号自动生成）。见 [[entity-project]]。

## MODIFIED Requirements (from change 2026-06-24-project-code-autogen / v0.0.49)

### Requirement: 创建项目与立项表单去掉编号输入

ProjectsPage 新建/编辑抽屉 SHALL NOT 含项目编号输入框（编号自动生成）；列表/详情 SHALL 仍只读展示自动编号。DeliveryFlow 立项
「新建」表单 SHALL 仅含 名称 + 项目负责人（去掉编号输入），提交 SHALL 调 `initiateOpportunity({projectName, projectOwnerUserId, decision})`。

#### Scenario: 新建项目抽屉无编号输入

- **WHEN** 打开 ProjectsPage 新建抽屉
- **THEN** SHALL NOT 出现 `projects-code` 编号输入框
- **AND** 类型/名称/负责人输入 SHALL 仍在

#### Scenario: 立项新建只需名称与负责人

- **GIVEN** 一个 INITIATION/WON 商机
- **WHEN** 在立项新建表单填 名称 + 负责人、点移交
- **THEN** SHALL 以 `{projectName, projectOwnerUserId, decision:'PASS'}` 调 `initiateOpportunity`（无 projectCode）
- **AND** SHALL NOT 出现 `delivery-new-code` 编号输入框
