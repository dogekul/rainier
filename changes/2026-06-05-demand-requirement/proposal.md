# 诉求 + 需求 + 关联 (rainier_demand / rainier_requirement / rainier_demand_requirement)

## Why

角色卡片 [§4.1 卡片 2-3](../../A-角色意图卡片.md) 围绕一条主线：**业务方提诉求 → PO 翻译为需求 → 开发拆 Story 实现**。v1 已建好"人 + 组织"维度（rainier_user / rainier_organization / rainier_user_organization），但项目维度（诉求 / 需求 / Project / Story / Task）整体为空。

最先要落的是这两个**起点实体**：

- 没有 `rainier_demand` → 业务方没有 PM 系统入口（只能私下沟通，AI 抓不到事实层）
- 没有 `rainier_requirement` → PO 没有"诉求 → 可执行工作"的中间产物，无法对开发派单
- 角色卡片 §4.1 卡片 16 明示"AI 在这层做归类去重"是高价值能力 → `ai_classification` / `ai_duplicate_hint` 字段位**现在就要预留**，避免后期 schema 大改

业务**转化关系**（一诉求拆多需求 / 多相似诉求合一需求）需要 M2M 表承载；用户在 Phase 1 决定一并落地。

## What Changes

### A. 数据层

- 3 张表 + `db/migration/V2__init_pm.sql`（仅作 schema 文档基线；dev 实际由 Hibernate `ddl-auto=update` 生成，与 v1 同策略，沿用 Flyway 禁用决定）
- `rainier_demand`（12 字段）—— 业务方原始输入
- `rainier_requirement`（12 字段）—— PO 翻译产物
- `rainier_demand_requirement`（10 字段含审计）—— M2M 关联，hard delete

### B. id 策略调整（与 v1 不同）

- **本变更及未来所有新表的 id 类型改为 `BIGINT AUTO_INCREMENT`** （Java `Long`，JPA `@GeneratedValue(strategy = GenerationType.IDENTITY)`）
- v1 已交付的 3 张表（rainier_organization / user / user_organization）保留 `VARCHAR(32)` UUID 不动
- FK 跨代引用：本变更新表的 `submitter_user_id` / `owner_user_id` 列仍为 `VARCHAR(32)`，引用 v1 的 `rainier_user.id`
- 新建抽象 `com.rainier.common.persistence.BaseAutoIdEntity`（`Long id` + 同 v1 BaseEntity 的 6 个审计字段 + del_flag）；v1 `BaseEntity` 保留不动

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

### E. 实体字段表（已按 BIGINT id 调整）

**rainier_demand**

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT AUTO_INCREMENT | 主键（新策略） |
| title | VARCHAR(100) NN | 一句话主题 |
| description | VARCHAR(2000) | 自然语言描述 |
| submitter_user_id | VARCHAR(32) NN FK rainier_user(id) | 提交人，引用 v1 用户表 |
| status | VARCHAR(16) NN | PENDING / IN_REVIEW / CONVERTED / DONE / CLOSED（默认 PENDING） |
| priority | VARCHAR(16) NN | URGENT / HIGH / MEDIUM / LOW（默认 MEDIUM） |
| source | VARCHAR(16) NN | WEB / WECHAT / EMAIL / DINGTALK / OTHER（默认 WEB） |
| ai_classification | VARCHAR(100) | AI 推断分类（占位） |
| ai_duplicate_hint | BIGINT | 可能重复的 demand id（占位，引用本表 id，跨代 FK 暂不加约束） |
| close_reason | VARCHAR(500) | CLOSED 时填 |
| 6 审计 + del_flag | | BaseAutoIdEntity 继承 |

**rainier_requirement**

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT AUTO_INCREMENT | 主键 |
| code | VARCHAR(64) UNIQUE | 业务码（如 `REQ-2026-0001`，PO 手填，v0 不自动生成） |
| title | VARCHAR(100) NN | |
| description | VARCHAR(4000) | 含验收标准 |
| owner_user_id | VARCHAR(32) NN FK rainier_user(id) | 负责 PO |
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
| demand_id | BIGINT NN FK | 诉求 |
| requirement_id | BIGINT NN FK | 需求 |
| link_type | VARCHAR(16) NN | DERIVED（PO 派生）/ RELATED（相关背景） |
| 6 审计 + del_flag（继承但 del_flag 永远 0 因硬删） | | |
| UNIQUE (demand_id, requirement_id) | | 不重复挂同一对 |

