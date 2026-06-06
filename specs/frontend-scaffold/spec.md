# Capability: frontend-scaffold

> Change log (recent):
> - 2026-06-05 (v0.0.6-demand-requirement) — added 需求管理 menu group + `/pm/*` routes.
> - 2026-06-05 (v0.0.7-position-role) — added 人事配置 menu group + `/hr/*` routes.
> - 2026-06-07 (v0.0.8-project) — added 项目 menu item (first position in 需求管理) + `/pm/projects` route + ProjectsPage; converted projectId numeric input → Project dropdown in RequirementEditDrawer and UserRolesPage; added 项目 column to Requirement / UserRole lists; `/pm` redirect now points at `/pm/projects` (was `/pm/demands`).

## Requirements

### Requirement: 路由守卫保护需登录页面

前端 SHALL 在未登录时阻止访问受保护路由，并将用户重定向至 `/login`。

#### Scenario: 未登录访问首页时重定向

- **GIVEN** 浏览器 localStorage 不含有效 `rainier.token`
- **WHEN** 用户访问 `/`
- **THEN** 系统 SHALL 渲染 `/login` 页面
- **AND** 浏览器地址栏 SHALL 为 `/login`

#### Scenario: 登录后访问受保护路由通过

- **GIVEN** localStorage 中存在有效 `rainier.token` 且 Zustand store 中 `user.username` 为 `alice`
- **WHEN** 用户访问 `/`
- **THEN** 系统 SHALL 渲染首页（Home 组件）
- **AND** 页面 SHALL 在右上角显示文本 `alice`
- **AND** 页面主区域 SHALL 显示文本 `Hello, alice`

### Requirement: 应用飞书风格全局主题

前端 SHALL 在 `:root` 注入飞书风格 design tokens（CSS variables），所有按钮 / 输入框 / 卡片组件 SHALL 引用 tokens 而非硬编码颜色。

#### Scenario: 主题 tokens 在 DOM 上可见

- **GIVEN** 前端应用已挂载到 `#root`
- **WHEN** 测试代码读取 `document.documentElement` 的 computed style
- **THEN** 系统 SHALL 暴露 CSS 变量 `--rainier-color-primary` 值为 `#3370FF`
- **AND** 系统 SHALL 暴露 `--rainier-radius-button` 值为 `6px`
- **AND** 系统 SHALL 暴露 `--rainier-radius-card` 值为 `8px`

#### Scenario: 登录按钮使用主色

- **GIVEN** 用户位于 `/login` 页面
- **WHEN** 测试通过 `getComputedStyle` 读取登录按钮的 `background-color`
- **THEN** 系统 SHALL 返回与主色 `#3370FF` 等价的颜色（`rgb(51, 112, 255)`）

---

<!-- Appended from change 2026-06-04-org-tree-and-employee -->


## MODIFIED Requirements

### Requirement: 左侧导航 Sider

前端 SHALL 在登录后页面（AppLayout 下）显示左侧 Sider 导航。

#### Scenario: Sider 含组织菜单组

- **GIVEN** 用户已登录访问 `/`
- **WHEN** 页面渲染完成
- **THEN** 左侧 Sider SHALL 渲染
- **AND** Sider SHALL 含菜单组 "组织"
- **AND** 该组展开后 SHALL 含 3 项：组织节点 / 用户 / 用户-组织关系
- **AND** 点击 "组织节点" SHALL 跳转 `/org/organizations`

### Requirement: 通用 CRUD 组件

前端 SHALL 在 `components/ui/` 提供 `Table` / `Pagination` / `Drawer` / `ConfirmDialog` / `TreeSelect` 5 个通用组件。

#### Scenario: Table 渲染 columns + rows

- **GIVEN** 测试 mount `<Table columns={[{key:'name',title:'名称',render:(r)=>r.name}]} dataSource={[{id:'1',name:'A'}]} rowKey="id" />`
- **WHEN** 渲染完成
- **THEN** SHALL 渲染 `<table>` 元素
- **AND** 表头 SHALL 含文本 "名称"
- **AND** 表体 SHALL 渲染 1 行，含文本 "A"

#### Scenario: TreeSelect 父节点选择器

- **GIVEN** 用户在 organizations 新建表单
- **WHEN** TreeSelect 被点击且 `GET /api/organizations/tree` 返回 3 节点（含层级）
- **THEN** SHALL 弹出树形面板
- **AND** 面板 SHALL 渲染 3 个节点，按 parent_id 嵌套
- **AND** 选择一个节点后 SHALL 关闭面板并填入 `parentId`



## MODIFIED Requirements (from change 2026-06-05-v1-id-migration)

### Requirement: 前端 TS 接口 id 字段统一为 number

前端 `api/*.ts` 中所有实体类型 SHALL 用 `number` 表达 id 类字段；TreeSelect 与 usePaginated 等通用组件类型跟随。

#### Scenario: TypeScript 类型契约

