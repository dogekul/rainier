# D7: 运营 per-product PO scope + 问题清单分页 (v0.0.95)

## Why
- /api/me/inbox 全量返回, PO 多产品时混淆。需 ?productId= 范围过滤。
- 运营详情问题清单无分页, 数据量增长后不可用。
- 已识别的运营问题需要快速转工单 (Task)。

## What
1. `GET /api/me/inbox?productId={id}`: 当传 productId 时, myRequirements 仅返回 owner=self ∧ 其 opportunity.productId == productId 的 requirement; unconvertedDemands 留为空 (demands 无 product 标签, PO scope 收窄)。
2. NEW `GET /api/operations/{id}/issues/page?page=&size=&status=&severity=` 分页 + 过滤。
3. NEW `POST /api/operation-issues/{id}/convert-to-task` body `{projectId}` → 创建 Task (title=issue.title, description=issue.description, code 自动), issue.status=CONVERTED。
4. 前端运营详情页问题清单分页 + 「转工单」按钮。

## Out of Scope
- ProductPO 角色 (用 productId 过滤即可)
- 工单回流到问题列表 (单向 convert)
- 既有 `GET /api/operations/{id}/issues` 仍保留 (不分页, 向后兼容)

## Risk
- 新增 CONVERTED 状态需进 IssueStatus.ALL; 既有 update 验证仍通过。
- productId 过滤需 Opportunity 联查; 用 batch lookup 避免 N+1。
