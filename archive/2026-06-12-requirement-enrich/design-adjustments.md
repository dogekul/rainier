# 设计调整说明 — v0.0.19-requirement-enrich

> 基线:Phase 2 design.md + specs + test-plan。来源:Phase 3-5 实现与评审。

## 调整汇总

| # | 调整 | 涉及 | 严重 | 阶段 | 用户已知 |
|---|------|------|------|------|---------|
| 1 | 前端中文标签集中化 + 表格列也中文化 | frontend-scaffold | Minor | Phase 4 | 是 |
| 2 | expectedDate PUT 全量替换契约固化 | design Decision 4 | Minor | Phase 5 (Code-M1/Test-M2) | 是 |
| 3 | 测试补强(跨实体 LOWEST 端点 / 旧状态参数化 / 回填多行) | test-plan | Minor | Phase 5 | 是 |

## 详细

### 调整 1: 中文标签集中化 + 列中文化
- **原始**: design.md 前端只说状态/优先级下拉用中文。
- **调整**: 把 priority 中文标签集中到 `api/demand.ts:PRIORITY_LABELS`、requirement 状态中文到 `api/requirement.ts:REQUIREMENT_STATUS_LABELS`,4 个优先级下拉 + 需求状态下拉/**表格列**复用(列也中文化,贴「好看」)。
- **原因**: 避免 4 处内联重复 + 表格显示英文枚举不友好。
- **影响**: api/demand、api/requirement、RequirementEditDrawer、RequirementsPage、StoryEditDrawer、TaskEditDrawer、DemandsPage。行为不变(值仍英文)。

### 调整 2: expectedDate PUT 契约 = 全量替换
- **原始**: spec 只说 update 改 expectedDate。
- **调整**: `RequirementService.update` 对 expectedDate 全量替换(省略即清空),与 projectId 一致(closeReason 是 preserve-on-null,有意不同)。补测固化(省略→清空)。前端总送该字段,清空输入即清空,无意外抹除路径。
- **原因**: 评审 Code-M1/Test-M2 指出契约未固定。
- **影响**: RequirementService.update + RequirementEnrichTest。

### 调整 3: 测试补强
- 跨实体 LOWEST 由 Priority 单测 + 补 DemandControllerCreateTest 端点测试(Test-H1)。
- 旧状态拒绝参数化覆盖 4 个 remap 源值(Test-M1)。
- 回填字段不变量加 DRAFT 行校验(Test-M3)。
- **影响**: DemandControllerCreateTest、RequirementEnrichTest、RequirementStatusBackfillTest。
