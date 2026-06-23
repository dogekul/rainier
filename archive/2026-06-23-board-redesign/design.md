# Design: v0.0.47 — 商机看板改版

基线 tag v0.0.46-contract-artifacts / commit bfe3224。看板**只读**性质不变。

## Context

- 看板数据来自 `GET /api/opportunities?size=100`（`OpportunityDetail`）。阶段/相位常量在前端 `api/opportunity.ts`（`OPP_PHASES` 售前/实施、`OPP_STAGE_LABELS`、`OPP_GATE_STAGES`、`OPP_STAGE_ORDER`）。
- 既有可复用：`utils/board.ts`（`StatusTier=red|yellow|green|gray`、`todayISO`）、`components/board`（`StatTiles`、`StatusChip`）、`Drawer`、`MarkdownView`。
- 后端 `Opportunity extends BaseEntity`（有 `createTime/updateTime` Instant）；`OpportunityService.advance()` 阶段推进在第 195 行 `o.setStage(next)`；create 在 LEAD 设阶段。启动 backfill 既有范式：`RequirementStatusBackfill`/`ProjectTypeBackfill`（`CommandLineRunner` + `@Order(HIGHEST_PRECEDENCE)` + native UPDATE，幂等）。

## Decisions

### D1 停留时间字段 `stageEnteredAt`（后端，P2 核心）
- 在 `Opportunity` 增 `@Column(name="stage_entered_at") Instant stageEnteredAt`（nullable）。
- `create()`：`o.setStageEnteredAt(Instant.now())`（创建即进入 LEAD）。
- `advance()`：仅在真正发生阶段变更的 PASS/非关口分支（第 195 行后）`o.setStageEnteredAt(Instant.now())`；REJECT 分支阶段不变 → 不刷新。
- `OpportunityDetail` 增 `stageEnteredAt` 并在 `from()` 赋值。
- **备选**：用 `updateTime` 当代理（零字段）。排除：updateTime 被任意编辑（改备注/金额）污染，无法反映"进入阶段"语义，且用户明确要求做 P2 后端字段。

### D2 既有行 backfill（非破坏）
- 新 `OpportunityStageEnteredAtBackfill`（`CommandLineRunner`，`@Order(HIGHEST_PRECEDENCE)`）：native `UPDATE rainier_opportunity SET stage_entered_at = update_time WHERE stage_entered_at IS NULL`，幂等。
- **仅填新列空值**，不触碰任何既有业务字段（客户名/阶段/金额/状态均不动），符合"不删改既有业务数据"。`update_time` 是"最近一次变更"的合理下界代理。
- **备选**：不 backfill，既有行显示灰（未知）。排除：会让既有 5 商机的预警点全灰、特性在 demo 数据上不可见；backfill 既非破坏又复用项目既有范式，更优（Gate 3 可一键回退）。

### D3 停留预警分级（前端纯函数 `utils/dwell.ts`）
- `dwellDays(stageEnteredAt, now)`：天数（向下取整）；null → null。
- `dwellTier(stageEnteredAt, now): StatusTier`：null→gray、≤`DWELL_GREEN_MAX`(7) 绿、≤`DWELL_YELLOW_MAX`(14) 黄、>14 红。阈值具名常量，便于后续按阶段细化。
- 复用 `board.ts` 的 `StatusTier`。纯函数、`now` 注入、单测隔离（同 `ryg.ts` 范式）。

### D4 金额格式化（前端纯函数 `utils/money.ts`）
- `formatCNY(amount)`：null/undefined→`'—'`；`<1e4`→`¥` + 千分位；`<1e8`→`¥{n/1e4}万`（≤1 位小数、去尾 0）；`≥1e8`→`¥{n/1e8}亿`。`Math.round` 防浮点尾差。单测覆盖边界。

### D5 看板布局（前端 `OpportunityBoard.tsx` 重写）
- **相位带堆叠**：售前带、实施带上下两行（`flex-direction:column`），每带列容器 `grid-template-columns:repeat(5,1fr)` → 列随容器自适应、不再 168px 定宽超宽横条。
- **漏斗条**：`OPP_STAGE_ORDER` 10 段，每段计数 + 相位色条（售前蓝/实施青）+ 关口 ⭐。
- **卡片**：客户（粗）+ `formatCNY(amount)` + 负责人 + 产品标签 + 停留预警点（`dwellTier`）+ WON/LOST chip；**整卡 `onClick`→产出物抽屉**，删除每卡「产出物」按钮；空列渲染 `—`。
- **列表视图**：`view` 状态 `'board'|'list'`；列表表格列 客户/标题/阶段/金额/负责人/停留天数/状态，可按 金额 / 阶段序 / 停留天数 排序（纯前端 `sort`）。
- 默认 board 视图。两视图均不含流转控件（只读不回归）。

### D6 过滤（前端）
- `ownerName` / `productName`（下拉，选项取自当前 rows 去重）、`q`（客户名包含，忽略大小写）、`includeLost`（默认 false：丢单仅计数；点"丢单"磁贴置 true → 丢单进列/表，带 LOST chip）。纯前端 filter，不加后端参数。

## Architecture / 数据流

`listOpportunities({size:100})` → rows → (filters) → filtered → {metric tiles, funnel counts, 两相位带列分组 / 列表行}。卡片/行点击 → `listOpportunityArtifacts(id)` → 只读抽屉（产出物 + 导出 Word，复用既有）。后端 advance 刷新 stageEnteredAt → 下次拉取反映新停留起点。

## Risks / Trade-offs

| 风险 | 缓解 |
|---|---|
| 既有测试（TC-OPPB-01/02、TC-OBA-01）依赖每卡 `opp-artifacts-{id}` 按钮 | 改版去按钮、整卡可点 → 同步更新这 3 个 TC 改点 `opp-card-{id}`/`opp-list-row-{id}` |
| Instant.now() 致 advance 时间断言脆弱 | 断言"非空 + 刷新后 ≥ 之前值 + REJECT 不变"，不做绝对时刻断言 |
| backfill native UPDATE 触碰既有行 | 仅 `WHERE stage_entered_at IS NULL` 填新列、不动业务字段；幂等；Gate 3 可回退 |
| 列数多致列表/看板横向溢出（680/容器宽） | board 每带 `repeat(5,1fr)` 自适应；列表 `table-layout` 控列宽 |
| 漏斗 + 过滤 + 列表令组件变大 | 纯函数（money/dwell）抽出单测；组件只做编排 |
