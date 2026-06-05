# 设计调整说明 — 删除 Organization 的 isPmo 字段

> 原始设计基线：Phase 2 产出的 design.md（6 决策）+ 2 个 MODIFIED specs + test-plan.md
> 调整来源：Phase 5 多路并行 Review

## 调整汇总

| # | 调整类型 | 涉及文档 | 严重程度 | 调整阶段 | 用户已知 |
|---|---|---|---|---|---|
| 1 | metadata 修正（评审命中） | test-plan.md §1.3 baseline table | Minor | Phase 5 Step 0 | 是 |
| 2 | metadata 修正（评审命中） | test-plan.md 后端合计 65 → 64 + tasks.md ≥63 → 64 描述 | Minor | Phase 5 Step 0 | 是 |

无任何设计层 / 行为层的偏离。所有移除按 design.md 6 个决策原样实施。

## 调整详细说明

### 调整 1：test-plan.md §1.3 baseline `OrganizationControllerQueryTest` 行 "0 修改" 标错（Minor）

- **原始内容**：test-plan §1.3 已有测试资产表中 `OrganizationControllerQueryTest.java` 行 "0 修改（未提 isPmo）"
- **实际**：M02 在该文件内新增了 2 个 `@Test`（`get_byId_responseDoesNotContainIsPmo`、`put_withIsPmoInBody_silentlyIgnored_returns200`）覆盖 TC-RMP-002 + TC-RMP-003
- **修复**：行末改为 "+2 新增（TC-RMP-002 + TC-RMP-003）"
- **触发**：Phase 5 Step 0 docs/STDD 代理报 M-1（表自相矛盾）
- **影响范围**：仅 plan 文档；行为不变

### 调整 2：test-plan.md 后端合计 65 → 64；tasks.md "≥ 63" 表述（Minor）

- **原始内容**：test-plan §1.3 末行 "后端合计 65"；tasks §1.2.4 / §4.1.6 "≥ 63"
- **实际**：v0.0.4 baseline 62 + 2 新增（TC-RMP-002/003）= 64；TC-RMP-001 是 line 67 in-place 替换（doesNotExist 替代原 value(false)），不增计
- **修复**：test-plan 合计行 → 64；tasks.md §1.2.4 文案改为精确数字 + 说明
- **触发**：Phase 5 Step 0 docs/STDD 代理报 M-2（`.stdd.yaml` 64 vs test-plan 65 漂移）
- **影响范围**：仅 plan 文档；与 `.stdd.yaml`、实际 `mvn test` 输出对齐

## 不构成调整的 review 命中

### Known Limitation：`V1__init_org.sql` 仍含 `is_pmo` 列定义（test/config H-1）

- **代理观点**：若未来重新启用 Flyway（`spring.flyway.enabled=true`），`V1__init_org.sql:25` 中的 `is_pmo TINYINT(1) NOT NULL DEFAULT 0` 会回到 schema，违反 TC-RMP-E2E-001 契约
- **本变更立场**：design.md 决策 1 + proposal `explicitly_excluded: v1-historical-sql-rewrite` 显式将该文件标为"v1 历史档不动"。当前 `spring.flyway.enabled=false`（Adjustment #1 from v0.0.3），Hibernate `ddl-auto=update` 从 entity 生成 schema 不含 `is_pmo`，TC-RMP-E2E-001 实测通过（DESCRIBE 输出已验证）
- **接受理由**：本变更范围严格限制为"删除一个错位字段"，不触碰历史 SQL 档；未来若启用 Flyway，应作为独立 change 修订 V1（或新建 V2 ALTER 列）
- **风险登记**：在 test-report §8.1 known-limitations 标记

### L-1 ~ L-8（共 8 条）— 全部接受

- **L 共 8 条**包括：POST create 端点缺 isPmo-in-body 反向测试（与 PUT 的 TC-RMP-003 对称的）、spec 缺 POST response 字段集 Scenario、`queryByLabelText('PMO 团队')` 在原 markup 下本就找不到 (但留作冗余防御无害)、tree 端点未独立断言 doesNotExist、async test 缺 explicit await on mock settle、tasks.md 行号是 pre-edit snapshot 等
- **共同评估**：纯属可选优化；不阻塞；不修复

## 结论

2 项调整均为 Minor 文档级修正（review 直接命中），已在本阶段即时修复。Phase 4 build 完全按 Phase 2 spec 执行；移除链路（Organization entity → 3 DTOs → Service → tests → 前端 api → EditDrawer → OrganizationsPage → 新建 2 test → 主规范 in-place 编辑）整体性 + 一致性符合 design.md 6 决策。
