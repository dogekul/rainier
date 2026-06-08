# v0.0.10-sprint 任务清单

> 13 切片 → ~70 颗粒任务。所有 P0。
> 长程模式：单轮执行 RED+GREEN+REFACTOR。

## 1. M01 — Sprint 基础设施

- [ ] 1.1 创建 `backend/src/main/java/com/rainier/sprint/domain/Sprint.java`（@Entity + @Table("rainier_sprint") + @SQLDelete + @Where + extends BaseEntity）字段：code / name / description / goal VARCHAR(2000) / status VARCHAR(16) / requirementId BIGINT NN / ownerUserId BIGINT NN / startDate / endDate
- [ ] 1.2 创建 `domain/SprintStatus.java`：4 项常量 (PLANNING/ACTIVE/COMPLETED/CANCELLED) + Set<String> ALL（Java 8 兼容）
- [ ] 1.3 创建 `repository/SprintRepository.java`：existsByCode / countByRequirementId / countByOwnerUserId
- [ ] 1.4 创建 `dto/SprintCreateRequest.java` + `SprintUpdateRequest.java` + `SprintDetail.java`（含富化字段 ownerName/Login + requirementCode/Title + projectName/Code + storyCount）

## 2. M02 — Sprint POST create + enrich + storyCount

- [ ] 2.1 创建 `SprintService.java`：注入 SprintRepo + RequirementRepo + UserRepo + ProjectRepo + StoryRepo
- [ ] 2.2 实现 `create(SprintCreateRequest)`：校验 requirementId + ownerUserId 存在 + code 唯一 + status 合法；持久化 + enrich
- [ ] 2.3 实现私有 `enrich(Sprint s)`：join User + Requirement + Project（NN+NN）+ storyCount = storyRepo.countBySprintId(s.id) — 注意：storyRepo 此时尚无 countBySprintId，需要先加该方法
- [ ] 2.4 加 `StoryRepository.countBySprintId(Long sprintId)` 方法（M06 也会用）
- [ ] 2.5 创建 `SprintController.java` POST 端点
- [ ] 2.6 测试：TC-SPR-001..008（含 TC-SPR-008 时间字段不校验）`SprintControllerCreateTest.java`

## 3. M03 — Sprint GET 单 + 列表

- [ ] 3.1 实现 `Service.findById(Long id)` + Controller GET("/{id}")
- [ ] 3.2 实现 `Service.list(Long requirementId, String status, PageParams page)` + Specification + Controller GET 列表
- [ ] 3.3 测试：TC-SPR-009..011 `SprintControllerQueryTest.java`

## 4. M04 — Sprint PUT update

- [ ] 4.1 实现 `Service.update(Long id, SprintUpdateRequest)`：owner 可改（沿用 v0.0.8 Decision 6b）+ status/code/dates/goal 更新；requirementId 不可改
- [ ] 4.2 Controller PUT("/{id}")
- [ ] 4.3 测试：TC-SPR-012..014（追加到 QueryTest 或独立 UpdateTest）

## 5. M05 — Sprint DELETE + Story FK 保护

- [ ] 5.1 实现 `Service.delete(Long id)`：getOrThrow + `if (storyRepo.countBySprintId(id) > 0) throw ConflictException("sprint has linked stories")` + repo.delete（@SQLDelete 自动）
- [ ] 5.2 Controller DELETE("/{id}")
- [ ] 5.3 测试：TC-SPR-015..017 `SprintControllerDeleteTest.java`

## 6. M06 — Story 改造：drop requirementId → sprintId NN

- [ ] 6.1 修改 `story/domain/Story.java`：删 requirementId 字段；加 `@Column(name="sprint_id", nullable=false, columnDefinition="BIGINT") private Long sprintId` + getter/setter（**columnDefinition 关键**）
- [ ] 6.2 修改 `story/dto/StoryCreateRequest.java`：删 requirementId 字段，加 @NotNull sprintId
- [ ] 6.3 修改 `story/dto/StoryDetail.java`：删 requirementId（DTO 层面），加 sprintId/sprintCode/sprintName/sprintStatus + 保留 requirementCode/requirementTitle（来源改为 sprint.requirement 二段 join）+ 加 storyCount field 用？(no — storyCount 是 Sprint/Requirement detail 用，不在 Story detail)
- [ ] 6.4 修改 `story/service/StoryService.java`：删 requirementRepo 依赖（实际仍需，用于二段 enrich），加 SprintRepository 依赖；create 改：`sprintRepo.findById → sprint → sprint.requirementId → requirementRepo.findById → requirement.projectId`，全 enrich 链路二段
- [ ] 6.5 修改 `story/repository/StoryRepository.java`：加 `countBySprintId`，删 `countByRequirementId`（或保留兼容 — 看 Migration 是否还用 → migration 用 native，所以可删）
- [ ] 6.6 **改写 16 个旧 Story 测试** `StoryControllerCreateTest / QueryTest / DeleteTest`：所有 fixture `createRequirement` 改为 `createRequirement + createSprint`，body 传 sprintId 不再传 requirementId。新加 TC-STR-SPR-001..004
- [ ] 6.7 改 `DanglingProjectIdCleanupTest`（v0.0.8 既有）：seed Story 时用 sprintId 而非 requirementId

