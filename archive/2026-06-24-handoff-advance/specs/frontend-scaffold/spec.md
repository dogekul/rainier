# Capability: frontend-scaffold — v0.0.52 handoff-advance delta (MODIFIED)

> 合并入 canonical `specs/frontend-scaffold/spec.md`（Phase 6）。DeliveryFlow 立项行按钮调整。见 [[opportunity]]。

## MODIFIED Requirements (from change 2026-06-24-handoff-advance / v0.0.52)

### Requirement: DeliveryFlow 立项行 = 立项移交（即推进）+ 驳回

DeliveryFlow「立项(INITIATION)」行 SHALL 仅含 **立项移交**（主操作，关联/新建项目并由后端推进到现场调研）+ **驳回**（REJECT，停留）；
SHALL NOT 再有独立「通过」按钮（其会绕过项目直接推进、不正确）。立项移交后刷新 SHALL 见商机进入「现场调研」。

#### Scenario: 立项行无独立「通过」

- **GIVEN** 一个 立项 商机
- **WHEN** DeliveryFlow 渲染该行
- **THEN** SHALL 含 `delivery-handoff-{id}` 与 `delivery-reject-{id}`
- **AND** SHALL NOT 含 `delivery-pass-{id}`
