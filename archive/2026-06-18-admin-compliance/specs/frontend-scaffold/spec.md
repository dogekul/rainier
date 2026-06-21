# Capability: frontend-scaffold — v0.0.41 admin-compliance delta (MODIFIED)

> 合并入 canonical `specs/frontend-scaffold/spec.md`（Phase 6）。仅新增以下 Requirements。

## ADDED Requirements (from change 2026-06-18-admin-compliance / v0.0.41)

### Requirement: 「合规仪表盘」页（admin）

前端 SHALL 在 `/sys/compliance` 提供「合规仪表盘」页（admin），消费 `GET /api/compliance/audit-summary` +
`GET /api/compliance/residual-permissions`：渲染 审计 StatTiles（事件总量 + 停用-残留权限用户数，残留>0 标红）+
停用-残留权限对账表（停用用户 + 角色数 + 角色名，空时 EmptyState）+ 按动作/按实体类型分布 + 最近活动表。

#### Scenario: 渲染审计聚合与残留对账

- **GIVEN** audit-summary 返回 `{total:5, byAction:[{label:"CREATE",count:3}], recent:[1 条]}`，residual 返回 1 个停用用户 ghost（DEV）
- **WHEN** 用户打开 `/sys/compliance`
- **THEN** SHALL 显示审计总量 5
- **AND** SHALL 显示残留行含 "ghost" 与 "DEV"
- **AND** SHALL 显示按动作 "CREATE"

#### Scenario: 无残留 → 空态

- **GIVEN** residual-permissions 返回空数组
- **WHEN** 用户打开 `/sys/compliance`
- **THEN** SHALL 显示 EmptyState（无残留）

### Requirement: 合规仪表盘导航入口（admin）

前端 SHALL 在 AppLayout「系统」组加入「合规仪表盘」入口指向 `/sys/compliance`；`/sys/compliance` SHALL 被
`isAdminPath` 门控（经 `/sys` 前缀，仅 admin 可达）。`AppRoutes` SHALL 注册 `/sys/compliance` 路由。

#### Scenario: /sys/compliance 为 admin

- **WHEN** 检查 `isAdminPath('/sys/compliance')`
- **THEN** SHALL 返回 true

#### Scenario: 路由已注册

- **WHEN** 在 `/sys/compliance` 挂载 AppRoutes
- **THEN** SHALL 渲染「合规仪表盘」页（compliance 容器可见）
