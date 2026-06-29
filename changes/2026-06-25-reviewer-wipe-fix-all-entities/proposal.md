# G2 — reviewer-wipe 修复扩展到 Task/Sprint/Requirement

## 背景

C1 (v0.0.81) 修了 **Story** PUT 全量替换误清 reviewer 字段的 bug：StoryUpdateRequest 加 `reviewerUserIdSet` / `reviewStatusSet` 内部 boolean，由 Jackson setter 自动置 true；StoryService.update 仅 `set=true` 时才覆盖。

C1 test-report Caveats 列出了一条遗留：**Task/Sprint/Requirement 的 PUT 路径同款 bug 仍存在**，需独立 sub-change 修复。这就是 G2 的初衷。

## 调查结论（务实范围裁定）

逐实体核对：

### Task — 已修复（v0.0.82 顺手完成）
- `TaskUpdateRequest` 已带 `reviewerUserId` + `reviewerUserIdSet` + `reviewStatus` + `reviewStatusSet`（Jackson setter 模式）
- `TaskService.update` 已实现 patch-like：
  ```java
  if (req.isReviewerUserIdSet()) { ... t.setReviewerUserId(req.getReviewerUserId()); }
  if (req.isReviewStatusSet())  { ... t.setReviewStatus(req.getReviewStatus()); }
  ```
- 测试 `TaskUpdateNoReviewerWipeTest`（3 用例）已存在并 GREEN：
  - `put_omitsReviewerFields_keepsExistingReviewer` ✅
  - `put_explicitNullReviewerFields_clearsReviewer` ✅
  - `put_explicitNewReviewer_replaces` ✅
- → C2 在引入 Task 评审字段时复用了 C1 模式，G2 范围内的 Task 工作 **已完成**。

### Sprint — 无 reviewer 字段，无 wipe 风险
- `SprintUpdateRequest` 字段：code/name/description/goal/status/ownerUserId/startDate/endDate
- 无任何 reviewer/review 字段 → PUT 不可能误清。
- Sprint 当前无评审需求；若未来给 Sprint 加 reviewer，须在那次需求中同步加 set 标志位（复用 C1/C2 模式）。

### Requirement — 无 reviewer 字段，无 wipe 风险
- `RequirementUpdateRequest` 字段：code/title/description/status/priority/complexity/projectId/ownerUserId/closeReason/expectedDate
- 无任何 reviewer/review 字段 → PUT 不可能误清。
- 同上，未来给 Requirement 加 reviewer 时同步加保护。

## 结论

G2 在调研后是 **no-op 归档**：C1 Caveats:4 提出的 Task 风险已在 C2 顺手关闭；Sprint/Requirement 当前根本无 reviewer 字段。

本 change 价值：
1. 把 C1 的 Caveats:4 正式 close 掉（避免后续 audit 反复触发）
2. 记录"未来给 Sprint/Requirement 加 reviewer 时必须复用 set 标志位模式"
3. 留下 `TaskUpdateNoReviewerWipeTest` 作为回归护栏（已存在，本次仅再次确认绿）

## OutOfScope

- 给 Sprint/Requirement 新增 reviewer 字段（独立需求）
- POST create 路径（无歧义，全量字段必填）

## 决策

- **范围 = Task only**，且 Task 已在 C2 完成
- 本次不动任何 production 代码
- 创建 spec.md + test-report.md 归档调研结论
- 不 bump 后端逻辑版本，仅作为 STDD 流程闭环

## commit

`fix(reviewer-wipe-all-entities): G2 Task PUT 缺字段不清 reviewer (复用 C1 模式) (v0.0.106)`
