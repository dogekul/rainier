# v0.0.10-sprint: Sprint 实体补全 Requirement → Sprint → Story 三层

> **版本**：v0.0.10-sprint
> **基线**：v0.0.9-story（需先 Phase 6 DELIVER 打 tag）
> **类型**：domain-entity + breaking-data-shape
> **触发文档**：v0.0.9-story 交付后用户提出"在 Requirement 与 Story 之间插入 Sprint 中间层"

## Why

v0.0.9 的 Story 直挂 Requirement，对小需求够用；但**对大需求，PO 需要把 Requirement 拆成多个发布阶段 / 功能集 / 价值切片**——这个"中间层"目前缺失。Story 是"管理视角的最小垂直切片"，许多 Story 自然属于"同一个发布波次"——这是 Sprint 要表达的概念。

**Sprint 在本系统是"需求拆解的中间层"，不是 agile 时间盒**（Round 1 Q2 锁定）。同一 Requirement 下多个 Sprint 是 PO 的拆解动作，与团队迭代周期解耦。Sprint 仍记录 `start_date / end_date / goal` 作为参考元数据，但 Service 不做时间一致性校验，**不是 scheduled iteration**。

完成 v0.0.10 后，主线叙事变为 **Demand → Requirement → Sprint → Story**，与产品蓝图 A 卡片 2 PO「拆 Epic → Story → Task」高频动作中的 Epic 概念对齐（Sprint = Epic 角色）。Task 在 v0.0.11+ 再补。

## What Changes

- 新表 `rainier_sprint`：requirementId NN + ownerUserId NN（可改）+ status + start_date + end_date + goal + 6 审计 + del_flag
- 后端 5 endpoint CRUD + 4 项状态机（PLANNING / ACTIVE / COMPLETED / CANCELLED）
- Service create 校验 requirementId 存在；ownerUserId 必填、validates 存在；status/start_date/end_date 不做时间一致性强校验
- Service update owner 可改（沿用 v0.0.8 Decision 6b）；requirementId 不可改
- 软删 + FK 保护：删 Sprint 有 Story 引用 → 409 "sprint has linked stories"；删 Requirement 有 Sprint 引用 → 409 "requirement has linked sprints"（替换 v0.0.9 "requirement has linked stories"）
- 富化：返回含 ownerName/ownerLoginName + requirementCode/requirementTitle + projectName/projectCode + storyCount
- Story 改造（v0.0.9 反转）：`Story.requirementId` 列**移除**（在新代码中），新增 `Story.sprintId BIGINT NN`；`Story.projectId` 仍存（从 sprint.requirement.projectId 创建时继承）
- StoryService 改造：create 校验 sprintId 存在并从 sprintId → sprint.requirementId → requirement.projectId 链路继承 projectId；enrich 改 join Sprint（requirementCode/requirementTitle 来自二段 join sprint→requirement）
- RequirementService 改造：delete FK 检查从 storyRepo.countByRequirementId 改为 sprintRepo.countByRequirementId；enrich 字段 storyCount 改为 sprintCount
- **数据迁移自愈**：新增 `LegacyStoryToSprintMigration` CommandLineRunner @Order(HIGHEST_PRECEDENCE) — 启动时扫描所有 `Story.sprint_id IS NULL` 的行，按其 (将被移除的) `requirement_id` 分组，每组创一个 `code="SPRINT-DEFAULT-{requirement.code}", name="默认 Sprint", status=ACTIVE, ownerUserId=requirement.ownerUserId` 的 Sprint，并把该组 Story 的 sprint_id 全部设为新 Sprint id；log INFO per Requirement + summary per run。**仅在首次 v0.0.10 启动有效**，subsequent boots no-op
- 前端：新增 `/pm/sprints` 路由 + SprintsPage（独立 CRUD 页面，沿用 ProjectsPage 模式）+ Sider「需求管理」组追加「Sprint」菜单项（位于「项目」之后）
- 前端 RequirementsPage 改造：drilldown 子区域从 StoryListPanel 改为 **SprintListPanel**；"Story 数" 列改为 "Sprint 数"
- 前端 SprintsPage 内部 drilldown：每个 Sprint 行展开 → 复用 StoryListPanel（v0.0.9 已有，传入 sprintId）
- 前端 StoryEditDrawer 改造：「所属需求」锁定显示替换为「所属 Sprint」锁定显示（含 Sprint name + Requirement name 二段）
- 前端 api/story.ts：`Story.requirementId` 字段删，加 `sprintId`；create 参数同步
- 前端 api/requirement.ts：`storyCount` 字段移除，加 `sprintCount`

## Capabilities

### Modified Capabilities

- **entity-requirement**：DELETE FK 保护检查改为 sprint 引用；enrich 改 sprintCount；移除 storyCount
- **entity-story**：requirementId 列从代码层面删除 → sprintId NN；create/update 接受 sprintId；enrich projectId 继承链路改为 sprint.requirement.projectId；FE Drawer 锁定字段显示 Sprint
- **frontend-scaffold**：Sider「需求管理」组追加「Sprint」；/pm/sprints 路由 + SprintsPage CRUD；RequirementsPage drilldown 改 SprintListPanel；StoryEditDrawer 锁定字段改 Sprint

