# A7: weekly-draft (AI 周报草稿 — 模板规则版)

## What
新增 `WeeklyDraft` 实体 + `/api/me/weekly-drafts` 端点：基于「本周完成的 task / story」按
模板规则生成一份 markdown 周报草稿（status=DRAFT）；调用 accept 将其标记为 ACCEPTED。
不接 LLM、不发邮件、不出前端页面（OutOfScope，A8 才接 push）。

## Why
A1-A6 已搭建事件管线 + 状态同步 + 风险雷达；周报是飞轮的最常见交付物之一。先用模板
规则版打通「数据 → 草稿 → 人工确认」链路，让后续 A8（push）和真实 LLM 替换有最小的
持久化骨架。

## Scope
- NEW entity `com.rainier.weekly.domain.WeeklyDraft` → `rainier_weekly_draft`
- NEW `WeeklyDraftRepository` / `WeeklyDraftService`（generate / accept / list）
- NEW DTO `WeeklyDraftResponse`、request `GenerateRequest`
- NEW `MeWeeklyDraftController`（`/api/me/weekly-drafts`），token-gated
- NEW spec.md（capability=weekly-draft，2 个 Scenario）
- 测试：`WeeklyDraftServiceGenerateTest`（@SpringBootTest）覆盖 generate + accept

## OutOfScope
- 真实 LLM 调用（仅 markdown 模板拼接）
- 邮件 / push 通知（A8）
- 前端页面 / API client
- 周报订阅 / 调度（手动 trigger）
- 跨人员（manager-view）的汇总周报

## Decisions
- `userId` 解析：endpoint 从 SecurityFilter 拿 username → User.id；service 只接受 userId（Long）便于直测
- DONE 判定：`task.status="DONE"` 且 `update_time` 落在 `[periodStart 00:00, periodEnd 23:59:59]`
  且 `assignee_user_id = userId`；story 同：`status="DONE"` 且 `owner_user_id = userId`
- markdown 模板：
  ```
  # 本周完成 (periodStart ~ periodEnd)
  ## Task
  - [code] title
  ## Story
  - [code] title
  # 进行中
  ## Task
  - [code] title
  ```
- 空数据也生成草稿（每段下 "- 无"）——草稿永远是 DRAFT 起步
- `contentMarkdown` @Lob，存全文
- accept 只允许 DRAFT → ACCEPTED；其他状态抛 ConflictException
- 不新增 npm/maven 依赖
