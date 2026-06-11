# v0.0.16-project-type 测试报告

> 测试日期：2026-06-12
> 测试环境：macOS (darwin 25.5.0)；Java 8（docker maven:3.9-eclipse-temurin-8）；JUnit5 + MockMvc + H2；Vitest 2.1.9 + RTL；MySQL 8 (docker E2E)
> 被测版本：基线 v0.0.15-audit-log / commit e6b0878（本变更未提交）

## 一、总体概况

| 指标 | 数值 |
|------|------|
| 后端用例总数 | 320（309 baseline + 11 new） |
| 后端通过 | 320 |
| 后端失败 | 0 |
| 前端用例总数 | 62（58 baseline + 4 new） |
| 前端通过 | 62 |
| 前端失败 | 0 |
| 通过率 | 100% |

### 1.1 覆盖率诊断（仅变更文件）

> 仅诊断，不作门禁。

| 变更文件 | 覆盖 | 状态 |
|----------|------|------|
| ProjectType.java | 常量类，由 create/update/list/coalesce 全路径间接覆盖 | ✅ |
| Project.java (projectType 列+getter/setter) | TC-PROJTYPE-001..010 | ✅ |
| ProjectService (create/update/list) | TC-PROJTYPE-001..007 | ✅ |
| ProjectDetail.from (coalesce) | TC-PROJTYPE-010（null + present 双分支） | ✅ |
| ProjectTypeBackfill.java | TC-PROJTYPE-009 + E2E（docker 实测回填 3 行） | ✅ |
| ProjectsPage.tsx (下拉/列/过滤/提交) | TC-FES-PROJTYPE-001..004 | ✅ |

## 二、按模块统计

| 测试模块 | 用例数 | 通过 | 说明 |
|----------|--------|------|------|
| ProjectControllerProjectTypeTest | 8 | 8 | create 默认/显式/非法 + update 转化/保留/非法 + list 过滤 + detail |
| ProjectTypeBackfillTest | 1 | 1 | NULL→CASUAL 回填 + 其它列不变 |
| ProjectDetailProjectTypeTest | 2 | 2 | null→CASUAL coalesce + present pass-through |
| ProjectControllerQueryTest（同步） | 5 | 5 | 字段集追加 projectType |
| ProjectsPage.test.tsx | 7 | 7 | 既有 3 + 新 4（类型下拉默认/列中文/过滤带参/提交携带） |
| 后端全量回归 | 320 | 320 | 无回归 |

## 三、E2E 测试结果

### 3.1 总体概况

| 指标 | 数值 |
|------|------|
| E2E 用例数 | 1（TC-E2E-PROJTYPE-001，多步关键路径） |
| 通过 | 1 |
| 失败 | 0 |

### 3.2 关键路径结果（docker compose + MySQL，仅 rebuild backend，MySQL 卷保留）

| 路径 | 状态 | 说明 |
|------|------|------|
| 加列：`project_type varchar(16) NULL`（ddl-auto=update） | ✅ | 列 nullable，存量行不阻塞 ALTER |
| 启动回填：现有 3 项目 NULL→CASUAL | ✅ | 日志 `backfilled 3 rainier_project rows project_type → CASUAL` |
| 存量数据不变（standing 约束） | ✅ | before/after 快照：id 1/2/3 的 code/name/status/owner/enabled/del_flag 一字未改，仅新增 project_type=CASUAL |
| 创建：POST 默认→CASUAL；显式→FORMAL | ✅ | id=4 CASUAL，id=5 FORMAL |
| 转化：PUT CASUAL→FORMAL | ✅ | id=4 → FORMAL |
| 保留：PUT 省略 projectType（改 name/status） | ✅ | id=4 仍 FORMAL，无静默降级（D4 端到端验证） |
| 过滤：`?projectType=FORMAL`/`=CASUAL` | ✅ | FORMAL→[5,4]；CASUAL→[3,2,1] 既有 |
| 审计白拿（v0.0.15 切面） | ✅ | UPDATE PROJECT#4 / CREATE PROJECT#4,#5 自动入审计表 |
| 清理 + 表数 | ✅ | DELETE 4,5 → 204；`SHOW TABLES`=18（0 新表）；API 活动项目回到 [1,2,3] |

