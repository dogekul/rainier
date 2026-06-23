# Test Plan — v0.0.44-customer-flow

> Baseline backend 470 / frontend 180 green. New TCs below; all P0. 表数 21→23.

## 测试策略

- 后端集成（@SpringBootTest + MockMvc，H2）= Opportunity 创建/校验/推进状态机/关口/赢丢单/立项；Operation 创建/线性推进/关闭。
- 前端组件（Vitest，mock api）= 两看板渲染 + 新建/推进；AppRoutes /crm/*；navGuardConsistency 自动。
- E2E（Docker 真 MySQL）= 全链：建商机→推进→关口 PASS→赢单→立项链 Project；运营推进。+ 表数 23 + 存量零改。

## 详细测试案例

### opportunity（后端，OpportunityControllerTest）

| TC-ID | Scenario | 预期 |
|---|---|---|
| TC-OPP-001 | 最小创建 | 201；stage=LEAD、status=OPEN |
| TC-OPP-002 | 负责人不存在 | 400 |
| TC-OPP-003 | 非关口推进（LEAD→OPPORTUNITY） | 200 |
| TC-OPP-004 | 关口缺决策 | 400 |
| TC-OPP-005 | 关口 PASS（OPPORTUNITY→POC） | 200 |
| TC-OPP-006 | 售前关口 REJECT → LOST | 200；status=LOST |
| TC-OPP-007 | CONTRACT PASS → WON + 入实施 | 200；status=WON、stage=INITIATION |
| TC-OPP-008 | 已丢单（LOST）推进 | 409 |
| TC-OPP-009 | 立项移交：WON + PASS 链 projectId | 200；projectId 设置 |
| TC-OPP-010 | 立项移交：未 WON | 409 |
| TC-OPP-011 | 列表过滤 status | 仅匹配 |
| TC-OPP-012 | 实施环节推进（立项评审 PASS：INITIATION→SURVEY） | 200；stage=SURVEY、status=WON |
| TC-OPP-013 | 验收（ACCEPTANCE）为终点推进 | 409 |
| TC-OPP-014 | 立项评审 REJECT 停在立项 | 200；stage=INITIATION、status=WON（不丢单） |

### operation（后端，OperationControllerTest）

| TC-ID | Scenario | 预期 |
|---|---|---|
| TC-OPR-001 | 最小创建 | 201；stage=MAINTENANCE、status=ACTIVE |
| TC-OPR-002 | 线性推进 | 200；stage=OPERATING |
| TC-OPR-003 | 末节点推进 → CLOSED | 200；status=CLOSED |
| TC-OPR-004 | 已关闭推进 | 409 |

### frontend-scaffold（前端）

| TC-ID | Scenario | 预期 |
|---|---|---|
| TC-OPPB-01 | 商机看板**只读**：两段泳道 + 关键列 + 赢单 chip（size 100） | 售前/实施 band + 列 + WON chip |
| TC-OPPB-02 | 看板零操作控件（read-only） | 无 新建/推进/通过/否决；button 数 0 |
| TC-PRE-01 | 售前流转：只列 OPEN×售前；关口→通过/否决，非关口→推进；WON/LOST 排除 | 行 + 阶段操作 + 过滤 |
| TC-PRE-02 | 售前关口 通过→advance(id,'PASS')+refetch | advance 调用 + 刷新 |
| TC-PRE-03 | 售前 新建商机抽屉 → create | createOpportunity 调用 |
| TC-PRE-04 | 新建空必填 → formError，无 create | 表单报错 |
| TC-DEL-01 | 实施流转：只列 WON×实施；立项→移交+通过/否决，非关口→推进，验收→已验收；OPEN 排除 | 行 + 阶段操作 + 过滤 |
| TC-DEL-02 | 立项 通过→advance(id,'PASS')+refetch | advance 调用 + 刷新 |
| TC-DEL-03 | 立项移交：选 Project → initiate(id,projectId,'PASS') | initiateOpportunity 调用 |
| TC-OPRB-01 | 运营看板渲染（不变） | 列 + 卡 |
| TC-FES-CRM-01 | 4 条 /crm 路由注册 + 商机看板挂载 | 路由字面 + 容器可见 |
| TC-FES-CRM-02 | /crm/presale-flow 挂载售前流转 | presale-summary 可见 |
| TC-FES-CRM-03 | /crm/delivery-flow 挂载实施流转 | delivery-summary 可见 |
| TC-FES-CRM-04 | isAdminPath('/crm/*')===false（navGuard 自动） | all-users（含 2 新页） |

### 表数 + E2E

| TC-ID | Scenario | 预期 |
|---|---|---|
| TC-CRM-TABLES | LegacyProductCategoryCleanupTest | 23 |
| TC-E2E-CRM-001 | 建商机→推进→关口→赢单→立项链 Project | 链路通 |
| TC-E2E-CRM-002 | 运营建单→推进→关闭 | 链路通 |
| TC-E2E-CRM-003 | 存量业务数据不变 | 数据零改（仅 +2 表） |

## 回归风险矩阵

| 区域 | 风险 | 缓解 |
|---|---|---|
| 2 新表 + 状态机 | 🟡中 | advance 守卫 status==OPEN + STAGE_ORDER 边界；WON/LOST 终态；关口/立项测试覆盖；表数测试更新 |
| 负责人校验 | 🟢低 | existsById 派生校验，复用 Story owner 范式 |
| 新「客户」导航组 | 🟢低 | navGuardConsistency 自动钉 all-users |
| 存量数据 | 🟢低 | 纯新增表，不动 Project/既有实体 |
