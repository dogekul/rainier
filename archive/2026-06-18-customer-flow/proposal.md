# v0.0.44-customer-flow — 客户全流程：售前商机 + 售后运营（分阶段多实体）

> Baseline: tag `v0.0.43-ai-work-log` / commit 0666a61。来自用户提供的「客户全流程管理」图（13 节点 / 3 环节 / 4 负责人 / 4 关口）。
>
> **理顺 (2026-06-22)**：按用户「按截图把流程理顺」要求，Opportunity 由 5 售前节点扩为 faithful 的 **10 节点**
> （售前 5 + 实施 5），关口由 3 增至 **4**（加「立项评审」）。13-node 图 = Opportunity 10（售前5+实施5）+ Operation 3（售后）。
> 下文标注 (5)/(3 关口) 的旧表述以本注记为准更新为 (10)/(4 关口)。

## Why

公司新部门做对外软件交付，管理一条「线索→…→复购」客户全生命周期。本系统目前只覆盖**实施**（Project/Sprint/Story/
Task/里程碑），完全缺**售前**（线索/商机/POC/投标/合同）与**售后**（回款维保/运营/复购）。本版按「分阶段多实体」补两个
新实体（售前商机 + 售后运营），实施沿用 Project，把全流程串起来。

## 已锁定模型（Gate 1 用户确认）

- **分阶段多实体**：售前=`Opportunity`(新)、实施=复用 `Project`(B)、售后=`Operation`(新)。
- **四负责人固定字段**：商务 / 解决方案 / 项目经理 / 运营。
- **赢单=合同签订**（CONTRACT 关口 PASS → WON）；**丢单=任一售前关口 REJECT**（→ LOST）。
- **加金额**（amount，pipeline 价值 / 高价值项目）。
- **关口位置**：商机决策(商机) / 投标·报价决策(投标) / 合同评审(合同) 在售前；立项评审在实施入口（售前→Project 移交处）。

## What Changes

**后端（NEW `opportunity` + NEW `operation`，+2 表）**

`Opportunity`（客户全流程，表 rainier_opportunity）：
- 字段：`customerName` / `title` / `amount`(可空) / `stage`(售前 LEAD/OPPORTUNITY/POC/BIDDING/CONTRACT + 实施
  INITIATION/SURVEY/REQUIREMENT/DELIVERY/ACCEPTANCE) / `status`(OPEN/WON/LOST) / 四负责人 `commercialOwnerUserId`
  `solutionOwnerUserId` `pmUserId` `opsOwnerUserId`(可空，存在则校验) / `projectId`(可空，立项后链 Project) / `gateDecidedBy`。
- `OpportunityStage`(10) + `STAGE_ORDER`（推进序）+ `GATE_STAGES`={OPPORTUNITY,BIDDING,CONTRACT,INITIATION}、
  `PRESALES_GATES`={OPPORTUNITY,BIDDING,CONTRACT}；`OpportunityStatus`(OPEN/WON/LOST)。
- CRUD（建/查/列[过滤 stage/status/owner]/改/软删）。
- **推进** `POST /api/opportunities/{id}/advance` body `{decision?, note?}`：沿 10 节点链推进；非关口节点直接进下一节点；
  **关口节点必须带 decision**（记 gateDecidedBy）；售前关口 REJECT→status=LOST，立项关口 REJECT 停在「立项」可重审；
  从 CONTRACT PASS → **status=WON 且 stage=INITIATION**（赢单、入实施）；终点「验收」与已 LOST 不可再推进（409）。
- **立项移交** `POST /api/opportunities/{id}/initiate` body `{projectId, decision, note?}`：仅 status=WON 可立项；
  PASS 记 `projectId`（链入实施=Project）+ 立项留痕；REJECT 记否决。与 advance 的立项关口并存（advance 走链、initiate 链项目）。

`Operation`（售后运营，表 rainier_operation）：
- 字段：`customerName` / `title` / `stage`(MAINTENANCE 回款维保/OPERATING 运营/REPURCHASE 复购) / `status`(ACTIVE/CLOSED) /
  `opsOwnerUserId`(可空) / `projectId`(可空，链交付完成的 Project)。
- `OperationStage`(3) + STAGE_ORDER。CRUD + 推进 `POST /api/operations/{id}/advance`（无关口，仅线性进 + 末段可 CLOSED）。

实施阶段**复用现有 Project**（立项→现场调研→产品诉求→交付实施→验收 仍在 Project/Sprint/Story/Task/里程碑跑），本版仅加 Opportunity↔Project / Operation↔Project 的链字段，不改 Project。

**前端（frontend-scaffold MOD）**

- `api/opportunity.ts` / `api/operation.ts`。
- 新顶级「**客户**」导航组（all-users，4 项）。**看板/流转分离（2026-06-23 修订，详见 design D7）**：
  - `OpportunityBoard`「商机看板」`/crm/opportunities`：**只读**两段泳道 10 节点分列（只读卡片：客户+标题+金额+负责人+赢单标识；**零操作控件**）+ StatTiles（进行中 + 赢单 + 丢单 + 在谈金额）。给监控角色（待定）看全商机进展。
  - `PresaleFlow`「售前流转」`/crm/presale-flow`：售前操作页（新建商机 + 推进 + 关口 通过/否决，否决先确认）。
  - `DeliveryFlow`「实施流转」`/crm/delivery-flow`：实施操作页（立项移交链 Project + 立项 通过/否决 + 推进 + 验收终态）。
  - `OperationBoard`「运营看板」`/crm/operations`：按 3 节点分列 + 建单。
- `/crm/*` 不入 isAdminPath（all-users）。

## Capabilities

### New Capabilities
- `opportunity`：客户全流程 pipeline（10 节点：售前 5 + 实施 5；4 关口 + 赢/丢单 + 立项移交）。
- `operation`：售后运营 pipeline（3 节点）。

### Modified Capabilities
- `frontend-scaffold`：新「客户」导航组 + 商机看板 + 运营看板 + /crm/* 路由。

## Impact

- 后端 ~20 文件（2 实体 + 2 status 常量 + 2 repo + ~6 DTO + 2 service + 2 controller）。新测试 2-3 类。**表数 21→23**（LegacyProductCategoryCleanupTest 更新）。
- 前端 ~7 文件（2 api + 2 board+index + AppRoutes + AppLayout）。新测试 2-3。
- 配置/基础设施：**+2 表**（ddl-auto 自动建）、0 AI、0 新依赖。新增 ~10 个 all-users API。

## Success Criteria

- [ ] Opportunity：10 节点推进（售前 5 + 实施 5）；关口节点（商机/投标/合同/立项）推进必带 decision（缺→400）；售前 REJECT→LOST、立项 REJECT 停留；CONTRACT PASS→WON 且入 INITIATION；验收/LOST 终态 409。
- [ ] 立项移交：仅 WON 可 initiate；PASS 链 projectId；非 WON initiate→409/400。
- [ ] 四负责人字段持久化 + 存在校验；amount 持久化（pipeline 汇总可用）。
- [ ] Operation：3 节点线性推进；末段可 CLOSED。
- [ ] 商机看板**只读**按节点分列渲染（零操作控件）；新建/推进/关口 在「售前流转」、立项移交/推进/关口 在「实施流转」（操作页，售前/实施 分离）；/crm/* all-users（navGuardConsistency 自动钉）。
- [ ] 表数 23；backend 全绿（470 baseline + 新增）+ frontend 全绿（180 baseline + 新增）+ E2E（建单→推进→关口→赢单→立项 / 运营推进）+ 存量数据零改。
