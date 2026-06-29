# Test Report — story-review-ui (v0.0.81)

## Backend
- 命令: `cd backend && mvn test`
- 总计: **716 tests, 0 failures, 0 errors, 0 skipped** — BUILD SUCCESS
- 新增: `StoryUpdateNoReviewerWipeTest` (3 cases)
  - TC-SRU-001 put_omitsReviewerFields_keepsExistingReviewer
  - TC-SRU-002 put_explicitNullReviewerFields_clearsReviewer
  - TC-SRU-003 put_explicitNewReviewer_replaces

## Frontend
- 命令: `cd frontend && npm test -- --run`
- 总计: **272 tests, 0 failures**
- 新增: 2 cases in `StoryEditDrawer.test.tsx`
  - TC-SRU-004a omits reviewerUserId key when user did not touch reviewer
  - TC-SRU-004b sends explicit null when user clears reviewer

## Caveats
- StoryReviewTest 的 TC-REVQ-009 "update_setsReviewFields" 仍然 pass —
  既有 PUT 显式传 reviewer 的语义未受影响。
- create 路径 (POST) 未改 — 仍是全量字段；只有 PUT (update) 进入 patch-like 分支。
- 其他实体 (Task / Sprint / Requirement) 的同类 PUT 全量替换问题不在本 sub-change 范围。
