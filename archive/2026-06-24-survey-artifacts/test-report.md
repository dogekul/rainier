# Test Report — v0.0.53 现场调研产出物门禁

## 1. 总体概况

| 层 | 总数 | 通过 | 失败 | 跳过 | 通过率 |
|----|------|------|------|------|--------|
| 后端 (temurin-8 全量) | 532 | 532 | 0 | 0 | 100% |
| 前端 (Vitest) | 250 | 250 | 0 | 0 | 100% |
| tsc | — | clean | — | — | — |
| eslint | — | clean | — | — | — |
| E2E (live MySQL+HTTP) | 1 链路 | 通过 | 0 | — | 100% |

后端 528→532（+4 TC-SUR-01..04）；前端 248→250（TC-DEL-02 重写 + TC-DEL-08/09 新增，净 +2）。

## 2. 按模块

- **OpportunityControllerTest**: 34/34（+TC-SUR-01..04；TC-OSEA-02 改 SURVEY→REQUIREMENT）。
- **OpportunityArtifactTest**: 26/26（TC-OAR-006 改 SURVEY→REQUIREMENT 作非门禁样例）。
- **DeliveryFlow.test.tsx**: 9/9（TC-DEL-02 现验证开补充表单；TC-DEL-08 提交报告+附件后推进；TC-DEL-09 产品诉求直接推进）。

## 3. E2E 结果（关键路径 TC-E2E-SUR-01）

throwaway opp（id 42，DB 提升至 SURVEY/WON）：
1. `POST /advance {}` 无产出物 → **400** `此转换需先提交产出物：《现场调研报告》、《现场调研附件》` ✓
2. `POST /artifacts {SURVEY_REPORT, content}` → **201** ✓
3. `POST /artifacts {SURVEY_ATTACHMENT, link}` → **201** ✓
4. `POST /advance {}` → **200**，stage=**REQUIREMENT** ✓
5. 清理：删除 throwaway opp 42 + 其产出物（opp42=0, art42=0）；真实数据未动。

确认 `rainier_opportunity_artifact.type` 列为 VARCHAR(32)，`SURVEY_ATTACHMENT`(17)/`SURVEY_REPORT`(13) 均容纳（无 v0.0.49 式列宽溢出风险）。

## 4. 失败项

无。

## 5. 功能/测试覆盖对照

| 功能 | 实现 | 测试 |
|------|------|------|
| SURVEY 门禁缺件 400 | TransitionArtifactRules.SURVEY + Path B | TC-SUR-01/03, TC-E2E-SUR-01 |
| 备齐推进 REQUIREMENT | advance（零改动，多类型 Path B） | TC-SUR-02, TC-E2E-SUR-01 |
| 两新类型可提交 | ArtifactType.ALL + POST /artifacts | TC-SUR-04, TC-E2E-SUR-01 |
| 前端补充表单 | DeliveryFlow requestAdvance/submitSupplement | TC-DEL-02/08 |
| 非门禁直接推进 | requestAdvance 分流 | TC-DEL-09 |

## 6. 设计调整

见 design-adjustments.md：ADJ-1（M1 在途守卫）、ADJ-2（M2 advance 失败可见）。

## 7. 多路评审（Step 0，单代理对抗审查）

C:0 H:0 M:2 L:4。M1/M2 已修复（见 §6）；L1-L4 评审确认可接受（L3/L4 为测试质量正向确认）。前后端契约一致性（LINK_TYPES/LABELS/ALL/STAGE_REQUIRED）逐项核对 match；Java 8 无 List.of/Set.of/var；advance/persist 逻辑确认未改。

## 8. 结论

后端 532 + 前端 250 全绿，tsc/lint clean，E2E 现场调研门禁链路 live 验证通过，存量数据未改。质量信号全绿，建议进入 Phase 6 交付（待 Gate 3 用户确认）。
