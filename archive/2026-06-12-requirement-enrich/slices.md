# v0.0.19-requirement-enrich 切片计划

| # | TC | 目标 | 依赖 |
|---|----|------|------|
| R01 | TC-REQE-001/002 | `RequirementStatus` 新 6 常量 + ALL | 无 |
| R02 | TC-REQE-004/TC-PRIO-001 | `Priority` +LOWEST + ALL | 无(并行) |
| R03 | TC-REQE-005 | `Requirement` +expectedDate + 3 DTO 透传 + Detail.from + RequirementService set | 无(并行) |
| R04 | TC-REQE-003 | `RequirementStatusBackfill` runner(native UPDATE ×4 remap) | R01 |
| R05 | TC-REQE-001..005,TC-PRIO-001 | backend 测试 + 既有 RequirementControllerQueryTest APPROVED→IN_ANALYSIS | R01-R04 |
| R06 | (前端基础) | `api/requirement.ts`(状态新值+expectedDate) + `api/demand.ts`(Priority +LOWEST) | 无(并行) |
| R07 | TC-FES-REQE-001..003 / TC-FES-PRIO-001 | RequirementsPage(状态新6/优先级5/expectedDate) + demand/story/task 优先级 +最低 + 测试 | R06 |
| R08 | TC-E2E-REQE-001 | docker 重建 + remap + 新状态/LOWEST/expectedDate + 19 表/存量不变 | R01-R07 |

## 批次
- 批次1: R01 ‖ R02 ‖ R03 ‖ R06
- 批次2: R04
- 批次3: R05 ‖ R07
- 批次4: R08

## 陷阱
- A: Java 8 — 枚举 ALL 用 Arrays.asList。
- remap native UPDATE 用字面旧值字符串('IN_REVIEW' 等),非已删除的常量。逐条 WHERE status='旧'。
- remap 测试: H2 无存量 → seedRequirement 后 setStatus('IN_REVIEW') 直存(实体 String 字段, 直存绕校验)再调 runner;断言其它字段不变。
- 既有 RequirementControllerQueryTest line143/152 "APPROVED" → "IN_ANALYSIS"(唯一命中;demand 的 IN_REVIEW 是 DemandStatus 无关,不动)。
- 既有 detail 字段集测试 presence-loop → 加 expectedDate 不破;主动加 "expectedDate" 到 expected[]。
- Priority 共用: 加 LOWEST 后 demand/story/task accept 端自动接受;前端 4 下拉(api/demand Priority 联合类型 + 各页 PRIORITY_OPTIONS)同步。
- expectedDate LocalDate↔string YYYY-MM-DD。
- 契约 K: 后端 expectedDate 字段名 ↔ 前端;status/priority 新值前后端一致。
