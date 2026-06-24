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
> - 2026-06-17 (v0.0.22-board-kit) — NEW shared dashboard 词汇 `components/board/*`: `StatusBar`(纯 CSS 分布/比例条)、`DashboardCard`、`StatusChip`(后加 `tier` prop)、`OwnerChip`、`EmptyState`、`StatTiles`(KPI 磁贴) + `utils/board.ts`(`statusColor`/`groupByStatus`/`isOverdue`/`todayISO`/`rygToTier`/`RYG_LABEL`)+ 4 档状态色 token。
> - 2026-06-17 (v0.0.23-pm-cockpit) — `pages/Cockpit` + 路由 `/pm/cockpit` + 需求管理组首项「项目驾驶舱」(全员)。单项目 5 类状态分布 + 逾期/风险 + 待办(Task TODO/IN_PROGRESS)/待评审(Story DRAFT/READY)行内状态快改;EmptyState 无项目;切项目防竞态(active flag)。
> - 2026-06-17 (v0.0.25-team-lead) — `utils/ryg.ts`(`ryg`/`loadTier`/`isOpenTaskStatus`/`RYG_ORDER`)+ `pages/TeamLead` + 路由 `/team` + 工作台组「团队负责人面板」(全员)。成员负载阈值条 + 项目红黄绿(v0.0.29 起改用 `scope=led`)。
> - 2026-06-17 (v0.0.26-demand-lite) — `pages/Demand/DemandSubmitPage` + 路由 `/demand-submit` + 工作台组「提个诉求」。极简单表单(主题必填/描述可选/优先级默认中,提交人只读)复用 createDemand,成功确认卡 + 再提一个。
> - 2026-06-17 (v0.0.29-portfolio-consumers) — Cockpit 顶部跨项目健康条(`getPortfolio('mine')`,点卡钻入);TeamLead 项目红黄绿改用 `getPortfolio('led')`(团队足迹,修 mis-scope);`StatusChip` +`tier` prop / `rygToTier`/`RYG_LABEL`;`api/portfolio.ts`。
> - 2026-06-17 (v0.0.30-portfolio-map) — `pages/Portfolio` + 路由 `/portfolio` + 新全员「数据看板」组「项目地图」。scope 切换 我的/我带的/全公司,健康磁贴 + 项目列表(worst-first,RYG chip + 下钻)。
> - 2026-06-17 (v0.0.31-link-panel) — `api/link.ts` + `components/LinkPanel`(极简外链:类型 chip + url + 删除 + 一行新增 + 计数),接入 TaskEditDrawer(TASK)/StoryEditDrawer(STORY)编辑既有实体时显示。见 [[entity-link]]。
> - 2026-06-17 (v0.0.32-workbench-focus) — 工作台「我的任务」改今日聚焦:逾期→今天→有期限→无期限分桶(已完成沉底)+ 优先级 tiebreak + 逾期/今天 chip。
> - 2026-06-17 (v0.0.33→37 UI 翻新) — 设计系统:`.rainier-select`(紧凑原生 select,取代撑满行的 treeselect-trigger)、`.rainier-page`/`.rainier-page-head`(限宽 1240 + 标题栏不换行)、`.rainier-list-table`(行分隔/悬停)、全局蓝色链接、Card 轻量化(细边框+轻投影+18/20 内边距)、状态色/边框 token。**侧边栏**每项线性图标(`NavIcon`,选中变蓝)。**全局**内容限宽(`.rainier-shell-content`)+ CRUD 表格去「卡中卡」(`.rainier-table` 去自带阴影圆角,本就在 Card 内)+ 顶栏细边框轻量化。**工作台**重做(KPI 磁贴 + 两栏)。**全部 16 个 CRUD 页**统一标题栏(`.rainier-page-head` h2 + 新建/筛选移右)+ Card 只剩 Table+Pagination + 状态列上 `StatusChip` + 页过滤 select 改 `.rainier-select`(多代理 workflow 统一改,testid 全保留)。`StatTiles` KPI 磁贴用于工作台/项目地图。
> - 2026-06-18 (v0.0.39-review-queue) — `pages/Reviews/ReviewsPage`（标题「我的评审」）+ 路由 `/reviews` + 全员「数据看板」组第 2 项「评审看板」(icon `check`,/portfolio 旁,**不入 isAdminPath**)。消费 `GET /api/me/pending-reviews`:StatTiles(待评数) + 待评 Story 列表(优先级 StatusChip + 提交人 OwnerChip + 标题纯文本[无 Story 详情路由,避死链] + 通过/打回 `Button`)调 `POST /api/stories/{id}/review` 后 refetch + EmptyState。新 `api/reviews.ts`(getPendingReviews/submitReview)。navGuardConsistency 自动钉 /reviews 为全员。见 [[entity-story]]。
> - 2026-06-18 (v0.0.40-me-profile) — `pages/Profile/ProfilePage`（标题「我的档案」）+ 路由 `/profile` + 全员「工作台」组第 2 项「我的档案」(icon `badge`,在「我的工作台」之后,**不入 isAdminPath**)。消费 `GET /api/me/profile`:身份 DashboardCard(OwnerChip + 岗位 + 直接上级) + 贡献 StatTiles(我负责的 Story 数/分配给我的任务数) + 组织身份列表(org 名 + 类型中文 + 角色 StatusChip + 主组织标记) + 无组织 EmptyState。新 `api/profile.ts`(getMyProfile)。navGuardConsistency 自动钉 /profile 为全员。见 [[me-profile]]。
> - 2026-06-18 (v0.0.41-admin-compliance) — `pages/Compliance/CompliancePage`（标题「合规仪表盘」）+ 路由 `/sys/compliance` + **admin**「系统」组第 2 项「合规仪表盘」(icon `gauge`,审计日志旁,经 `/sys` 前缀 isAdminPath 门控)。消费 `GET /api/compliance/audit-summary` + `/residual-permissions`:审计 StatTiles(事件总量 + 停用-残留权限用户数,残留>0 标红) + 停用-残留权限对账表(停用用户 + 角色数 + 角色名 + EmptyState) + 按动作/按实体类型分布 + 最近活动表。新 `api/compliance.ts`(getAuditSummary/getResidualPermissions)。navGuardConsistency 自动钉 /sys/compliance 为 admin。见 [[admin-compliance]]。
> - 2026-06-18 (v0.0.42-po-inbox) — `pages/Inbox/InboxPage`（标题「需求收件箱」）+ 路由 `/inbox` + 全员「工作台」组第 3 项「需求收件箱」(icon `inbox`,我的档案之后,**不入 isAdminPath**)。消费 `GET /api/me/inbox`:StatTiles(待处理诉求数/我的需求数) + 待处理诉求列表(优先级 chip + 标题→/pm/demands + 状态) + 我的需求列表(状态 chip + code+标题→/pm/requirements + 优先级 + 期望日期 + projectName) + 各区 EmptyState。新 `api/inbox.ts`(getInbox)。复用 PRIORITY_LABELS/REQUIREMENT_STATUS_LABELS。navGuardConsistency 自动钉 /inbox 为全员。见 [[me-inbox]]。
> - 2026-06-18 (v0.0.43-ai-work-log) — `pages/AiWorkLog/AiWorkLogsPage`（标题「AI 工作日志」）+ 路由 `/ai/work-logs` + **新顶级「AI」全员导航组**(icon `loop`,**不入 isAdminPath**)。消费 `GET /api/ai-work-logs`:StatTiles(待裁决/已采纳/已驳回) + 状态过滤 + 提议列表(agentType+action+summary+evidence + 状态 chip;PROPOSED 行带 采纳/驳回 `Button`,驳回经 window.prompt 取理由)调 `POST /api/ai-work-logs/{id}/decision` 后 refetch + EmptyState。新 `api/aiWorkLog.ts`(listAiWorkLogs/decideAiWorkLog)。navGuardConsistency 自动钉 /ai/work-logs 为全员。飞轮层底座。见 [[ai-work-log]]。

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

