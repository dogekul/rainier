# Capability: frontend-scaffold

> Change log (recent):
> - 2026-06-05 (v0.0.6-demand-requirement) — added 需求管理 menu group + `/pm/*` routes.
> - 2026-06-05 (v0.0.7-position-role) — added 人事配置 menu group + `/hr/*` routes.
> - 2026-06-07 (v0.0.8-project) — added 项目 menu item (first position in 需求管理) + `/pm/projects` route + ProjectsPage; converted projectId numeric input → Project dropdown in RequirementEditDrawer and UserRolesPage; added 项目 column to Requirement / UserRole lists; `/pm` redirect now points at `/pm/projects` (was `/pm/demands`).
> - 2026-06-08 (v0.0.9-story) — RequirementsPage row drilldown: per-Requirement expand button reveals a StoryListPanel (sub-table + new Story button + edit/delete); added `"Story 数"` column showing `storyCount` enrichment; introduced `StoryEditDrawer` for Story create/edit (default owner = current logged-in user, Requirement field locked; sibling of v0.0.8 default-owner pattern); added shared Table primitive `isExpanded` / `renderExpanded` props to support drilldown without bespoke per-page expand state.
> - 2026-06-09 (v0.0.10-sprint) — Sider 「需求管理」 grows to 5 items: 项目 / **Sprint** / 诉求 / 需求 / 诉求-需求关联; added `/pm/sprints` route → SprintsPage (read-only browser with row-expand revealing StoryListPanel keyed by sprintId); RequirementsPage drilldown swapped: StoryListPanel → SprintListPanel (CRUD for Sprint, with row Sprint count column renamed 'Story 数' → 'Sprint 数' sourced from `sprintCount`); SprintEditDrawer mirrors StoryEditDrawer pattern (default owner = current logged-in user, parent Requirement field locked at creation, owner mutable on edit); StoryEditDrawer locked-display swap: shows parent Sprint + grandparent Requirement (was Requirement-only); StoryListPanel prop change: requirementId → sprintId; new `api/sprint.ts` for Sprint CRUD.
> - 2026-06-09 (v0.0.11-task) — Sider 「需求管理」 grows to 6 items: 项目 / Sprint / **任务** / 诉求 / 需求 / 诉求-需求关联; 「任务」 排第三 (执行靠近); added `/pm/tasks` route → `TasksPage` (full CRUD list + filter projectId/status/priority/assigneeUserId/sprintId/storyId + pagination); `TaskEditDrawer` with Project/Sprint/Story/Assignee 联动级联 selects (size=100 client-side filter, sprint/story options narrow when Project chosen; sprint/story selects clear when Project changes); new `api/task.ts` for Task CRUD.
> - 2026-06-09 (v0.0.12-product) — NEW Sider 顶级菜单组「产品」, 位于「组织」与「需求管理」之间. Sider 顶级组保持 4 组: 组织 → 产品 → 需求管理 → 人事配置. 「产品」展开后含 4 项: 产品分类 / 产品 / 产品模块 / 功能, 对应 4 条新路由 (`/pm/product-categories` / `/pm/products` / `/pm/product-modules` / `/pm/features`). 4 个 EditDrawer 用 v0.0.11 同款 cascading-select (size=100 客户端 filter): Product 选 Category 必选; Module 选 Product 必选 (可加 Category filter); Feature 选 Product + Module 必选. 新 `api/{productCategory,product,productModule,feature}.ts` 4 个 api 文件.
> - 2026-06-10 (v0.0.13-product-restructure) — 「产品」组 4 项 → **3 项** (删「产品分类」入口 + `/pm/product-categories` 路由 + `ProductCategoriesPage` + `api/productCategory.ts`). ProductsPage 去 Category 列; ProductEditDrawer 去 Category select. ProductModulesPage 改 **树形列表** (嵌套 UL/LI + depth 缩进, 完全替换 Table); ProductModuleEditDrawer 改 **Product → 可选 parentModule** 二级 cascade (服务器侧 `listProductModules({productId})` 过滤, 支持 reparent). FeatureEditDrawer 模块下拉用后端 `pathName` 显示父链 ("钱包 / 余额"). `api/productModule.ts` + `api/product.ts` 字段调整 (加 parentId/pathName/pathCodes, 去 categoryId).
> - 2026-06-11 (v0.0.14-sprint-feature-link) — SprintListPanel 每 sprint 行加「功能」按钮 → SprintFeaturePanel (挂载/解绑 feature, 下拉按 sprint 产品过滤); FeaturesPage 每 feature 行加「所在迭代」→ FeatureSprintsPanel; 新 `api/sprintFeature.ts`, `api/sprint.ts` 加 productId/productName.
> - 2026-06-11 (v0.0.15-audit-log) — NEW Sider **第 5 顶级组「系统」**(末位, 位于「人事配置」之后), 含 1 项「审计日志」→ `/sys/audit-logs`. 新只读 `AuditLogsPage` (表格 actor/entityType/entityId/action/时间 + 过滤 + 分页, **无新建/编辑/删除**); 新 `api/auditLog.ts`. Sider 顶级组 4 → **5**.

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

