# Capability: opportunity — v0.0.48 project-types delta (MODIFIED)

> 合并入 canonical `specs/opportunity/spec.md`（Phase 6）。立项(initiate) 支持「创建或关联 对外-交付 项目」。见 [[entity-project]] / [[frontend-scaffold]]。

## MODIFIED Requirements (from change 2026-06-23-project-types / v0.0.48)

### Requirement: 立项可创建或关联对外-交付项目（原子）

`POST /api/opportunities/{id}/initiate` 请求体 SHALL 支持「已有 `projectId`」**或**「内联新建项目 `projectCode`/`projectName`/可选 `projectOwnerUserId`」二选一。
立项 PASS 时：传 `projectId` SHALL 校验该项目存在且类型为 `EXTERNAL_DELIVERY`（否则 400）后关联；传新建字段 SHALL 以
`projectType=EXTERNAL_DELIVERY` 创建项目（复用 ProjectService.create：owner 默认商机 pmUserId、code 唯一）并关联，且建项目与关联 SHALL 同事务提交。
二者皆缺 SHALL 返回 400。立项仍要求商机为 WON。REJECT SHALL 仅记录决策、不创建/关联项目。

#### Scenario: 立项关联已有对外-交付项目

- **GIVEN** 一个 WON 商机与一个 `EXTERNAL_DELIVERY` 项目
- **WHEN** `POST /{id}/initiate` body `{projectId, decision:"PASS"}`
- **THEN** SHALL 返回 200，商机 projectId SHALL 为该项目

#### Scenario: 立项关联非对外-交付项目被拒

- **GIVEN** 一个 WON 商机与一个 `CASUAL` 项目
- **WHEN** `POST /{id}/initiate` body `{projectId(该 CASUAL 项目), decision:"PASS"}`
- **THEN** SHALL 返回 400（须关联对外-交付项目）

#### Scenario: 立项内联新建对外-交付项目

- **GIVEN** 一个 WON 商机（含 pmUserId）
- **WHEN** `POST /{id}/initiate` body `{projectCode, projectName, decision:"PASS"}`（无 projectId）
- **THEN** SHALL 返回 200，新建项目 projectType SHALL 为 EXTERNAL_DELIVERY 且商机已关联其 id

#### Scenario: 立项既无 projectId 也无新建信息被拒

- **GIVEN** 一个 WON 商机
- **WHEN** `POST /{id}/initiate` body `{decision:"PASS"}`
- **THEN** SHALL 返回 400

#### Scenario: 非 WON 不可立项（不回归）

- **GIVEN** 一个 OPEN 商机
- **WHEN** `POST /{id}/initiate` body `{projectId, decision:"PASS"}`
- **THEN** SHALL 返回 409
