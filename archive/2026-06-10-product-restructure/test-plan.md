# v0.0.13-product-restructure 测试方案与详细案例

> 版本：v0.0.13
> 创建日期：2026-06-10
> 对应 Phase 2 Spec：specs/entity-product/spec.md, specs/entity-product-module/spec.md, specs/frontend-scaffold/spec.md
> Baseline：v0.0.12-product / commit fdb82e0 / 265 backend + 49 frontend tests

## 一、测试策略

### 1.1 测试金字塔

- **集成层**（主战场）：Spring MockMvc + H2 in-memory，覆盖 4 个修改的 Controller（Product / ProductModule + 2 修改）+ 新增 LegacyCleanup。
- **单元层**（轻）：ProductModuleService 三大算法纯逻辑测试（depth walk / cross-product / cycle DFS）。
- **性能层**：ProductModule list batch enrich + pathName 拼接 ≤ 5 SQL/page。
- **前端组件层**：Vitest + RTL，覆盖 tree assembly + cascade drawer + path label。
- **E2E**：docker compose + curl chain，验证 16 张表 + 树形操作 + 删除链。

### 1.2 测试原则

- v0.0.12 测试中所有 `categoryId` 引用必须删除或迁移到等价的「无 category」断言。
- 树形场景至少覆盖 2 层（顶层 + 子）；深度上限 / 环 / 跨产品独立 case 覆盖。
- LegacyCleanup 测试必须既验证「DROP 已执行」也验证「重复跑不报错」(idempotent)。
- 性能测试使用范围断言 `≥ N ∧ ≤ M`（防 statistics 关闭假绿）。

### 1.3 已有测试资产（v0.0.12 baseline）

| 测试文件 | 用例数 | 类型 | v0.0.13 处理 |
|----------|--------|------|----------|
| backend `productcategory/**` (4 controller + 1 perf) | ~22 | 集成+perf | **全删** |
| backend `product/controller/Product*Test` | 22 | 集成 | **修改**（去 categoryId 部分） |
| backend `product/perf/ProductListSqlCountTest` | 1 | perf | 微改（无 category 富化） |
| backend `productmodule/controller/ProductModule*Test` | 22 | 集成 | **大幅扩展**（加 parent/depth/cycle/reparent） |
| backend `productmodule/perf/ProductModuleListSqlCountTest` | 1 | perf | 修改（path 拼接预算） |
| backend `feature/controller/Feature*Test` | 22 | 集成 | **不变** |
| backend `feature/perf/FeatureListSqlCountTest` | 1 | perf | **不变** |
| frontend `pages/Product/ProductsPage.test.tsx`（如有） / `ProductEditDrawer.test.tsx` | 1 | 组件 | 修改（去 Category select 测试） |
| frontend `pages/ProductCategory/*.test.tsx` | 1 | 组件 | **全删** |
| frontend `pages/ProductModule/ProductModuleEditDrawer.test.tsx` (v0.0.13 A2 新增) | 1 | 组件 | **重写**（parent select 而非 category） |
| frontend `pages/Feature/FeatureEditDrawer.test.tsx` | 1 | 组件 | 微改（path 显示） |
| frontend `components/AppLayout.test.tsx` (TC-FES-PROD-001) | 1 | 组件 | 修改（4→3 项产品组） |

## 二、详细测试案例

### 功能 1 — Schema Migration: ProductCategory 表 / 列清理

#### 案例 1.1 — LegacyProductCategoryCleanup 启动期清理

| 字段 | 内容 |
|------|------|
| **ID** | TC-LCLN-001 |
| **对应 Spec** | proposal.md C7 |
| **优先级** | P0 |
| **预置条件** | H2 DB 初始化时 v0.0.12 schema 含 `rainier_product_category` 表 + `rainier_product.category_id` 列 |
| **输入** | 应用启动触发 `LegacyProductCategoryCleanup.run()` |
| **预期结果** | `SHOW TABLES` 不含 `rainier_product_category`；`DESCRIBE rainier_product` 不含 `category_id` 列；启动日志含 "LegacyProductCategoryCleanup completed" |
| **当前状态** | ❌ |

#### 案例 1.2 — Cleanup idempotent（重跑不报错）

