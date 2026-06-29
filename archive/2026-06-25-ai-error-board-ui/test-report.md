# Test Report — ai-error-board-ui (A9, v0.0.73)

## 新增文件
- `frontend/src/api/aiErrors.ts` — `listAiErrors(params)` / `fixAiError(id, fixAction)` + types `AiError` / `AiErrorStatus` + `AI_ERROR_STATUS_LABELS`.
- `frontend/src/pages/AiError/AiErrorsPage.tsx` — AI 错误公示板列表页。
  - 列：`occurredAt · aiAction` / `errorDesc` / `affectedEntityType#id` / `StatusChip(status)`.
  - 状态过滤：全部 / 待修复(OPEN) / 已修复(FIXED).
  - 「标记修复」按钮（仅 `isElevated(currentUser)` 为 true 时显示，对 OPEN 行有效）→ Drawer 输入修复说明（必填、非空校验）→ 调 `fixAiError`，成功后 reload。
- `frontend/src/pages/AiError/index.ts` — default export.
- `frontend/src/pages/AiError/AiErrorsPage.test.tsx` — 3 个 vitest 测试。

## 修改文件
- `frontend/src/components/AppLayout.tsx` — 「AI」组追加 `{ to: '/ai/errors', label: '错误公示板', icon: 'shield' }`。AI 组非 requiresAdmin，all-users 可见。
- `frontend/src/AppRoutes.tsx` — `import AiErrorsPage from './pages/AiError'` + `<Route path="/ai/errors" element={<AiErrorsPage />} />`。

## 未新增表 / DDL
后端无任何变更（A4 v0.0.68 已就位）。

## 测试通过数
- frontend: **269 tests pass / 56 files**（全量 `npm test -- --run`，含本次新增 3 tests: TC-AIEP-01/02/03）。
- backend: 未改动，未运行（A4 已经 611 tests 全绿）。
- `navGuardConsistency.test.tsx` 32 tests 全绿 → 新路由 `/ai/errors` 在 AI 组（非 requiresAdmin）与 `isAdminPath` 默认（不在 ADMIN_PATH_PREFIXES）保持一致。

## Caveats
- 没有创建「单条错误详情页」（spec OutOfScope；列表展示已覆盖所有字段）。
- 没有推送通知；用户主动来公示板查看（spec OutOfScope）。
- 错误录入仍依赖后端 `AiErrorService.record(...)`，前端不提供录入 UI（A4 / A9 一致 OutOfScope）。
- 复用 `frontend/src/pages/AiWorkLog/AiWorkLogsPage.css`（共享 `.ai-list / .ai-row*` 样式，无需新增 CSS 文件）。