- **GIVEN** `frontend/` 工作目录已 `npm ci`
- **WHEN** 执行 `npm run build`
- **THEN** `tsc -b` 阶段 SHALL 退出码 0
- **AND** vite build 阶段 SHALL 退出码 0
- **AND** `frontend/src/api/{organization,user,userOrganization}.ts` 中所有 `id` / `parentId` / `userId` / `organizationId` 字段类型 SHALL 为 `number` 或 `number | null`
- **AND** `frontend/src/components/ui/TreeSelect.tsx` 中 `TreeNode.id` / `parentId` 字段类型 SHALL 为 `number` / `number | null`



## MODIFIED Requirements (from change 2026-06-05-remove-org-pmo)

### Requirement: 组织编辑抽屉与列表不再渲染 PMO 控件

前端 `OrganizationsPage.tsx` 列表 SHALL 不渲染 "PMO" 列；`EditDrawer.tsx` 抽屉表单 SHALL 不渲染 PMO 复选框 + label `PMO 团队`；`api/organization.ts` 中 `Organization` / `OrganizationCreate` / `OrganizationUpdate` 类型 SHALL 不含 `isPmo` 字段。

#### Scenario: EditDrawer 渲染时无 PMO 复选框

- **GIVEN** 测试 mount `<OrganizationEditDrawer open={true} editing={null} onClose={...} onSubmit={...} />`
- **WHEN** 渲染完成且初次 useEffect 已跑（mock `getOrganizationTree` 返回 `[]`）
- **THEN** `screen.queryByLabelText('PMO 团队')` SHALL 为 `null`
- **AND** `screen.queryByText('PMO 团队')` SHALL 为 `null`
- **AND** 抽屉中可见的 label SHALL 仅包含：父节点 / 类型 / 编码 / 名称 / 描述 / 启用

#### Scenario: OrganizationsPage 列表表头无 PMO 列

- **GIVEN** 测试 mount `<OrganizationsPage />`，mock `listOrganizations` 返回 1 条数据
- **WHEN** 渲染完成
- **THEN** `screen.queryAllByRole('columnheader')` 文本数组 SHALL 不含 `PMO`
- **AND** 表头 SHALL 仅含：编码 / 名称 / 类型 / 全路径 / 操作

#### Scenario: TypeScript 类型契约 — Organization 类型无 isPmo

- **GIVEN** `frontend/` 工作目录已 `npm ci`
- **WHEN** 执行 `npm run build`
- **THEN** `tsc -b` 阶段 SHALL 退出码 0
- **AND** `frontend/src/api/organization.ts` 中 `interface Organization` SHALL 不含 `isPmo` 字段
- **AND** `interface OrganizationCreate` 与 `interface OrganizationUpdate` SHALL 不含 `isPmo` 字段
- **AND** `grep -n 'isPmo' frontend/src/**/*.ts frontend/src/**/*.tsx` SHALL 返回 0 行



## MODIFIED Requirements (from change 2026-06-05-demand-requirement)

### Requirement: Sider 含「需求管理」菜单组（v0.0.8 起 4 项，项目排第一）

前端 SHALL 在登录后页面（AppLayout 下）的左侧 Sider 中新增菜单组「需求管理」，位于「组织」之后；展开后含 4 项（v0.0.8 起）：项目 / 诉求 / 需求 / 诉求-需求关联，项目项排第一。

#### Scenario: Sider 含「需求管理」4 路由（v0.0.8）

- **GIVEN** 用户已登录访问 `/`
- **WHEN** 页面渲染完成
- **THEN** 左侧 Sider SHALL 渲染
- **AND** Sider SHALL 含菜单组 `"需求管理"`
- **AND** 该组展开后 SHALL 含 4 项：`"项目"`、`"诉求"`、`"需求"`、`"诉求-需求关联"`
- **AND** 项目项 SHALL 位于诉求项之前
- **AND** 点击 `"项目"` SHALL 跳转 `/pm/projects`
- **AND** 点击 `"诉求"` SHALL 跳转 `/pm/demands`

### Requirement: /pm/* 路由全部注册（v0.0.8 加 /pm/projects + 重定向变更）

前端 SHALL 在 router 中注册 `/pm/projects`（v0.0.8）、`/pm/demands`、`/pm/requirements`、`/pm/demand-requirements` 四条路由；访问 `/pm` SHALL 重定向至 `/pm/projects`（v0.0.8 起；v0.0.6 重定向至 `/pm/demands`）。

#### Scenario: /pm/projects 路由直接访问（v0.0.8）

- **GIVEN** 用户已登录
- **WHEN** 浏览器直接访问 `/pm/projects`
- **THEN** SHALL 渲染 `ProjectsPage` 组件
- **AND** `grep -c "/pm/projects" frontend/src/AppRoutes.tsx` SHALL ≥ 1（防止 linter 静默回退）

#### Scenario: /pm/demands 路由直接访问

- **GIVEN** 用户已登录
- **WHEN** 浏览器直接访问 `/pm/demands`
- **THEN** SHALL 渲染 DemandsPage 组件
- **AND** `grep -c "/pm/demands" frontend/src/AppRoutes.tsx` SHALL ≥ 1（防止 linter 静默回退）

### Requirement: ProjectsPage CRUD + 默认 owner（v0.0.8）

