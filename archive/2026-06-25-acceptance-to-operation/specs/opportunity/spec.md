# Capability: opportunity — v0.0.58 acceptance-to-operation delta (MODIFIED)

> 合并入 canonical `specs/opportunity/spec.md`（Phase 6）。

## MODIFIED Requirements (from change 2026-06-25-acceptance-to-operation / v0.0.58)

### Requirement: 商机推进至 ACCEPTANCE 自动建 Operation

`OpportunityService.advance` 推进至 ACCEPTANCE 后 SHALL 调用 `OperationService.createForAcceptedOpportunity` 自动建一条 Operation（customerName/title/opsOwnerUserId/projectId 同源、opportunityId=opp.id、stage=MAINTENANCE/status=ACTIVE）；同 opportunityId 已存在 SHALL 跳过（幂等）。advance 返回原 OpportunityDetail（stage=ACCEPTANCE），副作用在事务内完成；后续可经 `GET /api/operations?opportunityId={oppId}` 查到该 Operation。