### 3.3 E2E 结论

运行时行为验证通过：两层默认（回填 + 读兜底）真实生效，转化/保留/过滤端到端正确，存量数据零改动，审计自动留痕。

## 五-B、多路并行 Review 结果（Step 0）

### Review 迭代历史

| 轮次 | C | H | M | L | 状态 |
|------|---|---|---|---|------|
| 1 | 0 | 0 | 2 | 8 | 阈值内通过（C=0 / H≤3 / M≤10）;主动修 2 M + 1 L |

### 最终 Review 汇总

| 维度 | Critical | High | Medium | Low | 总计 |
|------|----------|------|--------|-----|------|
| 代码质量 | 0 | 0 | 1 | 3 | 4 |
| 测试/配置 | 0 | 0 | 0 | 3 | 3 |
| 文档/Skills | 0 | 0 | 1 | 2 | 3 |

### Review 已修复问题

| # | 严重性 | 文件 | 问题 | 状态 |
|---|--------|------|------|------|
| Code-M1 | M | ProjectService.update | projectType set 早于 owner 校验 → 重排「校验早/写入晚」镜像 status | ✅ 已修复 |
| Docs-M1 | M | test-plan.md §三 | 执行矩阵用缩写 TC-001 → 改规范全名 TC-PROJTYPE-* | ✅ 已修复 |
| Test-L1 | L | ProjectControllerProjectTypeTest | TC-008 冗余 body.has 断言 + unused JsonNode import → 移除 | ✅ 已修复 |

### Review 已知限制（未修复，阈值内）

| # | 严重性 | 文件 | 问题 |
|---|--------|------|------|
| Code-L | L | ProjectService.list | projectType 过滤不校验枚举（非法值静默 0 行）——与既有 status/enabled 过滤同款行为，有意一致，非回归 |
| Test-L3 | L | ProjectsPage.test | 无「清回全部类型」负路径测试——test-plan 未要求 |
| Spec-L2 | L | entity-project spec | 过滤 scenario 的 AND 缺 SHALL——逐字镜像既有 status 过滤 house style |

## 六、设计调整说明

3 项 Minor 调整（update 校验/写入顺序重排、字段集测试风险 🟡→🟢、前端过滤专用 refetch effect）。详见 [design-adjustments.md](design-adjustments.md) 与 [pending-adjustments.md](pending-adjustments.md)。

## 七、修复确认记录

| 问题 | 修复文件 | 状态 |
|------|----------|------|
| Code-M1 校验/写入顺序 | ProjectService.java | ✅（重跑 27 project 测试 + checkstyle 绿） |
| Docs-M1 TC-ID | test-plan.md | ✅ |
| Test-L1 冗余断言/import | ProjectControllerProjectTypeTest.java | ✅ |

## 八、结论

可交付。纯实体加字段（0 新表、0 新依赖、0 新端点），标准 CRUD 模式扩展;两层默认（回填 + 读兜底）经单测与 docker E2E 双重验证;standing 约束（不改存量业务数据）由 before/after MySQL 快照证明。无 C/H 问题，2 M 已修，剩余 L 均为与既有模式一致的有意取舍。

### 8.1 质量信号汇总

| 信号源 | 状态 | 备注 |
|--------|------|------|
| 单元/集成测试 | ✅ | 后端 320/320 + 前端 62/62，通过率 100% |
| E2E 测试 | ✅ | 加列+回填+转化+保留+过滤+审计+存量不变 全绿，18 表 |
| Lint (checkstyle + eslint) | ✅ | 0 违规 |
| 类型检查 (tsc) | ✅ | clean |
| 多版本测试 | N/A | — |
| 覆盖率 | N/A | 仅诊断，变更文件全覆盖，0 低覆盖 |
| 十一类失败模式 (a-k) | ✅ | 命中 0 |
