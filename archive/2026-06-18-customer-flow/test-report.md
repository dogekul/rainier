# v0.0.44-customer-flow — 测试报告 (Phase 5 VERIFY)

> Baseline: tag `v0.0.43-ai-work-log` / commit 0666a61。来源：用户「客户全流程管理」图。
> **理顺 (2026-06-22)**：Opportunity 由 5 售前节点扩为 faithful 的 **10 节点**（售前 5 + 实施 5），关口 3→**4**（加立项评审）。
> **看板/流转拆分 (2026-06-23)**：按用户「看板只做看板，流转单独做操作页，售前/实施区分开」要求，前端重构为 3 页
> （只读看板 + 售前流转 + 实施流转）。后端 advance/initiate/list 端点零改。详见 §8。本报告 §1/§7 反映最终（拆分后）形态。

## 1. 总体概况（最终）

| 维度 | 结果 |
|------|------|
| 后端单元/集成 | **488 / 488** ✅（470 baseline + 18 new；0 fail/error/skip；拆分不动后端） |
| 前端组件/路由 | **203 / 203** ✅（180 baseline + 23 new；50 files）+ tsc clean + eslint 0 warn |
| 新增后端测试 | OpportunityControllerTest **14/14** + OperationControllerTest **4/4** |
| 新增前端测试 | 看板只读 2 + 售前流转 5 + 实施流转 4 + AppRoutes /crm 4 + AppLayout 客户组 1 + OperationBoard 2 + navGuard 自动 +2 |
| E2E（Docker 真 MySQL） | 10 节点全链 + 关口分支 + 边界 + **3 页浏览器实测 + 立项移交 initiate** 全绿 ✅ |
| 多路评审 (Step 0) | 理顺轮 3 reviewers C:0 H:0；**拆分轮 4 reviewers + 对抗式 verify：16→confirmed 13（1H code/1H docs/1H ux + 10M），已处置（见 §8）** |
| 表数 | 21 → **23**（real MySQL 实测 23；rainier_opportunity + rainier_operation 均在） |

## 2. 模型（用户图 → 实现，理顺后）

| 环节 | 实体 | 节点 | 关口 |
|---|---|---|---|
| 售前 | **Opportunity**(NEW) | LEAD 线索→OPPORTUNITY 商机→POC 推介/POC→BIDDING 投标→CONTRACT 合同签订 | 商机 / 投标 / 合同（PASS/REJECT；REJECT→丢单） |
| 实施 | **Opportunity**(NEW，节点) + 链 **Project** | INITIATION 立项→SURVEY 现场调研→REQUIREMENT 产品诉求→DELIVERY 交付实施→ACCEPTANCE 验收 | 立项评审（advance 推进 / initiate 链 Project） |
| 售后 | **Operation**(NEW) | MAINTENANCE 回款维保→OPERATING 运营→REPURCHASE 复购 | — |

- 13-node 图 = Opportunity 10（售前 5 + 实施 5）+ Operation 3（售后）。
- 赢单=合同签订（CONTRACT PASS → WON 且 stage=INITIATION，入实施）；丢单=任一**售前**关口 REJECT → LOST。
- **立项评审 REJECT** 停在「立项」可重审（status WON 不变，≠丢单）；**验收**与 **LOST** 为终态（推进→409）。
- WON 商机仍可在实施环节继续推进（赢单 ≠ 流程结束）。四负责人字段 + 金额。

## 3. 新增测试

**opportunity（OpportunityControllerTest，14）**：创建 201/LEAD/OPEN；负责人不存在 400；非关口推进；关口缺决策 400；
关口 PASS 推进；售前关口 REJECT→LOST；**CONTRACT PASS→WON 且 stage=INITIATION**；LOST 推进 409；立项移交 WON+PASS
链 projectId；立项移交未 WON 409；列表过滤；**实施推进（立项 PASS：INITIATION→SURVEY，status 仍 WON）**；
**验收终态 409**；**立项 REJECT 停在 INITIATION（status WON 不变）**。
**operation（4）**：创建；线性推进；末段→CLOSED；已关闭推进 409。

