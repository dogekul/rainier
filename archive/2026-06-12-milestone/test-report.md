# v0.0.17-milestone 测试报告

> 测试日期：2026-06-12
> 测试环境：macOS (darwin 25.5.0)；Java 8（docker maven:3.9-eclipse-temurin-8）；JUnit5 + MockMvc + H2；Vitest 2.1.9 + RTL；MySQL 8 (docker E2E)
> 被测版本：基线 v0.0.16-project-type / commit 75c53a7（本变更未提交）

## 一、总体概况

| 指标 | 数值 |
|------|------|
| 后端用例总数 | 336（320 baseline + 16 new） |
| 后端通过 | 336 |
| 后端失败 | 0 |
| 前端用例总数 | 66（62 baseline + 4 new） |
| 前端通过 | 66 |
| 前端失败 | 0 |
| 通过率 | 100% |

### 1.1 覆盖率诊断（仅变更文件）

| 变更文件 | 覆盖 | 状态 |
|----------|------|------|
| Milestone / MilestoneStatus | TC-MILE-001..014 间接全覆盖 | ✅ |
| MilestoneService (create/list/update/delete + 校验) | TC-MILE-001..014 | ✅ |
| MilestoneController | TC-MILE-001..014 | ✅ |
| ProjectService.delete (级联) | TC-MILE-CAS-001/002 + 既有 ProjectControllerDeleteTest | ✅ |
| MilestonesPanel.tsx | TC-FES-MILE-002/003/004 | ✅ |
| ProjectsPage.tsx (里程碑按钮/面板挂载) | TC-FES-MILE-001 | ✅ |

## 二、按模块统计

| 测试模块 | 用例数 | 通过 | 说明 |
|----------|--------|------|------|
| MilestoneControllerTest | 14 | 14 | create 默认/显式/校验(projectId/code/targetDate/status/dup/跨项目) + list 过滤排序 + update + 软删 |
| ProjectMilestoneCascadeTest | 2 | 2 | 级联软删 + 被需求引用 409 回滚里程碑不删 |
| LegacyProductCategoryCleanupTest（同步） | 3 | 3 | 表数 18→19 + rainier_milestone |
| MilestonesPanel.test.tsx | 3 | 3 | 列出/新建携带 projectId/删除 |
| ProjectsPage.test.tsx | 8 | 8 | 既有 7 + 里程碑按钮展开面板 |
| 后端全量回归 | 336 | 336 | 无回归（含 v0.0.16 ProjectService 构造变更） |

## 三、E2E 测试结果

### 3.1 总体概况

| 指标 | 数值 |
|------|------|
| E2E 用例数 | 1（TC-E2E-MILE-001，多步关键路径） |
| 通过 | 1 |

### 3.2 关键路径结果（docker compose + MySQL，仅 rebuild backend，MySQL 卷保留）

| 路径 | 状态 | 说明 |
|------|------|------|
| 加表 rainier_milestone（ddl-auto=update） | ✅ | SHOW TABLES 18→19 |
| create 默认 / 显式 | ✅ | 默认 PLANNED+sortOrder0；显式 REACHED+sortOrder2+actualDate |
| 校验 | ✅ | bad project 400 / bad status 400 / dup code 409 |
| list sortOrder ASC | ✅ | M-1(0) 在 M-2(2) 前 |
| update 标记达成 | ✅ | PLANNED→REACHED + actualDate |
| 级联软删 | ✅ | DELETE 测试项目 204 → 该项目里程碑 total 0；DB del_flag=1（软删非物删） |
| 审计白拿（v0.0.16 切面） | ✅ | CREATE/CREATE/UPDATE MILESTONE 自动入审计表 |
| 存量数据不变（standing） | ✅ | 现有 3 项目 before/after 快照逐字不变；API 活动项目回到 [1,2,3] |

### 3.3 E2E 结论

运行时行为验证通过：新实体 CRUD、复合唯一、sortOrder 排序、级联软删、审计自动留痕均真实生效；存量业务数据零改动。