### Requirement: Sider 含「需求管理」菜单组（v0.0.11 起 6 项，任务 排第三）

前端 SHALL 在登录后页面（AppLayout 下）的左侧 Sider 中渲染菜单组「需求管理」（位于「组织」之后）；展开后含 6 项（v0.0.11 起）：项目 / Sprint / **任务** / 诉求 / 需求 / 诉求-需求关联，任务项位于 Sprint 与 诉求 之间。

#### Scenario: Sider 含「需求管理」6 路由（v0.0.11）

- **GIVEN** 用户已登录访问 `/`
- **WHEN** 页面渲染完成
- **THEN** 左侧 Sider SHALL 渲染
- **AND** Sider SHALL 含菜单组 `"需求管理"`
- **AND** 该组展开后 SHALL 含 6 项：`"项目"`、`"Sprint"`、`"任务"`、`"诉求"`、`"需求"`、`"诉求-需求关联"`
- **AND** 任务 项 SHALL 位于 Sprint 项之后、诉求项之前
- **AND** 点击 `"任务"` SHALL 跳转 `/pm/tasks`

### Requirement: Sider 顶级菜单组「产品」（v0.0.13 改为 3 项）

> v0.0.13-product-restructure (2026-06-10) — 「产品分类」入口删除（ProductCategory capability 整层移除）。

前端 SHALL 在 Sider 渲染 4 个顶级菜单组 — **组织 → 产品 → 需求管理 → 人事配置**. 「产品」组保持第 2 位（v0.0.12 起加），展开后含 **3 项**：**产品 / 产品模块 / 功能**, 对应 `/pm/products` / `/pm/product-modules` / `/pm/features` 路由. 不再含 v0.0.12 的「产品分类」入口.

#### Scenario: Sider 顶级 4 组 + 产品组 3 路由（v0.0.13）

- **GIVEN** 用户已登录访问 `/`
- **WHEN** 页面渲染完成
- **THEN** 左侧 Sider SHALL 含 4 个顶级菜单组：「组织」/「产品」/「需求管理」/「人事配置」
- **AND** 「产品」 组 SHALL 位于「组织」之后、「需求管理」之前
- **AND** 「产品」 组展开后 SHALL 含 3 项：`"产品"` / `"产品模块"` / `"功能"`
- **AND** 「产品」组 SHALL **不含**「产品分类」项
- **AND** 点击 `"产品"` SHALL 跳转 `/pm/products`
- **AND** 点击 `"功能"` SHALL 跳转 `/pm/features`

### Requirement: /pm/product-* 3 路由注册（v0.0.13 去 categories）

前端 SHALL 在 router 中注册 **3** 条路由：`/pm/products` → `ProductsPage`, `/pm/product-modules` → `ProductModulesPage`, `/pm/features` → `FeaturesPage`. `/pm/product-categories` 路由 SHALL 删除（访问回落到 `/`）.

#### Scenario: /pm/products 路由直接访问 + grep guard

- **GIVEN** 用户已登录
- **WHEN** 浏览器直接访问 `/pm/products`
- **THEN** SHALL 渲染 `ProductsPage` 组件
- **AND** `grep -c "/pm/products" frontend/src/AppRoutes.tsx` SHALL ≥ 1

#### Scenario: /pm/product-categories 路由已删除（v0.0.13）