| 字段 | 内容 |
|------|------|
| **ID** | TC-LCLN-002 |
| **对应 Spec** | design.md Decision 1 |
| **优先级** | P0 |
| **预置条件** | 表已不存在 / 列已不存在 |
| **输入** | 再次执行 `LegacyProductCategoryCleanup.run()` |
| **预期结果** | 无异常；日志含 "skipped — already clean" 或不打印 |
| **当前状态** | ❌ |

#### 案例 1.3 — 表数 16 验证

| 字段 | 内容 |
|------|------|
| **ID** | TC-LCLN-003 |
| **对应 Spec** | proposal.md Success Criteria |
| **优先级** | P0 |
| **预置条件** | 应用启动 + Hibernate ddl-auto=update 完成 |
| **输入** | `SHOW TABLES` |
| **预期结果** | 结果集恰含 16 张表（v0.0.12 17 - 1 product_category）；列表 SHALL 含 `rainier_product`, `rainier_product_module`, `rainier_feature` |
| **当前状态** | ❌ |

### 功能 2 — Product（修改：去 categoryId）

#### 案例 2.1 — Create 无 categoryId 字段

| 字段 | 内容 |
|------|------|
| **ID** | TC-PROD-001 |
| **对应 Spec** | entity-product/spec.md → Scenario: 最小 payload + ownerName 富化 |
| **优先级** | P0 |
| **预置条件** | User id=1 |
| **输入** | `POST /api/products {"code":"PROD-PAY","name":"支付平台","ownerUserId":1}` |
| **预期结果** | 201；body.status="PLANNING"; ownerName="Alice"; body **不含** categoryId/categoryCode/categoryName |
| **当前状态** | ❌ |

#### 案例 2.2 — Create body 含 categoryId 静默忽略

| 字段 | 内容 |
|------|------|
| **ID** | TC-PROD-002 |
| **对应 Spec** | entity-product/spec.md → Scenario: 请求体含 categoryId 时静默忽略 |
| **优先级** | P0 |
| **预置条件** | User id=1 |
| **输入** | POST body 含 `"categoryId": 999`（废弃字段） |
| **预期结果** | 201；body 不含 categoryId |
| **当前状态** | ❌ |

#### 案例 2.3 — Create code 重复 → 409

| 字段 | 内容 |
|------|------|
| **ID** | TC-PROD-003 |
| **对应 Spec** | entity-product/spec.md → Scenario: code 重复 → 409 |
| **优先级** | P0 |
| **预置条件** | 已有 code="PROD-DUP" |
| **输入** | 再 POST 同 code |
| **预期结果** | 409；message 含 "code already exists" |
| **当前状态** | ❌ |

#### 案例 2.4 — Create 非法 status → 400

| 字段 | 内容 |
|------|------|
| **ID** | TC-PROD-004 |
| **对应 Spec** | entity-product/spec.md → Scenario: 非法 status → 400 |
| **优先级** | P0 |
| **输入** | POST body 含 status="UNKNOWN" |
| **预期结果** | 400；message 含 "invalid status" |
| **当前状态** | ❌ |

#### 案例 2.5 — Create 缺必填字段 → 400（无 categoryId 项）

| 字段 | 内容 |
|------|------|
| **ID** | TC-PROD-005 |
| **对应 Spec** | entity-product/spec.md → Scenario: 缺必填字段 → 400 |
| **优先级** | P0 |
| **输入** | POST body `{"code":"PROD-X"}` |
| **预期结果** | 400；fieldErrors 含 `"name"` / `"ownerUserId"`；**不含** `"categoryId"` |
| **当前状态** | ❌ |

#### 案例 2.6 — GET 详情字段集无 category 字段

| 字段 | 内容 |
|------|------|
| **ID** | TC-PROD-006 |
| **对应 Spec** | entity-product/spec.md → Scenario: GET 详情字段集 |
| **优先级** | P0 |
| **预置条件** | Product id=1 |
| **输入** | `GET /api/products/1` |
| **预期结果** | 200；body 含 `[id, code, name, description, status, ownerUserId, ownerName, ownerLoginName, ...audit]`；不含 categoryId/categoryCode/categoryName |
| **当前状态** | ❌ |

#### 案例 2.7 — List 端点 categoryId 参数静默忽略

| 字段 | 内容 |
|------|------|
| **ID** | TC-PROD-007 |
| **对应 Spec** | entity-product/spec.md → Scenario: list 端点请求含 categoryId 参数时静默忽略 |
| **优先级** | P0 |
| **预置条件** | 3 个 Product |
| **输入** | `GET /api/products?categoryId=1` |
| **预期结果** | 200; total=3（filter 失效但不报错） |
| **当前状态** | ❌ |

