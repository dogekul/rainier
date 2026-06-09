# v0.0.10.1-cleanup — Technical Design

> Baseline: commit `c806f04` / tag `v0.0.10-sprint` (2026-06-09).
> Change type: cleanup (mixed code + tests + archive docs).

## Context

v0.0.10-sprint Phase 5 Review 留下 **3 H + 12 M 项未修复**（见 `archive/2026-06-08-sprint/pending-adjustments.md §C/§D`）。其中：

- 真正影响运行的：
  - `rainier_story.requirement_id` 列已无代码读写但 DB schema 仍持有（v0.0.10 显式延后到 v0.0.11+ DROP），列保留期长会让 `SHOW CREATE TABLE` 误导未来 reader。
  - `StoryService.enrich` / `SprintService.enrich` 在 list 路径上每行单独查 `user/sprint/requirement/project`（StoryService 还含 sprintRepo 一次单查）— size=20 时单页 ~80 个 SELECT；同时 `SprintService.storyCount` 是 per-row `countBySprintId`，N+1 双重。
  - `LegacyStoryToSprintMigration` 与 `DanglingProjectIdCleanup` 都用 `@Order(HIGHEST_PRECEDENCE)`，Spring 排序未定，逻辑上独立但未来加 cleanup runner 会踩雷。
- 真正影响测试可信度的：
  - `TC-SPR-009` 只断言 4 个字段（mirror `TC-STR-010` 应有 22 字段）→ 静默契约漂移盲区。
  - `TC-SPR-001` 不断言响应体的 `id/code/requirementId/ownerUserId` echo → POST 响应缩水无人察觉。
  - `TC-REQS-SPR-003` 不断言 `body.has("storyCount") == false` → v0.0.9 残留字段悄悄回来无人察觉。
- 归档文档与 Phase 4 实测的偏差（不影响运行但误导未来 reader）：
  - `archive/2026-06-08-sprint/design.md` Decision 2 仍说 `columnDefinition="BIGINT"` 让 ADD COLUMN 落地为 NULL — 实测 Hibernate 没认这个 hint，MySQL 用 `NOT NULL` 加列并自动填 `0`；migration 的 orphan filter 实际是 `IS NULL OR =0 OR NOT IN live sprints`。
  - design.md 14 条决策没有显式 1:1 映射回 proposal 锁定的 8 条。
  - `test-plan.md` TC-MIG + `slices.md` M08 的 native SQL 示例仍写 `WHERE sprint_id IS NULL`。

技术栈和约束：Spring Boot 2.7.18 / Java 8 / Hibernate JPA / MySQL 8 / H2 (test profile) / React 18 / Vite / TypeScript / Vitest。Spring `CommandLineRunner` + `@Order` + `EntityManager` 原生 SQL 已被 `LegacyStoryToSprintMigration` 验证可用；`Hibernate Statistics` 内建支持 SQL count 断言。

## Decisions

### 1. DROP COLUMN 走独立 `CommandLineRunner`，不合并进 LegacyStoryToSprintMigration

**方案**：新增 `LegacyRequirementIdColumnCleanup`（@Component + @Order + implements CommandLineRunner），只负责一件事：通过 `INFORMATION_SCHEMA` 案例不敏感地检测 `rainier_story.requirement_id` 是否存在，存在则 `ALTER TABLE rainier_story DROP COLUMN requirement_id` + INFO 日志；不存在则 INFO no-op 日志后立即返回。idempotent — 二次重启自然走 no-op 分支。

**为什么**：
- 单一职责：`LegacyStoryToSprintMigration` 的契约是 "data 自愈 + sprint_id 升级 NN"，DROP 是另一类操作（不可逆 schema 撤销），混进同一 runner 会让 Phase 5 Test-H3 "MIG-002 二次启动 no-op" 类断言更复杂。
- 删除路径清晰：v0.0.12+ 当 LegacyStoryToSprintMigration 也可以删时（已确认数据全迁完），可以独立移除 cleanup runner，不需要拆 migration。
- 测试隔离：可以单独写 `LegacyRequirementIdColumnCleanupTest`（参 `LegacyStoryToSprintMigrationTest` 的 Logback ListAppender 模式），不影响 migration test。

**备选方案及排除原因**：
- 备选 A — 加进 `LegacyStoryToSprintMigration.run()` 的 Step 3：违反单一职责；测试断言 "migrated stories>0 才 ALTER" 的逻辑会和 "不管 storiesMigrated 是否>0 都应该 DROP 列" 冲突。
- 备选 B — 用 Flyway/Liquibase 迁移脚本：项目至今 Flyway 仍禁用（baseline v0.0.6 Decision），引入会拉大变更面。

### 2. 显式 @Order 间隔 — migration `+ 1`，cleanup `+ 2`

