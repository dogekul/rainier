# Test Plan — v0.0.45-gate-artifacts

> Baseline backend 488 / frontend 203 green. New + updated TCs below; all P0. 表数 23→24.

## 测试策略

- 后端集成（@SpringBootTest + MockMvc，H2）= 门禁（缺产出物 400 / 带产出物建+流转）+ 列查 + 导出 docx（字节合法）+ 既有 advance 不回归。
- 前端组件（Vitest，mock api）= 售前流转 产出物表单（报告/纪要）+ 商机看板 产出物抽屉/导出；既有用例随门禁更新。
- E2E（Docker 真 MySQL + temurin-8 含 POI）= 门禁挡住→带产出物放行→列查→导出 .docx（PK 头 + content-type）；表数 24；存量零改。

## 详细测试案例

### opportunity-artifact（后端，OpportunityArtifactTest）

| TC-ID | Scenario | 预期 |
|---|---|---|
| TC-OAR-001 | LEAD advance 无 artifact | 400 |
| TC-OAR-002 | LEAD advance 带 report | 200；stage=OPPORTUNITY；新建 RESEARCH_REPORT(stageFrom=LEAD) |
| TC-OAR-003 | OPPORTUNITY PASS 无 artifact | 400 |
| TC-OAR-004 | OPPORTUNITY PASS 带 minutes | 200；stage=POC；新建 DECISION_MINUTES(decision=PASS) |
| TC-OAR-005 | OPPORTUNITY REJECT 带 minutes | 200；status=LOST；新建 DECISION_MINUTES(decision=REJECT) |
| TC-OAR-006 | 非门禁转换(POC→BIDDING) 无 artifact | 200（不受影响） |
| TC-OAR-007 | artifact 标题/正文空 | 400 |
| TC-OAR-008 | 列查 GET /{id}/artifacts | 倒序返回；append-only 无写端点 |
| TC-OAR-009 | 导出 GET /{id}/artifacts/{aid}/export | 200；content-type wordprocessingml；体为合法 docx(PK 头) |

### opportunity 回归 + 产品标签（后端，OpportunityControllerTest 更新）

| TC-ID | Scenario | 预期 |
|---|---|---|
| TC-OPP-003* | LEAD→OPPORTUNITY 现需带 report（+断言生成 RESEARCH_REPORT） | 更新：发 artifact → 200/OPPORTUNITY |
| TC-OPP-005* | OPPORTUNITY PASS→POC 现需带 minutes（+断言 DECISION_MINUTES/PASS） | 更新：发 decision+artifact → 200/POC |
| TC-OPP-007/012/014 | CONTRACT/INITIATION（非门禁） | 不变，全绿 |
| TC-OPP-015 | 创建带 productId | 201；productId 持久化 + productName enriched |
| TC-OPP-016 | 创建带不存在的 productId | 400 |

### frontend（PresaleFlow / OpportunityBoard）

| TC-ID | Scenario | 预期 |
|---|---|---|
| TC-PRE-02* | 商机通过 → 弹纪要表单 → 提交 | 更新：填纪要后 advance(id,'PASS',_,artifact) |
| TC-PAR-01 | 线索推进 → 弹《商机调研报告》表单 → 提交 | advance(id,undefined,_,{title,content}) |
| TC-PAR-02 | 商机否决 → 弹《决策评审纪要》表单 → 提交 | advance(id,'REJECT',_,artifact) |
| TC-PAR-03 | 产出物表单标题/正文空 → 报错不提交 | formError；advance 未调 |
| TC-OBA-01 | 商机看板 产出物按钮 → 抽屉列查 + 导出入口 | listArtifacts 调；导出按钮在 |
| TC-OBA-02 | 看板仍无流转控件 | 无 opp-new/pass/reject/advance |
| TC-PRE-03* | 新建商机抽屉含产品下拉，选产品 → create 带 productId | productId 传入；产品下拉渲染 |
| TC-OPPB-01* | 看板卡片显示产品标签 | opp-product-{id} 显示 productName |

### 表数 + E2E

| TC-ID | Scenario | 预期 |
|---|---|---|
| TC-CRM-TABLES* | LegacyProductCategoryCleanupTest | 24 |
| TC-E2E-OAR-001 | 门禁挡住→带产出物放行→列查→导出 docx | 链路通；docx 合法 |
| TC-E2E-OAR-002 | 存量业务数据不变（仅 +1 表） | 数据零改 |

## 回归风险矩阵

| 区域 | 风险 | 缓解 |
|---|---|---|
| 改既有 advance 语义（LEAD/OPPORTUNITY 需产出物） | 🟡中 | 同步更新 OpportunityControllerTest + PresaleFlow 既有用例；E2E 走带产出物路径 |
| Apache POI / Java-8 / temurin-8 构建 | 🟡中 | 钉 poi-ooxml 4.1.2；Docker 后端重建验证；导出走 byte[] |
| 看板只读语义 | 🟢低 | 产出物查看/导出为只读；测试断言「无流转控件」 |
| 1 新表 | 🟢低 | ddl-auto；表数测试更新 24 |
| 存量数据 | 🟢低 | 纯新增表 + 增量门禁，不动既有商机 |
