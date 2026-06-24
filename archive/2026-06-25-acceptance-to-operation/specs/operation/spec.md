# Capability: operation — v0.0.58 acceptance-to-operation delta (MODIFIED)

> 合并入 canonical `specs/operation/spec.md`（Phase 6 — 若 canonical 缺则创建）。

## MODIFIED Requirements (from change 2026-06-25-acceptance-to-operation / v0.0.58)

### Requirement: Operation +opportunityId（来源商机可追溯）

`Operation` SHALL 含可空 `opportunityId`。create/update DTO SHALL 接受；detail SHALL 返回；list SHALL 支持 `?opportunityId=` 过滤。

### Requirement: 商机验收自动建 Operation（幂等）

当 `OpportunityService.advance` 推进至 ACCEPTANCE 时 SHALL 调 `OperationService.createForAcceptedOpportunity` 自动建一条 Operation：customerName/title/opsOwnerUserId/projectId 同源、opportunityId=opp.id、stage=MAINTENANCE、status=ACTIVE。已存在同 opportunityId 的 Operation 时 SHALL 跳过新建（幂等返回既有）。

#### Scenario: 验收成功自动建运营

- **GIVEN** 一个 DELIVERY/WON 商机已备《甲方验收报告》
- **WHEN** `POST /opportunities/{id}/advance` 推进至 ACCEPTANCE
- **THEN** SHALL 返回 200/ACCEPTANCE
- **AND** `GET /api/operations?opportunityId={oppId}` SHALL 返回 ≥1 条 Operation（stage=MAINTENANCE/status=ACTIVE/opportunityId=该商机）
