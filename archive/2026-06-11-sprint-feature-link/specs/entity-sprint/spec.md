# Capability: entity-sprint

> MODIFIED in v0.0.14-sprint-feature-link (2026-06-11).
> 新增 `productId`（nullable）—— sprint 成为"产品迭代"，桥接 project 域与 product 域。
> productId 惰性建立（首个 feature 挂载时反推写入）+ 一旦非空即不可变。
> 新增反查端点 `GET /api/sprints/{id}/features`。
> requirementId 仍不可变；其余 v0.0.10/v0.0.12.1 行为不变。

## MODIFIED Requirements

### Requirement: Sprint 持有可选 productId（惰性建立、不可变）

后端 SHALL 在 `rainier_sprint` 提供 nullable `productId`；`SprintCreateRequest` 接受可选 `productId`（建时预绑）；`SprintUpdateRequest` 不接受 productId（不可变）；productId 非空后不可改。

#### Scenario: 创建 Sprint 不传 productId → productId 为 null

- **GIVEN** Requirement R 与 User U 存在
- **WHEN** `POST /api/sprints` body 含 `requirementId=R, ownerUserId=U`，不含 productId
- **THEN** SHALL 返回 201
- **AND** body.productId SHALL 为 null

#### Scenario: 创建 Sprint 预绑 productId

- **GIVEN** Requirement R、User U、Product P 存在
- **WHEN** `POST /api/sprints` body 含 `requirementId=R, ownerUserId=U, productId=P`
- **THEN** SHALL 返回 201
- **AND** body.productId SHALL 为 P
- **AND** body.productName SHALL 为 P 的名称

#### Scenario: 更新 Sprint 携带 productId 被静默忽略

- **GIVEN** Sprint S 的 productId 已为 P
- **WHEN** `PUT /api/sprints/{S}` body 含 `productId=Q`（另一产品）
- **THEN** SHALL 返回 200
- **AND** Sprint S 的 productId SHALL 仍为 P（Update 不接受改 productId）

### Requirement: 查询 Sprint 详情含 product 富化

后端 SHALL 在 `GET /api/sprints/{id}` 与列表中返回 `productId` + `productName`（productId 为 null 时 productName 为 null）。

#### Scenario: GET 详情含 productId 与 productName

- **GIVEN** Sprint S 的 productId 为 Product P
- **WHEN** `GET /api/sprints/{S}`
- **THEN** SHALL 返回 200
- **AND** body 字段集 SHALL 含 `productId` 与 `productName`
- **AND** body.productName SHALL 为 P 的名称

### Requirement: 反查 Sprint 下的功能

后端 SHALL 提供 `GET /api/sprints/{id}/features`，返回该 sprint 已挂的 Feature 列表（含 feature 富化字段）。

#### Scenario: 列出 sprint 的功能

- **GIVEN** Sprint S 挂了 Feature F1、F2
- **WHEN** `GET /api/sprints/{S}/features`
- **THEN** SHALL 返回 200
- **AND** 返回数组长度 SHALL 为 2
- **AND** 每项 SHALL 含 featureId / code / name / moduleId

#### Scenario: sprint 不存在 → 404

- **GIVEN** 数据库无 Sprint id=999999
- **WHEN** `GET /api/sprints/999999/features`
- **THEN** SHALL 返回 404
- **AND** body.message SHALL 含 "sprint not found"
