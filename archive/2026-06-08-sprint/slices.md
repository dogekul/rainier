# v0.0.10-sprint 切片执行计划

> 13 切片（M01-M13）：后端 8 + 前端 4 + E2E 1
> 全部 P0；严格依赖链；M06+M08 是高风险点（Story 改造 + 启动迁移含 DDL ALTER）

## 切片表

| # | 优先级 | TC 覆盖 | 实现目标 | 依赖 |
|---|---|---|---|---|
| M01 | P0 | — (基础设施) | `com.rainier.sprint` 新包：Sprint domain + SprintStatus + Repo + DTO×3。Entity Story 字段 sprintId 用 `@Column(name="sprint_id", nullable=false, columnDefinition="BIGINT")` —— Hibernate ADD COLUMN 落地为 NULL（关键：Decision 2 修订） | 无 |
| M02 | P0 | TC-SPR-001..008 | SprintService.create + enrich + Controller POST。注入 SprintRepo + RequirementRepo + UserRepo + ProjectRepo + StoryRepo（for storyCount enrich）。时间字段 service 不做一致性校验（TC-SPR-008 end<start 仍 201） | M01 |
| M03 | P0 | TC-SPR-009..011 | SprintService.findById + list（Specification: requirementId/status/search）+ Controller GET 单/列表 | M02 |
| M04 | P0 | TC-SPR-012..014 | SprintService.update（owner 可改，沿用 v0.0.8 Decision 6b）+ Controller PUT | M02 |
| M05 | P0 | TC-SPR-015..017 | SprintService.delete（软删 + Story FK 保护 → 409 "sprint has linked stories"）+ Controller DELETE | M02 |
| M06 | P0 | TC-STR-SPR-001..004 | **Story 实体改造**：移除 requirementId 字段（DB 列保留为死列）、添加 sprintId NN；StoryCreateRequest 删 requirementId 加 sprintId；StoryDetail 添 sprintCode/sprintName/sprintStatus；StoryService.create 改：用 sprintRepo.findById → 取 sprint.requirementId → 取 requirement.projectId 二段继承；enrich 二段 join sprint+requirement+project。**改写 16 个旧 Story 测试** (TC-STR-001..016) 全部传 sprintId | M02 |
| M07 | P0 | TC-REQS-SPR-001..003 | **RequirementService 改造**：注入 SprintRepository、移除 StoryRepository 依赖；delete FK 检查从 storyRepo 改 sprintRepo → 409 "requirement has linked sprints"；enrich 用 sprintCount 替换 storyCount；RequirementDetail.storyCount 字段删，加 sprintCount。改写 v0.0.9 TC-REQS-001/001b/002 → TC-REQS-SPR-001/002/003 | M02 |
| M08 | P0 | TC-SPR-MIG-001/002 | **LegacyStoryToSprintMigration** `@Component @Order(HIGHEST_PRECEDENCE) implements CommandLineRunner` + `@Transactional`：Step 1: native SQL `SELECT id, requirement_id FROM rainier_story WHERE sprint_id IS NULL AND del_flag=0`（**Phase 5 修正：实际 filter 宽化为 `sprint_id IS NULL OR sprint_id = 0 OR sprint_id NOT IN (SELECT id FROM rainier_sprint WHERE del_flag = 0)` — Hibernate 5.6 不遵守 `columnDefinition="BIGINT"`；MySQL 在有数据的表 `ADD COLUMN NOT NULL` 自动填 0**）（注意：用 native 因为 Story 实体已无 requirementId 字段）→ 按 requirement_id group → 每 group 创默认 Sprint (`code="SPRINT-DEFAULT-{reqCode}", name="默认 Sprint", status="ACTIVE", owner=req.owner`) → native UPDATE rainier_story SET sprint_id=新值 WHERE id IN (...)；Step 2: `ALTER TABLE rainier_story MODIFY COLUMN sprint_id BIGINT NOT NULL`（**Phase 5 修正：解耦于 Step 1 计数 — 探测 `IS_NULLABLE` 后无条件升级，避免崩溃后第二次启动跳过**）；log INFO per requirement + summary | M06 + M07 |
| M09 | P0 | TC-FES-API-1 | 前端 api 层：新 `api/sprint.ts`（Sprint type + 5 CRUD）；`api/story.ts` 删 requirementId 字段、加 sprintId + sprintCode/sprintName；`api/requirement.ts` 删 storyCount 加 sprintCount | M02 (后端 endpoint 形状稳定) |
| M10 | P0 | TC-FES-SPR-02/03/04/07 | `pages/Sprint/SprintsPage.tsx`（参考 v0.0.8 ProjectsPage CRUD + 行展开 drilldown）+ `SprintEditDrawer.tsx`（默认 owner + 可改 + 表单错误）+ `SprintsPage.test.tsx` 测 drilldown 渲染 StoryListPanel(sprintId)。复用 v0.0.9 StoryListPanel（传 sprintId 而非 requirementId） | M09 |
| M11 | P0 | TC-FES-SPR-01/02 | Sider 加 Sprint 项（位于「项目」之后「诉求」之前）+ AppRoutes 注册 `/pm/sprints` + AppLayout.test / AppRoutes.test 扩展 | M10 |
| M12 | P0 | TC-FES-SPR-05/06 | RequirementsPage drilldown 改：`renderExpanded` 改 `SprintListPanel`（新组件）；"Story 数" 列改 "Sprint 数"。`SprintListPanel.tsx` 新建（参考 v0.0.9 StoryListPanel 但 listSprints + drilldown 二级展开 Stories 不在此期）。StoryEditDrawer 锁定字段改 Sprint 显示（接收 sprintId/sprintCode/sprintName props）。RequirementsPage.test 改、StoryEditDrawer.test 改 | M10 + M11 |
| M13 | P0 | E2E | `mvn package` + `docker compose build backend frontend` + `up -d --no-deps --force-recreate`（**不 down -v**）+ docker exec mysql 验证：SHOW TABLES = 12 含 rainier_sprint + DESCRIBE rainier_sprint + DESCRIBE rainier_story 看 sprint_id Null="NO"（**ALTER 已升级**）+ docker logs 看 INFO 行迁移日志 + curl flow（创 Sprint、bad requirementId 400、创 Story w/ sprintId 二段继承 projectId 富化、DELETE Sprint with Story 409、DELETE Requirement with Sprint 409、GET Requirement sprintCount）+ 验证 v0.0.9 STR-E2E-001 已被迁到 SPRINT-DEFAULT-REQ-E2E-001 | M08 + M12 |

