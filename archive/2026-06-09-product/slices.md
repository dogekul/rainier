# v0.0.12-product 切片执行计划

> 16 切片，全 P0。Backend 4 实体 × (impl + test) = 8 + 1 perf = 9；Frontend 4 page + Sider/Routes = 5；E2E 1 = 1。后端链 M01→M09 → 前端 M10→M15 → E2E M16。

## 切片表

| # | 优先级 | TC 覆盖 | 实现目标 | 依赖 |
|---|--------|---------|----------|------|
| M01 | P0 | — | `ProductCategoryStatus.java` (2 const + ALL set Java 8) + `ProductCategory.java` entity (@SQLDelete + columns 无 parent_id) + `ProductCategoryRepository.java` (existsByCode + countByXxx) + 3 DTOs (Create/Update/Detail) + `ProductCategoryService.java` (CRUD + owner mutable + softdelete + list batch enrich 2-stage user) + `ProductCategoryController.java` (5 endpoints) | 无 |
| M02 | P0 | TC-PCAT-001..012 (12) | `ProductCategoryControllerCreateTest.java` (8 TCs) + `ProductCategoryControllerQueryTest.java` (2 TCs incl. 12-field loop) + `ProductCategoryControllerUpdateTest.java` (2 TCs) + `ProductCategoryControllerDeleteTest.java` (1 TC) — 等 M09 加 Product FK test | M01 |
| M03 | P0 | — | `ProductStatus.java` (4 const) + `Product.java` entity (categoryId NN) + `ProductRepository.java` (existsByCode + countByCategoryId) + DTOs (Update 不含 categoryId) + `ProductService.java` (categoryId 存在性校验 → 400 / categoryName 跨层富化 / batch enrich 2-stage user+category) + Controller | M01 |
| M04 | P0 | TC-PROD-001..012 (12) | 4 个 ProductController*Test 类 — 含 TC-PROD-007 字段集 15 字段 loop + TC-PROD-008 按 categoryId 过滤 + TC-PROD-010 PUT 不接受 categoryId | M03 |
| M05 | P0 | — | `ProductModuleStatus.java` (3 const) + `ProductModule.java` entity (productId NN) + `ProductModuleRepository.java` + DTOs + `ProductModuleService.java` (productId 校验 + productName 富化) + Controller | M03 |
| M06 | P0 | TC-PMOD-001..012 (12) | 4 个 ProductModuleController*Test 类 | M05 |
| M07 | P0 | — | `FeatureStatus.java` (3 const) + `Feature.java` entity (moduleId NN) + `FeatureRepository.java` + DTOs + `FeatureService.java` (moduleId 校验 + moduleName 富化) + Controller | M05 |
| M08 | P0 | TC-FEAT-001..011 (11) | 4 个 FeatureController*Test 类 | M07 |
| M09 | P0 | TC-PCAT-012 / TC-PROD-012 / TC-PMOD-012 (3) | 在 M01/M03/M05 的 *Service.delete() 内追加 FK chain 检查 (`countByCategoryId`/`countByProductId`/`countByModuleId` > 0 → 409 "X has linked Ys")；同步补 TC-PCAT-012 / TC-PROD-012 / TC-PMOD-012 delete-with-children 测试断言 | M01/M03/M05/M07 |
| M10 | P0 | TC-PERF-PCAT/PROD/PMOD/FEAT-001 (4) | 4 个 perf test 类（每个 list endpoint 一个）— `@SpringBootTest(properties=...generate_statistics=true)` + Hibernate Statistics 范围断言 `stmtCount <= 5L && stmtCount >= 3L`（Cat 无 parent 时 ≥3；其他 ≥4） | M07 |
| M11 | P0 | — | 前端 4 个 api/{productCategory,product,productModule,feature}.ts — type + Create/Update/ListParams + 5 fn each | M07 (backend endpoint shape 稳定) |
| M12 | P0 | — | `pages/ProductCategory/ProductCategoriesPage.tsx` + `ProductCategoryEditDrawer.tsx`（无 parent 联动 — 最简） + `index.tsx` | M11 |
| M13 | P0 | — | `pages/Product/ProductsPage.tsx` + `ProductEditDrawer.tsx`（Category 下拉，加载 listCategories size=100） + index | M11 |
| M14 | P0 | — | `pages/ProductModule/ProductModulesPage.tsx` + `ProductModuleEditDrawer.tsx`（Category + Product 级联，Product 选项 filter `p.categoryId === selectedCat || categoryId 未选`） + index | M11 |
| M15 | P0 | TC-FES-PROD-001..004 (4) | `pages/Feature/FeaturesPage.tsx` + `FeatureEditDrawer.tsx`（Product + Module 级联，Module 选项 filter `m.productId === selectedProduct`） + index；`components/AppLayout.tsx` 顶级组 4 → 5（产品组 4 项 第 2 位）+ `AppRoutes.tsx` 加 4 条 /pm/product-* 路由；改 `AppLayout.test.tsx` v0.0.11「4 项 Sider」 → v0.0.12「5 项 + 产品组 4 子项」；新加 4 个 FE TC 测试 | M11-M14 |
| M16 | P0 | — | E2E：`mvn -q package -DskipTests` → `npx vite build` → `DOCKER_BUILDKIT=0 docker compose build` → `up -d --no-deps --force-recreate`：(a) `SHOW TABLES` = 17 含 4 新表 (b) `DESCRIBE rainier_{product_category,product,product_module,feature}` 字段集 (c) curl chain: 建 Category → 建 Product (跨层 categoryId 校验) → 建 Module → 建 Feature → 跨 category Product 缺 categoryId → 400 / 删 Category w/ Product → 409 "category has linked products" / 删 Product w/ Module → 409 "product has linked modules" / 删 Module w/ Feature → 409 "module has linked features" / 删 Feature → 204 | M01-M15 |

