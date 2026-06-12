# Capability: frontend-scaffold

> MODIFIED by `changes/2026-06-12-workbench` (v0.0.18-workbench, 2026-06-12):
> 占位 Home(`/`)换成「我的工作台」WorkbenchPage —— 当前用户的问候 + 角色 chips + 我的任务(含状态快改)
> / 我的 Story / 我的项目。`AuthUser` 扩展带 id/name/roles/projects。既有 Requirements 保留;此文件仅列本次 ADDED。

## ADDED Requirements (from change 2026-06-12-workbench / v0.0.18)

### Requirement: 我的工作台（替换占位 Home）

前端 SHALL 在 `/` 渲染 `WorkbenchPage`：挂载调 `GET /api/auth/me` 取当前用户上下文，展示问候(name 优先 username)、我的角色 chips、我的任务(assigneeUserId=我)、我的 Story(ownerUserId=我)、我的项目(来自 me.projects)。

#### Scenario: 渲染问候 + 角色 + 三块

- **GIVEN** `me()` 返回 `{id:5, username:"alice", name:"Alice", roles:[{roleName:"PMO", projectName:"采购"}], projects:[{id:9,code:"PRJ-1",name:"采购"}]}`
- **AND** `listTasks` 返回 1 个我的任务，`listStories` 返回 1 个我的 Story
- **WHEN** `/` 渲染完成
- **THEN** 页面 SHALL 显示问候含 "Alice"
- **AND** SHALL 显示角色 chip 含 "PMO"
- **AND** SHALL 显示「我的任务」「我的 Story」「我的项目」三区块的数据

#### Scenario: 我的任务/Story 携当前用户 id 查询

- **GIVEN** `me()` 返回 `id:5`
- **WHEN** WorkbenchPage 加载数据
- **THEN** SHALL 调用 `listTasks` 且 params 含 `assigneeUserId:5`
- **AND** SHALL 调用 `listStories` 且 params 含 `ownerUserId:5`

### Requirement: 我的任务状态快改

前端 SHALL 在「我的任务」每行提供状态下拉，选新状态即调用 `updateTask` 提交（复用任务的现有字段 + 新 status），并刷新列表。

#### Scenario: 改任务状态触发 updateTask

- **GIVEN** WorkbenchPage 我的任务列出任务 id=11（status="TODO"）
- **WHEN** 用户在该任务的状态下拉选择 "IN_PROGRESS"
- **THEN** SHALL 调用 `updateTask` 且第一参为 `11`、body.status 为 "IN_PROGRESS"

### Requirement: 工作台条目可跳转（Gate 3 反馈）

前端 SHALL 把 WorkbenchPage 的「我的任务/我的 Story/我的项目」条目渲染为链接 —— 任务 → `/pm/tasks`、Story → `/pm/sprints`、项目 → `/pm/projects`。

#### Scenario: 项目条目链接到项目页

- **GIVEN** WorkbenchPage 我的项目列出项目 id=9
- **WHEN** 页面渲染完成
- **THEN** 该项目条目 SHALL 是链接，href 指向 `/pm/projects`

### Requirement: Sider 导航壳增强（工作台入口 + 折叠，Gate 3 反馈）

前端 SHALL 在 Sider 顶部提供「工作台」菜单组（含「我的工作台」→ `/`）；顶部品牌「Rainier」SHALL 链接到 `/`；每个菜单组标题 SHALL 可点击折叠/展开其子项；并 SHALL 提供一个开关收起/展开整个 Sider。

#### Scenario: 工作台菜单组居首且链接到根

- **GIVEN** 用户已登录访问任意受保护页
- **WHEN** Sider 渲染
- **THEN** Sider 首组 SHALL 为「工作台」，含「我的工作台」链接 href=`/`
- **AND** 品牌「Rainier」SHALL 为 href=`/` 的链接

#### Scenario: 点击组标题折叠该组

- **GIVEN** Sider 已展开「系统」组（含「审计日志」）
- **WHEN** 用户点击「系统」组标题
- **THEN** 「审计日志」项 SHALL 从 DOM 移除（折叠）
- **AND** 再次点击组标题 SHALL 恢复显示

#### Scenario: 收起整个 Sider

- **GIVEN** Sider 可见
- **WHEN** 用户点击顶部 Sider 开关
- **THEN** 整个 Sider SHALL 从 DOM 移除
- **AND** 再次点击 SHALL 恢复显示
