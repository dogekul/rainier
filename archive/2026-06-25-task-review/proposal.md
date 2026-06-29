# Proposal: task-review (v0.0.82)

## 背景
C1 已为 Story 引入「评审字段 + POST /review + 评审队列」。Task 作为另一个执行单元也需要同样
能力：QA / 架构师可对 Task 评审打回。

## 范围
1. Task entity 加两个 nullable 字段：`reviewerUserId` (Long) / `reviewStatus` (String 16)，
   复用 `com.rainier.story.domain.ReviewStatus` 常量。
2. 新增 `POST /api/tasks/{id}/review` body `{decision: APPROVED|REJECTED, reason?}`：
   - decision 必须 ∈ DECISIONS。
   - REJECTED 时必须带 reason（≤500 字符），写入 task.closeReason 或新字段（本次复用
     closeReason 简化）。
   - APPROVED 时 reason 可空。
   - reviewer 不被覆盖。
3. `GET /api/me/pending-reviews` 扩展：合并 Story + Task pending review 行，加 `kind`
   字段区分（"STORY" / "TASK"）。Task 行 storyId=null；新增 taskId。
4. `PUT /api/tasks/{id}` patch-like：reviewerUserId / reviewStatus 字段缺失不清空（同 C1 修复）。
5. 前端「我的评审」加 Task tab。

## 不在范围
- review 状态对 task.status 联动
- 评审分配自动化

## 兼容性
- 老 PUT body 不含 reviewer 键 → 字段保留（patch-like）。
- 老 GET /pending-reviews 客户端不识别 kind 字段忽略即可；新增 taskId 字段不破坏现有
  storyId-only 消费者（仍存在）。

## 提交版本
v0.0.82
