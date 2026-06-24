# Test Plan: v0.0.53 — 现场调研产出物门禁

## 测试策略

后端 mockMvc 集成（OpportunityControllerTest，复用 seedOpp/seedArtifact）；前端 Vitest + RTL（DeliveryFlow.test.tsx，复用 mock）。E2E：docker 重建后 curl SURVEY 门禁链路。standing 约束：不删改存量数据。

## 详细测试案例

### opportunity（后端）

| TC-ID | 场景 | 预期 |
|-------|------|------|
| TC-SUR-01 | SURVEY/WON 未备产出物 advance `{}` | 400，消息含《现场调研报告》《现场调研附件》 |
| TC-SUR-02 | SURVEY/WON 备齐 报告+附件 advance `{}` | 200，stage=REQUIREMENT，status=WON |
| TC-SUR-03 | SURVEY/WON 仅备报告 advance `{}` | 400，消息含《现场调研附件》 |
| TC-SUR-04 | `POST /{id}/artifacts` type=SURVEY_ATTACHMENT link | 持久化成功，type/link 正确 |
| TC-OSEA-02（改） | 非关口无门禁 advance 刷新 stageEnteredAt | 改用 REQUIREMENT→DELIVERY（原 SURVEY 现已有门禁）|

### frontend-scaffold（前端）

| TC-ID | 场景 | 预期 |
|-------|------|------|
| TC-DEL-02（改） | SURVEY 行点「推进」 | 打开补充表单（delivery-supp-SURVEY_REPORT/SURVEY_ATTACHMENT），不立即 advance |
| TC-DEL-08 | 补充表单填报告正文+附件链接，点提交 | createOpportunityArtifact 各调用 + advanceOpportunity(id, undefined) + 刷新 |
| TC-DEL-09 | REQUIREMENT 行点「推进」 | 直接 advanceOpportunity(id, undefined)（不开表单）|

### E2E

| TC-ID | 场景 | 预期 |
|-------|------|------|
| TC-E2E-SUR-01 | SURVEY 商机 advance 无产出物 | 400 含《...》；POST 2 产出物后 advance → SURVEY→REQUIREMENT |

## 测试执行矩阵

| 功能 | 单元/集成 | E2E |
|------|----------|-----|
| SURVEY 门禁缺失 400 | TC-SUR-01/03 | TC-E2E-SUR-01 |
| 备齐推进 | TC-SUR-02 | TC-E2E-SUR-01 |
| 新类型可提交 | TC-SUR-04 | TC-E2E-SUR-01 |
| 前端补充表单 | TC-DEL-02/08/09 | — |

## 回归风险矩阵

| 区域 | 风险 | 缓解 |
|------|------|------|
| advance gate 逻辑 | 🟢低（零改动，仅加规则） | TC-SUR-* + 全量回归 |
| TransitionArtifactRules | 🟡中（新规则影响 SURVEY 推进语义） | TC-OSEA-02 改用 REQUIREMENT |
| DeliveryFlow 推进路由 | 🟡中（推进按钮行为按阶段分流） | TC-DEL-02/08/09 |
| ArtifactType 前端 union | 🟢低（tsc 强制补齐 LABELS） | tsc + lint |

## 建议补充顺序

P0：TC-SUR-01/02/03/04、TC-DEL-02改/08/09、TC-OSEA-02改 → P0：TC-E2E-SUR-01。