- **GIVEN** 用户已登录
- **WHEN** 浏览器直接访问 `/pm/product-categories`
- **THEN** SHALL **不**渲染 `ProductCategoriesPage`（页面已删）
- **AND** `grep -c "/pm/product-categories" frontend/src/AppRoutes.tsx` SHALL 为 0

### Requirement: ProductModulesPage 树形列表显示（v0.0.13）

前端 SHALL 在 `/pm/product-modules` 页面将模块列表渲染为树形结构 — 顶层模块（parentId 为 null）作为根节点，子模块通过嵌套 UL 缩进显示；保留 search + status filter.

#### Scenario: 树形渲染含 ≥ 2 层缩进

- **GIVEN** 后端返回 Module 列表：M1 (id=1, parentId=null) → M2 (id=2, parentId=1) → M3 (id=3, parentId=2)
- **WHEN** `/pm/product-modules` 页面渲染完成
- **THEN** SHALL 渲染嵌套 UL/LI 结构
- **AND** M2 SHALL 在 M1 的 `<ul>` 子元素内
- **AND** M3 SHALL 在 M2 的 `<ul>` 子元素内
- **AND** M2 SHALL 比 M1 多一级缩进（CSS padding-left）

### Requirement: ProductModuleEditDrawer Product + parentModule 二级 cascade（v0.0.13）

前端 SHALL 在 `ProductModuleEditDrawer` 中用 **Product → 可选 parentModule** 二级 cascade（替换 v0.0.12 的 Category→Product）. 选 Product 后通过服务器侧过滤 `listProductModules({productId})` 拉取候选父模块；切换 Product 时清空 parentModule. 创建时 parentModule 默认空（顶层）.

#### Scenario: Product 切换触发 parentModule 候选刷新

- **GIVEN** Product A 下有若干 Module, Product B 下有若干 Module
- **WHEN** 用户在 EditDrawer 切换 Product A → Product B
- **THEN** parentModule 下拉 SHALL 清空当前选项
- **AND** SHALL 触发 `listProductModules({productId: B.id})` API 调用
- **AND** parentModule 下拉 SHALL 显示 Product B 的 module 选项
- **AND** parentModule 留空提交时 SHALL `POST` body 不含 parentId（创建顶层）

### Requirement: FeatureEditDrawer 模块下拉显示父链 pathName（v0.0.13）

前端 SHALL 在 `FeatureEditDrawer` 的模块下拉中使用后端 `pathName` 字段渲染选项标签（如 `"钱包 / 余额"`），让用户区分多层结构下同名子模块；仍单选，productId 切换时重拉.

#### Scenario: 多层模块下拉显示完整 pathName

- **GIVEN** Product 1 下 Module 树：根 M1 (name="钱包") → M2 (name="余额")
- **WHEN** 用户在 FeatureEditDrawer 选 Product 1
- **THEN** 模块下拉 SHALL 含 2 个选项
- **AND** M1 选项可见文本 SHALL 含 `"钱包"`
- **AND** M2 选项可见文本 SHALL 含 `"钱包 / 余额"`（pathName）

### Requirement: /pm/* 路由全部注册（v0.0.11 加 /pm/tasks）

前端 SHALL 在 router 中注册 `/pm/projects`、`/pm/sprints`（v0.0.10）、**`/pm/tasks`（v0.0.11）**、`/pm/demands`、`/pm/requirements`、`/pm/demand-requirements` 六条路由；`/pm` 重定向至 `/pm/projects` 保持不变。

#### Scenario: /pm/tasks 路由直接访问（v0.0.11）

- **GIVEN** 用户已登录
- **WHEN** 浏览器直接访问 `/pm/tasks`
- **THEN** SHALL 渲染 `TasksPage` 组件
- **AND** `grep -c "/pm/tasks" frontend/src/AppRoutes.tsx` SHALL ≥ 1（防止 linter 静默回退）

### Requirement: TaskEditDrawer Project/Sprint/Story 联动级联（v0.0.11）

