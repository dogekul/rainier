# Test Report — fine-grained-permissions (B4, v0.0.77)

## Backend

```
cd backend && mvn test
Tests run: 681, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 新增/修改测试

- `PermissionServiceTest` — 8 cases，覆盖：
  - 有权限 / 无匹配角色 / 角色无该点
  - 禁用用户 → 无权限
  - null 入参短路
  - byUsername 解析
  - pointsOf 完整集合
  - 陈旧字符串常量被忽略（rename 容忍）

- `PermissionInterceptorTest` (@SpringBootTest) — 5 cases，flag ON：
  - admin + 有 AUDIT_VIEW → 200
  - admin 但无 AUDIT_VIEW → 403（关键：admin 身份不再是免死金牌）
  - 无 token → 401
  - 未声明 @RequiresPermission 的 admin 端点（/api/roles GET）→ admin 通过
  - 多点 ANY 默认 → 任一命中放行

- `LegacyProductCategoryCleanupTest` — 表数量 34 → 35（新增 `rainier_role_permission`）

## 兼容性
- 默认 test profile 关闭 `app.security.fine-grained-permissions.enabled`，~39 个 legacy admin 控制器测试零回归
- AdminAuthorizationTest / ComplianceAuthzTest 等已有 authz 测试全部通过

## Frontend
无前端变更。
