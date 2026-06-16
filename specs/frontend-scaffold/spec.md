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
> - 2026-06-11 (v0.0.16-project-type) — ProjectsPage 新建/编辑抽屉加「项目类型」下拉 (`projects-type-select`, 选项 轻量/正式, 新建默认 轻量); 表格加「类型」列 (中文 轻量/正式); 表格上方加「类型」过滤下拉 (`projects-type-filter`, 含「全部类型」, 改变即带 `projectType` 重查). `api/project.ts` 加 `ProjectType` 类型 + `projectType` 字段. 无新增页面/路由/Sider 组.
> - 2026-06-12 (v0.0.17-milestone) — ProjectsPage 每行操作区加「里程碑」按钮 (`projects-milestones-btn-${id}`) → 用 Table 的 `isExpanded`/`renderExpanded` 在行下内联展开 `MilestonesPanel` (`milestones-panel-${projectId}`, SprintsPage 同款展开模式); 面板内对该项目里程碑做 列表(按 sortOrder)/新建/编辑/删除 (`milestone-save-btn` 等). 新 `api/milestone.ts`. 无新增页面/路由/Sider 组.
> - 2026-06-12 (v0.0.18-workbench) — 占位 Home 删除 → `/` 改为「我的工作台」`WorkbenchPage`(挂载调 `GET /api/auth/me` 取当前用户上下文;问候 + 角色 chips + 我的任务(assignee=我, 状态快改 `updateTask`) + 我的 Story(owner=我) + 我的项目;条目均为链接 → 任务/pm/tasks、Story/pm/sprints、项目/pm/projects). `AuthUser` 扩展 id/name/roles/projects(可选);新 `api/auth.ts` 富 MeResponse、`api/story.ts` +ownerUserId. **导航壳增强**(Gate 3 反馈): AppLayout 加「工作台」菜单组(我的工作台→/)、品牌「Rainier」→`/` 链接、菜单组标题可折叠(`appshell-group-${key}`)、顶部 `appshell-sider-toggle` 收起整个 Sider、样式优化.
> - 2026-06-15 (v0.0.19-requirement-enrich) — RequirementEditDrawer 状态下拉改新 6 态中文(草稿/审批中/分析中/实施中/已交付/已关闭)、优先级下拉 5 级中文(紧急/高/中/低/最低)、加「期望交付日期」输入(`req-expected-date`);RequirementsPage 状态/优先级列中文化;demand/story/task 优先级下拉加「最低」. 中文标签集中:`api/demand.ts:PRIORITY_LABELS`、`api/requirement.ts:REQUIREMENT_STATUS_LABELS`(全下拉/列复用).
> - 2026-06-15 (v0.0.20-role-nav) — **角色分级导航**: `store/auth.ts` 加 `isElevated(user)`(任一角色 `adminAccess` 为真); `AppLayout` 的 `navGroups` 加 `requiresAdmin`(组织/产品/人事配置/系统 = admin, 工作台/需求管理 = 全员), 按 `isElevated` 过滤可见组(普通用户只见 工作台+需求管理). `ProtectedRoute` 升为 **app 级 me() 注水点**(挂载调一次 `GET /api/auth/me` 写 store, 取代 WorkbenchPage 自调) + **admin 路由守卫**(`isAdminPath` 命中 `/org`、`/hr`、`/sys`、`/pm/products`、`/pm/product-modules`、`/pm/features` 且非 elevated → redirect `/`; hydrated 门控避免首帧误踢; `/pm` 全员路由不被守卫). `navGroups` 与 `isAdminPath` 导出 + `navGuardConsistency.test` 机械钉死两者一致. `RolesPage` 加「管理员权限」复选框(`role-admin-access`) + 列表「管理员」列. `WorkbenchPage` 改读 store(不再调 me()). `api/auth.ts` MeRole + `api/role.ts` Role/RoleCreate/RoleUpdate 加 `adminAccess`. **仅前端 UX 收口**(后端 API 鉴权收口留待后续).

## Requirements

### Requirement: 角色分级导航 (v0.0.20)

前端 SHALL 按当前用户的提升态（`isElevated` = 任一角色 `adminAccess` 为真）裁剪导航并守卫 admin 路由。普通用户
SHALL 只见 工作台 + 需求管理；管理员 SHALL 见全 6 组。

#### Scenario: 普通用户只见工作台与需求管理

- **GIVEN** 已登录用户所有角色 `adminAccess = false`
- **WHEN** AppLayout Sider 渲染
- **THEN** `工作台` 与 `需求管理` 组 SHALL 可见
- **AND** `组织` / `产品` / `人事配置` / `系统` 组 SHALL NOT 渲染

#### Scenario: 管理员见全六组

