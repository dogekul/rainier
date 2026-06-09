# v0.0.10.1-cleanup 实现任务清单

## S01 — Cleanup runner + @Order 调整

- [ ] 1.1 在 `backend/src/main/java/com/rainier/sprint/bootstrap/` 新增 `LegacyRequirementIdColumnCleanup.java`
  - `@Component` + `@Order(Ordered.HIGHEST_PRECEDENCE + 2)` + `implements CommandLineRunner`
  - 字段：`@PersistenceContext EntityManager em` + slf4j Logger
  - `run(String...)` 方法 `@Transactional`：调用 private `legacyColumnExists()`（INFORMATION_SCHEMA case-insensitive 查询）；存在则 try MySQL `ALTER TABLE rainier_story DROP COLUMN requirement_id` → 失败回退 H2 同语句（语法相同，try/catch 保险）+ INFO 日志 `"LegacyRequirementIdColumnCleanup: dropped legacy requirement_id column from rainier_story"`；不存在则 INFO `"LegacyRequirementIdColumnCleanup: no-op — requirement_id column already absent"` 立即 return
- [ ] 1.2 修改 `LegacyStoryToSprintMigration.java`：`@Order(Ordered.HIGHEST_PRECEDENCE)` → `@Order(Ordered.HIGHEST_PRECEDENCE + 1)`（显式 +1，与 DanglingProjectIdCleanup 的 +0 / cleanup 的 +2 间隔）
- [ ] 1.3 `mvn -q compile` 确认无 compile error

## S02 — Cleanup runner 测试

- [ ] 2.1 新增 `backend/src/test/java/com/rainier/sprint/bootstrap/LegacyRequirementIdColumnCleanupTest.java`
  - `@SpringBootTest` + `@ActiveProfiles("test")`
  - Logback `ListAppender<ILoggingEvent>` 模式（mirror `LegacyStoryToSprintMigrationTest`）
  - `@BeforeEach` cleanDb + DROP rainier_story.requirement_id（如存在）+ attach appender
  - TC-CLN-001: ADD COLUMN requirement_id BIGINT → `cleanup.run()` → 断言 column count = 0 + INFO log "dropped legacy" 存在
  - TC-CLN-002: 不 ADD 列 → `cleanup.run()` → INFO log "no-op — requirement_id column already absent" 存在 + 严格不含 "dropped legacy"
  - TC-CLN-003: 反射读取 `LegacyStoryToSprintMigration` 与 `LegacyRequirementIdColumnCleanup` 的 `@Order` 注解 value 字段，断言 cleanup.value > migration.value
- [ ] 2.2 `mvn -q test -Dtest=LegacyRequirementIdColumnCleanupTest` 全绿

## S03 — StoryService.enrich 批量化

- [ ] 3.1 重构 `StoryService.list(sprintId, status, priority, page)`：在 `result.getContent()` 上一次性提取 `userIds / sprintIds`；`userRepo.findAllById(...) → Map<Long,User>`；`sprintRepo.findAllById(...) → Map<Long,Sprint>`；从 sprintMap 提取 `requirementIds`；`requirementRepo.findAllById → Map<Long,Requirement>`；从 reqMap 提取 `projectIds`（filter null）；`projectRepo.findAllById → Map<Long,Project>`
- [ ] 3.2 抽取新 private 方法 `enrichBatch(Story, Map<Long,User>, Map<Long,Sprint>, Map<Long,Requirement>, Map<Long,Project>) : StoryDetail`；list 路径用 batch 版本，单 GET / POST / PUT 路径继续用现有 `enrich(Story)`（已经一次单查，正确）
- [ ] 3.3 保护 `findAllById(emptyIterable)` 路径：在 list 首部 if result empty → return empty PageResponse
- [ ] 3.4 `mvn -q test -Dtest=StoryController*Test` 确认富化值与 v0.0.10 一致（既有 12 Story tests 仍绿）

## S04 — StoryListSqlCountTest

- [ ] 4.1 新增 `backend/src/test/java/com/rainier/story/perf/StoryListSqlCountTest.java`
  - `@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")` + MockMvc
  - 在 cleanDb 后 seed 4 Project / 4 Requirement / 5 User / 4 Sprint / 20 Story
  - `Statistics stats = emf.unwrap(SessionFactory.class).getStatistics(); stats.clear();`
  - `mockMvc.perform(get("/api/stories?page=0&size=20"))` → 200
  - 断言 page total=20、SHALL 抽样验证 1 行的 sprintCode/requirementCode/projectCode 富化、`stats.getPrepareStatementCount()` 增量 ≤ 5
- [ ] 4.2 `mvn -q test -Dtest=StoryListSqlCountTest` 全绿

## S05 — SprintService.enrich + storyCount 批量化

