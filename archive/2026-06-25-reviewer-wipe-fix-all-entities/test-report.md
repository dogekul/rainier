# Test Report — reviewer-wipe-fix-all-entities (G2)

## 范围

无 production 代码变更；仅归档调研结论 + 验证既有 `TaskUpdateNoReviewerWipeTest` 仍绿。

## 既有测试

`backend/src/test/java/com/rainier/task/controller/TaskUpdateNoReviewerWipeTest.java`（v0.0.82 引入）

```
mvn test -Dtest=TaskUpdateNoReviewerWipeTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

3 用例：
- `put_omitsReviewerFields_keepsExistingReviewer` ✅
- `put_explicitNullReviewerFields_clearsReviewer` ✅
- `put_explicitNewReviewer_replaces` ✅

## Sprint / Requirement 跳过依据

- `SprintUpdateRequest`：无 reviewerUserId / reviewStatus 字段（已 Read 验证）
- `RequirementUpdateRequest`：无 reviewerUserId / reviewStatus 字段（已 Read 验证）
- grep `reviewerUserId|reviewStatus` 在 sprint/ + requirement/ 包内 0 命中

→ 无 wipe 风险面，无需添加防御代码或测试。

## Caveats

- C1 test-report Caveats:4 中 "Task PUT 同款 bug" → **已 close**（C2 v0.0.82 顺手完成 + 本 change 再确认）
- Sprint/Requirement 若未来增加 reviewer 字段，须遵循 spec.md "未来扩展守则" 一节
- 前端无改动