### F. 不在本变更（显式排除）

- ❌ Project / Epic / Story / Task 实体（留下次切片）
- ❌ 真实 AI 归类去重（接 LLM） —— `ai_classification` / `ai_duplicate_hint` 仅占位
- ❌ 钉钉 / 企业微信 / 邮件 source 集成 —— source 仅 enum 字段
- ❌ 业务方简化入口（角色卡片 §3 "比微信还简单"） —— 当前 admin/PO 视角足够 v0
- ❌ 状态自动转换（如所有关联需求 DELIVERED → demand 自动 DONE） —— 全手动
- ❌ requirement.code 自动生成 —— PO 手填
- ❌ 验收 workflow（单独表）
- ❌ v1 已交付表的 id 类型回迁（v1 保留 varchar(32) 不动）

## Capabilities

### Modified Capabilities

- `frontend-scaffold`：Sider 新增菜单组「需求管理」+ 3 路由 `/pm/*`
- `backend-scaffold`：新建 `BaseAutoIdEntity` 抽象（v1 BaseEntity 不动，两者并存供不同代际表选用）

### New Capabilities

- `entity-demand`：诉求 CRUD（含 5 状态 enum + AI 占位 + 软删 + FK 保护）
- `entity-requirement`：需求 CRUD（含 6 状态 enum + complexity + project_id 字段位 + 软删 + FK 保护 + 含 sourceDemandIds 转化）
- `entity-demand-requirement`：M2M CRUD（hard delete + uniqueness + 2 个辅助查询）
- `workflow-demand-conversion`：`POST /api/requirements` 同时接受 sourceDemandIds 的转化语义（封装在 requirement service）

## Impact

**代码层面**：

- 后端约 +50 文件（3 entity × ~12 文件 + 转化逻辑 + BaseAutoIdEntity + ~14 测试）
- 前端约 +25 文件（3 entity × ~7 文件 + 路由 + 菜单 + ~6 测试）
- 修改：backend 不动现有 entity；frontend `AppLayout.tsx` 加菜单组、`AppRoutes.tsx` 加 4 路由（含 `/pm` 默认重定向）
- archive/2026-06-04-org-tree-and-employee 不动（v1 已交付）

**配置层面**：

- 无（不加依赖、不动 application.yml、不动 docker-compose）

**基础设施**：

- 无（mysql 已在；3 新表自动由 Hibernate ddl-auto=update 加上）

## Success Criteria

- [ ] `mvn -ntp test` 全绿；后端测试 ≥ 30 新增
- [ ] `mvn -ntp spotless:check checkstyle:check` 0 违规
- [ ] `npm test -- --run` 全绿；前端测试 ≥ 6 新增
- [ ] `npm run build` 无 type error；`npm run lint` 0 错误
- [ ] docker compose up 后 MySQL 含 6 张表（v1 的 3 + 本变更 3 = `rainier_{organization, user, user_organization, demand, requirement, demand_requirement}`）
- [ ] 3 张新表的 `id` 列类型为 `BIGINT AUTO_INCREMENT`（不是 VARCHAR(32)）
- [ ] `POST /api/demands` 最小 payload（`title` + `description` + `submitterUserId`）→ 201 + body.id 为数字 + 默认 `status=PENDING` / `priority=MEDIUM` / `source=WEB`
- [ ] `POST /api/requirements` 含 `sourceDemandIds: [1, 2]` → 201 + DB 中 demand_requirement 表新增 2 行 `link_type=DERIVED`
- [ ] `DELETE /api/demands/{id}` 若 demand 有 link → 409
- [ ] `DELETE /api/requirements/{id}` 若 requirement 有 link → 409
- [ ] `GET /api/requirements/{id}/source-demands` 返回该需求关联的诉求清单（含 demand 字段 + linkType）
- [ ] `GET /api/demands/{id}/derived-requirements` 返回该诉求派生的需求清单
- [ ] 浏览器 `/pm/demands` / `/pm/requirements` / `/pm/demand-requirements` 三页可见列表 + 新建 + 编辑 + 删除
- [ ] Sider 含「需求管理」菜单组 + 3 项可点击
- [ ] requirement 编辑抽屉「源诉求」多选可异步加载 demand 列表并保存
- [ ] v1 已交付表的 id 类型保持 `VARCHAR(32)` 不变（git diff 显示 v1 文件未动）
