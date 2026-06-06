# Capability: workflow-demand-conversion

## ADDED Requirements

### Requirement: POST /api/requirements 支持 sourceDemandIds 的原子转化

后端 SHALL 在 `POST /api/requirements` 请求 body 含可选数组 `sourceDemandIds: number[]` 时，在同一事务内创建 requirement 并为每个 demand id 创建一条 `linkType=DERIVED` 的 demand_requirement 行；任一 demand 不存在或重复 → 整体回滚。

#### Scenario: 含 sourceDemandIds 的成功转化

- **GIVEN** 数据库存在 user id=1、demand id=10 和 id=20
- **WHEN** `POST /api/requirements` body `{"code":"REQ-100","title":"X","description":"X","ownerUserId":1,"sourceDemandIds":[10,20]}`
- **THEN** SHALL 返回 201
- **AND** body.id SHALL 为正整数（设为 R）
- **AND** body SHALL **不含** `sourceDemandIds`（不污染 RequirementDetail 字段集）
- **AND** DB 表 `rainier_demand_requirement` SHALL 新增 2 行：`(demandId=10, requirementId=R, linkType="DERIVED")` 和 `(demandId=20, requirementId=R, linkType="DERIVED")`
- **AND** 后续 `GET /api/requirements/R/source-demands` SHALL 返回 2 个元素

#### Scenario: sourceDemandIds 中含不存在的 demand → 整体回滚

- **GIVEN** demand id=10 存在，id=999_999 不存在
- **WHEN** `POST /api/requirements` body `sourceDemandIds=[10, 999999]`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "demand not found: id=999999"
- **AND** DB 中 SHALL 不存在 code 为该 payload code 的 requirement（回滚）
- **AND** DB 中 demand_requirement SHALL 不含 (demandId=10, requirementId=?)（回滚）

#### Scenario: 空 sourceDemandIds 或字段缺失 — 等价于普通创建

- **GIVEN** user id=1 存在
- **WHEN** `POST /api/requirements` body 不含 `sourceDemandIds` 或 body 含 `sourceDemandIds=[]`
- **THEN** SHALL 返回 201
- **AND** DB 表 `rainier_demand_requirement` SHALL 无新行
