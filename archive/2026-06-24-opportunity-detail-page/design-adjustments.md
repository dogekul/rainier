# Design Adjustments — v0.0.55 opportunity-detail-page

Phase 5 评审（Step 0 单代理对抗审查）：**C:0 H:0 M:0 L:3**。无阻塞缺陷。

## 已补（评审 Low — 覆盖缺口）
- **TC-ODP-07**：添加链接类产出物（切换类型为 SURVEY_ATTACHMENT → 提交 `link` 而非 `content`），覆盖 add-artifact 的 link 分支。
- **TC-ODP-08**：非法 id（`/crm/opportunities/abc`）→ 错误态且不调 getOpportunity，覆盖 `Number.isFinite` 守卫。

## 记录未改（评审确认非缺陷）
- L1 `prefill` 作为 effect 隐式依赖：纯函数（仅依赖入参 o），无 stale 风险，lint 干净（--max-warnings 0），无需 eslint-disable。
- L2 StrictMode 开发期 effect 双跑 → getOpportunity/loadArts 各两次：dev-only、幂等、测试未启用 StrictMode，TC-ODP-04 的 times(2) 稳定。
- L3 saveDetail 不刷新列表：本页无列表；返回时流转页 remount 触发其 load() 重取，编辑可见，无 stale。

## 范围外（Gate 1 已定）
- 看板(OpportunityBoardPage) 未接入「行→详情页」跳转 —— Gate 1 明确「看板暂不接入」。可作后续扩展。

## 设计意图变更（取代 v0.0.54）
- v0.0.54 的 DeliveryFlow 详情抽屉 + PresaleFlow 详情抽屉 **均被移除**，统一为 `/crm/opportunities/:id` 详情页。推进/门禁保留在两个流转列表页的行上（详情页不含推进）。这同时消除了两份抽屉的结构重复（无需再抽共享组件）。
