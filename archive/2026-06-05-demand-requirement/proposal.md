# 诉求 + 需求 + 关联 (rainier_demand / rainier_requirement / rainier_demand_requirement)

> 本提案为修订版。原版（2026-06-05 Phase 1 完成后挂起）假设 v1 表仍用 VARCHAR(32) UUID、需要新建 BaseAutoIdEntity、跨代 FK 用 VARCHAR(32)。v0.0.4-id-migration 将 v1 表统一为 BIGINT 自增、v0.0.5-remove-org-pmo 删除了 Organization.isPmo 字段后，本提案大幅简化：**单一 BaseEntity 世界 + 全栈 BIGINT FK**。

## Why

角色卡片 [§4.1 卡片 2-3](../../A-角色意图卡片.md) 围绕一条主线：**业务方提诉求 → PO 翻译为需求 → 开发拆 Story 实现**。

v0.0.5 基线已就绪：
- v1：rainier_user / rainier_organization / rainier_user_organization 全部 BIGINT 自增；BaseEntity (Long id) 是唯一基类
- v0.0.5：Organization 不再持有 isPmo（PMO 是"人 × 岗位"属性，待 Position 实体支持）

项目维度（诉求 / 需求 / Project / Story / Task）整体为空。要落的最先两个**起点实体**：

- 没有 `rainier_demand` → 业务方没有 PM 系统入口（只能私下沟通，AI 抓不到事实层）
- 没有 `rainier_requirement` → PO 没有"诉求 → 可执行工作"的中间产物，无法对开发派单
- 角色卡片 §4.1 卡片 16 明示"AI 在这层做归类去重"是高价值能力 → `ai_classification` / `ai_duplicate_hint` 字段位**现在就要预留**，避免后期 schema 大改

业务**转化关系**（一诉求拆多需求 / 多相似诉求合一需求）需要 M2M 表承载；用户在 Phase 1 决定一并落地。

## What Changes

### A. 数据层

- 3 张表
- `rainier_demand`（约 12 字段）—— 业务方原始输入
- `rainier_requirement`（约 12 字段）—— PO 翻译产物
- `rainier_demand_requirement`（约 10 字段含审计）—— M2M 关联，hard delete
- schema 由 Hibernate `ddl-auto=update` 在空 schema 上生成（沿用 v0.0.3 Adjustment #1 Flyway 禁用决定；不写 V2 SQL 历史档以免重蹈 v0.0.5 H-finding 覆辙）

### B. id 策略 — 沿用 v0.0.4 单一 BIGINT 自增标准（无任何"调整"）

- 3 张新表的 id 类型 = `BIGINT AUTO_INCREMENT`（Java `Long`，JPA `@GeneratedValue(strategy = GenerationType.IDENTITY)`）
- 直接继承现有 `com.rainier.common.persistence.BaseEntity`（v0.0.4 已是 Long id）
- **不引入** `BaseAutoIdEntity` 或任何新基类
- FK 列均为 BIGINT — `submitter_user_id` / `owner_user_id` 引用 `rainier_user.id`（BIGINT）；本次内部 FK（`demand_id` / `requirement_id` / `ai_duplicate_hint`）也全部 BIGINT

### C. 后端

- 3 套 entity / repository / service / controller / DTO（包结构 `com.rainier.{demand, requirement, demandrequirement}`）
- **15 个标准 REST endpoint**（5 CRUD × 3）+ **2 关联查询辅助**（`GET /api/requirements/{id}/source-demands`、`GET /api/demands/{id}/derived-requirements`）
- **特殊：`POST /api/requirements` 支持 `sourceDemandIds[]`** —— 原子创建需求 + 多行 demand_requirement，将"诉求转需求"封装为一次调用
- FK 删除保护：删 demand 若有 link → 409；删 requirement 若有 link → 409
- 软删除：demand + requirement 启用 `@SQLDelete` + `@Where`；demand_requirement 硬删
- 复用 v1 异常体系 / PageResponse / 软删除模式 / GlobalExceptionHandler

### D. 前端

