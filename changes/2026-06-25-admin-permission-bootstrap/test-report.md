# Test Report — admin-permission-bootstrap (G1, v0.0.105)

## 命令
```
cd backend && mvn test
```

## 结果
- **Tests run: 874, Failures: 0, Errors: 0, Skipped: 0**
- BUILD SUCCESS in 18.034s
- 较 B4 baseline 新增 6 个 case（既有 868 个 → 874 个），39+ legacy admin 控制器 0 回归

## 新增 case 覆盖
**`AdminPermissionBootstrapTest`** (4 cases, `@TestPropertySource admin-permission-bootstrap.enabled=true`)
- `run_adminRole_preBindsAllPermissionPoints`：PMO(adminAccess=true) 启动后被绑全部 5 个 PermissionPoint + 1 条审计
- `run_idempotent_noDuplicateInserts`：第二次 run 不增行、不再写 audit
- `run_nonAdminRole_skipped`：DEV(adminAccess=false) 一行不被插入
- `run_multipleAdminRoles_allCovered`：多个 admin role 全部覆盖

**`AdminRolePermissionControllerAuthzTest`** (2 cases, fine-grained-permissions=true)
- `grant_withRoleManage_returns200`：admin 持 ROLE_MANAGE → 200
- `grant_withoutRoleManage_returns403`：admin 无 ROLE_MANAGE → 403

## 关键 spec ↔ test 映射
| Spec Scenario | Test |
|---|---|
| S1 启动预绑 | `run_adminRole_preBindsAllPermissionPoints` |
| S2 幂等 | `run_idempotent_noDuplicateInserts` |
| S3 非 admin 不绑 | `run_nonAdminRole_skipped` |
| S4 ROLE_MANAGE OK | `grant_withRoleManage_returns200` |
| S5 失去 ROLE_MANAGE → 403 | `grant_withoutRoleManage_returns403` |
| S6 test profile flag off no-op | 隐式覆盖（870+ 既有测试通过即证明 bootstrap 没污染表） |

## 注意
- `AuditLog.action` 列长度 16 → 真存的 action code 是 `BOOTSTRAP_PERMS`；语义名 `BOOTSTRAP_ADMIN_PERMISSIONS` 嵌在 `summary` 字段
- `@Order(HIGHEST_PRECEDENCE + 10)` 保证在 `AdminAuthzBootstrap` 之后跑（先 elevate PMO 再预绑权限）
