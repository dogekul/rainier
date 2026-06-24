# Capability: frontend-scaffold — v0.0.55 opportunity-detail-page delta (MODIFIED)

> 合并入 canonical `specs/frontend-scaffold/spec.md`（Phase 6）。统一商机详情页，取代售前/实施的详情抽屉。见 [[opportunity]]。

## MODIFIED Requirements (from change 2026-06-24-opportunity-detail-page / v0.0.55)

### Requirement: 统一商机详情页 /crm/opportunities/:id

系统 SHALL 提供路由 `/crm/opportunities/:id` 的商机详情页：按 `:id` 经 `getOpportunity` 拉取商机并渲染 **概览**（客户/标题/阶段中文/状态/金额/产品/四负责人/关联项目/备注/最近决策人）+ 可「编辑」(updateOpportunity) + **流转产出物**列表（报告类预览/导出 Word、链接类打开）+「添加产出物」+「返回」。售前流转、实施流转 行「详情」SHALL 跳转到该页（不再用抽屉）。推进/门禁 SHALL 仍在列表行上，不在详情页。

#### Scenario: 详情页按 id 加载商机与产出物

- **GIVEN** 存在商机 id=7
- **WHEN** 访问 `/crm/opportunities/7`
- **THEN** SHALL 调 `getOpportunity(7)` 与 `listOpportunityArtifacts(7)`
- **AND** SHALL 渲染 `opp-detail-page`，显示其客户·标题与阶段中文标签

#### Scenario: 详情页编辑保存

- **GIVEN** 详情页已加载
- **WHEN** 点「编辑」改标题并保存
- **THEN** SHALL 以新值调 `updateOpportunity` 并刷新展示

#### Scenario: 详情页添加产出物

- **GIVEN** 详情页已加载
- **WHEN** 打开「添加产出物」填内容并保存
- **THEN** SHALL 调 `createOpportunityArtifact` 并重新拉取产出物列表

#### Scenario: 售前流转行详情跳转

- **GIVEN** 售前流转渲染一行商机 id=5
- **WHEN** 点击 `presale-detail-5`
- **THEN** SHALL 导航到 `/crm/opportunities/5`（SHALL NOT 打开抽屉）

#### Scenario: 实施流转行详情跳转

- **GIVEN** 实施流转渲染一行商机 id=7
- **WHEN** 点击 `delivery-detail-7`
- **THEN** SHALL 导航到 `/crm/opportunities/7`（SHALL NOT 打开抽屉）

### Requirement: CRM 商机页面视觉优化（卡片化，复用设计令牌）

商机看板 / 商机详情页 / 售前流转 / 实施流转 SHALL 采用统一卡片化视觉，复用 `global.css` 设计令牌（颜色/圆角/阴影/状态色）与 board 组件（StatusChip/StatTiles）：详情页以卡片承载概览(头像+客户·标题+阶段/状态 chip)与流转产出物；售前/实施流转以行卡片(hover-lift)取代朴素表格；看板的漏斗条/阶段泳道/列表以卡片承载。所有既有 data-testid SHALL 保持不变（行为不回归）。

#### Scenario: 流转页行卡片渲染且 testid 不回归

- **GIVEN** 售前/实施流转有商机行
- **WHEN** 页面渲染
- **THEN** 行 SHALL 以卡片(`oppflow-card`)呈现
- **AND** `presale-row-{id}`/`delivery-row-{id}`/`*-detail-{id}`/推进类按钮 testid SHALL 保持存在

#### Scenario: 详情页卡片化概览

- **GIVEN** 访问 `/crm/opportunities/:id`
- **THEN** SHALL 以卡片承载概览(含 StatusChip 阶段/状态)与流转产出物，且 `opp-detail-*` testid 不变
