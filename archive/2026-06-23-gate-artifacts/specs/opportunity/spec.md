# Capability: opportunity — v0.0.45 gate-artifacts delta (MODIFIED)

> 合并入 canonical `specs/opportunity/spec.md`（Phase 6）。仅新增/修改以下 Requirements。见 [[opportunity-artifact]]。

## MODIFIED Requirements (from change 2026-06-23-gate-artifacts / v0.0.45)

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
