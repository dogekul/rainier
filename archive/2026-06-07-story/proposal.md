# v0.0.9-story: Story 实体补全 Requirement 下层切片

> **版本**：v0.0.9-story
> **基线**：v0.0.8.1-cleanup（commit f4268e0）
> **类型**：domain-entity
> **触发文档**：A-角色意图卡片.md（卡片 2 PO 高频动作）+ archive/B-驱动飞轮.md（§ 4.1 跨层对齐 OKR↔项目↔Story 关联）

## Why

当前 Requirement 是空载体 — 没有下层切片。PO 拆完需求后没地方放产物；PM 看不到细颗粒进度；测试不知道按什么维度验收；后续飞轮 ① 抽取 commit→Story 状态、飞轮 ② 风险雷达定位"哪个 Story 卡了"，都无对象可挂。

**Story = Requirement 的管理视角切片**（垂直业务功能片），是产品蓝图（A 卡片 2 PO 高频动作 / 飞轮 § 4.1）锁定的核心实体。本变更补这一层，**不动 Task**（Task 在蓝图中是独立执行单元 — 用户自建 / 系统调度 / AI 生成，下个 change 再做）。

## What Changes

- 新表 `rainier_story`：requirementId NN + projectId NN（从 Requirement 创建时继承）+ ownerUserId NN + 状态/优先级/复杂度/验收标准/关闭原因 + 6 项审计 + del_flag
- 后端 5 endpoint CRUD（POST / GET 单 / GET 列表 / PUT / DELETE）+ 状态机 6 项常量集
- Service 创建时校验 requirementId 存在 → 自动从 Requirement 复制 projectId 填入；ownerUserId 必填、validates 用户存在
- Service 更新时校验新 ownerUserId 存在（可改，沿用 v0.0.8 Decision 6b 家族模式）；requirementId / projectId 不可改（结构不变）
- Service 软删（@SQLDelete + del_flag）；删除 Requirement 有 Story 引用 → 409 "requirement has linked stories"
- 富化（enrich）：response 含 ownerName / ownerLoginName + requirementCode / requirementTitle + projectName / projectCode
- 前端 RequirementsPage drilldown：每行展开看下层 Stories 列表 + 新增 / 编辑 / 删除 Story 抽屉（不加独立 Sider 菜单）
- 列表排序：默认 createTime DESC（不加 sort_order 字段，本期不实现拖拽）
- Requirement DTO 富化字段 `storyCount`（聚合计数，便于前端 drilldown 控件标示）
- code 唯一性沿用家族 service 级（无 DB UNIQUE）

## Capabilities

### Modified Capabilities

- **entity-requirement**：DELETE 加 Story 引用 FK 保护（沿用 v0.0.8 加 UserRole + Project 保护时的相同模式）；GET 单 / 列表返回新增字段 `storyCount`
- **frontend-scaffold**：RequirementsPage 行增加展开按钮；Stories 子列表 + 抽屉组件；api/story.ts 新增

### New Capabilities

- **entity-story**：Story CRUD + 状态机 + Requirement 子层切片 + owner 可改 + 富化 + 软删 + FK 保护

## Impact

### A. 代码层面 — 后端（新 `com.rainier.story` 包，10 文件）

- `domain/Story.java`（@SQLDelete + @Where + 字段 + getter/setter）
- `domain/StoryStatus.java`（6 项常量集 + ALL Set，Java 8 兼容）
- `repository/StoryRepository.java`（含 `existsByCode` / `countByRequirementId` / `countByOwnerUserId`，extends JpaSpecificationExecutor）
- `dto/StoryCreateRequest.java`（@NotBlank code / title，@NotNull requirementId + ownerUserId，可选 description + acceptanceCriteria + priority + complexity + status）
- `dto/StoryUpdateRequest.java`（@NotBlank code / title，@NotNull status + priority + ownerUserId，可选其余；无 requirementId / projectId）
- `dto/StoryDetail.java`（业务字段 + 富化字段 ownerName / ownerLoginName / requirementCode / requirementTitle / projectName / projectCode）
- `service/StoryService.java`（注入 StoryRepository + RequirementRepository + UserRepository + ProjectRepository；create / findById / list / update / delete + enrich 私有方法）
- `controller/StoryController.java`（5 endpoint REST）
- 复用 `common.domain.Priority`（无新文件）
- 复用 `requirement.domain.Complexity`（无新文件）

### B. 代码层面 — 后端改造（3 文件）

- `requirement/service/RequirementService.java`：注入 `StoryRepository`；`delete` 加 `storyRepo.countByRequirementId(id) > 0 → 409`；`enrich` 加 `storyCount`
- `requirement/dto/RequirementDetail.java`：加 `Long storyCount` 字段 + setter
- `requirement/dto/RequirementListItem.java`（如存在）或 list path enrich：同步加 storyCount

### C. 代码层面 — 后端测试（新 + 改，新增 ≥ 18 用例）

- 新 `backend/src/test/java/com/rainier/story/controller/StoryControllerCreateTest.java`（TC-STR-001..006）
- 新 `StoryControllerQueryTest.java`（TC-STR-007..011）
- 新 `StoryControllerDeleteTest.java`（TC-STR-012..014）
- 改 `RequirementControllerDeleteTest.java`：加 1 用例验证 Story 引用 → 409
- 改 `RequirementControllerQueryTest.java`：加 1 用例验证 storyCount 富化

### D. 代码层面 — 前端（新文件 3 + 改造 2）

