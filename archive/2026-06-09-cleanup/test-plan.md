# v0.0.10.1-cleanup 测试方案与详细案例

> 版本：v0.0.10.1-cleanup
> 创建日期：2026-06-09
> 对应 Phase 2 Spec：`specs/entity-story/spec.md`、`specs/entity-sprint/spec.md`、`specs/entity-requirement/spec.md`
> 基线：commit `c806f04` / tag `v0.0.10-sprint`（186 backend + 41 frontend tests）

## 一、测试策略

### 1.1 测试金字塔

- **单元/集成**：8 新增 P0 backend test cases（含 1 新 test class + 4 个现有 test 的加固）。前端无新增（archive 文档 + 后端性能/契约测试为主）。
- **E2E**：不引入新 E2E 文件；M13 阶段的 curl flow 沿用 v0.0.10 已有 + 增加 `SHOW CREATE TABLE rainier_story` 验证列已 DROP。

### 1.2 测试原则

- 优先复用 v0.0.10 已有 fixture 与 helper（`createSprint` / `createStory` / Logback `ListAppender` 模式）。
- N+1 性能用 Hibernate `Statistics.getPrepareStatementCount()` 做断言，不依赖日志格式（Decision 5）。
- 加固 v0.0.10 既有 test 时**就地**加 assert（不新建 test 方法），保持 Scenario 内聚（Decision 6）。
- 字段集守护断言 `body.has(field)` / `body.has("storyCount") == false`，能在 mutation testing 下立刻红。

### 1.3 已有测试资产（v0.0.10 baseline）

| 测试文件 | 用例数 | 类型 | 本次相关性 |
|----------|--------|------|------------|
| `LegacyStoryToSprintMigrationTest.java` | 2 | 集成 | Decision 1/2 — cleanup runner 是 sibling；测试模式（ListAppender + INFORMATION_SCHEMA 断言）直接 mirror |
| `SprintControllerCreateTest.java` | 8 | 集成 | B2 加固 TC-SPR-001 echo asserts |
| `SprintControllerQueryTest.java` | 6 | 集成 | B1 加固 TC-SPR-009 22-字段集 |
| `RequirementControllerQueryTest.java` | 5 | 集成 | B3 加固 TC-REQS-SPR-003 storyCount 缺席 |
| `StoryControllerQueryTest.java` | 6 | 集成 | 性能基线参考（TC-STR-010 22-字段集） |
| `DanglingProjectIdCleanupTest.java` (v0.0.8) | — | 集成 | runner 测试 + Logback ListAppender 模式参考 |

## 二、详细测试案例

### 功能 1：`LegacyRequirementIdColumnCleanup` runner（entity-story Requirement #1）

#### 案例 1.1 — TC-CLN-001 — 首次启动 DROP 遗留列 + 输出 INFO 日志

| 字段 | 内容 |
|------|------|
| **ID** | TC-CLN-001 |
| **对应 Spec** | `entity-story/spec.md` → Scenario: 首次启动 DROP 遗留列 + 输出 INFO 日志 |
| **优先级** | P0 |
| **预置条件** | H2 schema 模拟 v0.0.10 状态：`ALTER TABLE rainier_story ADD COLUMN requirement_id BIGINT`；Logback ListAppender 已 attach 到 `LegacyRequirementIdColumnCleanup` logger |
| **输入** | 调用 `cleanup.run()` |
| **预期结果** | INFORMATION_SCHEMA 查询 `requirement_id` 列返回 0 行；ListAppender 含 INFO log `"LegacyRequirementIdColumnCleanup: dropped legacy requirement_id column from rainier_story"`；既有 Story 行（含 sprint_id, code）SHALL 保留 |
| **当前状态** | ❌ 新增（`LegacyRequirementIdColumnCleanupTest`） |

#### 案例 1.2 — TC-CLN-002 — 二次启动 no-op + 输出 no-op 日志

| 字段 | 内容 |
|------|------|
| **ID** | TC-CLN-002 |
| **对应 Spec** | `entity-story/spec.md` → Scenario: 二次启动 no-op + 输出 no-op 日志 |
| **优先级** | P0 |
| **预置条件** | H2 schema 已 DROP `requirement_id` 列（即 v0.0.10.1 之后的状态）；ListAppender attach 完毕 |
| **输入** | 调用 `cleanup.run()` |
| **预期结果** | INFORMATION_SCHEMA 列计数仍为 0；ListAppender 含 INFO `"LegacyRequirementIdColumnCleanup: no-op — requirement_id column already absent"`；ListAppender 严格不含 `"dropped legacy"` 子串 |
| **当前状态** | ❌ 新增 |

#### 案例 1.3 — TC-CLN-003 — runner 链式顺序：cleanup 跑在 migration 之后

