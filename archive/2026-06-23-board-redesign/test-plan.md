# Test Plan: v0.0.47 — 商机看板改版

## 策略
金字塔：纯函数单测（money/dwell）→ 后端集成（stageEnteredAt + backfill）→ 前端组件（board）。E2E 真栈烟雾。
复用既有 `OpportunityBoard.test.tsx` 三例（更新去按钮、整卡可点）。

## 后端 TC（OpportunityControllerTest / OpportunityArtifactTest 同栈）

| TC | 场景 | 断言 |
|---|---|---|
| TC-OSEA-01 | create 记录 stageEnteredAt | 创建后 detail.stageEnteredAt 非空 |
| TC-OSEA-02 | PASS 推进刷新 | 非关口/PASS 推进后 stageEnteredAt ≥ 旧值且非空 |
| TC-OSEA-03 | REJECT 不刷新 | BIDDING REJECT 后 stageEnteredAt == 旧值（丢单、阶段不变） |
| TC-OSEA-04 | backfill 兜底 | 手插 stage_entered_at=NULL 行 → 运行 backfill → 该行=update_time、业务字段不变 |

## 前端 TC（utils 单测）

| TC | 场景 | 断言 |
|---|---|---|
| TC-MON-01 | formatCNY 边界 | null→'—'；5000→`¥5,000`；200000→`¥20万`；2000000→`¥200万`；120000000→`¥1.2亿` |
| TC-DWL-01 | dwellTier 分级 | null→gray；3天→green；10天→yellow；20天→red |
| TC-DWL-02 | dwellDays | 给定 stageEnteredAt + now 算整天数 |

## 前端 TC（OpportunityBoard.test.tsx）

| TC | 场景 | 断言 |
|---|---|---|
| TC-OPPB-01（改） | 两相位带 + 阶段列 | opp-phase-presale/delivery + opp-col-LEAD/CONTRACT/INITIATION/ACCEPTANCE + WON chip + 产品标签 |
| TC-OPPB-02（改） | 只读无流转控件 + 整卡可点 | 无 opp-new/pass/reject/advance；无 opp-artifacts-7 按钮；点 opp-card-7 开抽屉 |
| TC-OBA-01（改） | 点卡看产出物 + 导出 | 点 opp-card-7 → listArtifacts(7) → opp-artifact-55 → 导出 docx |
| TC-OPPB-03 | 漏斗分布条 | opp-funnel + opp-funnel-OPPORTUNITY 计数正确 |
| TC-OPPB-04 | 金额格式化 | 卡片显示 `¥200万`（amount=2000000） |
| TC-OPPB-05 | 按负责人过滤 | 选 opp-filter-owner=王伟 → 仅留王伟卡片 |
| TC-OPPB-06 | 丢单磁贴切换 | 默认无 LOST 卡；点 opp-tile-lost → LOST 卡出现 + LOST chip |
| TC-OPPB-07 | 看板/列表切换 | 点 opp-view-list → opp-list 出现；点 opp-view-board → 回带 |
| TC-OPPB-08 | 停留预警点分级 | stageEnteredAt 20 天前 → opp-dwell-{id} 红级 class/title |

## E2E（docker 真栈）
- 后端：create 商机 → detail.stageEnteredAt 非空；advance → 刷新；REJECT → 不变（curl）。
- 前端：看板页 served bundle 含 opp-funnel / opp-view-list 标记；登录浏览看板呈两带（served 校验 + 可选浏览器）。
- 不删改既有业务数据：throwaway 商机建即删。

## 回归风险
- 🟡 OpportunityBoard 重写：三个既有 TC 同步改点（去按钮、整卡可点）。
- 🟢 后端 stageEnteredAt 纯新增列 + create/advance 两处赋值；advance 既有阶段/赢丢单语义不变（回归 OpportunityControllerTest 全绿）。
- 🟢 backfill 幂等、仅填空值。