## ADDED Requirements (from change 2026-06-18-review-queue / v0.0.39)

### Requirement: 「我的评审」落地页

前端 SHALL 在 `/reviews` 提供「我的评审」页（all-users），消费 `GET /api/me/pending-reviews`：渲染待评审计数
（StatTiles）+ 待评 Story 列表（每行 优先级 StatusChip + 提交人 OwnerChip + 标题 + 通过/打回 按钮）；空时显示
EmptyState。「通过/打回」按钮 SHALL 调 `POST /api/stories/{id}/review` 后刷新列表。

#### Scenario: 渲染待评审列表

- **GIVEN** `GET /api/me/pending-reviews` 返回 2 条待评 Story
- **WHEN** 用户打开 `/reviews`
- **THEN** 页面 SHALL 渲染这 2 条 Story
- **AND** SHALL 渲染待评审计数为 2

#### Scenario: 通过评审后刷新

- **GIVEN** `/reviews` 已渲染 1 条待评 Story
- **WHEN** 用户点击该行「通过」按钮
- **THEN** 前端 SHALL 调用 `submitReview(storyId, "APPROVED")`
- **AND** SHALL 重新拉取 pending-reviews 列表

#### Scenario: 空队列

- **GIVEN** `GET /api/me/pending-reviews` 返回空数组
- **WHEN** 用户打开 `/reviews`
- **THEN** 页面 SHALL 显示 EmptyState（无待评审）

### Requirement: 评审看板导航入口（all-users）

前端 SHALL 在 AppLayout「数据看板」组加入「评审看板」入口指向 `/reviews`，且 `/reviews` SHALL NOT 被 `isAdminPath`
门控（普通用户可达）。`AppRoutes` SHALL 注册 `/reviews` 路由。

#### Scenario: /reviews 为 all-users

- **WHEN** 检查 `isAdminPath('/reviews')`
- **THEN** SHALL 返回 false

#### Scenario: 路由已注册

- **WHEN** 在 `/reviews` 挂载 AppRoutes
- **THEN** SHALL 渲染「我的评审」页（reviews 容器可见）

## ADDED Requirements (from change 2026-06-18-me-profile / v0.0.40)

### Requirement: 「我的档案」落地页

前端 SHALL 在 `/profile` 提供「我的档案」页（all-users），消费 `GET /api/me/profile`：渲染 身份卡（姓名/岗位/
登录名）+ 贡献 StatTiles（我负责的 Story 数 / 分配给我的任务数）+ 组织身份列表（组织名 + 类型 + 角色 chip + primary
标记）+ 直接上级（OwnerChip）；无组织关系时该区块显示 EmptyState。

#### Scenario: 渲染身份与贡献

- **GIVEN** `GET /api/me/profile` 返回 `{name:"Alice", positionName:"后端工程师", ownedStoryCount:3, assignedTaskCount:5, memberships:[...], manager:{name:"Bob"}}`
- **WHEN** 用户打开 `/profile`
- **THEN** 页面 SHALL 显示 "Alice" 与 "后端工程师"
- **AND** SHALL 显示贡献磁贴 Story=3 / Task=5
- **AND** SHALL 显示直接上级 "Bob"

#### Scenario: 组织关系列表

- **GIVEN** profile.memberships 含 1 项 `{organizationName:"采购小队", role:"MEMBER", isPrimary:true}`
- **WHEN** `/profile` 渲染完成
- **THEN** SHALL 显示 "采购小队" 与其角色标记

### Requirement: 我的档案导航入口（all-users）

前端 SHALL 在 AppLayout「工作台」组加入「我的档案」入口指向 `/profile`，且 `/profile` SHALL NOT 被 `isAdminPath`
门控（普通用户可达）。`AppRoutes` SHALL 注册 `/profile` 路由。

#### Scenario: /profile 为 all-users

- **WHEN** 检查 `isAdminPath('/profile')`
- **THEN** SHALL 返回 false

#### Scenario: 路由已注册

- **WHEN** 在 `/profile` 挂载 AppRoutes
- **THEN** SHALL 渲染「我的档案」页（profile 容器可见）

## ADDED Requirements (from change 2026-06-18-admin-compliance / v0.0.41)

### Requirement: 「合规仪表盘」页（admin）

