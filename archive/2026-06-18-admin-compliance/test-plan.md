# Test Plan — v0.0.41-admin-compliance

> Baseline backend 442 green / frontend 167 green. New TCs below; all P0.

## 测试策略

- 后端功能测试（admin-authz off 默认 test profile）= 审计聚合 + 残留对账。
- 后端门控测试（@TestPropertySource admin-authz=true）= 401/403/200 双端点。
- 前端组件（Vitest，mock api/compliance）= CompliancePage 渲染/空态；AppRoutes /sys/compliance；navGuardConsistency 自动。
- E2E（Docker 真 MySQL，admin-authz on）= admin token 审计聚合 + 残留对账 + 非 admin 403 + 存量零改。

## 详细测试案例

### admin-compliance 功能（后端，ComplianceControllerTest）

| TC-ID | Scenario | 预期 |
|---|---|---|
| TC-COMP-001 | audit-summary 总量 + byAction 最频在前 | total=5；byAction[0]={CREATE,3}；recent size=5 |
| TC-COMP-002 | byEntityType 聚合 | byEntityType[0]={TASK,2} |
| TC-COMP-003 | 空审计表 | total=0；byAction/recent 空 |
| TC-COMP-004 | residual 仅停用且有角色 | size=1；ghost；roleCount=1；roleNames=[DEV] |
| TC-COMP-005 | 无残留 | 空数组 |

### admin 门控（后端，ComplianceAuthzTest，admin-authz on）

| TC-ID | Scenario | 预期 |
|---|---|---|
| TC-COMP-AUTHZ-001 | 无 token audit-summary | 401 |
| TC-COMP-AUTHZ-002 | 非 admin audit-summary | 403 |
| TC-COMP-AUTHZ-003 | admin audit-summary | 200 |
| TC-COMP-AUTHZ-004 | 非 admin residual | 403 |
| TC-COMP-AUTHZ-005 | admin residual | 200 |

### frontend-scaffold（前端）

| TC-ID | Scenario | 预期 |
|---|---|---|
| TC-COMPP-01 | 渲染总量 + 残留表 + 按动作 + 最近 | compliance-summary 含 5、残留行 ghost/DEV、by-action CREATE、recent 行 |
| TC-COMPP-02 | 无残留 → 空态 | compliance-residual-empty 可见 |
| TC-FES-COMP-01 | /sys/compliance 路由挂载 CompliancePage | compliance-summary 可见 |
| TC-FES-COMP-02 | AppRoutes.tsx 含 /sys/compliance literal | grep ≥1 |
| TC-FES-COMP-03 | isAdminPath('/sys/compliance')===true（navGuardConsistency 自动） | admin |

### E2E

| TC-ID | Scenario | 预期 |
|---|---|---|
| TC-E2E-COMP-001 | admin token → audit-summary 返回 total/byAction/recent | 链路通 |
| TC-E2E-COMP-002 | admin token → residual-permissions 返回（list） | 200 |
| TC-E2E-COMP-003 | 非 admin → 403；无 token → 401 | 门控生效 |
| TC-E2E-COMP-004 | 存量业务数据不变（纯读） | 数据零改 |

## 回归风险矩阵

| 区域 | 风险 | 缓解 |
|---|---|---|
| AdminPaths +/api/compliance | 🟡中 | base-or-prefix matches；AdminAuthorizationTest sibling 边界回归；新门控测试 |
| AuditLogRepository GROUP BY | 🟢低 | 纯新增 @Query；H2 + MySQL E2E |
| UserRepository findByEnabledFalse | 🟢低 | 派生查询，尊重 @Where del_flag=0 |
| 前端导航守卫 | 🟢低 | /sys 已门控；navGuardConsistency 自动钉 /sys/compliance admin |
