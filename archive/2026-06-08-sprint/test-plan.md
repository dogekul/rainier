# v0.0.10-sprint 测试方案与详细案例

> 版本：v0.0.10-sprint
> 创建日期：2026-06-08
> 对应 Phase 2 Spec：specs/entity-sprint/spec.md + specs/entity-requirement/spec.md + specs/entity-story/spec.md + specs/frontend-scaffold/spec.md
> 基线：v0.0.9-story (commit 20d175e, tag v0.0.9-story)

## 一、测试策略

### 1.1 测试金字塔

- **后端集成（~80%）**：`@SpringBootTest @AutoConfigureMockMvc` 覆盖 Sprint 5 endpoint + LegacyMigration + Story 改造分支 + Requirement 改造分支
- **后端单元（~5%）**：SprintStatus.ALL 内容验证；LegacyMigration boundary
- **前端组件（~15%）**：vitest 覆盖 SprintsPage + SprintListPanel + SprintEditDrawer + RequirementsPage drilldown 改造 + StoryEditDrawer 锁定字段改造

### 1.2 测试原则

- **迁移幂等显式**：TC-SPR-MIG-001 验证首次迁移；TC-SPR-MIG-002 验证二次启动 no-op（沿用 v0.0.8 cleanup family）
- **二段富化必跟随**：Story 现通过 sprint.requirement 获取 requirementCode/Title（TC-STR-FK 验证）
- **FK 检查顺序锁定**：v0.0.10 Requirement DELETE 同时有 demand + sprint → 优先 demand 错误（TC-REQS-FK-COMBINED）
- **owner 富化跟随**：Sprint PUT 改 owner → ownerName/ownerLoginName 跟随（沿用 TC-PRJ-009 / TC-REQP-005 / TC-STR-014 模式）
- **时间字段不校验**：TC-SPR-008 显式验证 end 早于 start 仍 201（语义是层级）
- **默认 owner 解析依赖前端 listUsers + loginName 匹配**：TC-FES-SPR-03 / TC-FES-SPR-04
- **TS 字段切换硬约束**：grep 验证 requirementId 字段从 api/story.ts 中删除，storyCount 从 api/requirement.ts 中删除（TC-FES-API-1）

### 1.3 已有测试资产（v0.0.9-story baseline）

| 测试文件 | 用例数 | 类型 | 本变更影响 |
|---|---|---|---|
| 后端 v0.0.9 全部测试 | 167 | 集成/单元 | StoryControllerCreateTest / QueryTest / DeleteTest 全改 sprint-based（约 16 用例修改）；RequirementControllerDeleteTest TC-REQS-001/001b 改 sprint；RequirementControllerQueryTest TC-REQS-002 改 sprintCount |
| frontend v0.0.9 全部测试 | 37 | 组件 | RequirementsPage.test 改 SprintListPanel；StoryEditDrawer.test 改 sprint props |
| **新增后端测试** | **≥ 22** | 集成 | 见第二节（实际 24：TC-SPR-001..017 + TC-SPR-MIG-001/002 + TC-STR-SPR-001..003 + TC-REQS-SPR-001..002） |
| **新增前端测试** | **≥ 5** | 组件 | 见第二节（TC-FES-SPR-01..05） |

## 二、详细测试案例

### 功能 1：entity-sprint — Sprint CRUD + Migration（17 + 2 = 19 TCs）

| TC-ID | 优先级 | Scenario | 关键断言 | 位置 |
|---|---|---|---|---|
| TC-SPR-001 | P0 | 最小 payload + 默认值 + 富化 | 201 + status="PLANNING" + storyCount=0 + ownerName/requirementCode/projectName 全有 | SprintControllerCreateTest |
| TC-SPR-002 | P0 | code 重复 → 409 | 409 + "code already exists" | SprintControllerCreateTest |
| TC-SPR-003 | P0 | requirementId 不存在 → 400 | 400 + "requirement not found" | SprintControllerCreateTest |
| TC-SPR-004 | P0 | ownerUserId 不存在 → 400 | 400 + "owner user not found" | SprintControllerCreateTest |
| TC-SPR-005 | P0 | 非法 status → 400 | 400 + "invalid status" | SprintControllerCreateTest |
| TC-SPR-006 | P0 | 缺必填字段 → 400 fieldErrors | name/requirementId/ownerUserId 三项 | SprintControllerCreateTest |
| TC-SPR-007 | P0 | createBy 自动注入 | body.createBy 非空 | SprintControllerCreateTest |
| TC-SPR-008 | P0 | 时间字段不做一致性校验 | end < start 仍 201 | SprintControllerCreateTest |
| TC-SPR-009 | P0 | GET 详情完整字段集 | 22 字段全有 + storyCount 准确 | SprintControllerQueryTest |
| TC-SPR-010 | P0 | 按 requirementId 过滤 | total + content[*].requirementId 全为目标 | SprintControllerQueryTest |
| TC-SPR-011 | P0 | 按 status 过滤 | total + content[*].status | SprintControllerQueryTest |
| TC-SPR-012 | P0 | 更新 status + goal | 200 + 两字段都按 PUT body 更新 | SprintControllerQueryTest |
| TC-SPR-013 | P0 | PUT 改 ownerUserId 富化跟随 | ownerName/ownerLoginName 跟随 | SprintControllerQueryTest |
| TC-SPR-014 | P0 | PUT 新 ownerUserId 不存在 → 400 | "owner user not found" | SprintControllerQueryTest |
| TC-SPR-015 | P0 | 无引用软删 + GET 404 | 204 + DB del_flag=1 | SprintControllerDeleteTest |
| TC-SPR-016 | P0 | 有 Story 引用 → 409 | "sprint has linked stories" | SprintControllerDeleteTest |
| TC-SPR-017 | P0 | (备用 — 与 016 不同 status 的 Story) 多 Story 仍 409 | "sprint has linked stories" | SprintControllerDeleteTest |
| TC-SPR-MIG-001 | P0 | 首次启动迁移旧 Story + ALTER 升级 NN（**Phase 5 修正**：orphan 不仅是 `sprint_id IS NULL`，也包括 `= 0`（MySQL 自动填）和 `NOT IN (live sprints)`） | 创默认 Sprint + Story.sprint_id 更新 + INFO 日志 + DESCRIBE rainier_story sprint_id Null="NO" | LegacyStoryToSprintMigrationTest |
| TC-SPR-MIG-002 | P0 | 二次启动 no-op + 不重复 ALTER | 无新 Sprint 创建 + 无 summary 日志 + DB 列保持 NN 不变 | LegacyStoryToSprintMigrationTest |

