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
