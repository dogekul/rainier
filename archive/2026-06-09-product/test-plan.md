# v0.0.12-product 测试方案与详细案例

> 版本：v0.0.12-product
> 创建日期：2026-06-09
> 对应 Phase 2 Spec：`specs/entity-product-category/spec.md` / `specs/entity-product/spec.md` / `specs/entity-product-module/spec.md` / `specs/entity-feature/spec.md` / `specs/frontend-scaffold/spec.md`
> 基线：tag `v0.0.11-task` / commit `31c9721`（212 backend / 44 frontend tests）

## 一、测试策略

### 1.1 测试金字塔

- **单元/集成**：~55 新增 backend P0 + 5 frontend P0 = **~60 P0 TCs**
  - 每实体 12 TCs (Create 8 + Query 2 + Update 1 + Delete 1) × 4 = 48
  - Perf (list 锁 ≤5 statements) × 4 = 4
  - Frontend: Sider 1 + 路由 grep 1 + cascading 2 = 4

### 1.2 测试原则

- 沿用 v0.0.11 fixture 模式（cleanDb dependency 顺序 + 注入 5 个 repo + ObjectMapper）
- Perf 测试用 Hibernate Statistics + 范围断言 `≤ 5 AND ≥ 3`（PA-1 教训：等号锁死不留余地）
- 4 entities 模板化 fixture helpers — `createCategory`/`createProduct`/`createModule`/`createFeature`
- 错误 message 精确文案 startWith 断言

### 1.3 已有测试资产（v0.0.11 baseline）

| 测试文件 | 用例数 | 类型 | 本次相关性 |
|----------|--------|------|------------|
| `TaskControllerCreateTest.java` (v0.0.11) | 12 | 集成 | 模板 — 12 TC 模式可平行复制至 4 entities |
| `SprintListSqlCountTest.java` (v0.0.10.1) | 1 | perf | 模板 — Hibernate Statistics 模式 |
| `ProjectControllerDeleteTest.java` (v0.0.8 / 0.0.10 / 0.0.11) | 5 | 集成 | FK chain 模式参考 |
| `AppLayout.test.tsx` (v0.0.11) | 4 | vitest | Sider 5 项已就位；新加 5 顶级组 + 产品 4 项断言 |

## 二、详细测试案例

> 由于 4 entities 高度同构，下表压缩展示。完整 TC 内容详见各 spec.md 的 Scenario 描述。

### 功能 1 — ProductCategory CRUD (entity-product-category)

| TC-ID | 优先级 | 描述 | 关键断言 | 当前状态 |
|---|---|---|---|---|
| TC-PCAT-001 | P0 | 最小 payload 创建 + 默认 status=ACTIVE + ownerName 富化 | 201, status=ACTIVE, ownerName=Alice | ❌ 新增 |
| TC-PCAT-002 | P0 | code 重复 → 409 | message startsWith "code already exists" | ❌ |
| TC-PCAT-003 | P0 | ownerUserId 不存在 → 400 | message startsWith "owner user not found" | ❌ |
| TC-PCAT-004 | P0 | 非法 status → 400 | message startsWith "invalid status" | ❌ |
| TC-PCAT-005 | P0 | 缺必填字段 → 400 fieldErrors | fieldErrors 含 name + ownerUserId | ❌ |
| TC-PCAT-006 | P0 | createBy 自动注入 | body.createBy 存在 | ❌ |
| TC-PCAT-007 | P0 | GET 详情完整字段集 | loop assert 12 字段 | ❌ |
| TC-PCAT-008 | P0 | 按 status 过滤列表 | total=2, all status=ACTIVE | ❌ |
| TC-PCAT-009 | P0 | PUT 更新 status + owner 转移 + 富化跟随 | body.status, body.ownerName | ❌ |
| TC-PCAT-010 | P0 | PUT 新 owner 不存在 → 400 | message startsWith "owner user not found" | ❌ |
| TC-PCAT-011 | P0 | 无引用软删 → 204 + 后续 GET 404 | 204; 404 | ❌ |
| TC-PCAT-012 | P0 | 有 Product 引用 → 409 | message startsWith "category has linked products" | ❌ |

### 功能 2 — Product CRUD (entity-product)

