# 组织维度骨架 (rainier_organization / rainier_user / rainier_user_organization)

## Why

角色意图卡片 [§2 双维度模型](../../A-角色意图卡片.md) 规定"组织维度是稳定结构"，所有项目维度的角色（PO / Dev / QA / PM）都从组织维度征调人，所有 AI 推荐（派单建议、能力沉淀、绩效汇总）都需要 User 画像。组织维度缺位则后续业务实体（Project / Story / Task / Demand …）寸步难行。

本变更建立组织维度的 3 张表，**同时引入持久化基础设施**（JPA + Flyway + MySQL，UUID 主键，软删除全局模式），关闭 v0 Adjustment #1。模型采用**自引用树 + 人 + M2M 归属**结构，单表 `rainier_organization` 通过 `type` 字段承载角色卡片 §2.2 全部 5 个组织层级（公司→部门→领域→团队→小组），不在 schema 层硬绑定 5 张表。

字段集严格从 legacy `sys_depart` / `sys_user` 中筛过，保留必要 + 软删除，丢弃 oa 上游溯源 / 重复状态字段 / 个人隐私字段（决策已锁定见 design.md §字段筛选）。登录流程**不动** —— 仍是 mock JWT 接受任意账号密码，rainier_user 表与登录态解耦。

## What Changes

**A. 持久化基础设施**（一次性、未来所有业务实体复用）

- `pom.xml` 引入 `spring-boot-starter-data-jpa` / `mysql-connector-j 8.x` / `flyway-core` / `flyway-mysql`
- `application.yml`（dev）：连真实 MySQL；`spring.jpa.hibernate.ddl-auto=validate`；`spring.flyway.enabled=true`
- `application-test.yml`：H2 内存库 + `ddl-auto=create-drop` + Flyway 禁用
- `@EnableJpaAuditing`；抽 `BaseEntity`：UUID `id` + `create_by` + `create_time` + `update_by` + `update_time`
- 全局**软删除模式**：所有 entity 加 `@SQLDelete("UPDATE ... SET del_flag=1 WHERE id=?")` + `@Where("del_flag=0")`；DELETE 接口实际是 UPDATE
- 异常体系扩展：`NotFoundException`(404) / `ConflictException`(409)；`MethodArgumentNotValidException` → 400 JSON 含 `fieldErrors`
- 自定义 `PageResponse<T> {content, page, size, total}` 跨所有 list 接口统一

**B. Flyway 迁移**（`db/migration/V1__init_org.sql`）

3 张表均为 `VARCHAR(32)` UUID 主键、`utf8mb4_0900_ai_ci`、InnoDB、JPA Auditing 时间戳。

- `rainier_organization`（15 字段）—— 自引用树
  - `type` ∈ {COMPANY, DEPARTMENT, DOMAIN, TEAM, SUBGROUP}
  - `is_pmo` 标识 PMO 团队（角色卡片 §2.2）
  - `path` + `whole_name` 树缓存（服务层维护）
  - 唯一：`(parent_id, code)`；FK `parent_id → rainier_organization(id)` ON DELETE RESTRICT
- `rainier_user`（12 字段）—— 极简：身份 + 工号 + 邮箱 + `is_internal` + 启停 + 软删 + 审计
- `rainier_user_organization`（11 字段）—— M2M
  - `role` ∈ {MEMBER, HEAD}；HEAD 角色按所在 `org.type` 推断为 "部门负责人/领域负责人/团队负责人/小组负责人"
  - `is_primary` 单人 ≤ 1 行 primary（服务层保证）
  - `joined_at` + `left_at` 历史；`left_at IS NULL` 表示在岗
  - 唯一：`(user_id, organization_id)`；FK 同上

**C. 后端 CRUD**

- 3 套 entity / repository / service / controller / DTO
- 17 个 REST endpoint：每实体 5 CRUD + 2 个辅助查询（`GET /api/organizations/tree`、`GET /api/users/{id}/organizations`、`GET /api/organizations/{id}/users`）
- 树形操作：创建子节点自动派生 `path` / `whole_name`；移动节点（改 `parent_id`）级联重算自身及全部子孙；改 `name` 级联重算子孙的 `whole_name`
- 软删除：`DELETE /api/organizations/{id}` 若有子节点 / 关联用户 → 409；否则 UPDATE `del_flag=1`
- 类型软约束：服务层默认链 COMPANY→DEPT→DOMAIN→TEAM→SUBGROUP；允许跳级（角色退化），仅给警告日志

