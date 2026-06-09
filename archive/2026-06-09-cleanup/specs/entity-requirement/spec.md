# Capability: entity-requirement

## MODIFIED Requirements

### Requirement: `RequirementDetail` 严格不含 `storyCount` 字段（v0.0.10.1 测试加固）

后端在 `GET /api/requirements/{id}` 和 `GET /api/requirements?...` 的响应中 SHALL **不**含 `storyCount` 字段（v0.0.10 已用 `sprintCount` 替换 — 见 v0.0.10 canonical spec）。本 Requirement 用测试断言 fence 住该约束，防止 v0.0.9 残留字段未来悄悄返回。

#### Scenario: GET 详情严格无 `storyCount` + list item 严格无 `storyCount`

- **GIVEN** Requirement id=1 下有 3 个 Sprint（status: PLANNING / ACTIVE / COMPLETED，del_flag 都为 0）
- **WHEN** 客户端调用 `GET /api/requirements/1`
- **THEN** 系统 SHALL 返回 HTTP 200
- **AND** `body.sprintCount` SHALL 为 3（v0.0.10 既有 enrichment 保持）
- **AND** `body.has("storyCount")` SHALL 严格为 `false`
- **AND** `GET /api/requirements?page=0&size=20` 返回的每个 list item 也 SHALL `has("storyCount") == false`