#### 案例 2.8 — Update status + owner

| 字段 | 内容 |
|------|------|
| **ID** | TC-PROD-008 |
| **对应 Spec** | entity-product/spec.md → Scenario: 更新 status + owner 转移 |
| **优先级** | P0 |
| **预置条件** | Product id=1, User id=2 |
| **输入** | PUT body 含 status="ACTIVE", ownerUserId=2 |
| **预期结果** | 200；status="ACTIVE"；ownerName 跟 User 2 |
| **当前状态** | ❌ |

#### 案例 2.9 — Delete 无引用 204

| 字段 | 内容 |
|------|------|
| **ID** | TC-PROD-009 |
| **对应 Spec** | entity-product/spec.md → Scenario: 无引用软删成功 |
| **优先级** | P0 |
| **输入** | DELETE Product id=1（无 Module） |
| **预期结果** | 204；后续 GET 404 |
| **当前状态** | ❌ |

#### 案例 2.10 — Delete 有 Module 409

| 字段 | 内容 |
|------|------|
| **ID** | TC-PROD-010 |
| **对应 Spec** | entity-product/spec.md → Scenario: 有 Module 引用 → 409 |
| **优先级** | P0 |
| **预置条件** | Product 1 有 1 个 Module |
| **输入** | DELETE Product 1 |
| **预期结果** | 409；message 含 "product has linked modules" |
| **当前状态** | ❌ |

### 功能 3 — ProductModule（大幅扩展：parent / depth / cycle / reparent）

#### 案例 3.1 — 顶层 Create（parentId null）

| 字段 | 内容 |
|------|------|
| **ID** | TC-PMOD-001 |
| **对应 Spec** | entity-product-module/spec.md → Scenario: 顶层 module 创建 |
| **优先级** | P0 |
| **预置条件** | Product 1, User 1 |
| **输入** | POST `{"code":"MOD-WALLET","name":"钱包","productId":1,"ownerUserId":1}` 不传 parentId |
| **预期结果** | 201；parentId=null；pathName="钱包"；pathCodes="MOD-WALLET"；status="PLANNING" |
| **当前状态** | ❌ |

#### 案例 3.2 — 子 Create（同 product 顶层下）

| 字段 | 内容 |
|------|------|
| **ID** | TC-PMOD-002 |
| **对应 Spec** | entity-product-module/spec.md → Scenario: 子 module 创建 |
| **优先级** | P0 |
| **预置条件** | Product 1 下顶层 M1 (id=10, code="MOD-WALLET", name="钱包") |
| **输入** | POST `{"code":"MOD-BALANCE","name":"余额","productId":1,"parentId":10,"ownerUserId":1}` |
| **预期结果** | 201；parentId=10；pathName="钱包 / 余额"；pathCodes="MOD-WALLET / MOD-BALANCE" |
| **当前状态** | ❌ |

#### 案例 3.3 — Create parentId 不存在 → 400

| 字段 | 内容 |
|------|------|
| **ID** | TC-PMOD-003 |
| **对应 Spec** | entity-product-module/spec.md → Scenario: parentId 不存在 → 400 |
| **优先级** | P0 |
| **输入** | POST body parentId=999 |
| **预期结果** | 400；message 含 "parent module not found" |
| **当前状态** | ❌ |

#### 案例 3.4 — Create productId 不存在 → 400

| 字段 | 内容 |
|------|------|
| **ID** | TC-PMOD-004 |
| **对应 Spec** | entity-product-module/spec.md → Scenario: productId 不存在 → 400 |
| **优先级** | P0 |
| **输入** | POST body productId=999 |
| **预期结果** | 400；message 含 "product not found" |
| **当前状态** | ❌ |

#### 案例 3.5 — Create 非法 status → 400

| 字段 | 内容 |
|------|------|
| **ID** | TC-PMOD-005 |
| **对应 Spec** | entity-product-module/spec.md → Scenario: 非法 status → 400 |
| **优先级** | P0 |
| **预期结果** | 400；message 含 "invalid status" |
| **当前状态** | ❌ |

#### 案例 3.6 — Create 缺必填 → 400

