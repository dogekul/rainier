# 设计调整说明 — v0.0.18-workbench

> 基线：Phase 2 design.md + specs + test-plan.md ｜ 来源：Phase 3-5 实现与评审

## 调整汇总

| # | 类型 | 文档 | 严重 | 阶段 | 用户已知 |
|---|------|------|------|------|---------|
| 1 | 实现健壮性（异步兜底） | design Decision 5 | Minor | Phase 5 (Review Code-M1/M2) | 是 |
| 2 | 占位 Home 删除而非原地改 | design Decision 5 | Minor | Phase 4 | 是 |
| 3 | test-plan 状态值修正 + 补覆盖 | test-plan | Minor | Phase 5 | 是 |
| 4 | Gate 3 反馈：Sider 导航壳增强 + 工作台可跳转 | frontend-scaffold | Minor | Gate 3 → 回 Build | 是（用户提出） |

## 详细

### 调整 1: WorkbenchPage 异步调用加错误兜底

- **原始**：design Decision 5 述「挂载调 me() → 渲染」，未规定错误处理。
- **调整**：me() effect 与 changeTaskStatus 加 try/catch（401 由 axios interceptor 处理；其它错误留空态/重同步，不崩、不产生 unhandled rejection）。
- **原因**：评审 Code-M1/M2 —— 缺 catch 在非-401 路径产生 unhandled rejection 并触发测试 act 警告。
- **影响**：WorkbenchPage.tsx；行为更健壮，渲染契约不变。

### 调整 2: 占位 Home 删除，新 pages/Workbench

- **原始**：design 述「Home → WorkbenchPage」。
- **调整**：新建 `pages/Workbench/`（WorkbenchPage + index 默认导出），AppRoutes `/` 改指它，`pages/Home/index.tsx` 删除；`ProtectedRoute.test` 的 `home-greeting` 断言迁移为 `workbench-greeting` + mock me()。
- **原因**：避免遗留死代码（占位 Home 无复用价值）。
- **影响**：AppRoutes.tsx / ProtectedRoute.test.tsx / 删 pages/Home。

### 调整 3: test-plan 状态值 + 补覆盖

- **原始**：test-plan TC-FES-WB-003 写 status "DOING"。
- **调整**：改 "IN_PROGRESS"（合法 TaskStatus；spec 与测试本就用它）；并补 TC-ME-005（项目去重）+ StoryOwnerFilter 负路径 + ProtectedRoute act-await。
- **原因**：评审 Docs-H/Test-M —— 文档非法状态值 + 去重分支无覆盖。
- **影响**：test-plan.md / AuthMeContextTest / StoryOwnerFilterTest / ProtectedRoute.test。

### 调整 4: Gate 3 反馈 —— Sider 导航壳增强 + 工作台可跳转

- **原始**：design 只把占位 Home 换成 WorkbenchPage，未动 AppLayout 导航壳。
- **反馈**：用户 Gate 3 手动测试时反馈「工作台页没有菜单可以点」—— 工作台落地后与导航脱节（无工作台菜单项、品牌非链接、条目不可点）。
- **调整**（用户确认 1-6 全做）：AppLayout 加「工作台」菜单组（我的工作台→/，NavLink `end`）+ 品牌「Rainier」改 `<Link to="/">` + 菜单组标题改可折叠按钮（caret 旋转）+ 顶部加 Sider 收起开关 + WorkbenchPage 条目改 `<Link>`（任务/Story/项目 → 对应列表页）+ CSS 样式优化。
- **影响**：AppLayout.tsx / AppLayout.css / WorkbenchPage.tsx + 测试（AppLayout.test +4 TC-FES-WB-101..104；WorkbenchPage.test 包 MemoryRouter）。前端 69→73。docker 前端重建 + 浏览器实测确认 6 项全生效。
- **阶段**：Gate 3 → 回 Build → 重新 Verify。

