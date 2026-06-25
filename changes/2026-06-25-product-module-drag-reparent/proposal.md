# Product Module 拖拽改父级 UI (v0.0.98 · E2)

## Why
PUT /api/product-modules/{id} 已支持 reparent（archive/2026-06-10-product-restructure），但前端
ProductModuleTreeView 只能通过编辑抽屉手工选父节点 — 在多级树下效率低且没有"父子层级"的直觉。
本切片补齐拖拽 UI：在树视图里直接把节点拖到另一个节点（成为子）或拖到根 dropzone（成为顶级）。

## Scope
- frontend/src/pages/ProductModule/ProductModuleTreeView.tsx
  - 节点行 `draggable`，根 UL 之外渲染"拖到此处成为顶级"dropzone
  - dragstart 携带 moduleId / productId
  - dragover 允许 drop 时高亮，dragleave 取消
  - 校验：跨 productId 直接拒绝（不触发 API）；拖到自身或自身子孙也拒绝
  - drop 时通过 prop callback `onReparent(id, newParentId | null)` 调用 PUT
- ProductModulesPage 注入 onReparent → updateProductModule + refetch
- 后端：已有 cross-product 400 兜底，无需改动

## OutOfScope
- 排序（仅 parentId 改）
- 跨 product 移动
- 多选拖拽 / 树键盘 a11y

## Acceptance
- 拖一个非根节点到另一个节点上 → 触发 PUT，body 含 parentId=目标 id
- 拖到根 dropzone → PUT body parentId=null
- 跨 productId 拖拽不触发 API（前端短路）
- 拖到自身或后代节点不触发 API
