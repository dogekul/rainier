# v0.0.13-product-restructure 测试报告

> 日期：2026-06-11
> Baseline：v0.0.12-product / commit fdb82e0
> 对应 test-plan：64 P0 TC（3 LCLN + 10 PROD + 25 PMOD + 11 FEAT + 3 PERF + 6 FES + 6 E2E）

## 一、总体概况

| 套件 | 总数 | 通过 | 失败 | 跳过 | 通过率 | 耗时 |
|------|------|------|------|------|--------|------|
| Backend (mvn test) | 269 | 269 | 0 | 0 | **100%** | ~75s |
| Frontend (vitest) | 51 | 51 | 0 | 0 | **100%** | ~3s |
| TypeScript (tsc --noEmit) | — | ✅ | — | — | clean | ~8s |
| ESLint | — | 0 errors | — | — | 1 个 v0.0.8 遗留 warning（不在本 diff） | — |
| E2E (docker + curl) | 6 TC | 6 | 0 | 0 | **100%** | live |

测试数变化：backend 265 → **269**（−22 category +26 新）；frontend 49 → **51**（−1 +3）。

### 1.1 覆盖率诊断（变更文件）

Java 项目未配置 jacoco（与 v0.0.10-12 一致）— 以 TC 映射代偿：64 P0 TC 全部有自动化或 E2E 执行记录（第五节）。变更核心 `ProductModuleService` 的 3 算法（depth walk / cycle DFS / cross-product）各有 ≥2 个独立 TC 直接命中。

## 二、按模块统计

| 测试类 | 数量 | 备注 |
|--------|------|------|
| LegacyProductCategoryCleanupTest | 3 | TC-LCLN-001..003（fixture 假表 → DROP → 断言；idempotent；16 表计数） |
| ProductControllerCreate/Query/Update/DeleteTest | 13 | TC-PROD-001..010 + 3 个回归附加 |
| ProductModuleControllerCreateTest | 12 | TC-PMOD-001..012 |
| ProductModuleControllerUpdateTest | 6 | TC-PMOD-013..018（reparent 三检） |
| ProductModuleControllerQueryTest | 3 | TC-PMOD-019..021 |
| ProductModuleControllerDeleteTest | 4 | TC-PMOD-022..025（双向 409 + 顺序） |
| Feature 4 测试类 + perf | 23 | TC-FEAT-001..011 继承不变（seed 去 category） |
| 3 个 perf SqlCountTest | 3 | PROD ≥2∧≤4 / PMOD ≥4∧≤6 / FEAT ≥4∧≤5 |
| 其余 v0.0.12 既有 | 202 | 全部无回归 |
| Frontend 25 文件 | 51 | 含 TC-FES-PROD-001..003 / PMOD-001..002 / FEAT-001 |

## 三、E2E 测试结果

环境：docker compose（MySQL 8 + Spring Boot + Nginx 前端），真实 MySQL。

| TC | 内容 | 结果 |
|----|------|------|
| TC-E2E-001 | `SHOW TABLES` = 16，无 rainier_product_category | ✅ |
| TC-E2E-002 | `DESCRIBE rainier_product` 无 category_id | ✅ |
| TC-E2E-003 | `DESCRIBE rainier_product_module` 含 parent_id BIGINT NULL + MUL 索引 | ✅ |
| TC-E2E-004 | 建链 Product→模块A→子A1→孙A11→Feature；pathName "模块A / 子模块A1 / 孙模块" 富化 | ✅ |
| TC-E2E-005 | 跨产品 parent 400 / 自指 cycle 400 / 第 4 层 "max module depth exceeded: 4 > 3" 400 | ✅ |
| TC-E2E-006 | 删 A1（双引用）→409 features；删 Feature 204；再删 A1→409 sub-modules；删 Product→409 modules；倒序清理 5×204 | ✅ |

**LegacyCleanup 生产级双分支验证**：首次启动日志 `dropped legacy table` + `dropped legacy category_id column`；重启后 `no-op — schema already clean`。E2E 实体全部 `*-E2E13` 前缀自建自清，未触碰既有数据。

## 四、失败项详细分析

无失败项。Phase 5 迭代中发现并修复 1 项（Round 0 前的 Phase 4 期间）：ProductModuleControllerQueryTest 中文断言 charset（ISO-8859-1 → UTF-8，PA-2）。

## 五、功能/测试覆盖对照

| 功能 | Requirements | Scenarios | TC | 自动化 | 状态 |
|------|----|----|-----|------|------|
| Schema Migration | — (proposal C7) | — | 3 | 3 集成 + 3 E2E | ✅ |
| entity-product (MOD) | 4 | 10 | 10 | 13 集成 + 1 perf | ✅ |
| entity-product-module (MOD) | 7 | 25 | 25 | 25 集成 + 1 perf | ✅ |
| entity-feature (继承) | 4 | — | 11 | 22 集成 + 1 perf | ✅ |
| frontend-scaffold (MOD) | 5 | 8 | 6 | 6 组件 | ✅ |
| E2E | — | — | 6 | 6 curl live | ✅ |
| **Total** | **16(+4 继承)** | **43** | **64** | **64/64** | ✅ **100%** |

## 六、设计调整说明

见 `design-adjustments.md`：PA-1..PA-5（Phase 4）+ Step 0 评审 H×3 / M×2 修复 + M×2 / L×7 记录。要点：
- LegacyCleanup 包位置 domain-following（design.md 已更正）
- spec「reparent 到祖先」Scenario 标题矛盾已修正（行为本来正确）
- application-test.yml 显式声明 depth.max=3
- ProductModulesPage 树形视图完全替换 Table（PA-5）

## 七、修复确认记录（Phase 5 迭代）

| Round | 修复 | 复核 |
|-------|------|------|
| 0 | H-1 test yml 显式配置 / H-2 design.md 包名×2 / H-3 spec Scenario 改名 / M-2 mock 去 categoryId / M-4 Decision 11 标题 / .stdd.yaml current | backend 269 ✓ frontend 51 ✓ tsc ✓ |

## 八、结论

| 质量信号 | 结果 |
|----------|------|
| Backend / Frontend / E2E 通过率 | 100% / 100% / 100% |
| Step 0 三路评审（修复后） | C:0 H:0 M:2(记录) L:7(记录) |
| Diff 审查 | 63 文件全部在 proposal Impact 内，无范围蔓延 |
| 十一类失败模式 (a-k) | 11/11 PASS（陷阱 A-L 12 条逐一落实核对） |
| 覆盖真空 (j) | 无 — 每个 capability ≥1 自动化层 |
| 契约断层 (k) | 无 — parentId/pathName/pathCodes 前后端字段名一致（tsc 兜底） |
| Schema 迁移 | 真实 MySQL 双分支验证（DROP + idempotent） |

**部署建议**：可交付。注意事项：(1) 生产部署时 LegacyCleanup 将不可逆 DROP rainier_product_category 表与 rainier_product.category_id 列 — 本版无生产数据，风险已知可接受；(2) v0.0.14 候选：ancestor-reparent 专项测试 / depth 配置调大时的存量校验 / 拖拽 reparent UI。
