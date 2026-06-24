# Capability: frontend-scaffold — v0.0.53 survey-artifacts delta (MODIFIED)

> 合并入 canonical `specs/frontend-scaffold/spec.md`（Phase 6）。DeliveryFlow 现场调研行的推进补充表单。见 [[opportunity]]。

## MODIFIED Requirements (from change 2026-06-24-survey-artifacts / v0.0.53)

### Requirement: DeliveryFlow 现场调研推进 = 补充产出物表单

DeliveryFlow「现场调研(SURVEY)」行的「推进」SHALL 在缺必需产出物（《现场调研报告》《现场调研附件》）时打开「补充产出物并推进」表单：报告填标题+正文、附件填链接（可多份），提交 SHALL 逐个建档后再 advance；产出物齐备 SHALL 直接 advance。其它实施环节（产品诉求/交付）SHALL 仍为无门禁直接「推进」。

#### Scenario: 现场调研行推进打开补充表单

- **GIVEN** DeliveryFlow 渲染一个 现场调研(SURVEY) 商机行（无已存产出物）
- **WHEN** 点击 `delivery-advance-{id}`
- **THEN** SHALL 打开补充表单（含 `delivery-supp-SURVEY_REPORT` 与 `delivery-supp-SURVEY_ATTACHMENT`）
- **AND** SHALL NOT 立即调用 advance

#### Scenario: 补充表单提交逐个建档并推进

- **GIVEN** 补充表单已打开，报告填了正文、附件填了链接
- **WHEN** 点击 `delivery-supp-save`
- **THEN** SHALL 为每类产出物调用 `createOpportunityArtifact`
- **AND** SHALL 随后调用 `advanceOpportunity(id, undefined)` 并刷新列表

#### Scenario: 产品诉求行仍直接推进

- **GIVEN** DeliveryFlow 渲染一个 产品诉求(REQUIREMENT) 商机行
- **WHEN** 点击 `delivery-advance-{id}`
- **THEN** SHALL 直接调用 `advanceOpportunity(id, undefined)`（不打开补充表单）
