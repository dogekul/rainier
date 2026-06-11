# Capability: entity-feature

> MODIFIED in v0.0.14-sprint-feature-link (2026-06-11).
> 新增反查端点 `GET /api/features/{id}/sprints`（该功能所在的迭代）。
> Feature 实体本身不变（无新增字段，productId 仍经 module 间接获得）。

## MODIFIED Requirements

### Requirement: 反查 Feature 所在的迭代

后端 SHALL 提供 `GET /api/features/{id}/sprints`，返回该 feature 已挂的 Sprint 列表（含 sprint 富化字段：code/name/status/requirementId/productId）。

#### Scenario: 列出 feature 所在的迭代

- **GIVEN** Feature F 被挂到 Sprint S1、S2
- **WHEN** `GET /api/features/{F}/sprints`
- **THEN** SHALL 返回 200
- **AND** 返回数组长度 SHALL 为 2
- **AND** 每项 SHALL 含 sprintId / code / name / status

#### Scenario: feature 未挂任何 sprint → 空数组

- **GIVEN** Feature F 存在但未挂任何 sprint
- **WHEN** `GET /api/features/{F}/sprints`
- **THEN** SHALL 返回 200
- **AND** 返回数组 SHALL 为空

#### Scenario: feature 不存在 → 404

- **GIVEN** 数据库无 Feature id=999999
- **WHEN** `GET /api/features/999999/sprints`
- **THEN** SHALL 返回 404
- **AND** body.message SHALL 含 "feature not found"
