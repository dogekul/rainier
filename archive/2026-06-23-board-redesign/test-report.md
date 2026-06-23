# Test Report: v0.0.47 — 商机看板改版（P0+P1+P2）

> 范围：看板上下两条相位泳道带 + 漏斗分布条 + 负责人/产品/客户过滤 + 看板/列表切换 + 停留时长预警点 + 金额格式化；
> 后端 `Opportunity.stageEnteredAt`（create 设、advance 刷新、REJECT 不刷新）+ 既有行 backfill。基线 v0.0.46-contract-artifacts / bfe3224。看板仍只读。

## 1. 总体概况

| 维度 | 结果 |
|---|---|
| 后端单元/集成 (temurin-8) | **515 / 515** ✅（511 baseline + 4 TC-OSEA；0 fail/error/skip） |
| 前端组件/路由/工具 | **245 / 245** ✅（54 files；+TC-OPPB-03..11、money 6、dwell 3）+ tsc clean + eslint 0 warn |
| 后端新增 TC | 4（TC-OSEA-01..04） |
| 前端新增 TC | 13（utils 9 it-block + board TC-OPPB-03..11） |
| E2E（docker 真 MySQL，前后端均重建） | green |
| 多代理评审 (Step 0) | 4 维度 + 对抗式 verify：**13 raw → 确认 10（C:0 H:0 M:4 L:6）+ 证伪 3**，全部处置 |
| 表 | 25（无新表；新增列 `rainier_opportunity.stage_entered_at`） |
| 存量数据 | 完好（仅 stage_entered_at 新列经 backfill 填充；商机 stage/status/名称/金额未改；throwaway 建即删） |

## 2. 改动

- 后端：`Opportunity` +`stage_entered_at`（Instant，nullable）；`OpportunityService` create 设 `Instant.now()`、advance 阶段前进分支刷新（REJECT 早退分支不刷）；`OpportunityDetail` +`stageEnteredAt`；NEW `OpportunityStageEnteredAtBackfill`（native `UPDATE ... = update_time WHERE stage_entered_at IS NULL`，幂等、仅填空值）。
- 前端：NEW `utils/money.ts`（formatCNY，舍入后选单位）+ `utils/dwell.ts`（dwellDays/dwellTier，复用 board.ts StatusTier）；`api/opportunity.ts` +`stageEnteredAt`；重写 `pages/Crm/OpportunityBoard.tsx`（相位带堆叠 + 漏斗 + 过滤 + 看板/列表 + 停留点 + 整卡可点）。
- 无新表/依赖/API。

## 3. E2E（live stack — Docker，真 MySQL + JDK-8，前后端均重建）

| # | 步骤 | 结果 |
|---|---|---|
| A | backfill：7 条既有商机 | stage_entered_at 全部非空 = 各自 update_time ✅ |
| B | create | stageEnteredAt 非空（=创建时刻）✅ |
| C | LEAD→OPPORTUNITY 推进 | stageEnteredAt 刷新前进（晚于创建）✅ |
| D | OPPORTUNITY REJECT | status=LOST、stageEnteredAt 不变 ✅ |
| E | 前端 served bundle | opp-funnel / opp-view-list / opp-filter-owner / opp-list-sort 均在 ✅ |

> 注：首轮 E2E 因 backend 镜像被旧 jar 缓存命中（stageEnteredAt 缺失），经 `--no-cache` 重建后通过。已记入「修复确认」。
> throwaway 商机（36/37/38）与 `__E2E_*` 客户（15/16/17）均建即删；用户自有商机/客户（id 4–9）未动。

## 4. 多代理评审（Step 0）+ 处置

4 维度并行（backend / frontend / test-spec / failure-modes）→ 每条 finding 对抗式 verify。**13 raw → 确认 10（C:0 H:0 M:4 L:6）+ 证伪 3**。

