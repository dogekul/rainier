# Proposal: v0.0.54 — 实施中商机查看详情（DeliveryFlow 详情抽屉）

## Why

用户反馈：「实施中的商机，应该同样可以查看详情。」售前流转(PresaleFlow) 每行有「详情」抽屉（商机字段 + 可编辑 + 流转产出物历史 + 添加产出物 + 导出 Word），但实施流转(DeliveryFlow) 的行只有操作按钮（立项移交/推进/驳回），无法查看商机详情，也看不到已提交的流转产出物（现场调研报告/附件、合同等）。实施阶段同样需要查看/编辑商机与回看产出物。

经 Gate 1 确认：做**完整详情（可编辑商机字段）**。

## What Changes

- DeliveryFlow 每行新增「详情」按钮 → 打开详情抽屉（镜像 PresaleFlow 的详情抽屉）：
  - 只读展示：客户·标题、阶段、状态、最近决策人、备注、金额、产品、商务/解决方案/项目经理/运营。
  - 「编辑」切换 → 可改 客户/标题/备注/金额/产品/四负责人，保存调 `updateOpportunity`。
  - 产出物区：「添加产出物」表单（报告类填标题+正文、链接类填 URL）+ 产出物历史列表（类型/标题/来源/决策/作者 + 链接「打开」或报告「预览/导出 Word」）。
- 推进操作**仍留在行上**（立项移交/驳回/推进 + 现场调研补充表单），详情抽屉不含推进，避免与行上门禁逻辑重复。

## Capabilities

- Modified: `frontend-scaffold`（DeliveryFlow 详情抽屉）。New: 无。**无后端改动**（复用 `updateOpportunity` / `listCustomers` / `listProducts` / `listUsers` / `listOpportunityArtifacts` / `createOpportunityArtifact` / `exportArtifactDocx`）。

## Impact

- 代码：`DeliveryFlow.tsx`（+详情/编辑/添加产出物 state + 详情抽屉 JSX，本地移植 PresaleFlow 模式，复用共享 api/util）+ 测试。
- 后端契约：无变化。无新表/列/依赖/接口。
- 数据：仅在用户主动编辑/添加产出物时写对应商机；不动其它数据。
- 已知取舍：详情抽屉为 DeliveryFlow 本地实现（与 PresaleFlow 有结构重复）；推进不进抽屉。后续可抽共享组件（flag follow-up），不破坏既有 PresaleFlow。

## Success Criteria

- [ ] DeliveryFlow 每行有 `delivery-detail-{id}`；点击打开详情抽屉（`delivery-detail-body`）。
- [ ] 抽屉只读展示商机字段；「编辑」可改并 `updateOpportunity` 保存后刷新。
- [ ] 抽屉列出该商机的流转产出物；报告类可预览/导出 Word、链接类可打开；可「添加产出物」。
- [ ] 推进仍在行上（抽屉无推进按钮），行为不回归。
- [ ] 前端全绿 + tsc + lint clean。
