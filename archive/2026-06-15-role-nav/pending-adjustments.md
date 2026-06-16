# Pending Adjustments — v0.0.20-role-nav

## Step 0 三代理评审结果（C:0 H:0 M:2 L:多）

阈值 C=0 / H≤3 / M≤10 全部满足。以下为采纳修复（M 级 + 高价值 L 级）。

### PA-1 (code-M1 / test-M1): nav 与 guard 双源真相 → 一致性测试 + /pm/products 守卫测试
- 发现：`ADMIN_PATH_PREFIXES`(ProtectedRoute) 与 `requiresAdmin` 组(AppLayout) 是两套独立真相，未来可能漂移成守卫空洞；
  且 guard 测试未覆盖 `/pm/products`（正是前缀列表存在的理由）。
- 修复：导出 `navGroups`(AppLayout) + `isAdminPath`(ProtectedRoute)；新增 `navGuardConsistency.test.tsx`
  断言「每个 requiresAdmin 组的每个 item.to → isAdminPath=true；每个全员组 item.to → isAdminPath=false」；
  并在 guard 测试加 RN-003b：非管理员 `/pm/products` → 跳回 /。

### PA-2 (test-M1): RN-004/005 的 `wb-stub` 断言是重言式
- 发现：`/hr/roles`、`/pm/projects` 下 `/` 路由未挂载，`wb-stub` 恒不存在，删了守卫也通过。真正承重的是
  `getByTestId('roles-stub'/'projects-stub')`。
- 修复：移除重言 `wb-stub` 断言，保留承重的页面 stub 断言。

### PA-3 (test-L1): ADM-001/002 未独立校验持久化列（spec AND 子句）
- 修复：ADM-001 POST 后加 `repo.findById(id).getAdminAccess()==false` 断言；ADM-002 同理加 true 断言。

### PA-4 (test-L2): RN-008 仅覆盖 create 路径
- 修复：RolesPage 测试加 update 分支（编辑既有角色勾选 → updateRole body.adminAccess=true）。

### PA-5 (docs-L1/L3): 「保留 me() fallback」陈述与实现矛盾
- 发现：design.md/slices.md/tasks.md 写「保留 me() fallback」，但 WorkbenchPage 实际完全移除了 me() 调用
  （这反而让 proposal「应用内只有 ProtectedRoute 调一次 me()」严格成立）。
- 修复：从三处文档删除「保留 me() fallback」措辞。

### 未修复（记录在案，可接受）
- code-L1（Java 字段初始值 cosmetic）：无害，保留作意图文档。
- code-L2 / test-L4（me() 非 401 失败 → 视为非管理员；E2E-only schema 安全）：有意 fail-safe + 已由 ADM-004 + E2E 兜底。
- test-L3（store 跨文件隔离靠 per-file beforeEach）：今日 benign，不引入全局 reset 以免动既有约定。
