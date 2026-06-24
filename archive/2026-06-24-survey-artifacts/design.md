# Design: v0.0.53 — 现场调研产出物门禁

## Context

OpportunityStage 顺序 ...INITIATION→SURVEY→REQUIREMENT→DELIVERY→ACCEPTANCE。GATE_STAGES={OPPORTUNITY,BIDDING,CONTRACT,INITIATION}；SURVEY 是非关口阶段（advance 无 decision）。`TransitionArtifactRules.RULES: Map<sourceStage, List<artifactType>>` + `requiredFor(stage)`，`OpportunityService.advance()` 调 `persistRequiredArtifact(o, stage, decision, artifact)`：

- `required.isEmpty()` → no-op。
- 单一报告类型（required.size()==1 && !isLink && artifact{title,content}）→ Path A 内联建档。
- 否则 → Path B：从 `artifactRepo` 查已存类型集合，逐个比对 required，缺失 → 400 列出《label》。

多产出物阶段（POC/CONTRACT）已走 Path B（预提交 via `POST /artifacts`）。SURVEY 加 2 类型规则后**自动套用 Path B**，advance/gate 逻辑零改动。

## Decisions

### D1: 两类产出物 SURVEY_REPORT(报告) + SURVEY_ATTACHMENT(链接)
Gate 1 选「多份材料（报告+附件）」。`SURVEY_REPORT` 报告类（富文本 content，可 Word 导出，与 商机调研报告/评审纪要 一致）；`SURVEY_ATTACHMENT` 链接类（URL link，可多份，承载现场照片/记录等外部材料，沿用 v0.0.46 的「URL 占位」附件方案）。两类全齐才能推进。
- 备选：单报告（Path A 内联）— 被 Gate 1 否决（用户要多份材料）。

### D2: 走 Path B（多产出物预提交），不改 advance/gate
`required.size()==2` 必然落入 Path B；advance 时不内联建档。前端补充表单负责先 `POST /artifacts` 建 2 类，再 advance。后端 `persistRequiredArtifact` / `advance()` 一行不改。
- 风险：低。POC/CONTRACT 已验证同路径。

### D3: 前端复用 PresaleFlow 的补充表单模式
DeliveryFlow 移植 PresaleFlow 的 `STAGE_REQUIRED_ARTIFACTS` 驱动的「补充产出物并推进」表单（suppOpp/suppTypes/suppData state + setSuppField/setSuppLink/addSuppLink/removeSuppLink + submitSupplement + Drawer）。「推进」按钮经 `requestAdvance(r)` 路由：`STAGE_REQUIRED_ARTIFACTS[r.stage]` 有定义且缺产出物 → 开表单；否则直接 advance。testid 前缀用 `delivery-supp-*`（与 presale-supp 区分）。
- 备选：抽公共组件 — 暂不（两页表单细节略异，过早抽象；本版先复制，后续可重构）。

### D4: testid 命名
`delivery-supp-{type}` / `delivery-supp-title-{type}` / `delivery-supp-content-{type}` / `delivery-supp-link-{type}-{idx}` / `delivery-supp-addlink-{type}` / `delivery-supp-rmlink-{type}-{idx}` / `delivery-supp-error` / `delivery-supp-save`。

## Architecture

```
DeliveryFlow「推进」(SURVEY) → requestAdvance(r)
  └ STAGE_REQUIRED_ARTIFACTS['SURVEY']=[SURVEY_REPORT,SURVEY_ATTACHMENT]
     └ listOpportunityArtifacts → 缺失? → 开补充表单(suppOpp=r)
        └ submitSupplement: 报告 createArtifact{content} / 附件 createArtifact{link}(每条)
           └ advanceOpportunity(id, undefined) → 后端 persistRequiredArtifact Path B 校验通过 → stage=REQUIREMENT
```

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 既有 TC-OSEA-02 用 SURVEY 作「无门禁」样例会回归失败 | 改用 REQUIREMENT（非关口、无规则），断言 next=DELIVERY |
| 既有前端 TC-DEL-02 直接 advance SURVEY 会失败 | 改为断言「SURVEY 推进打开补充表单」；新增 REQUIREMENT 直接推进用例 |
| Java 8：无 Set.of/List.of | 用 Arrays.asList / Collections.unmodifiableList（沿用现有写法）|
| 前端 Record<ArtifactType,...> 需补齐新成员 | ARTIFACT_TYPE_LABELS 必须加 2 成员否则 tsc 报错 |
