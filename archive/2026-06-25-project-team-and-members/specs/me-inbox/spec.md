# Capability: me-inbox — v0.0.64 增量

## MODIFIED Requirements

### Requirement: 我的项目列表 UNION project_member

`MeService.listMyProjects(userId)` SHALL 返回 (user 作为 owner) UNION (user 作为 user_role.project_id) UNION (user 作为 project_member.user_id) 的项目集合去重。

#### Scenario: 仅作为 member 的项目算"我的项目"

- **GIVEN** 用户 6 (陈敏) 不是任何项目 owner，user_role 无 project_id 关联
- **AND** 用户 6 是项目 3 的成员 role=DEV
- **WHEN** `GET /api/me/projects` 由 user 6 调
- **THEN** 响应 SHALL 含 项目 3
- **AND** 工作台 `WorkbenchPage` 我的项目卡片 SHALL 渲染项目 3
