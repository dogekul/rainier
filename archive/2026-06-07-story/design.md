# v0.0.9-story 技术设计

> 对应 Phase 1：`proposal.md`
> 基线：v0.0.8.1-cleanup（commit f4268e0）
> Gate 1 锁定的 8 项设计决策详见 proposal §E + .stdd.yaml `design_decisions_pre_locked`

## Context

### 技术栈与约束（v0.0.8.1 不变）

- Spring Boot 2.7.18 + Java 8 + Hibernate JPA（Flyway DISABLED, `ddl-auto=update`）+ MySQL 8
- React 18 + Vite + TypeScript + Zustand + Axios + vitest + React Testing Library
- 单 `BaseEntity` 提供 `Long id` (auto-inc) + 6 审计字段 + `del_flag` boolean
- `AuditorAwareImpl` 从 JWT username 自动注入 `createBy`/`updateBy`
- `@SQLDelete` + `@Where("del_flag = 0")` 软删模式（Project / Requirement / Demand / Organization）
- 服务级 code 唯一（无 DB UNIQUE — 软删后可复用，家族一致）
- VARCHAR(16) 状态字段 + Java 服务级 `Set<String> ALL` 校验（Java 8 兼容：`Collections.unmodifiableSet(new HashSet<>(Arrays.asList(...)))`）
- 富化 enrich 模式：service 读时 join 上其它表加显示字段

### v0.0.8.1 完整 schema 上下文

10 张表：`rainier_organization` / `rainier_user` / `rainier_user_organization` / `rainier_position` / `rainier_role` / `rainier_user_role` / `rainier_demand` / `rainier_requirement` / `rainier_demand_requirement` / `rainier_project`。

关键 FK 关系：
- `rainier_requirement.project_id` → `rainier_project.id`（v0.0.8 激活强校验）
- `rainier_requirement.owner_user_id` → `rainier_user.id`（v0.0.8 mutable）
- `rainier_user_role.project_id` → `rainier_project.id`（v0.0.8 激活）

### v0.0.8 启动自愈机制

`DanglingProjectIdCleanup @Component @Order(HIGHEST_PRECEDENCE)` 在启动时 NULL 掉指向已软删 Project 的 dangling project_id。本次 v0.0.9 的 `rainier_story.project_id` 暂不纳入这一 cleanup —— Story 的 projectId 是从 Requirement 创建时继承的，Requirement 的 projectId 已被 cleanup 守卫，所以 Story.projectId 只能在 Requirement.projectId 已经合法时被写入。**待后续 change 再评估**是否需要把 Story 加入 cleanup 表清单（如果出现 Requirement 软删 + Story 残留场景）。

---

## Decisions

### 1. Story 必须挂 Requirement（NN FK）

**方案**：`Story.requirementId BIGINT NOT NULL`，FK to `rainier_requirement.id`；Service 在 create 时强校验 `requirementRepo.existsById(req.getRequirementId())`，不存在 → 400。

**为什么**：
- 严格"Story 是 Requirement 拆解产物"模型，与产品蓝图 A 卡片 2 PO 高频动作 + 飞轮 § 4.1 OKR↔项目↔Story 关联一致
- Story 的 projectId 才可以"从 Requirement 自动继承"——Requirement 必存在才有得继承
- 列表/查询路径单纯：所有 Story 都能通过 `requirementId` 索引快速反查所属需求

**备选方案及排除原因**：
- 允许游离 Story（仅挂 Project）：模型多分支，PM 沿袭"诉求→需求→故事"主线叙事会断裂
- 必须同时挂 Requirement + Project：过于笨拙；Requirement.project_id 已经可为空，强制 Story.project_id 非空会破坏可空性

### 2. 独立 acceptanceCriteria VARCHAR(4000) 字段

**方案**：`acceptance_criteria VARCHAR(4000) NULL`，与 `description VARCHAR(4000)` 并列。

**为什么**：PM/测试验收时直接读独立字段，不用扫描自由文本描述。为后续飞轮 ② 风险雷达 / AI 验收推断保留结构化锚点。

**备选方案及排除原因**：
- 合并到 description：靠格式约定（如 "## 验收"），PO 手动维护、测试手动扫，飞轮 ② 抽取信号麻烦
- 单独 acceptanceTests JSON：本期复杂度过高，AI 验收推断未启动；待飞轮 ② 启动时再单独 change

### 3. 6 项状态机（含 BLOCKED）

**方案**：`StoryStatus` 常量集 = `{DRAFT, READY, IN_PROGRESS, DONE, BLOCKED, CANCELLED}`。VARCHAR(16) 列 + Service 级 `ALL.contains(status)` 校验。

