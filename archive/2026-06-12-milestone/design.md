# v0.0.17-milestone — 技术设计

## Context

- 栈：Java 8 / Spring Boot 2.7 / JPA(Hibernate) / MySQL(dev+docker, `ddl-auto=update`) / H2(test).
- 项目维度现有 `Project`(立项容器) + `Sprint`(层级分解，非时间盒)。缺「时点检查点」。
- 范本：`Sprint`(`com.rainier.sprint`) 是 project/requirement 挂载实体的成熟形状(extends BaseEntity，
  `@SQLDelete`+`@Where del_flag=0`，service 级 code 唯一，status 校验 `SprintStatus.ALL`)。
- `ProjectStatus` 常量类：`ALL = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(...)))`(Java 8)。
- `ProjectService.delete`：现有 `requirementRepo/userRoleRepo/taskRepo.countByProjectId>0 → ConflictException(409)`，
  顺序 Requirement→UserRole→Task，末 `repo.delete(p)`。ProjectService 已注入 4 个跨包 repo。
- 前端 `SprintFeaturePanel`：内联展开 `<div data-testid="sprint-feature-panel-${id}">`，从行按钮 toggle，面板内 CRUD。
- v0.0.16 `AuditAspect` 自动记 `*Service.create/update/delete` → 审计行。
- 约束：A2 收窄精神(暂不校验、无状态机)；standing(不删改存量)。

## Decisions

### 1. Milestone 实体形状照搬 Sprint（projectId 挂载 + 时点字段）

**方案**: `Milestone`(`com.rainier.milestone.domain`，表 `rainier_milestone`)extends BaseEntity，`@SQLDelete`+
`@Where del_flag=0`。字段：`projectId`(NN) / `code`(64) / `name`(NN,100) / `description`(2000) /
`targetDate`(LocalDate,NN) / `status`(16,NN) / `actualDate`(LocalDate,可空) / `sortOrder`(Integer)。**无 ownerUserId**。

**为什么**: 复用 Sprint 的挂载+软删形状，团队已熟悉;里程碑是时点 → 加 targetDate(计划)/actualDate(实际)。

**备选及排除**: 复用 Sprint 加字段 —— 语义不同(Sprint 是区间装 story/task)，会污染 Sprint，排除。

### 2. MilestoneStatus 三态常量类

**方案**: `MilestoneStatus`(`PLANNED`/`REACHED`/`MISSED` + `ALL`)，照搬 `ProjectStatus`(Collections.unmodifiableSet +
Arrays.asList，Java 8 无 Set.of)。

**为什么**: 与家族一致;VARCHAR 存储，未来可扩展。

### 3. (projectId, code) service 级复合唯一

**方案**: create / update(改 code 时) 用 `repo.existsByProjectIdAndCode(projectId, code)` 校验，重复 → `ConflictException`。
不加 DB UNIQUE。

**为什么**: 里程碑 per-project 命名;不同项目可同 code。soft-deleted 行因 `@Where del_flag=0` 不计入 → code 可复用，
与家族(Sprint/Project service 级 code 唯一)一致。

**备选及排除**: 全局 code 唯一 —— 里程碑跨项目无意义，per-project 更自然，排除。DB 复合 UNIQUE —— 与软删复用冲突，排除。

### 4. create 校验链 + 默认值

**方案**: 顺序——(1) `projectRepo.existsById(projectId)` 否则 `BadRequestException("project not found")`;
(2) `existsByProjectIdAndCode` 否则 `ConflictException("code already exists")`;
(3) `status = req.status==null ? PLANNED : req.status`，`MilestoneStatus.ALL.contains` 否则 `BadRequestException("invalid status")`;
(4) `targetDate` DTO `@NotNull`(缺→400 fieldErrors);(5) `sortOrder = req.sortOrder==null ? 0 : req.sortOrder`。

**为什么**: 镜像 SprintService.create 校验风格;D2(targetDate NN) / D4(status 默认 PLANNED + 仅枚举校验) / D5(sortOrder 默认 0)。

### 5. update：自由改，无状态机

**方案**: `update` 改 name/description/targetDate/status/actualDate/sortOrder;status `ALL.contains` 校验(非法→400);
code 改则重检复合唯一;`projectId` immutable(不接受改)。

**为什么**: D4 自由改无状态机(与 v0.0.16「暂不校验」一致);projectId 不可变(里程碑归属固定)。

