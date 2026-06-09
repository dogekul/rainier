# Capability: frontend-scaffold

## MODIFIED Requirements (v0.0.11)

### Requirement: Sider 含「需求管理」菜单组（v0.0.11 起 6 项，任务排第 3）

前端 SHALL 在 Sider 中渲染菜单组「需求管理」（位于「组织」之后）；展开后含 6 项（v0.0.11 起）：项目 / Sprint / **任务** / 诉求 / 需求 / 诉求-需求关联，任务项位于 Sprint 与 诉求 之间。

#### Scenario: Sider 含「需求管理」6 路由（v0.0.11）

- **GIVEN** 用户已登录访问 `/`
- **WHEN** 页面渲染完成
- **THEN** Sider SHALL 含菜单组 `"需求管理"`
- **AND** 该组展开后 SHALL 含 6 项：`"项目"`、`"Sprint"`、`"任务"`、`"诉求"`、`"需求"`、`"诉求-需求关联"`
- **AND** 「任务」 SHALL 位于「Sprint」之后、「诉求」之前
- **AND** 点击 `"任务"` SHALL 跳转 `/pm/tasks`

### Requirement: /pm/tasks 路由注册 + TasksPage（v0.0.11）

前端 SHALL 在 router 中注册 `/pm/tasks` 路由 → `TasksPage`；TasksPage SHALL 提供 list（含 filter projectId / status / priority / assigneeUserId / sprintId / storyId）+ 分页 + 新建按钮 + 每行编辑/删除。`TaskEditDrawer` SHALL 包含联动级联下拉：选 Project 后 Sprint/Story 下拉显示该 Project 范围内选项；Sprint 选定后 Story 下拉进一步过滤为该 Sprint 范围内 Story；Sprint/Story 均允许 unselected（空选项）。Assignee 下拉允许 unassigned（空选项）。

#### Scenario: /pm/tasks 路由直接访问

- **GIVEN** 用户已登录
- **WHEN** 浏览器直接访问 `/pm/tasks`
- **THEN** SHALL 渲染 `TasksPage` 组件
- **AND** `grep -c "/pm/tasks" frontend/src/AppRoutes.tsx` SHALL ≥ 1（防止 linter 静默回退）

#### Scenario: TaskEditDrawer Sprint/Story 联动 — Project 切换后清空 Sprint/Story 选择

- **GIVEN** TaskEditDrawer 打开为新建模式；mock listProjects / listSprints / listStories 返回若干数据；初始 Sprint id=20 已选
- **WHEN** 用户从 Project A 切换到 Project B
- **THEN** Sprint 下拉的当前选中值 SHALL 为空
- **AND** Story 下拉的当前选中值 SHALL 为空
- **AND** Sprint 下拉的可选项 SHALL 只含 `s.projectId === B.id` 的 Sprint