## 五-B、多路并行 Review 结果（Step 0）

### Review 迭代历史

| 轮次 | C | H | M | L | 状态 |
|------|---|---|---|---|------|
| 1 | 0 | 0 | 2 | 8 | 阈值内通过；主动修 2 M + 2 加固 + 1 L |

### 最终 Review 汇总

| 维度 | Critical | High | Medium | Low | 总计 |
|------|----------|------|--------|-----|------|
| 代码质量 | 0 | 0 | 1 | 3 | 4 |
| 测试/配置 | 0 | 0 | 0 | 4 | 4 |
| 文档/Skills | 0 | 0 | 1 | 1 | 2 |

### Review 已修复问题

| # | 严重性 | 文件 | 问题 | 状态 |
|---|--------|------|------|------|
| Docs-M | M | entity-milestone spec / proposal | code 误述为「默认值」，实为 @NotBlank 必填，且无覆盖 | ✅ 修正表述 + 新增「缺 code→400」scenario + TC-MILE-014 |
| Code-M1 | M | Milestone / MilestoneService | sort_order 列可空 vs 非空不变量 | ✅ 字段默认 0 + update keep-current 注释 |
| 加固-1 | — | ProjectMilestoneCascadeTest | CAS-001 未断言项目本身已删 | ✅ 补 GET project→404 |
| Slices-L | L | slices.md | 过时 testid milestone-create-btn | ✅ 改 milestone-save-btn |

### Review 已知限制（未修复，阈值内）

| # | 严重性 | 文件 | 问题 |
|---|--------|------|------|
| Code-L2 | L | MilestoneRepository | countByProjectId 暂无调用方（前瞻保留） |
| Code-L3 | L | MilestoneService | actualDate 无与 status 交叉校验（D4 无状态机有意宽松） |
| Test-L | L | Milestone/Panel tests | TC-MILE-009 弱负路径；TC-FES-MILE-003 未断言 refetch（可选加固） |
| Code-审计 | L | ProjectService 级联 | 级联软删走 repo（非 MilestoneService.delete），不单独审计——项目删除即审计事件，有意 |

## 六、设计调整说明

3 项 Minor 调整（code 必填表述修正、sortOrder 不变量文档化、测试补强）。详见 [design-adjustments.md](design-adjustments.md) 与 [pending-adjustments.md](pending-adjustments.md)。

## 七、修复确认记录

| 问题 | 修复文件 | 状态 |
|------|----------|------|
| Docs-M code 必填 | spec/proposal/test-plan + MilestoneControllerTest | ✅（重跑 16 milestone 测试 + checkstyle 绿） |
| Code-M1 sortOrder | Milestone.java / MilestoneService.java | ✅ |
| CAS-001 加固 | ProjectMilestoneCascadeTest.java | ✅ |
| Slices testid | slices.md | ✅ |

## 八、结论

可交付。新增 Milestone 实体（+1 表 rainier_milestone、新包、0 新依赖）+ 一处既有 ProjectService.delete 级联软删扩展。级联事务原子性（被引用项目 409 回滚、里程碑不删）经单测 CAS-002 + docker E2E 双证；复合唯一两方向验证；standing 约束由 before/after MySQL 快照证明。无 C/H 问题，2 M 已修。

### 8.1 质量信号汇总

| 信号源 | 状态 | 备注 |
|--------|------|------|
| 单元/集成测试 | ✅ | 后端 336/336 + 前端 66/66，通过率 100% |
| E2E 测试 | ✅ | 加表+CRUD+校验+排序+级联+审计+存量不变 全绿，19 表 |
| Lint (checkstyle + eslint) | ✅ | 0 违规 |
| 类型检查 (tsc) | ✅ | clean |
| 多版本测试 | N/A | — |
| 覆盖率 | N/A | 仅诊断，变更文件全覆盖，0 低覆盖 |
| 十一类失败模式 (a-k) | ✅ | 命中 0 |
