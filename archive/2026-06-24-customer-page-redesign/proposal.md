# Proposal: v0.0.51 — 客户管理页视觉改版（卡片网格）

## Why

客户管理页是一张朴素的表格（名称/行业/联系人/操作），视觉单薄、不够友好（用户："太丑了"）。本版改为飞书风格的卡片网格，提升观感与可读性。

## What Changes

- 客户页由「Card + Table」改为**响应式卡片网格**（`auto-fill, minmax(264px)`）。
- 每张卡：彩色首字母头像（按名字确定性取色、避开红色）+ 客户名 + 行业标签（无行业显示「未填行业」灰标）+ 联系人 + 备注（2 行截断）+ 底部 编辑/删除。
- 卡片悬浮抬升（边框加深 + 阴影 + 上移 1px）。
- 标题旁显示「共 N 家」计数；空态用 `EmptyState`。
- 搜索 / 新建 / 编辑抽屉 / 删除确认 行为与 testid **完全不变**（纯展示层改版）。

## Capabilities

- Modified: `frontend-scaffold`（客户页视觉）。New: 无。

## Impact

- 代码：新增 `CustomerPage.css`；重写 `CustomerPage.tsx`（去 Table、改卡片网格、+头像取色 helper、用 EmptyState）。
- 后端/数据：无。Customer API、字段、路由、导航不变。

## Success Criteria

- [ ] 客户页以卡片网格呈现：头像 + 名称 + 行业标签 + 联系人 + 备注 + 编辑/删除。
- [ ] 计数「共 N 家」、空态、搜索、分页正常。
- [ ] 既有 testid（customers-new-btn / customer-name / customer-save / customer-edit-* / customer-delete-* …）保留，CRUD 行为不变。
- [ ] 前端全绿 + tsc/lint clean。
