# v0.0.19-requirement-enrich — 技术设计

## Context

- `RequirementStatus`(常量类)现 6 态 DRAFT/IN_REVIEW/APPROVED/IN_DEV/DELIVERED/DEPRECATED;`RequirementService` 校验 status ∈ ALL,无转换约束。
- `Priority`(`common.domain`,共用)4 级 URGENT/HIGH/MEDIUM/LOW;被 demand/requirement/story/task 4 service 校验使用。
- `Requirement` 字段 code/title/description/ownerUserId/status/priority/complexity/projectId/closeReason。
- docker 现 0 条活动需求(remap 实际 no-op,但防御保留);既有 bootstrap runner 模式(ProjectTypeBackfill)。
- 仅 `RequirementControllerQueryTest` 用旧值 "APPROVED"(line 143/152)需改;demand 测试的 IN_REVIEW 是 DemandStatus 无关。
- 约束:status remap 是有意的存量变更(保留语义);不强制转换(暂不校验);priority 加值无迁移。

## Decisions

### 1. 状态集替换为新 6 态 + 中文映射

**方案**: `RequirementStatus` 常量改为 `DRAFT/IN_APPROVAL/IN_ANALYSIS/IN_PROGRESS/DELIVERED/CLOSED`,`ALL` 同步。校验逻辑不变(create 默认 DRAFT、ALL.contains)。

**为什么**: D1;贴「草稿→审批中→分析中→实施中→已交付→已关闭」流程。

### 2. 存量状态 remap(bootstrap runner)

**方案**: 新 `RequirementStatusBackfill`(CommandLineRunner+@Order HIGHEST+@Transactional)启动时 native UPDATE:
`IN_REVIEW→IN_APPROVAL`、`APPROVED→IN_ANALYSIS`、`IN_DEV→IN_PROGRESS`、`DEPRECATED→CLOSED`(DRAFT/DELIVERED 不动)。逐条 UPDATE ... WHERE status='旧值'。

**为什么**: D4;改枚举会让存量旧值失配,remap 保留语义把它们迁到新值。照搬 ProjectTypeBackfill。

**备选及排除**: 保留旧值为合法(超集)—— 用户要「调整」非「追加」,排除。

### 3. Priority 共用加 LOWEST

**方案**: `Priority` 加 `LOWEST` 常量 + `ALL`。4 service 校验自动接受。无迁移(旧值仍合法)。

**为什么**: D2;共用一致,4 实体都多「最低」。

### 4. Requirement 加 expectedDate

**方案**: `Requirement` 加 `@Column(name="expected_date") private LocalDate expectedDate`(可空,nullable);`RequirementCreateRequest`/`RequirementUpdateRequest`(透传)/`RequirementDetail`(from + getter)。`RequirementService` create/update set。

**为什么**: D3 期望交付日期;LocalDate↔string YYYY-MM-DD,同 project date 模式。nullable 列加在存量表安全(ddl-auto=update)。

### 5. 前端

**方案**:
- `api/requirement.ts`: `RequirementStatus` 联合类型改新 6 值;`Requirement`/`Create`/`Update` 加 `expectedDate`。
- `api/demand.ts`: `Priority` 联合类型加 `'LOWEST'`(被 requirement/story/task 复用)。
- RequirementsPage: 状态下拉用新 6 态 + 中文标签(草稿/审批中/分析中/实施中/已交付/已关闭);优先级下拉 5 级(+最低);加 expectedDate 输入。
- demand/story/task 页: 优先级 PRIORITY_OPTIONS 加 LOWEST(+「最低」标签)。

## Architecture

```
启动 ─► RequirementStatusBackfill ─► UPDATE rainier_requirement SET status=<new> WHERE status=<old> (×4)
POST/PUT /api/requirements ─► RequirementService: status∈新ALL? priority∈ALL(含LOWEST)? + set expectedDate
前端 RequirementsPage: 状态(新6中文) / 优先级(5级) / expectedDate；demand·story·task 优先级 +最低
```

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 改 status 枚举使存量旧值失配 | remap runner 迁移(保留语义);docker 现 0 条 |
| 既有 RequirementControllerQueryTest 用 APPROVED | 改为新值 IN_ANALYSIS |
| Priority 共用,改动波及 4 实体 | 加值非删值,旧值仍合法,4 实体仅多一选项;前端 4 下拉同步加 |
| H2 test 无存量 → remap no-op | remap 测试手动 seed 旧状态行(native/setStatus)再调 runner |
| expectedDate nullable 列加在存量表 | ddl-auto=update 加 nullable 安全 |
| 其它 requirement 测试断言旧状态 | grep 校对(仅 QueryTest 命中) |