**方案**：
- `LegacyStoryToSprintMigration` 改为 `@Order(Ordered.HIGHEST_PRECEDENCE + 1)`
- `LegacyRequirementIdColumnCleanup` 新增 `@Order(Ordered.HIGHEST_PRECEDENCE + 2)`
- `DanglingProjectIdCleanup`（v0.0.8）保持 `@Order(Ordered.HIGHEST_PRECEDENCE)`，跑在最前

**为什么**：
- 显式顺序避免 Spring 排序未定（Phase 5 Code-M3）。
- DanglingProjectIdCleanup 处理的是 `requirement.project_id`，与 sprint/requirement_id 链路无依赖 — 放最前不会触发新行为。
- LegacyStoryToSprintMigration 必须先跑完（Step 0 loosen + Step 1 fill + Step 2 NN upgrade），cleanup 才能安全 DROP `requirement_id` 列（migration 的 Step 0 用 INFORMATION_SCHEMA 检测该列存在性来决定是否走自愈路径 — 列 DROP 后再 boot 时 migration 早返回，符合预期）。

**备选方案及排除原因**：
- 备选 A — 不动 @Order，依赖 Spring 默认排序：违反 Phase 5 Code-M3，未来加 runner 风险面更大。
- 备选 B — 把所有 runner 合并成一个 `BootstrapHealing` 类：单一类承担太多语义，违反 Phase 4 教训（单职责 runner 容易测）。

### 3. enrich 批量化 — 一次 `findAllById(setOf(ids))` + `Map<Long, Entity>` 拼接

**方案**：在 `StoryService.list` 和 `SprintService.list` 的 enrich 路径中：

```java
// pseudo
Set<Long> userIds = result.stream().map(Story::getOwnerUserId).collect(toSet());
Set<Long> sprintIds = result.stream().map(Story::getSprintId).collect(toSet());
Map<Long, User> userMap = userRepo.findAllById(userIds).stream()
    .collect(toMap(User::getId, identity()));
Map<Long, Sprint> sprintMap = sprintRepo.findAllById(sprintIds).stream()
    .collect(toMap(Sprint::getId, identity()));
// then enrich each story by map lookup, not by-id query
```

**为什么**：
- `JpaRepository.findAllById(Iterable<Long>)` 已是 Spring Data JPA 标准 API，生成 `WHERE id IN (?, ?, ...)` 单 SELECT。Java 8 + Spring Boot 2.7 完整支持。
- 4 个被 join 的实体（User / Sprint / Requirement / Project）—— page=20 时 enrich 阶段从 ~80 个 SELECT 降到 4 个 batch SELECT。
- **Phase 4 实测修正（PA-1）**：Spring Data `findAll(Specification, Pageable)` 强制下发 **2** 个语句（data + count），不是 1 — 总计 2 page + 4 batch = **6**，不是 5。Sprint case 是 2 page + 3 batch + 1 storyCount aggregate = **6**。规格 / 测试 / 设计文本统一为 6。
- 不引入新依赖、不动 entity 关系、不引入 fetch graph。

**备选方案及排除原因**：
- 备选 A — `@EntityGraph` / JPQL `JOIN FETCH`：要修改 repository 方法签名 + 引入 Hibernate 反向解析，对 ad-hoc enrich 字段（projectName/Code 间接来自 Requirement.projectId）支持差。
- 备选 B — Hibernate 二级缓存：v0 不在生产场景，预热与失效都是新维度。
- 备选 C — DTO Projection JPQL：每个 list 端点要写专用 query，DTO 字段集变化时重复劳动。

### 4. storyCount 聚合 — native SQL `GROUP BY sprint_id`

**方案**：`SprintService.list` 中替换 per-row `storyRepo.countBySprintId(id)` 为：

```java
List<Object[]> rows = em.createNativeQuery(
    "SELECT sprint_id, COUNT(*) FROM rainier_story "
    + "WHERE del_flag = 0 AND sprint_id IN (?1) "
    + "GROUP BY sprint_id")
    .setParameter(1, sprintIds)
    .getResultList();
Map<Long, Long> storyCountMap = rows.stream()
    .collect(toMap(r -> ((Number) r[0]).longValue(),
                   r -> ((Number) r[1]).longValue()));
```

**为什么**：
- Spring Data JPA derived query 不直接支持 `countBy<Field> group by <Field>` 这种"分组返回多行"语义；自定义 `@Query` 写起来差不多，native 还能避免方言惊吓。
- 一次 round-trip，page=20 时 storyCount 维度从 20 个 COUNT 降到 1。
- 与 `LegacyStoryToSprintMigration` 的 native SQL 用法一致（同 codebase）。