前端 SHALL 在 `/sys/compliance` 提供「合规仪表盘」页（admin），消费 `GET /api/compliance/audit-summary` +
`GET /api/compliance/residual-permissions`：渲染 审计 StatTiles（事件总量 + 停用-残留权限用户数，残留>0 标红）+
停用-残留权限对账表（停用用户 + 角色数 + 角色名，空时 EmptyState）+ 按动作/按实体类型分布 + 最近活动表。

#### Scenario: 渲染审计聚合与残留对账

- **GIVEN** audit-summary 返回 `{total:5, byAction:[{label:"CREATE",count:3}], recent:[1 条]}`，residual 返回 1 个停用用户 ghost（DEV）
- **WHEN** 用户打开 `/sys/compliance`
- **THEN** SHALL 显示审计总量 5
- **AND** SHALL 显示残留行含 "ghost" 与 "DEV"
- **AND** SHALL 显示按动作 "CREATE"

#### Scenario: 无残留 → 空态

- **GIVEN** residual-permissions 返回空数组
- **WHEN** 用户打开 `/sys/compliance`
- **THEN** SHALL 显示 EmptyState（无残留）

### Requirement: 合规仪表盘导航入口（admin）

前端 SHALL 在 AppLayout「系统」组加入「合规仪表盘」入口指向 `/sys/compliance`；`/sys/compliance` SHALL 被
`isAdminPath` 门控（经 `/sys` 前缀，仅 admin 可达）。`AppRoutes` SHALL 注册 `/sys/compliance` 路由。

#### Scenario: /sys/compliance 为 admin

- **WHEN** 检查 `isAdminPath('/sys/compliance')`
- **THEN** SHALL 返回 true

#### Scenario: 路由已注册

- **WHEN** 在 `/sys/compliance` 挂载 AppRoutes
- **THEN** SHALL 渲染「合规仪表盘」页（compliance 容器可见）

## ADDED Requirements (from change 2026-06-18-po-inbox / v0.0.42)

### Requirement: 「需求收件箱」落地页

前端 SHALL 在 `/inbox` 提供「需求收件箱」页（all-users），消费 `GET /api/me/inbox`：渲染 StatTiles（待处理诉求数 /
我的需求数）+ 待处理诉求列表（标题 + 优先级 chip + 状态，链接到 `/pm/demands`）+ 我的需求列表（code+标题 + 状态 chip +
优先级 + 期望日期，链接到 `/pm/requirements`）；两区各自空态 EmptyState。

#### Scenario: 渲染两区

- **GIVEN** inbox 返回 1 条待处理诉求 + 1 条我的需求
- **WHEN** 用户打开 `/inbox`
- **THEN** SHALL 显示待处理诉求与我的需求各 1 行
- **AND** SHALL 显示计数磁贴

#### Scenario: 空收件箱

- **GIVEN** inbox 返回两区皆空
- **WHEN** 用户打开 `/inbox`
- **THEN** SHALL 显示两区的 EmptyState

### Requirement: 需求收件箱导航入口（all-users）

前端 SHALL 在 AppLayout「工作台」组加入「需求收件箱」入口指向 `/inbox`，且 `/inbox` SHALL NOT 被 `isAdminPath`
门控。`AppRoutes` SHALL 注册 `/inbox` 路由。

#### Scenario: /inbox 为 all-users

- **WHEN** 检查 `isAdminPath('/inbox')`
- **THEN** SHALL 返回 false

#### Scenario: 路由已注册

- **WHEN** 在 `/inbox` 挂载 AppRoutes
- **THEN** SHALL 渲染「需求收件箱」页（inbox 容器可见）

## ADDED Requirements (from change 2026-06-18-ai-work-log / v0.0.43)

### Requirement: 「AI 工作日志」落地页

前端 SHALL 在 `/ai/work-logs` 提供「AI 工作日志」页（all-users），消费 `GET /api/ai-work-logs`：渲染 StatTiles
（待裁决/已采纳/已驳回）+ 状态过滤 + 日志列表（agentType + action + summary + evidence + 状态 chip；PROPOSED 行带
采纳/驳回 按钮，调 `POST /api/ai-work-logs/{id}/decision` 后刷新）+ EmptyState。

#### Scenario: 渲染日志与裁决

- **GIVEN** `GET /api/ai-work-logs` 返回 1 条 PROPOSED 日志
- **WHEN** 用户打开 `/ai/work-logs`
- **THEN** SHALL 显示该日志（agentType/summary/evidence）
- **AND** SHALL 显示采纳/驳回按钮

#### Scenario: 采纳后刷新

- **GIVEN** 列表含 1 条 PROPOSED 日志
- **WHEN** 用户点击「采纳」
- **THEN** SHALL 调用 decideAiWorkLog(id, "ACCEPTED")
- **AND** SHALL 重新拉取列表

#### Scenario: 空列表

- **GIVEN** `GET /api/ai-work-logs` 返回空
- **WHEN** 用户打开 `/ai/work-logs`
- **THEN** SHALL 显示 EmptyState

### Requirement: AI 导航组（all-users）

前端 SHALL 在 AppLayout 新增「AI」顶级导航组（all-users），含「AI 工作日志」指向 `/ai/work-logs`；
`/ai/work-logs` SHALL NOT 被 `isAdminPath` 门控。`AppRoutes` SHALL 注册 `/ai/work-logs` 路由。

#### Scenario: /ai/work-logs 为 all-users

- **WHEN** 检查 `isAdminPath('/ai/work-logs')`
- **THEN** SHALL 返回 false

#### Scenario: 路由已注册

- **WHEN** 在 `/ai/work-logs` 挂载 AppRoutes
- **THEN** SHALL 渲染「AI 工作日志」页（ai-work-logs 容器可见）

<!-- merged from change 2026-06-18-customer-flow / v0.0.44 (客户全流程：商机看板只读 + 售前/实施流转操作页 + 运营看板 + 客户导航组) -->

### Requirement: 「商机看板」只读进展总览

> 2026-06-23（v0.0.44）：看板拆为「只读总览」，所有流转操作移到独立操作页（见下两条 Requirement）。

