# Capability: opportunity-artifact — v0.0.46 contract-artifacts delta (MODIFIED)

> 合并入 canonical `specs/opportunity-artifact/spec.md`（Phase 6）。投标→合同→立项 产出物门禁。附件先 URL 占位（链接类）。
> 见 [[opportunity]] / [[frontend-scaffold]]。

## MODIFIED / ADDED Requirements (from change 2026-06-23-contract-artifacts / v0.0.46)

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
