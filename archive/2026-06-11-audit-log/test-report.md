# 测试报告 — v0.0.15-audit-log

> 版本：v0.0.15 | 日期：2026-06-11 | Baseline：v0.0.14-sprint-feature-link / 717ae99

## 1. 总体概况

| 指标 | 后端 | 前端 |
|---|---|---|
| 总数 | 309 | 58 |
| 通过 | 309 | 58 |
| 失败 | 0 | 0 |
| 跳过 | 0 | 0 |
| 通过率 | 100% | 100% |
| 类型检查 | mvn compile ✅ | tsc --noEmit ✅ |

本版新增：后端 +16（293→309），前端 +4（54→58）。

### 1.1 覆盖率诊断（变更文件）

新 capability `entity-audit-log` 全栈自动化覆盖（无覆盖真空）：切面 9 + 读 API 5 + perf 2 + 前端 4。

## 2. 按模块统计

| 测试文件 | 用例 | 覆盖 TC |
|---|---|---|
| AuditAspectIntegrationTest | 9 | TC-AUD-001..009 |
| AuditLogControllerQueryTest | 5 | TC-AUD-010..014 |
| AuditLogSqlCountTest | 2 | TC-PERF-AUD-001/002 |
| LegacyProductCategoryCleanupTest (MOD) | 3 | 表数 17→18 |
| AppLayout.test (+TC-FES-AUD-001) | — | 系统组 |
| AuditLogsPage.test | 2 | TC-FES-AUD-002/003 |
| AppRoutes.test (+TC-FES-AUD-004) | — | /sys/audit-logs |

## 3. E2E 测试结果

| 检查 | 结果 |
|---|---|
| SHOW TABLES = 18（含 rainier_audit_log） | ✅ |
| CREATE requirement → audit CREATE REQUIREMENT#4 summary 正确 | ✅ |
| UPDATE → audit UPDATE | ✅ |
| DELETE → audit DELETE；反查倒序 DELETE,UPDATE,CREATE | ✅ |
| 失败写(dup 409) → audit 总数 4→4 不变（失败不记） | ✅ |
| 既有 3 requirements 未改（standing 约束） | ✅ |

**结论**：E2E 全链 green。

## 4. 失败项分析

无失败项。

## 5. 功能/测试覆盖对照

| Capability | Requirements | Scenarios | TC | 自动化覆盖 | 状态 |
|---|---|---|---|---|---|
| entity-audit-log (NEW) | 7 | 13 | TC-AUD-001..014 + perf | 100% | ✅ |
| frontend-scaffold (MOD) | 3 | 4 | TC-FES-AUD-001..004 | 100% | ✅ |
| **Total** | **10** | **17** | **22 P0** | **100%** | ✅ |

## 6. 设计调整说明

见 `design-adjustments.md`。摘要：PA-1..PA-4 + Step 0.3 修复 6 项（含 C/H 级 TC-AUD-006 重言式 → 加事务内 +1 在场断言）。

## 7. 多路并行技术评审（Step 0）

| 维度 | 初始 | 修复后 |
|---|---|---|
| 代码质量 | C:0 H:0 M:2 L:3 | C:0 H:0 M:2 L:3（M1 同事务耦合记 v0.0.16 候选；M2 dead-defense 无害）|
| 测试/配置 | C:1 H:1 M:5 L:2 | C:0 H:0 M:1 L:2（C1+H1 TC-AUD-006 已修；M1/M2/M3/M5 已修；M4 记录）|
| 文档/Skills | C:0 H:0 M:0 L:5 | C:0 H:0 M:0 L:2（L1/L2/L4 已修；L3/L5 cosmetic）|
| **合计** | **C:1 H:1 M:7 L:10** | **C:0 H:0 M:3 L:7** |

修复 7 项（1C + 1H + 5M/L）；剩余 M/L 记录在 design-adjustments.md §C，均在阈值内（C=0 ✅ H≤3 ✅ M≤10 ✅）。

**关键修复**：TC-AUD-006（同事务回滚）原为重言式（"回滚后审计行不存在" 从未写入也满足）。Step 0.3 加入「事务内审计行 +1 在场」断言：先证审计行写进了开启的事务（count==before+1），再证回滚后归零。二者配对才真正验证「加入同事务并回滚」。

## 8. 十一类失败模式检查

| 模式 | 结果 |
|---|---|
| (a) 幻觉行为 | ✅ 路径/错误串/端点经 agent 核实 |
| (b) 范围蔓延 | ✅ 仅 LegacyCleanup 跨版本表数（PA-2）|
| (c) 级联错误 | ✅ @AfterReturning 只记成功；同事务耦合(Code-M1)为有意设计、记 v0.0.16 |
| (d) 上下文丢失 | ✅ 匹配 12 Decisions |
| (e) 工具误用 | N/A |
| (f) 运行时行为偏差 | ✅ E2E 验证切面运行时真实织入 |
| (g) 管线断链 | ✅ 业务写→切面→审计行 双链 E2E 验证 |
| (h) 内容质量偏差 | N/A |
| (i) 指令衰减 | ✅ standing 约束「不改存量数据」E2E 验证 |
| (j) 覆盖真空 | ✅ entity-audit-log 100% 自动化覆盖 |
| (k) 契约断层 | ✅ AuditLogDetail 字段名 == 前端 AuditLog 接口（agent k-check）|

## 8. 结论

| 质量信号 | 状态 |
|---|---|
| 后端 309/309 | ✅ |
| 前端 58/58 + tsc | ✅ |
| E2E 全链 + 18 表 + 存量未改 | ✅ |
| Spec→TC 覆盖 | ✅ 100%（22/22 P0）|
| Step 0 评审 | ✅ C:0 H:0 M:3（阈内，修复 C1+H1）|
| 11 类失败模式 | ✅ 无命中 |
| 🔴 AOP 横切风险 | ✅ 293 全量回归零失败；同事务+失败不记+防递归均验证 |

**部署建议**：v0.0.15 质量达标，可进入 Phase 6 DELIVER。遗留 v0.0.16 候选：审计写防御性截断（Code-M1 同事务耦合）+ 审计读权限收口。
