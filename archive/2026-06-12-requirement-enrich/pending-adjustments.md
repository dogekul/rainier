# Pending Adjustments — v0.0.19-requirement-enrich

## PA-1 (Build) — 仅 RequirementControllerQueryTest 命中旧状态值
枚举改 6 态后,全 src 仅 `RequirementControllerQueryTest` 用旧值 "APPROVED"(update 测试,line 143/152)→ 改为新值 "IN_ANALYSIS"。demand 测试的 IN_REVIEW 是 DemandStatus(独立枚举)无关,不动。无 `RequirementStatus.<旧常量>` 引用(全字面串),删旧常量零编译影响。

## PA-2 (Build) — 既有 detail 字段集测试 presence-loop,加 expectedDate 不破
`RequirementControllerQueryTest` 字段集 `assertTrue(body.has(f))` 为存在性循环;加 expectedDate 到 DTO 不破,主动加 "expectedDate" 到 expected[]。

## PA-3 (Build) — 前端中文标签集中化
priority 中文标签集中到 `api/demand.ts:PRIORITY_LABELS`,requirement 状态中文集中到 `api/requirement.ts:REQUIREMENT_STATUS_LABELS`,4 个优先级下拉 + 需求状态下拉/表格列复用,无内联重复。requirement 表格列加 `?? r.status`/`?? r.priority` 兜底(旧值也能渲染不崩)。

## PA-4 (Verify Step0 Test-H1) — 跨实体 LOWEST 补端点测试
评审指出 TC-PRIO-001 原仅 Priority 单测,未在非 requirement 实体端点验证。补 `DemandControllerCreateTest.post_priorityLowest_returns201`(POST /api/demands priority=LOWEST → 201)证共用枚举端到端生效。

## PA-5 (Verify Step0 Test-M1) — 旧状态拒绝参数化
`post_legacyStatus_returns400` 原仅测 APPROVED → 改为循环 {IN_REVIEW, APPROVED, IN_DEV, DEPRECATED} 全测 400(覆盖 remap 的 4 个源值)。

## PA-6 (Verify Step0 Test-M2/Code-M1) — expectedDate PUT 契约固化
`RequirementService.update` expectedDate 为全量替换(同 projectId;closeReason 是 preserve-on-null,有意不同)。补测 update 省略 expectedDate → 清空(前端总送该字段,清空输入即清空)。

## PA-7 (Verify Step0 Test-M3) — 回填字段不变量多行校验
remap 测试原仅校验 1 行(IN_REVIEW)其它字段不变 → 加 DRAFT 行(未被 remap)的 code/title/priority 校验。

## PA-8 (Verify Step0 docs/lint) — 文档 + lint 清理
test-plan TC-PRIO-001 描述由「POST /api/tasks」改为实际实现(Priority 单测 + Demand 端点测试);.stdd.yaml "IIN_REVIEW" typo 修正;RequirementEditDrawer.test 去除已失效的 eslint-disable 指令;TC-FES-REQE-001 补「已批准」负断言。

## 未修复（评审 L,阈值内）
- 既有 detail 字段集 presence-loop 不校验 expectedDate 实值(值由 TC-REQE-005 round-trip 覆盖)。
- 前端 getByText('最低'/'草稿') 未 scope 到具体 select(当前唯一,不歧义);CLOSED→其它状态切换时 closeReason state 不清(忠实移植旧 DEPRECATED 行为,非回归)。
- backfill 测试直接调 run() 非真启动断言(逻辑已覆盖)。
