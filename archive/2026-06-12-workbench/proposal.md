# v0.0.18-workbench — 当前用户上下文 + 一线「我的工作台」

> Baseline: tag `v0.0.17-milestone` / commit ac280fd. backend 336 + frontend 66 测试 green, 19 表.
> 来源: 角色卡 `A-角色意图卡片.md` —— 转向「按角色使用逻辑完善功能」的第一步(地基 + 一线工作台).

## Why

系统现在 = 一堆 admin 风格 CRUD 表 + 占位首页(`Home` = "Hello, {username} v0 占位")。登录后**每个角色看到的都一样**;
`GET /api/auth/me` 只回 `{username}`,前端**不知道当前用户的 id / 角色 / 项目**。17 张角色卡的价值核心是每个角色的
**专属工作台(每日钩子)**,而这一切的地基是「我是谁 → 我的活」。本版补这个地基 + 最该先做的**一线「我的工作台」**
(服务开发/测试工程师 —— 数据采集关键人群,「少用」是核心目标)。纯实体聚合,不依赖 AI。

## What Changes

- 后端 `GET /api/auth/me` 扩成返回**当前用户上下文**:`{id, username, name, roles:[{roleId,roleCode,roleName,projectId,projectName,projectCode}], projects:[{id,code,name}]}`(由 username 解析 User + join UserRole/Role/Project)。
- 后端 `story` list 加 `ownerUserId` 过滤(让「我的 Story」可查;task list 已有 `assigneeUserId`)。
- 前端 `auth` store 的 `AuthUser` 扩成带 `id/name/roles/projects`;`api/auth.ts` MeResponse 类型同步。
- 前端占位 Home(`/`)换成**「我的工作台」WorkbenchPage**:问候 + 我的角色 chips + **我的任务**(assignee=我，含状态快改) + **我的 Story**(owner=我) + **我的项目**(来自我的 roles)。

## Capabilities

### Modified Capabilities

- `auth-placeholder`:`GET /api/auth/me` 由 `{username}` 扩成完整当前用户上下文(id/name/roles/projects)。
- `entity-story`:list 加 `ownerUserId` 过滤参数。
- `frontend-scaffold`:占位 Home 换成 WorkbenchPage「我的工作台」(我的任务含状态快改 / 我的 Story / 我的项目);`AuthUser` 扩展。

### New Capabilities

- 无(纯扩展;0 新表 / 0 新包)。

## Impact

**代码层面**:
- 后端:`UserRepository`(+findByLoginName) / `UserRoleRepository`(+findByUserId) / `MeResponse`(扩展 + 嵌套 MeRole/MeProject DTO) / 新 `MeService`(组装上下文) / `AuthController.me`(改用 MeService) / `StoryController`+`StoryService`(list +ownerUserId)。
- 前端:`api/auth.ts`(MeResponse 类型) / `api/story.ts`(StoryListParams +ownerUserId) / `store/auth.ts`(AuthUser 扩展) / `pages/Home`→`WorkbenchPage`(+test) / `AppRoutes`(Home 引用)。
- **配置/基础设施**:无新依赖、无新表、无新端点(me 扩返回、story 加 param)。

## 显式排除(往后 / 飞轮层)

- AI 能力:健康分、风险雷达、状态自动同步(commit/PR)、日报/周报草稿、派单建议。
- 其它角色的仪表盘(PM 驾驶舱、团队健康面板、公司项目地图…)。
- 角色化导航门禁 / 权限收口(Sider 按角色裁剪)。
- 真实身份提供方(me 仍基于 mock JWT 的 username)。

## Success Criteria

- [ ] `GET /api/auth/me` 返回 `{id, username, name, roles[], projects[]}`;无有效 token → 401。
- [ ] roles 来自当前用户的 user-role 行(含 roleCode/roleName + projectId/projectName,project 级与 org 级(projectId null)都返回)。
- [ ] projects 为当前用户参与项目的去重列表(来自其 user-roles)。
- [ ] `GET /api/stories?ownerUserId=X` 仅返回 owner=X 的 Story。
- [ ] 前端 `/` 渲染「我的工作台」:问候名 + 角色 chips + 我的任务(assignee=我) + 我的 Story(owner=我) + 我的项目。
- [ ] 我的任务可一键改 status(调用 task update,列表刷新)。
- [ ] backend 336+ / frontend 66+ 全绿 + tsc clean;E2E:me 返回上下文 + story owner 过滤 + 工作台数据正确;19 表不变;存量数据未改。
