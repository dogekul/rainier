# I1 — Profile 能力标签前端

## 问题

C5 已经在 `/api/me/profile` 返回 `capabilities[]`，但「我的档案」页没有消费，能力标签仍然藏在 API 里。

## 范围

- `frontend/src/api/profile.ts` 增加能力标签类型。
- `ProfilePage` 增加「能力标签」卡片。
- 显示 tag name、category 中文标签、level、source 中文标签。
- 空列表显示空态。

## OutOfScope

- 能力标签编辑/自评表单。
- capability tag 字典管理 UI。
- 下属档案页。