| 维度 | 严重度 | 发现 | 处置 |
|---|---|---|---|
| frontend | L | formatCNY 在 99,995,000–99,999,999 舍入成「¥10000万」而非「¥1亿」 | **已修**：舍入后再选单位 + TC（99999999→¥1亿） |
| frontend | L | 列表行缺 keyboard 可达（无 tabIndex/onKeyDown） | **已修**：tr +tabIndex/onKeyDown(Enter/Space) |
| failure-modes | L | tabStyle 用未定义裸 token `var(--rainier-color-text)` | **已修**：→ `--rainier-color-text-1` |
| test-spec | M | TC-OSEA-04 未断言 backfill 值=update_time、未钉 WHERE-NULL 守卫 | **已修**：断言 ==update_time + sentinel 行不被改 |
| test-spec | M | 列表排序/列表停留点/OPEN 状态格 无测试 | **已修**：TC-OPPB-09（金额/停留排序 + 列表停留 tier + 进行中） |
| test-spec | M | 产品过滤/客户搜索 无测试 | **已修**：TC-OPPB-10（产品）/ TC-OPPB-11（客户名忽略大小写） |
| test-spec | M | dwellTier gray/green/yellow 未在 DOM 断言（仅 red） | **已修**：TC-OPPB-08 参数化 4 档 |
| test-spec | L | formatCNY 负数分支 + funnel 过滤后计数 未测 | **已修**（负数）；funnel 过滤经 TC-OPPB-05/06 卡片断言间接覆盖 |
| test-spec | L | 产品/客户/排序 spec 无独立 Scenario | **已修**：change spec 补 4 个 Scenario |
| failure-modes | L | 列表排序零测试（同上 M 项） | **已修**（TC-OPPB-09） |
| frontend | （证伪） | listRows `now` 快照跨午夜与显示天数差 1 | 证伪：同一闭包内排序自洽；纯外观、极低概率、只读看板无副作用 |
| test-spec | （证伪） | TC-OSEA-02/03 单边时间界，mutant 存活 | 证伪：源码无 t0+δ/now() 表达式可变异；删刷新/移入 REJECT 均被现有断言杀死 |
| test-spec | （证伪） | 无 gate-PASS 刷新 stageEnteredAt 的 TC | 证伪：刷新为 gate/非 gate 共用同一行，TC-OSEA-02（SURVEY 非关口）已覆盖该行 |

评审结论：C:0 H:0 M:4 L:6，均 < 阈值；确认 10 条全修，证伪 3 条经核属实。

## 5. 失败模式（a–k）

- (b) 范围：仅改计划内文件（后端 4 + 前端 5 + STDD 文档）。
- (d) 一致：实现符合 design.md D1–D6（PASS-only 刷新 / backfill 非破坏 / 整卡可点 / 漏斗 / 停留分级）。
- (j) 覆盖真空：评审补齐列表排序/过滤/停留分级覆盖。
- (k) 契约：前端 `stageEnteredAt` 字段名/类型与后端 `OpportunityDetail` JSON 一致（E2E 实测返回）；StatusTier 复用 board.ts；裸 CSS token 已修。
- 数据安全：backfill 仅 `WHERE stage_entered_at IS NULL` 填新列，不改任何业务字段（TC-OSEA-04 sentinel 行验证）。

## 6. 修复确认

- 评审 10 条确认项全部修复并回归（前端 245/245、后端 515/515）。
- E2E backend 镜像缓存导致 stageEnteredAt 缺失 → `--no-cache` 重建解决，复测全绿。

## 7. 结论

✅ 商机看板改版（P0+P1+P2）按规格落地：两条相位泳道带 + 漏斗 + 过滤 + 看板/列表 + 停留预警 + 金额格式化；看板保持只读不回归。后端 stageEnteredAt create/advance/reject 语义正确，既有行 backfill 非破坏。
后端 515/515（temurin-8）+ 前端 245/245 + tsc/lint clean；E2E 全链绿；评审 C:0 H:0 M:4 L:6 全处置。建议进入 Gate 3。