### New Capabilities

- **entity-sprint**：Sprint CRUD + 4 项状态机 + Requirement 子层 + Story 父层 + owner 可改 + 富化 + 软删 + FK 保护 + 启动自愈迁移

## Impact

### A. 代码层面 — 后端（com.rainier.sprint 新包 10 文件）

- `domain/Sprint.java` + `domain/SprintStatus.java`
- `repository/SprintRepository.java`（含 existsByCode / countByRequirementId / countByOwnerUserId）
- `dto/SprintCreateRequest.java` / `SprintUpdateRequest.java` / `SprintDetail.java`
- `service/SprintService.java`（注入 SprintRepo + RequirementRepo + UserRepo + StoryRepo）
- `controller/SprintController.java`
- `bootstrap/LegacyStoryToSprintMigration.java`（@Component + @Order(HIGHEST_PRECEDENCE)，沿用 v0.0.8 DanglingProjectIdCleanup 模式）

### B. 代码层面 — 后端改造

- `requirement/service/RequirementService.java`：注入 SprintRepository；delete FK 改 sprintRepo；enrich storyCount → sprintCount
- `requirement/dto/RequirementDetail.java`：storyCount 字段改 sprintCount
- `story/domain/Story.java`：requirementId 列删，sprintId 加（@Column NN）
- `story/dto/StoryCreateRequest.java`：requirementId 字段删，sprintId NN 字段加
- `story/dto/StoryUpdateRequest.java`：不动（不可改 sprintId/projectId）
- `story/dto/StoryDetail.java`：requirementId 字段 + requirementCode/requirementTitle 仍保留但 source 改为 sprint.requirement
- `story/service/StoryService.java`：create / update / enrich 全改 sprint-based；projectId 继承链路 sprint → requirement → projectId

### C. 代码层面 — 后端测试

新：
- `SprintControllerCreateTest.java`（TC-SPR-001..009）
- `SprintControllerQueryTest.java`（TC-SPR-010..015）
- `SprintControllerDeleteTest.java`（TC-SPR-016..017，含 Story FK 保护）
- `LegacyStoryToSprintMigrationTest.java`（TC-SPR-MIG-001 验证迁移）

改：
- `StoryControllerCreateTest / QueryTest / DeleteTest`：全部 fixture 加 createSprint，body 传 sprintId 不再传 requirementId（约 16 用例修改）
- `RequirementControllerDeleteTest` TC-REQS-001/001b：Story FK 改 Sprint FK；TC-REQ-009 不动
- `RequirementControllerQueryTest` TC-REQS-002：storyCount 改 sprintCount

### D. 代码层面 — 前端

新：
- `api/sprint.ts`
- `pages/Sprint/SprintsPage.tsx` + `index.tsx`
- `pages/Sprint/SprintEditDrawer.tsx`
- `pages/Requirement/SprintListPanel.tsx`

改造：
- `api/story.ts`：requirementId → sprintId 字段切换
- `api/requirement.ts`：storyCount → sprintCount
- `pages/Requirement/RequirementsPage.tsx`：renderExpanded 改 SprintListPanel；列改 sprintCount
- `pages/Requirement/StoryEditDrawer.tsx`：requirement display → sprint display；接收 sprintId/sprintCode/sprintName props
- `pages/Requirement/StoryListPanel.tsx`：接收 sprintId 而非 requirementId，调用 listStories({sprintId: X})
- `components/AppLayout.tsx`：Sider「需求管理」组加 Sprint 项
- `AppRoutes.tsx`：注册 /pm/sprints
- `AppRoutes.test.tsx`：加 Sprint 路由测试
- `AppLayout.test.tsx`：扩展为 5 项（项目 / 诉求 / 需求 / Sprint / 关联）

### E. 前端测试

新：
- `SprintEditDrawer.test.tsx`（默认 owner + 编辑可改 + 表单错误 = 3 用例）
- `SprintsPage.test.tsx`（drilldown 展开 Stories + 列表 storyCount = 2 用例）

改：
- `StoryEditDrawer.test.tsx`（locked field display 改 sprint）
- `RequirementsPage.test.tsx`（drilldown 渲染 SprintListPanel 而非 StoryListPanel）

### F. 配置层面

- 无新配置项；Flyway 仍禁用 → ddl-auto=update 自动建 rainier_sprint 表 + 自动 ADD COLUMN Story.sprint_id
- ⚠️ **风险点**：ddl-auto=update 通常不删列。`Story.requirement_id` 列将**被遗留为不再使用的死列**（数据已迁，新代码不写不读）。可接受（v0.0.11+ cleanup 时手动 DROP）

### G. 基础设施

