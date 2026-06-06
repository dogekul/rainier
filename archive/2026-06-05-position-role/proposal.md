# 岗位（Position）+ 角色（Role）实体 — 双轴建模

## Why

v0.0.5 删除 `Organization.isPmo` 时承诺："PMO 是人 × 岗位/角色的属性，未来独立 change 引入。" 本变更兑现承诺，建立"形式岗位 vs 功能角色"双轴模型。

### 双轴模型

| 维度 | 实体 | 含义 | 挂载位置 |
|---|---|---|---|
| 形式岗位（Position） | `rainier_position` | 全局岗位池：Backend Engineer / PO / 设计师 / 测试 / SRE | `User.position_id` 单字段，1 user → 1 position |
| 功能角色（Role） | `rainier_role` | 全局角色池：PMO / TechLead / Reviewer / Approver / Scrum Master | M2M 通过 `rainier_user_role`，作用域到项目 |

### 作用域

- **岗位**是 User 的标签（跟着人走）：alice 是 Backend Engineer（永远；不区分她在哪个组织）。升职 = 直接改 `User.position_id`，v0 不追历史
- **角色**是 User × Role × Project 三元组：alice 在「项目 X」上戴「PMO」hat
- v0.0.7 Project 实体仍**不引入**（沿用 v0.0.6 `Requirement.project_id` 占位 FK 模式）— `UserRole.project_id` 是 `BIGINT NULL` 占位字段位，**无 FK 约束、无 service 校验**，v0 阶段任意 BIGINT 都接受，等 v0.0.8 落地 Project 后回头清理脏数据

### 命名澄清

v0.0.3 `UserOrganization.role: UserOrgRole(HEAD/MEMBER)` 是**组织树层级位**（"这个人在该组织节点是负责人 vs 普通成员"），与本变更新引入的 `Role` 实体（功能 hat）是两个不同概念，并存不冲突。

### PMO 用例链路

```
v0.0.5：删 Org.isPmo（错误地把 PMO 当组织属性）
v0.0.7：引入 Position + Role 双轴 + UserRole M2M  ← 本变更
v0.0.8：引入 Project 实体 + PMO 看板查询端点
```

## What Changes

### A. 数据层（3 新表 + 1 列）

- `rainier_position`（10 字段含审计）：全局岗位池
  - `id BIGINT auto_increment PK / code VARCHAR(64) service-级唯一 / name VARCHAR(100) NN / description VARCHAR(500) / category VARCHAR(16) NN (TECH/BIZ/PM/MGMT/OTHER) / enabled BOOLEAN NN`
  - + 6 审计字段 + `del_flag`（`@SQLDelete` + `@Where` 软删）
- `rainier_role`（9 字段含审计）：全局角色池
  - `id / code VARCHAR(64) service-级唯一 / name VARCHAR(100) NN / description VARCHAR(500) / enabled BOOLEAN NN`
  - + 6 审计 + `del_flag`（软删）
- `rainier_user_role`（10 字段含审计）：M2M 硬删
  - `id / user_id BIGINT NN FK rainier_user(id) / role_id BIGINT NN FK rainier_role(id) / project_id BIGINT NULL`
  - + 6 审计 + `del_flag`（保留但永 0）
  - + `UNIQUE (user_id, role_id, project_id)`（MySQL UNIQUE 允许多重 NULL → service 层补 NULL 唯一性兜底）
- `rainier_user` 加列 `position_id BIGINT NULL FK rainier_position(id)`

### B. 后端（约 38 文件）

- `com.rainier.position.{domain, dto, repository, service, controller}`（10 文件）：5 endpoint（POST/GET-id/GET-list/PUT/DELETE）；DELETE FK 保护（被任何 User 引用 → 409 "position has assigned users"）
- `com.rainier.role.{domain, dto, repository, service, controller}`（10 文件）：5 endpoint；DELETE FK 保护（被任何 user_role 引用 → 409 "role has assignments"）
- `com.rainier.userrole.{domain, dto, repository, service, controller}`（10 文件）：4 endpoint（POST/GET-id/GET-list/DELETE；M2M 硬删，无 PUT）；含 `(user_id, role_id, project_id)` 唯一性 service 校验（NULL 兜底）；`project_id` 不存在性校验 v0 **不做**（占位字段位）
- User 改造（4 文件）：
  - `User` entity 加 `position_id` 字段 + getter/setter
  - `UserCreateRequest` / `UserUpdateRequest` 加 `positionId`
  - `UserDetail` 富化 `positionName` / `positionCategory`
  - `UserService.create`/`update` 接 `positionId`（可空 → 校验 Position 存在，不存在 → 400）
- 常量类（4 个）：`PositionCategory.{TECH, BIZ, PM, MGMT, OTHER}`

### C. 前端（约 22 文件）

- 新 Sider 菜单组「**人事配置**」（位于「需求管理」之后）
- 路由前缀 `/hr/*`：
  - `/hr` → 重定向 `/hr/positions`
  - `/hr/positions` → `PositionsPage`
  - `/hr/roles` → `RolesPage`
  - `/hr/user-roles` → `UserRolesPage`
- `UsersPage` 编辑抽屉加 Position 下拉（异步加载岗位池）
- `UsersPage` 列表加「岗位」列（render `positionName` + category）
- `UserRolesPage`：用户/角色双 select + project_id 文本输入（占位）+ 列表富化 `userName`/`roleName`
- `api/{position, role, userRole}.ts` 3 个 module（沿用 v0.0.6 axios 模式）
- vitest：`PositionsPage` / `RolesPage` / `UserRolesPage` 表头 + `UsersEditDrawer` 岗位下拉断言（共 +5 测试）

