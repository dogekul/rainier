# v0.0.13-product-restructure 任务清单

## 1. Schema Migration（P0）

- [x] 1.1 `LegacyProductCategoryCleanup` ApplicationRunner 组件（DROP TABLE rainier_product_category + ALTER TABLE rainier_product DROP COLUMN category_id，information_schema 探测保 H2/MySQL 兼容，idempotent）
- [x] 1.2 `LegacyProductCategoryCleanupTest`（TC-LCLN-001 假表 DROP 验证 / 002 idempotent / 003 表数 16）（依赖 #1.1）

## 2. entity-product 去 categoryId（P0）

- [x] 2.1 `Product` entity 删 categoryId 字段
- [x] 2.2 `ProductCreateRequest` / `ProductUpdateRequest` / `ProductDetail` 删 category 相关字段（依赖 #2.1）
- [x] 2.3 `ProductRepository` 清理（countByCategoryId 等只服务于 ProductCategoryService 的方法）（依赖 #2.1）
- [x] 2.4 `ProductService` 去 ProductCategoryRepository 依赖 + enrich category 部分 + list categoryId 参数（依赖 #2.2, #2.3）
- [x] 2.5 `ProductController` 去 categoryId query param（依赖 #2.4）
- [x] 2.6 Product 4 个测试类改写 TC-PROD-001..010（依赖 #2.5）
- [x] 2.7 `ProductListSqlCountTest` 预算 ≥2∧≤4 TC-PERF-PROD-001（依赖 #2.4）

## 3. 删除 entity-product-category（P0）

- [x] 3.1 `git rm -r backend/src/main/java/com/rainier/productcategory backend/src/test/java/com/rainier/productcategory`（依赖 #2.4 编译通过后）
- [x] 3.2 全局 grep 校验后端无 productcategory 引用残留（依赖 #3.1）

## 4. entity-product-module 树化（P0）

- [x] 4.1 `application.yml` + `application-test.yml`（如有）加 `rainier.product-module.depth.max: 3`
- [x] 4.2 `ProductModule` entity 加 parentId 字段 + `@Table uniqueConstraints uk_product_module_parent_code`
- [x] 4.3 `ProductModuleRepository` 加 countByParentId / findByParentId / findAllByParentIdIn / existsByProductIdAndParentIdIsNullAndCode / existsByParentIdAndCode（依赖 #4.2）
- [x] 4.4 `ProductModuleCreateRequest` / `ProductModuleUpdateRequest` 加 parentId；`ProductModuleDetail` 加 parentId/parentCode/parentName/pathName/pathCodes（依赖 #4.2）
- [x] 4.5 `ProductModuleService`：walkParentChainDepth（@Value 注入 max）+ crossProductReject + cycleDfsCheck + 双层 code 唯一 + create/update 接 parentId + reparent 三检顺序 cross→cycle→depth（依赖 #4.3, #4.4, #4.1）
- [x] 4.6 `ProductModuleService` enrich：parent 富化 + pathName/pathCodes 拼接；list 端点 batch 路径查询 ≤6 SQL（依赖 #4.5）
- [x] 4.7 `ProductModuleService.delete` 双向 409：先 Feature 后子 Module（依赖 #4.5）
- [x] 4.8 `ProductModuleController` 加 parentId query param（依赖 #4.5）
- [x] 4.9 ProductModule 测试 TC-PMOD-001..025（4 测试类：Create 12 / Query 3 / Update-Reparent 6 / Delete 4）（依赖 #4.8）
- [x] 4.10 `ProductModuleListSqlCountTest` 调整 ≥4∧≤6 TC-PERF-PMOD-001（依赖 #4.6）

## 5. 前端删 ProductCategory + Sider（P0）

- [x] 5.1 `git rm -r frontend/src/pages/ProductCategory frontend/src/api/productCategory.ts`（依赖 #3.1）
- [x] 5.2 `AppRoutes.tsx` 删 import + `/pm/product-categories` 路由（依赖 #5.1）
- [x] 5.3 `AppLayout.tsx` 产品组 4→3 项（依赖 #5.1)

## 6. 前端 Product 去 category（P0）

- [x] 6.1 `api/product.ts` 删 categoryId/categoryCode/categoryName + ProductListParams.categoryId（依赖 #5.1）
- [x] 6.2 `ProductsPage` 删 Category 列 / filter（依赖 #6.1）
- [x] 6.3 `ProductEditDrawer` 删 Category select + listProductCategories import（依赖 #6.1）
- [x] 6.4 `ProductEditDrawer.test.tsx` 改写（去 category mock 保 TC 断言）（依赖 #6.3）

## 7. 前端 ProductModule 树形（P0）

- [x] 7.1 `api/productModule.ts` 加 parentId/pathName/pathCodes + ProductModuleListParams.parentId（依赖 #4.8）
- [x] 7.2 `ProductModulesPage` 树形渲染：buildTree + 递归 UL + depth indent + 保留 search/status filter（依赖 #7.1）
- [x] 7.3 `ProductModuleEditDrawer` 重写：Product → parentModule cascade（server-side filter；删 listProductCategories 残留 — 陷阱 H）（依赖 #7.1）
- [x] 7.4 `ProductModulesPage.test.tsx` 新增树形断言 TC-FES-PMOD-001（依赖 #7.2）
- [x] 7.5 `ProductModuleEditDrawer.test.tsx` 重写 TC-FES-PMOD-002（依赖 #7.3）

## 8. 前端 Feature + AppLayout 测试（P0）

- [x] 8.1 `FeatureEditDrawer` module 下拉 label 用 pathName fallback name（依赖 #7.1）
- [x] 8.2 `FeatureEditDrawer.test.tsx` 加 pathName 断言 TC-FES-FEAT-001（依赖 #8.1）
- [x] 8.3 `AppLayout.test.tsx` TC-FES-PROD-001 改 3 项 + 无产品分类断言（依赖 #5.3）
- [x] 8.4 AppRoutes grep guard 测试 TC-FES-PROD-003（/pm/product-categories grep = 0）（依赖 #5.2）

## 9. 测试与验证（P0）

- [x] 9.1 全量 backend `mvn test` 通过（预期 ≈ 271）
- [x] 9.2 全量 frontend `npx vitest run` 通过（预期 ≈ 53）
- [x] 9.3 E2E：docker compose 重建 + SHOW TABLES = 16 + DESCRIBE rainier_product 无 category_id + DESCRIBE rainier_product_module 有 parent_id（TC-E2E-001..003）
- [x] 9.4 E2E curl chain：建链 + pathName 验证 + 3 个 400 拒绝 + 双 409 删除链 + 倒序清理 204（TC-E2E-004..006）
