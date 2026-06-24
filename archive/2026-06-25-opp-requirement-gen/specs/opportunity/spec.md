# Capability: entity-demand / entity-requirement — v0.0.56 opp-requirement-gen delta (MODIFIED)

> 合并入 canonical `specs/entity-demand/spec.md` 与 `specs/entity-requirement/spec.md`（Phase 6）。诉求/需求可追溯来源商机。见 [[frontend-scaffold]]。

## MODIFIED Requirements (from change 2026-06-25-opp-requirement-gen / v0.0.56)

### Requirement: 诉求/需求可关联来源商机 opportunityId

`Demand` 与 `Requirement` SHALL 各含可空 `opportunityId`。`POST /api/demands` 与 `POST /api/requirements` SHALL 接受可选 `opportunityId`：非空时校验商机存在（否则 400），持久化并在 detail 返回。`GET /api/demands` 与 `GET /api/requirements` SHALL 支持 `opportunityId` 过滤参数，仅返回该商机派生项。既有不带 opportunityId 的创建/查询行为不变。

#### Scenario: 创建诉求带 opportunityId

- **GIVEN** 一个存在的商机 id=7
- **WHEN** `POST /api/demands` body 含 `opportunityId:7`
- **THEN** SHALL 返回 201，detail.opportunityId == 7

#### Scenario: 创建需求带不存在的 opportunityId 被拒

- **WHEN** `POST /api/requirements` body 含 `opportunityId:999999`
- **THEN** SHALL 返回 400

#### Scenario: 按 opportunityId 过滤诉求

- **GIVEN** 商机 7 派生 2 条诉求、商机 8 派生 1 条
- **WHEN** `GET /api/demands?opportunityId=7`
- **THEN** SHALL 仅返回商机 7 的 2 条

#### Scenario: 不带 opportunityId 创建仍可

- **WHEN** `POST /api/demands` body 不含 opportunityId
- **THEN** SHALL 返回 201，detail.opportunityId 为 null

### Requirement: 产品诉求→交付实施 推进卡点（商机须已有需求）

`POST /api/opportunities/{id}/advance` 当来源阶段为 **产品诉求(REQUIREMENT)** 时 SHALL 要求该商机已有 ≥1 条需求（`requirementRepo.countByOpportunityId>0`）；否则 SHALL 返回 400 且消息含「需求」，停留在产品诉求。满足则推进到 交付实施(DELIVERY)。

#### Scenario: 产品诉求无需求不可推进

- **GIVEN** 一个 产品诉求(REQUIREMENT) + WON 的商机，无派生需求
- **WHEN** `POST /{id}/advance` body `{}`
- **THEN** SHALL 返回 400，消息含「需求」

#### Scenario: 已有需求可推进到交付实施

- **GIVEN** 一个 产品诉求 商机，已有 ≥1 条 opportunityId 指向它的需求
- **WHEN** `POST /{id}/advance` body `{}`
- **THEN** SHALL 返回 200，stage SHALL 为 DELIVERY
