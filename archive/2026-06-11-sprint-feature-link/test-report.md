# 测试报告 — v0.0.14-sprint-feature-link

> 版本：v0.0.14 | 日期：2026-06-11 | Baseline：v0.0.13-product-restructure / 952e320

## 1. 总体概况

| 指标 | 后端 | 前端 |
|---|---|---|
| 总数 | 293 | 54 |
| 通过 | 293 | 54 |
| 失败 | 0 | 0 |
| 跳过 | 0 | 0 |
| 通过率 | 100% | 100% |
| 类型检查 | mvn compile ✅ | tsc --noEmit ✅ |

本版新增：后端 +24（269→293），前端 +3（51→54）。

### 1.1 覆盖率诊断（变更文件）

新 capability `entity-sprint-feature` 全栈自动化覆盖（无覆盖真空，11 类失败模式 j 通过）：
- SprintFeatureLinkService：create 校验链 4 态（惰性建立/同产品匹配/跨产品拒绝/productId 不回退）+ delete 硬删 + 3 反查方法 — 全覆盖。
- 反查 3 端点（sprint→features / feature→sprints / requirement→features 2 跳去重）：存在/空/404 三态各覆盖。
- Sprint productId：null/预绑/不可变/富化/unknown-400 — 5 TC 覆盖。
- 前端 SprintFeaturePanel（挂载+产品过滤+解绑）/ FeatureSprintsPanel（所在迭代）— 3 TC。

## 2. 按模块统计

| 测试文件 | 用例 | 覆盖 TC |
|---|---|---|
| SprintFeatureLinkControllerCreateTest | 6 | TC-SF-001..006 |
| SprintFeatureLinkControllerDeleteTest | 3 | TC-SF-007..009 |
| SprintFeatureReverseQueryTest | 8 | TC-SF-REV-001..008 |
| SprintControllerProductIdTest | 5 | TC-SPR-PF-001..004 + PA-3 |
| SprintProductEnrichSqlCountTest | 1 | TC-PERF-SPR-PF-001 (≥4∧≤7) |
| RequirementFeaturesSqlCountTest | 1 | TC-PERF-SF-REV-001 (≥2∧≤8) |
| LegacyProductCategoryCleanupTest (MOD) | 3 | 表数 16→17 |
| SprintFeaturePanel.test.tsx | 2 | TC-FES-SF-001/002 |
| FeatureSprintsPanel.test.tsx | 1 | TC-FES-SF-003 |

## 3. E2E 测试结果

| 检查 | 结果 |
|---|---|
| SHOW TABLES = 17（含 rainier_sprint_feature） | ✅ |
| rainier_sprint 加 product_id 列（nullable, bigint） | ✅ |
| 存量 2 sprint 行 product_id 仍 NULL（standing 约束未破） | ✅ |
| 挂 feature A → sprint.productId 惰性锁为 product | ✅ (=4) |
| 第二个同产品 feature → 201 | ✅ |
| 跨产品 feature → 400 "feature must belong to the sprint's product" | ✅ |
| 反查 sprint→features = 2 | ✅ |
| 反查 feature→sprints = 1 | ✅ |
| 反查 requirement→features 2 跳去重 = 2 | ✅ |
| 解绑 → 204；productId 不回退（仍 4） | ✅ |

**结论**：E2E 全链 green。

## 4. 失败项分析

无失败项。

## 5. 功能/测试覆盖对照

| Capability | Requirements | Scenarios | TC | 自动化覆盖 | 状态 |
|---|---|---|---|---|---|
| entity-sprint-feature (NEW) | 3 | 9 | TC-SF-001..009 + perf | 100% | ✅ |
| entity-sprint (MOD) | 3 | 6 | TC-SPR-PF-001..004 + REV-001/002 | 100% | ✅ |
| entity-feature (MOD) | 1 | 3 | TC-SF-REV-003..005 | 100% | ✅ |
| entity-requirement (MOD) | 1 | 3 | TC-SF-REV-006..008 | 100% | ✅ |
| frontend-scaffold (MOD) | 2 | 3 | TC-FES-SF-001..003 | 100% | ✅ |
| **Total** | **10** | **24** | **28 P0** | **100%** | ✅ |

## 6. 设计调整说明

见 `design-adjustments.md`。摘要：PA-1..PA-4（perf 预算/表数/额外 TC/helper 改名）+ Step 0.3 修复 4 项（H1 perf 文档号 / M2 SprintCreate productId / M5 前端过滤测试 / M3 反查值断言）+ D11 正向超交付（前端真做了产品过滤）。

## 7. 多路并行技术评审（Step 0）

| 维度 | 初始 | 修复后 |
|---|---|---|
| 代码质量 | C:0 H:0 M:2 L:4 | C:0 H:0 M:1 L:4（M2 orphan-link 记为 v0.0.15 候选） |
| 测试/配置 | C:0 H:1 M:6 L:4 | C:0 H:0 M:3 L:4（H1+M3+M5 已修） |
| 文档/Skills | C:0 H:0 M:2 L:2 | C:0 H:0 M:0 L:2（M1+M2 已修） |
| **合计** | **C:0 H:1 M:10 L:10** | **C:0 H:0 M:4 L:10** |

修复 4 项（1 H + 3 M）；剩余 M/L 记录在 design-adjustments.md §D，均在阈值内（C=0 ✅ H≤3 ✅ M≤10 ✅）。

## 8. 十一类失败模式检查

| 模式 | 结果 |
|---|---|
| (a) 幻觉行为 | ✅ 路径/错误串/端点全部经 agent 核实存在 |
| (b) 范围蔓延 | ✅ 仅 LegacyCleanup 跨版本表数维护（PA-2） |
| (c) 级联错误 | ✅ DataIntegrityViolation→409 在正确层；无空数组掩盖 |
| (d) 上下文丢失 | ✅ 匹配 12 Decisions；D11 正向超交付 |
| (e) 工具误用 | N/A（后端 Java） |
| (f) 运行时行为偏差 | ✅ 前端 vitest DOM + E2E curl 验证运行时 |
| (g) 管线断链 | ✅ feature→module→product + requirement→sprint→feature 双链 E2E 验证 |
| (h) 内容质量偏差 | N/A |
| (i) 指令衰减 | ✅ standing 约束「不改存量数据」E2E 验证（存量 sprint 仍 null） |
| (j) 覆盖真空 | ✅ entity-sprint-feature 100% 自动化覆盖 |
| (k) 契约断层 | ✅ 后端 DTO 字段名 == 前端 TS 接口字段名（agent k-check 逐一核对） |

## 8. 结论

| 质量信号 | 状态 |
|---|---|
| 后端测试 293/293 | ✅ |
| 前端测试 54/54 + tsc | ✅ |
| E2E 全链 + 17 表 + 存量数据未改 | ✅ |
| Spec→TC 覆盖 | ✅ 100%（28/28 P0） |
| Step 0 评审 | ✅ C:0 H:0 M:4（阈内）|
| 11 类失败模式 | ✅ 无命中 |

**部署建议**：v0.0.14 质量达标，可进入 Phase 6 DELIVER。遗留 1 项 v0.0.15 候选（orphan-link cascade cleanup，accepted limitation，与 DemandRequirementLink 同款先例）。
