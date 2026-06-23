# Test Report: v0.0.46 — 投标→合同→立项 产出物门禁

> 范围：投标 BIDDING→合同（《投标文件》）+ 合同 CONTRACT→立项（中标公示/合同/评审会议纪要/邮件归档/已盖章合同）产出物门禁；
> 门禁仅 PASS 强制；附件先 URL 占位（无文件上传/邮件基建）。基线 tag v0.0.45-gate-artifacts / commit b530dd0。

## 1. 总体概况

| 维度 | 结果 |
|---|---|
| 后端单元/集成 | **511 / 511** ✅（temurin-8 全量 509 实测 + 后续 +2 新增 TC 同类回归实测；0 fail/error/skip） |
| 前端组件/路由 | **227 / 227** ✅（52 files；+TC-FCAR-01/02/03/04）+ tsc clean + eslint 0 warn |
| 后端新增 TC | 11（TC-CAR-001..011） |
| 前端新增 TC | 4（TC-FCAR-01..04） |
| 回归更新 | OpportunityControllerTest.advance_contractPass（先 seed 5 件） |
| E2E | green（docker 真 MySQL；BIDDING→CONTRACT→INITIATION 全链） |
| 多代理评审 (Step 0) | 3 维度 + 对抗式 verify：3 raw → **confirmed 2（C:0 H:0 M:0 L:2）+ refuted 1**，2 条 L 已修 |
| 表数 | 25（无新表；附件 URL 占位） |
| 存量数据 | 完好（仅 throwaway opp 已建即删；用户自有商机未动） |

## 2. 模型 / 范围

- 后端：`ArtifactType` +6（BID_DOCUMENT/BID_WINNING_NOTICE/CONTRACT_DRAFT/REVIEW_EMAIL_ARCHIVE/SIGNED_CONTRACT=链接类；
  CONTRACT_REVIEW_MINUTES=报告类）；`TransitionArtifactRules` +BIDDING→[投标文件]、CONTRACT→[5 类] + `requiredOnReject`(仅 OPPORTUNITY)；
  `OpportunityService.persistRequiredArtifact` 门禁仅 PASS 强制（关口 REJECT 且 !requiredOnReject → 跳过）+ Path A `!isLink` 守卫。
- 前端：`api/opportunityArtifact.ts` 注册 6 类型 + `STAGE_REQUIRED_ARTIFACTS`(+BIDDING/CONTRACT)；
  `PresaleFlow.requestAdvance` 对 `decision==='REJECT'` 跳过补充表单。
- 无新表、无新依赖、无 multipart/SMTP 配置。

## 3. E2E（live stack — Docker，真 MySQL，真 JDK-8 + POI；前后端均已重建）

| # | 步骤 | 结果 |
|---|---|---|
| 1 | BIDDING PASS 缺《投标文件》 | 400，stage 不变 ✅ |
| 2 | POST 2× BID_DOCUMENT（仅 link，无 title） | 201 ✅ |
| 3 | BIDDING PASS | 200 → CONTRACT ✅ |
| 4 | CONTRACT PASS 缺件 | 400 ✅ |
| 5 | POST 中标公示/合同/邮件归档/已盖章合同(link) + 评审会议纪要(report) | 201×5 ✅ |
| 6 | CONTRACT PASS | 200 → **INITIATION + status=WON** ✅ |

> REJECT→LOST（不要求产出物）由后端集成测试 TC-CAR-006/007 在真 H2 栈验证（与 live 同代码路径）。
> throwaway 商机（id 34）已建即删；用户自有商机（12/13/21/27/30/33）未动。

## 4. 多代理评审（Step 0）+ 处置

3 维度并行（backend-code / frontend-code / test-spec）→ 每条 finding 对抗式 verify（默认怀疑、实际打开代码核实）。

| 维度 | 严重度 | 发现 | 处置 |
|---|---|---|---|
| test-spec | L | 前端「产出物已齐全→跳过补充表单直接推进」(missing.length===0) 分支无测试覆盖 | **已修** TC-FCAR-04 |
| test-spec | L | CONTRACT 5 件中 中标公示/合同 两类必需性未被独立守护（mutation 存活） | **已修** TC-CAR-010（对称断言） |
| test-spec | （refuted） | 「商机否决仍需纪要」requiredOnReject(OPPORTUNITY) 无守护 | **证伪**：TC-OAR-005 经控制流顺序已守护（REJECT 早退守卫在内联创建之前，mutation 会令 TC-OAR-005 失败）。附带补 TC-CAR-011 收紧 spec→test 映射 |

评审结论：C:0 H:0 M:0 L:2，均 < 阈值；2 条 L 全修，refuted 项亦补强。

## 5. 失败模式检查（a–k）

- (b) 范围蔓延：仅改 5 文件 + 3 测试 + STDD 文档，无越界。
- (d) 上下文一致：实现与 design.md D1-D5 一致（PASS-only / Path A !isLink / URL 占位）。
- (j) 覆盖真空：BIDDING/CONTRACT 的 PASS-缺件 / PASS-齐全 / REJECT-丢单 / 各类必需性 + 前端 PASS 弹表单/REJECT 跳过/齐全直推 全覆盖。
- (k) 契约断层：前端 `STAGE_REQUIRED_ARTIFACTS` 与后端 `TransitionArtifactRules.RULES` 的 BIDDING/CONTRACT 一一对应（评审 frontend-code 维度核对无误）。
- 其余 (a)(c)(e)(f)(g)(h)(i) 无命中。

## 6. 已知取舍 / 后续片

- 附件先 URL 占位（链接类）；真实文件上传 + 对象存储迁移、系统 SMTP 发信、结构化合同评审实体 均推迟（Gate-1 用户决策）。
- 评审会议纪要取报告类（富文本，可系统内编辑 + Word 导出），与《决策评审纪要》一致。

## 7. 结论

✅ 投标/合同产出物门禁按规格落地；门禁仅 PASS 强制（投标/合同 REJECT 丢单不阻），商机决策纪要 PASS/REJECT 都留痕（不回归）。
后端 511/511（temurin-8）+ 前端 227/227 + tsc/lint clean；E2E 全链绿；评审 C:0 H:0 M:0 L:2 全处置。建议进入 Gate 3。