## 依赖图

```
M01 ── M02 ──┐
             │
M03 ── M04 ──┤ ── M09 ── M10 ── M11 ── M12 ──┐
M05 ── M06 ──┤                               │
M07 ── M08 ──┘                  M13 ─────────┤
                                M14 ─────────┤ ── M15 ── M16
                                              │
```

注：M09 / M10 / M11 全部依赖 backend 链完成（M07）。M12-M15 frontend 间彼此独立（除 M15 含 Sider/AppRoutes 全局变更）。

## 隐藏陷阱记录

- **陷阱 A — 4 个 *Status 类的 ALL set Java 8 模式**：必须用 `Collections.unmodifiableSet(new HashSet<>(Arrays.asList(...)))`，不可用 `Set.of(...)`（Java 8 不支持）。沿用 v0.0.10 SprintStatus 模式。
- **陷阱 B — Product/Module/Feature update DTO 不含 parent_id 字段**：Jackson 静默丢弃（v0.0.11 Decision 11 已验证），service 也不读这些字段；测试 TC-PROD-010 / TC-PMOD-010 / TC-FEAT-010 用 `taskRepo.findById(...)` 读 DB 后断言 categoryId/productId/moduleId 未变。
- **陷阱 C — 4 个 entity 富化 batch enrich Map<Long, X> 模式中，empty set 早退要单独处理**：`userIds.isEmpty()` / `parentIds.isEmpty()` 都要返回 `Collections.emptyMap()` 防止 `findAllById(emptyCollection)` 行为不一致。沿用 v0.0.10.1/v0.0.11 模式。
- **陷阱 D — M09 FK chain 检查必须在 M02/M04/M06/M08 测试之后回填**：原因是 M02 写"无引用软删成功"时还没有 Product 实体可建，等 M03 实现 Product 后才能在 M09 加"有 Product 引用 → 409"测试。所以 M02 第一次写时跳过 TC-PCAT-012（Product FK），M09 再补充。同理 M04 跳过 TC-PROD-012；M06 跳过 TC-PMOD-012；M08 不需要（Feature 无下游）。
- **陷阱 E — Sider 顶级 4 → 5 组会破坏 v0.0.11 AppLayout.test 4 个 test 之一**：v0.0.11 的「6 项 Sider」 test 不受影响（断的是「需求管理」内 6 项），但任何断「顶级组数」或「组顺序」的 test 都要查。M15 同步处理。
- **陷阱 F — TC-FES-PROD-001 在 5 个顶级组里断 「产品」 第 2 位**：现行 navGroups 是 [org, pm, hr] (3 组)；v0.0.11 仍 3 组。v0.0.12 改成 [org, product, pm, hr] (4 组)，产品 第 2 位。Sider 实际渲染：「组织 / 产品 / 需求管理 / 人事配置」 — 用户视觉上是 4 个组顶级 + 「需求管理」展开 6 项 + 「人事」展开 3 项 + 「产品」展开 4 项 + 「组织」展开 3 项。proposal 写「5 顶级组」错了 — 实际是 **4 顶级组**（不含 hr expand 的话）。修订：spec/test 都用「4 顶级组 + 产品组 4 项」 — Phase 4 Build 时同步修。
- **陷阱 G — perf 范围断言用 `>=` 防止假绿**：`stmtCount <= 5 AND stmtCount >= 3` (Cat) / `>= 4` (其他)。如果 statistics 没启用，count=0 ≤ 5 但 ≥ 3 不满足 → 立刻红。沿用 v0.0.10.1 教训。
