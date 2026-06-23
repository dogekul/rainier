# v0.0.45-gate-artifacts — 流转产出物门禁（《商机调研报告》《决策评审纪要》+ Word 导出）

> Baseline: tag `v0.0.44-customer-flow` / commit e88c8ab。延续客户全流程：给关键流转挂「必交产出物」。

## Why

客户全流程的流转目前是「裸点」——推进/决策没有任何业务留痕。业务要求关键转换必须产出正式文档：
- **线索 → 商机**：必须提交《商机调研报告》（证明这条线索值得立项跟进）。
- **商机决策**（关口）：必须形成《决策评审纪要》，然后才流转进入下一阶段。

这些产出物是「门禁」——没有就不能流转——且需可检索、留痕、**导出 Word** 供线下传阅/归档。与审计日志（系统动作流水）
互补：这是**业务产出物**留痕。本版做成「转换 → 所需产出物类型」可配置的通用机制，先落地这 2 条，后续给投标/合同/立项加
产出物只需加一条配置。

## 已锁定（Gate 0 用户确认）

- **产出物形态**：结构化文本记录（标题 + 正文），**并支持导出为 Word(.docx)**。不做文件上传。
- **提交方式**：合并——操作（推进/决策）时弹表单填产出物，提交即「创建产出物 + 流转」原子完成；缺产出物则后端挡住（400）。
- **适用范围**：先做这 2 条（线索→商机《商机调研报告》；商机决策《决策评审纪要》），机制做成可配置扩展。
- **默认（可在 Gate 1 改）**：《决策评审纪要》在**通过与否决都要求**（决策即留痕）；产出物创建后**不可改不可删**（业务记录）；
  产出物的**查看/导出入口放在「商机看板」**（只读总览页 per-商机 抽屉 + 导出 Word 按钮）。

## What Changes

**后端（NEW `opportunity-artifact` + MOD `opportunity`，+1 表）**

`OpportunityArtifact`（产出物，表 `rainier_opportunity_artifact`，extends BaseEntity）：
- 字段：`opportunityId` / `type`（`RESEARCH_REPORT` 商机调研报告 / `DECISION_MINUTES` 决策评审纪要，可扩展）/
  `stageFrom`（产生该产出物的来源阶段，如 LEAD / OPPORTUNITY）/ `title` / `content`（长文本）/ `decision`（可空，纪要记 PASS/REJECT）；
  作者 = BaseEntity.createBy，时间 = createTime。append-only（无修改/删除 API）。
- `ArtifactType` 常量 + 「转换 → 所需产出物类型」配置（`TransitionArtifactRules`）：
  - `LEAD →(advance)` ⇒ 需 `RESEARCH_REPORT`
  - `OPPORTUNITY →(decision)` ⇒ 需 `DECISION_MINUTES`
- 读 API：`GET /api/opportunities/{id}/artifacts`（列某商机全部产出物，倒序）。

`opportunity` 能力修改：
- `OpportunityAdvanceRequest` 增加可选 `artifact { title, content }`。`advance()` 门禁：若该转换在 `TransitionArtifactRules`
  中要求产出物 → `artifact` 必填且 title/content 非空（缺 → 400「此转换需提交《X》」）；满足则**同事务**创建 `OpportunityArtifact`
  （type 由转换推导，decision 记入纪要）+ 推进。不要求产出物的转换忽略 `artifact`（向后兼容）。
- **Word 导出**：`GET /api/opportunities/{id}/artifacts/{artifactId}/export`（Apache POI 生成 .docx）——
  内容含 标题 / 类型 / 客户·商机 / 阶段 / 决策 / 作者 / 时间 / 正文；响应 `Content-Type: ...wordprocessingml.document`
  + `Content-Disposition: attachment; filename=...docx`。新增 `poi-ooxml` 依赖（选 Java-8 兼容版本，过 temurin-8 gate）。

**前端（frontend-scaffold MOD）**

