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

## MODIFIED / ADDED Requirements (v0.0.46 contract-artifacts)

> 合并自 change `2026-06-23-contract-artifacts`（Phase 6）。投标→合同→立项 产出物门禁；附件先 URL 占位（链接类）。
> 见 [[opportunity]] / [[frontend-scaffold]]。

### Requirement: 投标 / 合同 转换产出物门禁

`TransitionArtifactRules` SHALL 新增两条规则：投标 BIDDING → `[BID_DOCUMENT]`；合同 CONTRACT →
`[BID_WINNING_NOTICE, CONTRACT_DRAFT, CONTRACT_REVIEW_MINUTES, REVIEW_EMAIL_ARCHIVE, SIGNED_CONTRACT]`。`ArtifactType`
SHALL 新增这 6 个类型（除 `CONTRACT_REVIEW_MINUTES` 为报告类外，其余 5 类为链接类）。这些产出物经 `POST /{id}/artifacts`
独立提交；advance（PASS）时 SHALL 校验全部已存在，缺则返回 400 并列出缺失《类型名》。

#### Scenario: 投标缺投标文件被拒

- **GIVEN** 商机在 BIDDING（OPEN），未提交投标文件
- **WHEN** `POST /{id}/advance` body `{decision:"PASS"}`
- **THEN** SHALL 返回 400，消息 SHALL 含《投标文件》
- **AND** stage SHALL 仍为 BIDDING

#### Scenario: 投标有投标文件中标

- **GIVEN** 商机在 BIDDING，已提交 ≥1 条 BID_DOCUMENT（链接）
- **WHEN** `POST /{id}/advance` body `{decision:"PASS"}`
- **THEN** SHALL 返回 200，stage SHALL 为 CONTRACT

#### Scenario: 合同缺件被拒并列出缺项

- **GIVEN** 商机在 CONTRACT，仅提交了《合同》与《中标公示》
- **WHEN** `POST /{id}/advance` body `{decision:"PASS"}`
- **THEN** SHALL 返回 400，消息 SHALL 列出缺失的《评审会议纪要》《邮件归档》《已盖章合同》

#### Scenario: 合同五件齐进入立项赢单

- **GIVEN** 商机在 CONTRACT，已提交 中标公示+合同+评审会议纪要+邮件归档+已盖章合同
- **WHEN** `POST /{id}/advance` body `{decision:"PASS"}`
- **THEN** SHALL 返回 200，stage SHALL 为 INITIATION
- **AND** status SHALL 为 WON

### Requirement: 关口否决不要求前进产出物（仅 PASS 强制）

产出物门禁 SHALL 仅对前进（PASS / 非关口推进）强制。关口（GATE_STAGES）以 REJECT 否决时 SHALL NOT 要求/创建前进交付物，
但 `OPPORTUNITY` 商机决策的《决策评审纪要》SHALL 在 PASS 与 REJECT 两种结果都要求（决策留痕）。

#### Scenario: 投标否决丢单不需产出物

- **GIVEN** 商机在 BIDDING（OPEN），无任何产出物
- **WHEN** `POST /{id}/advance` body `{decision:"REJECT"}`
- **THEN** SHALL 返回 200，status SHALL 为 LOST
- **AND** SHALL NOT 因缺投标文件而 400

#### Scenario: 合同否决丢单不需产出物

- **GIVEN** 商机在 CONTRACT（OPEN），无任何产出物
- **WHEN** `POST /{id}/advance` body `{decision:"REJECT"}`
- **THEN** SHALL 返回 200，status SHALL 为 LOST

#### Scenario: 商机否决仍需纪要（不回归）

- **GIVEN** 商机在 OPPORTUNITY（OPEN）
- **WHEN** `POST /{id}/advance` body `{decision:"REJECT"}`（无 artifact）
- **THEN** SHALL 返回 400（《决策评审纪要》仍必需）

### Requirement: 单产出物内联创建仅限报告类

advance 表单内联创建产出物（Path A）SHALL 仅适用于单一规则且该类型为报告类（`!isLink`）。链接类单一规则（如投标文件）
SHALL 走预提交校验（须经 `POST /{id}/artifacts` 先建），不在 advance 表单内联创建。

#### Scenario: 投标文件不内联创建

- **GIVEN** 商机在 BIDDING，未预提交投标文件
- **WHEN** `POST /{id}/advance` body `{decision:"PASS", artifact:{title:"投标", content:"x"}}`
- **THEN** SHALL 返回 400（链接类不接受内联 content，仍判缺投标文件）
