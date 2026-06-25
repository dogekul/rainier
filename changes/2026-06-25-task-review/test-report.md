# Test Report: task-review (v0.0.82)

## Backend
- `mvn test` — **725 tests run, 0 failures, 0 errors, 0 skipped**.
- 新增/相关：
  - `TaskReviewTest`：5/5 通过 (TC-TREV-001..004 + unknown-task 404)
  - `TaskUpdateNoReviewerWipeTest`：3/3 通过 (TC-TREV-005 三场景)
  - `MePendingReviewsTaskMergeTest`：1/1 通过 (TC-TREV-006)
- 既有 `TaskListSqlCountTest`、`StoryReviewTest`、`MePendingReviewsControllerTest` 等仍绿；
  PendingReview DTO 新增 `kind/taskId` 字段未破坏既有断言。

## Frontend
- `npm test -- --run` — **56 files, 274 tests pass**。
- `ReviewsPage.test.tsx`：5/5（含新 Task tab 用例 TC-TREV-007 / TC-TREV-008）。
  既有 row-testid 由 `reviews-row-{id}` 调整为 `reviews-row-{S|T}-{id}` 以区分 kind，
  对应 3 个老用例同步更新。

## 覆盖映射
| TC | 类型 | 文件 | 状态 |
|----|----|------|----|
| TC-TREV-001 | backend | TaskReviewTest.create_withReviewer_returns201Enriched | PASS |
| TC-TREV-002 | backend | TaskReviewTest.review_approved_setsStatusKeepsReviewer | PASS |
| TC-TREV-003 | backend | TaskReviewTest.review_rejectedRequiresReason | PASS |
| TC-TREV-004 | backend | TaskReviewTest.review_invalidDecision_returns400 | PASS |
| TC-TREV-005 | backend | TaskUpdateNoReviewerWipeTest (3 cases) | PASS |
| TC-TREV-006 | backend | MePendingReviewsTaskMergeTest | PASS |
| TC-TREV-007 | frontend | ReviewsPage.test approves a task | PASS |
| TC-TREV-008 | frontend | ReviewsPage.test rejects a task with reason | PASS |

## Caveats
- Task 评审 REJECTED 的 reason 复用 `closeReason` 字段（未引入 `review_reason` 列）。若后续需
  与 Task 关闭原因区分，可加 nullable `review_reason` 字段——本期 OutOfScope。
- TaskListSqlCountTest 仍锁 6 条 prepared statement：reviewer 与 assignee 共用同一 user batch，
  无新增 batch。
- Task review 状态变化对 task.status 无联动（独立 review track，按 OutOfScope）。
