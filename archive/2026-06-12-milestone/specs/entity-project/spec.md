# Capability: entity-project

> MODIFIED by `changes/2026-06-12-milestone` (v0.0.17-milestone, 2026-06-12):
> `ProjectService.delete` 在通过现有 Requirement→UserRole→Task 的 409 检查后，**级联软删**该项目的里程碑
> (`rainier_milestone`)，再软删项目。里程碑**不**加入 409 阻断链。既有 v0.0.8–v0.0.16 Requirements 全部保留;
> 此文件仅列本次 ADDED Requirement。

## ADDED Requirements (from change 2026-06-12-milestone / v0.0.17)

### Requirement: 删除项目级联软删里程碑

后端 SHALL 在 `DELETE /api/projects/{id}` 成功删除项目时，**级联软删**该项目下的全部里程碑(`del_flag=1`)。里程碑的存在**不**阻断项目删除;但既有的 Requirement / UserRole / Task 引用仍优先以 409 阻断（顺序不变），此时项目与其里程碑均不被删除（同事务回滚）。

#### Scenario: 删除有里程碑且无其它引用的项目级联软删里程碑

- **GIVEN** 项目 id=1 有 2 个里程碑，且无 Requirement / UserRole / Task 引用
- **WHEN** `DELETE /api/projects/1`
- **THEN** 系统 SHALL 返回 204
- **AND** 该项目的 2 个里程碑 SHALL 全部 `del_flag=1`（级联软删）
- **AND** 后续 `GET /api/milestones?projectId=1` 的 total SHALL 为 0

#### Scenario: 被 Requirement 引用的项目仍 409 且里程碑不被删

- **GIVEN** 项目 id=1 有 1 个里程碑，且在 `rainier_requirement` 有 ≥1 行 projectId=1
- **WHEN** `DELETE /api/projects/1`
- **THEN** 系统 SHALL 返回 409
- **AND** body.message SHALL 含 `"project has linked requirements"`
- **AND** 该项目的里程碑 SHALL 仍 `del_flag=0`（事务回滚，级联未执行）
