# Capability: frontend-scaffold

## MODIFIED Requirements (from change 2026-06-08-sprint)

### Requirement: Sider「需求管理」组追加「Sprint」菜单项（v0.0.10 起 5 项）

前端 SHALL 在 AppLayout 左侧 Sider「需求管理」组的子项中追加「Sprint」一项，位于「项目」之后、「诉求」之前；点击跳转 `/pm/sprints`。保留其余 4 项不动。

#### Scenario: Sider「需求管理」组 5 项 + Sprint 第 2 位

- **GIVEN** 用户已登录访问 `/`
- **WHEN** 页面渲染完成
- **THEN** Sider「需求管理」组 SHALL 含 5 项：`"项目"`、`"Sprint"`、`"诉求"`、`"需求"`、`"诉求-需求关联"`
- **AND** Sprint 项 SHALL 位于 项目 项之后、诉求 项之前
- **AND** 点击「Sprint」SHALL 跳转 `/pm/sprints`

### Requirement: /pm/sprints 路由 + SprintsPage CRUD

前端 SHALL 在 `AppRoutes.tsx` 注册 `/pm/sprints` 路由；SprintsPage 提供列表 + 新建 + 编辑 + 删除；每行可展开 → 复用 v0.0.9 `StoryListPanel` 但传入 `sprintId` 而非 `requirementId`。

#### Scenario: /pm/sprints 路由直接访问

- **GIVEN** 用户已登录
- **WHEN** 浏览器直接访问 `/pm/sprints`
- **THEN** SHALL 渲染 `SprintsPage` 组件
- **AND** `grep -c "/pm/sprints" frontend/src/AppRoutes.tsx` SHALL ≥ 1（防止 linter 静默回退）

#### Scenario: SprintsPage 行展开渲染 StoryListPanel(sprintId)

- **GIVEN** mock `listSprints` 返回 1 行 `{id: 10, code: "SPR-A", name: "Phase 1", requirementId: 1, requirementCode: "REQ-1", requirementTitle: "登录流程", storyCount: 2}`；mock `listStories({sprintId: 10})` 返回 2 行 Story
- **WHEN** 用户在 SprintsPage 点 Sprint 10 行展开按钮
- **THEN** 子区域 SHALL 渲染 `data-testid="story-list-panel-10"`
- **AND** 子区域 SHALL 含 "新建 Story" 按钮（`data-testid="stories-new-btn"`）

### Requirement: SprintEditDrawer + 默认 owner

前端 SHALL 提供 `SprintEditDrawer` 组件用于新增 / 编辑 Sprint；新建抽屉「负责人」下拉 SHALL 默认选中当前登录用户（按 loginName 匹配 listUsers 池），编辑时 SHALL 不 disabled（可改，沿用 v0.0.8 / v0.0.9 模式）。「所属 Requirement」字段在新建/编辑两个模式下都 SHALL 锁定显示（不可改）。Drawer SHALL 在 owner 为空时显示表单错误（沿用 v0.0.8.1 Code-M7 模式）。

#### Scenario: 新建 Sprint 抽屉默认 owner = 当前登录用户

- **GIVEN** Auth store 中 `user.username="alice"`；mock `listUsers` 返回包含 `{id:1, loginName:"alice"}`；用户从 RequirementsPage drilldown 触发新建 Sprint，传入 `requirementId=1` + `requirementCode="REQ-1"` + `requirementTitle="登录流程"`
- **WHEN** 抽屉打开，listUsers promise 已 resolve
- **THEN** 「负责人」下拉的当前选中值 SHALL 等于 `1`
- **AND** 「Requirement」字段 SHALL 锁定显示 "登录流程（REQ-1）"，**不**可选择其它 Requirement

#### Scenario: 编辑 Sprint 抽屉 owner 可改 → 调用 updateSprint

- **GIVEN** 抽屉打开为编辑模式 `editing.ownerUserId=1`；mock listUsers 返回 `[{id:1, loginName:"alice"}, {id:2, loginName:"lili"}]`
- **WHEN** 用户切换下拉到 lili (id=2) 并点保存
- **THEN** 「负责人」下拉 SHALL 不 disabled
- **AND** mock `updateSprint` SHALL 被调用且参数 body.ownerUserId SHALL 等于 2

