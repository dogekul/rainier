# v0.0.41-admin-compliance — 管理员合规仪表盘（路线图 #10）

> Baseline: tag `v0.0.40-me-profile` / commit 75b4fa1。来自 [C-角色链路审计路线图](../../C-角色链路审计与建设路线图.md) #10（M/中，服务 管理员）。

## Why

管理员钩子（合规仪表盘）不存在，只落在原始审计表。数据已全有（AuditAspect 每写一行 + UserRole）。本版补一个
**管理员合规仪表盘**：审计活动聚合 + **停用-残留权限对账**（停用用户仍挂着角色授权 = 应回收的治理缺口）。补全
最后一个未服务的角色层（系统/管理员），并闭合 v0.0.38 真实鉴权 + v0.0.21 admin-authz 这条安全 track。

## What Changes

**后端（NEW capability `admin-compliance`，admin-gated）**

- `GET /api/compliance/audit-summary` → `{ total, byAction:[{label,count}], byEntityType:[{label,count}],
  recent:[最近 20 条 AuditLogDetail] }`（AuditLog JPQL GROUP BY，busiest-first + top-20 派生查询）。
- `GET /api/compliance/residual-permissions` → 停用用户（`enabled=false`、未软删）中仍有 UserRole 授权者：
  `[{userId, name, loginName, roleCount, roleNames[]}]`（建议回收）。授权是 inert（停用用户永不提升、不能登录），
  属 de-provisioning 卫生缺口，非活跃越权。
- `AdminPaths` 加 `/api/compliance` 到 **Tier A**（整体 admin 门控，沿用 matrix-param 安全 lookup path）。
- repo 加：`AuditLogRepository.countGroupedByAction/EntityType` + `findTop20ByOrderByCreateTimeDescIdDesc`、
  `UserRepository.findByEnabledFalse`。

**已确认子决策**：C1 = 新 `/api/compliance`（聚 audit-summary + residual，AdminPaths +1 前缀）；C2 = 停用且 ≥1 UserRole；
C3 = 总量 + 按动作 + 按实体 + 最近 20。

**前端（capability frontend-scaffold MOD，admin）**

- `api/compliance.ts`：getAuditSummary / getResidualPermissions。
- `CompliancePage`「合规仪表盘」`/sys/compliance`：审计 StatTiles（总量 + 残留用户数，残留>0 红）+ **残留权限对账表**
  （停用用户 + 角色数 + 角色名 + 回收提示，空时 EmptyState）+ 按动作/按实体分布 + 最近活动表。
- 加入「系统」组（admin，icon `gauge`，审计日志旁）+ `/sys/compliance` 路由（经 `/sys` 前缀自动 admin 门控）。

## Capabilities

### Modified Capabilities

- `frontend-scaffold`：新增 CompliancePage「合规仪表盘」+ 系统组导航 + /sys/compliance 路由（admin）。

### New Capabilities

- `admin-compliance`：审计聚合（GET /api/compliance/audit-summary）+ 停用-残留权限对账（GET /api/compliance/residual-permissions），admin-only。

## Impact

**代码层面**：
- 后端 ~8 文件：新 ComplianceController / ComplianceService / 3 DTO（AuditSummary/LabelCount/ResidualPermission）；
  AdminPaths +1 前缀；AuditLogRepository +2 GROUP BY +findTop20；UserRepository +findByEnabledFalse。新测试 2 类
  （ComplianceControllerTest 功能 + ComplianceAuthzTest 门控）。
- 前端 ~5 文件：api/compliance.ts / CompliancePage+index / AppRoutes / AppLayout。新测试 2（CompliancePage + AppRoutes）。

**配置层面**：无。

**基础设施**：无新服务、无新表、无新列、0 AI、0 新依赖。新增 2 个 admin-gated API。

## Success Criteria

- [ ] `GET /api/compliance/audit-summary` 返回 总量 + byAction（busiest-first）+ byEntityType + 最近 20 条；非 admin→403、无 token→401。
- [ ] `GET /api/compliance/residual-permissions` 仅返回 停用且 ≥1 角色授权 的用户（含角色名）；启用用户/无角色停用用户不返回。
- [ ] `/api/compliance` 经 AdminPaths Tier A 门控（无 sibling-prefix 误伤）。
- [ ] CompliancePage 渲染 审计聚合 + 残留权限表；`/sys/compliance` 仅 admin 可达（navGuardConsistency 自动钉，/sys 前缀）。
- [ ] backend 全绿（442 baseline + 新增）+ frontend 全绿（167 baseline + 新增）+ E2E（真实审计聚合 + 残留对账 + 门控）+ 存量数据零改。
