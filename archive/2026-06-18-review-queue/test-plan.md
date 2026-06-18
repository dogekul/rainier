# Test Plan — v0.0.39-review-queue

> Baseline backend 421 green. New TCs below; all P0.

## 测试策略

- 后端集成测试（@SpringBootTest + MockMvc，H2 test profile）= 评审字段持久化/校验、评审决定端点、pending-reviews 查询+排序+授权。
- 前端组件测试（Vitest + RTL，mock api/reviews）= ReviewsPage 渲染/交互/空态；AppRoutes 路由；navGuardConsistency 自动覆盖。
- E2E（Docker 真 MySQL）= 分派→pending→approve→离队 全链 + 存量数据零改。
- 复用既有 seed 链（user→project→requirement→sprint→story）；pending-reviews 测试可直接 storyRepo.saveAndFlush 设 reviewer 字段。

## 详细测试案例

### entity-story（后端）

| TC-ID | Scenario | 预期 |
|---|---|---|
| TC-REVQ-001 | 创建 Story 带 reviewerUserId + reviewStatus=PENDING | 201；reviewerUserId/reviewStatus/reviewerName 富化正确 |
| TC-REVQ-002 | 创建带 reviewerUserId 不存在 | 400 "reviewer user not found" |
| TC-REVQ-003 | 创建带非法 reviewStatus | 400 "invalid reviewStatus" |
| TC-REVQ-004 | 创建不带评审字段 | 201；reviewerUserId=null、reviewStatus=null |
| TC-REVQ-005 | `POST /stories/{id}/review {APPROVED}` | 200；reviewStatus=APPROVED；reviewerUserId 不变 |
| TC-REVQ-006 | review 决定 REJECTED | 200；reviewStatus=REJECTED |
| TC-REVQ-007 | review 非法 decision | 400 "invalid decision" |
| TC-REVQ-008 | review 不存在的 Story | 404 |
| TC-REVQ-009 | 更新 Story 改 reviewerUserId/reviewStatus（合法） | 200；字段更新 |
| TC-REVQ-010 | 既有 Story 更新（不带评审字段）回归 | 200；既有 owner-mutable 测试不破（null→null no-op） |

### 我的待评审队列（后端）

| TC-ID | Scenario | 预期 |
|---|---|---|
| TC-MEREV-001 | alice GET pending-reviews：只返回 alice 的 PENDING | 200；仅含 alice 的 PENDING，不含 bob 的、不含 APPROVED 的 |
| TC-MEREV-002 | 按优先级排序（URGENT 在 LOW 前） | 首条为 URGENT |
| TC-MEREV-003 | 富化（projectName/sprintName/ownerName） | 字段非空且正确 |
| TC-MEREV-004 | 无 token | 401 |
| TC-MEREV-005 | 无待评 → 空数组 | 200 + `[]` |

### frontend-scaffold（前端）

| TC-ID | Scenario | 预期 |
|---|---|---|
| TC-REVP-01 | 渲染 2 条待评 + 计数=2 | 列表 2 行、reviews-summary 显示 2 |
| TC-REVP-02 | 点「通过」→ submitReview(id,APPROVED) + 刷新 | mock 被以 APPROVED 调用、refetch |
| TC-REVP-03 | 空队列 → EmptyState | reviews-empty 可见 |
| TC-FES-REV-01 | /reviews 路由挂载 ReviewsPage | reviews 容器/标题可见 |
| TC-FES-REV-02 | AppRoutes.tsx 含 /reviews literal | grep ≥1 |
| TC-FES-REV-03 | isAdminPath('/reviews')===false（navGuardConsistency 自动） | all-users |

### E2E

| TC-ID | Scenario | 预期 |
|---|---|---|
| TC-E2E-REV-001 | 建 Story 设 reviewer=alice PENDING → GET /api/me/pending-reviews(alice token) 见之 → POST review APPROVED → 再查队列已无 | 全链通过 |
| TC-E2E-REV-002 | 存量 Story（无 reviewStatus）不在任何 pending 队列；既有业务计数不变 | 数据零改 |

## 回归风险矩阵

| 区域 | 风险 | 缓解 |
|---|---|---|
| StoryService create/update | 🟡中 | 评审字段为 null-safe 追加；既有 8 个 create + owner-filter 测试回归 |
| StoryDetail 序列化 | 🟢低 | 纯新增字段 |
| /api/me 路由 | 🟢低 | 复用 PortfolioController 范式，all-users 不入 AdminPaths |
| 前端导航守卫 | 🟢低 | navGuardConsistency 自动钉 /reviews all-users |
