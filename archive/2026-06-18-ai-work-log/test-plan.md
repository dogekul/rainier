# Test Plan — v0.0.43-ai-work-log

> Baseline backend 459 / frontend 175 green. New TCs below; all P0. 表数 20→21.

## 测试策略

- 后端集成（@SpringBootTest + MockMvc，H2）= create(evidence 必填) + list/filter + decision 状态机 + 种子。
- 种子测试：用 @TestPropertySource 开 `app.demo.ai-work-log-seed.enabled=true` 验证种入 + 幂等（默认 test profile flag=false 不污染其它测试）。
- 前端组件（Vitest，mock api/aiWorkLog）= 页面渲染 + 裁决交互 + 空态；AppRoutes /ai/work-logs；navGuardConsistency 自动。
- E2E（Docker 真 MySQL）= 种子 + 列表 + 裁决链 + 存量数据零改 + 表数 21。

## 详细测试案例

### ai-work-log（后端，AiWorkLogControllerTest）

| TC-ID | Scenario | 预期 |
|---|---|---|
| TC-AIW-001 | 创建提议 | 201；status=PROPOSED |
| TC-AIW-002 | 缺 evidence | 400 |
| TC-AIW-003 | 按 status 过滤 | content 仅 PROPOSED |
| TC-AIW-004 | 采纳 | 200；status=ACCEPTED；decidedBy 非空 |
| TC-AIW-005 | 驳回无 reason | 400 |
| TC-AIW-006 | 驳回带 reason | 200；status=REJECTED；rejectReason="误判" |
| TC-AIW-007 | 重复裁决 | 409 |
| TC-AIW-008 | 非法 decision | 400 |
| TC-AIW-009 | 裁决不存在 id | 404 |

### 种子（后端，AiWorkLogSeedTest，@TestPropertySource flag=true）

| TC-ID | Scenario | 预期 |
|---|---|---|
| TC-AIW-SEED-001 | 表空 → 种入 ≥1 PROPOSED（带 evidence） | count>0 |
| TC-AIW-SEED-002 | 已有数据 → 不重复种（幂等） | count 不变 |

### frontend-scaffold（前端）

| TC-ID | Scenario | 预期 |
|---|---|---|
| TC-AIWP-01 | 渲染日志 + 裁决按钮 | 行 + 采纳/驳回 |
| TC-AIWP-02 | 采纳 → decide(id,ACCEPTED) + 刷新 | mock 调用 + refetch |
| TC-AIWP-03 | 空列表 → EmptyState | ai-empty |
| TC-FES-AIW-01 | /ai/work-logs 路由挂载 | 容器可见 |
| TC-FES-AIW-02 | AppRoutes.tsx 含 /ai/work-logs literal | grep ≥1 |
| TC-FES-AIW-03 | isAdminPath('/ai/work-logs')===false（navGuardConsistency 自动） | all-users |

### 表数 + E2E

| TC-ID | Scenario | 预期 |
|---|---|---|
| TC-AIW-TABLES | LegacyProductCategoryCleanupTest 表数 | 21 |
| TC-E2E-AIW-001 | 种子启动 + GET 列表 + 采纳/驳回链 | 链路通 |
| TC-E2E-AIW-002 | 存量业务数据不变 | 数据零改（仅新增 ai_work_log 表+种子） |

## 回归风险矩阵

| 区域 | 风险 | 缓解 |
|---|---|---|
| 新表 + 种子运行器 | 🟡中 | flag-gated（test off）+ 幂等 count==0；表数测试更新 |
| 裁决状态机 | 🟡中 | 仅 PROPOSED 可裁决（409）；非法/缺 reason 400；测试覆盖 |
| 新 AI 导航组 | 🟢低 | navGuardConsistency 自动钉 all-users |
| 存量数据 | 🟢低 | 纯新增表，不动既有实体 |