- 「售前流转」：线索行「推进」→ 弹《商机调研报告》表单（标题+正文）→ 提交即推进；商机行「通过/否决」→ 弹《决策评审纪要》
  表单（标题+正文，记录决策）→ 提交即流转。缺填 → 表单校验 + 后端 400 兜底。
- 「商机看板」（只读）：per-商机「产出物」抽屉——列出该商机的产出物，每条「导出 Word」按钮（触发 .docx 下载）。
- `api/opportunityArtifact.ts`（列查 + 导出 URL）；advance 调用带上 artifact。

## Fold-in (2026-06-23)：商机产品标签

用户追加：给商机加一个**产品标签**（使用既有产品枚举），创建时关联产品，不确定可留空。实现：Opportunity 加可空
`productId`（FK→Product），create/update 接收 + 校验存在；Detail enrich `productName`；售前流转新建抽屉加产品下拉（可空）；
只读看板卡片显示 `🏷 产品名`。无新表。见 design D7。

## Fold-in (2026-06-23)：客户实体

用户追加：客户做成实体，创建商机时可选，没有也允许填写新建。实现：NEW `customer` capability（Customer 实体 +
CRUD + 客户管理页 + 导航）；Opportunity 保留 customerName + 加可空 customerId FK；售前流转新建抽屉客户字段=datalist 组合框
（选已有或输入新名→自动建客户并链）。存量商机不动。表数 24→25。见 design D8。

## Capabilities

### New Capabilities
- `opportunity-artifact`：流转产出物（结构化文本 + 转换门禁 + Word 导出）。

### Modified Capabilities
- `opportunity`：advance 增加产出物门禁 + 同事务创建产出物 + 导出端点。
- `frontend-scaffold`：售前流转的产出物表单 + 商机看板的产出物查看/导出。

## Impact

- 后端 ~10 文件（1 实体 + 1 type 常量 + 1 规则配置 + 1 repo + ~3 DTO + 1 service + controller 改 + advance 改）+ POI 依赖 + 导出。新测试 2-3 类。**表数 23→24**（LegacyProductCategoryCleanupTest 更新）。
- 前端 ~4 文件（api + 售前流转表单改 + 商机看板产出物抽屉）。新测试 2-3。
- 配置/基础设施：**+1 表**（ddl-auto）、**+Apache POI 依赖**（Java-8 安全版本）、0 AI。新增 ~3 个 all-users API（列查 + 导出 + advance 改）。

## Success Criteria

- [ ] 线索→商机 advance **缺《商机调研报告》→ 400**；带（title+content）→ 创建 RESEARCH_REPORT 产出物 + 进商机（同事务）。
- [ ] 商机决策（通过/否决）**缺《决策评审纪要》→ 400**；带 → 创建 DECISION_MINUTES（记 decision）+ 流转（通过进 POC / 否决丢单）。
- [ ] 不要求产出物的转换（POC/投标…）不受影响，照常推进。
- [ ] `GET /{id}/artifacts` 列查倒序；`GET .../{artifactId}/export` 返回合法 .docx（正确 content-type + 内容含 标题/正文/客户/阶段/决策/作者/时间）。
- [ ] 机制可配置：新增「某转换需某产出物」只需加一条规则（无需改 advance 逻辑）。
- [ ] 前端：售前流转 线索推进 / 商机决策 弹表单提交即流转；商机看板可查看 + 导出 Word。
- [ ] 表数 24；backend 全绿 + frontend 全绿 + **temurin-8 Docker 构建过（含 POI）** + E2E（门禁挡住→带产出物放行→导出 docx）+ 存量数据零改。

## 显式排除（后续）

- 投标/合同/立项 的产出物（机制就绪，加配置即可）——本版只做 2 条。
- 产出物模板/富文本/多人协作/版本；产出物编辑（本版 append-only）。
- 文件上传附件（本版结构化文本 + 导出）。
- 产出物的权限收口（谁可看/可导出）——先 all-users。
