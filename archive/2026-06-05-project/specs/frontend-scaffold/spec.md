# Capability: frontend-scaffold

## MODIFIED Requirements (from change 2026-06-05-project)

### Requirement: Sider「需求管理」组追加「项目」菜单项

前端 SHALL 在 AppLayout 左侧 Sider「需求管理」组的子项**第 1 位**（位于「诉求」之前）追加「项目」一项；点击跳转 `/pm/projects`；保留诉求/需求/诉求-需求关联 3 项不动。

#### Scenario: Sider「需求管理」组 4 项

- **GIVEN** 用户已登录访问 `/`
- **WHEN** 页面渲染完成
- **THEN** Sider「需求管理」组 SHALL 含 4 项：`"项目"`、`"诉求"`、`"需求"`、`"诉求-需求关联"`
- **AND** 项目项 SHALL 位于诉求项之前
- **AND** 点击「项目」SHALL 跳转 `/pm/projects`

### Requirement: /pm/projects 路由 + ProjectsPage CRUD + 默认 owner

前端 SHALL 在 `AppRoutes.tsx` 注册 `/pm/projects` 路由；ProjectsPage 提供列表 + 新建 + 编辑 + 删除；编辑抽屉「负责人」下拉新建时默认选中当前登录用户（按 loginName 匹配 listUsers 返回的池），编辑时回显 editing.ownerUserId 且**不 disabled**（可改）。

#### Scenario: /pm/projects 路由直接访问

- **GIVEN** 用户已登录
- **WHEN** 浏览器直接访问 `/pm/projects`
- **THEN** SHALL 渲染 `ProjectsPage` 组件
- **AND** `grep -c "/pm/projects" frontend/src/AppRoutes.tsx` SHALL ≥ 1（防止 linter 静默回退）

#### Scenario: ProjectsPage 新建抽屉默认 owner = 当前登录用户

- **GIVEN** Auth store 中 `user.username="alice"`；mock `listUsers` 返回包含 `{id:1, loginName:"alice"}`
- **WHEN** 用户点击「新建项目」打开抽屉，且 listUsers promise 已 resolve
- **THEN** 「负责人」下拉的当前选中值 SHALL 等于 `1`（alice 的 id）

#### Scenario: ProjectsPage 编辑抽屉 owner 可改

- **GIVEN** 抽屉打开为编辑模式，editing.ownerUserId=1；mock listUsers 返回 [{id:1, loginName:"alice"}, {id:2, loginName:"lili"}]
- **WHEN** 用户切换下拉到 lili (id=2) 并点保存
- **THEN** 「负责人」下拉控件 SHALL 不 disabled
- **AND** mock `updateProject` SHALL 被调用且参数 body.ownerUserId SHALL 等于 2

### Requirement: RequirementEditDrawer 与 UserRolesPage 的 projectId 控件改造

前端 SHALL 将 `RequirementEditDrawer` 与 `UserRolesPage` 的 projectId 输入控件从「数字输入框」改为「Project 下拉」（异步 listProjects）。UserRolesPage 的下拉保留「留白」选项（=公司级 hat，传 null）。RequirementsPage 与 UserRolesPage 列表 SHALL 新增「项目」列，render `projectName (projectCode)` 或 "—"。

#### Scenario: UserRolesPage 新建关联 项目留白 = 公司级 hat

- **GIVEN** mock `listProjects` 返回 1 条 project；用户和角色都已选
- **WHEN** 用户「项目」下拉**留白**并点保存
- **THEN** mock `createUserRole` SHALL 被调用且参数 body.projectId SHALL 为 `null`（不是 undefined，不是 0）
