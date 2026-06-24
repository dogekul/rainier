# Proposal: v0.0.57 — 交付实施→验收 推进门禁（甲方验收报告）

## Why

客户全流程图最末一关 交付实施(DELIVERY) → 验收(ACCEPTANCE) 当前无任何门禁，与图上规定不符。需要提交《甲方验收报告》才能进入验收阶段。

经 Gate 1：单个产出物 = **甲方验收报告**（报告类，富文本正文 + 标题；与「现场调研报告」「商机调研报告」「评审纪要」同语义）。

## What Changes

- **后端**：
  - `ArtifactType` 新增 `DELIVERY_ACCEPTANCE_REPORT`（《甲方验收报告》，报告类）。
  - `TransitionArtifactRules` 新增 `DELIVERY → [DELIVERY_ACCEPTANCE_REPORT]`。advance 逻辑零改动（沿用已存在的 Path B 多产出物校验）。
  - 既有测试影响：TC-OSEA-02（stageEnteredAt 刷新） / TC-OAR-006（非门禁转换）当前用 DELIVERY 作「非门禁样例」，现需提前 seed 甲方验收报告再 advance；同时新增 TC-ACPT-01/02 覆盖新门禁。
- **前端**：
  - `opportunityArtifact.ts`：新增 `'DELIVERY_ACCEPTANCE_REPORT'` 类型 + 标签 + `STAGE_REQUIRED_ARTIFACTS[DELIVERY]`（非链接类）+ 可添加列表。
  - 实施流转 DELIVERY 行点「推进」→ 自动复用已有的「补充产出物并推进」表单（已为 SURVEY 实现，按 stage 路由），让用户填 甲方验收报告 标题+正文，提交后建档 + advance。

## Capabilities

- Modified: `opportunity`、`frontend-scaffold`。New: 无。无后端依赖/新表/新列。

## Impact

- 代码：`ArtifactType.java` / `TransitionArtifactRules.java` / `opportunityArtifact.ts` 加常量；advance/UI 流转零改动（自动经现成路径）。
- 数据：仅作用本次推进的商机；不动其它存量数据。
- 测试：后端 TC-ACPT-01/02 + 修复 TC-OSEA-02 / TC-OAR-006。前端 +DEL-supp-DELIVERY-* 覆盖。

## Success Criteria

- [ ] DELIVERY/WON 商机未提交报告 → advance 返回 400，消息含「甲方验收报告」。
- [ ] 提交 DELIVERY_ACCEPTANCE_REPORT 后 advance → 200，stage = ACCEPTANCE。
- [ ] 实施流转 DELIVERY 行点推进 → 弹出「补充产出物并推进」表单，提交即建档+推进。
- [ ] 后端 temurin-8 全绿 + 前端全绿 + tsc / lint clean。