### 功能 2：entity-requirement — DELETE FK + sprintCount（2 TCs）

| TC-ID | 优先级 | Scenario | 关键断言 | 位置 |
|---|---|---|---|---|
| TC-REQS-SPR-001 | P0 | Requirement 有 Sprint 引用 → 409 | "requirement has linked sprints" | RequirementControllerDeleteTest |
| TC-REQS-SPR-002 | P0 | demand + sprint 双引用 → demand 优先 | "requirement has linked demands" | RequirementControllerDeleteTest |
| TC-REQS-SPR-003 | P0 | GET Req 含 sprintCount，无 storyCount | body.sprintCount=3 + 无 storyCount 字段 | RequirementControllerQueryTest |

(注：TC-REQS-SPR-003 算作 entity-requirement TC，实际 3 个 TC)

### 功能 3：entity-story — sprintId 改造（3 TCs）

| TC-ID | 优先级 | Scenario | 关键断言 | 位置 |
|---|---|---|---|---|
| TC-STR-SPR-001 | P0 | POST Story w/ sprintId 富化二段 | sprintCode + requirementCode + projectCode 全有；projectId 二段继承 | StoryControllerCreateTest（修改原 TC-STR-001..009 全部 + 加 1） |
| TC-STR-SPR-002 | P0 | POST Story sprintId 不存在 → 400 | "sprint not found" | StoryControllerCreateTest |
| TC-STR-SPR-003 | P0 | GET Story 详情含 sprintCode/sprintName + 不含 requirementId | 字段集 24 项 + 无 requirementId 字段 | StoryControllerQueryTest |
| TC-STR-SPR-004 | P0 | 按 sprintId 过滤列表 | content[*].sprintId 全为目标 | StoryControllerQueryTest |

### 功能 4：frontend-scaffold — 5 个 Requirement / 9 Scenarios → 5 TCs

| TC-ID | 优先级 | Scenario | 关键断言 | 位置 |
|---|---|---|---|---|
| TC-FES-SPR-01 | P0 | Sider 含 Sprint 项 + 位置 | "Sprint" 在 "项目" 与 "诉求" 之间 | AppLayout.test |
| TC-FES-SPR-02 | P0 | /pm/sprints 路由 + grep guard | mount SprintsPage + grep ≥ 1 | AppRoutes.test |
| TC-FES-SPR-03 | P0 | SprintEditDrawer 默认 owner = 当前登录用户 | sel.value === '1' + Requirement 字段锁定 | SprintEditDrawer.test |
| TC-FES-SPR-04 | P0 | SprintEditDrawer 编辑 owner 可改 → updateSprint | sel 不 disabled + body.ownerUserId=2 | SprintEditDrawer.test |
| TC-FES-SPR-05 | P0 | RequirementsPage drilldown 渲染 SprintListPanel + 列改 Sprint 数 | data-testid="sprint-list-panel-1" + 含 SPR-A/SPR-B + 列标题 "Sprint 数" 单元格 "3" | RequirementsPage.test |
| TC-FES-SPR-06 | P0 | StoryEditDrawer 锁定字段改 Sprint + createStory 收到 sprintId | "Sprint" 显示 "Phase 1（SPR-A）— 创建时锁定" + body.sprintId=10 | StoryEditDrawer.test（改造 v0.0.9 既有 TC-FES-S03 / S04） |
| TC-FES-SPR-07 | P0 | SprintsPage 行展开渲染 StoryListPanel(sprintId) | data-testid="story-list-panel-10" + stories-new-btn | SprintsPage.test |
| TC-FES-API-1 | P0 | api/story.ts requirementId 字段删除 + api/requirement.ts storyCount 字段删除 + tsc 0 错误 | grep 命中 0 行 + tsc 通过 | 无独立测试文件 — 由 vitest + tsc 间接保证 |

