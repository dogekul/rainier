# Capability: opportunity-artifact — v0.0.45 (NEW)

> NEW capability。流转产出物：可配置「转换→所需产出物」门禁 + append-only 产出物记录 + Word 导出。新表
> rainier_opportunity_artifact。见 [[opportunity]] / [[frontend-scaffold]] / [[entity-audit-log]]。

## ADDED Requirements

### Requirement: 转换产出物门禁

`advance()` 从某阶段推进时，若该阶段在 `TransitionArtifactRules` 中要求产出物（线索→`RESEARCH_REPORT`、商机→
`DECISION_MINUTES`），则 `OpportunityAdvanceRequest` SHALL 携带 `artifact{title,content}`（title/content 非空），否则
SHALL 返回 400；满足则 SHALL 在同一事务内创建 `OpportunityArtifact`（type 由来源阶段推导、stageFrom=来源阶段、
decision 记入纪要）并完成推进/决策。不在规则内的转换 SHALL 忽略 artifact 照常推进。

#### Scenario: 线索推进缺报告被拒

- **GIVEN** 商机在 LEAD
- **WHEN** `POST /{id}/advance`（无 artifact）
- **THEN** SHALL 返回 400

#### Scenario: 线索带报告推进

- **GIVEN** 商机在 LEAD
- **WHEN** `POST /{id}/advance` body `{artifact:{title:"调研",content:"…"}}`
- **THEN** SHALL 返回 200，stage SHALL 为 OPPORTUNITY
- **AND** SHALL 新建一条 type=RESEARCH_REPORT、stageFrom=LEAD 的产出物

#### Scenario: 商机决策缺纪要被拒

- **GIVEN** 商机在 OPPORTUNITY
- **WHEN** `POST /{id}/advance` body `{decision:"PASS"}`（无 artifact）
- **THEN** SHALL 返回 400

#### Scenario: 商机通过带纪要

- **GIVEN** 商机在 OPPORTUNITY
- **WHEN** `POST /{id}/advance` body `{decision:"PASS",artifact:{title:"评审",content:"…"}}`
- **THEN** SHALL 返回 200，stage SHALL 为 POC
- **AND** SHALL 新建 type=DECISION_MINUTES、decision=PASS 的产出物

#### Scenario: 商机否决带纪要丢单

- **GIVEN** 商机在 OPPORTUNITY
- **WHEN** `POST /{id}/advance` body `{decision:"REJECT",artifact:{title:"评审",content:"…"}}`
- **THEN** SHALL 返回 200，status SHALL 为 LOST
- **AND** SHALL 新建 type=DECISION_MINUTES、decision=REJECT 的产出物

#### Scenario: 非门禁转换不受影响

- **GIVEN** 商机在 POC（无产出物规则）
- **WHEN** `POST /{id}/advance`（无 artifact）
- **THEN** SHALL 返回 200，stage SHALL 为 BIDDING

### Requirement: 产出物列查（append-only）

后端 SHALL 提供 `GET /api/opportunities/{id}/artifacts`，倒序返回该商机的全部产出物（含 type/stageFrom/title/content/
decision/作者/时间）。SHALL NOT 提供产出物的独立创建/修改/删除端点（仅经 advance 门禁创建）。

#### Scenario: 列出某商机产出物

- **GIVEN** 商机经线索推进 + 商机通过，产生 2 条产出物
- **WHEN** `GET /api/opportunities/{id}/artifacts`
- **THEN** SHALL 返回 2 条，最新在前

### Requirement: 产出物导出 Word

后端 SHALL 提供 `GET /api/opportunities/{id}/artifacts/{artifactId}/export`，返回该产出物的 .docx（Apache POI 生成），
`Content-Type` 为 `application/vnd.openxmlformats-officedocument.wordprocessingml.document`，附 `Content-Disposition:
attachment`；文档内容 SHALL 含 标题/类型/客户·商机/阶段/决策/作者/时间/正文。

#### Scenario: 导出合法 docx

- **GIVEN** 存在一条产出物
- **WHEN** `GET /api/opportunities/{id}/artifacts/{artifactId}/export`
- **THEN** SHALL 返回 200
- **AND** 响应体 SHALL 为合法 .docx（ZIP/OOXML，PK 头），Content-Type SHALL 为 wordprocessingml.document

## ADDED Requirements (v0.0.45 fold-ins)

> 后续 fold-in，扩展产出物模型与门禁。完整测试见 `archive/2026-06-23-gate-artifacts/test-report.md`。
> v0.0.46（投标→合同→立项 产出物）将在此基础上新增 ArtifactType + `TransitionArtifactRules` 规则。

### Requirement: 产出物 kind（链接 / 报告）+ 独立提交

`ArtifactType` SHALL 区分链接类（`LINK_TYPES`，存 `link` URL）与报告类（存 `content` 富文本）。后端 SHALL 提供
`POST /api/opportunities/{id}/artifacts` 独立提交一条产出物（用于推进前准备）：链接类 SHALL 校验 `link` 非空，报告类
SHALL 校验 `content` 非空；`title` 可空（链接类无需标题），为空时 SHALL 用类型名兜底。产出物仍 append-only（无改/删）。

#### Scenario: 链接类无标题提交

- **WHEN** `POST /{id}/artifacts` body `{type:"PRESENTATION_MATERIAL", link:"https://x/ppt"}`（无 title）
- **THEN** SHALL 返回 201，title SHALL 兜底为类型名「讲解材料」、link SHALL 为该 URL

#### Scenario: 链接类缺链接被拒

- **WHEN** `POST /{id}/artifacts` body `{type:"PRESENTATION_MATERIAL"}`（无 link）
- **THEN** SHALL 返回 400

### Requirement: 多产出物门禁（POC→投标）

`TransitionArtifactRules` SHALL 支持单阶段要求多类产出物（`Map<stage, List<type>>`）。POC→投标 SHALL 要求
讲解材料 + 甲方诉求清单 + POC 得分表 + 差距分析报告 全部已存在（经 `POST /{id}/artifacts` 预提交），缺则 advance
SHALL 返回 400 并列出缺失的《类型名》；齐全 SHALL 推进至 BIDDING。

#### Scenario: POC 缺产出物被拒

- **GIVEN** 商机在 POC，未提交任何 POC 产出物
- **WHEN** `POST /{id}/advance`
- **THEN** SHALL 返回 400，消息 SHALL 列出 4 类缺失产出物

#### Scenario: POC 备齐推进

- **GIVEN** 商机在 POC，4 类必需产出物均已提交
- **WHEN** `POST /{id}/advance`
- **THEN** SHALL 返回 200，stage SHALL 为 BIDDING
