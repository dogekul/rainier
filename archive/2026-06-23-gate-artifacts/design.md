# Design — v0.0.45-gate-artifacts

> Baseline: tag `v0.0.44-customer-flow` / commit e88c8ab. Gate 0/1 locked: 结构化文本 + Word 导出 / combined 提交 /
> 2 规则可配置 / 纪要通过+否决都要 / 产出物 append-only / 查看导出在商机看板.

## Context

客户全流程的流转是裸点，无业务留痕。本版给「转换」挂可配置的产出物门禁：advance 从某阶段推进时，若该阶段配置了
所需产出物，则必须随 advance 提交该产出物（结构化文本），否则 400；满足则同事务创建产出物并流转。产出物 append-only，
可列查 + 导出 Word(.docx)。先落地 线索→商机《商机调研报告》、商机决策《决策评审纪要》两条规则。后端无现成导出/POI 基建
（greenfield）。ddl-auto 自动建 1 表（23→24）。

## Decisions

### D1 — OpportunityArtifact 实体（append-only）
表 `rainier_opportunity_artifact` extends BaseEntity（复用 createBy=作者、createTime）。字段：`opportunityId`(Long) /
`type`(RESEARCH_REPORT|DECISION_MINUTES，可扩展) / `stageFrom`(产生该产出物的来源阶段) / `title` / `content`(长文本，
`columnDefinition="TEXT"`) / `decision`(可空；纪要记 PASS/REJECT)。**无修改/删除 API**（append-only，仿 AuditLog 不加 @SQLDelete）。

### D2 — TransitionArtifactRules（按来源阶段配置）
`advance()` 永远从 `o.getStage()` 推进；关口决策也是「从该阶段 advance」。故规则统一按**来源阶段**键：
`Map<stage, artifactType>` = `{LEAD: RESEARCH_REPORT, OPPORTUNITY: DECISION_MINUTES}`，`requiredFor(stage)` 返回类型或 null。
扩展投标/合同/立项产出物只需加一条，advance 逻辑不变。

### D3 — advance() 门禁（combined，同事务）
在「LOST/终态」校验、关口 decision 校验之后、状态变更之前插入：
```
String requiredType = TransitionArtifactRules.requiredFor(stage);
if (requiredType != null) {
  if (artifact == null || blank(artifact.title) || blank(artifact.content))
      throw new BadRequestException("此转换需提交《" + ArtifactType.label(requiredType) + "》");
  artifactRepo.save(new OpportunityArtifact(id, requiredType, stage, title, content, decision)); // decision 可空
}
```
对要求产出物的关口（如商机），**通过与否决都创建纪要**（decision 记入）。非关口要求（LEAD）创建报告（decision=null）。
不要求产出物的转换忽略 artifact（向后兼容）。原子：advance 已 @Transactional。

### D4 — Word 导出（Apache POI）
加依赖 `org.apache.poi:poi-ooxml:4.1.2`（Java-8 安全，过 temurin-8 gate）。`toDocx(artifactId)` 用 XWPFDocument 生成：
标题(heading) + 元信息段（类型/客户·商机/阶段/决策/作者/时间）+ 正文段落。`GET /{id}/artifacts/{aid}/export` 返回
`ResponseEntity<byte[]>`，`Content-Type: application/vnd.openxmlformats-officedocument.wordprocessingml.document`，
`Content-Disposition: attachment; filename="artifact-<oppId>-<aid>.docx"`（filename 用 ASCII 数字 id，避免中文在 header 的编码问题；前端下载时再用 typeLabel 命名）。

### D5 — 读 API（无写/改/删）
`GET /api/opportunities/{id}/artifacts` → 倒序列查（OpportunityArtifactDetail）。`GET /{id}/artifacts/{aid}/export` → docx。
产出物仅由 advance 创建，无独立 POST/PUT/DELETE。

### D6 — 前端
- `OPP_TRANSITION_ARTIFACT`（镜像后端规则）+ `advanceOpportunity(id, decision?, note?, artifact?)`。
- 「售前流转」：点操作时，若该行 stage 在规则内 → 打开产出物抽屉（LEAD→《商机调研报告》；商机→《决策评审纪要》，通过/否决
  各带 decision），填 标题+正文 → 提交即 advance(+artifact)；不在规则内的转换沿用现有（POC 直接推进；投标/合同 否决走确认弹窗）。