前端 SHALL 提供 `TaskEditDrawer` 组件用于新增 / 编辑 Task；Project 下拉必选；Sprint 下拉可选，其选项 SHALL 按客户端 filter `s.projectId === selectedProject.id` 过滤显示；Story 下拉可选，其选项 SHALL 按 `s.projectId === selectedProject.id && (sprintId 未选 || s.sprintId === selectedSprintId)` 过滤显示；切换 Project 时 Sprint / Story 当前选中 SHALL 被清空；Assignee 下拉含「待分配」空选项（提交 null 合法 — 与 Story owner 不同）。

#### Scenario: Project 切换后 Sprint/Story 清空

- **GIVEN** TaskEditDrawer 打开为新建模式；mock listProjects/listSprints/listStories 返回若干数据；初始 Sprint id=20 (projectId=1) 已选
- **WHEN** 用户从 Project A (id=1) 切换到 Project B (id=2)
- **THEN** Sprint 下拉的当前选中值 SHALL 为空
- **AND** Story 下拉的当前选中值 SHALL 为空
- **AND** Sprint 下拉的可选项 SHALL 只含 `s.projectId === 2` 的 Sprint

#### Scenario: /pm/sprints 路由直接访问（v0.0.10）

- **GIVEN** 用户已登录
- **WHEN** 浏览器直接访问 `/pm/sprints`
- **THEN** SHALL 渲染 `SprintsPage` 组件
- **AND** `grep -c "/pm/sprints" frontend/src/AppRoutes.tsx` SHALL ≥ 1（防止 linter 静默回退）

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

### Requirement: RequirementsPage 行级 drilldown — v0.0.10 改 Sprint 子表 + sprintCount 列

前端 SHALL 在 RequirementsPage 列表显示一列 `"Sprint 数"` 显示 `r.sprintCount`（v0.0.10 替换 v0.0.9 "Story 数" / `storyCount`）；每行展开按钮点击后渲染 `SprintListPanel`，调用 `listSprints({requirementId: r.id})` 取该 Requirement 下的 Sprints，渲染为表格 + "新建 Sprint" 按钮 + 每行"编辑 / 删除"按钮。**不**新增独立 Sider 菜单项专给 SprintListPanel。

#### Scenario: RequirementsPage 表格含 Sprint 数 列 (v0.0.10)

- **GIVEN** 用户已登录访问 `/pm/requirements`；mock `listRequirements` 返回 1 行 `{id: 1, code: "REQ-1", title: "X", sprintCount: 3}`
- **WHEN** 页面渲染完成
- **THEN** 表格 SHALL 含一列标题 `"Sprint 数"`
- **AND** 该行 `"Sprint 数"` 单元格 SHALL 显示 `"3"`
- **AND** 该行 SHALL 渲染 `"展开"` 按钮

#### Scenario: 点开行渲染 SprintListPanel + 子表 (v0.0.10)

- **GIVEN** mock `listSprints({requirementId: 1})` 返回 2 行 Sprint `[{id:10, code:"SPR-A", name:"Phase 1", status:"PLANNING"}, {id:11, code:"SPR-B", name:"Phase 2", status:"ACTIVE"}]`
- **WHEN** 用户点击 Requirement 行的展开按钮
- **THEN** 子区域 SHALL 渲染 `data-testid="sprint-list-panel-1"`
- **AND** 子表 SHALL 含 `"SPR-A"` 与 `"SPR-B"` 两行
- **AND** 子区域 SHALL 含 `"新建 Sprint"` 按钮（`data-testid="sprints-new-btn"`）

### Requirement: SprintsPage + SprintEditDrawer + Story drilldown reshape（v0.0.10）

前端 SHALL 提供 `SprintsPage` 浏览页（`/pm/sprints`，展开行渲染 `StoryListPanel`，按 `sprintId` 查询 Stories）；SHALL 提供 `SprintEditDrawer` 组件用于新增 / 编辑 Sprint（默认 owner = 当前登录用户；「所属 Requirement」字段创建时锁定，编辑模式同样锁定；owner 字段可改；空 owner 时显示表单错误，沿用 v0.0.8.1 Code-M7 模式）。`StoryEditDrawer` 锁定显示 SHALL 升级为「所属 Sprint + 上级 Requirement」两段；`StoryListPanel` 入参 SHALL 改为 sprintId（替换 v0.0.9 requirementId）。

