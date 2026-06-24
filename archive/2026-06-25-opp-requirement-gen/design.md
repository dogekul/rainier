# Design: v0.0.56 — 商机→诉求/需求生成

## Context

后端无 LLM（确认）。Demand/Requirement 均 JpaSpecificationExecutor + Specification 列表过滤。Demand.create(req) / Requirement.create(req) 走 DTO→entity。Opportunity 有 customerName/title/productId/productName(enriched)。现场调研产出物 listOpportunityArtifacts(oppId) 含 SURVEY_REPORT(content)/SURVEY_ATTACHMENT(link)。OpportunityRepository 可 existsById 校验。

## Decisions

### D1: 链接用 opportunityId 列（两实体各一），不建链接表
Demand/Requirement 各加可空 `opportunityId`（与既有 projectId 占位列同模式）。create DTO 接受、service 非空校验商机存在（注入 OpportunityRepository.existsById）、detail 返回；list 加 opportunityId 谓词。最小、可查询、可追溯。
- 备选：独立链接表 — 过重，诉求/需求与商机是 0..N→1 来源，列足够。

### D2: 助手式草稿在客户端组合（无后端生成端点）
草稿标题/描述由前端用已加载数据组合：`getOpportunity` + `listOpportunityArtifacts` + opp.productName。描述模板：
```
来源：商机 #<id> · <客户> · <商机标题>
产品：<productName 或 —>

【现场调研】
<拼接 SURVEY_REPORT.content>
附件：<SURVEY_ATTACHMENT.link 列表>
```
用户可编辑后提交。无需新后端端点、无 LLM。

### D3: 用户在草稿里选 诉求/需求
草稿表单含目标切换（诉求 Demand / 需求 Requirement）。
- 诉求：createDemand{title, description, priority, submitterUserId=当前用户, opportunityId}。
- 需求：createRequirement{code(自动 `OPP<oppId>-<时间戳后4>` 或用户填), title, description, priority, ownerUserId=当前用户, opportunityId}。

### D4: 详情页「已生成」区
详情页用 `listDemands({opportunityId})` + `listRequirements({opportunityId})` 拉取并列出（标题/类型/状态）。提交后刷新。

### D5: 列宽/迁移
opportunity_id BIGINT nullable；ddl-auto=update 加列安全（既有行 null）。无需 backfill。

## Architecture

```
OpportunityDetailPage:
  [生成产品诉求/需求] → 组合草稿(opp+artifacts+product) → 草稿表单(opp-gen-form)
     切换 诉求/需求 + 编辑 → 提交
        诉求: createDemand({...,opportunityId})
        需求: createRequirement({...,opportunityId})
     → 刷新「已生成」(listDemands/Requirements by opportunityId)
后端: Demand/Requirement +opportunityId（entity/DTO/service create+list filter/detail）
```

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 当前用户 id 来源（submitter/owner 必填） | 复用 useAuthStore 当前用户 id；缺失则用商机 pmUserId 兜底/报错 |
| 需求 code 唯一 | 自动生成 `OPP-<oppId>-<rand>`；冲突后端已 409，前端可重试 |
| 2 实体改动重复 | 对称实现，backend temurin-8 全量回归 |
| Java 8 | 无 var/Set.of；用既有写法 |
