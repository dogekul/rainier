# Capability: entity-project — v0.0.64 增量

## MODIFIED Requirements

### Requirement: Project +pmo_user_id 字段

`Project` SHALL 含可空 `pmo_user_id`。create/update DTO SHALL 接受；detail SHALL 返回 pmoUserId / pmoName / pmoLoginName。

#### Scenario: 创建项目带 pmoUserId

- **GIVEN** owner=lina (id=4)，team org id=2
- **AND** 当前用户=alice (admin)
- **WHEN** `POST /api/projects` body `{name, ownerUserId:4, organizationId:2, pmoUserId:2, projectType:"EXTERNAL_DELIVERY"}`
- **THEN** 系统 SHALL 返回 201
- **AND** 返回体 SHALL 含 pmoUserId=2 / pmoName="黎立"

#### Scenario: 更新项目 pmo

- **GIVEN** 项目 3 已有 pmoUserId=2
- **WHEN** `PUT /api/projects/3` body `{pmoUserId:5, ...}`
- **THEN** 系统 SHALL 返回 200
- **AND** 返回体 SHALL 含 pmoUserId=5

### Requirement: Project +organizationId 启用（已有列）

`rainier_project.organization_id` 已存在但前端未启用。本版 SHALL 在 create/update DTO 接受 organizationId；detail SHALL 返回 organizationId / organizationName / organizationType。

#### Scenario: 创建项目带 organizationId

- **WHEN** `POST /api/projects` body `{name, ownerUserId:4, organizationId:2, projectType:"EXTERNAL_DELIVERY"}`
- **THEN** 系统 SHALL 返回 201
- **AND** 返回体 SHALL 含 organizationId=2 / organizationName="研发中心"

### Requirement: 默认值后端注入（缺值时）

`ProjectService.create` SHALL 在 request 缺 organizationId 时取 owner 主组织（user_organization.is_primary=1）；缺 pmoUserId 时取 organizationId 的 effective-PMOs 首条。

#### Scenario: 缺 organizationId 自动填 owner 主组织

- **GIVEN** 用户 lina (id=4) 的主组织 = 研发中心 (id=2)（user_organization is_primary=1）
- **WHEN** `POST /api/projects` body `{name:"X", ownerUserId:4, projectType:"CASUAL"}` （未传 organizationId）
- **THEN** 系统 SHALL 返回 201
- **AND** 返回体 SHALL 含 organizationId=2 / organizationName="研发中心"

#### Scenario: 缺 pmoUserId 但有 team 时自动填 team 的 effective-PMOs 首条

- **GIVEN** 组织 2 有 effective-PMOs = [黎立 (own), alice (继承)]
- **WHEN** `POST /api/projects` body `{name:"X", ownerUserId:4, organizationId:2, projectType:"CASUAL"}` （未传 pmoUserId）
- **THEN** 系统 SHALL 返回 201
- **AND** 返回体 SHALL 含 pmoUserId=2 (黎立, own 优先于继承)

#### Scenario: owner 无主组织时 organizationId 留 null

- **GIVEN** 用户 X 没有 user_organization 行（或 left_at IS NOT NULL）
- **WHEN** `POST /api/projects` body `{name:"X", ownerUserId:X.id, projectType:"CASUAL"}` （未传 organizationId）
- **THEN** 系统 SHALL 返回 201
- **AND** 返回体 SHALL 含 organizationId=null
- **AND** pmoUserId=null (因为没 team 算不出 PMO)