| 字段 | 内容 |
|------|------|
| **ID** | TC-PMOD-006 |
| **对应 Spec** | entity-product-module/spec.md → Scenario: 缺必填字段 → 400 |
| **优先级** | P0 |
| **预期结果** | 400；fieldErrors 含 name / productId / ownerUserId |
| **当前状态** | ❌ |

#### 案例 3.7 — 同 parent 下 code 重复 → 409

| 字段 | 内容 |
|------|------|
| **ID** | TC-PMOD-007 |
| **对应 Spec** | entity-product-module/spec.md → Scenario: 同 parent 下 code 重复 → 409 |
| **优先级** | P0 |
| **输入** | 在 parent=10 下重建同 code |
| **预期结果** | 409；message 含 "code already exists under parent" |
| **当前状态** | ❌ |

#### 案例 3.8 — 不同 parent 下 code 可重复

| 字段 | 内容 |
|------|------|
| **ID** | TC-PMOD-008 |
| **对应 Spec** | entity-product-module/spec.md → Scenario: 不同 parent 下 code 可重复 |
| **优先级** | P0 |
| **输入** | 在 parent=10 下创建 MOD-CFG；在 parent=20 下再创建 MOD-CFG |
| **预期结果** | 两次均 201 |
| **当前状态** | ❌ |

#### 案例 3.9 — 同 product 顶层 code 重复 → 409

| 字段 | 内容 |
|------|------|
| **ID** | TC-PMOD-009 |
| **对应 Spec** | entity-product-module/spec.md → Scenario: 同 product 顶层 code 重复 → 409 |
| **优先级** | P0 |
| **输入** | Product 1 顶层已有 MOD-TOP；再 POST 同 code 不传 parentId |
| **预期结果** | 409；message 含 "code already exists in top-level" |
| **当前状态** | ❌ |

#### 案例 3.10 — 跨产品父级 → 400

| 字段 | 内容 |
|------|------|
| **ID** | TC-PMOD-010 |
| **对应 Spec** | entity-product-module/spec.md → Scenario: parent 属另一 product → 400 |
| **优先级** | P0 |
| **预置条件** | Product P1, P2; Module M (id=20, productId=P2) |
| **输入** | POST body productId=P1, parentId=20 |
| **预期结果** | 400；message 含 "parent module must belong to the same product" |
| **当前状态** | ❌ |

#### 案例 3.11 — 创建第 4 层超 max → 400

| 字段 | 内容 |
|------|------|
| **ID** | TC-PMOD-011 |
| **对应 Spec** | entity-product-module/spec.md → Scenario: 创建第 4 层（超 max=3）→ 400 |
| **优先级** | P0 |
| **预置条件** | max=3; 链 L1→L2→L3 |
| **输入** | POST parentId=L3 |
| **预期结果** | 400；message 含 "max module depth exceeded: 4 > 3" |
| **当前状态** | ❌ |

#### 案例 3.12 — 创建第 3 层等于 max → 201

| 字段 | 内容 |
|------|------|
| **ID** | TC-PMOD-012 |
| **对应 Spec** | entity-product-module/spec.md → Scenario: 创建第 3 层（等于 max=3）→ 201 |
| **优先级** | P0 |
| **预置条件** | max=3; 链 L1→L2 |
| **输入** | POST parentId=L2 |
| **预期结果** | 201 |
| **当前状态** | ❌ |

#### 案例 3.13 — Update 同产品内 reparent 成功

| 字段 | 内容 |
|------|------|
| **ID** | TC-PMOD-013 |
| **对应 Spec** | entity-product-module/spec.md → Scenario: 同产品内 reparent 成功 |
| **优先级** | P0 |
| **预置条件** | Module M (id=30, parentId=10); M2 (id=20, parentId=null) 同 product |
| **输入** | PUT M body parentId=20 |
| **预期结果** | 200；parentId=20；pathName 重新拼 |
| **当前状态** | ❌ |

#### 案例 3.14 — Update reparent 到自身 → 400 cycle

| 字段 | 内容 |
|------|------|
| **ID** | TC-PMOD-014 |
| **对应 Spec** | entity-product-module/spec.md → Scenario: reparent 到自身 → 400 cycle |
| **优先级** | P0 |
| **输入** | PUT M id=30 body parentId=30 |
| **预期结果** | 400；message 含 "cycle detected" |
| **当前状态** | ❌ |

#### 案例 3.15 — Update reparent 到真子孙 → 400 cycle

