# v0.0.10-sprint 技术设计

> 对应 Phase 1：`proposal.md`
> 基线：v0.0.9-story（commit 20d175e, tag v0.0.9-story）
> Gate 1 锁定的 8 项决策详见 proposal §I + .stdd.yaml `design_decisions_pre_locked`

## Context

### 技术栈与约束（v0.0.9 不变）

- Spring Boot 2.7.18 + Java 8 + Hibernate JPA (Flyway DISABLED, `ddl-auto=update`) + MySQL 8
- React 18 + Vite + TS + Zustand + Axios + vitest + RTL
- 单 `BaseEntity` (Long id + 6 审计字段 + del_flag)
- `@SQLDelete` + `@Where("del_flag = 0")` 软删模式
- 服务级 code 唯一（无 DB UNIQUE）
- VARCHAR(16) 状态字段 + Java `Set<String> ALL` 校验
- 富化 enrich 模式
- `@Order(Ordered.HIGHEST_PRECEDENCE)` CommandLineRunner 启动自愈模式（v0.0.8 `DanglingProjectIdCleanup` 已建立家族模板）

### v0.0.9 现状（v0.0.10 起点）

11 张表：`rainier_demand` / `rainier_demand_requirement` / `rainier_organization` / `rainier_position` / `rainier_project` / `rainier_requirement` / `rainier_role` / `rainier_story` / `rainier_user` / `rainier_user_organization` / `rainier_user_role`。

`rainier_story` v0.0.9 关键约束：
- `requirement_id BIGINT NOT NULL` (FK to rainier_requirement.id)
- `project_id BIGINT NULL` (从父 Requirement 创建时继承)
- `owner_user_id BIGINT NOT NULL` (可改)
- 6 项状态机
- service `existsByCode` 服务级唯一性
- 软删 + del_flag

v0.0.8 `DanglingProjectIdCleanup` 在每次启动时 NULL 掉 Story.project_id / UserRole.project_id 指向不存在 Project 的 dangling refs。本次 v0.0.10 复用其家族模板做迁移自愈。

### 风险背景

v0.0.10 引入 `Story.sprint_id BIGINT NN`，但**已有 v0.0.9 Story 数据没有 sprint_id**——直接 NN 约束会阻止启动。必须**先迁移再加约束**。

---

## Decisions

> **Proposal → Design Mapping addendum (v0.0.10.1-cleanup, 2026-06-09)**
> The v0.0.10 proposal locked 8 decisions; this design enumerates 14 (1..14, with 12.1/12.2 sub-points). Mapping for traceability:
>
> | Proposal # | Design § | Note |
> |---|---|---|
> | 1 (Sprint 引入 + 三段层级) | §1 (Sprint NN FK to Requirement) | 1:1 |
> | 2 (Sprint 状态 4 项 PLANNING/ACTIVE/COMPLETED/CANCELLED) | §3 | 1:1 |
> | 3 (Sprint 字段集) | §4 | 1:1 |
> | 4 (RequirementService.delete FK 改 Sprint) | §7 + §8 | 1:N — §7 changes the FK check; §8 swaps enrichment storyCount→sprintCount |
> | 5 (Story.requirementId 代码层移除；DB 列遗留为死列) | §5 + §9 + §13 | 1:N — §5 declares the strategy; §9 implements 2-stage enrich; §13 frontend type |
> | 6 (前端 RequirementsPage drilldown → SprintListPanel) | §11 + §12 + §12.1 + §12.2 + §14 | 1:N — drilldown reshape touches 5 frontend axes |
> | 7 (Sprint 不强制 time coherence — 层级语义) | §4 (字段集说明) | 1:1 — embedded in §4 prose |
> | 8 (LegacyStoryToSprintMigration on first boot) | §2 + §6 + §10 | 1:N — §2 = DB-NN strategy; §6 = migration impl; §10 = Sprint.delete Story-ref check |

### 1. Sprint 必须挂 Requirement（NN FK）

