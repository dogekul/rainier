# v0.0.12-product — introduce Product architecture (4 entities)

> Baseline: commit `31c9721` / tag `v0.0.11-task` (2026-06-09).
> 4-level chain: ProductCategory → Product → ProductModule → Feature.
> 项目视角 (Project / Sprint / Story / Task) 与 产品视角 (Category / Product / Module / Feature) 互补，互不依赖。

## Why

当前的工作分解金字塔 Project → Requirement → Sprint → Story → Task 是 **项目视角** 的执行链路 — 关心"这次迭代要交付什么"。但 PM/PMO 还需要 **产品视角** — 关心"我们家的产品长成什么样、有哪些模块、提供哪些功能"。

产品分类 / 产品 / 产品模块 / 功能 这 4 层是行业通用的产品架构语言；引入后：
- AI 助理在解析诉求时可以问"这是哪个产品的哪个功能要调整" — 进入功能锚点
- 后续 Demand / Requirement 可挂到 Feature (v0.0.13+)，形成 "诉求 → 产品功能" 双向追溯
- 报表层面能按产品维度汇总 (v0.0.14+ 路线图视图)
- 替代当前用户 / PMO 用 Excel / Notion 维护产品架构图的散乱做法

## What Changes

### A. 4 个新实体（A1-A4）

- **A1.** NEW `rainier_product_category` — 产品分类（顶层，flat 平铺，**不**做树）
  - id + 6 审计 + `del_flag` (@SQLDelete)
  - `code NN` / `name NN` / `description` / `status NN` / `owner_user_id NN`
  - status: `ACTIVE / ARCHIVED` (2-state)
- **A2.** NEW `rainier_product` — 产品（属一个 Category）
  - 同上结构 + `category_id BIGINT NN FK`
  - status: `PLANNING / ACTIVE / SUNSET / ARCHIVED` (4-state)
- **A3.** NEW `rainier_product_module` — 产品模块（属一个 Product）
  - 同上 + `product_id BIGINT NN FK`
  - status: `PLANNING / ACTIVE / DEPRECATED` (3-state)
- **A4.** NEW `rainier_feature` — 功能（属一个 Module）
  - 同上 + `module_id BIGINT NN FK`
  - status: `PLANNING / ACTIVE / DEPRECATED` (3-state)
- 全部 4 个实体：`code` service-级唯一（family pattern）；owner 可改（v0.0.8 Decision 6b sibling）；softdelete；list batch enrich。

### B. 后端（B1-B5）

- **B1-B4.** 4 套 `com.rainier.{productcategory,product,productmodule,feature}.*` (domain / dto / repository / service / controller) — 每套 5 CRUD endpoint
- **B5.** FK chain 保护：
  - 删 Category w/ Product → 409 `"category has linked products"`
  - 删 Product w/ Module → 409 `"product has linked modules"`
  - 删 Module w/ Feature → 409 `"module has linked features"`
  - Feature 无下游 — 直接软删

### C. 前端（C1-C3）

- **C1.** NEW Sider 顶级菜单组「产品」（位于「组织」与「需求管理」之间）
  - 4 项：产品分类 / 产品 / 产品模块 / 功能
- **C2.** NEW 4 个独立 CRUD 页面 + 4 个 EditDrawer
  - 联动级联 select：选 Category 后 Product 选项过滤；选 Product 后 Module 选项过滤；选 Module 后 Feature 选项过滤（v0.0.11 同款模式）
- **C3.** NEW 4 个 api/{productCategory, product, productModule, feature}.ts

### 显式排除（推到 v0.0.13+）

- Demand / Requirement / Story / Task 与 Feature 的关联字段
- ProductCategory 多级树（仍 flat）
- 路线图 / 产品看板 / 燃尽视图
- 产品成员制（owner 已够 v0）
- Feature 优先级 / 估算 / dependsOn
- 产品模块依赖图
- AI 助理自动建产品

## Capabilities

### Modified Capabilities

- `frontend-scaffold` — Sider 顶级组 4 → 5（加「产品」组 4 项 + 4 路由）；菜单序：组织 → 产品 → 需求管理 → 人事配置

### New Capabilities

- `entity-product-category` (2-state, flat)
- `entity-product` (4-state, Category FK)
- `entity-product-module` (3-state, Product FK)
- `entity-feature` (3-state, Module FK)

## Impact

**代码层面（~50 文件新增 + ~2 文件修改）**：

- backend NEW (~32)：4 entities × 8 文件 (domain / Status / CreateRequest / UpdateRequest / Detail / repo / service / controller)
- backend tests NEW (~12)：每 entity 3 文件 (Create / Query / Delete)
- backend MOD (0)：4 个 FK chain 守护都在 *Service.delete 内
- frontend NEW (~16)：4 api.ts + 4 *EditDrawer.tsx + 4 *Page.tsx + 4 test.tsx
- frontend MOD (2)：AppLayout.tsx (Sider 加产品组) + AppRoutes.tsx
- canonical spec NEW (4)：每 capability 一个 spec.md
- canonical spec MOD (1)：frontend-scaffold (Sider 顶级组演进)

**配置层面**：

- 无 application.yml / docker-compose / .env 变更

**基础设施**：

- 无新服务
- DB DDL: `ddl-auto=update` 自动建 4 表（首次启动 13 → 17 张表）
- 无启动迁移 runner（4 entities 都是新实体无遗留数据）

## Success Criteria

- [ ] `mvn test` 全绿（≥ 212 baseline + ~60 新 = **≥ 272 backend**）
- [ ] `npm test` 全绿（≥ 44 baseline + ~8 新 = **≥ 52 frontend**）
- [ ] `npm run build` 成功
- [ ] `docker compose up`：`SHOW TABLES` = **17**（含 4 新表）
- [ ] curl E2E：建 Category → 建 Product → 建 Module → 建 Feature 全链路富化；试创建 Product w/ 不存在 categoryId → 400；删 Category w/ Product → 409 `"category has linked products"`
- [ ] 前端 4 个新页面可建 / 编辑 / 删；联动级联 select 工作正常
- [ ] Sider 顶级组现有 4 → 5（含「产品」），点击 4 项跳对应路由
- [ ] enrich SQL count 每个 list endpoint 锁死 ≤ 5（2 page + 2 batch user/parent + 1 status field 富化 — v0.0.10.1 同款模式）
- [ ] 不 push 到 remote（待你后续决定）

## 已锁决策（用户 Gate 1 确认）

| # | 决策 | 选定方案 |
|---|------|---------|
| D1 | ProductCategory 结构 | flat 平铺（不做树） |
| D2 | 4 状态机不同 | Cat 2-state / Prod 4-state / Mod 3-state / Feat 3-state |
| D3 | Demand/Requirement ↔ Feature 关联 | 推到 v0.0.13+ |
| D4 | 拆分粒度 | 单一 change 一次性 4 实体（接受 ~2x v0.0.10 规模） |
| D5 | Sider 「产品」 位置 | 顶级独立组（位于「组织」与「需求管理」之间） |
