# Capability: entity-demand-requirement

## ADDED Requirements

### Requirement: 创建诉求-需求关联

后端 SHALL 通过 `POST /api/demand-requirements` 接受 `demandId` + `requirementId` + `linkType`，硬持久化（无 del_flag）。

#### Scenario: 合法链接创建

- **GIVEN** 数据库存在 demand id=1 与 requirement id=2
- **WHEN** `POST /api/demand-requirements` body `{"demandId":1,"requirementId":2,"linkType":"RELATED"}`
- **THEN** SHALL 返回 201
- **AND** body.id SHALL 为正整数
- **AND** body 字段集 SHALL 含 demandId / requirementId / linkType / createTime / createBy

#### Scenario: 唯一性冲突（同 demand + 同 requirement 重复挂）

- **GIVEN** 已存在链接 `(demandId=1, requirementId=2)`
- **WHEN** 再 `POST /api/demand-requirements` 同 `demandId=1, requirementId=2`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "link already exists"

#### Scenario: demandId 不存在被拒

- **GIVEN** 数据库无 demand id=999_999
- **WHEN** `POST /api/demand-requirements` body `demandId=999999, requirementId=2, linkType="RELATED"`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "demand not found"

### Requirement: 列表与单查询

后端 SHALL 通过 `GET /api/demand-requirements?demandId=&requirementId=&page=&size=` 返回 PageResponse；通过 `GET /api/demand-requirements/{id}` 返回单链接。

#### Scenario: 按 demandId 过滤

- **GIVEN** 数据库存在 3 个 demand_requirement 行，其中 2 行 demandId=1
- **WHEN** `GET /api/demand-requirements?demandId=1`
- **THEN** body.total SHALL 为 2
- **AND** body.content 全部 `demandId=1`

### Requirement: 硬删链接

后端 SHALL 通过 `DELETE /api/demand-requirements/{id}` 物理删除（无 del_flag 标记）；删除后 `GET` 返 404。

#### Scenario: 硬删成功

- **GIVEN** 链接 id=1
- **WHEN** `DELETE /api/demand-requirements/1`
- **THEN** SHALL 返回 204
- **AND** 后续 `GET /api/demand-requirements/1` SHALL 返回 404
- **AND** DB 中该行 SHALL 物理消失（`SELECT COUNT(*)` for id=1 = 0）

### Requirement: 关联查询辅助端点

后端 SHALL 提供 `GET /api/requirements/{id}/source-demands`（返回该需求的所有诉求 + linkType）与 `GET /api/demands/{id}/derived-requirements`（返回该诉求派生的所有需求 + linkType）。

#### Scenario: 需求的源诉求查询

- **GIVEN** requirement id=1 通过 2 个 link 关联了 demand id=10、id=20，linkType 分别为 DERIVED 和 RELATED
- **WHEN** `GET /api/requirements/1/source-demands`
- **THEN** SHALL 返回 200
- **AND** body SHALL 为数组，长度 = 2
- **AND** 每项 SHALL 含 demand 字段（id/title/...）+ `linkType` + `linkId`

#### Scenario: 诉求的派生需求查询

- **GIVEN** demand id=10 通过 link 关联了 requirement id=1，linkType=DERIVED
- **WHEN** `GET /api/demands/10/derived-requirements`
- **THEN** SHALL 返回 200
- **AND** body 长度 SHALL 为 1
- **AND** body[0] SHALL 含 requirement 字段 + `linkType="DERIVED"`
