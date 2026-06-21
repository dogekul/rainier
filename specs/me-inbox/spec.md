# Capability: me-inbox

> NEW capability (v0.0.42-po-inbox, 2026-06-18)。`GET /api/me/inbox` 自助 PO 需求收件箱 read-model：待处理诉求
> （未转化）+ 我的需求。all-users（token 必需，非 admin）。纯读聚合 Demand/DemandRequirementLink/Requirement，
> 零写、零新表。路线图 §4 PO（最弱角色）。见 [[frontend-scaffold]] / [[entity-demand]] / [[entity-requirement]]。

## ADDED Requirements

### Requirement: 我的需求收件箱

后端 SHALL 提供 `GET /api/me/inbox`（token 必需，非 admin），返回 `{ unconvertedDemands, myRequirements }`：
`unconvertedDemands` = 尚无 demand-requirement 关联且状态非终态（非 DONE/CLOSED）的 Demand `[{id,title,priority,
status,createTime}]`，按优先级高→低；`myRequirements` = `ownerUserId = 当前用户` 的 Requirement `[{id,code,title,
status,priority,expectedDate,projectId,projectName}]`，按优先级高→低。token 主体无对应用户 → 两区皆空。

#### Scenario: 待处理诉求 = 未关联且非终态

- **GIVEN** 诉求 D1（无关联、状态 PENDING）、D2（已关联到某需求）、D3（无关联、状态 CLOSED）
- **WHEN** 用户携带有效 token `GET /api/me/inbox`
- **THEN** SHALL 返回 200
- **AND** unconvertedDemands SHALL 含 D1
- **AND** unconvertedDemands SHALL NOT 含 D2（已关联）或 D3（终态）

#### Scenario: 我的需求按 owner 过滤

- **GIVEN** 需求 R1（ownerUserId=alice）、R2（ownerUserId=bob）
- **WHEN** alice `GET /api/me/inbox`
- **THEN** myRequirements SHALL 含 R1
- **AND** myRequirements SHALL NOT 含 R2

#### Scenario: 我的需求富化 projectName + 优先级排序

- **GIVEN** alice 的两条需求：R-LOW（priority=LOW，projectId=P）、R-URG（priority=URGENT）；项目 P 名称 "Apollo"
- **WHEN** alice `GET /api/me/inbox`
- **THEN** myRequirements 首条 SHALL 为 R-URG
- **AND** R-LOW 的 projectName SHALL 为 "Apollo"

#### Scenario: 缺 token 拒绝

- **WHEN** 未携带 token `GET /api/me/inbox`
- **THEN** SHALL 返回 HTTP 401

#### Scenario: token 主体无对应用户则两区皆空

- **GIVEN** token sub 为 "ghost"，数据库无该用户
- **WHEN** `GET /api/me/inbox`
- **THEN** SHALL 返回 200
- **AND** unconvertedDemands 与 myRequirements SHALL 皆为空数组
