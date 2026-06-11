# v0.0.13-product-restructure 切片执行计划

> 14 切片全 P0。依赖链严格：M02 必须先于 M04（编译依赖）；M05→M06→M07 链式；前端 M10-M13 依赖后端 API 形状（M06）定稿。

| # | 优先级 | TC 覆盖 | 实现目标 | 依赖 |
|---|--------|---------|---------|------|
| M01 | P0 | TC-LCLN-001..003 | `LegacyProductCategoryCleanup` (ApplicationRunner, DROP TABLE + DROP COLUMN, idempotent) + Test | 无 |
| M02 | P0 | — (编译态) | Product 去 categoryId：entity / 3 DTO / Service(去 categoryRepo+enrich+list param) / Controller | 无 |
| M03 | P0 | TC-PROD-001..010, TC-PERF-PROD-001 | Product 测试改写 + perf 预算 ≥2∧≤4 | M02 |
| M04 | P0 | — (删除态) | `git rm` com.rainier.productcategory 整包 (8 src + 5 test) | M02 |
| M05 | P0 | — (结构态) | ProductModule entity+parentId + uk(parent_id,code) + Repository 5 个新查询方法 | 无 |
| M06 | P0 | — (服务态) | ProductModuleService: depth walk + cross-product + cycle DFS + 双层 code 唯一 + path enrich batch + 双向 delete 409 + DTO/Controller 扩展 | M05, M09 |
| M07 | P0 | TC-PMOD-001..025 | ProductModule 测试 25 个（4 个测试类：Create/Query/Update(Reparent)/Delete） | M06 |
| M08 | P0 | TC-PERF-PMOD-001 | ProductModuleListSqlCountTest 调整 ≥4∧≤6 | M06 |
| M09 | P0 | — (配置态) | application.yml + application-test.yml 加 `rainier.product-module.depth.max: 3` | 无 |
| M10 | P0 | — (删除态) | 删前端 ProductCategory：pages/ + api + AppRoutes 路由 + AppLayout 4→3 项 | M04 |
| M11 | P0 | TC-FES-PROD-002 (回归) | Product 前端去 category：api/product.ts + ProductsPage + ProductEditDrawer + test 改写 | M03, M10 |
| M12 | P0 | TC-FES-PMOD-001..002 | ProductModule 前端：api 加 parentId/path + ProductModulesPage 树形 + EditDrawer 重写 cascade + 2 测试 | M06, M10 |
| M13 | P0 | TC-FES-FEAT-001, TC-FES-PROD-001, TC-FES-PROD-003 | Feature drawer pathName + AppLayout.test 3 项断言 + AppRoutes grep guard | M10, M12 |
| M14 | P0 | TC-E2E-001..006 | docker compose 重建 + SHOW TABLES=16 + DESCRIBE×2 + curl chain 全链 | M01..M13 |

## 执行批次（拓扑序）

```
批次 1（可并行）: M01, M02, M05, M09
批次 2: M03 (← M02), M04 (← M02), M06 (← M05, M09)
批次 3: M07 (← M06), M08 (← M06), M10 (← M04)
批次 4: M11 (← M03, M10), M12 (← M06, M10)
批次 5: M13 (← M10, M12)
批次 6: M14 (← 全部)
```

## 隐藏陷阱备忘（from Phase 2 + 经验）

- **陷阱 A**: Java 8 — 禁 `Set.of` / `List.of` / 无参 `orElseThrow()`。
- **陷阱 B**: M02 必须先于 M04 — ProductService 仍依赖 ProductCategoryRepository 时删包编译炸。
- **陷阱 C**: 复合唯一约束对 NULL parentId 不生效（MySQL/H2 同）— 顶层 code 唯一走应用层 `existsByProductIdAndParentIdIsNullAndCode`。
- **陷阱 D**: ddl-auto=update 不 DROP 旧表/列 — LegacyCleanup 显式执行；H2 支持 `ALTER TABLE ... DROP COLUMN IF EXISTS`，MySQL 8.0.23 之前不支持 IF EXISTS — 用 information_schema 探测后再执行，保两端兼容。
- **陷阱 E**: reparent depth = 新 parent 深度 + 自己子树高度（TC-PMOD-016），不是只算 parent 链。
- **陷阱 F**: AppLayout.test 的「产品组 4 项」断言改 3 项；其它 nav 组断言不动。
- **陷阱 G**: perf 范围断言 `≥N ∧ ≤M` 防 statistics 假绿。
- **陷阱 H**: v0.0.12 A2 版 ProductModuleEditDrawer.test.tsx mock 的是 listProductCategories — 重写删干净。
- **陷阱 I**: cycle check 范围 = {自己} ∪ 子孙集；reparent 到祖先合法（TC-PMOD-013）。
- **陷阱 J**: pathName list enrich 必须 batch — 页内收集全部祖先 id 一次查；不可每行 walk。
- **陷阱 K**: H2 测试库的旧 `rainier_product_category` 表在 fresh H2 里本来就不存在 — LegacyCleanup 测试需先手工 CREATE 假表再验证 DROP 生效（TC-LCLN-001），否则只能测 idempotent 分支。
- **陷阱 L**: Hibernate 对实体加 `@Table(uniqueConstraints=...)` 后 ddl-auto=update 不会在已有表上补约束 — 测试断言唯一性时依赖应用层检查路径，不要断言 DB constraint violation 文本。
