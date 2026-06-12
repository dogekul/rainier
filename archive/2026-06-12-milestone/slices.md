# v0.0.17-milestone 切片执行计划

> 10 切片 M01-M10。new-entity 类型 + 1 处既有 delete 级联扩展。

| # | 优先级 | TC 覆盖 | 实现目标 | 依赖 |
|---|--------|---------|---------|------|
| M01 | P0 | (基础) | `MilestoneStatus`(PLANNED/REACHED/MISSED+ALL) + `Milestone` 实体(projectId/code/name/description/targetDate/status/actualDate/sortOrder，@SQLDelete+@Where) | 无 |
| M02 | P0 | (基础) | `MilestoneRepository`(JpaRepo+Spec, existsByProjectIdAndCode, findByProjectId) + `MilestoneDetail`(from) + `MilestoneCreateRequest`/`MilestoneUpdateRequest` | M01 |
| M03 | P0 | TC-MILE-001..013 | `MilestoneService` create(校验 projectId存在+复合code唯一+status+默认PLANNED/sortOrder0)/findById/list(projectId+status过滤,sortOrder ASC,createTime DESC)/update(自由改+code重检)/delete(软删) | M02 |
| M04 | P0 | TC-MILE-001..013 | `MilestoneController` @RequestMapping("/api/milestones") POST/GET/GET{id}/PUT/DELETE | M03 |
| M05 | P0 | TC-MILE-CAS-001/002 | `ProjectService.delete` 注入 MilestoneRepository,FK 409 链后/repo.delete 前级联 `milestoneRepo.deleteAll(findByProjectId)` (entity-project MOD) | M02 |
| M06 | P0 | TC-MILE-001..013 + CAS-001/002 | backend 测试: MilestoneController 集成(create/校验/list/update/delete) + 级联软删(ProjectControllerDelete 扩展或新测试) | M01-M05 |
| M07 | P0 | (前端基础) | `api/milestone.ts`(Milestone 接口 + MilestoneStatus + Create/Update/ListParams + listMilestones/getMilestone/createMilestone/updateMilestone/deleteMilestone) | 无(可并行) |
| M08 | P0 | TC-FES-MILE-001..004 | `MilestonesPanel.tsx`(内联 div milestones-panel-${projectId}: list 按 sortOrder + 内联新建/编辑表单 + 删除) + `ProjectsPage` 行「里程碑」按钮 toggle expandedMilestonesProjectId | M07 |
| M09 | P0 | TC-FES-MILE-001..004 | `MilestonesPanel.test.tsx`(列出/新建/删除) + `ProjectsPage.test` 加里程碑按钮展开用例 | M08 |
| M10 | P0 | TC-E2E-MILE-001 | docker 重建 + SHOW TABLES=19 + Milestone CRUD + 级联软删(新建测试项目) + 存量 3 项目不变 | M01-M09 |

## 拓扑批次

- 批次 1：M01(domain) ‖ M07(前端 api)
- 批次 2：M02(repo+dto)
- 批次 3：M03(service) ‖ M05(project 级联)
- 批次 4：M04(controller)
- 批次 5：M06(backend 测试) ‖ M08(前端面板+页)
- 批次 6：M09(前端测试)
- 批次 7：M10(E2E)

## 陷阱清单（给 BUILD）

- A: Java 8 — MilestoneStatus.ALL 用 `Collections.unmodifiableSet(new HashSet<>(Arrays.asList(...)))`。
- 级联顺序: ProjectService.delete 级联软删【必须】在 requirement/userrole/task 的 409 检查之后、repo.delete(p) 之前。被引用项目 409 先抛 → 同 @Transactional 回滚 → 里程碑不被删。
- 级联用 `milestoneRepo.deleteAll(milestoneRepo.findByProjectId(id))` 触发 @SQLDelete 软删；findByProjectId 因 @Where 只返 active。
- 复合唯一: existsByProjectIdAndCode（@Where 过滤 del_flag，软删可复用）。不加 DB UNIQUE。
- 包依赖: ProjectService→MilestoneRepository（repo，无循环）；MilestoneService→ProjectRepository（校验 projectId）。
- targetDate @NotNull(DTO) → 缺 400 fieldErrors。status 默认 PLANNED，sortOrder 默认 0。
- 前端面板全 CRUD（比 link panel 复杂）：list+内联表单+删除。data-testid: milestones-panel-${projectId} / milestone-save-btn / milestone-name-input 等。
- LocalDate ↔ string(YYYY-MM-DD) 同 Project date 模式。
- 既有 ProjectControllerDeleteTest 回归: 级联只在「项目可删」分支新增，FK 409 行为不变。
- 契约 K: 后端 MilestoneDetail 字段名 ↔ 前端 Milestone 接口；list param projectId/status 一致。