| 字段 | 内容 |
|------|------|
| **ID** | TC-PMOD-015 |
| **对应 Spec** | entity-product-module/spec.md → Scenario: reparent 到自己的真子孙 → 400 cycle |
| **优先级** | P0 |
| **预置条件** | 树 A→B→C |
| **输入** | PUT A body parentId=C.id |
| **预期结果** | 400；message 含 "cycle detected" |
| **当前状态** | ❌ |

#### 案例 3.16 — Update reparent 后超 max → 400

| 字段 | 内容 |
|------|------|
| **ID** | TC-PMOD-016 |
| **对应 Spec** | entity-product-module/spec.md → Scenario: reparent 后总深度超 max → 400 |
| **优先级** | P0 |
| **预置条件** | max=3; A→B; X→Y |
| **输入** | PUT X body parentId=B (X 在 3 层，Y 在 4 层) |
| **预期结果** | 400；message 含 "max module depth exceeded" |
| **当前状态** | ❌ |

#### 案例 3.17 — Update reparent 到 null（移为顶层）

| 字段 | 内容 |
|------|------|
| **ID** | TC-PMOD-017 |
| **对应 Spec** | entity-product-module/spec.md → Scenario: reparent 到 null |
| **优先级** | P0 |
| **预置条件** | Module M (id=30, parentId=10) |
| **输入** | PUT body parentId=null |
| **预期结果** | 200；parentId=null；pathName 只含自己 |
| **当前状态** | ❌ |

#### 案例 3.18 — Update status + owner

| 字段 | 内容 |
|------|------|
| **ID** | TC-PMOD-018 |
| **对应 Spec** | derived from v0.0.12 update test，path 字段加 |
| **优先级** | P0 |
| **输入** | PUT M body status="ACTIVE", ownerUserId=2 |
| **预期结果** | 200；status="ACTIVE"；ownerName 跟 User 2 |
| **当前状态** | ❌ |

#### 案例 3.19 — GET 详情字段集（parent + path）

| 字段 | 内容 |
|------|------|
| **ID** | TC-PMOD-019 |
| **对应 Spec** | entity-product-module/spec.md → Scenario: GET 详情含 parent 与 path 字段 |
| **优先级** | P0 |
| **预置条件** | Module L2 (id=20, parentId=10, productId=1) |
| **输入** | GET /api/product-modules/20 |
| **预期结果** | 200；含 [id, code, name, status, productId, productCode, productName, parentId, parentCode, parentName, pathName, pathCodes, ownerUserId, ownerName, ownerLoginName, ...audit]；pathName 含两段 |
| **当前状态** | ❌ |

#### 案例 3.20 — 按 parentId 过滤

| 字段 | 内容 |
|------|------|
| **ID** | TC-PMOD-020 |
| **对应 Spec** | entity-product-module/spec.md → Scenario: 按 parentId 过滤列表 |
| **优先级** | P0 |
| **预置条件** | M1 顶层；M2, M3 parentId=M1.id；M4 parentId=null |
| **输入** | GET ?parentId=M1.id |
| **预期结果** | total=2；content[*].parentId 全为 M1.id |
| **当前状态** | ❌ |

#### 案例 3.21 — 按 productId 过滤（全树）

| 字段 | 内容 |
|------|------|
| **ID** | TC-PMOD-021 |
| **对应 Spec** | entity-product-module/spec.md → Scenario: 按 productId 过滤列表（含全树） |
| **优先级** | P0 |
| **预置条件** | Product A 下 5 module (含子孙); Product B 下 1 |
| **输入** | GET ?productId=A.id |
| **预期结果** | total=5 |
| **当前状态** | ❌ |

#### 案例 3.22 — Delete 无引用 204

| 字段 | 内容 |
|------|------|
| **ID** | TC-PMOD-022 |
| **对应 Spec** | entity-product-module/spec.md → Scenario: 无引用软删成功 |
| **优先级** | P0 |
| **预期结果** | 204；后续 GET 404 |
| **当前状态** | ❌ |

#### 案例 3.23 — Delete 有 Feature → 409

| 字段 | 内容 |
|------|------|
| **ID** | TC-PMOD-023 |
| **对应 Spec** | entity-product-module/spec.md → Scenario: 有 Feature 引用 → 409 |
| **优先级** | P0 |
| **预期结果** | 409；message 含 "module has linked features" |
| **当前状态** | ❌ |