- 新 Sider 菜单组「**需求管理**」展开后含 3 项：诉求 / 需求 / 诉求-需求关联
- 3 个独立 CRUD 页：`/pm/demands`、`/pm/requirements`、`/pm/demand-requirements`
- 路由前缀 `/pm/*`（PM = Project Management，为未来 project/story/task 留命名空间）
- 复用 v1 全部通用组件（Table/Pagination/Drawer/ConfirmDialog/TreeSelect/usePaginated）
- requirement 编辑抽屉新增「源诉求」多选区（从 demand 列表选 0+ 行）
- TS 类型契约：所有 id / userId / demandId / requirementId 字段类型为 `number`（沿用 v0.0.4 全栈约定）

### E. 实体字段表

**rainier_demand**

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT AUTO_INCREMENT | 主键 |
| title | VARCHAR(100) NN | 一句话主题 |
| description | VARCHAR(2000) | 自然语言描述 |
| submitter_user_id | BIGINT NN FK rainier_user(id) | 提交人 |
| status | VARCHAR(16) NN | PENDING / IN_REVIEW / CONVERTED / DONE / CLOSED（默认 PENDING） |
| priority | VARCHAR(16) NN | URGENT / HIGH / MEDIUM / LOW（默认 MEDIUM） |
| source | VARCHAR(16) NN | WEB / WECHAT / EMAIL / DINGTALK / OTHER（默认 WEB） |
| ai_classification | VARCHAR(100) | AI 推断分类（占位） |
| ai_duplicate_hint | BIGINT | 可能重复的 demand id（占位，引用本表 id；不加 FK 约束保留弹性） |
| close_reason | VARCHAR(500) | CLOSED 时填 |
| 6 审计 + del_flag | | BaseEntity 继承（含 create_by / create_time / update_by / update_time / del_flag） |

**rainier_requirement**

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT AUTO_INCREMENT | 主键 |
| code | VARCHAR(64) UNIQUE | 业务码（如 `REQ-2026-0001`，PO 手填，v0 不自动生成） |
| title | VARCHAR(100) NN | |
| description | VARCHAR(4000) | 含验收标准 |
| owner_user_id | BIGINT NN FK rainier_user(id) | 负责 PO |
| status | VARCHAR(16) NN | DRAFT / IN_REVIEW / APPROVED / IN_DEV / DELIVERED / DEPRECATED（默认 DRAFT） |
| priority | VARCHAR(16) NN | 同 demand |
| complexity | VARCHAR(8) | XS / S / M / L / XL |
| **project_id** | BIGINT | **FK 字段位预留**（NULL；不加 FK 约束；等 Project 表出现后独立变更补） |
| close_reason | VARCHAR(500) | DEPRECATED 时填 |
| 6 审计 + del_flag | | |

**rainier_demand_requirement**

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT AUTO_INCREMENT | 主键 |
| demand_id | BIGINT NN FK rainier_demand(id) | 诉求 |
| requirement_id | BIGINT NN FK rainier_requirement(id) | 需求 |
| link_type | VARCHAR(16) NN | DERIVED（PO 派生）/ RELATED（相关背景） |
| 6 审计 + del_flag（继承但 del_flag 永远 0 因硬删） | | |
| UNIQUE (demand_id, requirement_id) | | 不重复挂同一对 |

### F. 不在本变更（显式排除）

- ❌ **Project / Epic / Story / Task 实体** — 留下次切片
- ❌ **Position / Role 岗位角色实体** — 留独立 change；PMO 看板等需求依赖它，但本次 demand-requirement 不实现
- ❌ **真实 AI 归类去重**（接 LLM）— `ai_classification` / `ai_duplicate_hint` 仅占位
- ❌ **钉钉 / 企业微信 / 邮件 source 集成** — source 仅 enum 字段
- ❌ **业务方简化入口**（角色卡片 §3 "比微信还简单"）— 当前 admin/PO 视角足够 v0
- ❌ **状态自动转换**（如所有关联需求 DELIVERED → demand 自动 DONE）— 全手动
- ❌ **requirement.code 自动生成** — PO 手填
- ❌ **验收 workflow（单独表）**
- ❌ **V2__init_pm.sql Flyway 脚本** — Flyway 仍禁用；写历史档反而会再触发 v0.0.5 H-finding 同类问题（schema-vs-file 双源）
- ❌ **重新引入 BaseAutoIdEntity 或任何新基类** — 全栈单 BaseEntity
- ❌ **重新引入 Organization.isPmo** — 已由 v0.0.5 删除，不可恢复