**方案**：`Sprint.requirement_id BIGINT NOT NULL`；Service create 时校验 `requirementRepo.existsById(req.getRequirementId())`，不存在 → 400 "requirement not found"。

**为什么**：严格三层模型 Requirement → Sprint → Story。Sprint 不能游离，否则 Story 的 projectId 二段继承链路（sprint.requirement.projectId）会断。

**备选方案及排除原因**：
- 跨 Requirement 共享 Sprint：标准 agile 用法，但用户在 Round 1 Q2 锁定语义为"需求拆解中间层"，跨 Requirement 共享与该语义冲突
- 允许游离 Sprint（仅挂 Project）：模型多分支；与 proposal explicitly_excluded 一致排除

### 2. Story.sprint_id 一次性达到 DB 层 NOT NULL — 迁移收尾 ALTER（v0.0.10 修订）

**方案**（用户 Gate 2 修订：DB 层必须 NN）：v0.0.10 启动序列为：

1. Spring 容器启动 → Hibernate ddl-auto=update：(a) 创 `rainier_sprint` 表 (b) 对 `rainier_story` 执行 `ADD COLUMN sprint_id BIGINT`（**Hibernate 默认 NULL**）。Entity 字段用 `@Column(name="sprint_id", nullable=false, columnDefinition="BIGINT")` —— `columnDefinition` 覆盖 Hibernate 的 NN DDL 生成，让 ADD COLUMN 落地为 NULL，安全添加到有数据的表
2. `LegacyStoryToSprintMigration` CommandLineRunner @Order(HIGHEST_PRECEDENCE) 启动：
   - **Step 1**：扫 `Story.sprint_id IS NULL` 行 → 按 `requirement_id` 分组 → 每组创建默认 Sprint → UPDATE 该组 Story 的 sprint_id
   - **Step 2** (NEW)：执行 native `ALTER TABLE rainier_story MODIFY COLUMN sprint_id BIGINT NOT NULL` —— 在所有 sprint_id 已填充后，DB 列约束升级为 NN
   - **幂等保证**：Step 1 找不到 NULL 行时早退 → Step 2 不执行；后续启动 ALTER 不会触发（Step 1 early return）
3. 迁移结束后：DB 列层面 `sprint_id BIGINT NOT NULL`；Java 层 `@Column(nullable=false)` 与 DB 一致；DTO `@NotNull` 阻止新写入 NULL；三层一致

**为什么**：
- 用户要求 DB 层 NOT NULL（Gate 2 修订）
- Hibernate ddl-auto=update 不能在已有数据列上加 NN，所以必须 (a) 先 nullable ADD COLUMN (b) 应用层填充 (c) 应用层 ALTER 升级
- 后续启动 ALTER 不会再触发，因为迁移 Step 1 检测到没有 NULL 行就早退（family pattern with v0.0.8 DanglingProjectIdCleanup）
- 一次性部署不需要二次重启

**备选方案及排除原因**：
- 两次部署（v0.0.10a 填数据 → v0.0.10b 手动 ALTER）：清晰但需要两次重启 + 运维 SOP
- columnDefinition 写 `"BIGINT NOT NULL"` 让 Hibernate 强制 NN：ADD COLUMN 失败崩盘（有数据的表 NULL → NN 矛盾）
- 等到 v0.0.11+ 单独 cleanup ALTER：DB 层不一致窗口期太长，与 Gate 2 修订意图相悖

**Hibernate `columnDefinition` 行为说明**：`columnDefinition="BIGINT"` 让 Hibernate 把列定义为 `BIGINT`（默认 NULL），无视 `nullable=false`。这是 JPA 规范允许的覆盖。在 Step 2 ALTER 后，DB 是 `BIGINT NOT NULL`，与 entity 的 `nullable=false` 一致。后续 ddl-auto=update 不会修改已存在列。

#### Build addendum (2026-06-09, recorded by v0.0.10.1-cleanup)

> Phase 4 BUILD 和 M13 E2E 实际验证发现，前文 "columnDefinition 行为说明" 的假设**不成立**。下面记录修正后的事实，并保留原文以呈现"当时怎么想 → 后来发现什么"的演进轨迹。