**备选方案及排除原因**：
- 备选 A — 在 `Sprint` 实体加 `@Formula("(SELECT COUNT(*) FROM rainier_story WHERE sprint_id=id AND del_flag=0)")`：load 时每行单查，N+1 没真解；且 `@Formula` 让实体行为依赖 schema。
- 备选 B — 用 Spring Data JPA `@Query("SELECT s.id, COUNT(st) FROM Sprint s LEFT JOIN Story st ON st.sprintId=s.id WHERE s.id IN :ids AND st.delFlag=0 GROUP BY s.id")`：能行，但 JPQL 写法已经和 native 复杂度相当，且要在 SprintRepository 引入 Story 跨实体 JPQL（破坏 module 边界）。

### 5. SQL count 断言用 Hibernate Statistics（而非自建 logger 拦截）

**方案**：在 `application-test.properties` 或测试本地 `@SpringBootTest(properties=…)` 中启用 `hibernate.generate_statistics=true`；新增 `StoryListSqlCountTest` / `SprintListSqlCountTest` 用 `Session.getSessionFactory().getStatistics()` 取 `queryStatistics` / `prepareStatementCount` 在 list 调用前后 diff，断言增量 ≤ 5。

**为什么**：
- Hibernate Statistics 是标准 API，不需要 mock JDBC 层；测试稳定不依赖日志格式。
- 与 v0.0.8 `DanglingProjectIdCleanupTest` 用 Logback ListAppender 的模式互补 — 一类断 log，一类断 statistics，都是黑盒。

**备选方案及排除原因**：
- 备选 A — DataSource 包装器拦截 PreparedStatement：侵入太重，影响别的测试。
- 备选 B — 跳过测试，仅在 E2E 手动看 SQL log：Success Criteria 失去自动化保护，N+1 容易回归。

### 6. 测试断言加固策略 — 直接在现有 test 方法里加 assert，不新建测试方法

**方案**：
- `SprintControllerQueryTest.get_existingId_returnsFullDetailAndEnriched`（TC-SPR-009）添加 22-字段 loop assert（mirror `StoryControllerQueryTest:147-177`）。
- `SprintControllerCreateTest.post_minimalPayload_returns201WithDefaultsAndEnrichment`（TC-SPR-001）添加 `$.id`/`$.code`/`$.requirementId`/`$.ownerUserId` echo asserts。
- `RequirementControllerQueryTest.get_requirement_includesSprintCountEnrichment`（TC-REQS-SPR-003）添加 `assertFalse(body.has("storyCount"))`（detail + 每个 list item 都断言）。

**为什么**：
- 这些断言是同一 Scenario 的 invariants，分裂成新方法会让测试名失去 "what is this Scenario about" 的内聚性。
- 与 v0.0.10 已采用的 "loop over field array" 模式一致。

**备选方案及排除原因**：
- 备选 A — 新增独立 mutation-test 类：项目无 pitest 等设施，引入太重。

### 7. Archive 文档对齐 — addendum block，不重写原文

**方案**：
- `archive/2026-06-08-sprint/design.md` Decision 2 末尾追加 `### Build addendum (2026-06-09)` 块，明确 "Hibernate 没有遵守 `columnDefinition`；MySQL `ADD COLUMN NOT NULL` 在有数据时自动填 0；orphan filter 因此宽化为 `IS NULL OR =0 OR NOT IN (live sprints)`；INFORMATION_SCHEMA 在 MySQL 必须 `LOWER(TABLE_NAME)`"。
- `archive/2026-06-08-sprint/design.md` 顶部 `## Decisions` 章节开头插入 "Proposal → Design Decision Mapping" 表，建立 proposal 8 决策与 design 14 决策的 1:N / 1:1 / 0:1 映射。
- `archive/2026-06-08-sprint/test-plan.md` TC-SPR-MIG-001 案例 + `archive/2026-06-08-sprint/slices.md` M08 native SQL 片段 — 把 `WHERE sprint_id IS NULL` 改为宽 filter 文本，括号说明这是 Phase 5 修正后的实际实现。

**为什么**：
- 归档目录是历史记录；保留原文 + 追加 addendum 让未来 reader 能看到"当时怎么想 → 后来发现什么"的演进轨迹，比直接覆盖更有教育价值。
- 不动文件路径不会让 Phase 6 deliver 的 archive 链接失效。

**备选方案及排除原因**：
- 备选 A — 直接覆盖原文：丢失"Hibernate 没遵守 columnDefinition"这个未来 reader 应该知道的 lesson learned。

## Architecture

### Boot-time CommandLineRunner 序列（v0.0.10.1）

