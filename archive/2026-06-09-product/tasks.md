# v0.0.12-product 实现任务清单

## M01 — ProductCategory backend (no parent)

- [ ] 1.1 `domain/ProductCategoryStatus.java` — 2 const ACTIVE/ARCHIVED + ALL set Java 8
- [ ] 1.2 `domain/ProductCategory.java` — code/name/desc/status/owner_user_id 字段 + @SQLDelete + @Where
- [ ] 1.3 `repository/ProductCategoryRepository.java` — existsByCode
- [ ] 1.4 `dto/{Create,Update,Detail}Request.java`
- [ ] 1.5 `service/ProductCategoryService.java` — create/findById/list (batch user enrich) /update/delete (FK 检查留 M09)
- [ ] 1.6 `controller/ProductCategoryController.java` — 5 endpoints (`/api/product-categories`)
- [ ] 1.7 `mvn -q compile` 通过

## M02 — ProductCategory tests

- [ ] 2.1 `ProductCategoryControllerCreateTest.java` — TC-PCAT-001..006 (6 TCs)
- [ ] 2.2 `ProductCategoryControllerQueryTest.java` — TC-PCAT-007/008 (12-field loop + status filter)
- [ ] 2.3 `ProductCategoryControllerUpdateTest.java` — TC-PCAT-009/010
- [ ] 2.4 `ProductCategoryControllerDeleteTest.java` — TC-PCAT-011 (FK ref test 留 M09 补)
- [ ] 2.5 `mvn -q test -Dtest=ProductCategoryController*Test` 全绿

## M03 — Product backend (categoryId NN)

- [ ] 3.1 `ProductStatus.java` — 4 const PLANNING/ACTIVE/SUNSET/ARCHIVED
- [ ] 3.2 `Product.java` entity — 全字段 + categoryId NN
- [ ] 3.3 `ProductRepository.java` — existsByCode + countByCategoryId
- [ ] 3.4 DTOs — UpdateRequest 不含 categoryId (immutable)
- [ ] 3.5 `ProductService.java` — categoryId 存在性校验 → 400 "category not found" / batch enrich user + category / categoryCode + categoryName 富化
- [ ] 3.6 Controller — `/api/products`
- [ ] 3.7 `mvn -q compile`

## M04 — Product tests

- [ ] 4.1 `ProductControllerCreateTest.java` — TC-PROD-001..006 (含 002 categoryId not found)
- [ ] 4.2 `QueryTest` — TC-PROD-007 (15-field loop) / TC-PROD-008 (按 categoryId 过滤)
- [ ] 4.3 `UpdateTest` — TC-PROD-009 (status + owner) / TC-PROD-010 (PUT 不接受 categoryId — DB 不变)
- [ ] 4.4 `DeleteTest` — TC-PROD-011 (FK ref test 留 M09)
- [ ] 4.5 `mvn -q test -Dtest=ProductController*Test`

## M05 — ProductModule backend (productId NN)

- [ ] 5.1 `ProductModuleStatus.java` — 3 const PLANNING/ACTIVE/DEPRECATED
- [ ] 5.2 `ProductModule.java` entity (productId NN)
- [ ] 5.3 `ProductModuleRepository.java` — countByProductId
- [ ] 5.4 DTOs
- [ ] 5.5 `ProductModuleService.java` — productId 校验 + productCode/productName 富化 + batch enrich
- [ ] 5.6 Controller — `/api/product-modules`
- [ ] 5.7 compile

## M06 — ProductModule tests

- [ ] 6.1 `ProductModuleControllerCreateTest.java` — TC-PMOD-001..006
- [ ] 6.2 `QueryTest` — TC-PMOD-007/008
- [ ] 6.3 `UpdateTest` — TC-PMOD-009/010 (PUT 不接受 productId)
- [ ] 6.4 `DeleteTest` — TC-PMOD-011
- [ ] 6.5 `mvn -q test -Dtest=ProductModuleController*Test`

## M07 — Feature backend (moduleId NN)

- [ ] 7.1 `FeatureStatus.java` — 3 const
- [ ] 7.2 `Feature.java` entity (moduleId NN)
- [ ] 7.3 `FeatureRepository.java` — existsByCode (无下游 count)
- [ ] 7.4 DTOs
- [ ] 7.5 `FeatureService.java` — moduleId 校验 + moduleCode/moduleName 富化 + batch enrich
- [ ] 7.6 Controller — `/api/features`
- [ ] 7.7 compile

## M08 — Feature tests

- [ ] 8.1 `FeatureControllerCreateTest.java` — TC-FEAT-001..006
- [ ] 8.2 `QueryTest` — TC-FEAT-007/008
- [ ] 8.3 `UpdateTest` — TC-FEAT-009/010
- [ ] 8.4 `DeleteTest` — TC-FEAT-011 (无 FK — 直接软删 + GET 404)

## M09 — FK chain wires + retrofitted FK ref tests

- [ ] 9.1 `ProductCategoryService.delete()` 加 `productRepo.countByCategoryId > 0 → 409 "category has linked products"` (注入 ProductRepository)
- [ ] 9.2 `ProductService.delete()` 加 `productModuleRepo.countByProductId > 0 → 409 "product has linked modules"`
- [ ] 9.3 `ProductModuleService.delete()` 加 `featureRepo.countByModuleId > 0 → 409 "module has linked features"`
- [ ] 9.4 补回 TC-PCAT-012 / TC-PROD-012 / TC-PMOD-012 (delete-with-children → 409 with exact message)
- [ ] 9.5 `mvn -q test -Dtest='Product*Test'` 全绿

