# Test Report — H4 下属面板入口 (v0.0.111)

## 后端
- `cd backend && mvn test` — **Tests run: 900, Failures: 0, Errors: 0, Skipped: 0**.
- 新增 `MeSubordinatesControllerTest` (4 tests, all green):
  - TC-SUB-001 HEAD 用户返回组织下活跃成员（排除 self / leftAt 不为 null）
  - TC-SUB-002 非 HEAD 用户返回空数组
  - TC-SUB-003 未携带 token → 401
  - TC-SUB-004 HEAD 多个组织时跨组织聚合 + 去重

## 前端
- `cd frontend && npm test -- --run` — **Test Files 65 passed (65), Tests 316 passed (316)**。
- 新增 `SubordinatesPage.test.tsx` (3 tests, all green):
  - 渲染下属行 + 列 (姓名 / 主组织 / 本周完成 / 任务总数)
  - 空列表 → empty state
  - 「查看档案」按钮 href 指向 `/users/{id}/profile`

## 覆盖到的 Spec Scenario
- S1 / S4 ✓ 行渲染 + 列字段
- S2 ✓ 非 HEAD 空数组
- S3 ✓ 401
- S5 ✓ 跳转链接
- S6 — 由 AppLayout 的 useEffect + listLedTeams 长度判断实现；测试在 `AppLayout.test.tsx` 既有用例中通过（fetch 失败时默认 isHead=false，不破坏既有导航断言）。

## Caveats
- `/users/:id/profile` 前端路由本次未注册（按 OutOfScope 处理）— 后端 `GET /api/users/{id}/profile` (C3) 已经存在并做 authz 校验；当前点击会落到 `*` 兜底 → Workbench。后续 sub-change 应单独注册该前端路由。
- AppLayout 通过运行时 `GET /api/me/led-teams` 判断 isHead；非 HEAD 用户首次渲染时入口短暂不显示是预期行为（默认 false → fetch 后仍 false）。
