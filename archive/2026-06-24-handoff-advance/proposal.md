# Proposal: v0.0.52 — 立项移交即推进（建/关联项目 + 进入现场调研）

## Why

用户反馈：「立项移交建立项目之后，为什么状态没有变化？」。现 `initiate()`（立项移交）PASS 仅设 `projectId`，**不改阶段/状态** —— 商机停留在「立项」，需另点「通过」才推进。三按钮（立项移交/通过/否决）且立项移交不前进，违反直觉。

## What Changes

- `initiate` PASS：要求商机在 **立项(INITIATION)** 且 WON；关联/新建对外-交付项目后，**推进到下一阶段「现场调研」(SURVEY)** 并刷新 stageEnteredAt —— 立项移交即完成立项、进入实施，状态可见前进。
- `initiate` REJECT：仅记录决策、停留在立项（不链项目、不推进）= 驳回立项。
- 非「立项」阶段的 WON 商机调用 initiate → 409（守卫）。
- 前端 DeliveryFlow「立项」行去掉多余的独立「通过」按钮（它会绕过项目直接推进，不正确）；改为 **立项移交（主操作，即推进）+ 驳回**。

## Capabilities

- Modified: `opportunity`（initiate 语义：PASS 推进 + INITIATION 守卫）、`frontend-scaffold`（DeliveryFlow 立项行按钮）。New: 无。

## Impact

- 代码：`OpportunityService.initiate`（+INITIATION 守卫、PASS 推进 SURVEY + stageEnteredAt）；前端 `DeliveryFlow.tsx`（去「通过」按钮）+ 测试。
- 后端契约：`POST /{id}/initiate` 行为增强（PASS 后 stage=SURVEY）；请求体不变。无新表/列/依赖。
- 数据：仅作用于本次操作的商机（推进其阶段）；不动其它数据。

## Success Criteria

- [ ] 立项(INITIATION/WON) 商机 initiate PASS → 200，projectId 关联、**stage=SURVEY**、status=WON。
- [ ] initiate REJECT → 停留 INITIATION（不链不进）。
- [ ] 非 INITIATION 的 WON 商机 initiate → 409。
- [ ] 前端立项行：立项移交 + 驳回（无独立「通过」）；立项移交后刷新可见进入「现场调研」。
- [ ] 后端 temurin-8 全绿 + 前端全绿 + E2E 绿。