实际观察到的行为：

1. **Hibernate 5.6 不遵守 `columnDefinition="BIGINT"` 的隐含 NULL**。Hibernate 仍按 `nullable=false` 生成 `ADD COLUMN sprint_id BIGINT NOT NULL`。
2. **MySQL 8 在有数据的表上执行 `ADD COLUMN ... NOT NULL`** 时不会失败 — 它**默认填充 `0`** 到所有既存行（数值类型隐式默认 0），DDL 成功完成。
3. 由此 v0.0.10 启动序列里，Step 1 的 `sprint_id IS NULL` 探测 **没有命中任何行**，迁移看似 no-op；但实际所有遗留 Story 都被 MySQL 静默置为 `sprint_id = 0`，**指向不存在的 Sprint id=0**，造成数据孤儿。

修正后的真实 orphan 过滤逻辑（Phase 4 Code-H2 fix）：

```sql
SELECT s.id, s.requirement_id FROM rainier_story s
 WHERE s.del_flag = 0
   AND (s.sprint_id IS NULL
        OR s.sprint_id = 0
        OR s.sprint_id NOT IN (SELECT id FROM rainier_sprint WHERE del_flag = 0))
```

即 "三种孤儿都补"：真正的 NULL（其他方言可能出现）、MySQL ADD COLUMN 自动填的 0、以及指向已删 Sprint 的悬空引用。

附带其他 Phase 4 发现：

- **`INFORMATION_SCHEMA` 区分大小写**：MySQL 在 Linux 上的 `TABLE_NAME` 列实际是小写 `rainier_story`，所以 `WHERE TABLE_NAME = 'RAINIER_STORY'` 永远 0 行。所有此类查询必须用 `WHERE LOWER(TABLE_NAME) = 'rainier_story'` / `LOWER(COLUMN_NAME) = '...'`。
- **H2 vs MySQL ALTER 语法**：MySQL 用 `ALTER TABLE ... MODIFY COLUMN ... NOT NULL`；H2 用 `ALTER TABLE ... ALTER COLUMN ... SET NOT NULL`。migration 用 try / catch 双语法适配 + `addSuppressed` 保留原始 stack。
- **Step 2 ALTER 必须解耦于 `storiesMigrated > 0`**（Phase 5 Code-H2 fix）：原方案下若第一次启动 JVM 在 Step 1 后崩溃，第二次启动 Step 1 早退 → Step 2 永不执行 → DB 列永远停在 nullable。修正后 Step 2 单独探测 `IS_NULLABLE`，无条件 fix。

v0.0.10.1-cleanup（本变更）追加一条 cleanup runner `LegacyRequirementIdColumnCleanup` 在 migration 之后 DROP `requirement_id` 死列，完成 Decision 5 的 "DB 列遗留 → v0.0.11+ DROP" 收尾。

### 3. Sprint 状态机 4 项（PLANNING / ACTIVE / COMPLETED / CANCELLED）

**方案**：`SprintStatus.ALL = unmodifiableSet({PLANNING, ACTIVE, COMPLETED, CANCELLED})`，VARCHAR(16) 列，service 校验。

**为什么**：与 Project 5 项同量级；语义清晰；无业务过程状态（不需 BLOCKED，因为 Sprint 是拆解层，不直接执行）。

**备选方案及排除原因**：
- 沿用 Story 6 项含 BLOCKED：Sprint 不直接执行所以 BLOCKED 语义弱
- 只有 OPEN/CLOSED：表达力不足

### 4. Sprint 字段集（含 dates/goal）

**方案**：`code, name, description, goal, status, requirement_id, owner_user_id, start_date, end_date` + 6 审计 + del_flag = 16 字段（含 id）。

- `goal` VARCHAR(2000)：本 Sprint 交付目标描述
- `start_date` / `end_date` DATE NULL：参考元数据，**Service 不做时间一致性校验**

**为什么**：dates 字段不强校验对应 proposal Decision 2 "语义是层级而非时间箱"。goal 字段为飞轮 ② 风险雷达后续抽业务价值预留锚点。