**为什么**：BLOCKED 是飞轮 ② 风险雷达要抽的一类核心信号（蓝图 § 2.6 防反向机制 + § 3 决策反馈飞轮）。`closeReason` 保留为软文本说明字段。

**备选方案及排除原因**：
- 5 项不含 BLOCKED：用 closeReason 表达阻塞，但语义弱、状态查询无法精确过滤
- 跟 Requirement 同步 6 项（DRAFT/IN_REVIEW/APPROVED/IN_DEV/DELIVERED/DEPRECATED）：业务概念不对应（Story 走 IN_REVIEW 不合理）

### 4. projectId 从 Requirement Service 创建时继承（非读时 join）

**方案**：`Story.projectId BIGINT NN`（非空，因为 Requirement.projectId 非空场景下 Story 必然挂某 Project... 但 Requirement.projectId 实际**是可空的**，所以需要修正）。

**修正方案**：`Story.projectId BIGINT NULL`，Service create 时 `story.setProjectId(parentRequirement.getProjectId())`（可能是 null，表示父 Requirement 未挂项目）。DTO 不暴露 projectId 字段（不接受客户端设值）。

**为什么**：
- 与父 Requirement 创建时保持一致，简化主线叙事
- 写到独立列而非读时 join：query 性能 / 后续按 project 维度统计 Story（PMO 看板 / 风险雷达）方便

**备选方案及排除原因**：
- DTO 暴露 projectId：客户端可设和父 Requirement 不一致的项目，破坏一致性
- 不存列，读时 join Requirement.projectId：查询多一次 join，列表 N+1 风险
- 后续 Requirement.projectId 变更 cascade 到 Story：本期不做（与 v0.0.8 dirty data strategy 一致——一致性由后续 cleanup 守卫，非主动同步）

### 5. owner 可改（沿用 v0.0.8 Decision 6b）

**方案**：`StoryUpdateRequest` 含 `@NotNull Long ownerUserId`；Service update 时若 `!new.equals(old)`，校验 `userRepo.existsById(new)` → 400 若不存在，否则 `story.setOwnerUserId(new)`。

**为什么**：与 Project / Requirement 家族保持 owner 可转移一致；PO 离职 / 重组 场景必需。

### 6. 软删 + del_flag 沿用家族模式

**方案**：`@SQLDelete(sql = "UPDATE rainier_story SET del_flag = 1 WHERE id = ?") @Where(clause = "del_flag = 0")`。

**为什么**：与 Project / Requirement / Demand / Organization 一致。

### 7. Service 级 code 唯一（无 DB UNIQUE）

**方案**：`StoryRepository.existsByCode(code)`；Service create 时 `if (existsByCode) throw ConflictException("code already exists: " + code)`。

**为什么**：家族一致。软删后 code 可重用——已在 v0.0.8 pending-adjustments CR-H3 记录为 design intent。

### 8. 状态机 update 时无转换守卫

**方案**：本期 update 时仅校验 `ALL.contains(req.getStatus())`，不做 from→to 转换合法性守卫（如禁止 CANCELLED → IN_PROGRESS）。

**为什么**：状态机守卫属于工作流引擎，本期不引入。下个 change 加。

### 9. FK 保护：Requirement DELETE 加 Story 引用检查

**方案**：`RequirementService.delete` 在已有 `linkRepo.count(...)` 检查之后，追加 `if (storyRepo.countByRequirementId(id) > 0) throw ConflictException("requirement has linked stories")`。

**为什么**：与 v0.0.8 Project FK 保护模式一致（Project DELETE 检查 Requirement + UserRole 引用）。

### 10. storyCount 富化于 Requirement read path

**方案**：`RequirementService.enrich(Requirement r)` 在已有 owner + project 富化基础上追加 `dto.setStoryCount(storyRepo.countByRequirementId(r.getId()))`。

**为什么**：前端 RequirementsPage 行级展开按钮需要显示"展开看 N 个 Story"，避免单独多一次 list 请求。

**Trade-off**：list 路径每行 N+1 一次 count。Story 表预期规模小（v0 千级），可接受；优化在后续 change（如 join + group by 一次）。

### 11. 前端 drilldown 形态（无独立 Sider 菜单）

**方案**：RequirementsPage 每行加展开按钮（仿 antd Table expandable），点开显示子区域：Stories 列表 + "新建 Story" 按钮。Story 编辑/新增走 `StoryEditDrawer` 抽屉（仿 RequirementEditDrawer 模式）。无独立 `/pm/stories` 路由。

