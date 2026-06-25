# Proposal: B7 残留权限一键回收

## Problem
v0.0.41 的 admin-compliance change 引入了 `GET /api/compliance/residual-permissions`，列出「已停用但仍持有 role」的用户，但只能看不能改 —— admin 还得手工去删每条 UserRole。需要一键回收。

## Decision
在 `/api/compliance` 下加 2 个写端点（POST，admin Tier A）：

| 端点 | 鉴权 | 行为 |
|---|---|---|
| POST /api/compliance/users/{id}/revoke-roles | admin (Tier A) | 仅当 User.enabled=false 时允许；硬删该用户全部 UserRole；写审计行 REVOKE_RESIDUAL_ROLES（含 roleIds） |
| POST /api/compliance/disable-user/{id} | admin (Tier A) | 设 User.enabled=false（若已 false 则幂等）→ 自动 revoke 所有 UserRole；写两条审计（DISABLE_USER + REVOKE_RESIDUAL_ROLES）|

`/api/compliance` 已经在 AdminPaths Tier A，无需再加；但为了语义清晰、防止有人将来重写 AdminPaths 时漏掉 disable-user 分支，proposal 仍在 spec 显式列出 base 前缀（实质 no-op）。

## Non-Goals
- 重新启用用户的流程（B 批不做）
- HR / 离职系统联动
- 软删 UserRole（明确硬删，因 cleanup 语义）

## Compatibility
- 现有 `GET /api/compliance/residual-permissions` 行为不变
- 现有 admin-authz / SecurityFilter / AuditAspect 都不需要改 —— 这两个端点的 controller 方法名不是 create/update/delete，AuditAspect 不会自动抓，所以服务里手工调 `auditLogService.record(...)` 写一条
- 前端在 CompliancePage 的 residual 表格行加一个「一键回收」按钮，点击 → POST revoke-roles → reload