### 5. Story.requirementId 代码层删除（DB 列遗留为死列）

**方案**：
- `Story` entity 删除 `private Long requirementId` 字段 + getter/setter
- `StoryCreateRequest` 删除 `requirementId` 字段（替换为 `sprintId`）
- `StoryUpdateRequest` 无 `requirementId` 字段（v0.0.9 也无，因为 update 不可改 requirementId）
- `StoryDetail` 保留 `requirementId` 字段（业务读路径仍需），但 source 改为 `sprint.requirementId`（service enrich 二段 join）
- `StoryService.create`：参数从 `requirementId` 改为 `sprintId`；从 sprintId → sprint.requirementId → requirement.projectId 链路继承
- `StoryRepository.countByRequirementId(Long)` 方法**保留**（RequirementService.delete 不再调用，但 LegacyStoryToSprintMigration 需要按 requirement_id 反查）
- DB 列 `rainier_story.requirement_id` 保留（ddl-auto=update 不删列），由 v0.0.11+ cleanup 单独 DROP

**为什么**：
- 模型纯净：Story 只认 Sprint，Requirement 信息通过 sprint 二段 join 获取
- DB 列遗留死列符合 ddl-auto=update 行为；v0.0.11+ DROP 时再做 cleanup
- StoryRepository.countByRequirementId 保留为迁移工具——LegacyMigration 需要从遗留死列读取 (read-only 一次)

**备选方案及排除原因**：
- 同时保留 Story 实体 requirementId 字段（冗余）：避免漂移，但 sprintId 更换或 sprint.requirementId 变化时不同步——v0.0.10 不引入这复杂度

### 6. LegacyStoryToSprintMigration 实现策略

**方案**：`@Component @Order(Ordered.HIGHEST_PRECEDENCE) @Transactional` CommandLineRunner，伪代码：

```java
public void run(String... args) {
  // 1. 找所有 sprint_id IS NULL 的 Story，按 requirement_id 分组
  String selectSql =
      "SELECT id, requirement_id FROM rainier_story WHERE sprint_id IS NULL AND del_flag = 0 FOR UPDATE";
  List<Object[]> orphans = em.createNativeQuery(selectSql).getResultList();
  if (orphans.isEmpty()) return;  // idempotent: subsequent boots no-op

  Map<Long, List<Long>> byReq = groupBy(orphans);  // requirementId -> List<storyId>
  int sprintsCreated = 0, storiesMigrated = 0;

  for (Map.Entry<Long, List<Long>> e : byReq.entrySet()) {
    Long reqId = e.getKey();
    Requirement req = requirementRepo.findById(reqId).orElse(null);
    if (req == null) {
      log.warn("orphan stories under deleted requirement_id={}; skipping {} rows",
               reqId, e.getValue().size());
      continue;
    }
    Sprint defaultSprint = new Sprint();
    defaultSprint.setCode("SPRINT-DEFAULT-" + req.getCode());
    defaultSprint.setName("默认 Sprint");
    defaultSprint.setStatus(SprintStatus.ACTIVE);
    defaultSprint.setRequirementId(reqId);
    defaultSprint.setOwnerUserId(req.getOwnerUserId());
    Sprint saved = sprintRepo.saveAndFlush(defaultSprint);
    sprintsCreated++;

    String updateSql = "UPDATE rainier_story SET sprint_id = :sid WHERE id IN (:ids)";
    int updated = em.createNativeQuery(updateSql)
                    .setParameter("sid", saved.getId())
                    .setParameter("ids", e.getValue())
                    .executeUpdate();
    storiesMigrated += updated;
    log.info("legacy story migrated to default sprint: requirement_id={} → sprint_id={}, {} stories",
             reqId, saved.getId(), updated);
  }
  log.info("LegacyStoryToSprintMigration: created {} default sprints, migrated {} stories",
           sprintsCreated, storiesMigrated);
}
```

