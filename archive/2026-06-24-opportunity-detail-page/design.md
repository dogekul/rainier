# Design: v0.0.55 — 统一商机详情页

## Context

后端已存 `GET /api/opportunities/{id}` → OpportunityDetail（OpportunityController:65）。前端 api/opportunity.ts 无 get-by-id。售前(PresaleFlow)/实施(DeliveryFlow) 各有一份详情抽屉（view+编辑 updateOpportunity + 产出物 list/add + 预览 MarkdownView/导出 exportArtifactDocx/链接）。路由 AppRoutes 扁平、无 :id 路由先例。页面 wrapper 约定：`pages/Crm/<X>Page.tsx` default-export 组件。

## Decisions

### D1: 独立详情页承载，取代抽屉
新建 `OpportunityDetailPage`（route `/crm/opportunities/:id`），`useParams` 取 id → `getOpportunity(id)` + `listOpportunityArtifacts(id)`。全宽分区（概览 + 流转产出物），不再用 Drawer。售前/实施行「详情」`navigate` 进入。两页共用此页 = 消除抽屉重复。

### D2: getOpportunity(id) — 前端补 api，零后端
`api/opportunity.ts` 加 `getOpportunity(id): client.get('/opportunities/'+id)`，镜像 listOpportunities/updateOpportunity 写法。后端端点已存在。

### D3: 推进/门禁不进详情页
售前关口(通过/否决 + artifactReq/supplement)、实施(立项移交/现场调研补充/驳回) 的推进逻辑复杂且已在列表行实现；详情页只做 查看/编辑/产出物，避免迁移门禁路由的风险与重复。详情页「编辑」用 updateOpportunity（不改 stage/status）。

### D4: 详情页编辑/添加产出物 = 复用既有抽屉逻辑
prefill + saveDetail(updateOpportunity, 按客户名匹配 customerId) + 添加产出物(createOpportunityArtifact) + 产出物列表渲染 —— 从抽屉平移到页面。加载 customers/products/users 供编辑下拉。saveDetail 带 catch（沿用 v0.0.54 修复）。

### D5: 移除两页抽屉
PresaleFlow：删 detail state(detailOpp/detailArts/d*/addArt*/previewArtId) + detail useEffect + prefillDetail/saveDetail/openAddArtifact/submitAddArtifact/advanceFromDetail + detailRow/STATUS_LABEL + 详情 Drawer JSX；KEEP ownerSelect(create 抽屉用)/customers/products/users/artifactReq/supplement/requestAdvance/create 抽屉。详情按钮 → navigate。
DeliveryFlow：删 v0.0.54 detail state/函数/Drawer + detailRow/STATUS_LABEL；KEEP handoff/supplement/advance。详情按钮 → navigate。
tsc + eslint(no-unused) 兜底悬挂引用。

### D6: testid
详情页：`opp-detail-page` / `opp-detail-back` / `opp-detail-edit` / `opp-detail-title` 等(沿用 detail-* 字段名) / `opp-detail-add-artifact` / `opp-detail-artifact-{id}` / `opp-detail-export-{id}` / `opp-detail-link-{id}` / `opp-detail-loading` / `opp-detail-error`。行「详情」testid 不变（presale-detail-{id} / delivery-detail-{id}），仅行为改为跳转。

## Architecture

```
PresaleFlow/DeliveryFlow 行「详情」 → navigate(`/crm/opportunities/${id}`)
OpportunityDetailPage: useParams.id
  → getOpportunity(id) → opp;  listOpportunityArtifacts(id) → arts
  概览(detailRow 字段) / [编辑]→ updateOpportunity → 刷新
  产出物([添加]→createOpportunityArtifact / 预览 MarkdownView / 导出 exportArtifactDocx / 链接)
  [返回]→ navigate(-1)
```

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| PresaleFlow 大文件删抽屉伤及推进/create | 精准删 detail-only；KEEP 清单(D5)；tsc/eslint + 全量 PresaleFlow 测试回归 |
| 既有 detail 测试(presale-detail-*/TC-DDET-*) 失效 | 改写为「点击行详情→断言 navigate」；新增页面测试 |
| 深链 fetch 失败/不存在 id | opp-detail-error 态 + 加载态 opp-detail-loading |
| 首个 :id 路由 | AppLayout/ProtectedRoute 包裹一致；route 放 /crm 组 |