(注：TC-FES-API-1 不独立 TC 文件，但 spec 要求 grep + tsc 通过)

### 总计

- 后端新增 P0：19 (SPR) + 3 (REQS) + 4 (STR) = **26 P0 TCs 后端**
- 前端新增 P0：7 (FES-SPR-01..07) = **7 P0 TCs 前端**
- 总和 33 P0 TCs / 33 spec Scenarios / 14 spec Requirements

## 三、测试执行矩阵

| 功能模块 | 单元 | 集成 | 组件 | E2E | 状态 |
|---|---|---|---|---|---|
| entity-sprint | — | 19 TCs | — | E2E POST + DESCRIBE + MIGRATION log | 🟢 充分 |
| entity-requirement MODIFIED | — | 3 TCs（含 FK + sprintCount） | — | E2E DELETE + GET sprintCount | 🟢 充分 |
| entity-story MODIFIED | — | 4 TCs（sprintId + 二段富化） | — | E2E POST + GET 字段集 | 🟢 充分 |
| frontend-scaffold MODIFIED | — | — | 6 TCs | 浏览器手测 | 🟢 充分 |

## 四、回归风险矩阵

| 风险区域 | v0.0.10 改动 | 已有回归保护 | 风险等级 |
|---|---|---|---|
| v0.0.8 entity-project | 0 改动 | TC-PRJ-001..013 全部既有 | 🟢 低 |
| v0.0.8 entity-user-role + DanglingProjectIdCleanup | 0 改动 | TC-UROL + TC-URLP + cleanup test 既有 | 🟢 低 |
| v0.0.9 entity-story 改造 | requirementId → sprintId 大范围切换 | 既有 TC-STR-001..016 全部 fixture 修改；新加 TC-STR-SPR-001..004 | 🔴 高：所有 16 既有 Story 测试都需要 fixture 加 createSprint + body 参数改写 |
| v0.0.9 entity-requirement DELETE FK | storyRepo.countByRequirementId → sprintRepo.countByRequirementId | 既有 TC-REQS-001/001b 改 sprint；TC-REQS-002 改 sprintCount | 🟡 中：错误信息字符串改 + 验证修复 |
| v0.0.9 frontend RequirementsPage drilldown | StoryListPanel → SprintListPanel；列改 sprintCount | RequirementsPage.test.tsx 要改 mock | 🟡 中：mock 切换 + assertion 改 |
| v0.0.9 frontend StoryEditDrawer locked field | Requirement → Sprint props | StoryEditDrawer.test.tsx 改 props 调用 | 🟡 中 |
| 新增 rainier_sprint 表 | 启动 ddl-auto=update 建表 | LegacyStoryToSprintMigrationTest TC-SPR-MIG-001 | 🟡 中：迁移失败可能阻止启动 |
| rainier_story 加 sprint_id 列 | ddl-auto=update ALTER ADD COLUMN | DanglingProjectIdCleanupTest 不变；新 MIG-001 | 🟡 中 |
| 现有 mysql 数据 | LegacyMigration 写入（新 Sprint + Story UPDATE） | 不 down -v；MIG-002 验证幂等 | 🟡 中：迁移逻辑漏 / 错可能损坏 v0.0.9 数据 |

## 五、建议补充顺序

1. **第一优先（部署前必补）**：所有 P0 = 32 项全补
   - 后端：TC-SPR-001..017 + TC-SPR-MIG-001/002 + TC-STR-SPR-001..004 + TC-REQS-SPR-001..003 = 26
   - 前端：TC-FES-SPR-01..06 = 6
2. **第二优先（P1）**：无
3. **第三优先（P2）**：无

## 六、TC 编号对照表

| Capability | Requirements | Scenarios | TCs | 1:1 |
|---|---|---|---|---|
| entity-sprint (NEW) | 5 | 17 | TC-SPR-001..017 + TC-SPR-MIG-001/002 (=19) | TC-SPR-017 含同 Scenario 边界，余 1:1；MIG-001/002 严格 1:1 |
| entity-requirement (MODIFIED) | 2 | 3 | TC-REQS-SPR-001..003 (=3) | 1:1 |
| entity-story (MODIFIED) | 1 | 4 | TC-STR-SPR-001..004 (=4) | 1:1 |
| frontend-scaffold (MODIFIED) | 6 | 9 | TC-FES-SPR-01..07 + TC-FES-API-1 (=8) | TC-FES-SPR-05 同时覆盖 Requirement 4 的两个 Scenario；TC-FES-API-1 grep+tsc guard 由 CI 间接验证 |

**总计**：14 Spec Requirements / 33 Scenarios / 33 TCs（一 Scenario 一 TC 简单映射，TC-SPR-017 是 boundary 案例与 016 共用 Scenario；TC-FES-API-1 由 grep+tsc 间接验证）
