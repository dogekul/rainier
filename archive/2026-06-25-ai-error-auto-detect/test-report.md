# Test Report — ai-error-auto-detect (F5, v0.0.104)

## Backend
- `mvn test` from `/Users/dogekul/IdeaProjects/Rainier/backend`
- Result: **868 passed / 0 failed / 0 errors / 0 skipped** (BUILD SUCCESS)
- New: `AiErrorOverdueCountTest` (4 cases)
  - `countOverdueOpen_countsOnlyOpenAndOlderThanThreshold` — seed 5 rows, expect 2 OPEN&overdue
  - `countOverdueOpen_zeroOrNegativeHours_throws400`
  - `getOverdueCount_defaultsTo24h_andReturnsCountAndThreshold` — MockMvc, default param
  - `getOverdueCount_explicitHours_paramHonored` — hours=2 query param
- Coverage of F1 → AiError auto-record stays green via existing
  `AiWorkLogReverseControllerTest#reverse_restoresTask_revivesLog_andRecordsAiError`.

## Frontend
- `npm test -- --run` from `/Users/dogekul/IdeaProjects/Rainier/frontend`
- Result: **307 passed / 0 failed** (63 test files)
- New: `AiErrorOverdueBanner.test.tsx` (3 cases)
  - count > 0 → banner renders with the number and `/ai/errors` link
  - count === 0 → banner not in DOM
  - API rejects → silently swallowed, banner not in DOM
- Pre-existing `AppLayout.test.tsx` (13 cases) still green after banner injection.

## Caveats
- AiError 状态过期自动 close / 多级告警 / IM 推送告警 仍属 OutOfScope。
- Banner 轮询间隔硬编码 30s（与 NotificationBell 同 cadence），无 user-facing 调节入口。
- 阈值 24h 写死在前端常量 `THRESHOLD_HOURS`；若产品要灵活配置，下个迭代再开个 settings 表。
