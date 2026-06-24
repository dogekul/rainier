# Proposal: v0.0.55 — 统一商机详情页（/crm/opportunities/:id）

## Why

商机详情内容多（字段 + 流转产出物 + 各阶段留痕），抽屉(Drawer)装不下也不可深链。v0.0.54 在实施流转、更早在售前流转各做了一份抽屉详情，结构重复且会 drift。用户决定：改用**独立详情页**承载，售前 + 实施统一进入同一页面（取代两处抽屉）。

经 Gate 1 确认：独立详情页 `/crm/opportunities/:id`；售前+实施统一；推进/门禁仍留在列表行上；本版不做需要后端的「推进时间线」、暂不接入看板。

## What Changes（前端 only）

- `api/opportunity.ts` 新增 `getOpportunity(id)`（调已存在的后端 `GET /api/opportunities/{id}`，无后端改动）。
- 新建 `pages/Crm/OpportunityDetailPage.tsx` + 路由 `/crm/opportunities/:id`（首个 id 路由）。`useParams` 取 id → fetch → 渲染：
  - **概览**：客户/标题/阶段(中文)/状态/金额/产品/四负责人/关联项目/备注/最近决策人 + 「编辑」(updateOpportunity)。
  - **流转产出物**：全部产出物（类型/标题/来源阶段/决策/作者/时间 — 报告类预览(Markdown)/导出 Word、链接类打开）+ 「添加产出物」。
  - 顶部「返回」。
- `PresaleFlow.tsx` / `DeliveryFlow.tsx`：移除各自详情抽屉 + 相关 state/函数；行「详情」改 `navigate('/crm/opportunities/'+id)`。**推进/门禁保留在行上**（售前关口/补充表单、实施立项移交/现场调研补充）。

## Capabilities

- Modified: `frontend-scaffold`（新增详情页路由 + 两流转页详情入口改跳转）。New: 无。**无后端 / 表 / 依赖改动**。

## Impact

- 代码：`api/opportunity.ts`(+getOpportunity)、新 `OpportunityDetailPage.tsx`、`AppRoutes.tsx`(+route)、`PresaleFlow.tsx` / `DeliveryFlow.tsx`(去抽屉+跳转) + 测试。
- 取代 v0.0.54 实施抽屉与 PresaleFlow 抽屉的重复（详情页 = 共享详情面）。
- 数据：仅在用户编辑/添加产出物时写对应商机；不动其它数据。
- 测试：新增 `OpportunityDetailPage.test.tsx`；改写 PresaleFlow/DeliveryFlow 的 detail 测试为「行点击 → 跳转 /crm/opportunities/:id」；移除 v0.0.54 抽屉内联测试。

## Success Criteria

- [ ] 路由 `/crm/opportunities/:id` 渲染详情页：fetch 该商机 + 列出产出物；含概览/编辑/添加产出物/返回。
- [ ] 编辑保存 `updateOpportunity` 后刷新；添加产出物 `createOpportunityArtifact` 后刷新列表；报告导出/链接打开可用。
- [ ] 售前流转、实施流转 行「详情」→ 跳转到 `/crm/opportunities/:id`（不再开抽屉）。
- [ ] 推进/门禁仍在两页行上、行为不回归。
- [ ] 前端全绿 + tsc + lint clean。
