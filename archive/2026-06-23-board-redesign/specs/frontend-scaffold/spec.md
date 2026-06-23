# Capability: frontend-scaffold — v0.0.47 board-redesign delta (MODIFIED)

> 合并入 canonical `specs/frontend-scaffold/spec.md`（Phase 6）。商机看板改版（P0+P1+P2）。仍只读。见 [[opportunity]]。

## MODIFIED Requirements (from change 2026-06-23-board-redesign / v0.0.47)

### Requirement: 商机看板改版 — 泳道带 / 漏斗 / 过滤 / 列表 / 停留预警

商机看板 SHALL 以**上下两条相位泳道带**（售前 / 实施）呈现，每带含其 5 个阶段列（不再为单条 10 列横条）。看板 SHALL
提供：顶部 10 阶段**漏斗分布条**（计数 + 关口标记）；按 **负责人 / 产品 / 客户名** 过滤 + **丢单磁贴**切换可见；
**看板 / 列表** 视图切换（列表可按 金额 / 阶段 / 停留天数 排序）；卡片金额 SHALL 格式化（万/亿/千分位）、**整卡可点**打开
只读产出物抽屉（不再每卡一个按钮）、按 `stageEnteredAt` 渲染**停留预警点**（绿/黄/红/灰）。看板 SHALL 保持只读（无新建/推进/关口控件）。

#### Scenario: 两条相位泳道带渲染

- **GIVEN** 商机列表加载完成
- **WHEN** 看板渲染（board 视图）
- **THEN** SHALL 呈现 `opp-phase-presale` 与 `opp-phase-delivery` 两带
- **AND** SHALL 含 `opp-col-LEAD`…`opp-col-ACCEPTANCE` 各阶段列

#### Scenario: 漏斗分布条显示阶段计数

- **GIVEN** 若干商机分布于不同阶段
- **WHEN** 看板渲染
- **THEN** `opp-funnel` SHALL 出现，且每阶段 `opp-funnel-{STAGE}` SHALL 显示该阶段在谈计数

#### Scenario: 金额格式化与整卡可点

- **GIVEN** 一条 `amount=2000000` 的商机
- **WHEN** 看板渲染
- **THEN** 其卡片 SHALL 显示 `¥200万`（非裸数字）
- **AND** SHALL 无独立 `opp-artifacts-{id}` 按钮；点击 `opp-card-{id}` SHALL 打开只读产出物抽屉

#### Scenario: 按负责人过滤

- **GIVEN** 看板含负责人为「王伟」与「李娜」的商机
- **WHEN** 选择负责人「王伟」（`opp-filter-owner`）
- **THEN** SHALL 仅保留王伟负责的卡片

#### Scenario: 丢单磁贴切换可见

- **GIVEN** 存在 LOST 商机，默认不在列中
- **WHEN** 点击 `opp-tile-lost`
- **THEN** SHALL 将 LOST 商机纳入展示并标 LOST chip

#### Scenario: 看板/列表切换

- **WHEN** 点击 `opp-view-list`
- **THEN** SHALL 呈现列表视图 `opp-list`（含可排序列）
- **AND** 点击 `opp-view-board` SHALL 切回泳道带

#### Scenario: 停留预警点按天数分级

- **GIVEN** 距今 3 / 10 / 20 天及无 `stageEnteredAt` 的商机
- **WHEN** 看板渲染
- **THEN** 各卡片 `opp-dwell-{id}` SHALL 分别为 green / yellow / red / gray

#### Scenario: 按产品过滤

- **GIVEN** 看板含产品「采购平台」与「风控系统」的商机
- **WHEN** 选择 `opp-filter-product`=「采购平台」
- **THEN** SHALL 仅保留该产品的卡片

#### Scenario: 客户名搜索（忽略大小写）

- **GIVEN** 看板含客户「ACME 集团」与「蓝海物流」
- **WHEN** 在 `opp-filter-q` 输入「acme」
- **THEN** SHALL 仅保留客户名包含该子串（忽略大小写）的卡片

#### Scenario: 列表排序

- **GIVEN** 列表视图含金额/停留天数不同的多条商机
- **WHEN** `opp-list-sort` 选「按金额」或「按停留天数」
- **THEN** `opp-list-row-{id}` SHALL 按相应键降序排列（空值末位）
