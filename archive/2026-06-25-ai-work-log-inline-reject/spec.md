# Spec: ai-work-log-inline-reject (v0.0.97)

## Scenario S1 — 点驳回展开内联表单（同行下方）
**Given** AiWorkLogsPage 渲染了 PROPOSED 行 id=7
**When** 用户点击 `ai-reject-7`
**Then** 同一行下出现 `ai-reject-form-7`，包含 `ai-reject-reason-7`（textarea），
`ai-reject-submit-7`（确认）、`ai-reject-cancel-7`（取消）。

## Scenario S2 — 取消折回
**Given** S1 后 form 已展开
**When** 点击 `ai-reject-cancel-7`
**Then** `ai-reject-form-7` 消失；decideAiWorkLog 未被调用。

## Scenario S3 — 空 reason 不提交
**Given** S1 后 form 已展开，textarea 为空
**When** 点击 `ai-reject-submit-7`
**Then** 出现错误提示 `ai-reject-error-7`；decideAiWorkLog 未被调用。

## Scenario S4 — 有 reason → 调 decision API
**Given** S1 后 form 已展开
**When** 在 textarea 输入 "证据不足" 并点击 `ai-reject-submit-7`
**Then** decideAiWorkLog 被以 (7, 'REJECTED', '证据不足') 调用；
表单关闭；列表 refetch。

## Scenario S5 — 同时只能展开一个
**Given** 两行 PROPOSED id=7, id=8
**When** 点 `ai-reject-7` 再点 `ai-reject-8`
**Then** 只有 `ai-reject-form-8` 可见，`ai-reject-form-7` 消失。