- [ ] 5.1 重构 `SprintService.list(requirementId, status, page)`：batch fetch User / Requirement / Project 三 Map；storyCount 改 native SQL `SELECT sprint_id, COUNT(*) FROM rainier_story WHERE del_flag=0 AND sprint_id IN (?1) GROUP BY sprint_id`，参数为 sprintIds → `Map<Long, Long>`；空集合提前 return Empty Map
- [ ] 5.2 抽取 batch enrich 方法；单 GET 路径继续用现有 `enrich(Sprint)`（含 per-row storyCount，单查可接受）
- [ ] 5.3 `mvn -q test -Dtest=SprintController*Test` 全绿（17 Sprint tests 不受影响）

## S06 — SprintListSqlCountTest

- [ ] 6.1 新增 `backend/src/test/java/com/rainier/sprint/perf/SprintListSqlCountTest.java`
  - 模式同 S04；seed 2 Project / 5 Requirement / 3 User / 20 Sprint / 部分 Sprint 含 Story
  - `mockMvc.perform(get("/api/sprints?page=0&size=20"))` → 200
  - 断言 page total=20、抽样验证富化、storyCount 与 native COUNT 一致、`stats.getPrepareStatementCount()` 增量 ≤ 5
- [ ] 6.2 `mvn -q test -Dtest=SprintListSqlCountTest` 全绿

## S07 — 就地加固 3 个 test

- [ ] 7.1 `SprintControllerQueryTest.java` TC-SPR-009：在原有 4 个 jsonPath 断言后追加 22-field array + loop `assertTrue(body.has(f))`（参 `StoryControllerQueryTest:147-177` 写法）
- [ ] 7.2 `SprintControllerCreateTest.java` TC-SPR-001：追加 `$.id` isNumber / `$.code` value("SPR-001") / `$.requirementId` value(reqId) / `$.ownerUserId` value(userId) echo asserts
- [ ] 7.3 `RequirementControllerQueryTest.java` TC-REQS-SPR-003：在单 GET 路径加 `assertFalse(body.has("storyCount"))`；list 路径在 item 匹配 reqId 时加 `assertFalse(item.has("storyCount"))`
- [ ] 7.4 `mvn -q test -Dtest=SprintControllerQueryTest,SprintControllerCreateTest,RequirementControllerQueryTest` 全绿

## S08 — Archive 文档对齐 + E2E

- [ ] 8.1 `archive/2026-06-08-sprint/design.md` Decision 2 末尾追加 `### Build addendum (2026-06-09)` block：
  - "Hibernate 5.6 emitted `ADD COLUMN sprint_id BIGINT NOT NULL` regardless of `columnDefinition` hint"
  - "MySQL auto-filled existing rows with `sprint_id = 0`"
  - "orphan filter actual: `sprint_id IS NULL OR sprint_id = 0 OR sprint_id NOT IN (SELECT id FROM rainier_sprint WHERE del_flag = 0)`"
  - "INFORMATION_SCHEMA case-sensitive on Linux MySQL — must `LOWER(TABLE_NAME)` / `LOWER(COLUMN_NAME)`"
- [ ] 8.2 `archive/2026-06-08-sprint/design.md` `## Decisions` 章节顶部插入"Proposal → Design Decision Mapping"表（8 行 × `[proposal# | design#§ | 备注]`）
- [ ] 8.3 `archive/2026-06-08-sprint/test-plan.md` TC-SPR-MIG-001 描述：将"seed Story sprint_id=NULL"改为"seed Story sprint_id=NULL（或 =0，或指向已删 Sprint — broadened filter）"；标注 Phase 5 修正
- [ ] 8.4 `archive/2026-06-08-sprint/slices.md` M08 章节 native SQL 例子：`WHERE sprint_id IS NULL` → `WHERE sprint_id IS NULL OR sprint_id = 0 OR sprint_id NOT IN (SELECT id FROM rainier_sprint WHERE del_flag = 0)`；标注 Phase 5 修正
- [ ] 8.5 E2E 验证：
  - `mvn -q test` 全绿（≥ 194 backend = 186 baseline + 3 cleanup + 1 perf-story + 1 perf-sprint = 191 + S07 就地加固不增量，实际 ≥ 191 个 test 方法）
  - `npm test` 全绿（41 frontend — 无变化）
  - `npm run build` 成功
  - `mvn -q package -DskipTests` → docker compose build backend → `docker compose up -d --no-deps --force-recreate backend` → sleep 5
  - 启动日志含：`DanglingProjectIdCleanup` (existing) + `LegacyStoryToSprintMigration` no-op (twice-deployed) + `LegacyRequirementIdColumnCleanup: dropped legacy requirement_id column from rainier_story` (first boot 后) 或 `no-op` (后续)
  - `docker exec rainier-mysql mysql -uroot -prainier_root rainier -e "DESCRIBE rainier_story"` 严格无 `requirement_id` 列
  - 二次重启 backend，启动日志含 `LegacyRequirementIdColumnCleanup: no-op` 行
- [ ] 8.6 自动调用 `stdd-verify`
