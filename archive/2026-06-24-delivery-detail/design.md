# Design: v0.0.54 — DeliveryFlow 详情抽屉

## Context

PresaleFlow.tsx 有成熟的详情抽屉：read-only 字段 + 编辑(updateOpportunity) + 添加产出物 + 产出物列表(预览/导出/链接)。DeliveryFlow.tsx 仅有行操作按钮。Opportunity 字段含 customerName/title/note/amount/productId+productName/commercialOwnerUserId+Name/solutionOwnerUserId+Name/pmUserId+pmName/opsOwnerUserId+opsOwnerName/gateDecidedBy/stage/status/projectId。共享 api：updateOpportunity、listCustomers、listProducts、listUsers、listOpportunityArtifacts、createOpportunityArtifact、exportArtifactDocx；共享 util：isLinkArtifact、ARTIFACT_TYPE_LABELS、ADDABLE_ARTIFACT_TYPES、MarkdownView。

## Decisions

### D1: 本地移植，不抽共享组件（本次）
将 PresaleFlow 详情抽屉的 read-only + 编辑 + 添加产出物 + 列表 移植进 DeliveryFlow（testid 前缀 `delivery-detail-*`/`delivery-add-*`），复用全部共享 api/util/MarkdownView。
- 不抽共享组件原因：PresaleFlow 详情抽屉与其推进逻辑(advanceFromDetail/OPP_GATE_STAGES/artifactReq)紧耦合，干净抽取需改动并回归 PresaleFlow（已上线、测试多）。本地移植零回归风险、最快交付用户实际诉求。
- Follow-up：后续可抽 `<OpportunityDetailDrawer>` 共享组件（flag task）。

### D2: 推进不进抽屉
DeliveryFlow 推进语义特殊（INITIATION=立项移交抽屉、SURVEY=补充产出物表单、其它=直接 advance），已在行上实现。详情抽屉**不含**推进按钮，避免重复/冲突这套门禁路由。详情=查看/编辑/产出物。

### D3: 编辑表单字段
复用 PresaleFlow 的 prefillDetail/saveDetail：客户(datalist 选已有/填新)、标题、备注、金额、产品(下拉)、四负责人(下拉)。saveDetail 校验客户+标题非空、金额数字，匹配 customers 取 customerId，调 updateOpportunity，成功刷新 detailOpp + load()。

### D4: 抽屉打开即加载产出物
detailOpp 变更时 listOpportunityArtifacts(id) → detailArts；编辑/添加用户与产品/客户列表在打开时按需加载（懒加载，空才拉）。

## Architecture

```
DeliveryFlow 行「详情」(delivery-detail-{id}) → setDetailOpp(r)
  └ useEffect[detailOpp]: prefillDetail + listOpportunityArtifacts
Drawer(detailOpp):
  read-only 字段 / [编辑]→edit form→saveDetail(updateOpportunity)
  产出物: [添加产出物]→submitAddArtifact(createOpportunityArtifact) + 列表(预览 MarkdownView / 导出 exportArtifactDocx / 链接)
```

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 与 PresaleFlow 结构重复（drift） | 复用共享 api/util；flag follow-up 抽共享组件 |
| 编辑 WON 商机字段是否允许 | updateOpportunity 无阶段限制（与 PresaleFlow 一致）；仅改非阶段字段 |
| 与行上推进/补充表单 state 冲突 | 详情 state 独立(detailOpp/detailArts/编辑/添加)；推进保持行上不动 |
| testid 与 presale 混淆 | 用 `delivery-detail-*` 前缀 |
