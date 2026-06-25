# C1: Story 编辑 UI 暴露评审字段 + PUT 字段缺失不清空 (v0.0.81)

## 背景

v0.0.39 review-queue 给 Story 加了 `reviewerUserId` / `reviewStatus` 两个字段
以及 `POST /api/stories/{id}/review` 决定端点。但 `PUT /api/stories/{id}`
仍是**全量替换**语义：若客户端未携带 reviewer 字段，service.update 会把
`reviewerUserId` / `reviewStatus` 清成 null（service.update 第 322-323 行）。

现状 `frontend/src/api/story.ts` 的 `StoryUpdate` 类型完全没有 reviewer 字段，
所以任何走 Story 编辑抽屉的保存都会清掉 reviewer——典型的"PUT 全量替换误清"
缺陷。

## 决定

1. **后端 PUT 改为 patch-like 语义**——只对"显式传了"的 reviewer 字段做修改。
   实现方式：在 `StoryUpdateRequest` 加两个内部 boolean 标志位
   `reviewerUserIdSet` / `reviewStatusSet`，setter 内置 true（Jackson 调用
   setter 即视为字段出现）。Service.update 只在标志位为 true 时才覆盖。

2. **前端 StoryEditDrawer 暴露评审字段**——编辑模式下显示 reviewer 选择
   下拉 + reviewStatus 只读 chip（决定改用 `/review` 专用端点）。

3. **类型扩展**——`Story` 类型补 `reviewerUserId` / `reviewStatus` /
   `reviewerName` 三个 optional 字段（后端 DTO 已有，前端漏了）；
   `StoryUpdate` 新增 `reviewerUserId?` optional。

## 兼容

- 现有 PUT 调用方（不带 reviewer 字段）：保持原值（**修复了误清 bug**）。
- 显式传 `reviewerUserId: null`：清空（语义不变）。
- 显式传 `reviewerUserId: <number>`：替换（语义不变）。

## OutOfScope

- 改 `ReviewService` / `/review` 决定逻辑
- 新增 review action
- Sprint / Task 等同类型 PUT 全量替换问题（独立 sub-change）
