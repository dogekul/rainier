# Capability: frontend-scaffold — v0.0.42 po-inbox delta (MODIFIED)

> 合并入 canonical `specs/frontend-scaffold/spec.md`（Phase 6）。仅新增以下 Requirements。

## ADDED Requirements (from change 2026-06-18-po-inbox / v0.0.42)

### Requirement: 「需求收件箱」落地页

前端 SHALL 在 `/inbox` 提供「需求收件箱」页（all-users），消费 `GET /api/me/inbox`：渲染 StatTiles（待处理诉求数 /
我的需求数）+ 待处理诉求列表（标题 + 优先级 chip + 状态，链接到 `/pm/demands`）+ 我的需求列表（code+标题 + 状态 chip +
优先级 + 期望日期，链接到 `/pm/requirements`）；两区各自空态 EmptyState。

#### Scenario: 渲染两区

- **GIVEN** inbox 返回 1 条待处理诉求 + 1 条我的需求
- **WHEN** 用户打开 `/inbox`
- **THEN** SHALL 显示待处理诉求与我的需求各 1 行
- **AND** SHALL 显示计数磁贴

#### Scenario: 空收件箱

- **GIVEN** inbox 返回两区皆空
- **WHEN** 用户打开 `/inbox`
- **THEN** SHALL 显示两区的 EmptyState

### Requirement: 需求收件箱导航入口（all-users）

前端 SHALL 在 AppLayout「工作台」组加入「需求收件箱」入口指向 `/inbox`，且 `/inbox` SHALL NOT 被 `isAdminPath`
门控。`AppRoutes` SHALL 注册 `/inbox` 路由。

#### Scenario: /inbox 为 all-users

- **WHEN** 检查 `isAdminPath('/inbox')`
- **THEN** SHALL 返回 false

#### Scenario: 路由已注册

- **WHEN** 在 `/inbox` 挂载 AppRoutes
- **THEN** SHALL 渲染「需求收件箱」页（inbox 容器可见）