**前端（10）**：商机看板**两段泳道**（售前/实施）分列 + size:100 + 关口通过→advance(id,'PASS')+refetch+卡移列 + 新建抽屉 +
空字段 formError；运营看板分列 + 推进 + 新建；/crm/* 路由挂载；navGuardConsistency 自动钉 `isAdminPath('/crm/*')===false`。

## 4. E2E（live stack — Docker，真 MySQL，真 JDK-8 镜像）

| # | 验证 | 结果 |
|---|------|------|
| 1 | 全链：建单(LEAD)→OPPORTUNITY→[商机 PASS]→POC→BIDDING→[投标 PASS]→CONTRACT→[合同 PASS] | **WON + stage=INITIATION**（赢单入实施）✅ |
| 2 | 实施链：[立项 PASS]→SURVEY→REQUIREMENT→DELIVERY→ACCEPTANCE（status 全程 WON） | 逐节点推进 ✅ |
| 3 | 终态：ACCEPTANCE 推进→409；LOST 推进→409；关口缺决策→400 | 409 / 409 / 400 ✅ |
| 4 | 关口分支：售前 BIDDING REJECT→**LOST**；立项 INITIATION REJECT→**停 INITIATION/WON** | 丢单 vs 停留语义正确 ✅ |
| 5 | 表数（real MySQL）= **23**；rainier_opportunity + rainier_operation 均建 | ✅ |
| 6 | **浏览器实测**（Docker 前端 :80，alice 登录，SPA 进 /crm/opportunities） | 见 §4.1 ✅ |
| 7 | 存量业务数据：opportunities/operations 基线 0；测试数据全部 DELETE 清零 | 零改 ✅ |

### 4.1 浏览器实测（图保真度）

Chrome 实测 `/crm/opportunities`（真 Docker 前端 + 真 MySQL）：
- **两段泳道**渲染：`售前环节 · 负责人：商务 / 解决方案` 与 `实施环节 · 负责人：项目经理`。
- **10 列**按序：线索/商机⭐/推介POC/投标⭐/合同签订⭐（售前）｜立项⭐/现场调研/产品诉求/交付实施/验收（实施）；4 关口标 ⭐。
- 卡片落位正确：「演示售前客户 ¥120000 推进」在售前·线索；「演示实施客户 ¥500000 WON 通过/否决」在实施·立项。
- 磁贴：1 进行中 / 1 赢单 / 0 丢单 / 120000 在谈金额。截图已留存。

## 5. 多路评审（Step 0）+ 11 类失败模式

**3 reviewers**（code-quality / test-config / docs-spec）+ 对抗式 verify：**C:0 H:0**，confirmed-real **0**。

- 处置：**M-1（dead code）** `OpportunityStage.PRESALES_STAGES` 声明未用 → **已删**（rebuild 过 JDK-8 gate）。
- **H 误报澄清**：
  - 「initiate 不推进 stage」→ 设计本意：`advance` 走立项关口推进（INITIATION→SURVEY），`initiate` 单独链 Project，两者并存（design D2）。非 bug。
  - 「缺 initiate UI」→ 立项关口在看板可用（通过/否决调 advance）；链 Project 的 initiate 端点 UI 为后续片（§6），非本版范围。
- **状态机正确**（E2E 实证）：CONTRACT PASS 同时置 WON + stage=INITIATION；售前 REJECT→LOST、立项 REJECT 停留；ACCEPTANCE/LOST 终态 409；`idx>=size-1` 守卫无越界。
- **Java-8 clean**（temurin-8 Docker 镜像构建通过 — 真 Java-8 gate）；四负责人 existsById 校验；amount(Long) 端到端一致；
  前端 OPP_GATE_STAGES 与后端 GATE_STAGES 同源；OPP_STAGE_LABELS/OPP_PHASES 覆盖全 10 节点。
- **docs 对齐**：proposal/design/specs/test-plan 的旧 5 节点/3 关口表述均已更新为 10 节点/4 关口（理顺注记 + 逐条对齐 OpportunityStage.java）。

**11 类失败模式**：无幻觉；范围聚焦（opportunity + operation + frontend-scaffold）；(d) design D1-D6 与代码吻合；
(f) 运行时行为以浏览器实测覆盖；(j) 无覆盖真空（全状态机分支 + 全关口分支均有自动化测试）；(k) 前后端 DTO/字段对齐。

## 6. 已知取舍 / 后续片（不阻塞）

- **立项移交 UI 已补齐**（拆分轮）：「实施流转」页的「立项移交」抽屉调 `POST /initiate` 链交付 Project（原 §6 遗留项已 close）。
- **实施=Opportunity 节点 + 链 Project**（用户选 B）：实施环节既是 Opportunity 的 5 个推进节点，真正的交付工作仍在 Project/Sprint/Story/Task 跑；本版未给 Project 加售前/售后反向字段。
- **监控角色待定**：商机看板「给某角色查看」的角色 TBD，暂 all-users。LOST 商机看板仅进磁贴不进泳道列（监控可见性取舍，可后续加开关）。
- **后续片**：① 看板监控角色收口 + LOST 显隐开关 ② 各节点活动清单 + 输出物（复用关联面板）③ 度量（成单率/中标率、交付周期、逾期督办）④ Opportunity↔Project↔Operation 全链自动衔接/可视化。

## 7. 结论

| 信号 | 状态 |
|------|------|
| 后端 488/488 + 前端 203/203 + tsc/lint | ✅ |
| 新增 18 后端 + 23 前端测试全绿 | ✅ |
| E2E 10 节点全链 + 4 关口分支 + 全边界（真 MySQL） | ✅ |
| Docker 真 JDK-8 构建 + 表数 23（real DB 实测） | ✅ |
| 浏览器实测：只读看板（零控件）+ 售前流转 + 实施流转 + 立项移交 initiate 落库 | ✅ |
| 多路评审 理顺轮 C:0 H:0；拆分轮 confirmed 13 全处置（H×3 已修/已澄清，M×10 已修/记录） | ✅ |
| 存量业务数据零改（纯新增 2 表；测试数据已清零；用户 中信 数据未动） | ✅ |

## 8. 看板/流转拆分（2026-06-23 修订）

### 8.1 改动

按用户「商机看板只做看板（给某监控角色查看所有商机进展）；流转单独做操作页；售前/实施区分开；新建也移走」，**前端重构为 3 页，后端零改**：

- **商机看板** `/crm/opportunities` → **只读**：两段泳道 + 10 列 + 只读卡片（赢单 chip）+ StatTiles。`opp-readonly-hint` 提示去流转页操作。**零操作控件**（单测 `queryAllByRole('button')===0`）。
- **售前流转** `/crm/presale-flow`（NEW）：列 `OPEN∧售前` 操作表；新建商机抽屉（客户/标题/金额/四负责人）+ 推进 + 关口 通过/**否决（确认弹窗→丢单）**。
- **实施流转** `/crm/delivery-flow`（NEW）：列 `WON∧实施` 操作表；**立项移交**抽屉（选 Project → `initiate(id,projectId,'PASS')`）+ 立项 通过/否决 + 推进 + 验收终态「已验收」。
- `api/opportunity.ts` 加 `OPP_PRESALE_STAGES`/`OPP_DELIVERY_STAGES`；客户导航组 4 项（商机看板/售前流转/实施流转/运营看板）。

### 8.2 多路评审（Step 0，拆分轮）

4 reviewers（code/tests/docs/ux）+ 对抗式 verify：16 raw → **confirmed 13**（1H code、1H docs、1H ux、10M）。处置：

| 级别 | 发现 | 处置 |
|---|---|---|
| **H** code | 立项移交按钮仅 `busyId` 禁用，handoff 期间 `busyId=null` 可重复点（double-submit） | **已修**：`disabled={busyId===r.id \|\| handoffId===r.id}` |
| **H** ux | 否决（→丢单，不可恢复）无确认即提交 | **已修**：售前 否决加 `ConfirmDialog`（确认后才 REJECT）；实施立项否决为「停留可重审」非破坏性，不加确认 |
| **H** docs | proposal/test-report 残留「看板带操作/建单」旧表述 | **已修**：proposal L51/L76 + 本报告 §1/§7 改写为拆分后形态 |
| **M** tests×5 | 售前/实施过滤未独立验证 stage（仅验 status）；新建未验 owner 字段；看板只读断言脆弱；/crm isAdminPath 无显式断言；客户导航组无测试 | **已修**：加 OPEN×实施 / WON×售前 排除断言；TC-PRE-03 填 owner 并断言；加注释；TC-FES-CRM-04 显式 `isAdminPath===false`；AppLayout 客户组测试 |
| **M** ux×2 | 立项移交无可用项目时无反馈；看板 LOST 不进泳道 | 已修（空项目 `delivery-no-projects` 提示 + TC-DEL-04）；LOST 取舍记入 §6 后续片 |
| **M** docs/ux | AppLayout 客户组缺测试 | **已修**（TC-FES-CRM-NAV-001） |

修复后 frontend **203/203** + tsc/lint clean；浏览器实测 3 页 + 立项移交落库（projectId=8）。**confirmed-real 残留 = 0**（除记入 §6 的设计取舍）。

### 8.3 浏览器实测（真 Docker :80）

- **商机看板**：只读 hint + 4 导航项 + 赢单 chip×3 + 磁贴（2 进行中/3 赢单/2 丢单[含用户 中信]/¥230000）；页内操作按钮 **0**。
- **售前流转**：2 售前在办（商机⭐→通过/否决，线索→推进）+ 新建按钮；WON/LOST 已排除。
- **实施流转**：验收→已验收（无操作）/ 现场调研→推进 / 立项⭐→立项移交·通过·否决；**立项移交** 选「GYSMH 供应商门户」→ 商机 `projectId=8`、行显示 `#8`。

**部署建议**：可交付（待用户审阅后 push）。
