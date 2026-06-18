# Capability: frontend-scaffold — v0.0.40 me-profile delta (MODIFIED)

> 合并入 canonical `specs/frontend-scaffold/spec.md`（Phase 6）。仅新增以下 Requirements。

## ADDED Requirements (from change 2026-06-18-me-profile / v0.0.40)

### Requirement: 「我的档案」落地页

前端 SHALL 在 `/profile` 提供「我的档案」页（all-users），消费 `GET /api/me/profile`：渲染 身份卡（姓名/岗位/
登录名）+ 贡献 StatTiles（我负责的 Story 数 / 分配给我的任务数）+ 组织身份列表（组织名 + 类型 + 角色 chip + primary
标记）+ 直接上级（OwnerChip）；无组织关系时该区块显示 EmptyState。

#### Scenario: 渲染身份与贡献

- **GIVEN** `GET /api/me/profile` 返回 `{name:"Alice", positionName:"后端工程师", ownedStoryCount:3, assignedTaskCount:5, memberships:[...], manager:{name:"Bob"}}`
- **WHEN** 用户打开 `/profile`
- **THEN** 页面 SHALL 显示 "Alice" 与 "后端工程师"
- **AND** SHALL 显示贡献磁贴 Story=3 / Task=5
- **AND** SHALL 显示直接上级 "Bob"

#### Scenario: 组织关系列表

- **GIVEN** profile.memberships 含 1 项 `{organizationName:"采购小队", role:"MEMBER", isPrimary:true}`
- **WHEN** `/profile` 渲染完成
- **THEN** SHALL 显示 "采购小队" 与其角色标记

### Requirement: 我的档案导航入口（all-users）

前端 SHALL 在 AppLayout「工作台」组加入「我的档案」入口指向 `/profile`，且 `/profile` SHALL NOT 被 `isAdminPath`
门控（普通用户可达）。`AppRoutes` SHALL 注册 `/profile` 路由。

#### Scenario: /profile 为 all-users

- **WHEN** 检查 `isAdminPath('/profile')`
- **THEN** SHALL 返回 false

#### Scenario: 路由已注册

- **WHEN** 在 `/profile` 挂载 AppRoutes
- **THEN** SHALL 渲染「我的档案」页（profile 容器可见）
