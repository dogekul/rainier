# Capability: entity-requirement

> MODIFIED in v0.0.14-sprint-feature-link (2026-06-11).
> 新增反查端点 `GET /api/requirements/{id}/features` —— 2 跳汇总（需求→其 sprints→features，去重保序）。
> Requirement 实体本身不变。

## MODIFIED Requirements

### Requirement: 反查 Requirement 触达的功能（2 跳汇总）

后端 SHALL 提供 `GET /api/requirements/{id}/features`，返回该需求经其所有 sprint 触达的去重 Feature 列表。

#### Scenario: 跨多个 sprint 汇总去重

- **GIVEN** Requirement R 下有 Sprint S1、S2
- **AND** S1 挂 Feature F1、F2，S2 挂 Feature F2、F3
- **WHEN** `GET /api/requirements/{R}/features`
- **THEN** SHALL 返回 200
- **AND** 返回数组 SHALL 含且仅含 F1、F2、F3（F2 去重为 1 项）
- **AND** 每项 SHALL 含 featureId / code / name

#### Scenario: 需求下的 sprint 均未挂功能 → 空数组

- **GIVEN** Requirement R 下有 Sprint 但无任何 sprint-feature 链接
- **WHEN** `GET /api/requirements/{R}/features`
- **THEN** SHALL 返回 200
- **AND** 返回数组 SHALL 为空

#### Scenario: requirement 不存在 → 404

- **GIVEN** 数据库无 Requirement id=999999
- **WHEN** `GET /api/requirements/999999/features`
- **THEN** SHALL 返回 404
- **AND** body.message SHALL 含 "requirement not found"
