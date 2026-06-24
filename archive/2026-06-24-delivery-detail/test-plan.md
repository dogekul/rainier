# Test Plan: v0.0.54 — DeliveryFlow 详情抽屉

## 测试策略

前端 Vitest + RTL（DeliveryFlow.test.tsx 扩展）。mock：opportunity(updateOpportunity)、opportunityArtifact(list/create/export)、customer(listCustomers)、product(listProducts)、user(listUsers)。无后端改动 → 无后端用例。

## 详细测试案例（frontend-scaffold）

| TC-ID | 场景 | 预期 |
|-------|------|------|
| TC-DDET-01 | 行点「详情」 | 打开 `delivery-detail-body`，显示客户·标题；调 listOpportunityArtifacts |
| TC-DDET-02 | 详情列出产出物 | 报告类有 `delivery-detail-export-{id}`，链接类有 `delivery-detail-link-{id}` |
| TC-DDET-03 | 详情编辑保存 | 点编辑改标题→`delivery-detail-save`→updateOpportunity 收到新标题 + 刷新 |
| TC-DDET-04 | 添加产出物 | 打开添加表单填链接→保存→createOpportunityArtifact 调用 + 重新拉列表 |
| TC-DDET-05 | 抽屉无推进 | `delivery-detail-body` 内无推进/立项移交/驳回按钮 |
| TC-DEL-01（保持） | 行操作 testid 不回归 | 仍有 delivery-handoff/reject/advance；新增 delivery-detail-{id} |

## 测试执行矩阵

| 功能 | 单元/集成 |
|------|----------|
| 详情入口/打开/加载产出物 | TC-DDET-01/02 |
| 编辑保存 | TC-DDET-03 |
| 添加产出物 | TC-DDET-04 |
| 推进不在抽屉 | TC-DDET-05 |

## 回归风险矩阵

| 区域 | 风险 | 缓解 |
|------|------|------|
| DeliveryFlow 既有行操作/handoff/supplement | 🟡中（新增 state/JSX 同文件） | 既有 9 用例保持 + TC-DDET-* |
| 共享 api 复用 | 🟢低（只读+既有写接口） | mock + tsc |
| PresaleFlow | 🟢无（不改动） | — |

## 建议补充顺序

P0：TC-DDET-01/02/03/04/05 + 既有 9 用例回归。
