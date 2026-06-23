# v0.0.45-gate-artifacts — 测试报告 (Phase 5 VERIFY)

> Baseline: tag `v0.0.44-customer-flow` / commit e88c8ab。
> 范围：流转产出物门禁（《商机调研报告》线索→商机 / 《决策评审纪要》商机决策）+ Word 导出 + **fold-in 商机产品标签** + **fold-in 客户实体**。

## 1. 总体概况

| 维度 | 结果 |
|------|------|
| 后端单元/集成 | **500 / 500** ✅（temurin-8 全量实测；0 fail/error/skip，BUILD SUCCESS） |
| 前端组件/路由 | **223 / 223** ✅（52 files；+MarkdownView 4 + POC 2）+ tsc clean + eslint 0 warn |
| 新增后端测试 | OpportunityArtifactTest **11** + OpportunityControllerTest **18**（+2 product +2 customer）+ CustomerControllerTest **6** |
| E2E（Docker 真 MySQL + temurin-8 含 POI） | 门禁/导出 .docx + 产品 + **客户 CRUD/选已有/自动新建/dedup** 全绿 ✅ |
| 多代理评审 (Step 0) | 4 reviewers + 对抗式 verify：17 raw → **confirmed 13（C:0 H:0，M:7 L:6）** 全处置（产出物轮；客户为后续 fold-in） |
| 表数 | 23 → **25**（+rainier_opportunity_artifact +rainier_customer；real MySQL 实测 25）+ rainier_opportunity.product_id / customer_id 列 |

## 2. 模型

- **产出物门禁**（`TransitionArtifactRules`，按来源阶段配置，可扩展）：
  - 线索(LEAD) advance → 需《商机调研报告》(RESEARCH_REPORT)
  - 商机(OPPORTUNITY) 决策 → 需《决策评审纪要》(DECISION_MINUTES，通过+否决都留痕，记 decision)
  - `advance` 同事务：缺产出物 → 400；满足 → 建 OpportunityArtifact + 流转。非门禁转换不受影响。
- **产出物**：append-only（仅经 advance 创建，无改/删 API）；列查 `GET /{id}/artifacts`；导出 `GET /{id}/artifacts/{aid}/export` → .docx（Apache POI 4.1.2，Java-8 安全）。
- **商机产品标签**：Opportunity 加可空 `productId`（FK→既有 Product），create/update 接收 + 校验存在，Detail enrich productName。

## 3. E2E（live stack — Docker，真 MySQL，真 JDK-8 + POI）

| # | 验证 | 结果 |
|---|------|------|
| 1 | 创建商机带 productId=1 | 201；productId=1、productName=支付平台 ✅ |
| 2 | 创建带不存在产品 999999 | 400 ✅ |
| 3 | 线索 advance 缺《商机调研报告》 | 400（门禁）✅ |
| 4 | 线索 advance 带报告 | 200 → OPPORTUNITY ✅ |
| 5 | 商机决策缺《决策评审纪要》 | 400 ✅ |
| 6 | 商机 PASS 带纪要 | 200 → POC ✅ |
| 7 | 列查产出物 | 2 条，最新在前（DECISION_MINUTES/PASS + RESEARCH_REPORT）✅ |
| 8 | 导出 .docx | content-type wordprocessingml；PK 头；2514 bytes；valid zip ✅ |
| 9 | real MySQL schema | 24 表 + rainier_opportunity_artifact + product_id 列 ✅ |

### 3.1 浏览器实测（真 Docker :80）

- **新建商机 产品下拉**（用户报「无法选择关联产品」）：根因=旧 v0.0.44 前端未重建（无该字段）。重建后下拉显示「（未选择）」+ 4 个产品（演示·支付平台/另一产品/E2E产品/支付平台），**可选**；选「演示·支付平台」保存 → 商机带 productId=6/productName 落库 ✅。
- 产出物表单（报告/纪要）+ 商机看板产出物抽屉 + 导出 Word 入口均已部署（bundle 含 presale-new-product / presale-artifact-save / opp-artifacts / opp-export）。

