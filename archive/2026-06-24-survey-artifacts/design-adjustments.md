# Design Adjustments — v0.0.53 survey-artifacts

Phase 5 评审（Step 0，单代理对抗审查）发现 2 项 Medium，均已在本版修复（无需推迟）：

## ADJ-1 (M1): requestAdvance 增加在途守卫
- **原设计**：DeliveryFlow `requestAdvance` 直接镜像 PresaleFlow，无重复触发保护。
- **调整**：`requestAdvance` 顶部加 `if (busyId===r.id || suppOpp?.id===r.id) return`，并在异步 `listOpportunityArtifacts` 查询窗口期间 `setBusyId(r.id)`，开表单/直接推进前再清空。防止 lookup 未返回时快速双击导致 SURVEY→REQUIREMENT→DELIVERY 双跳。
- **影响**：DeliveryFlow.tsx requestAdvance；行为更稳健，测试不变（仍 9/9 绿）。

## ADJ-2 (M2): advance 失败可见化
- **原设计**：DeliveryFlow `advance()` 仅 try/finally，后端推进 400（如门禁/状态冲突）被静默吞掉。
- **调整**：`advance()` 加 catch + 新增 `advError` 页级错误条（testid `delivery-adv-error`），展示后端友好消息（如「此转换需先提交产出物：《...》」）。与 PresaleFlow 的 advError UX 对齐。
- **影响**：DeliveryFlow.tsx advance + 渲染；纯增强。

## 未调整（评审确认可接受）
- L1 ADDABLE_ARTIFACT_TYPES 含 SURVEY 两类型 → 售前 详情 可给非 SURVEY 商机加现场调研产出物（松散但无害，沿用既有 picker 语义）。
- L2 STAGE_REQUIRED_ARTIFACTS 以 string 为键（镜像后端，既有模式）。
- 后端 advance/persistRequiredArtifact 零改动（设计意图「advance 逻辑不动」成立，多类型 Path B 自动覆盖 SURVEY）。
