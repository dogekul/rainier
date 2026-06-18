# v0.0.39-review-queue — 测试报告 (Phase 5 VERIFY)

> Baseline: tag `v0.0.38-real-auth` / commit 4c9fd46。路线图 #7 待评审泛化 + #8 首个消费落地页。

## 1. 总体概况

| 维度 | 结果 |
|------|------|
| 后端单元/集成 | **435 / 435** ✅（421 baseline + 14 new；0 fail / 0 error / 0 skip） |
| 前端组件/路由 | **162 / 162** ✅（157 baseline + 5 new；42 files）+ tsc clean + eslint 0 warn |
| 新增后端测试 | StoryReviewTest **9/9**、MePendingReviewsControllerTest **5/5** |
| 新增前端测试 | ReviewsPage.test **3/3** + AppRoutes /reviews **1** + navGuardConsistency 自动 +1 |
| E2E（Docker 真 MySQL，real-auth on） | 评审全链 + 边界 6/6 ✅ |
| 多路评审 (Step 0) | 3 reviewers / 9 findings / **C:0 H:0** / confirmed-real **0** |

## 2. 新增测试

**entity-story（StoryReviewTest，TC-REVQ-001..009）**：create 带 reviewer→201+reviewerName 富化 / reviewer 不存在→400 / 非法 reviewStatus→400 / 无评审字段→201 null / review APPROVED→200(保留 reviewer) / REJECTED→200 / 非法 decision→400 / 未知 Story→404 / PUT 设评审字段→200。

**我的待评审队列（MePendingReviewsControllerTest，TC-MEREV-001..005）**：只返回我的 PENDING（排除他人/已决） / 优先级排序(URGENT 在 LOW 前) / 富化(ownerName/projectName/sprintName) / 无 token→401 / 空→`[]`。

**前端（ReviewsPage.test TC-REVP-01..03 + AppRoutes TC-FES-REV-01/02）**：渲染列表+计数 / 通过→submitReview(id,APPROVED)+refetch+离队 / 空态 / /reviews 路由挂载 + literal。navGuardConsistency 自动断言 `isAdminPath('/reviews')===false`。

## 3. E2E（live stack — Docker，真 MySQL，real-auth on）

| # | 验证 | 结果 |
|---|------|------|
| 1 | 建 Story 设 reviewerUserId=alice、reviewStatus=PENDING | reviewerName="Alice" 富化、reviewStatus=PENDING ✅ |
| 2 | `GET /api/me/pending-reviews`(alice) 含该 Story | count 1, contains ✅ |
| 3 | `POST /stories/{id}/review {APPROVED}` | reviewStatus=APPROVED、reviewerUserId 不变 ✅ |
| 4 | 再查队列已无该 Story | count 0 ✅ |
| 5 | 非法 decision→400 / 未知 Story→404 / 无 token→401 | 400 / 404 / 401 ✅ |
| 6 | 存量业务数据 + schema | reviewer_user_id/review_status 列由 ddl-auto 自动建；users 7 / projects 5 / requirements 10 不变；存量 Story 全部 review_status=null（不进任何队列）✅ |

> standing 约束：仅新增 1 条 E2E 测试 Story（additive），未删/改任何存量行；存量 Story 评审字段全 null。

## 4. 多路并行技术评审（Step 0）+ 11 类失败模式

**3 reviewers（code / test-config / docs-spec）**：9 findings，**C:0 H:0**，对抗式 verify 后 **confirmed-real = 0**。

核心 confirmations：
- **Java-8 clean**：ReviewStatus 用 `Collections.unmodifiableSet(new HashSet<>(Arrays.asList(...)))`；全 12 个新增/改动后端文件 grep 零 `Set.of/List.of/var/.isBlank/Stream.toList/无参 orElseThrow`（Docker temurin-8 已编译验证）。
- **N+1 规避**：MeReviewService 主查询 + 3 批 findAllById（owner/project/sprint），与 StoryService.list Code-M1 同款。
- **软删一致**：`findByReviewerUserIdAndReviewStatus` 与 `review()` 经 `@Where(del_flag=0)` 实体，软删 Story 不入队列且 review→404。
- **null-safe**：priorityRank(null)→末位、createTime `Comparator.nullsLast`、enrich 全 null 守卫。
- **授权真测**：`/api/me/pending-reviews` 无 token→401 是真断言（SecurityFilter 无条件解析身份 + controller `currentUsername` 守卫）。
- **测试非重言式**：reviewerName 富化、400 消息前缀、保留 reviewerUserId、优先级排序（seed 逆序仍断言 URGENT 在前）均钉真实分支。

**11 类失败模式**：无幻觉（API/类真实）；范围聚焦（仅 entity-story + frontend-scaffold）；契约 (k) — PendingReview 前后端字段逐一对齐、StoryDetail 形状增量；无覆盖真空 (j)。**(d) 上下文一致**：design.md D1–D5 与代码逐条吻合。

## 5. 设计调整 / 已知取舍（记录，不阻塞）

- **PUT 全量替换 reviewer 字段**（design.md D2 + Risks）：普通 Story PUT（不带 reviewer 字段）会把 reviewer 清空。当前无 Story 编辑 UI 触发，既有 Story 更新测试 null→null 无回归。**后续**：Story 编辑 UI 须暴露评审字段，或改用 PATCH 语义。
- **「标题」渲染为纯文本**（非链接）：Rainier 无 Story 详情路由，避免死链；评审动作经 通过/打回 按钮完成。
- **R2 all-users 评审授权**：任意已认证用户可记录评审决定，与既有全员 CRUD 一致；细粒度评审权限点留后续。

## 6. 结论

| 信号 | 状态 |
|------|------|
| 后端 435/435 + 前端 162/162 + tsc/lint | ✅ |
| 新增 14 后端 + 5 前端测试全绿 | ✅ |
| E2E 评审全链（分派→pending→approve→离队）+ 边界 | ✅ |
| Docker 真 JDK-8 构建 | ✅ |
| 多路评审 C:0 H:0 confirmed-real:0 | ✅ |
| 存量业务数据零改（standing 约束） | ✅ |
| 架构师 0%→「通」（评审看板落地） | ✅ |

**部署建议**：可交付。后续候选：Story 编辑 UI 暴露评审字段（消除 PUT-wipe）、Task 维度评审、细粒度评审权限、评审多轮历史。
