# Capability: entity-operation-issue — v0.0.58 (NEW)

> 新 capability：运营问题清单 (rainier_operation_issue)。

## ADDED Requirements

### Requirement: 运营问题清单 实体 + CRUD

`OperationIssue` SHALL 含：operationId (FK to Operation, required) · title (≤200, required) · description (TEXT, optional, Markdown) · severity (HIGH/MEDIUM/LOW, required default MEDIUM) · status (OPEN/IN_PROGRESS/RESOLVED/CLOSED, required default OPEN) · reporterUserId (required) · assigneeUserId (optional) · closeReason (≤500, optional)。 软删（@SQLDelete + @Where）。

REST：
- `GET /api/operations/{opId}/issues` — 列出该 Operation 的所有 issues（按 id DESC）
- `POST /api/operations/{opId}/issues` — 新建（reporterUserId 必填，severity 默认 MEDIUM，status 自动 OPEN）
- `GET /api/operation-issues/{id}` — 单查
- `PUT /api/operation-issues/{id}` — 编辑（含 status 切换 + closeReason）
- `DELETE /api/operation-issues/{id}` — 软删

#### Scenario: 创建 issue

- **GIVEN** 存在 Operation id=7
- **WHEN** `POST /api/operations/7/issues` body `{title:"X",reporterUserId:5}`
- **THEN** SHALL 返回 201，detail.operationId=7，detail.status="OPEN"，detail.severity="MEDIUM"

#### Scenario: 切换状态 + 关闭原因

- **WHEN** `PUT /api/operation-issues/{id}` body `{title:"X",status:"CLOSED",closeReason:"已修复"}`
- **THEN** SHALL 返回 200，detail.status="CLOSED"，detail.closeReason="已修复"
