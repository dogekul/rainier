# Design Adjustments — v0.0.54 delivery-detail

Phase 5 评审（Step 0 单代理对抗审查）C:0 H:0 M:3 L:6。两项已修复：

## ADJ-1 (M3): saveDetail 错误可见化
- **原**：移植自 PresaleFlow 的 `saveDetail` 仅 try/finally，无 catch → `updateOpportunity` 拒绝时静默无操作（用户无反馈 + 未处理 rejection）。
- **调整**：加 catch，设 `dError`（展示后端友好 message），与本页 `doHandoff`/`advance`/`submitSupplement` 的错误处理对齐。
- **影响**：DeliveryFlow.tsx saveDetail。纯增强。

## ADJ-2 (L1): TC-DDET-03 增强 customerId 匹配断言
- **原**：仅断言 `updateOpportunity` 收到新标题；customers mock 为空 → 按名解析 customerId 的分支从未被测。
- **调整**：TC-DDET-03 mock `listCustomers` 命中「X 集团」(id=42)，断言 `updateOpportunity` 收到 `customerId:42` + `customerName`。
- **影响**：DeliveryFlow.test.tsx。覆盖增强。

## 记录未改（评审确认）
- **M1**（users 跨 handoff/detail 抽屉共享 + length 守卫）：当前无 live bug（两处同一 listUsers 载荷）。记为潜在耦合，留待后续抽 `<OpportunityDetailDrawer>` 共享组件时统一处理（见 follow-up）。
- **M2**（可编辑 WON/终态商机字段，含金额）：Gate 1 明确选「完整可编辑」；v0.0.15 审计切面记录每次 update，编辑有留痕。是否对「已验收」终态收口编辑 = 产品决策，Gate 3 提请确认。
- L2-L6：评审正向确认（测试有效/无障碍一致/无死代码/effect 闭包安全/导出文件名唯一）。

## Follow-up（flag）
- 后续可抽 `<OpportunityDetailDrawer>` 共享组件，消除 DeliveryFlow 与 PresaleFlow 详情抽屉的结构重复（含 ADJ-1 的 catch 应同步回 PresaleFlow.saveDetail）。
