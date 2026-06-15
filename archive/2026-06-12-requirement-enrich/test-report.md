# v0.0.19-requirement-enrich 测试报告

> 测试日期：2026-06-15 ｜ 环境：Java 8(docker) + H2 / Vitest 2.1.9 + RTL / MySQL 8(docker E2E)
> 被测版本：基线 v0.0.18-workbench / commit 80a62d5（本变更未提交）

## 一、总体概况

| 指标 | 数值 |
|------|------|
| 后端 | 348 / 348（341 baseline + 7 new） |
| 前端 | 77 / 77（73 baseline + 4 new）+ tsc clean |
| 通过率 | 100% |
| Lint | checkstyle + eslint 0 违规 |

新增后端测试：RequirementEnrichTest(4) + RequirementStatusBackfillTest(1) + PriorityTest(1) + DemandControllerCreateTest LOWEST(1)。
新增前端测试：RequirementEditDrawer(3) + TasksPage 优先级最低(1)。

## 二、E2E（docker, MySQL 卷保留, classic builder 绕过 registry flake）

| 路径 | 状态 | 说明 |
|------|------|------|
| 状态 remap（真实数据） | ✅ | seed REQ-REMAP-E2E status=IN_REVIEW → 启动后 IN_APPROVAL；log "remapped 1 row" |
| 新状态创建 | ✅ | POST status=IN_ANALYSIS → 201 |
| 旧状态拒绝 | ✅ | POST status=APPROVED → 400 |
| 优先级 LOWEST | ✅ | POST priority=LOWEST → 201 |
| 期望交付日期 | ✅ | POST expectedDate=2026-09-01 → 返回 |
| 表数 / 前端 / standing | ✅ | 19 表；frontend HTTP 200；测试行已清理 |

## 三、Step 0 多路评审

| 维度 | C | H | M | L |
|------|---|---|---|---|
| 代码质量 | 0 | 0 | 1 | 3 |
| 测试/覆盖 | 0 | 0 | 3 | 4 |
| 文档一致 | 0 | 0 | 1 | 3 |
| **汇总** | **0** | **1** | **≈4** | **≈7** |

阈值内（C=0 / H≤3 / M≤10）。已修：

| 级 | 项 | 处置 |
|---|---|---|
| H | 跨实体 LOWEST 仅单测,未经端点 | ✅ 补 DemandControllerCreateTest LOWEST→201 |
| M | 旧状态拒绝仅测 APPROVED | ✅ 参数化覆盖 IN_REVIEW/APPROVED/IN_DEV/DEPRECATED |
| M | expectedDate PUT 契约未固定 | ✅ 固定为全量替换 + 补清空测试 |
| M | 回填字段不变量仅 1 行 | ✅ 加 DRAFT 行校验 |
| M | test-plan TC-PRIO-001 描述过时 | ✅ 改为实际(单测+demand 端点) |
| L | eslint 失效指令 / .stdd typo / REQE-001 漏「已批准」 | ✅ 全清 |

未修（L,阈值内）：detail 字段集 presence-only（值由 TC-REQE-005 覆盖）；前端 getByText 未 scope（当前唯一）；CLOSED 切换 closeReason state 不清（忠实移植，非回归）。

## 四、十一类失败模式 (a-k)：命中 0

remap 是有意存量变更(保留语义,E2E 真实数据证);priority 共用加值不破其它 3 实体(端点测试证);契约前后端一致。

## 五、结论

可交付。需求状态 6 态调整 + 存量 remap（E2E 真实数据验证）+ 共用 Priority 五级（端点证跨实体）+ expectedDate。0 新表。无 C/H 遗留(H 已修)，M 全修。

### 质量信号

| 信号 | 状态 |
|------|------|
| 单元/集成 | ✅ 后端 348 + 前端 77 |
| E2E | ✅ remap+新状态+LOWEST+expectedDate+存量,19 表 |
| Lint / tsc | ✅ 0 违规 / clean |
| 失败模式 | ✅ 0 命中 |