前端 SHALL 在 `/crm/opportunities` 提供 **只读** 的「商机看板」页（all-users；受众=监控角色，待定），消费
`GET /api/opportunities`（size 上限 100）：faithful 还原客户全流程图的两段泳道 —— **售前环节**（线索/商机/推介POC/
投标/合同签订）与 **实施环节**（立项/现场调研/产品诉求/交付实施/验收）各为一个 phase band（带负责人标注），band 内按
节点分列；关口列（商机/投标/合同/立项）标 ⭐。卡片含 客户/标题/金额/负责人/赢单标识。顶部 StatTiles：进行中/赢单/丢单
+ 在谈金额。OPEN+WON 在 band 内显示，LOST 仅滚动进 tile。看板 SHALL NOT 提供任何流转操作控件（无新建/推进/关口决策）。

#### Scenario: 只读渲染两段泳道

- **GIVEN** `GET /api/opportunities` 返回若干商机
- **WHEN** 用户打开 `/crm/opportunities`
- **THEN** SHALL 渲染「售前环节」「实施环节」两个 phase band，band 内按节点分列渲染只读卡片
- **AND** SHALL NOT 渲染任何操作按钮（新建/推进/通过/否决）

### Requirement: 「售前流转」操作页

前端 SHALL 在 `/crm/presale-flow` 提供「售前流转」操作页（all-users），消费 `GET /api/opportunities`（size 上限 100），
列出 `status=OPEN ∧ stage∈售前环节` 的商机为操作表：每行含 阶段 / 客户·标题 / 金额 / 负责人 / 操作。非关口节点（线索/
推介POC）SHALL 提供「推进」（`advance` 无 decision）；关口节点（商机/投标/合同）SHALL 提供「通过」（PASS）/「否决」
（REJECT→丢单）。SHALL 提供「新建商机」侧拉抽屉（客户/标题/金额 + 四负责人下拉 + 必填校验）。WON/LOST/实施 商机 SHALL NOT 出现在此页。

#### Scenario: 售前操作 + 新建

- **GIVEN** `GET /api/opportunities` 返回若干 OPEN 售前商机（含关口与非关口）
- **WHEN** 用户打开 `/crm/presale-flow`
- **THEN** SHALL 按阶段为每行渲染对应操作（关口→通过/否决，非关口→推进）
- **AND** SHALL 提供「新建商机」抽屉入口；WON/LOST/实施 商机 SHALL NOT 出现

### Requirement: 「实施流转」操作页

前端 SHALL 在 `/crm/delivery-flow` 提供「实施流转」操作页（all-users），消费 `GET /api/opportunities`（size 上限 100），
列出 `status=WON ∧ stage∈实施环节` 的商机为操作表：每行含 阶段 / 客户·标题 / 项目经理 / 关联Project / 操作。立项
（INITIATION）SHALL 提供「立项移交」（侧拉抽屉选交付 Project → `POST /api/opportunities/{id}/initiate` PASS 链 projectId）
与「通过」（PASS→现场调研）/「否决」（停在立项）；非关口（现场调研/产品诉求/交付实施）SHALL 提供「推进」；验收
（ACCEPTANCE）SHALL 为终态「已验收」无操作。

#### Scenario: 实施操作 + 立项移交

- **GIVEN** `GET /api/opportunities` 返回若干 WON 实施商机
- **WHEN** 用户在 INITIATION 行点「立项移交」选择一个 Project 并确认
- **THEN** SHALL 调 `initiate(id, projectId, 'PASS')` 链入交付 Project
- **AND** 验收（ACCEPTANCE）行 SHALL NOT 渲染任何推进操作

### Requirement: 「运营看板」落地页

前端 SHALL 在 `/crm/operations` 提供「运营看板」页（all-users），消费 `GET /api/operations`：按 3 节点分列 + 新建 + 推进。

#### Scenario: 渲染运营看板

- **GIVEN** `GET /api/operations` 返回若干运营单
- **WHEN** 用户打开 `/crm/operations`
- **THEN** SHALL 按节点分列渲染

### Requirement: 客户导航组（all-users）

前端 SHALL 在 AppLayout 新增「客户」顶级导航组（all-users），含 4 项：「商机看板」→`/crm/opportunities`、「售前流转」
→`/crm/presale-flow`、「实施流转」→`/crm/delivery-flow`、「运营看板」→`/crm/operations`；`/crm/*` SHALL NOT 被
`isAdminPath` 门控。`AppRoutes` SHALL 注册这 4 条 /crm 路由。

#### Scenario: /crm/* 为 all-users

- **WHEN** 检查 `isAdminPath('/crm/opportunities')`、`isAdminPath('/crm/presale-flow')`、`isAdminPath('/crm/delivery-flow')`
- **THEN** SHALL 全部返回 false

## MODIFIED / ADDED Requirements (v0.0.45 gate-artifacts)

> 合并自 change `2026-06-23-gate-artifacts`（Phase 6）。完整 fold-in（详情默认只读+编辑切换 / 富文本预览 MarkdownView /
> 添加产出物 / 推进时补充 / 链接类无标题·多份 / 客户管理页）记录见 `archive/2026-06-23-gate-artifacts/test-report.md`。

### Requirement: 「售前流转」产出物表单

> v0.0.45 增量：要求产出物的转换，操作按钮先弹产出物表单，提交即流转。

「售前流转」中，对来源阶段在 `OPP_TRANSITION_ARTIFACT` 内的行：线索行「推进」SHALL 弹《商机调研报告》表单（标题+正文），
商机行「通过」/「否决」SHALL 弹《决策评审纪要》表单（标题+正文，记录对应 decision）；提交 SHALL 调
`advanceOpportunity(id, decision?, note?, artifact)` 即「建产出物+流转」；标题/正文空 SHALL 表单报错且不提交。多产出物门禁
（推介POC→投标）SHALL 弹「补充产出物并推进」表单：链接类可多份且无需标题、报告类填正文（标题可空），提交即逐条建缺失产出物后推进。

#### Scenario: 线索推进弹报告表单

- **GIVEN** 售前流转有一条 LEAD 商机
- **WHEN** 用户点该行「推进」并填《商机调研报告》标题+正文后提交
- **THEN** SHALL 调 `advanceOpportunity(id, undefined, …, {title,content})`

#### Scenario: 商机通过弹纪要表单

