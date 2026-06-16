# Design Adjustments — v0.0.20-role-nav

Phase 4-5 期间相对 Phase 2 设计的调整。均为小调整（不改接口/行为语义），已在 Phase 5 自动处理。

## AD-1 (来自 Step 0 评审 PA-1): nav 与 guard 单一真相 + 一致性测试
- **原设计**：`ADMIN_PATH_PREFIXES`(ProtectedRoute) 与 `requiresAdmin` 组(AppLayout) 各自独立定义。
- **调整**：导出 `navGroups`(AppLayout) 与 `isAdminPath`(ProtectedRoute)，新增 `navGuardConsistency.test.tsx`
  机械断言两者一致（每个 requiresAdmin 组的 item.to → 必被守卫；每个全员组 item.to → 必不被守卫，17 条）。
  另加 TC-FES-RN-003b：非管理员 `/pm/products` → 跳回 /（钉死前缀冲突这个最棘手的 case）。
- **原因**：防止未来加 admin 导航项时守卫遗漏导致前端越权可见。纯增强，不改运行时行为。

## AD-2 (来自 Step 0 评审 PA-2): 移除 RN-004/005 的重言断言
- **原设计**：guard 测试断言 `queryByTestId('wb-stub')).toBeNull()`。
- **调整**：移除（`/` 路由在该 case 未挂载，恒为 null，删守卫也通过）；保留承重的 `getByTestId('roles-stub'/'projects-stub')`。

## AD-3 (来自 Step 0 评审 PA-3): ADM-001/002 校验持久化列
- **原设计**：仅断言 POST 响应 `$.adminAccess`。
- **调整**：补 `repo.findAll().get(0).getAdminAccess()` 断言（spec entity-role 的 AND 子句「persisted row SHALL read」）。

## AD-4 (来自 Step 0 评审 PA-4): RolesPage update 路径覆盖
- **调整**：新增 TC-FES-RN-008b（编辑既有 admin 角色 → 复选框预置勾选 → updateRole body.adminAccess=true）。

## AD-5 (来自 Step 0 评审 PA-5): 文档「保留 me() fallback」陈述纠正
- **原设计**：design.md/slices.md/tasks.md 写 WorkbenchPage「保留 me() fallback」。
- **调整**：实现完全移除 WorkbenchPage 的 me() 调用（让「应用内只有 ProtectedRoute 调一次 me()」严格成立）；
  三处文档措辞同步纠正为「不再调 me()」。

## 未调整（评审记录但有意保留）
- code-L1 Java 字段初始值 `=Boolean.FALSE` cosmetic（保留作意图文档，NULL 防御由 getter 兜底）。
- code-L2 me() 非 401 失败 → 视为非管理员（有意 fail-safe/closed）。
- test-L3 store 跨文件隔离靠 per-file beforeEach（今日 benign，不引入全局 reset 以免动既有约定）。
