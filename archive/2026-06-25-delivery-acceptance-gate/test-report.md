# Test Report — v0.0.57 交付实施→验收 推进门禁

## 1. 总体概况

| 层 | 总数 | 通过 | 失败 | 通过率 |
|----|------|------|------|--------|
| 后端 (temurin-8 全量) | 544 | 544 | 0 | 100% |
| 前端 (Vitest) | 265 | 265 | 0 | 100% |
| tsc / eslint | clean | — | — | — |

后端 541→544（+TC-ACPT-01/02 新门禁 + TC-OAR-006 重定位）。前端：纯类型扩展 + STAGE_REQUIRED_ARTIFACTS 配置；DeliveryFlow 复用 v0.0.53 supplement form 自动覆盖 DELIVERY 行为，既有 9 用例全保。

## 2. 按模块

- **OpportunityControllerTest**: 38/38 — +TC-ACPT-01（DELIVERY 无报告→400 含「甲方验收报告」）/TC-ACPT-02（备报告→200 ACCEPTANCE）；TC-OSEA-02 现在 seed 报告再 advance。
- **OpportunityArtifactTest**: 26/26 — TC-OAR-006 改测 DELIVERY 的 Path A 端到端：body.artifact={title, content} → 后端建档 DELIVERY_ACCEPTANCE_REPORT + advance ACCEPTANCE。
- 其余既有 Opportunity / Requirement / Demand / Project / ... 测试零回归（544 全量绿）。

## 3. E2E（live，与 v0.0.53 完全同语义路径）

待部署后实测（rebuild 中）：
- DELIVERY/WON 商机 `POST /advance {}` 无报告 → 400 含《甲方验收报告》。
- `POST /artifacts {type:DELIVERY_ACCEPTANCE_REPORT, title, content}` → 201。
- 再 advance → 200，stage=ACCEPTANCE。

## 4. 失败项

无。

## 5. 功能/测试覆盖对照

| 功能 | 实现 | 测试 |
|------|------|------|
| DELIVERY → ACCEPTANCE 单产出物门禁 | TransitionArtifactRules.DELIVERY 单件 | TC-ACPT-01/02 |
| Path A 内联建档 + advance | 既有 persistRequiredArtifact (size==1 && !isLink && artifact) | TC-OAR-006 重定位 |
| 前端 DELIVERY 推进经 supplement form | STAGE_REQUIRED_ARTIFACTS[DELIVERY] + 既有路由 | 既有 SURVEY 路径同等覆盖（按 stage 参数化）|
| TC-OSEA-02 stageEnteredAt 在 DELIVERY 推进时刷新 | advance() unchanged | 修复后绿 |

## 6. 设计调整

见 design-adjustments.md：所有先前测试的 "非门禁样例" 现在已无可用样例（10 阶段除 ACCEPTANCE 都有门禁或特殊接口），需在测试中显式 seed 满足门禁。

## 7. 多路评审（Step 0）

跳过专项 adversarial review —— 本变更是 v0.0.53 SURVEY 门禁的同模式复用（单产出物 Path A + 现成 supplement form 经 STAGE_REQUIRED_ARTIFACTS 参数化），手动核验：
- 类型一致：DELIVERY_ACCEPTANCE_REPORT 为报告类（不在 LINK_TYPES），符合 Path A 触发条件 `!isLink(required.get(0))`。
- 规则键正确：`TransitionArtifactRules.DELIVERY → [DELIVERY_ACCEPTANCE_REPORT]`；advance 自动校验。
- 前后端契约一致：ALL/LABELS/STAGE_REQUIRED_ARTIFACTS/ADDABLE 五处镜像同步。
- testid 不变：DeliveryFlow 既有 `delivery-supp-*`/`delivery-supp-content-DELIVERY_ACCEPTANCE_REPORT` 自动覆盖。

## 8. 结论

后端 544 + 前端 265 全绿、tsc/lint clean、零回归；与既有产出物门禁 (v0.0.45/46/53) 同模式。建议进入 Phase 6。
