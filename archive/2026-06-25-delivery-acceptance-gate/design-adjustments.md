# Design Adjustments — v0.0.57 delivery-acceptance-gate

## 已改
- **TC-OSEA-02**: 原"DELIVERY → ACCEPTANCE 是无门禁样例"假设失效；测试前先 `seedArtifact(DELIVERY_ACCEPTANCE_REPORT)` 再 advance，断言不变。
- **TC-OAR-006**: 原"非门禁转换"主旨失效；重命名 `deliveryAdvance_withAcceptanceReport_advancesToAcceptance`，改测 Path A 内联建档 + advance 端到端（与 LEAD→OPPORTUNITY 的 TC-OAR-002 同形）。

## 设计意图
- 客户全流程图末关 DELIVERY → ACCEPTANCE 落实门禁，与图一致。
- 复用现有 Path A（`persistRequiredArtifact` 在 size==1 && !isLink 且 advance 携带 ArtifactInput 时内联建档），advance() / persist 逻辑零改动。
- 前端复用 `STAGE_REQUIRED_ARTIFACTS` + DeliveryFlow 既有 supplement form（v0.0.53 SURVEY 同条路径），只需新增类型常量与 STAGE_REQUIRED_ARTIFACTS 一项。

## 隐含但已通过的考量
- 所有可推进阶段现在都有门禁/特殊接口：每条"非门禁转换"测试都需 seed 一个产出物或改换语义。本次只触及 TC-OSEA-02/TC-OAR-006；其它阶段早已有门禁，无新增 fixup。
- 前端 testid 不变（`delivery-supp-DELIVERY_ACCEPTANCE_REPORT` / `delivery-supp-content-...`）— 自动由现成模板生成，无需新增测试。