## 7. M07 — Requirement 改造：sprintCount + Sprint FK 保护

- [ ] 7.1 `requirement/service/RequirementService.java`：删 StoryRepository 注入（仍需保留？不需要，因为 storyCount 已删，FK 保护改 sprintRepo），加 SprintRepository
- [ ] 7.2 delete 路径：移除 storyRepo.countByRequirementId 改 sprintRepo.countByRequirementId → 409 "requirement has linked sprints"
- [ ] 7.3 enrich 路径：移除 storyCount 设置；加 dto.setSprintCount(sprintRepo.countByRequirementId(r.id))
- [ ] 7.4 `requirement/dto/RequirementDetail.java`：删 storyCount 字段（+ getter/setter），加 sprintCount 字段
- [ ] 7.5 改 v0.0.9 既有测试：`RequirementControllerDeleteTest` TC-REQS-001/001b 改为 TC-REQS-SPR-001/002（Story FK → Sprint FK）；`RequirementControllerQueryTest` TC-REQS-002 改 TC-REQS-SPR-003（storyCount → sprintCount + 不再含 storyCount）

## 8. M08 — LegacyStoryToSprintMigration（含 ALTER + 死列宽松）

- [ ] 8.1 创建 `backend/src/main/java/com/rainier/sprint/bootstrap/LegacyStoryToSprintMigration.java`：@Component + @Order(Ordered.HIGHEST_PRECEDENCE) implements CommandLineRunner；注入 EntityManager
- [ ] 8.2 **Step 0**: native ALTER `ALTER TABLE rainier_story MODIFY COLUMN requirement_id BIGINT NULL`（放宽 v0.0.9 NN 约束，允许新 INSERT 不写该列）— 幂等保证：先 SELECT 列定义看是否已 nullable 再决定是否 ALTER（或者总执行也行，MySQL 不报错重复 nullable）
- [ ] 8.3 **Step 1**: native `SELECT id, requirement_id FROM rainier_story WHERE sprint_id IS NULL AND del_flag=0 FOR UPDATE` → group by requirementId（Java 端 Map）→ 每 group 创默认 Sprint via `em.createNativeQuery("INSERT INTO rainier_sprint (...)")` → native `UPDATE rainier_story SET sprint_id=? WHERE id IN (...)` → log INFO per requirement
- [ ] 8.4 **Step 2**: native `ALTER TABLE rainier_story MODIFY COLUMN sprint_id BIGINT NOT NULL`（仅 Step 1 处理了 ≥1 行时执行；空集时早退）
- [ ] 8.5 全局 summary log INFO "LegacyStoryToSprintMigration: created N default sprints, migrated M stories; sprint_id column upgraded to NOT NULL"
- [ ] 8.6 测试 `LegacyStoryToSprintMigrationTest.java`：TC-SPR-MIG-001 (seed Story sprint_id=NULL → migration → 验证默认 Sprint + Story.sprintId 非空 + DB column Null="NO" via INFORMATION_SCHEMA 查询 + log capture)；TC-SPR-MIG-002 (二次跑无效果)

## 9. M09 — 前端 api 层

- [ ] 9.1 新 `frontend/src/api/sprint.ts`：Sprint TS interface + SprintStatus union + Create/Update/ListParams + 5 个 CRUD 函数
- [ ] 9.2 修改 `frontend/src/api/story.ts`：Story type 删 requirementId / requirementCode / requirementTitle 三字段（spec 4 saying 保留 requirementCode/Title 但其实通过 enrich 仍可有，保险起见保留）；加 sprintId NN + sprintCode + sprintName + sprintStatus；StoryCreate.requirementId 删，加 sprintId
- [ ] 9.3 修改 `frontend/src/api/requirement.ts`：删 storyCount 字段，加 sprintCount

## 10. M10 — SprintsPage + SprintEditDrawer

