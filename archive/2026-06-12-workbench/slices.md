# v0.0.18-workbench 切片计划

| # | TC | 目标 | 依赖 |
|---|----|------|------|
| W01 | — | `UserRepository.findByLoginName` + `UserRoleRepository.findByUserId` | 无 |
| W02 | TC-ME-001..003 | `MeResponse` 扩展(+ 嵌套 MeRole/MeProject) + `MeService.forUsername`(join 富化 + 降级) + `AuthController.me` 改用 | W01 |
| W03 | TC-STORY-OWN-001 | `StoryService.list` + `StoryController.list` 加 `ownerUserId` | 无(可并行) |
| W04 | TC-ME-001..004 + TC-STORY-OWN-001 | backend 测试(AuthMeContextTest + StoryController owner 过滤用例) | W02,W03 |
| W05 | — | 前端 `api/auth.ts` MeResponse 富类型 + `api/story.ts` StoryListParams +ownerUserId + `store/auth.ts` AuthUser 扩展 | 无(可并行) |
| W06 | TC-FES-WB-001..003 | `WorkbenchPage`(me + 我的任务含状态快改 + 我的 Story + 我的项目) 替换 Home;`AppRoutes` 引用;`WorkbenchPage.test` | W05 |
| W07 | TC-E2E-WB-001 | docker 重建 + GET /me 上下文 + story owner 过滤 + 19 表/存量不变 | W01-W06 |

## 拓扑批次
- 批次1: W01 ‖ W03 ‖ W05
- 批次2: W02
- 批次3: W04 ‖ W06
- 批次4: W07

## 陷阱
- me 降级: findByLoginName 空 → id=null/roles=[]/projects=[],不抛(TC-ME-003)。
- org 级角色 projectId=null → roles 含但 projects 去重时跳过 null(TC-ME-002)。
- 批量富化 findAllById(role/project)避 N+1。
- AuthUser 新字段全可选,不破 login 调用点。
- 状态快改: 从 TaskDetail 构造 TaskUpdate 全字段 + 新 status(task update 要求全量)。
- WorkbenchPage me.id 可空 → 各区块 guard(null 不发查询)。
- 既有 App/AppRoutes 测试: Home→WorkbenchPage 换路由组件,grep 校对断言。
- 契约 K: 后端 MeResponse 字段名 ↔ 前端 MeResponse 类型;story ownerUserId param 名一致。