#### 案例 3.24 — Delete 有子 Module → 409

| 字段 | 内容 |
|------|------|
| **ID** | TC-PMOD-024 |
| **对应 Spec** | entity-product-module/spec.md → Scenario: 有子 Module → 409 |
| **优先级** | P0 |
| **预期结果** | 409；message 含 "module has linked sub-modules" |
| **当前状态** | ❌ |

#### 案例 3.25 — Delete 同时有 Feature 和子 Module（首报 Feature）

| 字段 | 内容 |
|------|------|
| **ID** | TC-PMOD-025 |
| **对应 Spec** | entity-product-module/spec.md → Scenario: 同时有 Feature 和子 Module（首先报 Feature） |
| **优先级** | P0 |
| **预期结果** | 409；message 含 "module has linked features"（先检查 Feature） |
| **当前状态** | ❌ |

### 功能 4 — Feature（不变）

#### 案例 4.x — 沿用 v0.0.12 TC-FEAT-001..011

11 个 case 保持不变（CRUD + FK 保护），无修改。

### 功能 5 — 性能（perf）

#### 案例 5.1 — Product list SQL count 范围

| 字段 | 内容 |
|------|------|
| **ID** | TC-PERF-PROD-001 |
| **对应 Spec** | design.md performance budget |
| **优先级** | P0 |
| **预置条件** | 20 Product |
| **输入** | `GET /api/products?size=20` 用 Hibernate Statistics |
| **预期结果** | SQL 次数 ≥ 2 ∧ ≤ 4（去 category 富化后） |
| **当前状态** | ❌（v0.0.12 是 ≥3 ∧ ≤5，本版降一） |

#### 案例 5.2 — ProductModule list SQL count 范围

| 字段 | 内容 |
|------|------|
| **ID** | TC-PERF-PMOD-001 |
| **对应 Spec** | design.md performance budget |
| **优先级** | P0 |
| **预置条件** | 20 Module 含 mix 顶层/子 |
| **输入** | `GET /api/product-modules?size=20` |
| **预期结果** | SQL 次数 ≥ 4 ∧ ≤ 6（user + product + parent batch + path build batch） |
| **当前状态** | ❌ |

#### 案例 5.3 — Feature list SQL count 范围（不变）

| 字段 | 内容 |
|------|------|
| **ID** | TC-PERF-FEAT-001 |
| **对应 Spec** | v0.0.12 沿用 |
| **优先级** | P0 |
| **预期结果** | SQL 次数 ≥ 4 ∧ ≤ 5 |
| **当前状态** | ✅ |

### 功能 6 — Frontend Sider + Routes + Tree

#### 案例 6.1 — Sider 顶级 4 组 + 产品组 3 项

| 字段 | 内容 |
|------|------|
| **ID** | TC-FES-PROD-001 |
| **对应 Spec** | frontend-scaffold/spec.md → Sider 顶级菜单组「产品」 |
| **优先级** | P0 |
| **预置条件** | 已登录 |
| **输入** | render AppLayout |
| **预期结果** | 4 顶级；「产品」组含 3 项（产品/产品模块/功能）；**不含**「产品分类」 |
| **当前状态** | ❌（v0.0.12 测试 4 项需改） |

#### 案例 6.2 — /pm/products 路由（grep guard 不变）

| 字段 | 内容 |
|------|------|
| **ID** | TC-FES-PROD-002 |
| **对应 Spec** | frontend-scaffold/spec.md → /pm/products 路由直接访问 |
| **优先级** | P0 |
| **预期结果** | render ProductsPage；grep ≥1 |
| **当前状态** | ✅（沿用） |

#### 案例 6.3 — /pm/product-categories 路由已删

| 字段 | 内容 |
|------|------|
| **ID** | TC-FES-PROD-003 |
| **对应 Spec** | frontend-scaffold/spec.md → /pm/product-categories 路由已删除 |
| **优先级** | P0 |
| **预期结果** | 不 render ProductCategoriesPage；grep = 0 |
| **当前状态** | ❌ |

#### 案例 6.4 — ProductModulesPage 树形渲染含 ≥ 2 层缩进