**D. 前端 3 个独立 CRUD**

- `/org/organizations`：列表（按 type + parent_id 筛选 + 树视图切换 toggle，v0 先列表）+ 编辑（含 is_pmo 开关）+ 删除二次确认
- `/org/users`：列表 + 编辑（无 password 字段；含 is_internal 开关）+ 删除
- `/org/user-organizations`：列表 + 新建（选 user + org + role + is_primary）+ 编辑（设 role / 设 is_primary 自动 demote 旧 primary / 填 left_at）+ 删除
- 抽 `components/ui/{Table, Pagination, Drawer, ConfirmDialog, TreeSelect}`（飞书风格）
- 抽 `hooks/usePaginated.ts`
- `AppLayout` 增加左侧 Sider，菜单组 "组织 → 组织节点 / 用户 / 用户-组织关系"

## Capabilities

### Modified Capabilities

- `backend-scaffold`：JPA + Flyway + MySQL + 软删除全局模式 + `@Transactional` + Bean Validation handler + NotFoundException + ConflictException
- `frontend-scaffold`：左侧 Sider + 通用 CRUD 组件壳 + TreeSelect 父节点选择器
- `dev-runtime`：backend 启动时 Flyway 自动 migrate；mysql 含 4 张表（3 业务 + flyway_schema_history）
- `test-runtime`：测试 profile H2 + ddl-auto=create-drop

### New Capabilities

- `entity-organization`：自引用树 CRUD（含 path/whole_name 缓存、type 软约束、PMO 标识、移动节点、软删 FK 保护）
- `entity-user`：用户 CRUD（极简版，无密码，软删 FK 保护）
- `entity-user-organization`：人↔组织 M2M CRUD（含 role、is_primary 单一性、左离历史）
- `pagination-envelope`：统一分页响应形态

## Impact

**代码层面**：

- 后端约 +55 文件（3 entity × 5 层 + BaseEntity + 2 异常 + PageResponse + SoftDelete 工具 + Flyway V1 + ~32 测试）
- 前端约 +35 文件（3 实体 × ~8 文件 + 通用组件 5 + Sider + 测试 ~16）
- 修改 backend `GlobalExceptionHandler` / `application*.yml` / `pom.xml`；frontend `AppLayout` / `AppRoutes`
- 修改 `archive/2026-06-02-bootstrap-fullstack-scaffold/pending-adjustments.md` 关闭 Adjustment #1

**配置层面**：

- pom 4 新依赖
- `application*.yml` 扩展 datasource / jpa / flyway
- 前端无新依赖
- 不动 `backend/checkstyle.xml`、`frontend/eslint.config.js`

**基础设施**：

- `docker-compose.yml` 不动（mysql 已在）
- 不引入新外部服务

## Success Criteria

- [ ] 干净环境 `docker compose up` 后，MySQL 含 `rainier_organization` / `rainier_user` / `rainier_user_organization` / `flyway_schema_history` 四张表
- [ ] backend 启动日志含 `Successfully applied 1 migration to schema "rainier"`
- [ ] `mvn -ntp test` 全绿；后端测试 ≥ 32 个新增
- [ ] `npm test -- --run` 全绿；前端测试 ≥ 14 个新增
- [ ] 3 个 POST：合法 payload → 201 + `Location` header；非法 → 400 JSON `{message, fieldErrors[]}`
- [ ] 3 个 GET list：`{content, page, size, total}` 形态
- [ ] `GET /api/organizations/tree` 返回扁平 list 含每条 `parent_id`，可在前端 O(n) 装树
- [ ] `DELETE /api/organizations/{id}` 若有子节点 → 409；若有 user_organization 关联 → 409；否则 UPDATE `del_flag=1`，后续 GET 返回 404
- [ ] 移动节点（改 `parent_id`）后该节点及所有子孙的 `path` / `whole_name` 全部正确
- [ ] 同一 user 设第二个 `is_primary=true` 时，旧 primary 自动 demote 为 false
- [ ] 浏览器 `/org/organizations` / `/org/users` / `/org/user-organizations` 三页可见、可增删改查（飞书风格）
- [ ] mock JWT 登录未变（任意账号密码仍可登录）
- [ ] 后端 lint + 前端 lint 0 错误
- [ ] `archive/2026-06-02-.../pending-adjustments.md` 中 Adjustment #1 标"已解决，见 changes/2026-06-04-org-tree-and-employee"
