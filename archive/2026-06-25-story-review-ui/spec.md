# Spec: story-review-ui (v0.0.81)

## Scenario S1 — PUT 不带 reviewer 字段保留原值

**Given** 一条 Story `s` 已设 `reviewerUserId=alice`, `reviewStatus=PENDING`
**When** `PUT /api/stories/{id}` body 中**完全不包含** `reviewerUserId` 和
`reviewStatus` 字段（其他必填字段齐全）
**Then** 响应 200；`s.reviewerUserId` 仍为 alice，`s.reviewStatus` 仍为
PENDING。

## Scenario S2 — PUT 显式 null 清空 reviewer

**Given** 一条 Story `s` 已设 `reviewerUserId=alice`, `reviewStatus=PENDING`
**When** `PUT /api/stories/{id}` body 包含 `reviewerUserId: null` 和
`reviewStatus: null`
**Then** 响应 200；`s.reviewerUserId == null`，`s.reviewStatus == null`。

## Scenario S3 — PUT 显式新值替换

**Given** 一条 Story `s` 已设 `reviewerUserId=alice`
**When** `PUT /api/stories/{id}` body 包含 `reviewerUserId: bob`
**Then** 响应 200；`s.reviewerUserId == bob`。

## Scenario S4 — 前端 StoryEditDrawer 暴露 reviewer

**Given** 编辑模式 `editing.reviewerUserId = 7`
**When** 用户打开抽屉
**Then** 显示 reviewer 选择下拉，默认值=7；reviewStatus 显示为只读 chip。

## TestCase 映射

| ID | Scenario | 类型 | 位置 |
|----|----------|------|------|
| TC-SRU-001 | S1 | @SpringBootTest | StoryUpdateNoReviewerWipeTest |
| TC-SRU-002 | S2 | @SpringBootTest | StoryUpdateNoReviewerWipeTest |
| TC-SRU-003 | S3 | @SpringBootTest | StoryUpdateNoReviewerWipeTest |
| TC-SRU-004 | S4 | vitest | StoryEditDrawer.test.tsx |
