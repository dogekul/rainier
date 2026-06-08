# Capability: entity-requirement

## MODIFIED Requirements (from change 2026-06-07-story)

### Requirement: 软删需求 DELETE FK 保护扩展（v0.0.9 加 Story 引用）

后端 SHALL 在 `DELETE /api/requirements/{id}` 软删前，除原有 `demand_requirement` 引用检查外，新增 Story 引用检查：若 `rainier_story` 表中有 ≥ 1 行 `requirement_id = id 且 del_flag = 0`，SHALL 返回 409，错误 message 指明 "requirement has linked stories"。

#### Scenario: Requirement 有 Story 引用 → 409

- **GIVEN** Requirement id=1 在 `rainier_story` 表中有 ≥ 1 行 `requirement_id=1, del_flag=0`
- **WHEN** `DELETE /api/requirements/1`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "requirement has linked stories"

### Requirement: Requirement 读路径富化 storyCount（v0.0.9）

后端 SHALL 在 `GET /api/requirements/{id}` 与 `GET /api/requirements?...` 列表的每个返回项中加入 `storyCount` 字段，值为该 Requirement 关联的、del_flag=0 的 Story 行数（含所有状态，含 CANCELLED / DONE）。

#### Scenario: GET 详情 + list 项含 storyCount

- **GIVEN** Requirement id=1 下有 3 个 Story（status: DRAFT、IN_PROGRESS、DONE，del_flag 都为 0）
- **WHEN** `GET /api/requirements/1`
- **THEN** SHALL 返回 200
- **AND** body.storyCount SHALL 为 3
- **AND** 同样的 `GET /api/requirements?page=0&size=20` 返回的列表项 SHALL 也含 storyCount = 3