- +1 新表：rainier_sprint（11 → 12 张）
- 1 列添加：rainier_story.sprint_id NN（但首次启动时 LegacyStoryToSprintMigration 在 NN 约束生效前先填好）
- 0 schema 改其他 existing 表
- 部署沿用 v0.0.8/v0.0.9 模式：docker compose build / up --no-deps --force-recreate；**不 down -v**

## Success Criteria

- [ ] v0.0.9-story 已 Phase 6 DELIVER 并打 tag `v0.0.9-story`
- [ ] `mvn test` 全绿，新增 ≥ 22 后端测试
- [ ] `npx vitest run` 全绿，新增 ≥ 5 前端测试
- [ ] `npx tsc --noEmit` + `npx vite build` 0 错误
- [ ] docker compose 重启后 `SHOW TABLES;` = 12，含 `rainier_sprint`
- [ ] `DESCRIBE rainier_sprint;` 含字段：id / code / name / description / goal / status / requirement_id / owner_user_id / start_date / end_date + 6 审计 + del_flag = 17 字段
- [ ] `DESCRIBE rainier_story;` 含 sprint_id BIGINT NN；requirement_id 列仍存在但不被新代码读写（死列）
- [ ] **首次 v0.0.10 启动日志**含 INFO 行：`legacy story migrated to default sprint: requirement_id=N → sprint_id=M` + 全局汇总 `LegacyStoryToSprintMigration: created N default sprints, migrated M stories`
- [ ] 启动后 `SELECT id, sprint_id FROM rainier_story WHERE sprint_id IS NULL` — 0 行
- [ ] curl 流：POST Sprint w/ requirementId → 201 + 富化
- [ ] curl 流：POST Sprint w/ requirementId=999 → 400 "requirement not found"
- [ ] curl 流：POST Story w/ sprintId → 201 + projectId 自动从 sprint.requirement.projectId 继承
- [ ] curl 流：POST Story w/ sprintId=999 → 400 "sprint not found"
- [ ] curl 流：DELETE Sprint with Story → 409 "sprint has linked stories"
- [ ] curl 流：DELETE Requirement with Sprint → 409 "requirement has linked sprints"
- [ ] curl 流：GET Requirement → body.sprintCount 字段
- [ ] 前端 /pm/sprints 路由可访问，SprintsPage CRUD 工作
- [ ] 前端 RequirementsPage 行展开渲染 SprintListPanel；点 Sprint 行 → SprintsPage drilldown 看其 Stories
- [ ] mysql 卷未触碰：v0.0.9 STR-E2E-001 + REQ-E2E-001 + PROJ-E2E-001 全部保留；Story 现已有 sprint_id 指向自动创建的"默认 Sprint"
- [ ] `grep -rn 'is_pmo\|isPmo' backend/src/main/java backend/src/main/resources/application*.yml frontend/src` 命中数 = 0

## H. 显式排除（明确不在本期）

- ❌ DROP Story.requirement_id 列（v0.0.11+ cleanup 单独做；ddl-auto=update 不自动 DROP）
- ❌ Sprint 时间一致性校验（start ≤ end，dates ≤ requirement.endDate 等）— 语义是层级
- ❌ Sprint 之间依赖关系（dependsOn）
- ❌ Sprint Burndown / Velocity 报表
- ❌ Sprint 自动状态转换（如全 Story DONE → Sprint COMPLETED）
- ❌ Sprint 内 Story 顺序（拖拽排序）
- ❌ 跨 Requirement 共享 Sprint（一个 Sprint 只挂一个 Requirement）
- ❌ Story 跨 Sprint 复用（一个 Story 只挂一个 Sprint）
- ❌ Task 实体（v0.0.11+ 单独）
- ❌ AI 拆解 Requirement → Sprint 建议

## I. 锁定的设计决策（Gate 1 通过）

| # | 决策项 | 选择 | 理由 |
|---|---|---|---|
| 1 | 时机 | 先合并 v0.0.9-story 再做 v0.0.10 | v0.0.9 代码已全绿，反悔成本高 |
| 2 | Sprint 语义 | 需求拆解中间层（非时间箱） | dates 字段保留但 service 不做时间校验 |
| 3 | Story.sprintId | NN（必挂 Sprint） | 严格三层 |
| 4 | Sprint 字段集 | 全要 code/name/desc/requirementId/owner/status/dates/goal | dates/goal 作为参考元数据 |
| 5 | Story.requirementId | **删除**列（代码层面）；通过 sprint.requirementId 间接访问 | 模型纯净，避免冗余漂移；DB 列遗留死列由 v0.0.11+ cleanup |
| 6 | Sprint 状态机 | 4 项：PLANNING / ACTIVE / COMPLETED / CANCELLED | 与 Project 5 项同量级 |
| 7 | 前端形态 | /pm/requirements drilldown 显 Sprints + 独立 /pm/sprints 菜单 + drilldown 显 Stories | 两层叙事链清晰 |
| 8 | 数据迁移 | 启动自愈 CommandLineRunner 为每个有 Story 的 Requirement 创默认 Sprint，全迁旧 Story | 不丢数据；沿用 v0.0.8 DanglingProjectIdCleanup 模式 |