#### Scenario: SprintsPage 行展开渲染 StoryListPanel(sprintId)

- **GIVEN** mock `listSprints` 返回 1 行 `{id:10, code:"SPR-A", name:"Phase 1", ...}`；mock `listStories({sprintId:10})` 返回 2 行
- **WHEN** 用户访问 `/pm/sprints` 并点击 Sprint 行展开按钮
- **THEN** 子区域 SHALL 渲染 `data-testid="story-list-panel-10"`
- **AND** 子区域 SHALL 含 `"新建 Story"` 按钮

#### Scenario: SprintEditDrawer 默认 owner = 当前登录用户

- **GIVEN** Auth store 中 `user.username="alice"`；mock `listUsers` 返回包含 `{id:1, loginName:"alice"}`；用户在 RequirementsPage 展开了 Requirement id=42
- **WHEN** 用户点击 "新建 Sprint" 按钮打开抽屉，且 listUsers promise 已 resolve
- **THEN** 「负责人」下拉的当前选中值 SHALL 等于 `1`
- **AND** 「所属需求」字段 SHALL 锁定显示 Requirement 42 信息，**不**可选择其它 Requirement

### Requirement: StoryEditDrawer 新增/编辑 Story 抽屉（v0.0.9）

前端 SHALL 提供 `StoryEditDrawer` 组件用于新增 / 编辑 Story；新建路径从 `StoryListPanel` 的 "新建 Story" 按钮触发，传入 `requirementId`；编辑路径从子表行 "编辑" 按钮触发，回显 editing Story。新建抽屉「负责人」下拉 SHALL 默认选中当前登录用户（按 loginName 匹配 listUsers 池），编辑时 SHALL 不 disabled（可改，沿用 v0.0.8 模式）。「所属 Requirement」字段在新建/编辑两个模式下都 SHALL 锁定显示（不可改），格式为 `"<requirementTitle>（<requirementCode>）— 创建时锁定"`。Drawer SHALL 在 owner 为空时显示表单错误提示（沿用 v0.0.8.1 Code-M7 模式），不再静默 no-op。

#### Scenario: 新建 Story 抽屉默认 owner = 当前登录用户

- **GIVEN** Auth store 中 `user.username="alice"`；mock `listUsers` 返回包含 `{id:1, loginName:"alice"}`；用户在 RequirementsPage 展开了 Requirement id=1
- **WHEN** 用户点击 "新建 Story" 按钮打开抽屉，且 listUsers promise 已 resolve
- **THEN** 「负责人」下拉的当前选中值 SHALL 等于 `1`
- **AND** 「Requirement」字段 SHALL 锁定显示 Requirement 1 信息，**不**可选择其它 Requirement

#### Scenario: 编辑 Story 抽屉 owner 可改 → 调用 updateStory

- **GIVEN** 抽屉打开为编辑模式 editing.ownerUserId=1；mock listUsers 返回 `[{id:1, loginName:"alice"}, {id:2, loginName:"lili"}]`
- **WHEN** 用户切换下拉到 lili (id=2) 并点保存
- **THEN** 「负责人」下拉 SHALL 不 disabled
- **AND** mock `updateStory` SHALL 被调用且参数 body.ownerUserId SHALL 等于 2

### Requirement: 共享 Table 组件支持可展开行（v0.0.9 加）

`components/ui/Table.tsx` SHALL 增加可选 `isExpanded?: (row) => boolean` + `renderExpanded?: (row) => ReactNode` 两个 props。当 `isExpanded(row)` 返回 true，紧随该行的次行 SHALL 跨所有列渲染 `renderExpanded(row)`。父组件 SHALL 拥有展开状态（Set / Map），通过 props 注入控制；Table 本身 SHALL 不持有 expand state。

#### Scenario: Table 展开行渲染

- **GIVEN** Table props 含 `isExpanded={(r) => r.id === 1}` + `renderExpanded={(r) => <div data-testid="exp">{r.id}</div>}`
- **WHEN** Table 渲染含 id=1 的行
- **THEN** id=1 的行 SHALL 渲染常规 cells
- **AND** 紧随其后 SHALL 有一行 `<td colSpan=N>` 包含 `data-testid="exp"` 显示 "1"