| 字段 | 内容 |
|------|------|
| **ID** | TC-CLN-003 |
| **对应 Spec** | design.md Decision 2（@Order 间隔显式） |
| **优先级** | P0 |
| **预置条件** | Spring `@Order` 注解读取检查（反射 / `AnnotationUtils.getAnnotation`） |
| **输入** | 启动应用，捕获 `ApplicationContext.getBeansOfType(CommandLineRunner.class)` 排序 |
| **预期结果** | 序列 SHALL 为：`DanglingProjectIdCleanup` (HIGHEST_PRECEDENCE) → `LegacyStoryToSprintMigration` (+1) → `LegacyRequirementIdColumnCleanup` (+2)。可读 `@Order` 注解 value 字段断言 |
| **当前状态** | ❌ 新增（与 TC-CLN-001/002 同 test class） |

### 功能 2：StoryService.list enrich 批量化（entity-story Requirement #2）

#### 案例 2.1 — TC-PERF-STR-001 — `GET /api/stories?size=20` enrich ≤ 5 个 SELECT

| 字段 | 内容 |
|------|------|
| **ID** | TC-PERF-STR-001 |
| **对应 Spec** | `entity-story/spec.md` → Scenario: GET /api/stories?size=20 在 enrich 阶段 ≤ 5 个 SELECT |
| **优先级** | P0 |
| **预置条件** | seed 4 Project / 4 Requirement / 5 User / 4 Sprint / 20 Story；`hibernate.generate_statistics=true`；`stats.clear()` |
| **输入** | `mockMvc.perform(get("/api/stories?page=0&size=20"))` |
| **预期结果** | HTTP 200；返回 page.total=20；所有 20 行的 sprintCode / sprintName / requirementCode / requirementTitle / projectName / projectCode / ownerName / ownerLoginName 都正确富化；`stats.getPrepareStatementCount()` 增量 = 6 — 2 page + 4 batch（PA-1 修正自 5） |
| **当前状态** | ❌ 新增（`StoryListSqlCountTest`） |

### 功能 3：SprintService.list + storyCount 批量化（entity-sprint Requirement #1）

#### 案例 3.1 — TC-PERF-SPR-001 — `GET /api/sprints?size=20` enrich ≤ 5 个 SELECT

| 字段 | 内容 |
|------|------|
| **ID** | TC-PERF-SPR-001 |
| **对应 Spec** | `entity-sprint/spec.md` → Scenario: GET /api/sprints?size=20 在 enrich 阶段 ≤ 5 个 SELECT |
| **优先级** | P0 |
| **预置条件** | seed 2 Project / 5 Requirement / 3 User / 20 Sprint / 每 Sprint 0-3 个 Story；`hibernate.generate_statistics=true`；`stats.clear()` |
| **输入** | `mockMvc.perform(get("/api/sprints?page=0&size=20"))` |
| **预期结果** | HTTP 200；page.total=20；所有 Sprint 富化字段正确；每行 `storyCount` 等于该 Sprint 下 `del_flag=0` Story 数量；`stats.getPrepareStatementCount()` 增量 = 6（2 page + 3 batch enrich + 1 storyCount aggregate；PA-1 修正自 5） |
| **当前状态** | ❌ 新增（`SprintListSqlCountTest`） |

### 功能 4：SprintDetail GET 22-字段守护（entity-sprint Requirement #2）

#### 案例 4.1 — TC-SPR-009-FULL — GET 详情 22 字段全有

| 字段 | 内容 |
|------|------|
| **ID** | TC-SPR-009-FULL（就地加固 TC-SPR-009） |
| **对应 Spec** | `entity-sprint/spec.md` → Scenario: GET 详情 22 字段全有（loop assert） |
| **优先级** | P0 |
| **预置条件** | seed User / Project / Requirement / Sprint（Sprint code=SPR-Q1, requirementId=REQ-1） |
| **输入** | `mockMvc.perform(get("/api/sprints/" + id))` |
| **预期结果** | HTTP 200；逐项 `assertTrue(body.has(field))` for 22 字段：`id, code, name, description, goal, status, requirementId, requirementCode, requirementTitle, projectId, projectName, projectCode, ownerUserId, ownerName, ownerLoginName, startDate, endDate, storyCount, createTime, updateTime, createBy, updateBy` |
| **当前状态** | ⚠️ v0.0.10 已存在 TC-SPR-009 但仅断言 4 字段；本案例加 22-字段 loop |

#### 案例 4.2 — TC-SPR-001-ECHO — POST 响应 echo 关键字段

| 字段 | 内容 |
|------|------|
| **ID** | TC-SPR-001-ECHO（就地加固 TC-SPR-001） |
| **对应 Spec** | `entity-sprint/spec.md` → Scenario: POST 响应回包 echo 关键字段 |
| **优先级** | P0 |
| **预置条件** | seed User id=1 / Project / Requirement id=reqId |
| **输入** | `POST /api/sprints` body `{"code":"SPR-001","name":"Phase 1","requirementId":reqId,"ownerUserId":1}` |
| **预期结果** | HTTP 201；`body.id` 为正整数；`body.code="SPR-001"`；`body.requirementId=reqId`；`body.ownerUserId=1`；保持现有富化断言 |
| **当前状态** | ⚠️ v0.0.10 已存在 TC-SPR-001 但缺 echo asserts |

