# Test Plan — v0.0.42-po-inbox

> Baseline backend 453 / frontend 171 green. New TCs below; all P0.

## 测试策略

- 后端集成（@SpringBootTest + MockMvc，H2）= inbox 两区聚合 + 过滤 + 排序 + 富化 + 降级 + 401。
- 前端组件（Vitest，mock api/inbox）= InboxPage 渲染/空态；AppRoutes /inbox；navGuardConsistency 自动。
- E2E（Docker 真 MySQL）= 真实 PO 收件箱 + 存量数据零改。
- seed：直接 saveAndFlush（User/Demand/DemandRequirementLink/Requirement/Project）。

## 详细测试案例

### me-inbox（后端，MeInboxControllerTest）

| TC-ID | Scenario | 预期 |
|---|---|---|
| TC-INBOX-001 | 待处理诉求 = 未关联且非终态 | 含 D1(PENDING 无关联)；不含 D2(已关联)、D3(CLOSED) |
| TC-INBOX-002 | 我的需求按 owner 过滤 | 含 alice 的 R1；不含 bob 的 R2 |
| TC-INBOX-003 | 我的需求 projectName 富化 + 优先级排序 | 首条 URGENT；R-LOW.projectName=Apollo |
| TC-INBOX-004 | 待处理诉求按优先级排序 | URGENT 在 LOW 前 |
| TC-INBOX-005 | 无 token | 401 |
| TC-INBOX-006 | token sub 无对应用户 → 两区皆空 | 200；unconvertedDemands=[]、myRequirements=[] |

### frontend-scaffold（前端）

| TC-ID | Scenario | 预期 |
|---|---|---|
| TC-INBOXP-01 | 渲染两区 + 计数 | 待处理诉求行 + 我的需求行 + summary |
| TC-INBOXP-02 | 空收件箱 → 两区空态 | inbox-demands-empty + inbox-reqs-empty |
| TC-FES-INBOX-01 | /inbox 路由挂载 InboxPage | inbox 容器可见 |
| TC-FES-INBOX-02 | AppRoutes.tsx 含 /inbox literal | grep ≥1 |
| TC-FES-INBOX-03 | isAdminPath('/inbox')===false（navGuardConsistency 自动） | all-users |

### E2E

| TC-ID | Scenario | 预期 |
|---|---|---|
| TC-E2E-INBOX-001 | 真实用户 token → GET /api/me/inbox 返回两区 | 链路通 |
| TC-E2E-INBOX-002 | 存量业务数据不变（纯读） | 数据零改 |

## 回归风险矩阵

| 区域 | 风险 | 缓解 |
|---|---|---|
| 新 /api/me/inbox | 🟢低 | 纯新增只读端点，复用 me 范式 |
| RequirementRepository +findByOwnerUserId | 🟢低 | 派生查询，尊重 @Where |
| demandRepo.findAll 量级 | 🟡中 | 应用量级小；增长后改 Specification NOT-IN |
| 前端导航守卫 | 🟢低 | navGuardConsistency 自动钉 /inbox all-users |