## ADDED Requirements (from change 2026-06-11-sprint-feature-link / v0.0.14)

### Requirement: Sprint「关联功能」面板

前端 SHALL 在 Sprint 的行级 drilldown 或详情中提供「关联功能」面板：展示该 sprint 已挂的 Feature 列表，提供「挂载功能」（feature 下拉 + 提交）与每行「解绑」。挂载下拉在 sprint.productId 非空时 SHALL 仅显示该产品的 feature。

#### Scenario: 展示已挂功能并支持挂载

- **GIVEN** Sprint S 已挂 Feature F1
- **WHEN** 用户展开 S 的「关联功能」面板
- **THEN** 面板 SHALL 显示 F1
- **AND** SHALL 提供「挂载功能」下拉与提交按钮
- **AND** 提交后 SHALL 调用 `POST /api/sprint-features` 并刷新列表

#### Scenario: 解绑功能

- **GIVEN** Sprint S 已挂 Feature F1
- **WHEN** 用户点击 F1 行的「解绑」
- **THEN** SHALL 调用 `DELETE /api/sprint-features/{id}`
- **AND** 刷新后 F1 SHALL 从面板消失

### Requirement: Feature 列表/详情显示「所在迭代」

前端 SHALL 在 Feature 页对每个 feature 提供查看其所在迭代的能力（调用 `GET /api/features/{id}/sprints`），展示 sprint code/name/status。

#### Scenario: 查看 feature 的所在迭代

- **GIVEN** Feature F 被挂到 Sprint S1
- **WHEN** 用户在 Feature 页查看 F 的「所在迭代」
- **THEN** SHALL 调用 `GET /api/features/{F}/sprints`
- **AND** SHALL 显示 S1 的 code 与 name

## ADDED Requirements (from change 2026-06-11-audit-log / v0.0.15)

### Requirement: Sider 顶级菜单组「系统」（v0.0.15 起加，5 顶级组）

前端 SHALL 在 Sider 渲染 5 个顶级菜单组 — **组织 → 产品 → 需求管理 → 人事配置 → 系统**。「系统」组为第 5 位（末位），展开后含 1 项：**审计日志**，对应 `/sys/audit-logs` 路由。

#### Scenario: Sider 含「系统」组 + 审计日志入口

- **GIVEN** 用户已登录访问 `/`
- **WHEN** 页面渲染完成
- **THEN** 左侧 Sider SHALL 含 5 个顶级菜单组，末位为「系统」
- **AND** 「系统」组展开后 SHALL 含「审计日志」项
- **AND** 点击「审计日志」SHALL 跳转 `/sys/audit-logs`

### Requirement: AuditLogsPage 只读查询页

前端 SHALL 在 `/sys/audit-logs` 渲染 `AuditLogsPage`：表格展示 actor / entityType / entityId / action / 时间，提供 actor / entityType / entityId / action 过滤 + 分页。页面 SHALL **只读** —— 无新建 / 编辑 / 删除按钮、无 EditDrawer。

#### Scenario: 渲染审计表格

- **GIVEN** 后端 `GET /api/audit-logs` 返回 2 条审计行
- **WHEN** `/sys/audit-logs` 渲染完成
- **THEN** SHALL 渲染表格含这 2 行
- **AND** 表头 SHALL 含 `操作人` / `实体类型` / `实体ID` / `动作` 列
- **AND** 页面 SHALL **不**含「新建」按钮

#### Scenario: 按 entityType 过滤触发查询

- **GIVEN** `/sys/audit-logs` 已渲染
- **WHEN** 用户在 entityType 过滤输入 "REQUIREMENT" 并触发查询
- **THEN** SHALL 调用 `listAuditLogs` 且 params 含 `entityType: "REQUIREMENT"`

### Requirement: /sys/audit-logs 路由注册

前端 SHALL 在 router 注册 `/sys/audit-logs` → `AuditLogsPage`。

#### Scenario: 路由直接访问

- **GIVEN** 用户已登录
- **WHEN** 浏览器直接访问 `/sys/audit-logs`
- **THEN** SHALL 渲染 `AuditLogsPage` 组件
- **AND** `grep -c "/sys/audit-logs" frontend/src/AppRoutes.tsx` SHALL ≥ 1