- [ ] 10.1 新 `pages/Sprint/SprintsPage.tsx` + `index.tsx`（参考 ProjectsPage 模式 — CRUD 表格 + 行展开 drilldown 渲染 v0.0.9 StoryListPanel(sprintId=X)）
- [ ] 10.2 新 `pages/Sprint/SprintEditDrawer.tsx`：所属 Requirement 锁定显示 + 默认 owner = current loginName + 可改 + formError 模式（沿用 v0.0.8.1 Code-M7）
- [ ] 10.3 新 `pages/Sprint/SprintsPage.test.tsx`：TC-FES-SPR-07（drilldown 渲染 StoryListPanel(sprintId)）
- [ ] 10.4 新 `pages/Sprint/SprintEditDrawer.test.tsx`：TC-FES-SPR-03 + TC-FES-SPR-04
- [ ] 10.5 复用 v0.0.9 `pages/Requirement/StoryListPanel.tsx`：改 prop 接收 sprintId（也可继续接 requirementId 双兼容；本期改为 sprintId only）

## 11. M11 — Sider + AppRoutes

- [ ] 11.1 修改 `components/AppLayout.tsx`：「需求管理」组加 Sprint 项（位置：项目之后、诉求之前）
- [ ] 11.2 修改 `AppRoutes.tsx`：注册 `/pm/sprints` 路由
- [ ] 11.3 修改 `AppLayout.test.tsx`：5 项（项目 / Sprint / 诉求 / 需求 / 关联）— TC-FES-SPR-01
- [ ] 11.4 修改 `AppRoutes.test.tsx`：加 /pm/sprints mount + grep guard — TC-FES-SPR-02

## 12. M12 — RequirementsPage 重构 + SprintListPanel + StoryEditDrawer 修改

- [ ] 12.1 新 `pages/Requirement/SprintListPanel.tsx`：listSprints({requirementId}) → 表格（id/code/name/status/owner/storyCount actions）+ 「新建 Sprint」按钮 + 编辑/删除按钮（编辑/新建打开 SprintEditDrawer）
- [ ] 12.2 修改 `pages/Requirement/RequirementsPage.tsx`：renderExpanded 由 StoryListPanel 改 SprintListPanel；列名 "Story 数" 改 "Sprint 数"；列 render 由 r.storyCount 改 r.sprintCount
- [ ] 12.3 修改 `pages/Requirement/StoryEditDrawer.tsx`：requirement display 改 sprint display；接收 sprintId/sprintCode/sprintName props；保存 body sprintId 不再传 requirementId
- [ ] 12.4 修改 `pages/Requirement/RequirementsPage.test.tsx`：mock listSprints 而非 listStories；验 SprintListPanel 渲染 + Sprint 数列 — TC-FES-SPR-05
- [ ] 12.5 修改 `pages/Requirement/StoryEditDrawer.test.tsx`：默认 owner 测试仍有效，但锁定字段显示改 sprint — TC-FES-SPR-06；删 v0.0.9 RequirementsPage drilldown 路径如不再用

## 13. M13 — E2E 验证（不 down -v）

- [ ] 13.1 `cd backend && mvn -q spotless:apply test` 全绿（≥ 167 + ≥ 22 新增）
- [ ] 13.2 `cd frontend && npx vitest run` 全绿（≥ 37 + ≥ 5 新增）
- [ ] 13.3 `npx tsc -p tsconfig.json --noEmit` 0 错误（TC-FES-API-1 间接保证）
- [ ] 13.4 `npx vite build` 0 错误
- [ ] 13.5 `cd backend && mvn -q package -DskipTests`
- [ ] 13.6 `docker compose build backend frontend`（不 down -v）
- [ ] 13.7 `docker compose up -d --no-deps --force-recreate backend frontend`
- [ ] 13.8 `docker exec rainier-mysql ... -e "SHOW TABLES"` = 12，含 rainier_sprint
- [ ] 13.9 `DESCRIBE rainier_sprint` 字段集匹配 design
- [ ] 13.10 `DESCRIBE rainier_story` 看 sprint_id Null="NO" 且 requirement_id Null="YES"（迁移已宽松 + 升级）
- [ ] 13.11 docker logs grep 看 INFO 行 "legacy story migrated to default sprint: requirement_id=N → sprint_id=M" + summary
- [ ] 13.12 curl flow:
  - 登录 admin → token
  - GET v0.0.9 REQ-E2E-001 → sprintCount ≥ 1（迁移结果）
  - SELECT sprint_id 从 rainier_story 看 v0.0.9 STR-E2E-001 已迁
  - POST Sprint → 201 + 富化
  - POST Sprint requirementId=999 → 400
  - POST Story w/ sprintId → 201 + projectId 二段继承
  - POST Story w/ sprintId=999 → 400 "sprint not found"
  - DELETE Sprint w/ Story → 409 "sprint has linked stories"
  - DELETE Requirement w/ Sprint → 409 "requirement has linked sprints"
- [ ] 13.13 v0.0.9 测试数据完整：alice/lili/projects/requirements/v0.0.9 story 全部保留（story 现有 sprint_id）