- **GIVEN** 售前流转有一条 OPPORTUNITY 商机
- **WHEN** 用户点该行「通过」并填《决策评审纪要》后提交
- **THEN** SHALL 调 `advanceOpportunity(id, 'PASS', …, {title,content})`

### Requirement: 「商机看板」产出物查看 + 导出 Word

「商机看板」（只读）SHALL 为每个商机提供「产出物」入口，打开只读抽屉列出该商机产出物（消费
`GET /api/opportunities/{id}/artifacts`），每条提供「导出 Word」（带鉴权拉取 `.../export` 的 .docx 并下载）。看板 SHALL
仍不提供任何**流转**操作控件（无新建/推进/通过/否决）。

#### Scenario: 看板查看产出物并导出

- **GIVEN** 某商机有产出物
- **WHEN** 用户在商机看板点该商机「产出物」
- **THEN** SHALL 打开抽屉列出产出物
- **AND** 每条 SHALL 提供「导出 Word」入口；看板 SHALL NOT 渲染流转操作按钮（新建/推进/通过/否决）

### Requirement: 客户管理页（all-users）

> v0.0.45 fold-in：客户实体（见 [[customer]]）配套管理页 + 导航。

前端 SHALL 在「客户」导航组新增「客户管理」→`/crm/customers`（CRUD 页，消费 `/api/customers`），导航组由 4 项扩为 5 项；
`AppRoutes` SHALL 注册 `/crm/customers`。

#### Scenario: 客户管理为 all-users

- **WHEN** 检查 `isAdminPath('/crm/customers')`
- **THEN** SHALL 返回 false

## MODIFIED Requirements (from change 2026-06-23-contract-artifacts / v0.0.46)

> 合并自 change `2026-06-23-contract-artifacts`（Phase 6）。售前流转 BIDDING/CONTRACT 补充表单路由。见 [[opportunity-artifact]]。

### Requirement: 售前流转 投标/合同 关口补充产出物

`opportunityArtifact.ts` SHALL 注册 6 个新类型（标签/链接类）并在 `STAGE_REQUIRED_ARTIFACTS` 新增 `BIDDING:[BID_DOCUMENT]`
与 `CONTRACT:[中标公示,合同,评审会议纪要,邮件归档,已盖章合同]`。售前流转中 BIDDING/CONTRACT 行点「通过」SHALL 走「补充产出物并
推进」表单（缺则按 kind 填链接/正文，链接类可多份、无需标题；齐则直接推进）；点「否决」SHALL 跳过补充表单、直接确认丢单
（产出物仅 PASS 需要）。

#### Scenario: 投标通过弹补充表单

- **GIVEN** 售前流转有一条 BIDDING 商机，无产出物
- **WHEN** 用户点该行「通过」
- **THEN** SHALL 弹出补充表单，含《投标文件》链接输入（可多份）
- **AND** SHALL NOT 直接调 advance

#### Scenario: 投标否决直接确认丢单（不弹补充表单）

- **GIVEN** 售前流转有一条 BIDDING 商机，无产出物
- **WHEN** 用户点该行「否决」
- **THEN** SHALL 弹「确认否决（丢单）」对话框
- **AND** SHALL NOT 弹补充产出物表单

#### Scenario: 合同补充五件后推进

- **GIVEN** 售前流转有一条 CONTRACT 商机，无产出物
- **WHEN** 用户点「通过」、在补充表单填 中标公示/合同/邮件归档/已盖章合同(链接) + 评审会议纪要(正文) 后「提交并推进」
- **THEN** SHALL 逐条 `createOpportunityArtifact` 后 `advanceOpportunity(id, 'PASS')`

## MODIFIED Requirements (from change 2026-06-23-board-redesign / v0.0.47)

> 合并自 change `2026-06-23-board-redesign`（Phase 6）。商机看板改版（P0+P1+P2）。看板仍只读。见 [[opportunity]]。

### Requirement: 商机看板改版 — 泳道带 / 漏斗 / 过滤 / 列表 / 停留预警

商机看板 SHALL 以**上下两条相位泳道带**（售前 / 实施）呈现，每带含其 5 个阶段列（不再为单条 10 列横条）。看板 SHALL
提供：顶部 10 阶段**漏斗分布条**（计数 + 关口标记）；按 **负责人 / 产品 / 客户名** 过滤 + **丢单磁贴**切换可见；
**看板 / 列表** 视图切换（列表可按 金额 / 阶段 / 停留天数 排序）；卡片金额 SHALL 格式化（万/亿/千分位）、**整卡可点**打开
只读产出物抽屉（不再每卡一个按钮）、按 `stageEnteredAt` 渲染**停留预警点**（绿/黄/红/灰）。看板 SHALL 保持只读（无新建/推进/关口控件）。

#### Scenario: 两条相位泳道带渲染

- **GIVEN** 商机列表加载完成
- **WHEN** 看板渲染（board 视图）
- **THEN** SHALL 呈现 `opp-phase-presale` 与 `opp-phase-delivery` 两带
- **AND** SHALL 含 `opp-col-LEAD`…`opp-col-ACCEPTANCE` 各阶段列

#### Scenario: 漏斗分布条显示阶段计数

- **GIVEN** 若干商机分布于不同阶段
- **WHEN** 看板渲染
- **THEN** `opp-funnel` SHALL 出现，且每阶段 `opp-funnel-{STAGE}` SHALL 显示该阶段在谈计数

#### Scenario: 金额格式化与整卡可点

- **GIVEN** 一条 `amount=2000000` 的商机
- **WHEN** 看板渲染
- **THEN** 其卡片 SHALL 显示 `¥200万`（非裸数字）
- **AND** SHALL 无独立 `opp-artifacts-{id}` 按钮；点击 `opp-card-{id}` SHALL 打开只读产出物抽屉

#### Scenario: 按负责人 / 产品 / 客户名过滤

- **GIVEN** 看板含不同负责人 / 产品 / 客户名的商机
- **WHEN** 选择 `opp-filter-owner`、`opp-filter-product` 或在 `opp-filter-q` 输入客户名子串（忽略大小写）
- **THEN** SHALL 仅保留匹配该过滤条件的卡片

#### Scenario: 丢单磁贴切换可见

