# Capability: frontend-scaffold — v0.0.45 gate-artifacts delta (MODIFIED)

> 合并入 canonical `specs/frontend-scaffold/spec.md`（Phase 6）。仅新增/修改以下 Requirements。

## MODIFIED Requirements (from change 2026-06-23-gate-artifacts / v0.0.45)

### Requirement: 「售前流转」产出物表单

> v0.0.45 增量：要求产出物的转换，操作按钮先弹产出物表单，提交即流转。

「售前流转」中，对来源阶段在 `OPP_TRANSITION_ARTIFACT` 内的行：线索行「推进」SHALL 弹《商机调研报告》表单（标题+正文），
商机行「通过」/「否决」SHALL 弹《决策评审纪要》表单（标题+正文，记录对应 decision）；提交 SHALL 调
`advanceOpportunity(id, decision?, note?, artifact)` 即「建产出物+流转」；标题/正文空 SHALL 表单报错且不提交。不在映射内的
转换（推介POC 推进 / 投标·合同 否决）SHALL 沿用 v0.0.44 行为。

#### Scenario: 线索推进弹报告表单

- **GIVEN** 售前流转有一条 LEAD 商机
- **WHEN** 用户点该行「推进」并填《商机调研报告》标题+正文后提交
- **THEN** SHALL 调 `advanceOpportunity(id, undefined, …, {title,content})`

#### Scenario: 商机通过弹纪要表单

- **GIVEN** 售前流转有一条 OPPORTUNITY 商机
- **WHEN** 用户点该行「通过」并填《决策评审纪要》后提交
- **THEN** SHALL 调 `advanceOpportunity(id, 'PASS', …, {title,content})`

### Requirement: 「商机看板」产出物查看 + 导出 Word

「商机看板」（只读）SHALL 为每个商机提供「产出物」入口，打开只读抽屉列出该商机产出物（消费
`GET /api/opportunities/{id}/artifacts`），每条提供「导出 Word」（带鉴权拉取 `.../export` 的 .docx 并下载）。看板 SHALL
仍不提供任何**流转**操作控件（无新建/推进/通过/否决）。

#### Scenario: 看板查看产出物并导出

- **GIVEN** 某商机有产出物
- **WHEN** 用户在商机看板点该商机「产出物」
- **THEN** SHALL 打开抽屉列出产出物
- **AND** 每条 SHALL 提供「导出 Word」入口；看板 SHALL NOT 渲染流转操作按钮（新建/推进/通过/否决）
