# Capability: frontend-scaffold — v0.0.39 review-queue delta (MODIFIED)

> 合并入 canonical `specs/frontend-scaffold/spec.md`（Phase 6）。仅新增以下 Requirements。

## ADDED Requirements (from change 2026-06-18-review-queue / v0.0.39)

### Requirement: 「我的评审」落地页

前端 SHALL 在 `/reviews` 提供「我的评审」页（all-users），消费 `GET /api/me/pending-reviews`：
渲染待评审计数（StatTiles）+ 待评 Story 列表（每行 优先级 StatusChip + 提交人 OwnerChip + 标题链接 +
通过/打回 按钮）；空时显示 EmptyState。「通过/打回」按钮 SHALL 调 `POST /api/stories/{id}/review` 后刷新列表。

#### Scenario: 渲染待评审列表

- **GIVEN** `GET /api/me/pending-reviews` 返回 2 条待评 Story
- **WHEN** 用户打开 `/reviews`
- **THEN** 页面 SHALL 渲染这 2 条 Story 的标题
- **AND** SHALL 渲染待评审计数为 2

#### Scenario: 通过评审后刷新

- **GIVEN** `/reviews` 已渲染 1 条待评 Story
- **WHEN** 用户点击该行「通过」按钮
- **THEN** 前端 SHALL 调用 `submitReview(storyId, "APPROVED")`
- **AND** SHALL 重新拉取 pending-reviews 列表

#### Scenario: 空队列

- **GIVEN** `GET /api/me/pending-reviews` 返回空数组
- **WHEN** 用户打开 `/reviews`
- **THEN** 页面 SHALL 显示 EmptyState（无待评审）

### Requirement: 评审看板导航入口（all-users）

前端 SHALL 在 AppLayout「数据看板」组加入「评审看板」入口指向 `/reviews`，且 `/reviews` SHALL NOT 被
`isAdminPath` 门控（普通用户可达）。`AppRoutes` SHALL 注册 `/reviews` 路由。

#### Scenario: /reviews 为 all-users

- **WHEN** 检查 `isAdminPath('/reviews')`
- **THEN** SHALL 返回 false

#### Scenario: 路由已注册

- **WHEN** 在 `/reviews` 挂载 AppRoutes
- **THEN** SHALL 渲染「我的评审」页（reviews 容器可见）