**为什么**：
- @Order(HIGHEST_PRECEDENCE) 确保 v0.0.8 `DanglingProjectIdCleanup` 与此 migration 不冲突（family pattern）
- 单 @Transactional 确保整组原子（要么全迁要么全回滚）
- FOR UPDATE 锁住 orphans 避免 v0.0.8 Code-M2 family race
- 默认 Sprint owner = Requirement.owner 保证不丢权限上下文
- 迁移日志含 per-Requirement INFO + 全局 summary 便于运维核对

**备选方案及排除原因**：
- 不做迁移、直接拒绝旧 Story：违反"不删数据" v0.0.8 family 原则
- 每个 Story 单独建 Sprint：膨胀 Sprint 数；按 Requirement 分组更符合"默认拆解"语义

### 7. RequirementService.delete FK 检查改 Sprint

**方案**：
```java
// v0.0.9: storyRepo.countByRequirementId(id) > 0 → 409 "requirement has linked stories"
// v0.0.10 替换为：
sprintRepo.countByRequirementId(id) > 0 → 409 "requirement has linked sprints"
```

仍保留 demand_requirement 检查在前（既有顺序）。

**为什么**：v0.0.10 起 Story 必属于 Sprint，所以 Requirement 下层引用应通过 Sprint 计数，不再直接通过 Story 计数。

### 8. RequirementService.enrich 改 sprintCount（移除 storyCount）

**方案**：`RequirementDetail.storyCount` 字段删除，新增 `sprintCount`；service `sprintRepo.countByRequirementId(r.getId())`。

**为什么**：前端 RequirementsPage drilldown 改为展示 Sprints 列表；Sprint 数比 Story 数更符合"Requirement 拆解粒度"的管理视角。

**未来扩展**：v0.0.11+ 可加 `storyCountTotal` 跨 Sprint 总数（service 双查 join sprint.id IN ... AND story.del_flag = 0），本期不做。

### 9. StoryService.enrich 二段 join Sprint → Requirement

**方案**：service 注入 `SprintRepository`；enrich 顺序：
```java
sprintRepo.findById(s.getSprintId()).ifPresent(sprint -> {
  dto.setSprintCode(sprint.getCode());
  dto.setSprintName(sprint.getName());
  // 二段 join requirement
  requirementRepo.findById(sprint.getRequirementId()).ifPresent(req -> {
    dto.setRequirementCode(req.getCode());
    dto.setRequirementTitle(req.getTitle());
  });
});
```

projectId 在 create 时已从 sprint.requirement.projectId 继承到 Story.projectId 列，read 路径只需 join project（与 v0.0.9 不变）。

**为什么**：StoryDetail 兼容 v0.0.9 字段集（requirementCode/requirementTitle 保留），通过二段 join 透明获取。

### 10. SprintService.delete 加 Story FK 保护

**方案**：`sprintService.delete(id)` 先调 `storyRepo.countBySprintId(id) > 0 → 409 "sprint has linked stories"`，再调 `repo.delete(s)` 触发 @SQLDelete。

需要 `StoryRepository.countBySprintId(Long)` 方法。

**为什么**：与 Project DELETE FK 保护、Requirement DELETE FK 保护 家族一致。

### 11. 前端 RequirementsPage drilldown 改 SprintListPanel

**方案**：
- 列 "Story 数" 替换为 "Sprint 数"（render `r.sprintCount`）
- `renderExpanded` 从 `<StoryListPanel requirementId={r.id} />` 改为 `<SprintListPanel requirementId={r.id} />`
- `SprintListPanel` 子组件：表格列 [code/name/status/dates/owner/sprintActions]；每行有「编辑」+「删除」+「在 Sprint 页打开」（跳 `/pm/sprints?requirementId=R`）按钮
- 新建 Sprint 抽屉 `SprintEditDrawer`：锁定 Requirement 显示，与 v0.0.9 StoryEditDrawer 模式一致

**为什么**：Round 2 Q3 锁定。两层叙事链：Requirement drilldown 看 Sprints，Sprint drilldown 看 Stories。

