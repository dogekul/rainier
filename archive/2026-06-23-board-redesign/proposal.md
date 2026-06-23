# Proposal: v0.0.47 — 商机看板改版（P0+P1+P2）

## Why

现「商机看板」把 10 个阶段挤成一条 ~1900px 超宽横条（售前 5 + 实施 5 并排），空列照占整列、金额是裸数字、卡片每张挂「产出物」按钮、丢单看不见、无过滤/搜索、无"停留时长"预警 —— 监控角色（查看全商机进展）很难一眼看全、抓重点。本版按用户确认的 P0+P1+P2 全量改版。

## What Changes

P0（前端，立竿见影）
- 单条超宽横条 → **上下两条泳道带**（售前一行、实施一行），各 5 列基本一屏放下。
- 金额格式化（`¥200000` → `¥200万` / 千分位 / 亿）。
- 卡片瘦身：去掉每卡「产出物」按钮，**整卡可点**→只读产出物抽屉；空列收为占位 `—`。
- 关口列轻强调（⭐ + 列头/相位微染色）。

P1（前端）
- **漏斗分布条**：10 阶段计数 + 相位色条，全局分布不滚即见。
- **过滤/搜索**：按负责人 / 产品 / 客户名；"丢单"磁贴可点→纳入/筛出丢单。
- **看板 / 列表 视图切换**：列表视图可按 金额 / 阶段 / 停留天数 排序。

P2（后端 + 前端）
- 后端新增 `Opportunity.stageEnteredAt`（进入当前阶段的时间），create 设为当下、advance 阶段变更时刷新；`OpportunityDetail` 回传。
- 既有行 null 值由启动 backfill 以 `update_time` 兜底（非破坏，仅填新列空值）。
- 前端按 `now - stageEnteredAt` 计算停留天数 → 绿/黄/红预警点（≤7 绿 / ≤14 黄 / >14 红 / 无值灰）。

## Capabilities

- Modified: `opportunity`（+stageEnteredAt 字段/语义）、`frontend-scaffold`（商机看板改版）。
- New: 无。

## Impact

- 代码：后端 `Opportunity`/`OpportunityService`/`OpportunityDetail` + 新 backfill bootstrap + 测试；前端新 `utils/money.ts`、`utils/dwell.ts`（纯函数 + 单测）、改 `api/opportunity.ts`、重写 `pages/Crm/OpportunityBoard.tsx` + 测试。
- 配置/基建：无新表（`stage_entered_at` 为既有表新增列，ddl-auto=update 安全）、无新依赖、无新 API（复用 `GET /api/opportunities`、`/{id}/artifacts`）。
- 看板仍**只读**：不引入任何流转操作（新建/推进/关口决策仍在售前/实施流转页）。

## Success Criteria

- [ ] 看板呈两条上下泳道带，售前/实施各 5 列；不再是单条 10 列超宽横条。
- [ ] 金额在磁贴与卡片均格式化显示（万/亿/千分位）。
- [ ] 卡片无独立「产出物」按钮，整卡可点打开只读产出物抽屉；空列显示 `—`。
- [ ] 顶部漏斗条显示 10 阶段计数（关口标 ⭐）。
- [ ] 可按 负责人 / 产品 / 客户名 过滤；丢单可见（磁贴可点切换）。
- [ ] 可在 看板 / 列表 间切换；列表可排序。
- [ ] 后端 `OpportunityDetail.stageEnteredAt` 在 create 后非空、在 PASS/非关口 advance 后刷新、REJECT 不变；既有 null 行经 backfill 兜底。
- [ ] 前端按停留天数渲染绿/黄/红预警点（无值灰）。
- [ ] 不删改任何既有业务数据；后端 temurin-8 全绿 + 前端全绿 + E2E 绿。
