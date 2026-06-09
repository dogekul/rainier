# Capability: entity-project

## MODIFIED Requirements (v0.0.11)

### Requirement: 软删 Project（FK 保护扩展加 Task 引用）

后端 SHALL 在 `DELETE /api/projects/{id}` 时按家族 chain 顺序检查下游引用：
1. `requirementRepo.countByProjectId(id) > 0` → 409 "project has linked requirements"
2. `userRoleRepo.countByProjectId(id) > 0` → 409 "project has linked user roles"
3. **v0.0.11 NEW** `taskRepo.countByProjectId(id) > 0` → 409 "project has linked tasks"

无任何引用时执行软删，返回 204。

#### Scenario: v0.0.11 — 有 Task 引用被拒

- **GIVEN** Project id=1 下有 ≥ 1 行 `rainier_task.project_id=1 AND del_flag=0`；无 Requirement / UserRole 引用
- **WHEN** `DELETE /api/projects/1`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "project has linked tasks"

#### Scenario: v0.0.11 — 同时有 Requirement + Task 引用时优先返 requirement 错误

- **GIVEN** Project id=1 同时有 ≥ 1 Requirement 行 AND ≥ 1 Task 行
- **WHEN** `DELETE /api/projects/1`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "project has linked requirements"（家族 chain 顺序优先 Requirement）
