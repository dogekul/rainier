# Test Report — opportunity-stage-activities (v0.0.90 D2)

## Backend
- `mvn test` 全绿：**Tests run: 809, Failures: 0, Errors: 0, Skipped: 0**.
- 新增测试 `StageActivityControllerTest` 8 用例覆盖 TC-SA-001..007 + addActivity 空标题边界：
  - 不存在的 opportunityId → 404
  - 无效 stageCode → 400
  - add + list 创建顺序
  - markDone 设置 completedAt
  - skip 不设置 completedAt
  - 已 DONE 再 markDone → 400
  - dashboard 同时返回 activities + 该 stage 的 artifacts (按 stageFrom 过滤)
  - addActivity 空标题 → 400
- 同步更新 `LegacyProductCategoryCleanupTest` 表数从 40 → 41（新增 `rainier_stage_activity`）。

## Frontend
- `npx tsc --noEmit` 通过；`npm test -- --run OpportunityDetailPage` 16/16 通过。
- 新增 `src/api/stageActivity.ts` + `src/pages/Crm/StageActivityPanel.tsx`，在 OpportunityDetailPage
  产出物区之后挂载（仅当 opp 已加载）。

## Caveats
- 活动模板（按 stage 自动填充常用项）未做 — 显式 OutOfScope。
- 活动 → Task 转化未做 — 显式 OutOfScope。
- StageActivityPanel 使用极简内联样式，未做 RWD 打磨（留待批 E 体验打磨）。
