# Capability: opportunity — v0.0.52 handoff-advance delta (MODIFIED)

> 合并入 canonical `specs/opportunity/spec.md`（Phase 6）。立项移交即完成立项并推进到现场调研。见 [[frontend-scaffold]] / [[entity-project]]。

## MODIFIED Requirements (from change 2026-06-24-handoff-advance / v0.0.52)

### Requirement: 立项移交即推进（PASS 链项目 + 进入现场调研）

`POST /api/opportunities/{id}/initiate` SHALL 要求商机在 **立项(INITIATION)** 且 WON（否则 409）。PASS 时：关联/新建对外-交付项目后，
SHALL 将 stage 推进到下一阶段「现场调研」(SURVEY)、刷新 stageEnteredAt（status 仍 WON）。REJECT SHALL 仅记录决策、停留在立项（不链项目、不推进）。

#### Scenario: 立项移交 PASS 推进到现场调研

- **GIVEN** 一个 立项(INITIATION) + WON 的商机
- **WHEN** `POST /{id}/initiate` body `{projectId(对外-交付), decision:"PASS"}`
- **THEN** SHALL 返回 200，projectId SHALL 关联、stage SHALL 为 SURVEY、status SHALL 为 WON

#### Scenario: 立项移交 内联新建 也推进

- **GIVEN** 一个 立项 + WON 的商机
- **WHEN** `POST /{id}/initiate` body `{projectName, projectOwnerUserId, decision:"PASS"}`
- **THEN** SHALL 返回 200，新建对外-交付项目并关联、stage SHALL 为 SURVEY

#### Scenario: 非立项阶段不可立项

- **GIVEN** 一个 现场调研(SURVEY) + WON 的商机
- **WHEN** `POST /{id}/initiate` body `{projectId, decision:"PASS"}`
- **THEN** SHALL 返回 409
