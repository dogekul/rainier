# v0.0.39-review-queue — 待评审/待办泛化（路线图 #7）+ 我的评审看板（#8 首个消费落地页）

> Baseline: tag `v0.0.38-real-auth` / commit 4c9fd46。来自 [C-角色链路审计路线图](../../C-角色链路审计与建设路线图.md) #7（M/中，服务 架构师/测试负责人/PM/PO 4 角色）。

## Why

「谁等我评审」这条关系在数据层**根本不存在**。Story 今天只有 `ownerUserId`，没有 reviewer 概念 →
架构师 **0%**（整条链路卡在这）、测试负责人无放行门、PM/PO 拿不到真正的评审队列。本版在 Story 上补一层
`reviewerUserId + reviewStatus` 的 read-model，加自助查询端点与极简评审动作，并配第一个可见落地页「我的评审」，
把架构师从 0% 翻成「通」。延续 #1 作用域底座同款「建一次 read-model、多个落地页消费」的撬动模式。

## What Changes

**后端（capability entity-story MOD）**

- Story 实体 `+reviewerUserId`（Long，可空，软 FK→user）`+reviewStatus`（String，可空，length 16）。
- 新建 `ReviewStatus` 常量类：`PENDING`(待评审) / `APPROVED`(通过·放行) / `REJECTED`(打回)；`null` = 无评审需求。
- `StoryCreateRequest` / `StoryUpdateRequest` `+`可选 `reviewerUserId`（非空则 `userRepo.existsById` 校验）`+` 可选
  `reviewStatus`（非空则 `ReviewStatus.ALL` 校验）；`StoryDetail` `+reviewerUserId/reviewStatus/reviewerName` 富化。
- `GET /api/me/pending-reviews`（all-users 自助域）→ `reviewerUserId = 当前用户.id AND reviewStatus = PENDING`
  且未软删，富化（storyId/code/title/projectId/projectName/ownerName/sprintName/priority/status/createTime），
  排序：priority 高→低 tiebreak + 最久未评在前。
- `POST /api/stories/{id}/review` body `{decision: "APPROVED"|"REJECTED"}`（**R1 专用端点**；decision 校验，非法→400）
  → 置 reviewStatus，保留 reviewerUserId，返回更新后 `StoryDetail`。闭合「分派→待评→通过/打回→离开我的队列」回路，
  不要求重发整个 StoryUpdateRequest。**R2：all-users**（任意已认证用户可记录评审决定，与现有全员 CRUD 一致）。

**前端（capability frontend-scaffold MOD）**

- `api/reviews.ts`：`getPendingReviews()` / `submitReview(storyId, decision)`。
- `ReviewsPage`（标题「我的评审」）路由 `/reviews`：`StatTiles`(待评审计数) + `DashboardCard` 列出待评 Story
  （`OwnerChip` 提交人 + `StatusChip` 状态/优先级 + 通过/打回按钮调 submitReview 后刷新）+ `EmptyState`。
- 加入 `AppLayout`「数据看板」all-users 组（导航文案「评审看板」，/portfolio 旁）；`AppRoutes` 加 `/reviews`；
  **不入 `isAdminPath`**（all-users）。
- 测试：`ReviewsPage.test`（mock api + 渲染 + 通过/打回交互）、`AppRoutes.test`（/reviews literal+mount）、
  `navGuardConsistency` 自动覆盖（all-users 不被 admin 门控）。

**已确认子决策**：R1 = 专用 `POST /stories/{id}/review`；R2 = all-users（细粒度评审权限留后续）；
R3 = 三态 PENDING/APPROVED/REJECTED（放行 = APPROVED 语义复用）；R4 = 页面标题「我的评审」/ 导航「评审看板」（中性，4 角色通用）。

## Capabilities

### Modified Capabilities

- `entity-story`：Story 加 reviewer 字段（reviewerUserId/reviewStatus）+ 评审动作端点 + `/api/me/pending-reviews` 自助查询。
- `frontend-scaffold`：新增 ReviewsPage「我的评审」+「评审看板」导航入口 + /reviews 路由（all-users）。

### New Capabilities

- 无。

## Impact

**代码层面**：
- 后端 ~7 文件：`Story` / `StoryService` / `StoryCreateRequest` / `StoryUpdateRequest` / `StoryDetail` + 新
  `ReviewStatus` + 评审动作（`StoryController` 加 review 端点或新 `StoryReviewController`）+ `/api/me/pending-reviews`
  控制器+服务（mirror MeTeamController/PortfolioController）。新测试 ~2-3 个测试类。
- 前端 ~5 文件：`api/reviews.ts` / `ReviewsPage.tsx` + `index.tsx` / `AppRoutes.tsx` / `AppLayout.tsx`。新测试 ~2-3。

**配置层面**：
- 无。`spring.jpa.hibernate.ddl-auto=update`（Flyway 关）自动为 rainier_story 加 2 个可空列，零迁移、零回填。

**基础设施**：
- 无新服务、无新表、0 AI、0 新依赖。新增 2 个 API：`GET /api/me/pending-reviews`、`POST /api/stories/{id}/review`，均 all-users（token 必需、非 admin）。

## Success Criteria

- [ ] Story 持久化 reviewerUserId/reviewStatus；create/update 接受并校验（reviewer 不存在→400、reviewStatus 非法→400）。
- [ ] `GET /api/me/pending-reviews` 只返回「我作为 reviewer 且 reviewStatus=PENDING」的未软删 Story，富化齐全，排序正确；无 token→401。
- [ ] `POST /api/stories/{id}/review` 置 APPROVED/REJECTED 后该 Story 离开 pending 队列；非法 decision→400；不存在 id→404；无 token→401。
- [ ] 存量 Story（reviewStatus=null）不进任何人的待评队列；**业务数据零改**（standing 约束）。
- [ ] ReviewsPage 渲染待评列表 + 通过/打回交互可用并刷新；/reviews all-users 可达且 `isAdminPath('/reviews')===false`。
- [ ] backend 全绿（421 baseline + 新增）+ frontend 全绿（baseline + 新增）+ E2E 评审链路（分派→pending→approve→离队）真实验证。
