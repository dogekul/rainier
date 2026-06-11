# Capability: frontend-scaffold

> MODIFIED in v0.0.15-audit-log (2026-06-11).
> 新增「系统」顶级 Sider 组（第 5 组）+ 只读审计日志查询页 AuditLogsPage + /sys/audit-logs 路由。
> 其余 v0.0.8–v0.0.14 Requirement 不变。

## ADDED Requirements

### Requirement: Sider 顶级菜单组「系统」（v0.0.15 起加，5 顶级组）

前端 SHALL 在 Sider 渲染 5 个顶级菜单组 — **组织 → 产品 → 需求管理 → 人事配置 → 系统**。「系统」组为第 5 位（末位），展开后含 1 项：**审计日志**，对应 `/sys/audit-logs` 路由。

#### Scenario: Sider 含「系统」组 + 审计日志入口

- **GIVEN** 用户已登录访问 `/`
- **WHEN** 页面渲染完成
- **THEN** 左侧 Sider SHALL 含 5 个顶级菜单组，末位为「系统」
- **AND** 「系统」组展开后 SHALL 含「审计日志」项
- **AND** 点击「审计日志」SHALL 跳转 `/sys/audit-logs`

### Requirement: AuditLogsPage 只读查询页

前端 SHALL 在 `/sys/audit-logs` 渲染 `AuditLogsPage`：表格展示 actor / entityType / entityId / action / 时间，提供 actor / entityType / entityId / action 过滤 + 分页。页面 SHALL **只读** —— 无新建 / 编辑 / 删除按钮、无 EditDrawer。

#### Scenario: 渲染审计表格

- **GIVEN** 后端 `GET /api/audit-logs` 返回 2 条审计行
- **WHEN** `/sys/audit-logs` 渲染完成
- **THEN** SHALL 渲染表格含这 2 行
- **AND** 表头 SHALL 含 `操作人` / `实体类型` / `实体ID` / `动作` 列
- **AND** 页面 SHALL **不**含「新建」按钮

#### Scenario: 按 entityType 过滤触发查询

- **GIVEN** `/sys/audit-logs` 已渲染
- **WHEN** 用户在 entityType 过滤输入 "REQUIREMENT" 并触发查询
- **THEN** SHALL 调用 `listAuditLogs` 且 params 含 `entityType: "REQUIREMENT"`

### Requirement: /sys/audit-logs 路由注册

前端 SHALL 在 router 注册 `/sys/audit-logs` → `AuditLogsPage`。

#### Scenario: 路由直接访问

- **GIVEN** 用户已登录
- **WHEN** 浏览器直接访问 `/sys/audit-logs`
- **THEN** SHALL 渲染 `AuditLogsPage` 组件
- **AND** `grep -c "/sys/audit-logs" frontend/src/AppRoutes.tsx` SHALL ≥ 1