### Requirement: RequirementsPage drilldown 改为 SprintListPanel（v0.0.9 StoryListPanel 替换）

前端 SHALL 把 RequirementsPage 行展开渲染的子组件从 `StoryListPanel` 改为 `SprintListPanel`；「Story 数」列 SHALL 改为「Sprint 数」并 render `r.sprintCount`。SprintListPanel 子表展示 Sprint 行 (code/name/status/owner) + 「新建 Sprint」按钮 + 每行「编辑 Sprint」「删除 Sprint」按钮。**本期不再支持从 RequirementsPage 直接查看 Story**——Story drilldown 改为从 SprintsPage 进入。

#### Scenario: RequirementsPage 表格含 "Sprint 数" 列

- **GIVEN** mock `listRequirements` 返回 1 行 `{id: 1, code: "REQ-1", title: "X", sprintCount: 3}`（已无 storyCount 字段）
- **WHEN** 页面渲染完成
- **THEN** 表格 SHALL 含一列标题 "Sprint 数"
- **AND** 该行 "Sprint 数" 单元格 SHALL 显示 "3"
- **AND** 表格 SHALL **不**含 "Story 数" 列

#### Scenario: 点开 Requirement 行渲染 SprintListPanel

- **GIVEN** mock `listSprints({requirementId: 1})` 返回 2 行 `[{id:10, code:"SPR-A"}, {id:11, code:"SPR-B"}]`
- **WHEN** 用户点击 Requirement 1 行展开按钮
- **THEN** 子区域 SHALL 渲染 `data-testid="sprint-list-panel-1"`
- **AND** 子表 SHALL 含 "SPR-A" 与 "SPR-B" 两行
- **AND** 子区域 SHALL 含 "新建 Sprint" 按钮（`data-testid="sprints-new-btn"`）

### Requirement: StoryEditDrawer 锁定字段改 Sprint（v0.0.9 Requirement 替换）

前端 SHALL 把 `StoryEditDrawer` 的锁定显示字段从 Requirement 改为 Sprint；props 接收 `sprintId` / `sprintCode` / `sprintName` 替代 `requirementId` / `requirementCode` / `requirementTitle`；保存时调用 `createStory({sprintId, ...})` 或 `updateStory(id, {...})`。Drawer 锁定区显示二段信息：`"<sprintName>（<sprintCode>）— 创建时锁定"`，并在其下方以 readonly 形式显示 `"通过 Sprint 归属于需求：<requirementTitle>（<requirementCode>）"` （信息来自 props，可选展示）。

#### Scenario: 新建 Story 抽屉锁定 Sprint 字段

- **GIVEN** 抽屉以新建模式打开，传入 `sprintId=10, sprintCode="SPR-A", sprintName="Phase 1"`；mock `listUsers` 已 resolve
- **WHEN** 用户填写 code / title 后点保存
- **THEN** 「Sprint」字段 SHALL 锁定显示 "Phase 1（SPR-A）— 创建时锁定"
- **AND** mock `createStory` SHALL 被调用且参数 body.sprintId SHALL 等于 10（**非** requirementId）

### Requirement: api/story.ts 与 api/requirement.ts 字段切换

前端 `api/story.ts` SHALL 把 `Story.requirementId` 字段改为 `Story.sprintId: number`；`Story.requirementCode` / `Story.requirementTitle` 保留（来自后端二段 join 富化）；新增 `Story.sprintCode` / `Story.sprintName`。`StoryCreate` / `StoryListParams` 参数同步切换。`api/requirement.ts` SHALL 把 `Requirement.storyCount` 字段移除，新增 `Requirement.sprintCount`。

#### Scenario: 类型切换无编译错误

- **GIVEN** v0.0.10 source tree
- **WHEN** `npx tsc -p tsconfig.json --noEmit`
- **THEN** SHALL 0 错误
- **AND** `grep -r 'requirementId' frontend/src/api/story.ts` SHALL 命中 0 行（字段已删）
- **AND** `grep -r 'storyCount' frontend/src/api/requirement.ts` SHALL 命中 0 行（字段已删）
