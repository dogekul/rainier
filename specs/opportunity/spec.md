# Capability: opportunity — v0.0.44 (NEW)

> NEW capability。客户全流程：**售前环节** 线索→商机→推介POC→投标→合同签订，**实施环节** 立项→现场调研→产品诉求→
> 交付实施→验收。共 10 节点、4 关口（商机/投标/合同/立项决策）+ 赢单/丢单 + 立项移交（链 Project）。all-users。
> 新表 rainier_opportunity。faithful 还原客户全流程图（售前环节 + 实施环节两段泳道）。见 [[frontend-scaffold]] / [[entity-project]]。

## ADDED Requirements

### Requirement: 商机创建与字段

后端 SHALL 提供 `POST /api/opportunities`，必填 `customerName` / `title`；可选 `amount` 与四负责人
`commercialOwnerUserId`/`solutionOwnerUserId`/`pmUserId`/`opsOwnerUserId`（非空则校验用户存在，否则 400）；创建
`stage=LEAD`、`status=OPEN`。`GET /api/opportunities`（分页 + 过滤 stage/status/owner）+ `GET /{id}` + `PUT /{id}` + `DELETE`。

#### Scenario: 最小创建

- **WHEN** `POST /api/opportunities` body `{customerName:"X 集团", title:"采购系统"}`
- **THEN** SHALL 返回 201
- **AND** body.stage SHALL 为 "LEAD"、body.status SHALL 为 "OPEN"

#### Scenario: 负责人不存在被拒

- **WHEN** `POST /api/opportunities` body 含 `pmUserId=999999`（不存在）
- **THEN** SHALL 返回 400

### Requirement: 全流程节点推进 + 关口决策

后端 SHALL 提供 `POST /api/opportunities/{id}/advance` body `{decision?, note?}`，沿 10 节点链推进：
LEAD→OPPORTUNITY→POC→BIDDING→CONTRACT→INITIATION→SURVEY→REQUIREMENT→DELIVERY→ACCEPTANCE。

- 非关口节点（LEAD/POC/SURVEY/REQUIREMENT/DELIVERY）直接进下一节点。
- 关口节点（OPPORTUNITY/BIDDING/CONTRACT/INITIATION）SHALL 要求 `decision`∈{PASS,REJECT}（缺/非法→400），并记 gateDecidedBy。
- **售前关口**（商机/投标/合同）REJECT → `status=LOST`（丢单）；**立项关口**（INITIATION）REJECT 停在「立项」可重审，`status` 不变。
- 从 CONTRACT PASS → 进入 INITIATION 且 `status=WON`（赢单、入实施）。
- 已 LOST 的商机 SHALL NOT 再推进（409）；终点 ACCEPTANCE（验收）SHALL NOT 再推进（409）。
- 已 WON 的商机仍可在实施环节继续推进（赢单不等于流程结束）。

#### Scenario: 非关口节点直接推进

- **GIVEN** 商机在 LEAD
- **WHEN** `POST /{id}/advance`（无 decision）
- **THEN** SHALL 返回 200，stage SHALL 为 "OPPORTUNITY"

#### Scenario: 关口节点缺决策被拒

- **GIVEN** 商机在 OPPORTUNITY（关口）
- **WHEN** `POST /{id}/advance`（无 decision）
- **THEN** SHALL 返回 400

#### Scenario: 关口 PASS 推进

- **GIVEN** 商机在 OPPORTUNITY
- **WHEN** `POST /{id}/advance` body `{"decision":"PASS"}`
- **THEN** SHALL 返回 200，stage SHALL 为 "POC"

#### Scenario: 售前关口 REJECT 丢单

- **GIVEN** 商机在 BIDDING
- **WHEN** `POST /{id}/advance` body `{"decision":"REJECT","note":"失标"}`
- **THEN** SHALL 返回 200，status SHALL 为 "LOST"

#### Scenario: 合同签订赢单并入实施

- **GIVEN** 商机在 CONTRACT
- **WHEN** `POST /{id}/advance` body `{"decision":"PASS"}`
- **THEN** SHALL 返回 200，status SHALL 为 "WON"
- **AND** stage SHALL 为 "INITIATION"（进入实施环节首节点「立项」）

#### Scenario: 实施环节内推进（立项评审 PASS）

