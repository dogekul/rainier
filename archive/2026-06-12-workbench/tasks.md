# v0.0.18-workbench 任务清单

## auth-placeholder (后端 MOD)
- [ ] W01 UserRepository.findByLoginName + UserRoleRepository.findByUserId
- [ ] W02 MeResponse 扩展 + MeRole/MeProject + MeService + AuthController.me
- [ ] W04 backend 测试 TC-ME-001..004

## entity-story (后端 MOD)
- [ ] W03 StoryService/StoryController list +ownerUserId
- [ ] W04 backend 测试 TC-STORY-OWN-001

## frontend-scaffold (前端 MOD)
- [ ] W05 api/auth(MeResponse) + api/story(ownerUserId) + store/auth(AuthUser)
- [ ] W06 WorkbenchPage(替换 Home) + AppRoutes + WorkbenchPage.test TC-FES-WB-001..003

## E2E
- [ ] W07 docker 重建 + me 上下文 + story owner + 存量不变 TC-E2E-WB-001