### D. 显式排除（留下次或更晚）

- ❌ **Project 实体**（让 `UserRole.project_id` 真正可用）— v0.0.8
- ❌ **PMO 看板 query 端点**（`GET /api/users?roleCode=PMO&projectId=X` 富化列表）— v0.0.8
- ❌ **角色权限/ACL**（Role 仅是标签，不参与鉴权）
- ❌ **岗位等级**（initial / senior / staff / principal）
- ❌ **薪酬带、岗位历史**
- ❌ **Role 自动推断**（基于 `UO.role=HEAD` 自动给 Manager hat）
- ❌ **跨项目继承**
- ❌ **Position / Role 多语言名称**
- ❌ **V3 SQL 历史档**（Flyway 仍禁用；沿用 v0.0.5/v0.0.6 教训）
- ❌ **`project_id` 不存在性校验**（占位字段位，v0 阶段任意 BIGINT 都接受）
- ❌ **一个用户多岗位**（`User.position_id` 单字段；多岗位场景待业务真出现再扩 M2M）

## Capabilities

### Modified Capabilities

- `entity-user`：User 加 `position_id`；Detail 富化 `positionName`/`positionCategory`；Create/Update 接 `positionId`
- `frontend-scaffold`：Sider 加「人事配置」菜单组 + `/hr/*` 三路由 + `UsersPage` 编辑抽屉加岗位下拉 + 列表加岗位列

### New Capabilities

- `entity-position`：岗位池 CRUD + FK 保护（被任何 User 引用 → 409）
- `entity-role`：角色池 CRUD + FK 保护（被任何 user_role 引用 → 409）
- `entity-user-role`：用户 × 角色 × 项目作用域 M2M CRUD（含 `project_id` NULL 占位）

## Impact

**代码层面**：

- 后端约 +38 文件（3 entity × ~10 + 4 常量 + User 改 4）
- 前端约 +22 文件（3 api + 3 页 + UsersPage 编辑抽屉改 + 路由/菜单 + 测试）
- 修改：`User` entity/service/dto；`AppLayout.tsx` / `AppRoutes.tsx`
- `archive/*` 全不动；v0.0.6 demand/requirement/link 链路全不动；v0.0.3 UserOrganization 全不动（`role: UserOrgRole(HEAD/MEMBER)` 保持原义）

**配置层面**：无（不动 `application.yml` / `docker-compose.yml` / `pom.xml`）

**基础设施**：无（mysql 已在；3 新表 + 1 新列由 Hibernate `ddl-auto=update` 自动加）

## Success Criteria

- [ ] `mvn -ntp test` 全绿；后端测试 ≥ v0.0.6 baseline (94) + 25 新增 ≈ 119+
- [ ] `mvn -ntp spotless:check checkstyle:check` 0 违规
- [ ] `npm test -- --run` 全绿；前端测试 ≥ v0.0.6 baseline (19) + 5 新增 ≈ 24+
- [ ] `npm run build` 无 type error；`npm run lint` 0 错误
- [ ] `docker compose down -v` 后 `SHOW TABLES` 含 9 张表（v0.0.6 的 6 + 本变更 3 = `position` / `role` / `user_role`）
- [ ] `DESCRIBE rainier_user` 含新 `position_id BIGINT NULL` 列
- [ ] `DESCRIBE rainier_position` 含 `id / code / name / description / category / enabled` + 6 审计 + `del_flag`
- [ ] `DESCRIBE rainier_role` 含 `id / code / name / description / enabled` + 6 审计 + `del_flag`
- [ ] `DESCRIBE rainier_user_role` 含 `user_id / role_id / project_id`（NULL 允许）BIGINT
- [ ] `POST /api/positions` 最小 payload → 201 + body.id 数字 + 默认 `enabled=true`
- [ ] `POST /api/roles` 同上
- [ ] `POST /api/user-roles {userId, roleId, projectId=null}` → 201（未指派项目 hat）
- [ ] `POST /api/user-roles {userId, roleId, projectId=X}` 同对重复 → 409 "user-role already exists"
- [ ] `POST /api/user-roles {userId, roleId, projectId=null}` 同对（公司级）重复 → 409（service 层 NULL 兜底）
- [ ] `DELETE /api/positions/{id}` 若有 User 引用 → 409 "position has assigned users"
- [ ] `DELETE /api/roles/{id}` 若有 user-role 引用 → 409 "role has assignments"
- [ ] `DELETE /api/user-roles/{id}` → 204（硬删）
- [ ] `PUT /api/users/{id}` body 含 `positionId` → User 关联岗位；`GET /api/users/{id}` body 含 `positionName` / `positionCategory` 富化
- [ ] `PUT /api/users/{id}` body 含 `positionId` = 不存在 id → 400 "position not found"
- [ ] 浏览器 `/hr/positions` / `/hr/roles` / `/hr/user-roles` 三页可见列表 + 新建 + 编辑 + 删除
- [ ] Sider 含「人事配置」菜单组 + 3 子项
- [ ] `UsersPage` 列表新增「岗位」列；编辑抽屉「岗位」下拉可异步加载并保存
- [ ] `grep -rn 'isPmo\|is_pmo' backend/src/main/java frontend/src` 仍 0 行（v0.0.5 baseline）
- [ ] `grep -rn 'BaseAutoIdEntity' backend/src` 仍 0 行（v0.0.6 baseline）
- [ ] v0.0.6 baseline 保持：诉求 / 需求 / 关联 3 页所有功能仍可用
