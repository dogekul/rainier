# v0.0.18-workbench — 技术设计

## Context

- `GET /api/auth/me` 现读 request attr `rainier.username`(SecurityFilter 由 JWT sub 注入)→ 返回 `MeResponse{username}`。
- `UserRepository` 无 `findByLoginName`;`UserRoleRepository` 无 `findByUserId`(有 countByUserId)。
- `UserRole{userId, roleId, projectId(nullable)}`;`Role{code,name}`;`Project{code,name}`;`User{loginName,name}`。
- `task` list 已支持 `assigneeUserId`;`story` list 仅 projectId/sprintId/status/priority(无 owner)。Story 有 `ownerUserId`(NN)。
- 前端 `AuthUser={username}`;`me()` 返回 `{username}`;占位 `Home`(路由 `/`)。
- 约束:纯实体聚合,无 AI;standing 不改存量。

## Decisions

### 1. me() 富版上下文(一次返回 id/name/roles/projects)

**方案**: 新 `MeService.forUsername(username)` → `userRepo.findByLoginName` 取 {id,name};`userRoleRepo.findByUserId` 取角色行;
批量 `roleRepo.findAllById` + `projectRepo.findAllById` 富化;组装 `MeResponse{id,username,name,roles[],projects[]}`。
`roles[]` = MeRole{roleId,roleCode,roleName,projectId,projectName,projectCode}(projectId 可空=org 级角色);`projects[]` =
非空 projectId 去重 {id,code,name}。`AuthController.me` 改调 MeService。

**为什么**: 「我是谁」是工作台地基,一次调用拿全上下文最省往返(D2 富版);批量 join 避 N+1。

**备选及排除**: 精简 me(只 id/name)+ 前端另查 user-roles —— 多往返、上下文散,排除。

### 2. me() 找不到 user 的兜底

**方案**: 若 `findByLoginName` 空(token 合法但库里无此 user,如 mock 'system'),返回 `{id:null, username, name:null, roles:[], projects:[]}`(不抛)。

**为什么**: token 已验签;username 可能不对应真实 User(mock/system)。降级为「有身份无上下文」,工作台显示空列表,不崩。

### 3. story list 加 ownerUserId 过滤

**方案**: `StoryService.list` 增 `Long ownerUserId` 参 + `cb.equal(root.get("ownerUserId"), ownerUserId)` predicate;`StoryController.list` 加 `@RequestParam(required=false) Long ownerUserId`。镜像既有 projectId/sprintId 过滤。

**为什么**: 「我的 Story」可查(D4 只补 story)。

### 4. 前端 AuthUser 扩展(向后兼容)

**方案**: `AuthUser` 扩成 `{username; id?; name?; roles?; projects?}`(username 必填,其余可选)。login 仍只 set `{username}`;WorkbenchPage 挂载调 `me()` 拿富上下文 → `setAuth(token, full)` + 本地渲染。`api/auth.ts` MeResponse 类型扩为富版。

**为什么**: 既有 login/读 user.username 调用点不破(可选字段);me() 升级为完整上下文。

### 5. WorkbenchPage「我的工作台」

**方案**: 路由 `/` 由 Home 换 WorkbenchPage。挂载 `me()` → 渲染:
- 问候(name ?? username)+ 角色 chips(roles:roleName@projectName / roleName(组织级))
- **我的任务**: `listTasks({assigneeUserId: me.id})` → 表格(title/status/project)+ 每行 status `<select>` 快改(D3:取该 TaskDetail 全字段 + 新 status → `updateTask` → 刷新)
- **我的 Story**: `listStories({ownerUserId: me.id})` → 表格(title/status)
- **我的项目**: `me.projects` → 列表(code/name)
- me.id 为 null(无上下文)→ 各区块空态,不报错。

**为什么**: D1 三块;D3 状态快改贴「少填表」;复用既有 list/update 端点(task assignee 已支持、story 本版补 owner)。

## Architecture

```
GET /api/auth/me ─► AuthController(attr username) ─► MeService.forUsername
   findByLoginName → {id,name}  (空→降级 id=null,roles/projects=[])
   userRoleRepo.findByUserId → rows ─► batch roleRepo+projectRepo 富化
   ─► MeResponse{id,username,name, roles[MeRole], projects[distinct MeProject]}

前端 / (WorkbenchPage) ─► me() ─► setAuth + 渲染
   我的任务  listTasks(assigneeUserId=me.id)  + 行内 status<select>→updateTask→refetch
   我的 Story listStories(ownerUserId=me.id)
   我的项目  me.projects

GET /api/stories?ownerUserId= ─► StoryService.list +ownerUserId predicate
```

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| me() username 无对应 User(mock 'system') | Decision 2 降级返回空上下文,工作台空态,不崩;测试覆盖该路径 |
| AuthUser 扩展破坏既有调用点 | 新字段全可选;username 仍必填;login 调用点不动 |
| 状态快改需 task 全量 update 载荷 | 从 TaskDetail(list 已返全字段)构造 TaskUpdate + 新 status;复用既有 update 校验 |
| me() N+1 富化 | 批量 findAllById(role/project),非逐行 |
| 既有 auth/story 测试回归 | me 扩字段(加不减,既有 username 断言不破);story 加可选 param(既有 list 测试不破) |
| 前端工作台依赖 me.id 为数字 | me.id 可空 → 各区块 guard(null 时不发查询,空态) |