## 依赖图

```
M01 ── M02 ── M03
         │
         ├──── M04
         │
         ├──── M05
         │
         ├──── M06 ──┐
         │           │
         ├──── M07 ──┤
         │           │
         └────────── M08 ── M13
                            │
M09 ── M10 ── M11 ──────────┤
                            │
       M10 ── M12 ──────────┘
```

## 长程模式特别注意（v0.0.8/v0.0.9 教训累积）

- **Decision 2 关键点**：Story.sprintId `@Column(name="sprint_id", nullable=false, columnDefinition="BIGINT")` —— `columnDefinition` 必填，否则 Hibernate ADD COLUMN BIGINT NOT NULL 会在有数据的 rainier_story 表崩盘
- **M08 native SQL 必须**：Story entity 已无 requirementId 字段，migration 不能用 storyRepo.findAllBy* —— 必须用 `em.createNativeQuery("SELECT id, requirement_id FROM rainier_story ...")`
- **M08 ALTER 时机**：Step 2 ALTER 必须在 Step 1 UPDATE 完成且 transaction commit 后执行（或同 transaction 内通过 EntityManager.flush）。MySQL `ALTER TABLE` 是隐式 DDL commit，所以在 @Transactional 内执行会自动 commit 前面的 Step 1 UPDATE
- **rainier_story.requirement_id 死列**：M06 删除 entity 字段后，DB 列仍存在 NOT NULL（v0.0.9 时定义）。Hibernate ddl-auto=update 不会主动 DROP。要让旧列继续有 NULL 被允许吗？— 不会，因为旧的 NN 约束依然有效，但写入路径已不再写它 → 旧约束触发？答案：Hibernate INSERT 不带这一列时，DB 用 column default 或 NULL。v0.0.9 rainier_story.requirement_id 是 NN，没默认值 → INSERT 会失败！**这是个隐藏陷阱**
- **修复方案**（在 M06 内处理）：M06 必须先 ALTER TABLE rainier_story MODIFY COLUMN requirement_id BIGINT NULL（允许 NULL）才能让新代码 INSERT 不带 requirement_id 列。这一步加进 LegacyStoryToSprintMigration Step 3 或单独 startup ALTER
- **Java 8 兼容**：SprintStatus.ALL 用 `Collections.unmodifiableSet(new HashSet<>(Arrays.asList(...)))` —— 不能用 `Set.of`
- **无 DB UNIQUE**：Sprint.code 服务级唯一
- **前端 listX size: 100**：v0.0.7 教训
- **测试隔离**：所有 @SpringBootTest `cleanDb()` 按 FK 顺序：story → sprint → requirement → project / user
- **frontend test mock 全 promise resolve**：用 `await waitFor`
- **测试 createBy 弱断言**：test profile auditor 注入"system"
- **删除后 GET 404 检查**：@Where 过滤

## 隐藏陷阱记录（Phase 4 BUILD 必处理）

**陷阱 A**：rainier_story.requirement_id 列在 v0.0.9 是 NN，v0.0.10 新代码不再写它 → INSERT 失败
- **解决**：在 LegacyStoryToSprintMigration 加 Step 0：`ALTER TABLE rainier_story MODIFY COLUMN requirement_id BIGINT NULL`（放宽约束），早退条件：DESCRIBE 看到该列已是 NULL 则跳过

## 后续 STDD 衔接

所有 M01..M13 完成 → 自动进入 stdd-verify → Step 0-5 → Gate 3 等用户确认。
