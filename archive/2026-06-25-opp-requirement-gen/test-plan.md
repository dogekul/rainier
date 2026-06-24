# Test Plan: v0.0.56 — 商机→诉求/需求生成

## 测试策略

后端 mockMvc（DemandControllerTest / RequirementControllerTest 扩展）+ temurin-8 全量。前端 Vitest（OpportunityDetailPage.test.tsx 扩展，mock createDemand/createRequirement/listDemands/listRequirements/getOpportunity/listOpportunityArtifacts）。standing 约束：不删改既有数据。

## 详细测试案例

### entity-demand / entity-requirement（后端）

| TC-ID | 场景 | 预期 |
|-------|------|------|
| TC-OGEN-D1 | 创建诉求带 opportunityId(存在) | 201，detail.opportunityId == 商机 |
| TC-OGEN-D2 | 创建诉求带不存在 opportunityId | 400 |
| TC-OGEN-D3 | 不带 opportunityId 创建诉求 | 201，opportunityId null（既有行为不回归）|
| TC-OGEN-D4 | GET /demands?opportunityId= 过滤 | 仅返回该商机派生 |
| TC-OGEN-R1 | 创建需求带 opportunityId | 201，detail.opportunityId == 商机 |
| TC-OGEN-R2 | 创建需求带不存在 opportunityId | 400 |
| TC-OGEN-R3 | GET /requirements?opportunityId= 过滤 | 仅返回该商机派生 |

### frontend-scaffold（前端）

| TC-ID | 场景 | 预期 |
|-------|------|------|
| TC-OGEN-F1 | 点「生成」打开草稿，预填调研+产品 | opp-gen-form 出现，描述含调研正文/产品名 |
| TC-OGEN-F2 | 默认目标诉求，提交 | createDemand 调用，body 含 opportunityId + 编辑标题 |
| TC-OGEN-F3 | 切换需求，提交 | createRequirement 调用，body 含 opportunityId |
| TC-OGEN-F4 | 已生成区列出派生项 | opp-gen-list 含 listDemands/Requirements 返回项 |

## 测试执行矩阵

| 功能 | 单元/集成 |
|------|----------|
| opportunityId 持久化/校验/过滤 | TC-OGEN-D1..4 / R1..3 |
| 草稿预填 + 提交诉求/需求 | TC-OGEN-F1/F2/F3 |
| 已生成列表 | TC-OGEN-F4 |

## 回归风险矩阵

| 区域 | 风险 | 缓解 |
|------|------|------|
| Demand/Requirement create/list | 🟡中（加列+param） | TC-OGEN-* + 既有 Demand/Requirement 测试全回归 |
| ddl 加列 | 🟢低（nullable） | temurin-8 + 既有数据 null |
| 详情页 | 🟡中（同文件加生成 UI） | 既有 OpportunityDetailPage 8 用例保持 + TC-OGEN-F* |

## 建议补充顺序

P0：TC-OGEN-D1..4 / R1..3 / F1..4 + 既有回归。