新：
- `src/api/story.ts`（Story TS type + 5 endpoint 函数）
- `src/pages/Requirement/StoryListPanel.tsx`（drilldown 子组件 — 表格 + 新建按钮 + 编辑/删除按钮）
- `src/pages/Requirement/StoryEditDrawer.tsx`（新增 / 编辑 Story 抽屉 — ownerUserId 下拉默认当前登录用户、状态 / 优先级 / 复杂度下拉、验收标准 textarea）

改造：
- `src/pages/Requirement/RequirementsPage.tsx`：行级展开按钮 + 子面板渲染 + storyCount 列
- `src/api/requirement.ts`：Requirement type 加 `storyCount?: number`

### E. 代码层面 — 前端测试（新 + 改，新增 ≥ 4 用例）

- 新 `src/pages/Requirement/StoryEditDrawer.test.tsx`：默认 owner 当前登录用户 / 必填校验红框 / 新建调用 createStory（≥ 3 用例）
- 改 `src/pages/Requirement/RequirementsPage.test.tsx`（如不存在则新建）：drilldown 展开 + storyCount 渲染（≥ 1 用例）
- 不动 `RequirementEditDrawer.test.tsx`（Drawer 不变）

### F. 配置层面

- 无新配置项
- Flyway 仍禁用 → Hibernate ddl-auto=update 自动建 `rainier_story` 表
- 无新 application.yml 字段
- 无新 maven 依赖
- 无新 npm 依赖

### G. 基础设施

- +1 新表：`rainier_story`（10 张 → 11 张）
- 0 schema 改 existing 表
- 部署沿用 v0.0.8 模式：`docker compose build backend / frontend`；`up -d --no-deps --force-recreate`
- **不 docker compose down -v**，保留 mysql 卷 + v0.0.8 测试数据（alice id=1、lili id=2、既有 Project / Requirement / UserRole）

## Success Criteria

- [ ] `mvn test` 全绿，新增 ≥ 18 后端测试
- [ ] `npx vitest run` 全绿，新增 ≥ 4 前端测试
- [ ] `npx tsc --noEmit` + `npx vite build` 0 错误
- [ ] docker compose 重启后 `SHOW TABLES;` = 11，含 `rainier_story`
- [ ] `DESCRIBE rainier_story;` 字段集等于：id / code / title / description / acceptance_criteria / status / priority / complexity / requirement_id / project_id / owner_user_id / close_reason / 6 审计 (create_by / create_time / update_by / update_time / del_flag — BaseEntity 提供) — 共 13~14 字段
- [ ] curl 流：POST Story 含 requirementId → 201 + projectId 自动从 Requirement 继承 + ownerName / requirementCode / projectName 富化
- [ ] curl 流：POST Story 含不存在 requirementId → 400 "requirement not found"
- [ ] curl 流：PUT Story ownerUserId 改 → 200 + ownerName / ownerLoginName 跟随
- [ ] curl 流：DELETE Requirement 有 Story 引用 → 409 "requirement has linked stories"
- [ ] curl 流：GET `/api/requirements/{id}` 返 storyCount 字段
- [ ] 前端 RequirementsPage 行展开 → 子面板渲染 Stories + 新增 Story 抽屉默认 ownerUserId = 当前登录用户
- [ ] `grep -rn 'is_pmo\|isPmo' backend/src/main/java backend/src/main/resources/application*.yml frontend/src` 命中数 = 0
- [ ] mysql 卷未触碰：v0.0.8 测试数据全部保留

## D. 显式排除（明确不在本期）

- ❌ Task 实体（独立 change — Task 来源多元：用户自建 / 系统调度 / AI 生成）
- ❌ Story 之间的依赖关系（dependsOn / blocks）
- ❌ Story 拖拽排序 / sort_order 字段
- ❌ Story 评论 / 讨论
- ❌ Story 状态自动转换（commit → IN_PROGRESS、PR merged → DONE）— 飞轮 ① 集成后做
- ❌ Story 验收清单 acceptanceTests JSON（AC 字段是单 VARCHAR(4000)）
- ❌ AI 拆解 Requirement → Story 建议
- ❌ 独立 Sider 「故事」菜单项
- ❌ "我负责的所有 Story" 跨 Requirement 工作台视图

## E. 锁定的设计决策（Gate 1 通过）

| # | 决策项 | 选择 | 理由 |
|---|---|---|---|
| 1 | Story 关联结构 | 必须挂 Requirement | 严格"Story 是 Requirement 拆解产物"，模型纯净 |
| 2 | 验收标准字段 | 独立 `acceptanceCriteria` VARCHAR(4000) | PM / 测试 验收时直接看，不用从描述里扫 |
| 3 | 状态机 | 6 项（DRAFT / READY / IN_PROGRESS / DONE / BLOCKED / CANCELLED）| BLOCKED 对应蓝图阻塞信号，后续飞轮②风险雷达要抽 |
| 4 | projectId 来源 | Service 创建时从 Requirement 自动继承 | DTO 不暴露 projectId 字段；保证 Story.projectId 与 Requirement.projectId 创建一致 |
| 5 | 软删 | @SQLDelete + del_flag | 与 Project / Requirement / Demand / Organization 家族一致 |
| 6 | owner 可改 | 沿用 v0.0.8 Decision 6b | admin / PM 可转移 PO 交接 |
| 7 | 前端形态 | 仅 RequirementsPage drilldown | 主线叙事 Demand→Requirement→Story 明确，不增加菜单复杂度 |
| 8 | 默认排序 | createTime DESC 不加 sort_order | 本期最轻；后续要拖拽再单独 change 加 INT 列 |
