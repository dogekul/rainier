# Proposal: v0.0.58 — 验收完成自动进入运营 + 运营详情页 + 运营问题清单

## Why

验收(ACCEPTANCE) 是商机生命周期终点，但项目并不结束 — 进入运营/维护/复购。系统已有 Operation 实体(售后看板)，目前需手动「新建运营单」，与商机/项目断开。本变更：

- 商机 DELIVERY→ACCEPTANCE 推进成功后**自动**建一条 Operation（链入来源商机/项目）。
- 运营详情页 **一页预览该项目所有内容**：概览 + 商机产出物 + 诉求/需求 + 问题清单。
- 新增**运营问题清单**：标准字段 标题/描述/严重度/状态/上报人/负责人/关闭原因。

经 Gate 1：标准字段 + 详情页含 概览/产出物/诉求需求/问题清单。

## What Changes

### Backend
- **Operation +`opportunityId`**（可空 BIGINT，来源商机）：create/update DTO 接受；detail 返回；list 支持 `?opportunityId=` 过滤。
- **自动建运营单**：`OpportunityService.advance` 当推进至 ACCEPTANCE 时调 `OperationService.createForAcceptedOpportunity(opp)` —— customerName/title 取自 opp，opsOwnerUserId/projectId 同源，opportunityId=opp.id；幂等（同 opportunityId 已存在则跳过）。
- **新实体 OperationIssue**（rainier_operation_issue 新表）：id/operationId/title/description/severity(HIGH/MEDIUM/LOW)/status(OPEN/IN_PROGRESS/RESOLVED/CLOSED)/reporterUserId/assigneeUserId/closeReason + audit。REST：`GET/POST /api/operations/{id}/issues`、`PUT/DELETE /api/operation-issues/{id}`、`GET /api/operation-issues/{id}`。
- 测试：OperationControllerTest +opportunityId 持久化/过滤；OpportunityControllerTest +auto-create on ACCEPTANCE；OperationIssueControllerTest 新增 CRUD。

### Frontend
- `api/operation.ts`：Operation +opportunityId/Detail/List/getOperation；ListParams +opportunityId。
- `api/operationIssue.ts` 新：types + listIssues(opId)/createIssue(opId,body)/updateIssue(id,body)/deleteIssue(id)、SEVERITY_LABELS/ISSUE_STATUS_LABELS。
- **新页 `OperationDetailPage`** `/crm/operations/:id`：
  - Hero：客户·标题 + stage chip(回款/运营/复购) + status chip(ACTIVE/CLOSED) + 编辑按钮
  - 概览：客户/标题/负责人/项目/来源商机；编辑（updateOperation）
  - 流转产出物 list（按 operation.opportunityId 调 listOpportunityArtifacts，复用 v0.0.55 卡片样式）
  - 诉求/需求 list（按 opportunityId 调 listDemands/listRequirements）
  - 问题清单：列表 + 添加按钮 + 状态切换 + 编辑/关闭表单
- OperationBoard：每张运营卡片点击 → navigate(`/crm/operations/{id}`)；新增 testid `op-card-{id}`。
- AppRoutes：+`/crm/operations/:id` 路由。

## Capabilities

- Modified: `operation`（+opportunityId + 自动建）、`opportunity`（advance 副作用）、`frontend-scaffold`。
- New: `entity-operation-issue`（运营问题清单实体）。

## Impact

- 新表 1 张：`rainier_operation_issue`。新列 1 个：`rainier_operation.opportunity_id`。ddl-auto 自动加（nullable 列 + 新表）。
- 数据：仅当用户主动推进至 ACCEPTANCE 时新建一条 Operation；不动其它存量数据。
- 后端依赖图：OpportunityService → OperationService（无环；OperationService 不依赖 OpportunityService）。

## Success Criteria

- [ ] DELIVERY → ACCEPTANCE advance 成功后，自动新建一条 Operation（projectId/opportunityId 关联回去）；同商机重复 advance（理论不可能因 ACCEPTANCE 是 terminal）不重复建。
- [ ] 访问 `/crm/operations/:id` 显示 概览 + 流转产出物 + 诉求/需求 + 问题清单。
- [ ] 运营问题清单 CRUD（标准字段）live + 单测全绿。
- [ ] 后端 temurin-8 全绿 + 前端全绿 + tsc/lint clean + E2E 验收→自动建运营 → 详情页预览 → 加问题 → 关闭。
