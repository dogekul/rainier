# v0.0.17-milestone 实现任务清单

## entity-milestone (后端 NEW)

- [ ] M01 `MilestoneStatus` 常量类 + `Milestone` 实体
- [ ] M02 `MilestoneRepository`(existsByProjectIdAndCode/findByProjectId) + `MilestoneDetail` + Create/Update DTO
- [ ] M03 `MilestoneService` create/findById/list/update/delete
- [ ] M04 `MilestoneController` REST
- [ ] M06 backend 测试 TC-MILE-001..013 + 级联 CAS-001/002

## entity-project (后端 MOD)

- [ ] M05 `ProjectService.delete` 注入 MilestoneRepository + 级联软删

## frontend-scaffold (前端 MOD)

- [ ] M07 `api/milestone.ts`
- [ ] M08 `MilestonesPanel.tsx` + ProjectsPage 里程碑按钮 toggle
- [ ] M09 `MilestonesPanel.test.tsx` + ProjectsPage.test 加按钮用例

## E2E

- [ ] M10 docker 重建 + SHOW TABLES=19 + 级联 + 存量不变 TC-E2E-MILE-001
