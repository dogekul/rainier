# Capability: opportunity — v0.0.57 delivery-acceptance-gate delta (MODIFIED)

> 合并入 canonical `specs/opportunity/spec.md`（Phase 6）。客户全流程末关：交付实施→验收 推进门禁。见 [[frontend-scaffold]]。

## MODIFIED Requirements (from change 2026-06-25-delivery-acceptance-gate / v0.0.57)

### Requirement: 交付实施→验收 推进需提交《甲方验收报告》

`POST /api/opportunities/{id}/advance` 当来源阶段为 **交付实施(DELIVERY)** 时 SHALL 要求该商机已存在产出物 `DELIVERY_ACCEPTANCE_REPORT`（甲方验收报告，报告类，富文本正文）；否则 SHALL 返回 400 且消息含《甲方验收报告》。满足则 SHALL 推进到 验收(ACCEPTANCE) 并刷新 stageEnteredAt。

#### Scenario: 交付实施 无验收报告 不可推进

- **GIVEN** 一个 交付实施(DELIVERY) + WON 的商机，无任何产出物
- **WHEN** `POST /{id}/advance` body `{}`
- **THEN** SHALL 返回 400
- **AND** 消息 SHALL 含《甲方验收报告》

#### Scenario: 提交报告后推进到验收

- **GIVEN** 一个 交付实施 + WON 的商机，已提交 DELIVERY_ACCEPTANCE_REPORT
- **WHEN** `POST /{id}/advance` body `{}`
- **THEN** SHALL 返回 200
- **AND** stage SHALL 为 ACCEPTANCE
- **AND** status SHALL 仍为 WON

#### Scenario: 新产出物类型可独立提交

- **GIVEN** 一个 交付实施 商机
- **WHEN** `POST /{id}/artifacts` body `{type:"DELIVERY_ACCEPTANCE_REPORT", title:"X验收", content:"功能全部通过"}`
- **THEN** SHALL 返回 201 并持久化（type=DELIVERY_ACCEPTANCE_REPORT, content 非空）
