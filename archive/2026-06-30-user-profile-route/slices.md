# v0.0.118-user-profile-route 切片执行计划

| # | 优先级 | TC 覆盖 | 实现目标 | 依赖 |
|---|--------|---------|---------|------|
| 1 | P0 | TC-UPROF-001, TC-UPROF-002 | 新增 `/users/:id/profile` route + `UserProfilePage` + `getUserProfile` | 无 |
| 2 | P0 | TC-UPROF-003 | 抽取 `ProfileView` 后保持 `/profile` 行为不变 | 1 |
| 3 | P1 | TC-UPROF-004 | 确认下属链接目标被真实 route 消费 | 1 |
