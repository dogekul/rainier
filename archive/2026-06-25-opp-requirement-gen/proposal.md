# Proposal: v0.0.56 — 商机→产品诉求/需求生成（助手式草稿）

## Why

进入 产品诉求(REQUIREMENT) 阶段后，需把 现场调研成果 + 产品信息 沉淀为系统内的 诉求(Demand)/需求(Requirement)。当前商机与诉求/需求无连接，REQUIREMENT 阶段也无动作；后端无 LLM。经 Gate 1：助手式草稿（无 LLM）；提交目标由用户在草稿里选 诉求 或 需求。

## What Changes

- **后端**：`Demand` 与 `Requirement` 各加可空 `opportunityId`（来源商机，可追溯）；
  - create DTO 接受 `opportunityId`（非空则校验商机存在），response/detail 返回；
  - list 支持 `?opportunityId=` 过滤（供详情页回看已生成项）。
  - 2 个新列（nullable，ddl-auto 安全；不改既有行）。
- **前端**：商机详情页（实施阶段商机）新增「生成产品诉求/需求」动作 → 草稿抽屉：
  - **客户端预填**（无 LLM）：标题=`客户 · 商机标题`；描述=聚合 现场调研报告正文 + 现场调研附件链接 + 产品名 + 来源商机标注。
  - **切换 提交为 诉求(Demand) / 需求(Requirement)**；可编辑标题/描述/优先级（需求另含 code/负责人，默认当前用户/自动 code）。
  - 提交 → 创建对应实体，带 `opportunityId`。
  - 详情页新增「已生成诉求/需求」区，按 `opportunityId` 列出本商机派生条目。

## Capabilities

- Modified: `entity-demand`、`entity-requirement`（+opportunityId 链接 + list filter）、`frontend-scaffold`/`opportunity`（详情页生成草稿）。New: 无。无 LLM/新依赖。

## Impact

- 后端：demand(Demand/DTO/Service/Controller)、requirement(同) 各 +opportunityId + list filter；2 新列。
- 前端：`OpportunityDetailPage.tsx` + 草稿组件 + `api/demand.ts`/`api/requirement.ts`（+opportunityId 字段/过滤）。
- 数据：仅在用户主动生成时新建 诉求/需求；不动既有数据。
- 测试：后端 DemandControllerTest/RequirementControllerTest（create 带 opportunityId 持久化 + 返回；list 按 opportunityId 过滤）；前端 OpportunityDetailPage 生成草稿→提交 诉求/需求。

## Success Criteria

- [ ] `POST /api/demands` / `/api/requirements` 接受 `opportunityId`（非空校验商机存在）并持久化+返回。
- [ ] `GET /api/demands?opportunityId=` / `/api/requirements?opportunityId=` 仅返回该商机派生项。
- [ ] 详情页「生成」据 现场调研+产品 预填草稿；切诉求/需求；编辑后提交 → 新建并关联回商机。
- [ ] 详情页「已生成」区列出本商机的诉求/需求。
- [ ] 前端全绿 + 后端 temurin-8 全绿。
