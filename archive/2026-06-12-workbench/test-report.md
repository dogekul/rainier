# v0.0.18-workbench 测试报告

> 测试日期：2026-06-12 ｜ 环境：macOS；Java 8(docker)；JUnit5+MockMvc+H2；Vitest 2.1.9+RTL；MySQL 8(docker E2E)
> 被测版本：基线 v0.0.17-milestone / commit ac280fd（本变更未提交）

## 一、总体概况

| 指标 | 数值 |
|------|------|
| 后端用例总数 | 341（336 baseline + 5 new） |
| 后端通过 | 341 |
| 前端用例总数 | 73（66 baseline + 7 new：WorkbenchPage 3 + AppLayout 导航壳 4） |
| 前端通过 | 73 |
| 通过率 | 100% |

### 1.1 覆盖率（变更文件）

| 文件 | 覆盖 | 状态 |
|------|------|------|
| MeService / MeResponse | TC-ME-001..003,005（含项目级/组织级/降级/去重） | ✅ |
| AuthController.me | TC-ME-* + 既有 AuthControllerMeTest（401 路径） | ✅ |
| StoryService/Controller (ownerUserId) | TC-STORY-OWN-001（含负路径） | ✅ |
| api/auth, api/story, store/auth | 经 WorkbenchPage/ProtectedRoute 测试间接覆盖 | ✅ |
| WorkbenchPage.tsx | TC-FES-WB-001..003 + ProtectedRoute 挂载 | ✅ |

## 二、按模块统计

| 模块 | 用例 | 通过 | 说明 |
|------|------|------|------|
| AuthMeContextTest | 4 | 4 | 项目级/组织级 null/降级/同项目去重 |
| StoryOwnerFilterTest | 1 | 1 | owner 过滤 + 省略=全量 负路径 |
| AuthControllerMeTest（既有） | 5 | 5 | 401 路径不变（degrade 保持 username） |
| WorkbenchPage.test | 3 | 3 | 渲染问候/角色/三块 + 携 id 查询 + 状态快改（条目含 Link，包 MemoryRouter） |
| AppLayout.test（Gate 3 +4） | 10 | 10 | 既有 6 组断言 + 工作台组/品牌链接/组折叠/Sider 收起 |
| ProtectedRoute.test（改） | 2 | 2 | 未登录→/login；已登录→受保护工作台（act 已包裹） |
| 后端全量回归 | 341 | 341 | 无回归（含 me 扩字段 / story 加 param / 既有 me 测试） |

## 三、E2E（docker，仅 rebuild backend+frontend，MySQL 卷保留）

| 路径 | 状态 | 说明 |
|------|------|------|
| login alice → token | ✅ | 131 字符 JWT |
| GET /me 上下文 | ✅ | `{id:1, username:alice, name:Alice, roles:2(含组织级 projectId=null + 项目级 PMO@E2E test project), projects:[{id:1,code:PROJ-E2E-001}]}`；项目去重生效 |
| GET /me 无 token | ✅ | 401 |
| story owner 过滤 | ✅ | 既有 2 story(owner 1,2)；`?ownerUserId=1` → total 1 全 owner 1 |
| 前端 up | ✅ | HTTP 200（WorkbenchPage 上线） |
| standing | ✅ | 19 表不变；存量 3 项目（CASUAL）未改 |
| Gate 3 反馈：导航壳 | ✅ | 前端重建后浏览器实测：工作台菜单组/品牌链接/组折叠/Sider 收起/工作台条目可跳转 6 项全生效；系统组(审计日志)在 DOM 中确认存在 |

**结论**：运行时验证通过 —— me() 真返回含组织级+项目级角色与去重项目的完整上下文；story owner 过滤生效；存量零改动。

## 五-B、多路并行 Review（Step 0）

| 维度 | C | H | M | L |
|------|---|---|---|---|
| 代码质量 | 0 | 0 | 2 | 4 |
| 测试/配置 | 0 | 0 | 2 | 4 |
| 文档/Skills | 0 | 1 | 0 | 2 |

阈值内（C=0 / H≤3 / M≤10）。已修：

| # | 级 | 问题 | 状态 |
|---|----|------|------|
| Code-M1 | M | WorkbenchPage me() 无 catch → unhandled rejection + act 警告 | ✅ try/catch 包裹 |
| Code-M2 | M | changeTaskStatus 失败无兜底 | ✅ try/catch + loadWork 重同步 |
| Docs-H / Test-M1 | H/M | test-plan TC-FES-WB-003 "DOING"（非法状态） | ✅ 改 IN_PROGRESS（三处一致） |
| Test-M2 | M | MeService 同项目去重分支无覆盖 | ✅ 加 TC-ME-005 |
| Test-L / Story | L | story 过滤无负路径 | ✅ 加 省略=全量 断言 |
| Test-L / ProtectedRoute | L | act 警告（未 await me） | ✅ async + waitFor |

未修（L，阈值内）：WorkbenchPage 空/降级态无独立前端测试（ProtectedRoute 已挂 id=null + E2E 验 'system' 降级）；useEffect token 依赖潜在脆弱；roleId 孤儿渲染 '?' 无日志。

## 六、设计调整

3 项 Minor（异步错误兜底 / Home 删除而非原地改 / test-plan 状态值+补覆盖）。详见 design-adjustments.md + pending-adjustments.md。

## 八、结论

可交付。本版是「按角色使用逻辑完善功能」的第一步：当前用户上下文（me 富化）地基 + 一线「我的工作台」。纯实体聚合，0 新表/0 新依赖/0 AI。me() 降级路径、组织级角色 null、项目去重、N+1 批量富化均覆盖；standing 由 before/after MySQL 快照证明。无 C/H 代码问题，M 全修。

### 8.1 质量信号

| 信号 | 状态 | 备注 |
|------|------|------|
| 单元/集成 | ✅ | 后端 341/341 + 前端 73/73 |
| E2E | ✅ | me 上下文 + story 过滤 + 前端上线 + 存量不变，19 表 |
| Lint (checkstyle+eslint) | ✅ | 0 违规 |
| tsc | ✅ | clean |
| 覆盖率 | N/A | 变更文件全覆盖 |
| 十一类失败模式 | ✅ | 命中 0 |
