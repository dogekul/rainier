# v0.0.9-story 测试方案与详细案例

> 版本：v0.0.9-story
> 创建日期：2026-06-07
> 对应 Phase 2 Spec：specs/entity-story/spec.md + specs/entity-requirement/spec.md + specs/frontend-scaffold/spec.md
> 基线：v0.0.8.1-cleanup（commit f4268e0）

## 一、测试策略

### 1.1 测试金字塔

- **后端集成（~80%）**：`@SpringBootTest @AutoConfigureMockMvc` 覆盖 Story 5 endpoint + Requirement DELETE FK 改造 + storyCount 富化（沿用 v0.0.8 ROI 模式）
- **后端单元（~5%）**：状态机 ALL Set 内容、StoryRepository 派生查询命名
- **前端组件（~15%）**：vitest 覆盖 RequirementsPage drilldown + storyCount 列 + StoryEditDrawer 默认 owner / owner 可改

### 1.2 测试原则

- **富化必跟随**：Story owner 改 → 富化 ownerName 跟随（沿用 v0.0.8 TC-PRJ-009 / TC-REQP-005 模式）
- **state machine 边界显式**：每个非法值（status/priority/complexity）都有独立 TC，防止 ALL Set 漏项
- **projectId 自动继承**：TC-STR-001 显式断言 `body.projectId = 父 Requirement.projectId`（防 service 漏 set）
- **FK 保护对称**：v0.0.8 RequirementDelete 已加 demand_requirement 引用保护，本次再加 Story 引用保护 → 同一 endpoint 两类 409 路径必须分别测
- **AuditorAwareImpl 自动注入 createBy**：TC-STR-009 显式验证（防止未来配置漂移，沿用 TC-PRJ-006 模式）
- **默认 owner 解析依赖前端 listUsers + loginName 匹配**：TC-FES-S03 显式 mock 验证

### 1.3 已有测试资产（v0.0.8.1 baseline）

| 测试文件 | 用例数 | 类型 | 本变更影响 |
|---|---|---|---|
| 后端 v0.0.8.1 全部测试 | 148 | 集成/单元 | RequirementControllerDeleteTest / QueryTest 各加 ≥1 用例 |
| frontend v0.0.8.1 全部测试 | 32 | 组件 | RequirementsPage.test 新增（如不存在）；新增 StoryEditDrawer.test |
| **新增后端测试** | **≥ 18** | 集成 | 见第二节（实际 19：TC-STR ×16 + TC-REQS-001/001b/002 ×3；TC-REQS-001b 为 Phase 5 Test-H1 fix） |
| **新增前端测试** | **≥ 4** | 组件 | 见第二节（TC-FES-S01..S04）|
| 总计 | ≥ 202 | — | 148 + ≥18 = ≥ 166 backend；32 + ≥4 = ≥ 36 frontend |

## 二、详细测试案例

### 功能 1：entity-story — Story CRUD（16 TCs）

| TC-ID | 优先级 | Scenario | 关键断言 | 位置 |
|---|---|---|---|---|
| TC-STR-001 | P0 | 最小 payload + 默认值 + projectId 继承 + 富化 | 201 + body.status="DRAFT" + body.priority="MEDIUM" + body.projectId=父 Requirement.projectId + ownerName/requirementCode/projectName 全有 | StoryControllerCreateTest |
| TC-STR-002 | P0 | code 重复 → 409 | 409 + message contains "code already exists" | StoryControllerCreateTest |
| TC-STR-003 | P0 | requirementId 不存在 → 400 | 400 + message contains "requirement not found" | StoryControllerCreateTest |
| TC-STR-004 | P0 | ownerUserId 不存在 → 400 | 400 + message contains "owner user not found" | StoryControllerCreateTest |
| TC-STR-005 | P0 | 非法 status → 400 | 400 + message contains "invalid status" | StoryControllerCreateTest |
| TC-STR-006 | P0 | 非法 priority → 400 | 400 + message contains "invalid priority" | StoryControllerCreateTest |
| TC-STR-007 | P0 | 非法 complexity → 400 | 400 + message contains "invalid complexity" | StoryControllerCreateTest |
| TC-STR-008 | P0 | 缺必填字段 → 400 fieldErrors | 400 + fieldErrors 含 title / requirementId / ownerUserId 三项 | StoryControllerCreateTest |
| TC-STR-009 | P0 | createBy 自动注入 | body.createBy 非空（test profile auditor 注入） | StoryControllerCreateTest |
| TC-STR-010 | P0 | GET 详情完整字段集 + 富化 | 22 字段全有 + requirementCode + projectName + ownerName 准确 | StoryControllerQueryTest |
| TC-STR-011 | P0 | 按 requirementId 过滤 | body.total = 父 Req 下 Story 数 + content[*].requirementId 全为目标 | StoryControllerQueryTest |
| TC-STR-012 | P0 | 按 status 过滤 | body.total = 状态匹配 Story 数 + content[*].status 全为目标 | StoryControllerQueryTest |
| TC-STR-013 | P0 | 更新 status + priority + acceptanceCriteria | 200 + 三字段都按 PUT body 更新 | StoryControllerQueryTest |
| TC-STR-014 | P0 | PUT 改 ownerUserId 富化跟随 | 200 + ownerUserId=新值 + ownerName/ownerLoginName 跟随新 owner | StoryControllerQueryTest |
| TC-STR-015 | P0 | PUT 新 ownerUserId 不存在 → 400 | 400 + message contains "owner user not found" | StoryControllerQueryTest |
| TC-STR-016 | P0 | DELETE 软删 + GET 404 | 204 + 后续 GET 返 404 + DB del_flag=1 | StoryControllerDeleteTest |

