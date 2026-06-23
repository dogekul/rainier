# Proposal: v0.0.46 — 投标→合同→立项 产出物门禁

## Why

客户全流程售前末段（投标 → 合同签订 → 进入立项）目前无产出物门禁——可空手推进。本版为这 2 个关口转换补齐必需产出物，把
「中标 → 合同评审 → 邮件留痕 → 盖章 → 立项」的关键交付物沉淀为可追溯证据，复用 v0.0.45 的产出物门禁机制。

附件本版**先以 URL 占位**（贴外部链接），后续再迁移对象存储——故本版**不引入文件上传 / 邮件发送基建**。

## What Changes

- 投标 BIDDING → 合同（PASS=中标）SHALL 要求《投标文件》（链接，≥1，可多份）。
- 合同 CONTRACT → 立项（PASS）SHALL 要求《中标公示》(链接) +《合同》(链接) +《评审会议纪要》(报告) +《邮件归档》(链接) +
  《已盖章合同》(链接) 全齐。CONTRACT PASS 同时 `status=WON` + 进入 INITIATION（既有语义）。
- 新增 6 个 ArtifactType（5 链接类 + 评审会议纪要 报告类）。
- 产出物门禁改为**仅 PASS（前进）强制**：投标/合同关口 REJECT（丢单）不要求产出物；商机决策（OPPORTUNITY）的《决策评审纪要》
  保持「通过/否决都留痕」不变。
- 单产出物内联创建路径（Path A）加 `!isLink` 守卫：链接类单一规则（投标文件）统一走预提交校验（Path B）。
- 前端售前流转：BIDDING/CONTRACT 点「通过」走「补充产出物并推进」表单（链接类可多份、报告类填正文）；点「否决」直接丢单。

## Capabilities

### Modified Capabilities
- `opportunity-artifact`：+6 ArtifactType；+2 `TransitionArtifactRules` 规则（BIDDING/CONTRACT）；门禁仅 PASS 强制 +
  Path A `!isLink` 守卫。
- `opportunity`：`advance` 产出物门禁语义微调（关口 REJECT 不强制前进交付物）。
- `frontend-scaffold`：售前流转 BIDDING/CONTRACT「通过」走补充表单；注册 6 类型 + `STAGE_REQUIRED_ARTIFACTS`(+BIDDING/CONTRACT)。

### New Capabilities
- 无（附件先 URL 占位，不新建 file-storage / email capability）。

## Impact

- **代码（后端）**：`ArtifactType`(+6 常量/ALL/LINK_TYPES/LABELS)、`TransitionArtifactRules`(+2 规则 + requiredOnReject)、
  `OpportunityService.persistRequiredArtifact`(PASS-only + Path A `!isLink`)。无 DTO 改动（链接/报告/title-optional 已支持）。
- **代码（前端）**：`api/opportunityArtifact.ts`(union/labels/link-types/STAGE_REQUIRED_ARTIFACTS/addable)、
  `pages/Crm/PresaleFlow.tsx`(`requestAdvance` 对 REJECT 跳过补充表单)。
- **配置**：无（无新依赖、无 SMTP/multipart 配置）。
- **基础设施**：无（无新表，表数仍 25；无 Docker 卷、无邮件服务器）。
- **回归**：现有 `advance_contractPass_wonAndEntersDelivery`（CONTRACT PASS 无产出物）会因新门禁失败 → 需在该测试先 seed 5 件
  必需产出物（预期改动）。`advance_gateReject_lost`（BIDDING REJECT）依赖 PASS-only 不回归。

## Success Criteria

- [ ] BIDDING 缺《投标文件》PASS → 400；有 ≥1 → 200/CONTRACT
- [ ] 《投标文件》可贴多个 URL（多份），单条无需标题
- [ ] CONTRACT 缺任一必需件 PASS → 400 列出缺项；五件齐 → 200/INITIATION + status=WON
- [ ] BIDDING / CONTRACT REJECT → 200/LOST，且**不**要求/创建产出物
- [ ] OPPORTUNITY 商机决策《决策评审纪要》仍 PASS/REJECT 都留痕（不回归）
- [ ] 前端售前流转 BIDDING/CONTRACT「通过」弹补充表单、「否决」直接确认丢单
- [ ] backend 全量 + frontend 全量 + tsc/lint 绿；temurin-8 实测；E2E（真 Docker）门禁链路绿
- [ ] standing 约束：不删改用户存量商机/客户数据