- 「商机看板」（只读）：per-商机「产出物」按钮 → 只读抽屉列出产出物，每条「导出 Word」（带 bearer 的 fetch→blob→下载）。
  看板仍**无流转操作控件**；产出物查看/导出属只读，不破坏「看板只看不动流转」语义（测试断言改为「无流转控件」而非「零按钮」）。
- `api/opportunityArtifact.ts`（list + export blob）。

### D7 — 商机产品标签（2026-06-23 fold-in）
**理由**：用户要求「给商机加一个产品标签，使用产品的枚举，创建时关联产品，不确定可留空」。Opportunity 加可空
`productId`（FK 到既有 Product，无 @ManyToOne，纯 Long 列），create/update 接收 productId，非空时 `productRepo.existsById`
校验（缺→400），`OpportunityDetail` enrich `productName`。前端：售前流转 新建商机抽屉加 产品下拉（来自 listProducts，
可空「未选择」）；只读商机看板卡片显示 `🏷 productName`。无新表（复用 rainier_product），表数仍 24。

### D8 — 客户实体（2026-06-23 fold-in）
**理由**：用户要求「客户做成实体，创建商机时可选，没有也允许填写新建」。NEW `customer` capability：
`Customer`（rainier_customer，soft-delete，字段 name 必填 + industry/contactName/notes 可空）+ CRUD（`/api/customers`，
all-users）+ 前端「客户」管理页（/crm/customers，客户导航组首项）。**商机接法（非破坏）**：Opportunity 保留 customerName
显示字段 + 加可空 `customerId` FK。`advance`/create 的 `applyCustomer(customerId, customerName)`：给 customerId → 链该客户并
采用其名；否则按 customerName 复用同名客户（findFirstByNameIgnoreCase）或新建一个 → 链 customerId。存量商机（中信/招商）
customerId 留空、customerName 不动。前端 售前流转 新建抽屉的客户字段改为 datalist 组合框（选已有 or 输入新名）。表数 24→25。

### D9 — 商机备注 + 新建表单字段顺序（2026-06-23 fold-in）
用户要求：新建商机加「备注」（标题下方），产品移到备注下方，金额放最后。实现：Opportunity 加可空 `note`
（@Column length 2000）+ create/update/Detail；售前流转 新建抽屉字段顺序改为 **客户 → 标题 → 备注(textarea) → 产品 →
四负责人 → 金额(末位)**。无新表/新依赖。

## Architecture / Data flow

```
售前流转 点「推进/通过/否决」
  └─ stage∈规则? ── 是 → 弹产出物表单(报告/纪要) → POST /advance {decision?, note?, artifact{title,content}}
                    否 → 现有行为(直接 advance / 否决确认)
advance(): LOST/终态校验 → 关口 decision 校验 → [门禁: 规则要求则校验+建 OpportunityArtifact(同事务)] → 推进/赢丢单
商机看板(只读) → GET /{id}/artifacts (抽屉列查) → GET /{id}/artifacts/{aid}/export → .docx 下载
表: rainier_opportunity_artifact (23→24)
```

## Risks / Trade-offs

| Risk | Mitigation |
|---|---|
| **改了既有 advance 语义**：LEAD/OPPORTUNITY advance 现在缺产出物会 400 | 同步更新既有测试（OpportunityControllerTest TC-OPP-003/005 带 artifact；PresaleFlow TC-PRE-02 填纪要）+ E2E 走带产出物路径 |
| POI 在 Java 8 / temurin-8 构建 | 钉 poi-ooxml 4.1.2（Java-8 安全）；Docker 后端重建验证；导出走 byte[] 不落盘 |
| 看板「只读」语义被产出物按钮稀释 | 明确：只读=无**流转**控件；查看/导出是只读操作；测试断言改为「无 opp-new/pass/reject/advance」+「有产出物查看按钮」 |
| 导出需鉴权 | 前端用带 bearer 的 fetch 取 blob 触发下载（非 `<a href>`）；E2E 用 curl 验证 docx 字节(PK 头)+content-type |
| content 长文本 | `columnDefinition="TEXT"`；ddl-auto 建列 |
| 存量数据 | 纯新增表 + advance 增量门禁；不动既有商机数据；standing 约束 |