- **GIVEN** 已登录用户至少一个角色 `adminAccess = true`
- **WHEN** AppLayout Sider 渲染
- **THEN** 6 组（工作台/组织/产品/需求管理/人事配置/系统）SHALL 全部可见

#### Scenario: 非管理员直敲 admin 路由被守卫回首页

- **GIVEN** 已登录非管理员用户且角色上下文已注水
- **WHEN** 用户导航到 admin 路由（如 `/org/users`、`/hr/roles`、`/sys/audit-logs`、`/pm/products`）
- **THEN** 路由 SHALL redirect 到 `/`

#### Scenario: pm 组路由对非管理员开放

- **GIVEN** 已登录非管理员用户且角色上下文已注水
- **WHEN** 用户导航到 `/pm/projects`（需求管理组，全员）
- **THEN** ProjectsPage SHALL 渲染（不被守卫）

### Requirement: ProtectedRoute app 级注水当前用户上下文 (v0.0.20)

`ProtectedRoute` SHALL 在进入受保护路由时调一次 `GET /api/auth/me` 并把 id/name/roles/projects 写入 auth store
（应用内只此一处调 me()），使角色分级导航与路由守卫全局可用。

#### Scenario: 入口注水一次

- **GIVEN** 已登录用户但 store `user` 无 `roles`
- **WHEN** 受保护路由挂载
- **THEN** ProtectedRoute SHALL 调一次 `me()` 并把结果写入 auth store

#### Scenario: isElevated 助手反映任一 admin 角色

- **GIVEN** AuthUser 角色为 `[{adminAccess:false},{adminAccess:true}]`
- **WHEN** 求值 `isElevated(user)`
- **THEN** SHALL 返回 `true`
- **AND** 无 admin 角色的用户 SHALL 返回 `false`

### Requirement: RolesPage 维护 adminAccess (v0.0.20)

`RolesPage` 编辑/新建抽屉 SHALL 含「管理员权限」复选框，保存时把 `adminAccess` 带入 create/update 请求体。

#### Scenario: 勾选管理员权限保存携 adminAccess

- **GIVEN** RolesPage 编辑/新建抽屉打开
- **WHEN** 勾选「管理员权限」并保存
- **THEN** create/update 请求体 SHALL 携 `adminAccess: true`

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

## ADDED Requirements (from change 2026-06-11-project-type / v0.0.16)

### Requirement: ProjectsPage 项目类型下拉（新建/编辑抽屉）

前端 SHALL 在 ProjectsPage 的新建/编辑抽屉提供「项目类型」下拉(`projects-type-select`)，选项为 轻量(CASUAL) / 正式(FORMAL)，新建时默认选中 轻量，编辑时回显该项目的 projectType。

#### Scenario: 新建抽屉含项目类型下拉且默认轻量

- **GIVEN** 用户在 ProjectsPage 点击「新建项目」打开抽屉
- **WHEN** 抽屉渲染完成
- **THEN** 抽屉 SHALL 含「项目类型」下拉(`projects-type-select`)
- **AND** 下拉选项 SHALL 含「轻量」与「正式」
- **AND** 新建时默认值 SHALL 为「轻量」(CASUAL)

#### Scenario: 提交携带 projectType

- **GIVEN** 用户打开新建抽屉并把「项目类型」选为「正式」
- **WHEN** 用户填妥必填项并点击「保存」
- **THEN** SHALL 调用 `createProject` 且 body 含 `projectType: "FORMAL"`

### Requirement: ProjectsPage 表格类型列

前端 SHALL 在 ProjectsPage 表格展示「类型」列，按 projectType 显示中文 轻量/正式。

#### Scenario: 表格渲染类型列中文

- **GIVEN** `listProjects` 返回一行 `projectType="FORMAL"`
- **WHEN** ProjectsPage 渲染完成
- **THEN** 表格 SHALL 含「类型」列
- **AND** 该行类型列 SHALL 显示「正式」

### Requirement: ProjectsPage 按类型过滤

前端 SHALL 在 ProjectsPage 表格上方提供「类型」过滤下拉(`projects-type-filter`，含「全部类型」)；选择某类型 SHALL 以 `projectType` 参数重新查询列表。

#### Scenario: 选择类型过滤触发带参查询

- **GIVEN** ProjectsPage 已渲染
- **WHEN** 用户在「类型」过滤下拉选择「正式」
- **THEN** SHALL 调用 `listProjects` 且 params 含 `projectType: "FORMAL"`

## ADDED Requirements (from change 2026-06-12-milestone / v0.0.17)

### Requirement: ProjectsPage 里程碑按钮 + 内联面板

前端 SHALL 在 ProjectsPage 每个项目行的操作区提供「里程碑」按钮(`projects-milestones-btn-${id}`)；点击 SHALL 在该行下方内联展开/收起 `MilestonesPanel`(`milestones-panel-${projectId}`)。

#### Scenario: 点击里程碑按钮展开内联面板