### 6. list：projectId/status 过滤 + sortOrder 排序；MilestoneDetail 无跨实体 enrich

**方案**: `list(Long projectId, String status, PageParams)` Specification `projectId`/`status` equal predicate;
排序 `Sort.by(ASC,"sortOrder").and(DESC,"createTime")`。`MilestoneDetail` 仅含里程碑自身字段(无 owner/project join)，
service 保持简单(无 enrich)。

**为什么**: D5 排序;面板已 project-scoped，无需富化 projectName。

### 7. 级联软删：ProjectService.delete 插入里程碑级联

**方案**: `ProjectService` 注入 `MilestoneRepository`。`delete` 中——保留现有 Requirement→UserRole→Task 的 409 检查
**不变**;通过后、`repo.delete(p)` **之前**插入 `milestoneRepo.deleteAll(milestoneRepo.findByProjectId(id))`(触发
`@SQLDelete` 逐行软删)。里程碑**不**加入 409 阻断链。

**为什么**: D3 —— 允许删项目 + 里程碑级联软删(无独立意义);被 Requirement 引用的项目 409 先抛，同 `@Transactional` 回滚，
里程碑不被删。`findByProjectId` 因 `@Where` 只返 active，只软删活动里程碑。

**备选及排除**: 里程碑加入 409 阻断链 —— 用户明确要级联删非阻断，排除。物理删 —— 破坏软删一致性，排除。

### 8. 前端 MilestonesPanel 内联全 CRUD + ProjectsPage 行按钮

**方案**: `MilestonesPanel`(`pages/Project/`)照搬 SprintFeaturePanel 内联 `<div data-testid="milestones-panel-${projectId}">`：
list 该项目里程碑(按 sortOrder)+ 内联新建/编辑表单(code/name/targetDate/status/actualDate/sortOrder)+ 删除确认。
`ProjectsPage` 行操作区加「里程碑」按钮 toggle `expandedMilestonesProjectId`，展开渲染 `<MilestonesPanel projectId={r.id}/>`。
新 `api/milestone.ts`(Milestone 接口 + Create/Update/ListParams + CRUD)。

**为什么**: D4 前端 = 项目详情下内联面板;复用 v0.0.14 面板模式。

### 9. 审计白拿

**方案**: 不新做。v0.0.16 `AuditAspect` 自动记 `MilestoneService.create/update/delete` → `CREATE/UPDATE/DELETE MILESTONE`
(entityType 由 `MilestoneService` 类名推导)。

## Architecture

```
POST/PUT/DELETE /api/milestones ─► MilestoneController ─► MilestoneService
   create: projectExists? → (projectId,code) unique? → status∈ALL → default PLANNED/sortOrder0 → save
   update: status∈ALL → code unique(if changed) → set fields (projectId immutable)
   list(projectId,status): Spec predicate + Sort(sortOrder ASC, createTime DESC)
   delete: soft (@SQLDelete)

DELETE /api/projects/{id} ─► ProjectService.delete
   requirement/userrole/task count>0 → 409 (unchanged)
   else → milestoneRepo.deleteAll(findByProjectId(id))  [cascade soft-delete] → repo.delete(p)

(v0.0.16) AuditAspect @AfterReturning ─► CREATE/UPDATE/DELETE MILESTONE  (白拿)

前端 ProjectsPage 行 [编辑][删除][里程碑] → toggle → <MilestonesPanel projectId>
   面板: list(按 sortOrder) + 内联表单(新建/编辑) + 删除
```

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|----------|
| 级联软删与 project delete 事务一致性 | 同 `@Transactional`;被引用项目 409 抛出 → 整体回滚，里程碑不被删。TC-MILE-CAS-002 验证 |
| 包依赖 ProjectService→MilestoneRepository | 仅 repo(非 service)互引，无循环 bean;同 ProjectService 已注入 4 跨包 repo 的模式 |
| 复合 code 唯一靠 service 校验(无 DB 约束)并发窗口 | 与家族 code 唯一同款可接受风险;非高并发后台管理场景 |
| 前端面板全 CRUD 比 link panel 复杂 | 测试覆盖 list+create+delete + ProjectsPage 按钮 toggle;data-testid 规范 |
| targetDate/actualDate LocalDate↔string | 同 Project startDate/endDate 的 YYYY-MM-DD 模式，复用 |
| 既有 Project delete 测试回归 | 级联只在「项目可删」分支新增软删;既有 FK 409 行为不变;全量回归保护 |