### 12. 前端 /pm/sprints 独立菜单 + SprintsPage

**方案**：
- Sider「需求管理」组追加「Sprint」菜单项（位于「项目」之后、「诉求」之前）
- `/pm/sprints` 路由 → `SprintsPage` 组件
- SprintsPage：列表 + CRUD + 行展开 → `<StoryListPanel sprintId={s.id} />`（复用 v0.0.9 组件，传 sprintId 而非 requirementId）

**为什么**：用户可以直接在 Sprint 维度查看跨 Requirement 的工作；也可以从 RequirementsPage drilldown 进入 Sprint 看其下 Stories。

### 12.1 StoryListPanel API contract 改造

**方案**：`StoryListPanel` props 从 `requirementId` 改为 `sprintId`；调用 `listStories({sprintId: X})` 而非 `{requirementId: X}`。

**为什么**：Story 现在属于 Sprint，sprintId 是过滤路径。

### 12.2 StoryEditDrawer locked field 改 Sprint

**方案**：v0.0.9 抽屉锁定显示 "Requirement"，v0.0.10 改为 "Sprint"。`sprintId` + `sprintCode` + `sprintName` 通过 props 传入。「所属 Requirement」改为只读显示 `requirementTitle (requirementCode) — 通过 Sprint`。

### 13. api/story.ts 字段切换

**方案**：
- `Story.requirementId` 字段（v0.0.9）→ v0.0.10 替换为 `Story.sprintId: number`
- `Story.requirementCode / requirementTitle` 保留（via enrichment 来自 sprint→requirement）
- 新增 `Story.sprintCode / sprintName`
- `StoryCreate` 参数：`requirementId` → `sprintId`

### 14. api/requirement.ts 字段切换

**方案**：`Requirement.storyCount` → `Requirement.sprintCount`。

---

## Architecture

### 数据流（v0.0.10）

```
   Project ◄────┐
                │
                │ project_id (NN, FK)
                │
        Requirement ◄────────────┐
                                  │ requirement_id (NN, FK)
                                  │
                              Sprint ◄────────────┐
                                                   │ sprint_id (NN, code-level via @Column nullable=false)
                                                   │
                                               Story (legacy requirement_id column orphaned)
```

### 启动序列（v0.0.10 首次部署）

```
1. JVM start → Spring context refresh
2. Hibernate ddl-auto=update:
   - CREATE TABLE rainier_sprint
   - ALTER TABLE rainier_story ADD COLUMN sprint_id BIGINT NULL
3. CommandLineRunner @Order(HIGHEST_PRECEDENCE) start:
   - DanglingProjectIdCleanup (v0.0.8) — typically no-op
   - LegacyStoryToSprintMigration (v0.0.10) — 创默认 Sprint + 迁旧 Story
4. Tomcat connector unlatch → HTTP traffic
5. Subsequent boots: Migration scans 0 orphans → early return
```

### Class layout (后端)

```
com.rainier.sprint
├── domain
│   ├── Sprint.java                # @SQLDelete + @Where
│   └── SprintStatus.java          # 4 constants + ALL Set
├── repository
│   └── SprintRepository.java      # existsByCode / countByRequirementId / countByOwnerUserId / JpaSpecificationExecutor
├── dto
│   ├── SprintCreateRequest.java
│   ├── SprintUpdateRequest.java
│   └── SprintDetail.java          # 业务字段 + ownerName/ownerLoginName + requirementCode/requirementTitle + projectName/projectCode + storyCount
├── service
│   └── SprintService.java         # inject SprintRepo + RequirementRepo + UserRepo + StoryRepo + ProjectRepo
├── controller
│   └── SprintController.java      # 5 REST endpoints
└── bootstrap
    └── LegacyStoryToSprintMigration.java   # @Component @Order(HIGHEST_PRECEDENCE)

com.rainier.story (改造)
├── domain/Story.java              # 删 requirementId 字段；加 sprintId @Column(nullable=false) BIGINT
├── dto/StoryCreateRequest.java    # requirementId 删，sprintId NN 加
├── dto/StoryDetail.java           # 加 sprintCode/sprintName 字段；requirementCode/requirementTitle 仍存（via二段 join）
├── repository/StoryRepository.java  # 加 countBySprintId(Long)；保留 countByRequirementId 给 LegacyMigration
└── service/StoryService.java      # create 改 sprint-based；enrich 二段 join；inject SprintRepository

com.rainier.requirement (改造)
├── dto/RequirementDetail.java     # storyCount 字段改 sprintCount
└── service/RequirementService.java # inject SprintRepository；delete FK 改 sprint；enrich 改 sprintCount
```

