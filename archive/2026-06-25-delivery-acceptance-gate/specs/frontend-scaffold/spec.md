# Capability: frontend-scaffold — v0.0.57 delivery-acceptance-gate delta (MODIFIED)

> 合并入 canonical `specs/frontend-scaffold/spec.md`（Phase 6）。DeliveryFlow 交付实施 行推进时补充《甲方验收报告》。见 [[opportunity]]。

## MODIFIED Requirements (from change 2026-06-25-delivery-acceptance-gate / v0.0.57)

### Requirement: DeliveryFlow 交付实施推进 = 补充甲方验收报告

DeliveryFlow「交付实施(DELIVERY)」行的「推进」SHALL 在缺《甲方验收报告》时打开「补充产出物并推进」表单（报告类，填标题+正文），提交 SHALL 建档后调 `advanceOpportunity(id)`；报告已存在时直接 advance。验收 (ACCEPTANCE) 行仍为只读「已验收」。

#### Scenario: 交付实施行推进打开补充表单

- **GIVEN** DeliveryFlow 渲染一个 交付实施(DELIVERY) 商机行（无报告）
- **WHEN** 点 `delivery-advance-{id}`
- **THEN** SHALL 打开补充表单（含 `delivery-supp-DELIVERY_ACCEPTANCE_REPORT`）
- **AND** SHALL NOT 直接调 advance

#### Scenario: 补充表单提交后推进到验收

- **GIVEN** 补充表单已打开，已填正文
- **WHEN** 点 `delivery-supp-save`
- **THEN** SHALL 调 `createOpportunityArtifact({type:"DELIVERY_ACCEPTANCE_REPORT", title, content})`
- **AND** SHALL 调 `advanceOpportunity(id)` 推进到 ACCEPTANCE 并刷新列表