- **GIVEN** 存在 LOST 商机，默认不在列中
- **WHEN** 点击 `opp-tile-lost`
- **THEN** SHALL 将 LOST 商机纳入展示并标 LOST chip

#### Scenario: 看板/列表切换与排序

- **WHEN** 点击 `opp-view-list`
- **THEN** SHALL 呈现列表视图 `opp-list`；`opp-list-sort` 选「按金额」或「按停留天数」时 `opp-list-row-{id}` SHALL 按该键降序（空值末位）
- **AND** 点击 `opp-view-board` SHALL 切回泳道带

#### Scenario: 停留预警点按天数分级

- **GIVEN** 距今 3 / 10 / 20 天及无 `stageEnteredAt` 的商机
- **WHEN** 看板渲染
- **THEN** 各卡片 `opp-dwell-{id}` SHALL 分别为 green / yellow / red / gray

## MODIFIED Requirements (from change 2026-06-23-project-types / v0.0.48)

> 合并自 change `2026-06-23-project-types`（Phase 6）。ProjectsPage 四类型 + DeliveryFlow 立项「创建或关联对外-交付」。见 [[entity-project]] / [[opportunity]]。

### Requirement: ProjectsPage 四种项目类型 + 共享类型常量

`api/project.ts` SHALL 导出共享 `PROJECT_TYPE_OPTIONS`(=[CASUAL,CORE_FEATURE,CORE_TECH,EXTERNAL_DELIVERY]) 与 `PROJECT_TYPE_LABELS`
(轻量/主业-功能建设/主业-技术改造/对外-交付)。ProjectsPage 新建/编辑类型下拉与列表标签 SHALL 用该共享常量、呈现 4 个中文选项。

#### Scenario: 类型下拉显示四类

- **WHEN** 打开 ProjectsPage 新建抽屉
- **THEN** `projects-type-select` SHALL 含 轻量/主业-功能建设/主业-技术改造/对外-交付 四个选项

### Requirement: DeliveryFlow 立项「关联或新建对外-交付项目」

DeliveryFlow 立项抽屉 SHALL **默认「新建」模式**（立项主流动作 = 为本次赢单新建一个交付项目；该默认 SHALL NOT 依赖既有项目数量
——真实使用中对外-交付项目会很多，按数量判断会长期埋没「新建」）。「关联已有」SHALL 作为次要选项一键可达。关联模式 SHALL 只列
`EXTERNAL_DELIVERY` 项目；新建模式 SHALL 收 code/name + **项目负责人**（默认当前用户/商机 PM，类型固定显示「对外-交付」）。提交 SHALL 调
`initiateOpportunity`（关联传 projectId / 新建传 projectCode+projectName+projectOwnerUserId）；失败 SHALL 展示后端 message。

#### Scenario: 默认进入新建模式（与项目数量无关）

- **GIVEN** 任意数量的 EXTERNAL_DELIVERY 项目（含 0 个或很多个）
- **WHEN** 打开立项抽屉
- **THEN** SHALL 直接呈现新建表单（code/name/负责人），无需额外切换点击
- **AND** 「关联已有」SHALL 作为次要选项一键可切

#### Scenario: 立项新建对外-交付项目

- **GIVEN** 一个 INITIATION/WON 商机
- **WHEN** 在新建表单填 code/name/负责人、点移交
- **THEN** SHALL 以 `{projectCode, projectName, projectOwnerUserId, decision:'PASS'}` 调 `initiateOpportunity`

#### Scenario: 立项关联已有对外-交付项目

- **GIVEN** 一个 INITIATION/WON 商机与一个 EXTERNAL_DELIVERY 项目
- **WHEN** 切到「关联已有」、选该项目、点移交
- **THEN** SHALL 以 `{projectId, decision:'PASS'}` 调 `initiateOpportunity`

#### Scenario: 立项失败展示后端原因

- **GIVEN** 新建项目编号已存在
- **WHEN** 提交立项
- **THEN** SHALL 展示后端返回的 message（而非通用错误串）

## MODIFIED Requirements (from change 2026-06-24-project-code-autogen / v0.0.49)

> 合并自 change `2026-06-24-project-code-autogen`（Phase 6）。创建/立项表单去掉项目编号输入（编号自动生成）。见 [[entity-project]]。

### Requirement: 创建项目与立项表单去掉编号输入

ProjectsPage 新建/编辑抽屉 SHALL NOT 含项目编号输入框（编号自动生成）；列表/详情 SHALL 仍只读展示自动编号。DeliveryFlow 立项
「新建」表单 SHALL 仅含 名称 + 项目负责人（去掉编号输入），提交 SHALL 调 `initiateOpportunity({projectName, projectOwnerUserId, decision})`。

#### Scenario: 新建项目抽屉无编号输入

- **WHEN** 打开 ProjectsPage 新建抽屉
- **THEN** SHALL NOT 出现编号输入框；类型/名称/负责人输入 SHALL 仍在

#### Scenario: 立项新建只需名称与负责人

- **GIVEN** 一个 INITIATION/WON 商机
- **WHEN** 在立项新建表单填 名称 + 负责人、点移交
- **THEN** SHALL 以 `{projectName, projectOwnerUserId, decision:'PASS'}` 调 `initiateOpportunity`（无 projectCode、无编号输入）

## MODIFIED Requirements (from change 2026-06-24-task-status-i18n / v0.0.50)

> 合并自 change `2026-06-24-task-status-i18n`（Phase 6）。任务状态展示中文。

### Requirement: 任务状态展示中文

`api/task.ts` SHALL 导出共享 `TASK_STATUS_LABELS`（TODO=待办 / IN_PROGRESS=进行中 / DONE=已完成 / BLOCKED=阻塞 /
CANCELLED=已取消）与 `TASK_STATUS_OPTIONS`。TasksPage 列表状态、TaskEditDrawer/工作台/驾驶舱 的任务状态下拉 SHALL 显示中文标签；
下拉提交值 SHALL 仍为英文枚举（后端契约不变）。

#### Scenario: 任务列表显示中文状态

- **GIVEN** 一个 status=TODO 的任务
- **WHEN** TasksPage 渲染
- **THEN** 状态 chip SHALL 显示「待办」，SHALL NOT 显示「TODO」