**为什么**：主线叙事 Requirement → Story 父子关系明确；菜单不膨胀。"我的所有 Story 跨 Requirement 工作台"留到 v0.1.x。

### 12. RequirementEditDrawer 不动

**方案**：仅 RequirementsPage 表格改造 + 新增 StoryListPanel + StoryEditDrawer 两个子组件。RequirementEditDrawer 不改。

**为什么**：Story 关联只走单独抽屉，不嵌入 Requirement 编辑流。降低 RequirementEditDrawer 复杂度。

---

## Architecture

### 数据流

```
                       ┌────────────────────┐
                       │ rainier_requirement│
                       └─────────┬──────────┘
              project_id (NN)    │      project_id (FK，nullable)
                       │         │
                       ▼         ▼
                ┌──────────┐   ┌─────────┐
                │ project  │◄──│  story  │
                └──────────┘   └─────────┘
                       ▲         │
              owner_user_id (NN) │ requirement_id (NN, FK)
                       │         │ owner_user_id (NN, FK)
                       │         │
                       └───┬─────┘
                           │
                       ┌───────┐
                       │  user │
                       └───────┘
```

### Class layout（后端）

```
com.rainier.story
├── domain
│   ├── Story.java                   # entity, @SQLDelete + @Where
│   └── StoryStatus.java             # constants + ALL Set
├── repository
│   └── StoryRepository.java         # existsByCode / countByRequirementId / countByOwnerUserId / JpaSpecificationExecutor
├── dto
│   ├── StoryCreateRequest.java
│   ├── StoryUpdateRequest.java
│   └── StoryDetail.java             # business fields + ownerName/ownerLoginName + requirementCode/requirementTitle + projectName/projectCode
├── service
│   └── StoryService.java            # inject StoryRepo + RequirementRepo + UserRepo + ProjectRepo
└── controller
    └── StoryController.java         # 5 REST endpoints

复用：
- com.rainier.common.domain.Priority    (URGENT/HIGH/MEDIUM/LOW)
- com.rainier.requirement.domain.Complexity  (XS/S/M/L/XL)
```

### REST API

| HTTP | Path | 说明 |
|---|---|---|
| POST | `/api/stories` | 创建 Story（projectId 自动从 Requirement 继承） |
| GET | `/api/stories/{id}` | 单 Story 详情（富化 owner + requirement + project） |
| GET | `/api/stories?requirementId=&status=&priority=&search=&page=&size=` | 分页列表 + 过滤 |
| PUT | `/api/stories/{id}` | 更新（owner 可改；requirementId/projectId 不可改） |
| DELETE | `/api/stories/{id}` | 软删 |

### Class layout（前端）

```
frontend/src/
├── api/
│   └── story.ts                       # Story TS type + 5 CRUD 函数
└── pages/Requirement/
    ├── RequirementsPage.tsx           # 改造：展开按钮 + storyCount 列
    ├── StoryListPanel.tsx             # 新：子组件 — 表格 + 新建按钮 + 编辑/删除按钮
    └── StoryEditDrawer.tsx            # 新：抽屉 — 标题/描述/AC/状态/优先级/复杂度/负责人
```

---

## Risks / Trade-offs

| 风险 | 缓解措施 |
|---|---|
| Story.projectId 与父 Requirement.projectId 后续不一致（如 Requirement 改了 projectId） | 本期不级联同步，与 v0.0.8 family pattern 一致；待后续 change 评估是否加入 DanglingProjectIdCleanup 表清单 |
| storyCount 富化每行 N+1 count 查询 | Story 表 v0 规模小（千级），可接受；后续优化为 join + group by |
| 状态机无转换守卫，可能出现 CANCELLED → IN_PROGRESS 等非法转换 | 工作流引擎留待下个 change；本期纯前端引导（编辑抽屉禁用部分状态选项可选） |
| 6 项状态机过细，PO 不愿意维护 | 状态默认 DRAFT；PO 不主动改一直停在 DRAFT 是可接受的（蓝图 § 5.3 入口能量原则——少用胜过逼用） |
| Requirement 软删后残留 Story 引用（admin 直接 SQL 删 Requirement） | 与 Project soft-delete + dangling 情形对称；FK 保护已在 RequirementService.delete；非正路径不守 |
| drilldown 展开多个 Requirement 同时打开导致前端列表性能下降 | 子面板每次展开按需 `listStories(requirementId=X)`；多个展开仍是 N 次独立请求；v0 可接受 |
| owner 可改 + soft-deleted user → 富化时 ownerName=null | 与 v0.0.8 RequirementService.enrich 模式一致：`userRepo.findById(...).ifPresent(...)` 防御性 null |
