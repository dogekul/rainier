# Capability: opportunity — v0.0.53 survey-artifacts delta (MODIFIED)

> 合并入 canonical `specs/opportunity/spec.md`（Phase 6）。现场调研(SURVEY)→产品诉求(REQUIREMENT) 转换的产出物门禁。见 [[frontend-scaffold]]。

## MODIFIED Requirements (from change 2026-06-24-survey-artifacts / v0.0.53)

### Requirement: 现场调研推进需备齐产出物（报告 + 附件）

`POST /api/opportunities/{id}/advance` 当来源阶段为 **现场调研(SURVEY)** 时 SHALL 要求两类产出物全部已存在：《现场调研报告》(SURVEY_REPORT，报告类) + 《现场调研附件》(SURVEY_ATTACHMENT，链接类)。缺任一 SHALL 返回 400 且消息列出缺失的《...》；齐备 SHALL 推进到 产品诉求(REQUIREMENT) 并刷新 stageEnteredAt。两类型通过 `POST /api/opportunities/{id}/artifacts` 预提交（报告填 content、附件填 link）。

#### Scenario: 现场调研缺产出物不可推进

- **GIVEN** 一个 现场调研(SURVEY) + WON 的商机，未提交任何产出物
- **WHEN** `POST /{id}/advance` body `{}`
- **THEN** SHALL 返回 400
- **AND** 消息 SHALL 含《现场调研报告》与《现场调研附件》

#### Scenario: 备齐报告+附件后推进到产品诉求

- **GIVEN** 一个 现场调研(SURVEY) + WON 的商机，已提交 SURVEY_REPORT(content) + SURVEY_ATTACHMENT(link)
- **WHEN** `POST /{id}/advance` body `{}`
- **THEN** SHALL 返回 200
- **AND** stage SHALL 为 REQUIREMENT
- **AND** status SHALL 仍为 WON

#### Scenario: 仅备齐报告仍不可推进

- **GIVEN** 一个 现场调研(SURVEY) + WON 的商机，仅提交了 SURVEY_REPORT
- **WHEN** `POST /{id}/advance` body `{}`
- **THEN** SHALL 返回 400
- **AND** 消息 SHALL 含《现场调研附件》

#### Scenario: 新产出物类型可独立提交

- **GIVEN** 一个 现场调研(SURVEY) 商机
- **WHEN** `POST /{id}/artifacts` body `{type:"SURVEY_ATTACHMENT", link:"https://x/site.jpg"}`
- **THEN** SHALL 返回 201/200 并持久化该产出物（type=SURVEY_ATTACHMENT, link 非空）