### REST API

| HTTP | Path | 说明 |
|---|---|---|
| POST | `/api/sprints` | 创 Sprint |
| GET | `/api/sprints/{id}` | 单详情（含 storyCount 富化） |
| GET | `/api/sprints?requirementId=&status=&search=&page=&size=` | 分页 |
| PUT | `/api/sprints/{id}` | 更新（owner/status/dates/goal/description 可改；requirementId 不可改） |
| DELETE | `/api/sprints/{id}` | 软删（FK 保护：Story 引用 → 409） |

### Class layout (前端)

```
frontend/src/
├── api/
│   ├── sprint.ts                  # NEW
│   ├── story.ts                   # 改 requirementId → sprintId
│   └── requirement.ts             # 改 storyCount → sprintCount
└── pages/
    ├── Sprint/                    # NEW
    │   ├── SprintsPage.tsx        # CRUD + drilldown → StoryListPanel(sprintId)
    │   ├── SprintEditDrawer.tsx
    │   └── index.tsx
    └── Requirement/
        ├── RequirementsPage.tsx   # 改 renderExpanded → SprintListPanel；列改 sprintCount
        ├── SprintListPanel.tsx    # NEW
        ├── StoryEditDrawer.tsx    # 锁定字段改 Sprint
        └── StoryListPanel.tsx     # props 改 sprintId
```

---

## Risks / Trade-offs

| 风险 | 缓解措施 |
|---|---|
| Hibernate ddl-auto=update 不能在已有数据上加 NOT NULL | 列层保持 nullable，service @NotNull + @Column(nullable=false) 阻止新写入 NULL；DB 强化由 v0.0.11+ cleanup |
| LegacyStoryToSprintMigration 启动失败导致整个应用不启动 | @Transactional 保证回滚；migration 异常会冒泡阻止启动（强约束）；若 requirement 已被硬删则 WARN skip 不阻塞 |
| StoryRepository.countByRequirementId 死方法残留 | 注释明确"用于 LegacyMigration 一次性反查；v0.0.11+ DROP requirement_id 列时同步删该方法" |
| RequirementService.enrich 不再返回 storyCount，前端 RequirementsPage 仍有 v0.0.9 测试断言"3" | 修改 RequirementsPage 列 + 测试同步切换，否则前端测试 fail |
| StoryListPanel props 改造影响 v0.0.9 RequirementsPage drilldown 测试 | 必须改前端测试 mock 配合 sprintId 改造，否则 TC-FES-S02 fail |
| Hibernate 同步 schema 时遇 sprint_id 列已存在但 NN 不一致 | 仅首次启动添加列；后续 ddl-auto=update no-op |
| 用户在 Requirement drilldown 展开 Sprint 后立即想看其 Story → 跳页 | SprintListPanel 行加 「在 Sprint 页打开」 → /pm/sprints?focus=<sprintId> 跳转 + drilldown 自动展开（best-effort，本期不做参数自动展开） |
| Sprint code 与 Story code 命名空间不同（家族一致：每个实体独立 code 空间） | 沿用 v0.0.9 Story 模式；code 唯一性 service 级，各表独立 |
| 死列 `rainier_story.requirement_id` 占用空间 + 容易让维护者误读 | spec 文档明示死列 + v0.0.11+ DROP；列上加 SQL 注释（如 `COMMENT 'legacy v0.0.9; superseded by sprint_id v0.0.10'`）但 ddl-auto=update 不可控注释，作为 TODO |
