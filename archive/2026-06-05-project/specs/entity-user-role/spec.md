# Capability: entity-user-role

## MODIFIED Requirements (from change 2026-06-05-project)

### Requirement: 用户角色的 projectId 校验 + 富化 + 启动自愈

后端 SHALL 在 `POST /api/user-roles` 时，若 body 含 `projectId` 且非空，校验对应 Project 必须存在；若 NULL 则跳过校验（保持公司级 hat 语义）。响应 body SHALL 富化 `projectName` + `projectCode`。

应用启动时 `DanglingProjectIdCleanup` `CommandLineRunner` SHALL 扫描 `rainier_user_role.project_id` 所有非空值；对不存在 Project 的引用 native UPDATE SET NULL + log WARN。启动后 reads 不再有 dangling 状态。

#### Scenario: POST 含 projectId 存在 → 富化

- **GIVEN** 用户 id=1 / 角色 id=1（code="PMO"，name="PMO"）/ 项目 id=1（code="PROJ-001"，name="采购系统改造"）存在
- **WHEN** `POST /api/user-roles` body `{"userId":1,"roleId":1,"projectId":1}`
- **THEN** SHALL 返回 201
- **AND** body.projectId SHALL 为 1
- **AND** body.projectName SHALL 为 "采购系统改造"
- **AND** body.projectCode SHALL 为 "PROJ-001"

#### Scenario: POST 含 projectId 不存在 → 400

- **GIVEN** 数据库无 project id=999_999；用户 id=1 / 角色 id=1 存在
- **WHEN** `POST /api/user-roles` body 含 `projectId=999999`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "project not found"

#### Scenario: POST 含 projectId=null → 公司级 hat 保留

- **GIVEN** 用户 id=1 / 角色 id=1 存在
- **WHEN** `POST /api/user-roles` body `{"userId":1,"roleId":1,"projectId":null}`
- **THEN** SHALL 返回 201
- **AND** body.projectId SHALL 为 null
- **AND** body.projectName SHALL 为 null（无项目语义）

#### Scenario: 启动自愈 — v0.0.7 测试遗留脏数据 id=2 projectId=42 被清理

- **GIVEN** 启动前 `rainier_user_role` 表存在 id=2 行，projectId=42（v0.0.7 测试遗留），数据库无 project id=42
- **WHEN** Spring 应用启动，`DanglingProjectIdCleanup` 执行
- **THEN** id=2 行 projectId SHALL 被 native UPDATE SET NULL
- **AND** 应用日志 SHALL 含 WARN 行 `"cleaned dangling project_id from rainier_user_role.2"`
- **AND** 后续 `GET /api/user-roles/2` SHALL 返回 200 + body.projectId=null + body.projectName=null
- **AND** body.userName / body.roleName SHALL 仍正常富化（user/role 路径不受影响）
