# Capability: frontend-scaffold — v0.0.46 contract-artifacts delta (MODIFIED)

> 合并入 canonical `specs/frontend-scaffold/spec.md`（Phase 6）。售前流转 BIDDING/CONTRACT 补充表单路由。

## MODIFIED Requirements (from change 2026-06-23-contract-artifacts / v0.0.46)

### Requirement: 售前流转 投标/合同 关口补充产出物

`opportunityArtifact.ts` SHALL 注册 6 个新类型（标签/链接类）并在 `STAGE_REQUIRED_ARTIFACTS` 新增 `BIDDING:[BID_DOCUMENT]`
与 `CONTRACT:[中标公示,合同,评审会议纪要,邮件归档,已盖章合同]`。售前流转中 BIDDING/CONTRACT 行点「通过」SHALL 走「补充产出物并
推进」表单（缺则按 kind 填链接/正文，链接类可多份、无需标题；齐则直接推进）；点「否决」SHALL 跳过补充表单、直接确认丢单
（产出物仅 PASS 需要）。

#### Scenario: 投标通过弹补充表单

- **GIVEN** 售前流转有一条 BIDDING 商机，无产出物
- **WHEN** 用户点该行「通过」
- **THEN** SHALL 弹出补充表单，含《投标文件》链接输入（可多份）
- **AND** SHALL NOT 直接调 advance

#### Scenario: 投标否决直接确认丢单（不弹补充表单）

- **GIVEN** 售前流转有一条 BIDDING 商机，无产出物
- **WHEN** 用户点该行「否决」
- **THEN** SHALL 弹「确认否决（丢单）」对话框
- **AND** SHALL NOT 弹补充产出物表单

#### Scenario: 合同补充五件后推进

- **GIVEN** 售前流转有一条 CONTRACT 商机，无产出物
- **WHEN** 用户点「通过」、在补充表单填 中标公示/合同/邮件归档/已盖章合同(链接) + 评审会议纪要(正文) 后「提交并推进」
- **THEN** SHALL 逐条 `createOpportunityArtifact` 后 `advanceOpportunity(id, 'PASS')`