## M10 — Perf tests (4)

- [ ] 10.1 `perf/ProductCategoryListSqlCountTest.java` — `@SpringBootTest(properties=hibernate.generate_statistics=true)` + seed 4 Owner + 20 Category → `stats.getPrepareStatementCount() <= 5 && >= 3`
- [ ] 10.2 `perf/ProductListSqlCountTest.java` — seed 4 Owner + 4 Cat + 20 Product → `<= 5 && >= 4`
- [ ] 10.3 `perf/ProductModuleListSqlCountTest.java` — seed + 20 Module → `<= 5 && >= 4`
- [ ] 10.4 `perf/FeatureListSqlCountTest.java` — seed + 20 Feature → `<= 5 && >= 4`
- [ ] 10.5 `mvn -q test -Dtest='Product*SqlCountTest,FeatureListSqlCountTest'`

## M11 — Frontend api files (4)

- [ ] 11.1 `api/productCategory.ts` — ProductCategoryStatus type + ProductCategory + Create/Update + ListParams + 5 fn
- [ ] 11.2 `api/product.ts` — ProductStatus 4 values + Product (含 categoryId/categoryCode/categoryName) + 5 fn
- [ ] 11.3 `api/productModule.ts` — ProductModuleStatus 3 values + ProductModule (含 productId/productCode/productName) + 5 fn
- [ ] 11.4 `api/feature.ts` — FeatureStatus 3 values + Feature (含 moduleId/moduleCode/moduleName) + 5 fn
- [ ] 11.5 `npx tsc --noEmit -p tsconfig.json`

## M12 — ProductCategoriesPage (simplest)

- [ ] 12.1 `pages/ProductCategory/ProductCategoriesPage.tsx` (list + filter + new btn + 行操作)
- [ ] 12.2 `pages/ProductCategory/ProductCategoryEditDrawer.tsx` — owner select + status + form-error
- [ ] 12.3 `pages/ProductCategory/index.tsx`

## M13 — ProductsPage (Category select)

- [ ] 13.1 `pages/Product/ProductsPage.tsx`
- [ ] 13.2 `pages/Product/ProductEditDrawer.tsx` — Category 下拉 (listCategories size=100) + owner + status
- [ ] 13.3 `pages/Product/index.tsx`

## M14 — ProductModulesPage (Product select w/ Category filter)

- [ ] 14.1 `pages/ProductModule/ProductModulesPage.tsx`
- [ ] 14.2 `pages/ProductModule/ProductModuleEditDrawer.tsx` — Category 可选下拉 + Product 必选下拉 (filter by categoryId) + owner + status；切 Category 时清空 Product
- [ ] 14.3 `pages/ProductModule/index.tsx`

## M15 — FeaturesPage + AppLayout + AppRoutes + tests

- [ ] 15.1 `pages/Feature/FeaturesPage.tsx`
- [ ] 15.2 `pages/Feature/FeatureEditDrawer.tsx` — Product 必选 + Module 必选下拉 (filter by productId)；切 Product 时清空 Module
- [ ] 15.3 `pages/Feature/index.tsx`
- [ ] 15.4 `components/AppLayout.tsx` — navGroups 顶级 3 → 4 组 (插入 'product' 组在 'org' 之后 'pm' 之前 — 4 项产品分类/产品/产品模块/功能)
- [ ] 15.5 `AppRoutes.tsx` — 加 4 条路由 + import 4 page
- [ ] 15.6 改 `AppLayout.test.tsx` — 把 v0.0.11 「需求管理 4 项 (项目排第一)」 测试中没显式 break 的留着；新加 TC-FES-PROD-001 (5 顶级组 + 产品组 4 项 + 顺序 + /pm/product-categories href)
- [ ] 15.7 新加 `pages/Feature/FeatureEditDrawer.test.tsx` (TC-FES-PROD-004 Product/Module cascading)
- [ ] 15.8 新加 `pages/ProductCategory/ProductCategoriesPage.test.tsx` smoke (TC-FES-PROD-002 grep guard via /pm/products + render check)
- [ ] 15.9 `npx tsc --noEmit && npx vitest run` 全绿

## M16 — E2E

- [ ] 16.1 `mvn -q package -DskipTests` + `cd frontend && npx vite build`
- [ ] 16.2 `DOCKER_BUILDKIT=0 docker compose build backend frontend`
- [ ] 16.3 `docker compose up -d --no-deps --force-recreate backend frontend`
- [ ] 16.4 验证 SHOW TABLES = 17 (含 rainier_product_category, rainier_product, rainier_product_module, rainier_feature)
- [ ] 16.5 验证 DESCRIBE 4 张新表字段集
- [ ] 16.6 curl chain:
  - 建 Cat → 201 富化 ownerName
  - 建 Product w/ categoryId → 201 富化 categoryName
  - 建 Product w/ 不存在 categoryId → 400 "category not found"
  - 建 Module w/ productId → 201 富化 productName
  - 建 Feature w/ moduleId → 201 富化 moduleName
  - 删 Category w/ Product → 409 "category has linked products"
  - 删 Product w/ Module → 409 "product has linked modules"
  - 删 Module w/ Feature → 409 "module has linked features"
  - 删 Feature (无下游) → 204
- [ ] 16.7 自动调用 `stdd-verify`
