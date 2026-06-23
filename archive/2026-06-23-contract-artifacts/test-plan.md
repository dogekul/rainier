# Test Plan: v0.0.46 — 投标→合同→立项 产出物门禁

## 测试策略

复用 v0.0.45 门禁测试模式（`OpportunityArtifactTest` 的 `postArtifact` + advance helper）。后端：投标/合同门禁
+ PASS-only 语义 + Path A 守卫。前端：补充表单路由（PASS 弹 / REJECT 跳过）。E2E：真 Docker 走 BIDDING→CONTRACT→INITIATION。
回归：更新既有 CONTRACT-PASS 测试先 seed 产出物。

## 详细测试案例（后端，加入 OpportunityArtifactTest）

| TC-ID | 场景 | 预期 |
|---|---|---|
| TC-CAR-001 | BIDDING PASS 缺投标文件 | 400 含《投标文件》，stage 不变 |
| TC-CAR-002 | BIDDING 提交 ≥1 BID_DOCUMENT(link) 后 PASS | 200 / CONTRACT |
| TC-CAR-003 | BIDDING 多份 BID_DOCUMENT(2 链接,无标题) | 2 条落库 + PASS → CONTRACT |
| TC-CAR-004 | CONTRACT PASS 缺件 | 400 列出缺失《评审会议纪要》《邮件归档》《已盖章合同》 |
| TC-CAR-005 | CONTRACT 五件齐 PASS | 200 / INITIATION + status=WON |
| TC-CAR-006 | BIDDING REJECT 无产出物 | 200 / LOST（不 400） |
| TC-CAR-007 | CONTRACT REJECT 无产出物 | 200 / LOST（不 400） |
| TC-CAR-008 | BIDDING PASS 带内联 artifact(title+content) | 400（链接类不内联，仍判缺投标文件） |
| TC-CAR-009 | 新类型 label/kind | BID_DOCUMENT..已盖章合同 在 ALL；5 链接类 isLink=true；评审会议纪要 isLink=false |

## 回归（更新既有测试）

| 既有测试 | 改动 |
|---|---|
| `OpportunityControllerTest.advance_contractPass_wonAndEntersDelivery` (TC-OPP-007) | 先 seed 5 件 CONTRACT 必需产出物再 PASS |
| `OpportunityControllerTest.advance_gateReject_lost` (TC-OPP-006, BIDDING REJECT) | 不变（PASS-only 守护，保持绿） |

## 详细测试案例（前端，加入 PresaleFlow.test）

| TC-ID | 场景 | 预期 |
|---|---|---|
| TC-FCAR-01 | BIDDING「通过」 | 弹补充表单，含 `presale-supp-BID_DOCUMENT` + 链接输入 `presale-supp-link-BID_DOCUMENT-0`，未调 advance |
| TC-FCAR-02 | BIDDING「否决」 | 弹确认否决对话框，未弹补充表单 |
| TC-FCAR-03 | CONTRACT「通过」补齐 5 件提交 | createArtifact ×5 + advance(id,'PASS') |

## 测试执行矩阵

| 功能 | 后端单元 | 前端组件 | E2E |
|---|---|---|---|
| 投标门禁 | TC-CAR-001/002/003/008 | TC-FCAR-01 | ✓ |
| 合同门禁 | TC-CAR-004/005 | TC-FCAR-03 | ✓ |
| PASS-only(REJECT) | TC-CAR-006/007 | TC-FCAR-02 | — |
| 类型注册 | TC-CAR-009 | union/labels tsc | — |

## 回归风险矩阵

| 区域 | 风险 | 缓解 |
|---|---|---|
| `persistRequiredArtifact`（advance 核心路径） | 🔴 高 | PASS-only + Path A 守卫均加回归测试；OPPORTUNITY/LEAD/POC 既有测试守护 |
| 既有 CONTRACT-PASS 测试 | 🟡 中 | 显式更新 seed 产出物 |
| 前端 requestAdvance 路由 | 🟡 中 | REJECT 跳过补充的新测试 + 既有 POC 补充测试守护 |

## 建议补充顺序

P0：TC-CAR-001..009 + 回归更新 → TC-FCAR-01..03 → E2E。