- **GIVEN** 商机 status=WON、stage=INITIATION
- **WHEN** `POST /{id}/advance` body `{"decision":"PASS"}`
- **THEN** SHALL 返回 200，stage SHALL 为 "SURVEY"（现场调研）
- **AND** status SHALL 仍为 "WON"

#### Scenario: 立项评审 REJECT 停在立项

- **GIVEN** 商机 status=WON、stage=INITIATION
- **WHEN** `POST /{id}/advance` body `{"decision":"REJECT"}`
- **THEN** SHALL 返回 200，stage SHALL 仍为 "INITIATION"
- **AND** status SHALL 仍为 "WON"（不丢单，可重审）

#### Scenario: 已丢单不可推进

- **GIVEN** 商机 status=LOST
- **WHEN** `POST /{id}/advance`
- **THEN** SHALL 返回 409

#### Scenario: 验收为终点不可推进

- **GIVEN** 商机 stage=ACCEPTANCE（验收）
- **WHEN** `POST /{id}/advance`
- **THEN** SHALL 返回 409

### Requirement: 立项移交（链入实施 Project）

后端 SHALL 提供 `POST /api/opportunities/{id}/initiate` body `{projectId, decision, note?}`：仅 `status=WON` 可立项
（否则 409）；`decision`∈{PASS,REJECT}（否则 400）；`projectId` 须存在（否则 400）；PASS SHALL 记 `projectId`
（链入实施 Project）；REJECT 记否决。此端点是「立项移交」的显式入口，与 advance 的立项关口推进并存。

#### Scenario: 赢单后立项链项目

- **GIVEN** 商机 status=WON，存在项目 P
- **WHEN** `POST /{id}/initiate` body `{"projectId":P,"decision":"PASS"}`
- **THEN** SHALL 返回 200，body.projectId SHALL 为 P

#### Scenario: 未赢单不可立项

- **GIVEN** 商机 status=OPEN
- **WHEN** `POST /{id}/initiate` body `{"projectId":1,"decision":"PASS"}`
- **THEN** SHALL 返回 409

## MODIFIED / ADDED Requirements (v0.0.45 gate-artifacts)

> 合并自 change `2026-06-23-gate-artifacts`（Phase 6）。完整 fold-in 记录（客户实体 / 商机备注 / 详情可编辑可推进 /
> POC 多产出物门禁 / 推进时补充 / 链接类无标题·多份）见 `archive/2026-06-23-gate-artifacts/test-report.md`。
> 产出物门禁机制详见 [[opportunity-artifact]]，客户实体见 [[customer]]。

### Requirement: 全流程节点推进 + 关口决策（+ 产出物门禁）

> v0.0.45 增量：`advance` 在「LOST/终态」与「关口 decision」校验之后、状态变更之前，按 `TransitionArtifactRules` 施加
> 产出物门禁（详见 [[opportunity-artifact]]「转换产出物门禁」Requirement）。原 v0.0.44 推进/关口/赢丢单语义不变。

`OpportunityAdvanceRequest` SHALL 新增可选 `artifact{title,content}`。当来源阶段要求产出物时，advance SHALL 校验
artifact 非空（缺→400）并同事务创建产出物；不要求的阶段 SHALL 忽略该字段，行为与 v0.0.44 一致。

#### Scenario: 既有非门禁推进不回归

- **GIVEN** 商机在 INITIATION（WON，无产出物规则）
- **WHEN** `POST /{id}/advance` body `{decision:"PASS"}`
- **THEN** SHALL 返回 200，stage SHALL 为 SURVEY（v0.0.44 语义保持）

### Requirement: 商机产品标签（可空）

> v0.0.45 fold-in：商机可关联一个既有 Product（产品标签）。

`OpportunityCreateRequest` / `OpportunityUpdateRequest` SHALL 接受可空 `productId`；非空时 SHALL 校验该 Product 存在
（否则 400）。`OpportunityDetail` SHALL 回传 `productId` 与 enrich 的 `productName`。productId 可留空（不确定时）。

#### Scenario: 创建时关联产品

- **WHEN** `POST /api/opportunities` body 含存在的 `productId`
- **THEN** SHALL 返回 201，body.productId SHALL 为该值、body.productName SHALL 为该产品名

#### Scenario: 关联不存在的产品被拒

- **WHEN** `POST /api/opportunities` body 含不存在的 `productId`
- **THEN** SHALL 返回 400

### Requirement: 商机备注 + 客户关联（可空）

