# Capability: entity-story

> MODIFIED by `changes/2026-06-12-workbench` (v0.0.18-workbench, 2026-06-12):
> `GET /api/stories` 加 `ownerUserId` 过滤参数（支撑「我的 Story」工作台）。既有 Requirements 保留;
> 此文件仅列本次 ADDED Requirement。

## ADDED Requirements (from change 2026-06-12-workbench / v0.0.18)

### Requirement: 按 ownerUserId 过滤 Story 列表

后端 SHALL 在 `GET /api/stories?ownerUserId=` 按 `ownerUserId` 精确过滤，可与既有 projectId/sprintId/status/priority 组合。

#### Scenario: 按 ownerUserId 过滤仅返回匹配项

- **GIVEN** owner=1 有 2 个 Story，owner=2 有 1 个 Story
- **WHEN** `GET /api/stories?ownerUserId=1`
- **THEN** body.total SHALL 为 2
- **AND** body.content 全部 `ownerUserId=1`
