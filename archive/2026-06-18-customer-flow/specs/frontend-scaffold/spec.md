# Capability: frontend-scaffold — v0.0.44 customer-flow delta (MODIFIED)

> 合并入 canonical `specs/frontend-scaffold/spec.md`（Phase 6）。仅新增以下 Requirements。

## ADDED Requirements (from change 2026-06-18-customer-flow / v0.0.44)

### Requirement: 「商机看板」只读进展总览

> 2026-06-23 修订：看板拆为「只读总览」，所有流转操作移到独立操作页（见下两条 Requirement）。

前端 SHALL 在 `/crm/opportunities` 提供 **只读** 的「商机看板」页（all-users；受众=监控角色，待定），消费
`GET /api/opportunities`（size 上限 100）：faithful 还原客户全流程图的两段泳道 —— **售前环节**（线索/商机/推介POC/
投标/合同签订）与 **实施环节**（立项/现场调研/产品诉求/交付实施/验收）各为一个 phase band（带负责人标注），band 内按
节点分列；关口列（商机/投标/合同/立项）标 ⭐。卡片含 客户/标题/金额/负责人/赢单标识。顶部 StatTiles：进行中/赢单/丢单
+ 在谈金额。OPEN+WON 在 band 内显示，LOST 仅滚动进 tile。看板 SHALL NOT 提供任何流转操作控件（无新建/推进/关口决策）。

#### Scenario: 只读渲染两段泳道

- **GIVEN** `GET /api/opportunities` 返回若干商机
- **WHEN** 用户打开 `/crm/opportunities`
- **THEN** SHALL 渲染「售前环节」「实施环节」两个 phase band，band 内按节点分列渲染只读卡片
- **AND** SHALL NOT 渲染任何操作按钮（新建/推进/通过/否决）

### Requirement: 「售前流转」操作页

前端 SHALL 在 `/crm/presale-flow` 提供「售前流转」操作页（all-users），消费 `GET /api/opportunities`（size 上限 100），
列出 `status=OPEN ∧ stage∈售前环节` 的商机为操作表：每行含 阶段 / 客户·标题 / 金额 / 负责人 / 操作。非关口节点（线索/
推介POC）SHALL 提供「推进」（`advance` 无 decision）；关口节点（商机/投标/合同）SHALL 提供「通过」（PASS）/「否决」
（REJECT→丢单）。SHALL 提供「新建商机」侧拉抽屉（客户/标题/金额 + 四负责人下拉 + 必填校验）。WON/LOST/实施 商机 SHALL NOT 出现在此页。

#### Scenario: 售前操作 + 新建

- **GIVEN** `GET /api/opportunities` 返回若干 OPEN 售前商机（含关口与非关口）
- **WHEN** 用户打开 `/crm/presale-flow`
- **THEN** SHALL 按阶段为每行渲染对应操作（关口→通过/否决，非关口→推进）
- **AND** SHALL 提供「新建商机」抽屉入口；WON/LOST/实施 商机 SHALL NOT 出现

### Requirement: 「实施流转」操作页

前端 SHALL 在 `/crm/delivery-flow` 提供「实施流转」操作页（all-users），消费 `GET /api/opportunities`（size 上限 100），
列出 `status=WON ∧ stage∈实施环节` 的商机为操作表：每行含 阶段 / 客户·标题 / 项目经理 / 关联Project / 操作。立项
（INITIATION）SHALL 提供「立项移交」（侧拉抽屉选交付 Project → `POST /api/opportunities/{id}/initiate` PASS 链 projectId）
与「通过」（PASS→现场调研）/「否决」（停在立项）；非关口（现场调研/产品诉求/交付实施）SHALL 提供「推进」；验收
（ACCEPTANCE）SHALL 为终态「已验收」无操作。

#### Scenario: 实施操作 + 立项移交

- **GIVEN** `GET /api/opportunities` 返回若干 WON 实施商机
- **WHEN** 用户在 INITIATION 行点「立项移交」选择一个 Project 并确认
- **THEN** SHALL 调 `initiate(id, projectId, 'PASS')` 链入交付 Project
- **AND** 验收（ACCEPTANCE）行 SHALL NOT 渲染任何推进操作

### Requirement: 「运营看板」落地页

前端 SHALL 在 `/crm/operations` 提供「运营看板」页（all-users），消费 `GET /api/operations`：按 3 节点分列 + 新建 + 推进。

#### Scenario: 渲染运营看板

- **GIVEN** `GET /api/operations` 返回若干运营单
- **WHEN** 用户打开 `/crm/operations`
- **THEN** SHALL 按节点分列渲染

### Requirement: 客户导航组（all-users）

前端 SHALL 在 AppLayout 新增「客户」顶级导航组（all-users），含 4 项：「商机看板」→`/crm/opportunities`、「售前流转」
→`/crm/presale-flow`、「实施流转」→`/crm/delivery-flow`、「运营看板」→`/crm/operations`；`/crm/*` SHALL NOT 被
`isAdminPath` 门控。`AppRoutes` SHALL 注册这 4 条 /crm 路由。

#### Scenario: /crm/* 为 all-users

- **WHEN** 检查 `isAdminPath('/crm/opportunities')`、`isAdminPath('/crm/presale-flow')`、`isAdminPath('/crm/delivery-flow')`
- **THEN** SHALL 全部返回 false
