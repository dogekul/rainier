# Spec — admin-permission-bootstrap (G1, v0.0.105)

## Scenario 1: 启动时 admin role 被预绑全部 PermissionPoint
- Given 系统中存在 Role `PMO`（adminAccess=true）且 `rainier_role_permission` 表无相关行
- And `app.security.admin-permission-bootstrap.enabled=true`
- When 应用启动 `AdminPermissionBootstrap.run()`
- Then `rainier_role_permission` 表中存在 PMO role 对全部 `PermissionPoint`
  枚举值（`USER_MANAGE`/`ROLE_MANAGE`/`AUDIT_VIEW`/`COMPLIANCE_VIEW`/`PROJECT_ADMIN`）的绑定
- And 写入一条 `AuditLog action=BOOTSTRAP_ADMIN_PERMISSIONS` 总结新增点位数

## Scenario 2: 幂等 — 重启不会重复插入
- Given Scenario 1 已执行完
- When `AdminPermissionBootstrap.run()` 再次被调用
- Then 不抛异常
- And `rainier_role_permission` 表行数不变（`existsByRoleIdAndPermissionPoint` 全部命中跳过）
- And 不会再写新的 BOOTSTRAP_ADMIN_PERMISSIONS audit row

## Scenario 3: 非 admin role 不被绑权限
- Given Role `DEV`（adminAccess=false）+ Role `PMO`（adminAccess=true）共存
- When bootstrap 运行
- Then `DEV` role 在 `rainier_role_permission` 表中无任何行
- And `PMO` role 被绑全部 PermissionPoint

## Scenario 4: AdminRolePermissionController.grant 由 ROLE_MANAGE 守卫
- Given fine-grained-permissions=true + admin-permission-bootstrap=true
- And alice 持 admin role 且经 bootstrap 后拥有 ROLE_MANAGE
- When alice 调 `POST /api/admin/roles/{id}/permissions`
- Then 返回 200

## Scenario 5: 失去 ROLE_MANAGE 后无法维护权限
- Given fine-grained-permissions=true
- And bob 持有 admin role 但已手工删除该 role 的 ROLE_MANAGE 绑定
- When bob 调 `POST /api/admin/roles/{id}/permissions`
- Then 返回 403（@RequiresPermission 拦截）

## Scenario 6: bootstrap flag 关闭时 no-op
- Given `app.security.admin-permission-bootstrap.enabled=false`（test profile 默认）
- When 系统启动
- Then `rainier_role_permission` 表无任何 BOOTSTRAP 行
- And 39+ legacy admin tests 不受影响（fine-grained-permissions=false 也保留）
