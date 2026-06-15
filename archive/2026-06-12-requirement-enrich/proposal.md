# v0.0.19-requirement-enrich — 需求状态机调整 + 五级优先级 + 期望交付日期

> Baseline: tag `v0.0.18-workbench` / commit 80a62d5. backend 341 + frontend 73 测试 green, 19 表.

## Why

用户要丰富 Requirement 实体:把状态集调成更贴流程的 6 态(草稿→审批中→分析中→实施中→已交付→已关闭)、
优先级从 4 级扩到 5 级、补一个「期望交付日期」。纯实体增强,不依赖 AI。

## What Changes

- **需求状态**:`RequirementStatus` 6 态由 `DRAFT/IN_REVIEW/APPROVED/IN_DEV/DELIVERED/DEPRECATED` 改为
  `DRAFT(草稿)/IN_APPROVAL(审批中)/IN_ANALYSIS(分析中)/IN_PROGRESS(实施中)/DELIVERED(已交付)/CLOSED(已关闭)`。
  不强制转换(仅校验 ∈ 合法集)。存量旧值由启动 `RequirementStatusBackfill` remap:
  IN_REVIEW→IN_APPROVAL / APPROVED→IN_ANALYSIS / IN_DEV→IN_PROGRESS / DEPRECATED→CLOSED(DRAFT/DELIVERED 不变)。
- **优先级**:共用 `Priority` 加第 5 级 `LOWEST(最低)` → URGENT/HIGH/MEDIUM/LOW/LOWEST。后端 4 实体
  (demand/requirement/story/task)全接受;前端 4 个优先级下拉同步加。无迁移(旧值仍合法)。
- **期望交付日期**:Requirement 加 `expectedDate`(LocalDate,可空)。

## Capabilities

### Modified Capabilities

- `entity-requirement`:status 枚举改 6 态 + 存量 remap + `expectedDate` 字段 + priority 接受 LOWEST。
- `frontend-scaffold`:RequirementsPage 状态下拉(新 6 态中文)+ 优先级下拉(5 级)+ expectedDate 输入;
  demand/story/task 页优先级下拉加「最低」。

### New Capabilities

- 无(0 新表 / 0 新包)。

## Impact

**代码层面**:
- 后端:`RequirementStatus`(新 6 常量) / `Priority`(+LOWEST) / `Requirement`(+expectedDate) / 3 个 Requirement DTO(+expectedDate) /
  `RequirementService`(expectedDate set;status 校验复用新 ALL) / 新 `RequirementStatusBackfill` bootstrap runner /
  `RequirementControllerQueryTest`(APPROVED→IN_ANALYSIS)。
- 前端:`api/requirement.ts`(RequirementStatus 类型新值 + expectedDate) / `api/demand.ts`(Priority +LOWEST) /
  `RequirementsPage.tsx`(状态/优先级/expectedDate) / demand/story/task 页 PRIORITY_OPTIONS +LOWEST。
- **配置/基础设施**:无新表 / 无新端点。

## 显式排除（往后）

- 强制状态机转换(草稿→审批中→… 的合法转换约束)—— 暂不校验,自由设置。
- 业务价值 / 验收标准 / 标签等其它候选字段(本版只做状态 + 优先级 + 期望日期)。
- AI(风险、健康分等)。

## 已锁定决策 (Gate 1 确认 2026-06-12,用户「就这样」)

- D1 状态英文常量 + 迁移映射:IN_APPROVAL/IN_ANALYSIS/IN_PROGRESS/CLOSED(按上表)。
- D2 第 5 级 = `LOWEST(最低)`,**共用 Priority**(4 实体都加,前端 4 下拉同步)。
- D3 状态转换:不强制(只改状态集)。
- D4 存量迁移:bootstrap remap runner(旧→新)。

## Success Criteria

- [ ] `POST /api/requirements` status 取新 6 态之一成功;旧值(如 "APPROVED")→ 400 invalid status。
- [ ] `RequirementStatus.ALL` = 新 6 态;remap runner 把存量 IN_REVIEW/APPROVED/IN_DEV/DEPRECATED 改为新值,DRAFT/DELIVERED 不变。
- [ ] `Priority.ALL` 含 LOWEST;requirement/demand/story/task 创建 priority=LOWEST 成功。
- [ ] Requirement create/update 接受并返回 `expectedDate`;Detail 含该字段。
- [ ] 前端 RequirementsPage 状态下拉为新 6 态(中文)、优先级 5 级、可填期望交付日期;demand/story/task 优先级下拉含「最低」。
- [ ] backend 341+ / frontend 73+ 全绿 + tsc clean;E2E:remap + 新状态 + LOWEST + expectedDate;19 表不变;存量(除状态 remap 外)不改。