## 4. 多代理评审（Step 0）+ 处置

4 reviewers（code/tests/docs/security）+ 对抗式 verify：**17 raw → confirmed 13，C:0 H:0（M:7 L:6）**。代码逻辑（门禁/REJECT 重构/原子性/导出/IDOR）经核**正确**；findings 为测试强化 + 健壮性。处置：

| 级别 | 发现 | 处置 |
|---|---|---|
| M/sec | 产出物 content 无上限（导出内存 DoS 角度） | **已修**：ArtifactInput.content @Size(max=50000) |
| M/code | 缺 gating-order 测试（decision 缺 vs artifact 缺） | **已修**：TC-OAR-010 |
| L/sec | 缺 IDOR 测试（跨商机导出） | **已修**：TC-OAR-011（跨商机导出→404；代码本已防护） |
| M/tests | TC-OAR-001/003 缺「400 时阶段不变」断言 | **已修** |
| M/tests | TC-OPP-003/005 缺产出物持久化断言 | **已修**（注入 artifactRepo 断言 type/decision） |
| M/docs | 导出 filename 与 design 不符 | **已修**：design D4 改为 ASCII 安全 `artifact-<oppId>-<aid>.docx` |
| L | DocxWriter 防御性 null / POI 4.1.2 EOL / CONTRACT 非门禁 | 接受（无害 / Java-8 约束 / 设计本意）|

**11 类失败模式**：Java-8 clean（temurin-8 构建通过含 POI）；(d) design↔code 一致；(f) 运行时以浏览器+E2E 覆盖；(j) 无覆盖真空（门禁全分支 + 产品 + 导出 + IDOR）；(k) 前后端 artifact/product DTO 对齐。

## 5. 已知取舍 / 后续片

- 产出物 append-only（要改走「补充」后续片）；POI 4.1.2（升 5.x 待 Java 11+）；产出物权限收口（暂 all-users）。
- 产出物门禁目前 2 条规则；投标/合同/立项加产出物只需在 TransitionArtifactRules + OPP_TRANSITION_ARTIFACT 各加一条。

## 6. 结论

| 信号 | 状态 |
|------|------|
| 后端 500/500 + 前端 223/223 + tsc/lint | ✅ |
| E2E 门禁→产出物→导出 .docx + 产品 + 客户(CRUD/选已有/自动新建/dedup)（真 MySQL+POI） | ✅ |
| 浏览器实测 产品下拉 + 客户管理页 + 商机客户组合框(选/建) | ✅ |
| 多代理评审 C:0 H:0；13 confirmed 全处置 | ✅ |
| 表数 25 + product_id/customer_id 列（real DB 实测） | ✅ |
| 存量业务数据零改（中信/中信银行/招商 未动；测试数据已清零） | ✅ |

**部署建议**：可交付（待用户 Gate 3 确认后 commit/tag；push 待用户指示）。

## 7. 客户实体（fold-in，2026-06-23）

NEW `customer` capability：`Customer`（rainier_customer，soft-delete，name 必填 + industry/contactName/notes）+ CRUD
`/api/customers`（all-users）+ 客户管理页 `/crm/customers`（客户导航组首项）。Opportunity 加可空 `customerId`（保留 customerName 显示）。

| 维度 | 结果 |
|------|------|
| CustomerControllerTest | 6/6（create/blank-400/search/get/update/soft-delete→404）|
| OpportunityControllerTest +客户 | TC-OPP-017 选已有客户→链+采用其名；TC-OPP-018 新名→自动建客户+链 |
| E2E（真 MySQL） | 客户 CRUD；商机 customerId 选已有（名采用实体）；新名自动建；同名 dedup（O2/O3 共用 customerId=2）✅ |
| 浏览器实测 | 客户导航组 5 项；客户管理页建客户入列；售前流转 新建商机 客户字段=datalist 组合框（选已有/输入新名）✅ |
| 表数 | 24→25（rainier_customer）+ rainier_opportunity.customer_id 列（real DB 实测）✅ |

