# Pending Adjustments — v0.0.18-workbench

## PA-1 (Build) — AuthUser 携完整上下文（store 缓存 roles/projects）

design Decision 4 述 AuthUser 扩展。实现把 id/name/roles/projects **全部**纳入 AuthUser（均可选，username 必填），WorkbenchPage 挂载 me() 后 `setAuth` 写入完整上下文。login 仍只 set `{username}`，既有读 `user.username` 的调用点（AppLayout/Login）不破。

## PA-2 (Build) — 占位 Home **删除**（非原地改），新 pages/Workbench

WorkbenchPage 放 `pages/Workbench/`（含 index.tsx 默认导出），AppRoutes `/` 指向它；`pages/Home/index.tsx` 删除（git rm）。既有 `ProtectedRoute.test` 引用 `home-greeting`/"Hello, alice" → 迁移为 `workbench-greeting` + mock me()（避免渲染受保护页时打真实 me 网络）。

## PA-3 (Build) — TC-ME-004 复用既有测试

「无 token → 401」由既有 `AuthControllerMeTest.getMe_withoutAuthorizationHeader_returns401Json` 覆盖（401 在 SecurityFilter 层先于 MeService 触发，不受本版影响），不重复写。

## PA-4 (Verify Step 0 Code-M1+M2) — WorkbenchPage 异步错误兜底

评审发现 `me()` 与 `changeTaskStatus` 无 catch → 非-401 错误产生 unhandled rejection（且测试栈打印 act 警告）。修复：me() effect 改 try/catch 包裹（401 由 interceptor 重定向，其它错误留空态不崩）；changeTaskStatus 包 try/catch + 失败后 `loadWork` 重同步（status select 受控于服务端真值，自动回正）。

## PA-5 (Verify Step 0 Docs-H/Test-M) — test-plan DOING→IN_PROGRESS

test-plan TC-FES-WB-003 写 status `"DOING"`（非法 —— TaskStatus 无此值；spec scenario 与实际测试均用 `IN_PROGRESS`）。修正 test-plan 为 `IN_PROGRESS`，三处一致。

## PA-6 (Verify Step 0 Test-M2 + L) — 补覆盖

- 加 TC-ME-005：同一项目两个角色 → roles=2 / projects=1（覆盖 MeService 项目去重分支）。
- StoryOwnerFilter 加负路径断言：省略 ownerUserId → total=3（锁定过滤可选性）。
- ProtectedRoute 受保护页测试改 async + `waitFor(workbench-greeting=Alice)`：包裹 me() 解析后的状态更新，消除 act 警告。

## 未修复（评审 L，阈值内）

- WorkbenchPage 空/降级态（id=null → 不发 loadWork + 各「暂无…」文案）无独立前端测试 —— ProtectedRoute 测试已挂载 id=null 路径，E2E 对 'system' 类无上下文降级已验，价值低不补。
- WorkbenchPage useEffect 依赖 token：若未来 setAuth 轮换 token 会触发 me() 重取（当前 setAuth 不动 token，安全）—— 潜在脆弱，记录不改。
- roleId 孤儿（无匹配 Role）渲染 '?' 无日志 —— 数据完整性 smell，非本版关注。