```
Application start
        │
        ▼
DanglingProjectIdCleanup            (@Order HIGHEST_PRECEDENCE)
        │   清理 requirement.project_id 指向已删 Project
        ▼
LegacyStoryToSprintMigration        (@Order HIGHEST_PRECEDENCE + 1)
        │   Step 0: ALTER rainier_story.requirement_id → NULL (if NN)
        │   Step 1: 给孤儿 Story 创建 default Sprint + UPDATE sprint_id
        │   Step 2: ALTER rainier_story.sprint_id → NOT NULL (if still NULL)
        ▼
LegacyRequirementIdColumnCleanup    (@Order HIGHEST_PRECEDENCE + 2)   ⬅ NEW
        │   if column rainier_story.requirement_id exists:
        │       ALTER TABLE rainier_story DROP COLUMN requirement_id
        │       log INFO "dropped legacy column"
        │   else:
        │       log INFO "no-op — column already absent"
        ▼
Application ready
```

幂等性：所有 runner 都先用 INFORMATION_SCHEMA 探测 schema 真值再决定 ALTER 是否执行；二次启动自然走 no-op 路径。

### enrich 批量化数据流（StoryService.list 示例）

```
list(sprintId, ...) page=20
        │
        ▼
repo.findAll(spec, pageRequest)  →  Page<Story> stories (20 entities)
        │
        ▼
extract ids:
  userIds  = stories.map(getOwnerUserId).toSet()
  sprintIds = stories.map(getSprintId).toSet()
        │
        ▼
batch fetch:
  userRepo.findAllById(userIds)     →  Map<Long, User>
  sprintRepo.findAllById(sprintIds) →  Map<Long, Sprint>
  reqIds = sprintMap.values.map(getRequirementId).toSet()
  requirementRepo.findAllById(reqIds) → Map<Long, Requirement>
  projectIds = reqMap.values.map(getProjectId).filterNotNull().toSet()
  projectRepo.findAllById(projectIds) → Map<Long, Project>
        │
        ▼
stories.map(s -> enrich(s, userMap, sprintMap, reqMap, projMap))
        │
        ▼
PageResponse<StoryDetail>
```

总 SELECT：1 (page) + 4 (batch joins) = **5**（v0.0.10 是 1 + 20*4 = 81）。

### Test stack

`StoryListSqlCountTest` / `SprintListSqlCountTest`：

```java
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class StoryListSqlCountTest {
    @Autowired EntityManagerFactory emf;
    @Autowired StoryController controller;
    @Test
    void list_size20_enrichmentExecutesNoMoreThan5Queries() {
        // seed 20 stories
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        controller.list(null, null, null, PageParams.of(0, 20, ""));
        assertThat(stats.getPrepareStatementCount()).isEqualTo(6L);  // PA-1: 5 → 6 (2 page + 4 batch)
    }
}
```

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|----------|
| `ALTER TABLE ... DROP COLUMN requirement_id` 在生产 MySQL 上是 metadata-only 还是 copy-table 取决于版本 — 8.0+ 通常 INSTANT，但大表风险仍需评估 | 当前 v0 数据量小（单元/E2E 验证）；在 production deploy 前需确认 MySQL 版本支持 INSTANT DROP COLUMN，否则需要在维护窗口操作。本 PR 不解决生产 deploy 策略（v0 单 instance）。|
| `findAllById(Iterable)` 的批量大小若超过 Hibernate `IN` 子句限制（Oracle 1000，MySQL 65535）会失败 | page size 服务端 cap=100（PageParams 校验），远低于阈值。|
| Hibernate Statistics 测试在 Spring Boot 多 test class 串行运行时可能受其它 test 的查询影响 | 测试内显式 `stats.clear()` 后再调；`@SpringBootTest` 的 ApplicationContext 缓存机制保证 statistics 实例在同一 test class 内一致。|
| Phase 4 已发现 H2 vs MySQL ALTER 语法差异 — 本次 cleanup runner 的 DROP COLUMN 也需要同样 fallback 吗？ | 实际 `ALTER TABLE ... DROP COLUMN <col>` 在 H2 和 MySQL 语法相同（不像 NOT NULL 的 `MODIFY vs ALTER COLUMN SET`），单语句即可。仍 wrap try/catch 加日志保险。|
| archive 文档改动会让 v0.0.10-sprint 的 git diff 出现"对历史的二次编辑"，造成历史不可信感 | 仅追加（addendum block），不删原文；commit message 明确说"v0.0.10.1 cleanup addendum to v0.0.10 archive — lessons learned"。|
| `LegacyRequirementIdColumnCleanup` 跑完后，列被删掉；如果 production MySQL 上有未迁移的 Story 行（不可能但理论上）会先经 LegacyStoryToSprintMigration 迁移，所以 cleanup 跑时已经无 reader 引用该列 | Phase 5 Code-H2 已加固 — migration 的 ALTER sprint_id 到 NOT NULL 解耦于 storiesMigrated 计数；意味着不管 Story 表有没有数据，cleanup 之前的状态都是"sprint_id NN ready + requirement_id 已无 reader"。|