前端 SHALL 提供 `/pm/projects` 路由对应的 ProjectsPage，含列表 + 新建 + 编辑 + 删除；编辑抽屉「负责人」下拉新建时 SHALL 默认选中当前登录用户（按 loginName 匹配 listUsers 返回池），编辑时回显 editing.ownerUserId 且**不 disabled**（可改）；删除受后端 FK 保护 409 兜底。

#### Scenario: 新建抽屉默认 owner = 当前登录用户

- **GIVEN** Auth store 中 `user.username="alice"`；mock `listUsers` 返回包含 `{id:1, loginName:"alice"}`
- **WHEN** 用户点击「新建项目」打开抽屉，且 listUsers promise 已 resolve
- **THEN** 「负责人」下拉的当前选中值 SHALL 等于 `1`（alice 的 id）

#### Scenario: 编辑抽屉 owner 可改

- **GIVEN** 抽屉打开为编辑模式 editing.ownerUserId=1；mock listUsers 返回 `[{id:1, loginName:"alice"}, {id:2, loginName:"lili"}]`
- **WHEN** 用户切换下拉到 lili (id=2) 并点保存
- **THEN** 「负责人」下拉 SHALL 不 disabled
- **AND** mock `updateProject` SHALL 被调用且参数 body.ownerUserId SHALL 等于 2

### Requirement: RequirementEditDrawer 与 UserRolesPage 的 projectId 控件改造（v0.0.8）

前端 SHALL 把 `RequirementEditDrawer` 与 `UserRolesPage` 的 projectId 输入控件从「数字输入框」改为「Project 下拉」（异步 listProjects）。UserRolesPage 的下拉 SHALL 保留「留白」选项（=公司级 hat，传 `null`）。RequirementsPage 与 UserRolesPage 列表 SHALL 新增「项目」列，render `projectName (projectCode)` 或 "—"。

#### Scenario: UserRolesPage 新建关联 项目留白 = 公司级 hat

- **GIVEN** mock `listProjects` 返回 1 条 project；用户和角色都已选
- **WHEN** 用户「项目」下拉**留白**并点保存
- **THEN** mock `createUserRole` SHALL 被调用且参数 body.projectId SHALL 为 `null`（不是 undefined，不是 0）

### Requirement: requirement 编辑抽屉支持「源诉求」多选

前端 SHALL 在 RequirementsPage 的编辑抽屉中提供「源诉求」分区，渲染分页 + 搜索 demand 列表 + 复选；保存时把 checked id 收集为 `sourceDemandIds: number[]` 并传给 `POST /api/requirements`（新建）或 `PUT` 后调用关联 API（编辑）。

#### Scenario: 新建需求时多选源诉求

- **GIVEN** 抽屉打开（mock `listDemands` 返回 2 条 demand id=10、id=20）
- **WHEN** 用户勾选 id=10 + id=20，填写 code/title/description，点保存
- **THEN** mock `createRequirement` SHALL 被调用且参数 body.sourceDemandIds SHALL 等于 `[10, 20]`



## MODIFIED Requirements (from change 2026-06-05-position-role)

### Requirement: Sider 含「人事配置」菜单组

前端 SHALL 在 AppLayout 左侧 Sider 新增菜单组「人事配置」，位于「需求管理」之后；展开后含 3 项：岗位 / 角色 / 用户角色。

#### Scenario: Sider 含「人事配置」3 路由

- **GIVEN** 用户已登录访问 `/`
- **WHEN** 页面渲染完成
- **THEN** 左侧 Sider SHALL 渲染
- **AND** Sider SHALL 含菜单组 `"人事配置"`
- **AND** 该组展开后 SHALL 含 3 项：`"岗位"`、`"角色"`、`"用户角色"`
- **AND** 点击 `"岗位"` SHALL 跳转 `/hr/positions`

### Requirement: /hr/* 路由全部注册

前端 SHALL 在 `AppRoutes.tsx` 注册 `/hr/positions`、`/hr/roles`、`/hr/user-roles` 三条路由；访问 `/hr` SHALL 重定向至 `/hr/positions`。

#### Scenario: 路由直接访问

- **GIVEN** 用户已登录
- **WHEN** 浏览器直接访问 `/hr/positions`
- **THEN** SHALL 渲染 PositionsPage 组件
- **AND** `grep -c "/hr/positions" frontend/src/AppRoutes.tsx` SHALL ≥ 1（防止 linter 静默回退）

### Requirement: UsersPage 编辑抽屉新增「岗位」下拉

前端 SHALL 在 `UsersPage` 编辑抽屉新增「岗位」下拉，异步加载岗位池；保存时 POST/PUT 包含 `positionId`；列表新增「岗位」列展示 `positionName + category`。

#### Scenario: 新建用户时选择岗位

- **GIVEN** 抽屉打开（mock `listPositions` 返回 2 条岗位 id=1/2）
- **WHEN** 用户从下拉选择 id=1，填写 loginName + name，点保存
- **THEN** mock `createUser` SHALL 被调用且参数 body.positionId SHALL 等于 1
