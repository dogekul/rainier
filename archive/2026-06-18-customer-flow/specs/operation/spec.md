# Capability: operation — v0.0.44 (NEW)

> NEW capability。售后运营 pipeline：回款/维保→运营→复购（线性，无关口）。all-users。新表 rainier_operation。
> 客户全流程图「售后环节」。见 [[frontend-scaffold]] / [[entity-project]]。

## ADDED Requirements

### Requirement: 运营单创建与字段

后端 SHALL 提供 `POST /api/operations`，必填 `customerName` / `title`；可选 `opsOwnerUserId`（非空则校验存在）/
`projectId`（链交付完成的 Project）；创建 `stage=MAINTENANCE`、`status=ACTIVE`。`GET /api/operations`（分页 + 过滤
stage/status）+ `GET /{id}` + `PUT /{id}` + `DELETE`。

#### Scenario: 最小创建

- **WHEN** `POST /api/operations` body `{customerName:"X 集团", title:"采购系统运维"}`
- **THEN** SHALL 返回 201
- **AND** body.stage SHALL 为 "MAINTENANCE"、body.status SHALL 为 "ACTIVE"

### Requirement: 运营节点线性推进

后端 SHALL 提供 `POST /api/operations/{id}/advance`：`status=ACTIVE` 时进下一节点（MAINTENANCE→OPERATING→
REPURCHASE）；在末节点（REPURCHASE）推进 → `status=CLOSED`；已 CLOSED → 409。

#### Scenario: 线性推进

- **GIVEN** 运营单在 MAINTENANCE
- **WHEN** `POST /{id}/advance`
- **THEN** SHALL 返回 200，stage SHALL 为 "OPERATING"

#### Scenario: 末节点推进关闭

- **GIVEN** 运营单在 REPURCHASE
- **WHEN** `POST /{id}/advance`
- **THEN** SHALL 返回 200，status SHALL 为 "CLOSED"

#### Scenario: 已关闭不可推进

- **GIVEN** 运营单 status=CLOSED
- **WHEN** `POST /{id}/advance`
- **THEN** SHALL 返回 409
