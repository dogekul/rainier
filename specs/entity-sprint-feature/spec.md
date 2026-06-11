# Capability: entity-sprint-feature

> NEW capability from v0.0.14-sprint-feature-link (2026-06-11).
> M:N 链接，把产品域的 Feature 挂到 project 域的 Sprint（产品迭代）上，桥接两域。
> 硬删（无 del_flag 软删语义）。`(sprint_id, feature_id)` 唯一。
> 挂载触发 sprint.productId 惰性建立 + 产品一致性校验。

## ADDED Requirements

### Requirement: 创建 Sprint-Feature 链接（含产品一致性校验与 productId 惰性建立）

后端 SHALL 通过 `POST /api/sprint-features` 接受 `sprintId` + `featureId`；校验存在性与产品一致性后硬持久化并返回 201。

#### Scenario: 首个 feature 挂载触发 productId 惰性建立

- **GIVEN** Sprint S（productId 为 null）与 Feature F（经 module 属 Product P）均存在
- **WHEN** `POST /api/sprint-features` body `{"sprintId":S,"featureId":F}`
- **THEN** SHALL 返回 201
- **AND** body 字段集 SHALL 含 `sprintId` / `featureId` / `id` / `createTime`
- **AND** Sprint S 的 productId SHALL 被写为 P
- **AND** 后续 `GET /api/sprints/{S}` 的 productId SHALL 为 P

#### Scenario: 第二个同产品 feature 挂载成功

- **GIVEN** Sprint S 已挂 Product P 下的 Feature F1（productId 已锁为 P）
- **AND** Feature F2 也属 Product P
- **WHEN** `POST /api/sprint-features` body `{"sprintId":S,"featureId":F2}`
- **THEN** SHALL 返回 201

#### Scenario: 跨产品 feature 挂载被拒

- **GIVEN** Sprint S 的 productId 已锁为 Product P
- **AND** Feature G 属另一 Product Q
- **WHEN** `POST /api/sprint-features` body `{"sprintId":S,"featureId":G}`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "feature must belong to the sprint's product"

#### Scenario: 唯一性冲突（同 sprint + 同 feature 重复挂）

- **GIVEN** 已存在链接 `(sprintId=S, featureId=F)`
- **WHEN** 再 `POST /api/sprint-features` 同 `sprintId=S, featureId=F`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "link already exists"

#### Scenario: sprintId 不存在被拒

- **GIVEN** 数据库无 Sprint id=999999
- **WHEN** `POST /api/sprint-features` body `{"sprintId":999999,"featureId":F}`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "sprint not found"

#### Scenario: featureId 不存在被拒

- **GIVEN** 数据库无 Feature id=999999
- **WHEN** `POST /api/sprint-features` body `{"sprintId":S,"featureId":999999}`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "feature not found"

### Requirement: 解绑 Sprint-Feature 链接（硬删）

后端 SHALL 通过 `DELETE /api/sprint-features/{id}` 物理删除链接行；删除后 productId 不回退。

#### Scenario: 硬删成功

- **GIVEN** 链接 id=L 存在
- **WHEN** `DELETE /api/sprint-features/L`
- **THEN** SHALL 返回 204
- **AND** DB 中该行 SHALL 物理消失（`SELECT COUNT(*)` for id=L = 0）

#### Scenario: 解绑最后一个 feature 后 sprint.productId 不变

- **GIVEN** Sprint S 仅挂一个 Feature F（productId 已锁为 P）
- **WHEN** `DELETE /api/sprint-features/{该链接 id}`
- **THEN** SHALL 返回 204
- **AND** Sprint S 的 productId SHALL 仍为 P（不回退为 null）

### Requirement: 列表与单查询

后端 SHALL 通过 `GET /api/sprint-features?sprintId=&featureId=&page=&size=` 返回 PageResponse；通过 `GET /api/sprint-features/{id}` 返回单链接。

#### Scenario: 按 sprintId 过滤

- **GIVEN** 存在 3 个 sprint_feature 行，其中 2 行 sprintId=S
- **WHEN** `GET /api/sprint-features?sprintId=S`
- **THEN** body.total SHALL 为 2
- **AND** body.content[*].sprintId SHALL 全为 S