| TC-ID | 优先级 | 描述 | 关键断言 | 当前状态 |
|---|---|---|---|---|
| TC-PROD-001 | P0 | 最小 payload + categoryName 富化 + 默认 status=PLANNING | 201, status=PLANNING, categoryName | ❌ |
| TC-PROD-002 | P0 | categoryId 不存在 → 400 | message startsWith "category not found" | ❌ |
| TC-PROD-003 | P0 | code 重复 → 409 | message startsWith "code already exists" | ❌ |
| TC-PROD-004 | P0 | 非法 status → 400 (4 状态外) | message startsWith "invalid status" | ❌ |
| TC-PROD-005 | P0 | 缺必填字段 → 400 fieldErrors | fieldErrors 含 name+categoryId+ownerUserId | ❌ |
| TC-PROD-006 | P0 | createBy 自动注入 | body.createBy 存在 | ❌ |
| TC-PROD-007 | P0 | GET 详情完整字段集 (15 字段含 categoryCode/categoryName) | loop assert | ❌ |
| TC-PROD-008 | P0 | 按 categoryId 过滤列表 | total / categoryId 一致 | ❌ |
| TC-PROD-009 | P0 | PUT 更新 status + owner 转移 | body.status="ACTIVE", body.ownerName 跟随 | ❌ |
| TC-PROD-010 | P0 | PUT 不接受 categoryId 字段（静默丢弃，service 不改） | DB.categoryId 不变 | ❌ |
| TC-PROD-011 | P0 | 无引用软删 → 204 | 204; 404 | ❌ |
| TC-PROD-012 | P0 | 有 Module 引用 → 409 | message startsWith "product has linked modules" | ❌ |

### 功能 3 — ProductModule CRUD (entity-product-module)

| TC-ID | 优先级 | 描述 | 关键断言 | 当前状态 |
|---|---|---|---|---|
| TC-PMOD-001 | P0 | 最小 payload + productName 富化 | 201, productName | ❌ |
| TC-PMOD-002 | P0 | productId 不存在 → 400 | message startsWith "product not found" | ❌ |
| TC-PMOD-003 | P0 | code 重复 → 409 | message startsWith "code already exists" | ❌ |
| TC-PMOD-004 | P0 | 非法 status (3 状态外) → 400 | message startsWith "invalid status" | ❌ |
| TC-PMOD-005 | P0 | 缺必填字段 → 400 fieldErrors | fieldErrors 含 name+productId+ownerUserId | ❌ |
| TC-PMOD-006 | P0 | createBy 自动注入 | body.createBy 存在 | ❌ |
| TC-PMOD-007 | P0 | GET 详情完整字段集 (15 字段) | loop assert | ❌ |
| TC-PMOD-008 | P0 | 按 productId 过滤列表 | total / productId 一致 | ❌ |
| TC-PMOD-009 | P0 | PUT 更新 status + owner | body.status, body.ownerName | ❌ |
| TC-PMOD-010 | P0 | PUT 不接受 productId 字段 | DB.productId 不变 | ❌ |
| TC-PMOD-011 | P0 | 无引用软删 → 204 | 204; 404 | ❌ |
| TC-PMOD-012 | P0 | 有 Feature 引用 → 409 | message startsWith "module has linked features" | ❌ |

### 功能 4 — Feature CRUD (entity-feature)

| TC-ID | 优先级 | 描述 | 关键断言 | 当前状态 |
|---|---|---|---|---|
| TC-FEAT-001 | P0 | 最小 payload + moduleName 富化 | 201, moduleName | ❌ |
| TC-FEAT-002 | P0 | moduleId 不存在 → 400 | message startsWith "module not found" | ❌ |
| TC-FEAT-003 | P0 | code 重复 → 409 | message startsWith "code already exists" | ❌ |
| TC-FEAT-004 | P0 | 非法 status → 400 | message startsWith "invalid status" | ❌ |
| TC-FEAT-005 | P0 | 缺必填字段 → 400 fieldErrors | fieldErrors 含 name+moduleId+ownerUserId | ❌ |
| TC-FEAT-006 | P0 | createBy 自动注入 | body.createBy 存在 | ❌ |
| TC-FEAT-007 | P0 | GET 详情完整字段集 (15 字段) | loop assert | ❌ |
| TC-FEAT-008 | P0 | 按 moduleId 过滤列表 | total / moduleId 一致 | ❌ |
| TC-FEAT-009 | P0 | PUT 更新 status + owner | body.status, body.ownerName | ❌ |
| TC-FEAT-010 | P0 | PUT 不接受 moduleId 字段 | DB.moduleId 不变 | ❌ |
| TC-FEAT-011 | P0 | 软删 → 204 + 后续 GET 404（无下游 FK） | 204; 404 | ❌ |