取舍：客户软删后已关联商机仍保留 customerName 显示（不破坏）；存量商机 customerId 留空；同名去重用 findFirstByNameIgnoreCase。

## 8. 商机备注 + 新建表单顺序（fold-in，2026-06-23）

Opportunity 加可空 `note`（备注，length 2000）+ create/update/Detail。新建商机抽屉字段顺序：**客户 → 商机标题 →
备注(textarea) → 产品 → 四负责人 → 金额(末位)**。后端 TC-OPP-001 断言 note 持久化；前端 TC-PRE-03 填 note 并断言传入；
浏览器实测抽屉字段 DOM 顺序与要求一致。无新表/新依赖。

## 9. 售前流转 商机详情：可查看 / 可编辑 / 可推进（fold-in，2026-06-23）

售前流转每行「详情」→ 抽屉：
- **可编辑**表单（客户组合框/标题/备注/产品/四负责人/金额）→「保存修改」调 `PUT /api/opportunities/{id}`（updateOpportunity）；阶段/状态/最近决策人只读。
- **可推进**：抽屉内「推进」或「通过/否决」复用 advance 门禁（关口/产出物逻辑一致），推进时关抽屉再路由。
- **产出物历史** + 每条「导出 Word」（复用 `GET /{id}/artifacts`）。

测试：后端 TC-OPP-019（update 改 note + 链客户）；前端 TC-PDE-01（默认只读 + 编辑按钮 + 产出物富文本预览 + 导出）/
TC-PDE-02（点编辑→表单→保存调 update）/ TC-PDE-03（详情内通过 → 关抽屉 → 纪要表单 → advance）。后端 **510/510**、前端 **220/220**、tsc/lint clean。
浏览器实测（真后端）：编辑保存持久化；推进在位 ✅。

### 9.1 详情默认只读 + 编辑切换 + 富文本预览（2026-06-23 续）

- **默认只读**：详情打开为只读视图（字段以文本展示）+ 右上「编辑」按钮 → 切换为表单（保存修改 / 取消）。保存或取消回只读。
- **富文本预览**：NEW `MarkdownView`（自研、零依赖、XSS 安全的 md→React 渲染器，支持 标题/粗斜体/列表/段落）。详情 产出物每条「预览/收起」渲染报告/纪要为富文本；商机看板产出物内容也改为富文本。MarkdownView 单测 4/4（含 XSS-safe）。
- 浏览器实测（用户真实商机，只读未改）：详情默认只读 + 编辑按钮；产出物「预览」渲染富文本；点编辑显表单 → 取消回只读 ✅。

## 10. POC 多产出物门禁（fold-in，2026-06-23）

推介/POC → 投标 需备齐 **讲解材料(链接,≥1) + 甲方诉求清单(链接) + POC得分表(报告) + 差距分析报告(报告)** 才能推进。

