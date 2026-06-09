# v0.0.10.1-cleanup 切片执行计划

> 8 切片，全 P0；S01-S07 可分两条独立链并行（cleanup-runner 链 + enrich-perf 链 + 测试加固独立），S08 收口。

## 切片表

| # | 优先级 | TC 覆盖 | 实现目标 | 依赖 |
|---|--------|---------|----------|------|
| S01 | P0 | — | `LegacyRequirementIdColumnCleanup.java` 新增（@Order HIGHEST_PRECEDENCE + 2）+ `LegacyStoryToSprintMigration.java` @Order 改 +1 | 无 |
| S02 | P0 | TC-CLN-001/002/003 | `LegacyRequirementIdColumnCleanupTest.java` 新增 — ListAppender 模式 + INFORMATION_SCHEMA 断言 + @Order 注解检查 | S01 |
| S03 | P0 | — | `StoryService.enrich`/`list` 批量化：`findAllById(setOf(ids))` + `Map<Long, Entity>` 拼接（User/Sprint/Requirement/Project 各一次） | 无 |
| S04 | P0 | TC-PERF-STR-001 | `StoryListSqlCountTest.java` 新增 — `hibernate.generate_statistics=true` + `Statistics.getPrepareStatementCount()` 增量断言 ≤ 5 | S03 |
| S05 | P0 | — | `SprintService.enrich`/`list` 批量化（User/Requirement/Project 各一次）+ storyCount 改 native GROUP BY 单查 | 无 |
| S06 | P0 | TC-PERF-SPR-001 | `SprintListSqlCountTest.java` 新增 — 同 S04 模式，断言 ≤ 5 | S05 |
| S07 | P0 | TC-SPR-009-FULL / TC-SPR-001-ECHO / TC-REQS-SPR-003-NEG | 就地加固 3 个 test 方法：SprintControllerQueryTest TC-SPR-009 加 22 字段 loop；SprintControllerCreateTest TC-SPR-001 加 echo asserts；RequirementControllerQueryTest TC-REQS-SPR-003 加 storyCount 缺席断言 | 无 |
| S08 | P0 | — | Archive 文档对齐（design.md Decision 2 addendum + 决策映射表 / test-plan.md TC-MIG + slices.md M08 orphan filter 文本宽化）+ E2E 验证（mvn test + npm test + npm run build + docker compose up --no-deps --force-recreate + DESCRIBE rainier_story 无 requirement_id + 启动日志含 cleanup INFO + 二次重启 no-op） | S01-S07 |

## 依赖图

```
S01 ── S02 ──────────────┐
                          ├── S08
S03 ── S04 ──────────────┤
                          │
S05 ── S06 ──────────────┤
                          │
S07 ──────────────────────┘
```

## 隐藏陷阱记录

- **陷阱 A — DROP COLUMN 与 LegacyStoryToSprintMigration 的 INFORMATION_SCHEMA 探测顺序**：cleanup runner @Order(+2) 在 migration @Order(+1) 后执行；如果 cleanup 先 DROP，migration 二次启动后会发现 `requirement_id` 列已 absent → migration 的 `legacyRequirementIdColumnExists()` 返回 false → 直接 early return（这是期望行为，不算 bug）。S08 E2E 验证二次重启时两 runner 都走 no-op 日志路径。
- **陷阱 B — Hibernate Statistics 默认未启用**：S04/S06 测试必须用 `@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")` 覆盖 application-test.properties，否则 `stats.getPrepareStatementCount()` 返回 0 → 测试假绿。
- **陷阱 C — `findAllById(emptyCollection)` 行为**：Spring Data JPA 对空 Iterable 返回空 List，不发 SQL。enrich 实现需先 `if (ids.isEmpty()) return emptyMap()` 保护，避免 batch 阶段无意触发 IN () 语法错误。
- **陷阱 D — native GROUP BY `IN (?1)` 参数绑定**：JPA `setParameter(1, Collection)` 通常 OK，但 H2 与 MySQL 对 `IN` 子句空集合反应不同；同样 `if (sprintIds.isEmpty()) skip` 保护。
- **陷阱 E — S07 就地加固字段顺序**：`SprintControllerQueryTest.TC-SPR-009` 已存在的 4 个断言不能误删；新增 22 字段 loop 在原断言之后追加（mirror v0.0.10 TC-STR-010 写法）。
- **陷阱 F — Archive 文档 commit 与 production deploy 风险**：S08 archive 文档改动会让 v0.0.10-sprint 历史 commit 出现"对历史的二次编辑"。commit message 必须明确"addendum，不改写"。
