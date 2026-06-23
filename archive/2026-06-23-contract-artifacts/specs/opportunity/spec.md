# Capability: opportunity — v0.0.46 contract-artifacts delta (MODIFIED)

> 合并入 canonical `specs/opportunity/spec.md`（Phase 6）。仅微调 advance 产出物门禁语义。见 [[opportunity-artifact]]。

## MODIFIED Requirements (from change 2026-06-23-contract-artifacts / v0.0.46)

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