| 字段 | 内容 |
|------|------|
| **ID** | TC-FES-PMOD-001 |
| **对应 Spec** | frontend-scaffold/spec.md → ProductModulesPage 树形列表显示 |
| **优先级** | P0 |
| **预置条件** | mock 返回 M1 (parentId=null) → M2 (parentId=1) → M3 (parentId=2) |
| **输入** | render ProductModulesPage |
| **预期结果** | 嵌套 UL/LI；M2 在 M1 的 `<ul>` 内；M3 在 M2 内 |
| **当前状态** | ❌ |

#### 案例 6.5 — ProductModuleEditDrawer Product 切换 → parent 候选刷新

| 字段 | 内容 |
|------|------|
| **ID** | TC-FES-PMOD-002 |
| **对应 Spec** | frontend-scaffold/spec.md → ProductModuleEditDrawer Product + parentModule 二级 cascade |
| **优先级** | P0 |
| **预置条件** | mock listProductModules({productId}) 按 productId 过滤 |
| **输入** | 切 Product A → B |
| **预期结果** | parent 下拉清空；listProductModules({productId:B}) 被调；下拉显示 B 的 2 个模块 |
| **当前状态** | ❌ |

#### 案例 6.6 — FeatureEditDrawer 模块下拉显示 pathName

| 字段 | 内容 |
|------|------|
| **ID** | TC-FES-FEAT-001 |
| **对应 Spec** | frontend-scaffold/spec.md → FeatureEditDrawer 模块下拉显示父链 pathName |
| **优先级** | P0 |
| **预置条件** | mock listProductModules 返回 M1（pathName="钱包"）+ M2（pathName="钱包 / 余额"） |
| **输入** | render FeatureEditDrawer + select Product 1 |
| **预期结果** | 模块下拉含 2 项；M1 文本含 "钱包"；M2 文本含 "钱包 / 余额" |
| **当前状态** | ❌ |

### 功能 7 — E2E (docker compose + curl chain)

#### 案例 7.1 — 启动后 SHOW TABLES = 16

| 字段 | 内容 |
|------|------|
| **ID** | TC-E2E-001 |
| **对应 Spec** | proposal.md Success Criteria |
| **优先级** | P0 |
| **输入** | docker-compose up + 等待健康 + 连 MySQL `SHOW TABLES` |
| **预期结果** | 16 张表；不含 rainier_product_category |
| **当前状态** | ❌ |

#### 案例 7.2 — DESCRIBE rainier_product 无 category_id 列

| 字段 | 内容 |
|------|------|
| **ID** | TC-E2E-002 |
| **对应 Spec** | proposal.md Success Criteria |
| **优先级** | P0 |
| **预期结果** | `DESCRIBE rainier_product` 输出不含 `category_id` |
| **当前状态** | ❌ |

#### 案例 7.3 — DESCRIBE rainier_product_module 含 parent_id 列

| 字段 | 内容 |
|------|------|
| **ID** | TC-E2E-003 |
| **对应 Spec** | proposal.md Success Criteria |
| **优先级** | P0 |
| **预期结果** | `DESCRIBE rainier_product_module` 含 `parent_id BIGINT NULL` |
| **当前状态** | ❌ |

#### 案例 7.4 — curl chain 建树 + 树路径富化

| 字段 | 内容 |
|------|------|
| **ID** | TC-E2E-004 |
| **对应 Spec** | proposal.md Success Criteria |
| **优先级** | P0 |
| **输入** | curl POST product → top module A → 子 module A.1 → A.1 下 Feature；每步 GET 详情 |
| **预期结果** | A.1.pathName="A / A.1"；Feature.modulePath="A / A.1"（如前端显示用） |
| **当前状态** | ❌ |

#### 案例 7.5 — curl chain 拒绝场景三连

| 字段 | 内容 |
|------|------|
| **ID** | TC-E2E-005 |
| **对应 Spec** | proposal.md Success Criteria |
| **优先级** | P0 |
| **输入** | curl 试图：(a) 跨产品 reparent → 400；(b) reparent 到自身 → 400 cycle；(c) 创建超 max 层 → 400 depth |
| **预期结果** | 三个独立 400 响应，message 各对 |
| **当前状态** | ❌ |

#### 案例 7.6 — curl chain 删除 409 双链

| 字段 | 内容 |
|------|------|
| **ID** | TC-E2E-006 |
| **对应 Spec** | proposal.md Success Criteria |
| **优先级** | P0 |
| **输入** | curl 试图删 (a) 有 Feature 的 Module → 409；(b) 有子 Module 的父 Module → 409；(c) 删空 Feature 后 204；(d) 删空 子 module 后再删父 204 |
| **预期结果** | 4 个独立结果，409 message 区分明确 |
| **当前状态** | ❌ |