#### Scenario: 状态下拉显示中文、提交英文值

- **WHEN** 打开任务编辑抽屉的状态下拉
- **THEN** 选项 SHALL 显示中文（待办/进行中/已完成/阻塞/已取消）
- **AND** 选中提交的 value SHALL 仍为对应英文枚举

## MODIFIED Requirements (from change 2026-06-24-customer-page-redesign / v0.0.51)

### Requirement: 客户管理页卡片网格视觉

客户页 SHALL 以响应式卡片网格呈现客户：每卡含 首字母头像（按名字取色）+ 客户名 + 行业标签（缺省「未填行业」）+ 联系人 + 备注 + 编辑/删除。
SHALL 在标题旁显示「共 N 家」计数、空态用 EmptyState。搜索/新建/编辑/删除的行为与 testid SHALL 保持不变（纯展示层改版）。

#### Scenario: 卡片网格渲染客户

- **GIVEN** 若干客户
- **WHEN** 客户页渲染
- **THEN** SHALL 出现 `customers-grid`，每个客户一张 `customer-card-{id}`（含名称文本）

#### Scenario: CRUD 入口不回归

- **WHEN** 客户页渲染
- **THEN** `customers-new-btn` SHALL 在；卡片 SHALL 含 `customer-edit-{id}` / `customer-delete-{id}`
- **AND** 新建/编辑抽屉 SHALL 保留 `customer-name`/`customer-save` 等输入与提交

## MODIFIED Requirements (from change 2026-06-24-handoff-advance / v0.0.52)

### Requirement: DeliveryFlow 立项行 = 立项移交（即推进）+ 驳回

DeliveryFlow「立项(INITIATION)」行 SHALL 仅含 **立项移交**（主操作，关联/新建项目并由后端推进到现场调研）+ **驳回**（REJECT，停留）；
SHALL NOT 再有独立「通过」按钮（其会绕过项目直接推进、不正确）。立项移交后刷新 SHALL 见商机进入「现场调研」。

#### Scenario: 立项行无独立「通过」

- **GIVEN** 一个 立项 商机
- **WHEN** DeliveryFlow 渲染该行
- **THEN** SHALL 含 `delivery-handoff-{id}` 与 `delivery-reject-{id}`
- **AND** SHALL NOT 含 `delivery-pass-{id}`

## MODIFIED Requirements (from change 2026-06-24-survey-artifacts / v0.0.53)

### Requirement: DeliveryFlow 现场调研推进 = 补充产出物表单

DeliveryFlow「现场调研(SURVEY)」行的「推进」SHALL 在缺必需产出物（《现场调研报告》《现场调研附件》）时打开「补充产出物并推进」表单：报告填标题+正文、附件填链接（可多份），提交 SHALL 逐个建档后再 advance；产出物齐备 SHALL 直接 advance。其它实施环节（产品诉求/交付）SHALL 仍为无门禁直接「推进」。

#### Scenario: 现场调研行推进打开补充表单

- **GIVEN** DeliveryFlow 渲染一个 现场调研(SURVEY) 商机行（无已存产出物）
- **WHEN** 点击 `delivery-advance-{id}`
- **THEN** SHALL 打开补充表单（含 `delivery-supp-SURVEY_REPORT` 与 `delivery-supp-SURVEY_ATTACHMENT`）
- **AND** SHALL NOT 立即调用 advance

#### Scenario: 补充表单提交逐个建档并推进

- **GIVEN** 补充表单已打开，报告填了正文、附件填了链接
- **WHEN** 点击 `delivery-supp-save`
- **THEN** SHALL 为每类产出物调用 `createOpportunityArtifact`
- **AND** SHALL 随后调用 `advanceOpportunity(id, undefined)` 并刷新列表

#### Scenario: 产品诉求行仍直接推进

- **GIVEN** DeliveryFlow 渲染一个 产品诉求(REQUIREMENT) 商机行
- **WHEN** 点击 `delivery-advance-{id}`
- **THEN** SHALL 直接调用 `advanceOpportunity(id, undefined)`（不打开补充表单）

## MODIFIED Requirements (from change 2026-06-24-delivery-detail / v0.0.54)

### Requirement: DeliveryFlow 行可查看商机详情 + 流转产出物

DeliveryFlow 每个实施中商机行 SHALL 含「详情」按钮（`delivery-detail-{id}`），点击 SHALL 打开详情抽屉（`delivery-detail-body`）：
只读展示商机字段（客户·标题/阶段/状态/备注/金额/产品/四负责人），可切换「编辑」改字段并经 `updateOpportunity` 保存后刷新；
产出物区 SHALL 列出该商机全部流转产出物（报告类可预览/导出 Word、链接类可打开），并可「添加产出物」。推进操作 SHALL 仍在行上（抽屉不含推进按钮）。

#### Scenario: 行有详情入口并打开抽屉

- **GIVEN** DeliveryFlow 渲染一个实施中商机行
- **WHEN** 点击 `delivery-detail-{id}`
- **THEN** SHALL 打开 `delivery-detail-body`，展示该商机的客户·标题与阶段中文标签
- **AND** SHALL 调 `listOpportunityArtifacts` 加载其产出物

#### Scenario: 详情列出流转产出物

- **GIVEN** 一个有 SURVEY_REPORT + SURVEY_ATTACHMENT 产出物的商机详情已打开
- **THEN** SHALL 出现 `delivery-detail-artifact-{aid}`，报告类 SHALL 有 `delivery-detail-export-{aid}`、链接类 SHALL 有 `delivery-detail-link-{aid}`

#### Scenario: 详情编辑保存

- **GIVEN** 详情抽屉已打开
- **WHEN** 点「编辑」改标题、点 `delivery-detail-save`
- **THEN** SHALL 以新值调 `updateOpportunity` 并刷新列表

#### Scenario: 推进不在抽屉内

- **WHEN** 详情抽屉渲染
- **THEN** SHALL NOT 含推进/立项移交/驳回按钮（这些仍在行上）

## MODIFIED Requirements (from change 2026-06-24-opportunity-detail-page / v0.0.55)

### Requirement: 统一商机详情页 /crm/opportunities/:id