- **GIVEN** ProjectsPage 已渲染，含项目行 id=7
- **WHEN** 用户点击该行的「里程碑」按钮
- **THEN** 页面 SHALL 渲染 `milestones-panel-7` 面板
- **AND** 面板 SHALL 调用 `listMilestones` 且 params 含 `projectId: 7`

### Requirement: MilestonesPanel 内联 CRUD

前端 SHALL 在 `MilestonesPanel` 列出该项目里程碑(按 sortOrder)，并提供内联新建/编辑表单(code/name/targetDate/status/actualDate/sortOrder)与删除。

#### Scenario: 面板列出该项目里程碑

- **GIVEN** `listMilestones({projectId:7})` 返回 2 个里程碑
- **WHEN** `MilestonesPanel` 渲染完成
- **THEN** 面板 SHALL 显示这 2 个里程碑的 name 与 status

#### Scenario: 面板新建里程碑携带 projectId

- **GIVEN** 项目 id=7 的 `MilestonesPanel` 已展开
- **WHEN** 用户填妥必填项（code/name/targetDate）并点击「新建里程碑」
- **THEN** SHALL 调用 `createMilestone` 且 body 含 `projectId: 7`

#### Scenario: 面板删除里程碑

- **GIVEN** 项目 id=7 的面板列出里程碑 id=11
- **WHEN** 用户点击该里程碑的删除并确认
- **THEN** SHALL 调用 `deleteMilestone` 且参数为 `11`

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

前端 SHALL 在「我的任务」每行提供状态下拉，选新状态即调用 `updateTask` 提交（复用任务现有字段 + 新 status），并刷新列表。

#### Scenario: 改任务状态触发 updateTask

- **GIVEN** WorkbenchPage 我的任务列出任务 id=11（status="TODO"）
- **WHEN** 用户在该任务的状态下拉选择 "IN_PROGRESS"
- **THEN** SHALL 调用 `updateTask` 且第一参为 `11`、body.status 为 "IN_PROGRESS"

### Requirement: 工作台条目可跳转

前端 SHALL 把 WorkbenchPage 的「我的任务/我的 Story/我的项目」条目渲染为链接 —— 任务 → `/pm/tasks`、Story → `/pm/sprints`、项目 → `/pm/projects`。

#### Scenario: 项目条目链接到项目页

- **GIVEN** WorkbenchPage 我的项目列出项目 id=9
- **WHEN** 页面渲染完成
- **THEN** 该项目条目 SHALL 是链接，href 指向 `/pm/projects`

### Requirement: Sider 导航壳增强（工作台入口 + 折叠 + 收起）

前端 SHALL 在 Sider 顶部提供「工作台」菜单组（含「我的工作台」→ `/`）；品牌「Rainier」SHALL 链接到 `/`；每个菜单组标题(`appshell-group-${key}`) SHALL 可点击折叠/展开其子项；并 SHALL 提供开关(`appshell-sider-toggle`)收起/展开整个 Sider。

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

## ADDED Requirements (from change 2026-06-12-requirement-enrich / v0.0.19)

### Requirement: RequirementsPage 新状态 / 五级优先级 / 期望交付日期

前端 SHALL 在 RequirementEditDrawer 状态下拉提供新 6 态中文(草稿/审批中/分析中/实施中/已交付/已关闭)、优先级下拉提供 5 级中文(含「最低」)、并提供「期望交付日期」输入(`req-expected-date`),提交透传 `expectedDate`。RequirementsPage 状态/优先级列 SHALL 中文化。

#### Scenario: 状态下拉为新 6 态中文

- **GIVEN** 用户打开 RequirementEditDrawer
- **WHEN** 抽屉渲染
- **THEN** 状态下拉 SHALL 含「草稿」「审批中」「分析中」「实施中」「已交付」「已关闭」
- **AND** SHALL 不含旧值中文化标签（评审中/已批准/已废弃）

#### Scenario: 优先级下拉含「最低」

- **GIVEN** RequirementEditDrawer 已渲染
- **WHEN** 查看优先级下拉
- **THEN** SHALL 含 5 个选项,包括「最低」

#### Scenario: 提交携带 expectedDate

- **GIVEN** 用户在新建抽屉填妥必填项并填「期望交付日期」= "2026-09-01"
- **WHEN** 点击保存
- **THEN** SHALL 调用 `createRequirement` 且 body 含 `expectedDate: "2026-09-01"`

### Requirement: demand/story/task 优先级含最低

前端 SHALL 在 demand/story/task 页的优先级下拉提供 5 级中文(含「最低」)。

#### Scenario: TasksPage 优先级下拉含最低

- **GIVEN** 用户打开 TasksPage 新建任务抽屉
- **WHEN** 查看优先级下拉
- **THEN** SHALL 含「最低」选项
