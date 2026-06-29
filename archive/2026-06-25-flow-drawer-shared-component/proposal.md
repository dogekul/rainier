# D8 — PresaleFlow / DeliveryFlow 抽屉共用组件抽象 (v0.0.96)

## 痛点
`PresaleFlow.tsx` 与 `DeliveryFlow.tsx` 各自本地维护了一份「推进时补充必需产出物」抽屉：
- state：`suppOpp / suppTypes / suppData / suppError / suppSaving`
- helpers：`setSuppField / setSuppLink / addSuppLink / removeSuppLink`
- 提交：`submitSupplement` —— 逐个 `createOpportunityArtifact` 后回调 `advance`
- JSX：≈ 90 行的 Drawer 表单（报告类 title+content，链接类多份 URL，错误条 + footer）

两份实现一字不差（除 `data-testid` 前缀 + banner 文案 + 推进时是否带 `decision`）。
任何字段或交互变化都得改两遍，是真实的重复代码。

## 范围
1. NEW `frontend/src/components/flow/ArtifactSupplementDrawer.tsx`
   - props：`{ opportunityId, missingTypes, message?, testIdPrefix, onClose, onAdvance }`
   - 内部托管 form state，每次 `missingTypes` 变更（即父开新的一次推进）自动重置
   - 提交：校验 → 逐个 `createOpportunityArtifact` → `await onAdvance(opportunityId)`
   - 父保留「推进决策」（如 PASS/REJECT），通过闭包传给 `onAdvance`
2. 改 `PresaleFlow.tsx`：删除 ~90 行内联 form + 5 个 helpers + `submitSupplement`，
   换成 `<ArtifactSupplementDrawer .../>`
3. 改 `DeliveryFlow.tsx`：同上
4. 新增 `ArtifactSupplementDrawer.test.tsx`：5 项覆盖（不渲染/报告校验/链接校验/双类型提交+回调/增删链接行）

## 范围外
- 后端不动
- 不重做 PresaleFlow 的另一个产出物抽屉 (`artifactReq` 单文档 inline form)：那份逻辑（advance + 内联 artifact `title/content`）只 Presale 有，无 DeliveryFlow 镜像 → 不构成重复
- 不重做创建/立项移交 drawer：这些是各自独有的业务表单，无重复
- 不引入「FlowDetailDrawer / StageProgressBar / ArtifactList」抽象：当前 PresaleFlow / DeliveryFlow 不存在"流转详情抽屉"，是 list 页 + 各自专用抽屉；抽这些会创造无 caller 的死组件。聚焦真实重复（补充产出物）

## 验收
- `npx tsc --noEmit` 干净
- 5 项新测全绿
- `PresaleFlow.test.tsx`（16）+ `DeliveryFlow.test.tsx`（11）回归全绿
- 全前端 280 项测试全绿
