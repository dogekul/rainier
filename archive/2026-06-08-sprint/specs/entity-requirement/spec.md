# Capability: entity-requirement

## MODIFIED Requirements (from change 2026-06-08-sprint)

### Requirement: 软删需求 DELETE FK 保护改为 Sprint 引用（v0.0.9 Story FK 替换）

后端 SHALL 在 `DELETE /api/requirements/{id}` 软删前，除原有 `demand_requirement` 引用检查外，新增 **Sprint** 引用检查：若 `rainier_sprint` 表中有 ≥ 1 行 `requirement_id = id 且 del_flag = 0`，SHALL 返回 409 "requirement has linked sprints"。**v0.0.9 加的 Story 引用检查 (storyRepo.countByRequirementId) 移除**，因为 Story 现在通过 Sprint 间接挂 Requirement，FK 链路改走 Sprint。

#### Scenario: Requirement 有 Sprint 引用 → 409

- **GIVEN** Requirement id=1 在 `rainier_sprint` 表中有 ≥ 1 行 `requirement_id=1, del_flag=0`；无 demand_requirement 引用
- **WHEN** `DELETE /api/requirements/1`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "requirement has linked sprints"

#### Scenario: Requirement 有 Demand + Sprint 双引用，demand 错误优先

- **GIVEN** Requirement id=1 同时有 ≥ 1 demand_requirement 行 AND ≥ 1 sprint 行
- **WHEN** `DELETE /api/requirements/1`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "requirement has linked demands"（与 v0.0.9 family 模式一致：demand 检查先于 sprint）

### Requirement: Requirement 读路径富化 sprintCount（v0.0.9 storyCount 替换）

后端 SHALL 在 `GET /api/requirements/{id}` 与 `GET /api/requirements?...` 列表的每个返回项中加入 `sprintCount` 字段，值为该 Requirement 关联的、`del_flag=0` 的 Sprint 行数（含所有状态）。**v0.0.9 加的 `storyCount` 字段移除**——Story 数现在通过 Sprint drilldown 间接观察；v0.0.11+ 可考虑加 storyCountTotal 跨 Sprint 聚合（本期不做）。

#### Scenario: GET 详情 + list 项含 sprintCount

- **GIVEN** Requirement id=1 下有 3 个 Sprint（status: PLANNING / ACTIVE / COMPLETED，del_flag 都为 0）
- **WHEN** `GET /api/requirements/1`
- **THEN** SHALL 返回 200
- **AND** body.sprintCount SHALL 为 3
- **AND** body **不** SHALL 含 `storyCount` 字段
- **AND** 同样的 `GET /api/requirements?page=0&size=20` 返回的列表项 SHALL 也含 sprintCount = 3
