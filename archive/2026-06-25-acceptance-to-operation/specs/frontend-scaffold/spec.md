# Capability: frontend-scaffold — v0.0.58 acceptance-to-operation delta (MODIFIED)

> 合并入 canonical `specs/frontend-scaffold/spec.md`（Phase 6）。

## MODIFIED Requirements (from change 2026-06-25-acceptance-to-operation / v0.0.58)

### Requirement: 运营详情页 /crm/operations/:id

系统 SHALL 提供 `/crm/operations/:id` 运营详情页：getOperation 深链拉取，渲染：
- Hero（客户·标题 + 阶段(回款/运营/复购) chip + 状态(ACTIVE/CLOSED) chip + 编辑入口）
- 概览（运营负责人/项目/来源商机链接），含「编辑」并 updateOperation
- 流转产出物（按 op.opportunityId 调 listOpportunityArtifacts，复用 OpportunityDetailPage 卡片样式 + 预览/导出/链接）
- 来自商机的诉求/需求（按 opportunityId 调 listDemands/listRequirements）
- 运营问题清单：新建（标题/描述/严重度/负责人）+ 编辑（状态切换、关闭原因）+ 删除

OperationBoard 每张卡片 SHALL 含「详情」按钮 navigate(`/crm/operations/{id}`)，原「推进」按钮保留。

#### Scenario: 访问详情页加载并展示

- **WHEN** 访问 `/crm/operations/7`
- **THEN** SHALL 调 getOperation(7)、listOperationIssues(7)；若有 opportunityId，则同步调 listOpportunityArtifacts / listDemands / listRequirements
- **AND** 渲染 `op-detail-page`

#### Scenario: 新建问题

- **WHEN** 点 `op-issue-add` 填表后保存
- **THEN** SHALL 调 createOperationIssue 并刷新列表
