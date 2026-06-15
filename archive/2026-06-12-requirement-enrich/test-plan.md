# v0.0.19-requirement-enrich 测试方案

> 2026-06-12 ｜ Spec: entity-requirement (MOD) + frontend-scaffold (MOD)

## 一、策略
- 后端集成(MockMvc+H2):新状态 create/拒旧值、remap runner、priority LOWEST、expectedDate。
- 组件(vitest+RTL):RequirementsPage 状态/优先级/expectedDate、TasksPage 优先级含最低。
- E2E(docker):remap + 新状态 + LOWEST + expectedDate + 存量(除 remap)不变。
- standing:status remap 是有意存量变更(保留语义);priority/expectedDate 纯加。

## 二、详细案例

| ID | Spec Scenario | 优先级 | 预置/输入 → 预期 |
|----|---------------|--------|------------------|
| TC-REQE-001 | 新状态创建成功 | P0 | POST status=IN_ANALYSIS → 201, status=IN_ANALYSIS |
| TC-REQE-002 | 旧状态被拒 | P0 | POST status=APPROVED → 400 "invalid status" |
| TC-REQE-003 | 启动 remap 旧状态 | P0 | seed 6 行(IN_REVIEW/APPROVED/IN_DEV/DEPRECATED/DRAFT/DELIVERED) → run → IN_APPROVAL/IN_ANALYSIS/IN_PROGRESS/CLOSED/DRAFT/DELIVERED;其它字段不变 |
| TC-REQE-004 | requirement LOWEST | P0 | POST priority=LOWEST → 201, priority=LOWEST |
| TC-REQE-005 | expectedDate create+update | P0 | POST expectedDate=2026-09-01 → 201 含; PUT 改 2026-10-01 → 200 含 |
| TC-PRIO-001 | 共用 LOWEST 生效 | P0 | (a) PriorityTest 单测：Priority.ALL=5 含 LOWEST；(b) DemandControllerCreateTest：POST /api/demands priority=LOWEST → 201（证非 requirement 实体经端点接受 LOWEST） |
| TC-FES-REQE-001 | 状态下拉新6态中文 | P0 | RequirementsPage 抽屉 → 含 草稿/审批中/分析中/实施中/已交付/已关闭, 不含 评审中/已批准/已废弃 |
| TC-FES-REQE-002 | 优先级下拉含最低 | P0 | 优先级下拉 5 选项含「最低」 |
| TC-FES-REQE-003 | 提交携带 expectedDate | P0 | 填期望日期 → createRequirement body 含 expectedDate |
| TC-FES-PRIO-001 | TasksPage 优先级含最低 | P0 | TasksPage 抽屉优先级下拉含「最低」 |
| TC-E2E-REQE-001 | remap+新状态+LOWEST+expectedDate+standing | P0 | docker 重建;seed 旧状态需求→启动 remap 为新值;POST 新状态/LOWEST/expectedDate 成功;19 表;存量(除 remap)不变 |

## 三、回归风险矩阵

| 区域 | 改动 | 保护 | 等级 |
|------|------|------|------|
| RequirementStatus 枚举 | 6 态替换 | 新 TC + 既有 Requirement 测试(QueryTest APPROVED→IN_ANALYSIS 同步) | 🟡中 |
| 共用 Priority +LOWEST | 加值(非删) | 旧值仍合法,既有 4 实体优先级测试不破 + 新 TC | 🟢低 |
| Requirement +expectedDate | 加可空字段 | 既有 detail 字段集测试(presence-loop,加字段不破) | 🟢低 |
| 存量 remap | bootstrap runner | E2E remap + 存量不变断言 | 🟡中 |

## 四、补充顺序
1. P0 全部:TC-REQE-001..005 + TC-PRIO-001 + TC-FES-REQE-001..003 + TC-FES-PRIO-001 + TC-E2E-REQE-001
