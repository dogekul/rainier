# v0.0.17-milestone — NEW Milestone 实体（项目级时点里程碑）

> Baseline: tag `v0.0.16-project-type` / commit 75c53a7. 当前 backend 320 + frontend 62 测试 green, 18 张表.
> 来源: 角色卡 `A-角色意图卡片.md` §2.3（里程碑为正式项目补齐内容之一）, gap 项 B4.

## Why

角色卡 §2.3 把「里程碑」列为正式项目的补齐内容，但设计文档**未给详细实体形状**。当前项目维度只有
`Project`（立项容器）和 `Sprint`（时间盒迭代），缺少**时点检查点**这一规划锚点（如「需求评审完成」/
「Beta 发布」/「上线」）。本版补一个 per-project 的里程碑实体。

**三概念区分**（写进 spec，避免混淆）：
- **Milestone** = 项目内**时点**检查点（name + 目标日 + 是否达成）。本版建。
- **Sprint** = **时间盒区间**（start~end，装 story/task）。已存在，不动。
- **放行门 / 放行决策** = 质量门 + 缺陷/回归聚合的放行辅助（角色卡卡6）。AI/流程层，**显式排除**。

## What Changes

- 新 capability `entity-milestone`：`Milestone`（`com.rainier.milestone`，表 `rainier_milestone`）挂 Project。
- 字段：`projectId`(NN) / `code` / `name`(NN) / `description` / `targetDate`(NN) / `status`(PLANNED/REACHED/MISSED) /
  `actualDate`(可空) / `sortOrder`(Integer, 默认 0)。
- `MilestoneStatus` 三态常量类（照搬 `ProjectStatus` 模式，Java 8）。
- 标准 CRUD + `GET /api/milestones?projectId=&status=` 分页过滤（默认 sortOrder ASC, 再 createTime）+ 软删。
- 前端：ProjectsPage 每行加「里程碑」按钮 → 行下内联展开 `MilestonesPanel`（照搬 v0.0.14 SprintFeaturePanel 模式），
  面板内对该项目里程碑做 增/改/删/列。
- **entity-project 级联删除**：删除一个项目时，其里程碑**一并被软删**（里程碑无独立意义）。
- v0.0.16 审计切面自动记 CREATE/UPDATE/DELETE MILESTONE（白拿）。

## Capabilities

### New Capabilities

- `entity-milestone`：Milestone CRUD + 三态 status + 项目挂载 + 排序。

### Modified Capabilities

- `frontend-scaffold`：ProjectsPage 里程碑按钮 + 内联 MilestonesPanel（无新路由/Sider 组）。
- `entity-project`：`ProjectService.delete` 通过现有 FK 检查后，**级联软删**该项目的里程碑（D3）。

## Impact

**代码层面**：
- 后端新包 `com.rainier.milestone`：`Milestone` / `MilestoneStatus` / `MilestoneRepository` / `MilestoneDetail` +
  `MilestoneCreateRequest` / `MilestoneUpdateRequest` / `MilestoneService` / `MilestoneController`。**+1 表**。
- 后端 `entity-project`：`ProjectService.delete` 注入 `MilestoneRepository`，级联软删（`deleteAll` 触发 `@SQLDelete`）。
- 前端：`api/milestone.ts` / `MilestonesPanel.tsx`(+test) / `ProjectsPage.tsx`（里程碑按钮 + 展开 toggle）。

**配置层面**：无。
**基础设施**：无新依赖；`ddl-auto=update` 自动加表 `rainier_milestone`。

## 已锁定决策 (Gate 1 确认 2026-06-12)

- **5 个用户初锁**：① status 三态 PLANNED/REACHED/MISSED ② 要 actualDate（可空）③ 只挂 Project（projectId NN）
  ④ 前端 = 项目详情下的内联面板（SprintFeaturePanel 模式）⑤ 要 sortOrder（Integer，默认 0）、不要 ownerUserId。
- **D1 code 唯一性**：保留 `code`，`(projectId, code)` 复合唯一（per-project；soft-deleted 可复用，service 级校验）。
- **D2 targetDate**：必填（NN）—— 里程碑核心是日期。
- **D3 删除语义**：**允许删项目，里程碑级联软删**（不阻断、不留孤儿）。现有 Requirement→UserRole→Task 的 409 链不变；
  项目通过这些检查后，先级联软删里程碑再软删项目。
- **D4 status 流转**：自由改，**不做状态机**（与 v0.0.16「暂不校验」一致）；仅枚举合法性校验。
- **D5 list 排序**：默认 `sortOrder ASC, createTime DESC`；sortOrder 默认 0。

## 显式排除 (推后续)

- 放行门 / 放行决策辅助（AI/流程层，角色卡卡6）。
- 里程碑关联测试任务 / 用例 / 缺陷链接。
- A2「正式项目必须有里程碑」校验（v0.0.16 已定暂不校验）。
- 里程碑状态机强制流转。
- 里程碑挂 Sprint（仅挂 Project）。

## Success Criteria

- [ ] `POST /api/milestones`（projectId + code + name + targetDate）→ 201，默认 status=PLANNED、sortOrder=0。
- [ ] projectId 不存在 → 400；非法 status → 400；缺 targetDate → 400；缺 code → 400。
- [ ] `GET /api/milestones?projectId=X` 仅返回该项目里程碑，按 sortOrder ASC 排序。
- [ ] `PUT` 可改 status（如 PLANNED→REACHED）、targetDate、actualDate、sortOrder、name、description。
- [ ] `DELETE /api/milestones/{id}` 软删（del_flag=1）。
- [ ] 删除一个**有里程碑**且无 Requirement/UserRole/Task 引用的项目 → 204，且该项目里程碑全部 del_flag=1（级联）。
- [ ] 删除一个**被 Requirement 引用**的项目仍 409（里程碑不改变此行为）。
- [ ] 前端 ProjectsPage 行有「里程碑」按钮，点开内联展开 MilestonesPanel，可增/改/删/列。
- [ ] `SHOW TABLES`=**19**（+rainier_milestone）；既有数据未改；backend 320+ / frontend 62+ 全绿 + tsc clean。
