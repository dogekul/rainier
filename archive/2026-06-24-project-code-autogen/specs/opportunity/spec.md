# Capability: opportunity — v0.0.49 project-code-autogen delta (MODIFIED)

> 合并入 canonical `specs/opportunity/spec.md`（Phase 6）。立项内联新建项目不再传编号（自动生成）。见 [[entity-project]]。

## MODIFIED Requirements (from change 2026-06-24-project-code-autogen / v0.0.49)

### Requirement: 立项内联新建项目不再需要编号

`OpportunityInitiateRequest` SHALL 移除 `projectCode`。立项内联新建对外-交付项目 SHALL 仅需 `projectName`（+ 可选
`projectOwnerUserId`，默认商机 pmUserId），编号由 entity-project 自动生成。缺 `projectName`（且无 `projectId`）SHALL 返回 400。

#### Scenario: 立项内联新建仅需名称

- **GIVEN** 一个 WON 商机
- **WHEN** `POST /{id}/initiate` body `{projectName, projectOwnerUserId, decision:"PASS"}`（无 projectId、无 code）
- **THEN** SHALL 返回 200，新建项目 projectType=EXTERNAL_DELIVERY、code 匹配 `ED-<id>`、商机关联其 id

#### Scenario: 立项内联新建缺名称被拒

- **GIVEN** 一个 WON 商机
- **WHEN** `POST /{id}/initiate` body `{decision:"PASS"}`（无 projectId、无 projectName）
- **THEN** SHALL 返回 400