> v0.0.45 fold-in：新建/编辑商机支持备注（note）与关联既有客户（customerId，见 [[customer]]）。

`OpportunityCreateRequest`/`OpportunityUpdateRequest` SHALL 接受可空 `note`（≤2000）与可空 `customerId`；customerId 非空时
SHALL 关联既有客户，为空时 SHALL 以 `customerName` 创建/复用同名客户（忽略大小写去重）。`OpportunityDetail` SHALL 回传 `note`。

#### Scenario: 带备注与客户创建

- **WHEN** `POST /api/opportunities` body 含 `note` 与既有 `customerId`
- **THEN** SHALL 返回 201，body.note SHALL 为该值

## MODIFIED Requirements (v0.0.46 contract-artifacts)

> 合并自 change `2026-06-23-contract-artifacts`（Phase 6）。仅微调 advance 产出物门禁语义。见 [[opportunity-artifact]]。

### Requirement: 全流程节点推进 + 关口决策（产出物门禁 — 仅 PASS 强制）

> v0.0.46 增量：`advance` 的产出物门禁（见 [[opportunity-artifact]]）改为**仅前进（PASS / 非关口）强制**。关口以 REJECT
> 否决（丢单）SHALL NOT 要求前进交付物；唯 OPPORTUNITY 商机决策《决策评审纪要》在通过/否决都要求。投标/合同新增门禁
> （投标→《投标文件》；合同→中标公示/合同/评审会议纪要/邮件归档/已盖章合同）。其余 v0.0.44/45 推进/赢丢单语义不变。

#### Scenario: 投标否决丢单不受产出物门禁阻挡

- **GIVEN** 商机在 BIDDING（OPEN），无产出物
- **WHEN** `POST /{id}/advance` body `{decision:"REJECT"}`
- **THEN** SHALL 返回 200，status SHALL 为 LOST

#### Scenario: 合同五件齐赢单入实施（既有语义保持）

- **GIVEN** 商机在 CONTRACT，5 类必需产出物齐
- **WHEN** `POST /{id}/advance` body `{decision:"PASS"}`
- **THEN** SHALL 返回 200，stage SHALL 为 INITIATION、status SHALL 为 WON

## MODIFIED / ADDED Requirements (v0.0.47 board-redesign)

> 合并自 change `2026-06-23-board-redesign`（Phase 6）。新增「进入当前阶段时间」`stageEnteredAt`（停留时长预警的后端基础）。见 [[frontend-scaffold]]。

### Requirement: 商机记录「进入当前阶段时间」stageEnteredAt

`Opportunity` SHALL 持有 `stageEnteredAt`（进入当前阶段的时刻，nullable）。`create()` SHALL 将其设为创建时刻；
`advance()` 在**实际发生阶段变更**（PASS / 非关口推进）时 SHALL 刷新为当下；关口 REJECT（阶段不变）SHALL NOT 刷新。
`OpportunityDetail` SHALL 回传 `stageEnteredAt`。既有 null 行 SHALL 由启动 backfill 以 `update_time` 兜底（仅填空值、不改既有业务字段）。

#### Scenario: 创建即记录进入阶段时间

- **GIVEN** 一次合法的 `POST /api/opportunities`
- **WHEN** 创建成功
- **THEN** `OpportunityDetail.stageEnteredAt` SHALL 非空

#### Scenario: 推进刷新进入阶段时间

- **GIVEN** 商机在某非终态阶段、记录了 `stageEnteredAt = t0`
- **WHEN** `POST /{id}/advance` 触发阶段前进（PASS 或非关口）
- **THEN** `stageEnteredAt` SHALL 刷新为不早于 t0 的新时刻

#### Scenario: 关口否决不刷新进入阶段时间

- **GIVEN** 商机在售前关口（如 BIDDING）、记录了 `stageEnteredAt = t0`
- **WHEN** `POST /{id}/advance` body `{decision:"REJECT"}`（阶段不变、丢单）
- **THEN** `stageEnteredAt` SHALL 仍为 t0（不刷新）

#### Scenario: 既有空值经 backfill 兜底

- **GIVEN** 升级前已存在、`stage_entered_at` 为 NULL 的商机行
- **WHEN** 应用启动执行 backfill
- **THEN** 该行 `stage_entered_at` SHALL 被填为其 `update_time`
- **AND** 该行其它业务字段（客户/阶段/金额/状态）SHALL 不变
