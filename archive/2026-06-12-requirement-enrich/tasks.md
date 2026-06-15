# v0.0.19-requirement-enrich 任务清单

## entity-requirement (后端 MOD)
- [ ] R01 RequirementStatus 新 6 常量 + ALL
- [ ] R02 Priority +LOWEST + ALL
- [ ] R03 Requirement +expectedDate + 3 DTO + Detail.from + RequirementService set
- [ ] R04 RequirementStatusBackfill remap runner
- [ ] R05 backend 测试 TC-REQE-001..005 + TC-PRIO-001 + QueryTest APPROVED→IN_ANALYSIS

## frontend-scaffold (前端 MOD)
- [ ] R06 api/requirement(状态新值+expectedDate) + api/demand(Priority +LOWEST)
- [ ] R07 RequirementsPage 状态/优先级/expectedDate + demand/story/task 优先级+最低 + 测试

## E2E
- [ ] R08 docker 重建 + remap + 新状态/LOWEST/expectedDate + 存量不变 TC-E2E-REQE-001