系统 SHALL 提供路由 `/crm/opportunities/:id` 的商机详情页：按 `:id` 经 `getOpportunity` 拉取商机并渲染 **概览**（客户/标题/阶段中文/状态/金额/产品/四负责人/关联项目/备注/最近决策人）+ 可「编辑」(updateOpportunity) + **流转产出物**列表（报告类预览/导出 Word、链接类打开）+「添加产出物」+「返回」。售前流转、实施流转 行「详情」SHALL 跳转到该页（不再用抽屉）。推进/门禁 SHALL 仍在列表行上，不在详情页。

#### Scenario: 详情页按 id 加载商机与产出物

- **GIVEN** 存在商机 id=7
- **WHEN** 访问 `/crm/opportunities/7`
- **THEN** SHALL 调 `getOpportunity(7)` 与 `listOpportunityArtifacts(7)`
- **AND** SHALL 渲染 `opp-detail-page`，显示其客户·标题与阶段中文标签

#### Scenario: 详情页编辑保存

- **GIVEN** 详情页已加载
- **WHEN** 点「编辑」改标题并保存
- **THEN** SHALL 以新值调 `updateOpportunity` 并刷新展示

#### Scenario: 详情页添加产出物

- **GIVEN** 详情页已加载
- **WHEN** 打开「添加产出物」填内容并保存
- **THEN** SHALL 调 `createOpportunityArtifact` 并重新拉取产出物列表

#### Scenario: 售前流转行详情跳转

- **GIVEN** 售前流转渲染一行商机 id=5
- **WHEN** 点击 `presale-detail-5`
- **THEN** SHALL 导航到 `/crm/opportunities/5`（SHALL NOT 打开抽屉）

#### Scenario: 实施流转行详情跳转

- **GIVEN** 实施流转渲染一行商机 id=7
- **WHEN** 点击 `delivery-detail-7`
- **THEN** SHALL 导航到 `/crm/opportunities/7`（SHALL NOT 打开抽屉）

### Requirement: CRM 商机页面视觉优化（卡片化，复用设计令牌）

商机看板 / 商机详情页 / 售前流转 / 实施流转 SHALL 采用统一卡片化视觉，复用 `global.css` 设计令牌（颜色/圆角/阴影/状态色）与 board 组件（StatusChip/StatTiles）：详情页以卡片承载概览(头像+客户·标题+阶段/状态 chip)与流转产出物；售前/实施流转以行卡片(hover-lift)取代朴素表格；看板的漏斗条/阶段泳道/列表以卡片承载。所有既有 data-testid SHALL 保持不变（行为不回归）。

#### Scenario: 流转页行卡片渲染且 testid 不回归

- **GIVEN** 售前/实施流转有商机行
- **WHEN** 页面渲染
- **THEN** 行 SHALL 以卡片(`oppflow-card`)呈现
- **AND** `presale-row-{id}`/`delivery-row-{id}`/`*-detail-{id}`/推进类按钮 testid SHALL 保持存在

#### Scenario: 详情页卡片化概览

- **GIVEN** 访问 `/crm/opportunities/:id`
- **THEN** SHALL 以卡片承载概览(含 StatusChip 阶段/状态)与流转产出物，且 `opp-detail-*` testid 不变

## MODIFIED Requirements (from change 2026-06-25-opp-requirement-gen / v0.0.56)

### Requirement: 商机详情页据调研+产品助手式生成诉求/需求

商机详情页 SHALL 提供「生成产品诉求/需求」动作：打开草稿表单，**客户端预填**（无 LLM）标题=`客户·标题`、描述=聚合 现场调研报告正文 + 现场调研附件链接 + 产品名 + 来源商机标注；SHALL 可切换「提交为 诉求(Demand) / 需求(Requirement)」并可编辑；提交 SHALL 以 `createDemand`/`createRequirement` 创建并带 `opportunityId`。详情页 SHALL 列出本商机已生成的诉求/需求（按 opportunityId 拉取）。

#### Scenario: 生成草稿预填调研+产品

- **GIVEN** 商机有 现场调研报告/附件 与 产品名
- **WHEN** 点「生成产品诉求/需求」
- **THEN** SHALL 打开 `opp-gen-form`，描述预填含调研正文与产品名

#### Scenario: 提交为诉求

- **GIVEN** 草稿已打开、目标=诉求
- **WHEN** 编辑后点提交
- **THEN** SHALL 调 `createDemand`，body 含 `opportunityId` 与编辑后的标题/描述

#### Scenario: 切换为需求提交

- **GIVEN** 草稿已打开
- **WHEN** 切到「需求」并提交
- **THEN** SHALL 调 `createRequirement`，body 含 `opportunityId`

#### Scenario: 已生成区列出派生项

- **GIVEN** 商机已派生 1 诉求 + 1 需求
- **WHEN** 详情页渲染
- **THEN** SHALL 出现 `opp-gen-list`，含该诉求与需求

## MODIFIED Requirements (from change 2026-06-25-delivery-acceptance-gate / v0.0.57)

### Requirement: DeliveryFlow 交付实施推进 = 补充甲方验收报告

DeliveryFlow「交付实施(DELIVERY)」行的「推进」SHALL 在缺《甲方验收报告》时打开「补充产出物并推进」表单（报告类，填标题+正文），提交 SHALL 建档后调 `advanceOpportunity(id)`；报告已存在时直接 advance。验收 (ACCEPTANCE) 行仍为只读「已验收」。

#### Scenario: 交付实施行推进打开补充表单

- **GIVEN** DeliveryFlow 渲染一个 交付实施(DELIVERY) 商机行（无报告）
- **WHEN** 点 `delivery-advance-{id}`
- **THEN** SHALL 打开补充表单（含 `delivery-supp-DELIVERY_ACCEPTANCE_REPORT`）
- **AND** SHALL NOT 直接调 advance

#### Scenario: 补充表单提交后推进到验收

- **GIVEN** 补充表单已打开，已填正文
- **WHEN** 点 `delivery-supp-save`
- **THEN** SHALL 调 `createOpportunityArtifact({type:"DELIVERY_ACCEPTANCE_REPORT", title, content})`
- **AND** SHALL 调 `advanceOpportunity(id)` 推进到 ACCEPTANCE 并刷新列表
