# Test Plan: v0.0.55 — 统一商机详情页

## 测试策略

前端 Vitest + RTL。新增 OpportunityDetailPage.test.tsx（MemoryRouter + initialEntries `/crm/opportunities/:id` + Routes，mock getOpportunity/listOpportunityArtifacts/createOpportunityArtifact/updateOpportunity/listCustomers/listProducts/listUsers）。改写 PresaleFlow/DeliveryFlow 行「详情」测试为跳转断言（mock useNavigate）。无后端改动 → 无后端用例。

## 详细测试案例（frontend-scaffold）

| TC-ID | 场景 | 预期 |
|-------|------|------|
| TC-ODP-01 | 访问 /crm/opportunities/7 | 调 getOpportunity(7)+listOpportunityArtifacts(7)；渲染 opp-detail-page，显示客户·标题+阶段中文 |
| TC-ODP-02 | 产出物列表 | 报告类有 opp-detail-export-{id}、链接类有 opp-detail-link-{id} |
| TC-ODP-03 | 编辑保存 | 改标题→保存→updateOpportunity 收到新值（+customerId 按名匹配）+ 刷新 |
| TC-ODP-04 | 添加产出物 | 打开添加→填→保存→createOpportunityArtifact 调用 + 重拉列表 |
| TC-ODP-05 | 加载失败/无效 id | getOpportunity 拒绝 → opp-detail-error 态 |
| TC-ODP-06 | 返回 | 点 opp-detail-back → navigate(-1) |
| TC-PRE-NAV | 售前流转行「详情」 | 点 presale-detail-{id} → navigate('/crm/opportunities/'+id)，不开抽屉(presale-detail-body 不存在) |
| TC-DEL-NAV | 实施流转行「详情」 | 点 delivery-detail-{id} → navigate('/crm/opportunities/'+id)，不开抽屉(delivery-detail-body 不存在) |
| 既有回归 | 售前/实施推进/create/handoff/supplement | 不回归（保留行上） |

## 测试执行矩阵

| 功能 | 单元/集成 |
|------|----------|
| 详情页加载/渲染/产出物 | TC-ODP-01/02 |
| 编辑/添加 | TC-ODP-03/04 |
| 错误/返回 | TC-ODP-05/06 |
| 行详情跳转 | TC-PRE-NAV / TC-DEL-NAV |
| 推进/门禁不回归 | PresaleFlow/DeliveryFlow 既有用例 |

## 回归风险矩阵

| 区域 | 风险 | 缓解 |
|------|------|------|
| PresaleFlow 删抽屉 | 🟡中（大文件） | KEEP 清单 + tsc/eslint + 全量回归 |
| DeliveryFlow 删 v0.0.54 抽屉 | 🟡中 | 同上；TC-DDET-* 改写/移除 |
| 新路由 :id | 🟢低 | 页面测试 + AppRoutes grep |
| 共享 api 复用 | 🟢低 | mock + tsc |

## 建议补充顺序

P0：TC-ODP-01..06 + TC-PRE-NAV/TC-DEL-NAV + 两页既有回归。
