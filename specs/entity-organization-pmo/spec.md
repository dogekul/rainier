# Capability: entity-organization-pmo

## ADDED Requirements

### Requirement: 组织 PMO 关系表

系统 SHALL 提供 `rainier_organization_pmo` 表承载 (organization, user) 多对多 PMO 归属关系，UNIQUE(organization_id, user_id, del_flag) 防重复添加。

#### Scenario: 创建组织 PMO 关系

- **GIVEN** 一个组织 id=2 (研发中心) 已存在，一个用户 id=2 (黎立) 已存在
- **WHEN** 调用 `POST /api/organizations/2/pmos` body `{userId: 2}` 由 admin 身份
- **THEN** 系统 SHALL 返回 201
- **AND** 返回体 SHALL 含 organizationId=2 / userId=2 / userName="黎立"
- **AND** `rainier_organization_pmo` SHALL 出现该行 del_flag=0

#### Scenario: 重复添加同一 PMO 拒绝

- **GIVEN** 组织 2 已有 user 2 作为 PMO
- **WHEN** 再次 `POST /api/organizations/2/pmos` body `{userId: 2}`
- **THEN** 系统 SHALL 返回 409
- **AND** 错误消息 SHALL 含 "PMO 已存在"

#### Scenario: 删除自身配置的 PMO

- **GIVEN** 组织 2 有 user 2 作为 own PMO
- **WHEN** 调用 `DELETE /api/organizations/2/pmos/2` 由 admin 身份
- **THEN** 系统 SHALL 返回 200
- **AND** `rainier_organization_pmo` 对应行 SHALL del_flag=1（软删）
- **AND** 后续 `GET /api/organizations/2/effective-pmos` SHALL 不再含该 user

### Requirement: PMO 沿组织祖先链继承

系统 SHALL 提供 `GET /api/organizations/{id}/effective-pmos` 端点，返回该组织自身 + 全部祖先组织（沿 parent_id 链向上）的 PMO 列表 UNION 去重，每条带 inheritedFromOrgId / inheritedFromOrgName。

#### Scenario: 子组织继承父组织 PMO

- **GIVEN** 组织 1 (招联金融, root) 有 PMO alice (id=1)
- **AND** 组织 2 (研发中心, parent=1) 有 PMO 黎立 (id=2)
- **AND** 组织 6 (采购研发团队, parent=2) 无 own PMO
- **WHEN** `GET /api/organizations/6/effective-pmos`
- **THEN** 系统 SHALL 返回 200 含 2 条记录
- **AND** 一条 userId=2 inheritedFromOrgId=2 inheritedFromOrgName="研发中心"
- **AND** 一条 userId=1 inheritedFromOrgId=1 inheritedFromOrgName="招联金融"

#### Scenario: 顶级组织无祖先时只返回自身

- **GIVEN** 组织 1 是 root（parent_id=NULL），有 PMO alice
- **WHEN** `GET /api/organizations/1/effective-pmos`
- **THEN** 系统 SHALL 返回 200 含 1 条
- **AND** 该条 inheritedFromOrgId=1

### Requirement: 不可在子组织删除继承的 PMO

系统 SHALL 拒绝在子组织上删除来自祖先组织的 PMO，要求到源组织操作。

#### Scenario: 删继承 PMO 失败

- **GIVEN** 组织 1 有 PMO alice
- **AND** 用户访问组织 6 (孙子级)
- **WHEN** `DELETE /api/organizations/6/pmos/1` (alice 来自组织 1)
- **THEN** 系统 SHALL 返回 400
- **AND** 错误消息 SHALL 含 "请到上级组织 招联金融 操作"

### Requirement: 非 admin 用户无权管理组织 PMO

系统 SHALL 仅允许 adminAccess=true 的用户创建/删除组织 PMO。

#### Scenario: 非 admin POST 被拒

- **GIVEN** 当前用户非 admin (例如 黎立)
- **WHEN** `POST /api/organizations/2/pmos` body `{userId: 5}`
- **THEN** 系统 SHALL 返回 403
