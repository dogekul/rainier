# Proposal: v0.0.53 — 现场调研产出物门禁（SURVEY → 产品诉求 推进需提交资料）

## Why

用户反馈：「立项移交后的现场调研 推进到下一步，需要提交资料。」现 现场调研(SURVEY) 经「推进」按钮**无任何约束**直接进入 产品诉求(REQUIREMENT)，调研成果（报告 + 现场材料）不留痕、无门禁。需在 SURVEY → REQUIREMENT 这一转换上加产出物门禁。

经 Gate 1 确认：提交「多份材料（报告 + 附件）」，全齐才能推进。

## What Changes

- 后端 `ArtifactType` 新增两类产出物：`SURVEY_REPORT`（《现场调研报告》，报告类=富文本，可 Word 导出）+ `SURVEY_ATTACHMENT`（《现场调研附件》，链接类=URL，可多份）。
- 后端 `TransitionArtifactRules` 新增 `SURVEY → [SURVEY_REPORT, SURVEY_ATTACHMENT]`（多产出物门禁，两类全齐才能推进；走已有 Path B 预提交+存在性校验）。advance/gate 逻辑无需改动（已支持任意来源阶段的产出物规则）。
- 前端 `DeliveryFlow`「现场调研」行的「推进」改为：缺产出物时弹出「补充产出物并推进」表单（报告填标题+正文、附件填链接可多份），提交即逐个建档再 advance；产出物齐备则直接推进。其他实施环节（产品诉求/交付）仍为无门禁直接推进。
- 前端 `opportunityArtifact.ts` 同步新增两类型的标签 / 链接标记 / `STAGE_REQUIRED_ARTIFACTS[SURVEY]` / 可添加列表。

## Capabilities

- Modified: `opportunity`（SURVEY 转换产出物门禁）、`frontend-scaffold`（DeliveryFlow 补充产出物表单）。New: 无。

## Impact

- 代码：`ArtifactType.java`（+2 常量 / ALL / LINK_TYPES / LABELS）、`TransitionArtifactRules.java`（+SURVEY 规则）；前端 `DeliveryFlow.tsx`（补充表单）、`opportunityArtifact.ts`（+2 类型）+ 测试。
- 后端契约：`POST /{id}/advance` FROM SURVEY 现在强制产出物（缺失 → 400 列出《...》）；`POST /{id}/artifacts` 接受新两类型。无新表/列/依赖。
- 数据：仅作用于本次推进的商机（建产出物 + 推进阶段）；不动其它存量数据。
- 既有测试影响：后端 TC-OSEA-02（曾用 SURVEY 作「无门禁」非关口样例）改用 REQUIREMENT；前端 TC-DEL-02（曾直接 advance SURVEY）改为「SURVEY 推进打开补充表单」。

## Success Criteria

- [ ] SURVEY/WON 商机 未备齐产出物 → advance 返回 400，消息含《现场调研报告》《现场调研附件》。
- [ ] 备齐两类产出物后 advance → 200，stage=REQUIREMENT。
- [ ] `POST /{id}/artifacts` 接受 SURVEY_REPORT（content）与 SURVEY_ATTACHMENT（link）。
- [ ] 前端 SURVEY 行「推进」缺产出物时打开补充表单；提交逐个建档 + 推进；产品诉求/交付行仍直接推进。
- [ ] 后端 temurin-8 全绿 + 前端全绿 + E2E 绿（含 SURVEY 门禁链路）。
