# Capability: frontend-scaffold — v0.0.54 delivery-detail delta (MODIFIED)

> 合并入 canonical `specs/frontend-scaffold/spec.md`（Phase 6）。DeliveryFlow 行新增详情抽屉。见 [[opportunity]]。

## MODIFIED Requirements (from change 2026-06-24-delivery-detail / v0.0.54)

### Requirement: DeliveryFlow 行可查看商机详情 + 流转产出物

DeliveryFlow 每个实施中商机行 SHALL 含「详情」按钮（`delivery-detail-{id}`），点击 SHALL 打开详情抽屉（`delivery-detail-body`）：
只读展示商机字段（客户·标题/阶段/状态/备注/金额/产品/四负责人），可切换「编辑」改字段并经 `updateOpportunity` 保存后刷新；
产出物区 SHALL 列出该商机全部流转产出物（报告类可预览/导出 Word、链接类可打开），并可「添加产出物」。推进操作 SHALL 仍在行上（抽屉不含推进按钮）。

#### Scenario: 行有详情入口并打开抽屉

- **GIVEN** DeliveryFlow 渲染一个实施中商机行
- **WHEN** 点击 `delivery-detail-{id}`
- **THEN** SHALL 打开 `delivery-detail-body`，展示该商机的客户·标题与阶段中文标签
- **AND** SHALL 调 `listOpportunityArtifacts` 加载其产出物

#### Scenario: 详情列出流转产出物

- **GIVEN** 一个有 SURVEY_REPORT + SURVEY_ATTACHMENT 产出物的商机详情已打开
- **THEN** SHALL 出现 `delivery-detail-artifact-{aid}`，报告类 SHALL 有 `delivery-detail-export-{aid}`、链接类 SHALL 有 `delivery-detail-link-{aid}`

#### Scenario: 详情编辑保存

- **GIVEN** 详情抽屉已打开
- **WHEN** 点「编辑」改标题、点 `delivery-detail-save`
- **THEN** SHALL 以新值调 `updateOpportunity` 并刷新列表

#### Scenario: 推进不在抽屉内

- **WHEN** 详情抽屉渲染
- **THEN** SHALL NOT 含推进/立项移交/驳回按钮（这些仍在行上）
