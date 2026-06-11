# v0.0.13-product-restructure — Pending Adjustments (Phase 4 BUILD)

## PA-1. LegacyProductCategoryCleanup 包位置偏离 design.md

- **原始设计**（design.md Decision 1 / Architecture 图）: 组件放 `com.rainier.productrestructure.bootstrap`。
- **实际实现**: 放 `com.rainier.product.bootstrap`（与 `com.rainier.sprint.bootstrap` 家族模式一致 — bootstrap 跟随其作用的 domain 包，而非按 change 命名新包）。
- **原因**: change-名包会在 v0.0.14+ 失去语义；domain 内 bootstrap 子包与 v0.0.10.1 既有模式对齐。
- **影响**: 无行为差异。design.md 在 Phase 5 汇总时更正。

## PA-2. 测试断言中文字符集修复（test-only）

- **现象**: `MockHttpServletResponse.getContentAsString()` 默认 ISO-8859-1，断言 "钱包 / 余额" 时得到乱码。
- **修复**: `getContentAsString(StandardCharsets.UTF_8)`（ProductModuleControllerQueryTest）。
- **备注**: jsonPath 断言不受影响（内部正确处理 UTF-8）；只有手工 readTree 的场景需要显式 charset。

## PA-3. 测试数与 Phase 3 预估的差异（计数级，无功能缺口）

- **Backend**: 预估 ≈271 → 实际 **269**（265 基线 − 22 category 测试 + 26 新增：3 LCLN + 25 PMOD 中 21 个为新文件计入 + Product 改写净 +2 …计数法差异，无 TC 缺失 — 25 个 TC-PMOD 全部落地：Create 12 / Update 6 / Query 3 / Delete 4）。
- **Frontend**: 预估 ≈53 → 实际 **51**（49 基线 − 1 category 测试 + 3 新增：tree page 1 + pathName 1 + route guard 1；预估把 EditDrawer 重写多算了 2 个新测试，实际是改写不加数）。
- **影响**: 无 — 所有 64 个 P0 TC 均有对应自动化（E2E 6 个为 curl 验证）。

## PA-4. E2E 验证亮点（记录非偏离）

- LegacyCleanup 在真实 MySQL 上双分支验证：首次启动 `dropped legacy table` + `dropped legacy category_id column`；重启后 `no-op — schema already clean`（TC-LCLN-002 的生产级复现）。
- E2E 实体全部用 `*-E2E13` 前缀创建并倒序清理（5 × 204），未触碰任何既有数据（遵守长期约束「测试和修复时不要删除或更改已有的数据」— 注：rainier_product_category 表的 DROP 是本变更被授权的 schema 迁移本体，非测试副作用）。

## PA-5. ProductModulesPage 树形视图替换 Table 组件（UI 实现细节）

- **原始设计**（design.md Decision 11）: "保持表格但加 depth indent（或 UL 缩进）"二选一。
- **实际实现**: 选了嵌套 UL/LI（`ProductModuleTreeView.tsx`），完全替换 Table — 树形容器语义更准，测试断言 containment 更直接。
- **影响**: ProductModulesPage 不再有列头行；search/status filter 保留（usePaginated 不变）。