- 机制：`TransitionArtifactRules` 改为 `Map<stage, List<type>>`（POC→4 类）；advance 门禁双路径——单产出物阶段(线索/商机)仍内联提交即建，多产出物阶段(POC)校验已存在的产出物齐全，缺则 400 列出缺项。
- 产出物 kind：链接类(讲解材料/甲方诉求清单, `link` 列) vs 报告类(得分表/差距分析, `content` 富文本)；后端按 kind 校验（链接类需 link、报告类需 content）。
- NEW `POST /api/opportunities/{id}/artifacts` 独立提交（append-only）；前端「详情→添加产出物」表单（类型下拉 → 链接/正文按 kind 切换）；advance 缺产出物时前端 `presale-adv-error` 提示；链接类「打开链接」可点击，报告类「预览/导出 Word」。
- 多代理评审：1 focused reviewer + 对抗式核对 → H1/H2 经核为**误报**（POC 本就设计为非决策、仅产出物门禁；前端经 advError 优雅失败已测）；**M1**（链接/正文按 kind 校验）/**M3**（错误兜底 e.message）/**M2**（表单复位）**已修**。

| 维度 | 结果 |
|------|------|
| 后端 OpportunityArtifactTest | 14/14（+TC-OAR-012 缺产出物 400 / 013 备齐 4 类→BIDDING / 014 POST 校验：未知类型 400、链接类缺链接 400、链接类正常建） |
| 前端 PresaleFlow | TC-POC-01（POC 推进受阻显示后端缺项消息）/ TC-PAR-04（添加链接类产出物提交 link） |
| E2E（真 MySQL） | POC→投标 空→400(列 4 缺项)；POST 4 类(2 链接+2 报告，链接缺 link→400)；缺 1→400；齐→BIDDING；列查 link/content 按 kind 落位 ✅ |
| 浏览器实测 | 详情「添加产出物」：6 类型；讲解材料→链接输入 / POC得分表→正文域（按 kind 切换）；取消不改数据 ✅ |
| 表数 | 不变 **25**（link 为 rainier_opportunity_artifact 上的列，无新表）|

后端 **500/500** · 前端 **223/223** · tsc/lint clean。

### 10.1 推进时补充必需文档（2026-06-23 续）

POC 行点「推进」→ 若缺必需产出物，弹「补充产出物并推进」表单：逐项列出缺失类型（按 kind 填链接/正文），「提交并推进」
即创建缺失项 + 推进（复用 POST /artifacts + advance，前端编排；前端 only，后端不变）。已有的产出物自动跳过（仅补缺）。
`STAGE_REQUIRED_ARTIFACTS` 前端镜像后端规则。

测试：前端 TC-POC-01（POC 推进弹补充表单，4 缺失项 + 链接/正文按 kind）/ TC-POC-02（填 4 项→createArtifact×4 + advance）。
浏览器实测（真后端，throwaway 商机已删）：POC 推进 → 补充表单 4 段 → 填 2 链接 + 2 报告 → 提交并推进 → stage=BIDDING、6 产出物落库 ✅。
前端 **223/223** · tsc/lint clean。

### 10.2 链接类材料无需标题 + 链接可添加多份（2026-06-23 续）

用户："几个材料不需要标题；材料连接可以添加多份"。

- 后端：`OpportunityArtifactCreateRequest.title` 去 `@NotBlank`（仅 `@Size(200)`，标注 Optional）；`OpportunityArtifactService.create` 标题为空时用 `ArtifactType.label(type)` 兜底（`title @Column nullable=false` 不变，规避 ddl 改列）。链接类只存 link、报告类只存 content（按 kind）。
- 前端：补充表单 + 详情「添加产出物」——链接类去掉标题输入、支持多行链接（`+ 添加链接` / 逐行删除，testid `presale-supp-link-{type}-{idx}` / `presale-supp-addlink-{type}`）；提交时链接类按每条非空 link 各建一条产出物（无 title），报告类标题可空（兜底）。产出物展示头：title 等于类型名时不重复显示（避免「讲解材料 · 讲解材料」）。`OpportunityArtifactCreate.title` 改可空。
- 测试：后端 TC-OAR-015（链接无标题→201，title 兜底「讲解材料」），OpportunityArtifactTest **15/15**；前端 TC-POC-01（链接类 indexed 输入 + 添加链接按钮、无标题输入）/ TC-POC-02（讲解材料填 2 条链接→createArtifact×5、链接类无 title）/ TC-PAR-04（链接类添加无标题）。前端 **223/223** · tsc/lint clean。
- E2E（真 Docker，前后端均已重建；throwaway 商机 id=32 + 客户已处置）：POC 空推进→400 列 4 缺项；POST 2× 讲解材料(仅 link,无 title)→201 title 兜底「讲解材料」、甲方诉求清单(link) + 得分表/差距分析(content)→201；推进→**BIDDING**；列查 7 产出物 kind/title 兜底正确 ✅。
