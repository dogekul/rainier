# organization-detail-page (E3, v0.0.99)

## 背景
组织节点目前只能通过 OrganizationsPage 的 EditDrawer 维护，缺乏一个组织维度的“全景”视图：
- 谁是这个组织的 owner / 成员 / PMO
- 它的子组织 / 关联项目
- PMO 与基础信息的变更历史（AuditAspect 早就在记录，但没有页面入口）

## 范围
- NEW `frontend/src/pages/Organization/OrganizationDetailPage.tsx`（admin only，路径 `/org/orgs/:id`）
- 组织详情页含 Tab：基本信息 / 成员 / PMO / 子组织 / 关联项目 / 变更历史
- NEW backend endpoint `GET /api/organizations/{id}/audit-log?action=` —— 内部转发到 AuditLogService.query(entityType=ORGANIZATION, entityId=id, action)
- `OrganizationsPage` 表格“名称”列改成可点击链接，跳转到详情页

## OutOfScope
- 组织级仪表盘（RYG / 人均产出）
- 组织树拖拽改 parent 的 UI 操作
- PMO 行变更（ORGANIZATION_PMO entityType）混入聚合 —— 本版只展示 ORGANIZATION 维度

## commit
`feat(organization-detail-page): E3 组织详情页 + 审计 tab (v0.0.99)`