### 功能 5 — Perf SQL count guards

| TC-ID | 优先级 | 描述 | 关键断言 | 当前状态 |
|---|---|---|---|---|
| TC-PERF-PCAT-001 | P0 | size=20 list Cat → stmtCount ≤ 5 ∧ ≥ 3（无 parent 时 ≥ 3） | range assert | ❌ |
| TC-PERF-PROD-001 | P0 | size=20 list Product → ≤ 5 ∧ ≥ 4 | range assert | ❌ |
| TC-PERF-PMOD-001 | P0 | size=20 list Module → ≤ 5 ∧ ≥ 4 | range assert | ❌ |
| TC-PERF-FEAT-001 | P0 | size=20 list Feature → ≤ 5 ∧ ≥ 4 | range assert | ❌ |

### 功能 6 — Frontend Sider + Cascading (frontend-scaffold)

| TC-ID | 优先级 | 描述 | 关键断言 | 当前状态 |
|---|---|---|---|---|
| TC-FES-PROD-001 | P0 | Sider 顶级 5 组 + 产品组 4 项 + 顺序 | getByText 5 顶级，4 产品 项；序断言 | ❌ |
| TC-FES-PROD-002 | P0 | /pm/products 路由直接访问 + grep guard | renders ProductsPage; grep AppRoutes ≥ 1 | ❌ |
| TC-FES-PROD-003 | P0 | ProductEditDrawer Category select 设值正确 | body.categoryId == selected | ❌ |
| TC-FES-PROD-004 | P0 | FeatureEditDrawer 切 Product 后 Module 选项过滤 | Module options only `productId === selected` | ❌ |

## 三、测试执行矩阵

| 功能模块 | 单元 | 集成 | Perf | E2E | 状态 |
|----------|------|------|------|-----|------|
| ProductCategory CRUD | — | TC-PCAT-001..012 (12) | TC-PERF-PCAT-001 (1) | curl chain | 🟢 |
| Product CRUD | — | TC-PROD-001..012 (12) | TC-PERF-PROD-001 (1) | curl chain | 🟢 |
| ProductModule CRUD | — | TC-PMOD-001..012 (12) | TC-PERF-PMOD-001 (1) | curl chain | 🟢 |
| Feature CRUD | — | TC-FEAT-001..011 (11) | TC-PERF-FEAT-001 (1) | curl chain | 🟢 |
| Frontend Sider + Routes + Cascading | — | TC-FES-PROD-001..004 (4) | — | manual | 🟢 |

## 四、回归风险矩阵

| 风险区域 | v0.0.12 改动 | 已有回归保护 | 风险等级 |
|----------|---------------|-------------|---------|
| 4 张新表 ddl-auto=update | 自动建 | 4 × CRUD 测试 + E2E SHOW TABLES | 🟢 |
| Sider 顶级 4 → 5 组 + 路由 + 已有 v0.0.11 「4 项 Sider」 测试可能破坏 | 改 AppLayout + AppRoutes | 同步更新 v0.0.11 AppLayout.test 改为 5 顶级；新加 TC-FES-PROD-001 | 🟡 |
| 4 entity service 跨 entity 共享 batch enrich pattern bug 时影响全部 | enrich 模式复用 | 4 perf TCs + 4 list 详细字段断言 | 🟢 |
| FK chain message 4 处一致性 | 统一 "X has linked Ys" | 4 × delete-with-children TC | 🟢 |
| TaskEditDrawer / SprintEditDrawer 等既有 cascading drawer 不受 v0.0.12 影响 | 仅新增 4 个 Drawer | v0.0.11 既有 TC-FES-TSK-003 等不动 | 🟢 |
| `ddl-auto=update` 出现 column type 冲突（与遗留迁移交叉） | 4 张全新表 | 无遗留 | 🟢 |

## 五、建议补充顺序

1. **第一优先**（部署前必补 — P0 全部）：
   - Backend: 47 entity CRUD (12+12+12+11) + 4 perf + 4 frontend = **55 backend + 4 frontend = 59 P0 TCs**
2. **第二优先**（P1）：无（本变更不引入 P1 边界）
3. **第三优先**（P2 / v0.0.13+）：
   - Demand / Requirement / Story / Task 关联 Feature 的 TCs
   - 父级 status 级联守护 TCs
   - Cascading-select 在 size > 100 时的分页 fallback TC