## Capabilities

### Modified Capabilities

- `frontend-scaffold`：Sider 新增菜单组「需求管理」+ 3 路由 `/pm/*`

### New Capabilities

- `entity-demand`：诉求 CRUD（含 5 状态 enum + AI 占位 + 软删 + FK 保护）
- `entity-requirement`：需求 CRUD（含 6 状态 enum + complexity + project_id 字段位 + 软删 + FK 保护 + 含 sourceDemandIds 转化语义）
- `entity-demand-requirement`：M2M CRUD（hard delete + uniqueness + 2 个辅助查询）
- `workflow-demand-conversion`：`POST /api/requirements` 同时接受 sourceDemandIds 的原子转化语义（封装在 requirement service）

## Impact

**代码层面**：

- 后端约 +45 文件（3 entity × ~12 文件 + 转化逻辑 + ~14 测试；少了 BaseAutoIdEntity 那一组 5 文件）
- 前端约 +25 文件（3 entity × ~7 文件 + 路由 + 菜单 + ~6 测试）
- 修改：backend 不动现有 entity / BaseEntity / 异常体系；frontend `AppLayout.tsx` 加菜单组、`AppRoutes.tsx` 加 4 路由（含 `/pm` 默认重定向）
- archive/* 全部不动

**配置层面**：

- 无（不加依赖、不动 application.yml、不动 docker-compose）

**基础设施**：

- 无（mysql 已在；3 新表由 Hibernate ddl-auto=update 自动加上）
- 不写 SQL 迁移脚本（与 v0.0.5 决策一致 — Flyway 禁用情况下，留历史档反而引入 schema-vs-file 双源风险）

## Success Criteria

- [ ] `mvn -ntp test` 全绿；后端测试 ≥ v0.0.5 baseline (64) + 30 新增 ≈ 94+
- [ ] `mvn -ntp spotless:check checkstyle:check` 0 违规
- [ ] `npm test -- --run` 全绿；前端测试 ≥ v0.0.5 baseline (13) + 6 新增 ≈ 19+
- [ ] `npm run build` 无 type error；`npm run lint` 0 错误
- [ ] docker compose down -v && up 后 MySQL 含 6 张表（v1 的 3 + 本变更 3 = `rainier_{organization, user, user_organization, demand, requirement, demand_requirement}`）
- [ ] 3 张新表的 `id` 列类型为 `BIGINT AUTO_INCREMENT`，**与全栈统一**（与 v0.0.4 v1 表一致）
- [ ] 3 张新表的 FK 列（`submitter_user_id` / `owner_user_id` / `demand_id` / `requirement_id`）类型均为 `BIGINT`
- [ ] `grep -rn 'BaseAutoIdEntity' backend/src` 返回 0 行（不应出现该历史概念）
- [ ] `POST /api/demands` 最小 payload（`title` + `description` + `submitterUserId`）→ 201 + body.id 为 JSON 数字 + 默认 `status=PENDING` / `priority=MEDIUM` / `source=WEB`
- [ ] `POST /api/requirements` 含 `sourceDemandIds: [1, 2]` → 201 + DB 中 demand_requirement 表新增 2 行 `link_type=DERIVED`
- [ ] `DELETE /api/demands/{id}` 若 demand 有 link → 409
- [ ] `DELETE /api/requirements/{id}` 若 requirement 有 link → 409
- [ ] `GET /api/requirements/{id}/source-demands` 返回该需求关联的诉求清单（含 demand 字段 + linkType）
- [ ] `GET /api/demands/{id}/derived-requirements` 返回该诉求派生的需求清单
- [ ] 浏览器 `/pm/demands` / `/pm/requirements` / `/pm/demand-requirements` 三页可见列表 + 新建 + 编辑 + 删除
- [ ] Sider 含「需求管理」菜单组 + 3 项可点击
- [ ] requirement 编辑抽屉「源诉求」多选可异步加载 demand 列表并保存
- [ ] v0.0.5 baseline 保持：组织树 CRUD + 用户 CRUD + 人员-组织归属 全部仍可用；Organization 实体仍无 isPmo
