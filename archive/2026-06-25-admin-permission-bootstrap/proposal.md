# Proposal — admin-permission-bootstrap (G1, v0.0.105)

## 命门
B4 留下的 `app.security.fine-grained-permissions.enabled` flag 永远不敢打开 —
否则两个隐患让整批安全切片白做：

1. **admin 自锁**：所有 `adminAccess=true` 的 Role 没有任何 PermissionPoint 预绑，启 flag 后
   `@RequiresPermission` 端点（如 `/api/audit-logs`, `/api/compliance/audit-summary`）对 admin 直接 403。
2. **ROLE_MANAGE 漏网**：`AdminRolePermissionController`（角色↔权限点 M2M 维护端点）刻意没加
   `@RequiresPermission(ROLE_MANAGE)` — 因为「首次启用时没有任何 role 持 ROLE_MANAGE」会死锁。
   但这意味着启 flag 后，任何 admin token 持有人都能改全局权限点配置，
   仅靠 `AdminPaths` Tier A 路径前缀兜底，语义不洁。

G1 双修这两个问题，让 flag 默认 ON 不再死锁、不再裸奔。

## 范围
1. **NEW `AdminPermissionBootstrap`** `@Component CommandLineRunner`
   - flag `app.security.admin-permission-bootstrap.enabled`（默认 true，test=false）
   - 启动时查询所有 `adminAccess=true` 的 Role，给每个 role 预绑全部 `PermissionPoint` 枚举值
     (写入 `rainier_role_permission` 表)
   - 幂等：`existsByRoleIdAndPermissionPoint` 跳过已存在条目
   - 至少绑了一条新行时写一条 `AuditLog action=BOOTSTRAP_ADMIN_PERMISSIONS`
2. **修改 `AdminRolePermissionController`** 三个写方法（grant / revoke / list 的 admin 维护语义）
   - 给写方法加 `@RequiresPermission(PermissionPoint.ROLE_MANAGE)`
   - 因 G1 第 1 步已预绑，admin 启动后立即有 ROLE_MANAGE，不会死锁
3. **`application.yml`** 显式声明 `app.security.fine-grained-permissions.enabled=true`（与默认值对齐）
   + 新增 `admin-permission-bootstrap.enabled=true`
4. **`application-test.yml`** `admin-permission-bootstrap.enabled=false`
   （`fine-grained-permissions.enabled=false` 保留不变，承接 39+ legacy admin tests）

## OutOfScope
- 给 PMO/admin 之外的 role 配置任何 PermissionPoint（仍由 UI 手工分配）
- 移除 `AdminPaths` Tier A 双层兜底（保留 belt-and-suspenders）

## commit
`feat(admin-permission-bootstrap): G1 admin seed 预绑 PermissionPoint + Controller 补 @RequiresPermission (v0.0.105)`