### 功能 2：entity-requirement — DELETE FK + storyCount 富化（2 TCs）

| TC-ID | 优先级 | Scenario | 关键断言 | 位置 |
|---|---|---|---|---|
| TC-REQS-001 | P0 | Requirement 有 Story 引用 → 409 | 409 + message contains "requirement has linked stories" | RequirementControllerDeleteTest（追加） |
| TC-REQS-002 | P0 | GET Req 详情含 storyCount | 200 + body.storyCount=3（关联 3 Story 的情况） + list 返回项也含 storyCount | RequirementControllerQueryTest（追加） |

### 功能 3：frontend-scaffold — RequirementsPage drilldown + StoryEditDrawer（4 TCs）

| TC-ID | 优先级 | Scenario | 关键断言 | 位置 |
|---|---|---|---|---|
| TC-FES-S01 | P0 | RequirementsPage 表格含 Story 数 列 | 表格头含 "Story 数" + 行单元格显示 "3" | RequirementsPage.test |
| TC-FES-S02 | P0 | 点开行渲染 StoryListPanel | 子区域 data-testid="story-list-panel-1" 渲染 + 子表含 STR-10、STR-11 + "新建 Story" 按钮 testid="stories-new-btn" | RequirementsPage.test |
| TC-FES-S03 | P0 | StoryEditDrawer 默认 owner = 当前登录用户 | sel.value === '1'（alice id），同时 Requirement 字段锁定 | StoryEditDrawer.test |
| TC-FES-S04 | P0 | StoryEditDrawer 编辑模式 owner 可改 → updateStory | sel 不 disabled + 切换到 lili 后 updateStory 收到 body.ownerUserId=2 | StoryEditDrawer.test |

## 三、测试执行矩阵

| 功能模块 | 单元 | 集成 | 组件 | E2E | 状态 |
|---|---|---|---|---|---|
| entity-story | — | 16 TCs | — | E2E POST + DESCRIBE | 🟢 充分 |
| entity-requirement MODIFIED | — | 2 TCs（含 FK 与 storyCount） | — | E2E DELETE + storyCount | 🟢 充分 |
| frontend-scaffold MODIFIED | — | — | 4 TCs | 浏览器手测 | 🟢 充分 |

## 四、回归风险矩阵

| 风险区域 | v0.0.9 改动 | 已有回归保护 | 风险等级 |
|---|---|---|---|
| v0.0.8 entity-project | 0 改动 | TC-PRJ-001..013 全部既有 | 🟢 低 |
| v0.0.8 entity-requirement | DELETE 路径加 1 条 409 分支 + storyCount 富化 | TC-REQ + TC-REQP 既有保护原有 409 / 富化 / project 校验路径 | 🟡 中：FK 检查顺序不能搞错（先 demand_requirement 后 story，保证错误信息精确） |
| v0.0.8 entity-user-role | 0 改动 | TC-UROL + TC-URLP 全部既有 | 🟢 低 |
| DanglingProjectIdCleanup | 0 改动 | DanglingProjectIdCleanupTest（log 断言）既有 | 🟢 低 |
| frontend RequirementsPage | drilldown 展开按钮 + storyCount 列 + 子区域 | 既有 v0.0.8 Requirement CRUD vitest 用例需保持绿 | 🟡 中：展开按钮 toggling state 不能误触发其它行；新组件复杂度引入 |
| 现有 mysql 数据 | 0 触碰 | docker compose up --no-deps --force-recreate 保留卷 | 🟢 低 |

## 五、建议补充顺序

1. **第一优先（部署前必补，P0 = 22 项全部）**：
   - 后端：TC-STR-001..016 + TC-REQS-001..002 = 18 项
   - 前端：TC-FES-S01..S04 = 4 项
2. **第二优先（P1）**：无（本期单一目标，无 P1）
3. **第三优先（P2）**：无

## 六、TC 编号对照表

总计：23 P0 TCs；覆盖 8 Requirements / 22 Scenarios

> Phase 5 Test-H1 fix 加 TC-REQS-001b（combined demand_requirement + Story FK 顺序锚定），故 TC 总数 23。

| Capability | Requirements 数 | Scenarios 数 | TCs 数 | 1:1 映射 |
|---|---|---|---|---|
| entity-story (NEW) | 4 | 16 | TC-STR-001..016 (=16) | 严格 1:1 |
| entity-requirement (MODIFIED) | 2 | 2 | TC-REQS-001/001b/002 (=3) | TC-REQS-001 + 001b 共同覆盖 "Story FK + 顺序"；TC-REQS-002 对应 storyCount Scenario |
| frontend-scaffold (MODIFIED) | 2 | 4 | TC-FES-S01..S04 (=4) | TC-FES-S01+S02 对应 Requirement 1（一一对应 2 Scenarios），TC-FES-S03+S04 对应 Requirement 2（一一对应 2 Scenarios） |
