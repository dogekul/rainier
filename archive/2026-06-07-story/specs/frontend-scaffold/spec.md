# Capability: frontend-scaffold

## MODIFIED Requirements (from change 2026-06-07-story)

### Requirement: RequirementsPage 行级 Story drilldown + storyCount 列

前端 SHALL 在 RequirementsPage 列表新增一列 `"Story 数"` 显示 `r.storyCount`；每行新增展开按钮，点击展开后渲染子区域 `StoryListPanel`，其中调用 `listStories({requirementId: r.id})` 取该 Requirement 下的 Stories，渲染为表格 + "新建 Story" 按钮 + 每行"编辑 / 删除"按钮。**不**新增独立 Sider 菜单项，**不**新增独立 `/pm/stories` 路由。

#### Scenario: RequirementsPage 表格含 Story 数 列

- **GIVEN** 用户已登录访问 `/pm/requirements`；mock `listRequirements` 返回 1 行 `{id: 1, code: "REQ-1", title: "X", storyCount: 3}`
- **WHEN** 页面渲染完成
- **THEN** 表格 SHALL 含一列标题 `"Story 数"`
- **AND** 该行 `"Story 数"` 单元格 SHALL 显示 `"3"`
- **AND** 该行 SHALL 渲染 `"展开"` 按钮

#### Scenario: 点开行渲染 StoryListPanel + 子表

- **GIVEN** mock `listStories({requirementId: 1})` 返回 2 行 Story `[{id:10, code:"STR-10", title:"S10", status:"DRAFT"}, {id:11, code:"STR-11", title:"S11", status:"IN_PROGRESS"}]`
- **WHEN** 用户点击 Requirement 行的展开按钮
- **THEN** 子区域 SHALL 渲染 `data-testid="story-list-panel-1"`
- **AND** 子表 SHALL 含 `"STR-10"` 与 `"STR-11"` 两行
- **AND** 子区域 SHALL 含 `"新建 Story"` 按钮（`data-testid="stories-new-btn"`）

### Requirement: StoryEditDrawer 新增/编辑 Story 抽屉

前端 SHALL 提供 `StoryEditDrawer` 组件用于新增 / 编辑 Story；新建路径从 `StoryListPanel` 的 "新建 Story" 按钮触发，传入 `requirementId`；编辑路径从子表行 "编辑" 按钮触发，回显 editing Story。新建抽屉「负责人」下拉 SHALL 默认选中当前登录用户（按 loginName 匹配 listUsers 池），编辑时 SHALL 不 disabled（可改，沿用 v0.0.8 模式）。

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
