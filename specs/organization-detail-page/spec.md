# Capability: organization-detail-page

> NEW capability (v0.0.99, 2026-06-25) — admin 视角的组织详情独立页 + 该组织的审计历史 tab。

## ADDED Requirements

### Requirement: GET /api/organizations/{id}/audit-log

`OrganizationController` SHALL 暴露 `GET /api/organizations/{id}/audit-log`：

- 委托 `AuditLogService.query(actor=null, entityType="ORGANIZATION", entityId=id, action=action, page)`
- 接受可选 `action` 与分页参数 `page`/`size`
- 鉴权沿用 `/api/audit-logs`：注解 `@RequiresPermission(AUDIT_VIEW)`
- 当组织不存在 → 404（先 `OrganizationService.findById(id)` 触发 NotFoundException）

#### Scenario: 查询某组织的审计历史只返回该组织的 ORGANIZATION 行

- **GIVEN** 组织 100 与 200 各被 update 过一次
- **WHEN** `GET /api/organizations/100/audit-log`
- **THEN** 200 OK，content 仅含 entityType="ORGANIZATION" 且 entityId=100 的行

#### Scenario: action 过滤

- **GIVEN** 组织 100 被 CREATE 一次、UPDATE 两次
- **WHEN** `GET /api/organizations/100/audit-log?action=UPDATE`
- **THEN** content.size == 2 且每行 action="UPDATE"

#### Scenario: 不存在的组织 → 404

- **GIVEN** 组织 999 不存在
- **WHEN** `GET /api/organizations/999/audit-log`
- **THEN** HTTP 404

### Requirement: 前端 OrganizationDetailPage

`OrganizationDetailPage` SHALL 在路径 `/org/orgs/:id` 渲染：
- 头部：name + code + type chip
- Tabs：基本信息 / 成员 / PMO / 子组织 / 关联项目 / 变更历史
- 变更历史 tab 拉取 `GET /api/organizations/:id/audit-log` 并按 actor / action / time / summary 列展示
- `OrganizationsPage` 名称列点击跳转到详情页

#### Scenario: 渲染基础信息

- **GIVEN** mocked `getOrganization(1)` 返回 `{id:1, name:"总公司", code:"HQ", type:"COMPANY"}`
- **WHEN** 渲染 `<OrganizationDetailPage />` at `/org/orgs/1`
- **THEN** 文档中可见文本「总公司」与「HQ」
- **AND** 可见至少 6 个 tab 按钮