### 功能 5：RequirementDetail 严格不含 storyCount（entity-requirement Requirement #1）

#### 案例 5.1 — TC-REQS-SPR-003-NEG — storyCount 字段严格缺席

| 字段 | 内容 |
|------|------|
| **ID** | TC-REQS-SPR-003-NEG（就地加固 TC-REQS-SPR-003） |
| **对应 Spec** | `entity-requirement/spec.md` → Scenario: GET 详情严格无 storyCount + list item 严格无 storyCount |
| **优先级** | P0 |
| **预置条件** | seed User / Project / Requirement / 3 Sprint (PLANNING / ACTIVE / COMPLETED) |
| **输入** | `GET /api/requirements/{reqId}` + `GET /api/requirements?page=0&size=20` |
| **预期结果** | 单 GET：`body.sprintCount=3` AND `body.has("storyCount") == false`；list GET：找到目标 item 时 `item.sprintCount=3` AND `item.has("storyCount") == false` |
| **当前状态** | ⚠️ v0.0.10 已存在 TC-REQS-SPR-003 但缺 storyCount 缺席断言 |

## 三、测试执行矩阵

| 功能模块 | 单元测试 | 集成测试 | E2E | 状态 |
|----------|---------|----------|-----|------|
| LegacyRequirementIdColumnCleanup runner | — | TC-CLN-001/002/003（new class） | DESCRIBE rainier_story 验证（M13 curl） | 🟢 |
| StoryService.list enrich 批量化 | — | TC-PERF-STR-001（new class） | 手动 SQL log 看 enrich phase | 🟢 |
| SprintService.list enrich + storyCount 批量化 | — | TC-PERF-SPR-001（new class） | 手动 SQL log | 🟢 |
| SprintDetail 22-字段 + POST echo | — | TC-SPR-009-FULL + TC-SPR-001-ECHO（in-place） | — | 🟢 |
| RequirementDetail storyCount 缺席守护 | — | TC-REQS-SPR-003-NEG（in-place） | — | 🟢 |
| Archive doc alignment | — | git diff 审查 | — | 🟢 |

## 四、回归风险矩阵

| 风险区域 | v0.0.10.1 改动 | 已有回归保护 | 风险等级 |
|----------|---------------|-------------|---------|
| `rainier_story` schema | DROP 列 `requirement_id` | 18 Story tests（v0.0.10）全无 `Story.requirementId` 字段引用，编译期已校验；TC-CLN-001/002 启动期校验；TC-CLN-003 顺序校验 | 🟢 |
| `StoryService.enrich` 行为正确性 | 改用 `findAllById` + Map lookup | 12 Story tests 已断言 sprintCode/requirementCode/projectCode 富化值；TC-PERF-STR-001 同时断言富化正确性 + SQL count | 🟢 |
| `SprintService.enrich` + storyCount | 改用 batch fetch + native GROUP BY | 17 Sprint tests 已断言 storyCount=0/3 + ownerName 等；TC-PERF-SPR-001 + TC-SPR-009-FULL 加固 | 🟢 |
| `LegacyStoryToSprintMigration` 行为 | 仅 `@Order` 注解值从 `HIGHEST_PRECEDENCE` 变为 `HIGHEST_PRECEDENCE + 1` | 已有 `LegacyStoryToSprintMigrationTest`（2 test 通过 ListAppender / IS_NULLABLE 断言） | 🟢 |
| 既有 v0.0.9 Story 行存活 | DROP 列前 migration 已保证 sprint_id 填好 | `LegacyStoryToSprintMigrationTest` + M13 E2E（v0.0.10 已验证）+ TC-CLN-001 断言"既有 Story 行保留" | 🟢 |
| Archive 文档可读性 | 仅追加 addendum block，不改原文 | 人眼 review；不影响代码 | 🟢 |
| 生产 MySQL `DROP COLUMN` 锁表风险 | DROP 一列；MySQL 8.0+ INSTANT | 当前 v0 数据量极小；production deploy 策略未在本 PR 触及 | 🟡（运维注意） |

## 五、建议补充顺序

1. **第一优先**（部署前必补 — P0 全部）：
   - TC-CLN-001 / TC-CLN-002 / TC-CLN-003
   - TC-PERF-STR-001 / TC-PERF-SPR-001
   - TC-SPR-009-FULL / TC-SPR-001-ECHO / TC-REQS-SPR-003-NEG
2. **第二优先**（部署后尽快补 — P1）：无（本 PR 无 P1）
3. **第三优先**（后续补 — P2）：无（v0.0.11+ 再议 SprintsPage 全 CRUD / N+1 优化推广到其他服务）