---

## 三、测试执行矩阵

| 功能模块 | 单元 | 集成 | Perf | E2E | 状态 |
|----------|------|------|------|-----|------|
| Schema Migration | — | TC-LCLN-001..003 (3) | — | TC-E2E-001..003 (3) | 🟢 |
| Product 修改 | — | TC-PROD-001..010 (10) | TC-PERF-PROD-001 (1) | E2E 间接 | 🟢 |
| ProductModule 重构 | — | TC-PMOD-001..025 (25) | TC-PERF-PMOD-001 (1) | TC-E2E-004,005,006 | 🟢 |
| Feature 不变 | — | TC-FEAT-001..011 (11) | TC-PERF-FEAT-001 (1) | TC-E2E-004 | 🟢 |
| Frontend Sider/Routes/Tree | — | TC-FES-PROD-001..003 + PMOD-001..002 + FEAT-001 (6) | — | manual | 🟢 |

**TC 总数**：3 + 10 + 25 + 11 + 1 + 1 + 1 + 6 + 6 (E2E) = **64 P0**（vs v0.0.12 之 55 P0）

## 四、回归风险矩阵

| 风险区域 | v0.0.13 改动 | 已有回归保护 | 风险等级 |
|----------|---------------|-------------|---------|
| LegacyCleanup DROP 不可逆 | bootstrap 启动期执行 | TC-LCLN-001..003 启动后断言 schema 变化；E2E `SHOW TABLES` 双重确认 | 🟡 |
| Product 实体字段动（remove categoryId） | 实体 + 4 DTO + Service + Controller + 5 test | 22 既有 ProductController test → 改写 10 个核心 case；E2E 字段集断言 | 🟡 |
| ProductModule 加 parent 自 FK 系列 | 实体 + DTO + Service 三大算法 + Controller + Repository | 25 新 TC 覆盖 parent CRUD + 三检 + reparent + path 富化 + 双向删除 | 🔴 |
| Composite UQ 与应用层顶层 code 唯一冲突 | DB UQ + 应用层 check 并存 | TC-PMOD-007/008/009 三个 case 覆盖；E2E 间接验证 | 🟡 |
| 环检测 DFS 性能 | reparent 时扫子树 | 单测正常用例；perf 未直测但 max=3 树小不会爆 | 🟢 |
| 前端 ProductModulesPage 树形渲染 | 全新组件 | TC-FES-PMOD-001 snapshot；可补 perf 测试 | 🟡 |
| Sider 4→3 项可能引发 v0.0.11/12 其他测试 | AppLayout.test.tsx 改 | TC-FES-PROD-001 直接覆盖；grep "4 项" 校对 | 🟡 |
| 前端 Product / ProductCategory 路由删除 | AppRoutes.tsx 删 2 处 | TC-FES-PROD-003 grep guard | 🟢 |
| Feature 间接受影响（pathName 显示） | 仅 EditDrawer drawer 改 1 行 | TC-FES-FEAT-001 | 🟢 |
| 既有 v0.0.10/11/12 sprint/story/task 等 | 无 | 不变 backend 211 + frontend ~40 测试保护 | 🟢 |

**总评**：🔴 高: 1（ProductModule 重构主轴）/ 🟡 中: 5 / 🟢 低: 4

## 五、建议补充顺序

1. **第一优先**（部署前必补）：
   - TC-LCLN-001..003（schema migration 启动保护）
   - TC-PROD-001..010（Product 修改回归）
   - TC-PMOD-001..025（ProductModule 全新功能）
   - TC-PERF-PROD-001 / PMOD-001（性能 budget）
   - TC-FES-PROD-001..003 + PMOD-001..002 + FEAT-001（6 个前端）
   - TC-E2E-001..006（E2E 6 个）

2. **第二优先**（部署后尽快补）：无（本版 P0 已全覆盖）

3. **第三优先**（后续补）：
   - ProductModule reparent 并发 race condition（v0.0.14+ 加 optimistic lock）
   - 拖拽 reparent UI 测试（v0.0.14 UI 加后补）
   - 深度配置改大时已有 module 不超新限的启动检查（v0.0.14+）
