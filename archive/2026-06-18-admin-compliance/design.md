# Design — v0.0.41-admin-compliance

> Baseline: tag `v0.0.40-me-profile` / commit 75b4fa1. Gate 1 decisions C1–C3 locked.

## Context

The admin role's hook (a compliance dashboard) doesn't exist — only the raw `/sys/audit-logs` table.
The data is all present: AuditLog (append-only, written by AuditAspect on every entity write) + User
(`enabled`) + UserRole (grants) + Role (names). `/api/audit-logs` is already AdminPaths Tier A. This
closes the security track started by v0.0.21 (admin-authz) + v0.0.38 (real-auth).

## Decisions

### D1 — NEW capability `admin-compliance`; new /api/compliance, Tier A (C1)
`ComplianceController @RequestMapping("/api/compliance")` with two GETs. Add `/api/compliance` to
`AdminPaths.TIER_A` → both endpoints are token+elevation gated by the existing
`AdminAuthorizationInterceptor` (matrix-param-safe lookup path). `AdminPaths.matches` is exact-or-prefix,
no sibling collision (`/api/compliance` vs `/api/compliance-x` would not match — only `base` or `base/...`).

### D2 — audit-summary aggregation
`GET /api/compliance/audit-summary` → `AuditSummary{ total, byAction[LabelCount], byEntityType[LabelCount],
recent[AuditLogDetail] }`. JPQL `GROUP BY a.action / a.entityType ORDER BY COUNT(a) DESC` (busiest-first),
returned as `List<Object[]>` and mapped to `LabelCount{label,count}` in the service (null-safe:
`row[0]==null?null`, `((Number)row[1]).longValue()`). `total = auditRepo.count()`. `recent =
findTop20ByOrderByCreateTimeDescIdDesc → AuditLogDetail.from`.
- *Why Object[] not projection interface*: avoids Spring Data projection-proxy Jackson edge cases;
  explicit mapping is bulletproof and Java-8 clean.

### D3 — residual-permission reconciliation (C2)
`GET /api/compliance/residual-permissions` → `List<ResidualPermission>`. `userRepo.findByEnabledFalse()`
(honors User `@Where(del_flag=0)` → soft-deleted users excluded) → for each, `userRoleRepo.findByUserId`;
**include only users with ≥1 grant**; enrich role names via `roleRepo.findAllById` (deduped by
`LinkedHashSet` of roleIds). `roleCount = grants.size()`.
- *Security framing*: the grants are INERT — `ElevationService` never elevates a disabled user and
  real-auth blocks their login — so this is NOT an active breach. It's a de-provisioning hygiene gap
  (stale grants that should be revoked). The DTO/page frame it as「建议回收」, not「越权」.

### D4 — frontend「合规仪表盘」(admin)
`api/compliance.ts:getAuditSummary/getResidualPermissions`. `CompliancePage` at `/sys/compliance`:
`StatTiles`(总量 + 残留用户数, red when >0) + residual table (name/login + roleCount + roleNames +
`EmptyState`) + by-action/by-entity breakdown + recent activity table. Nav item「合规仪表盘」(icon
`gauge`) in the 系统 group (`requiresAdmin`); route under `/sys`. `/sys` is already in
`ADMIN_PATH_PREFIXES` → `isAdminPath('/sys/compliance')` is true → navGuardConsistency auto-pins it as
admin (consistent with the requiresAdmin group). The page renders its shell synchronously so it mounts
during the un-hydrated guard window (same as the existing AuditLogsPage test).

## Architecture / Data flow

```
GET /api/compliance/audit-summary    → ComplianceService.auditSummary()
  ├─ total = auditRepo.count()
  ├─ byAction = auditRepo.countGroupedByAction()  (JPQL GROUP BY, mapped to LabelCount)
  ├─ byEntityType = auditRepo.countGroupedByEntityType()
  └─ recent = auditRepo.findTop20ByOrderByCreateTimeDescIdDesc() → AuditLogDetail
GET /api/compliance/residual-permissions → ComplianceService.residualPermissions()
  └─ userRepo.findByEnabledFalse() → filter has-grants → enrich roleNames
[both gated: AdminPaths Tier A /api/compliance → token + isElevated]
CompliancePage /sys/compliance (系统 admin group) → both endpoints → board-kit
```

## Risks / Trade-offs

| Risk | Mitigation |
|---|---|
| Over-stating residual as active breach | D3 framing — inert grants, hygiene only; page says 建议回收 |
| AdminPaths sibling collision | matches() is base-or-base/... — /api/compliance does not catch siblings |
| N+1 in residual enrich | bounded by disabled-user count (small); findByUserId + one findAllById per user |
| GROUP BY portability (H2/MySQL) | plain JPQL COUNT/GROUP BY — portable; tested on H2, E2E on MySQL |
| Java 8 | Object[] mapping, Collectors.toList/toCollection, no Set.of/var/no-arg orElseThrow; temurin-8 gate |
