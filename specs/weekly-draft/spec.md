# Capability: weekly-draft

> NEW capability (v0.0.71-weekly-draft, 2026-06-25) — **AI 周报草稿（模板规则版）**。基于
> 本周完成 / 进行中的 task & story，按模板拼接 markdown，持久化为 `WeeklyDraft`
> (status=DRAFT)；人工 accept 后置为 ACCEPTED。本版**不接 LLM、不发邮件、不出前端页**。

## ADDED Requirements

### Requirement: 生成本周 DRAFT

`WeeklyDraftService.generate(userId, periodStart, periodEnd)` SHALL 持久化一条
`WeeklyDraft`：
- `userId / periodStart / periodEnd` 来自参数
- `status = "DRAFT"`，`createdAt = now()`，`acceptedAt = null`
- `contentMarkdown` 包含「# 本周完成」与「# 进行中」两段
- 「# 本周完成」段下，列出 `task.status="DONE"` 且 `update_time ∈ [periodStart, periodEnd]`
  且 `assignee_user_id = userId` 的任务，每行 `- [code] title`
- 「# 本周完成」段下同样列出 `story.status="DONE"` 且 `update_time ∈ [periodStart, periodEnd]`
  且 `owner_user_id = userId` 的 story
- 「# 进行中」段下列出 `status="IN_PROGRESS"` 的对应 task/story
- 任一段无数据时 SHALL 渲染 "- 无"（不省略段落）

#### Scenario: 一条 DONE task 出现在「本周完成」段

- **GIVEN** user(id=1, loginName=alice)；project(id=1)；task `{code:"T-1", title:"修登录", status:"DONE", assigneeUserId:1, projectId:1}` 且 `update_time` 落在 `[periodStart, periodEnd]`
- **WHEN** `WeeklyDraftService.generate(1L, periodStart, periodEnd)`
- **THEN** SHALL 持久化 1 条 `WeeklyDraft`，其 `status="DRAFT"`，`userId=1`
- **AND** `contentMarkdown` SHALL 含字串 `"# 本周完成"`
- **AND** `contentMarkdown` SHALL 含字串 `"[T-1] 修登录"`

#### Scenario: 没有任何 DONE / IN_PROGRESS → 草稿仍生成且每段为「- 无」

- **GIVEN** user(id=1)；该用户名下零 task / 零 story
- **WHEN** `WeeklyDraftService.generate(1L, periodStart, periodEnd)`
- **THEN** SHALL 持久化 1 条 `WeeklyDraft`，`status="DRAFT"`
- **AND** `contentMarkdown` SHALL 同时含 `"# 本周完成"` 与 `"# 进行中"` 与 `"- 无"`

### Requirement: accept 仅允许 DRAFT → ACCEPTED

`WeeklyDraftService.accept(id)` SHALL：
- 在 status=DRAFT 时把它改为 ACCEPTED 并写入 `acceptedAt = now()`
- 在 status ∈ {ACCEPTED, SENT} 时抛 `ConflictException`
- 找不到对应 id 时抛 `NotFoundException`

#### Scenario: accept 一份 DRAFT

- **GIVEN** WeeklyDraft id=10 status=DRAFT
- **WHEN** `WeeklyDraftService.accept(10L)`
- **THEN** 该行 SHALL `status="ACCEPTED"`
- **AND** `acceptedAt` SHALL 非空
