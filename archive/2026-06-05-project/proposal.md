# Project 实体 + 激活 2 个占位 FK

## Why

v0.0.6 引入 Requirement 时为未来 Project 预留 `project_id` 字段位（BIGINT NULL，无 FK 约束，无 service 校验 — 占位语义）。v0.0.7 引入 UserRole 时复制同款占位。两个 entity 的 architecture 注释都明示「v0.0.8 Project 落地后做数据清理 + 加校验」。

本变更兑现承诺：

- 引入 Project 实体（PM 主线核心实体）
- 激活 v0.0.6 `Requirement.project_id` 和 v0.0.7 `UserRole.project_id` 的存在性校验 + 富化
- 不动 schema（不加列），只动 service 校验逻辑 + DTO 富化 + 前端控件类型

### 角色配置归宿（设计回答）

**所有"项目维度的角色 hat"（PM / PMO / TechLead / QA Lead / Architect / Reviewer / ...）全部走 v0.0.7 引入的 UserRole M2M (user × role × project)，不在 Project 表里加字段。**

Project 表里**只保留 `owner_user_id`**（项目负责人 / 创建者 / 总责任人），因为：

- owner 不是 hat，是项目的本质属性（类似 Requirement.ownerUserId）
- 不固化到表里，业务加新 hat（如"Data Lead"）就要改 schema —— 这是 v0.0.5 删除 Org.isPmo 的同款反模式
- 走 UserRole 路径：业务在 /hr/roles 加一条 Role 即可全栈解锁；一人多 hat / 一 hat 多人天然支持

### owner 可改

与 v0.0.6 Requirement.ownerUserId 不同（Requirement 不可改 owner），Project 的 owner **可改**。理由：项目生命周期长，owner 转移是常见操作；Requirement 短期产物，转 owner 不如重建。

### 现有占位脏数据策略 — 启动时自愈清理（严格化）

当前 DB 中 `user_role.id=2` 的 `projectId=42` 是 v0.0.7 测试时填的占位 ID（无对应 Project）。本变更引入**应用启动时自动清理**机制（Spring `CommandLineRunner`）：

- 启动时扫描 `Requirement.project_id` + `UserRole.project_id` 所有非空值
- 对每个值调 `projectRepo.existsById`；不存在 → 把该行的 `project_id` SET NULL（native UPDATE 避免触 SQLDelete 软删）+ log WARN `"cleaned dangling project_id from <table>.<id>"`
- v0.0.8 启动后立即清掉 `user_role.id=2` projectId=42 → null
- 后续 reads 路径**严格**（不需要也不实现 dangling 容错；service 假设 projectId 非空时必然有对应 Project）

这把 Gate 1 时承诺的"v0 接受脏数据 admin 手动"承诺升级为"启动自愈"。实际效果：脏数据不会进入 v0.0.8 的运行时状态，admin 看 log 知道发生了什么。

## What Changes

### A. 数据层（1 新表，0 新列）

`rainier_project`（约 13 字段含审计）：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT PK` | |
| `code` | `VARCHAR(64) NN` | service-级唯一（与 Requirement.code / Position.code 一致，不加 DB UNIQUE 避免 soft-delete 残留） |
| `name` | `VARCHAR(100) NN` | |
| `description` | `VARCHAR(2000)` | |
| `status` | `VARCHAR(16) NN` | `PLANNING / ACTIVE / ON_HOLD / DELIVERED / ARCHIVED`（默认 PLANNING） |
| `owner_user_id` | `BIGINT NN` | FK `rainier_user(id)`；**业务字段必填**；**可改**（admin 自由转 owner） |
| `start_date` | `DATE NULL` | 项目预计开始 |
| `end_date` | `DATE NULL` | 项目预计结束 |
| `enabled` | `BOOLEAN NN` | 默认 true |
| 6 审计 + `del_flag` | | `create_by` / `update_by` 由 v0 `AuditorAwareImpl` 自动注入登录 username；`@SQLDelete` + `@Where` 软删 |

**不新增列**：v0.0.6 `Requirement.project_id` 与 v0.0.7 `UserRole.project_id` 是占位列，本次激活校验。

### B. 后端（约 22 文件）

- `com.rainier.project.{domain, dto, repository, service, controller}`（10 文件）
  - `Project` entity（继承 BaseEntity，@SQLDelete 软删）
  - `ProjectStatus` 常量类（5 状态）
  - 4 DTO：`ProjectCreateRequest` / `ProjectUpdateRequest`（含 ownerUserId，可改）/ `ProjectDetail`（含富化 ownerName/ownerLoginName）
  - `ProjectRepository`：`existsByCode`
  - `ProjectService`：create（校验 code 唯一 + status enum + ownerUserId 存在）/ findById（含富化）/ list（filter status/enabled + search）/ update（含 ownerUserId 改时校验存在）/ delete（FK 保护：被 Requirement 或 UserRole 引用 → 409）
  - `ProjectController`：5 endpoint

- Requirement 改造（6 文件）
  - `RequirementService.create / update` 增加 projectId 非空时存在性校验（不存在 → BadRequestException "project not found"）
  - `RequirementService.update` 增加 ownerUserId 字段支持（**owner 可改**；与 v0.0.6 不可改决策的对内修订）；校验新 owner 存在
  - `RequirementUpdateRequest` 加 `ownerUserId` 字段
  - `RequirementDetail` 加 `projectName` + `projectCode` 字段（owner 富化字段 ownerName 在 v0.0.6 已建立）
  - `RequirementService.enrich()` 富化 — service 注入 ProjectRepository（reads 严格：假设 projectId 非空时 Project 必存在；DanglingProjectIdCleanup 保证此前提）
  - 扩展 `RequirementControllerCreateTest` / `QueryTest` 加 TC：projectId 校验 + 富化 + owner 改

- 新增 `com.rainier.project.bootstrap.DanglingProjectIdCleanup`（1 文件）
  - `@Component` + `CommandLineRunner.run()`
  - 启动时执行：扫描 `Requirement.project_id` + `UserRole.project_id` 所有非空值 → 不存在 → native query SET NULL + log WARN
  - 第一次运行清干净；后续启动 noop

- UserRole 改造（4 文件）
  - `UserRoleService.create` 增加 projectId 非空时存在性校验（NULL 保持公司级 hat 语义）
  - `UserRoleDetail` 加 `projectName` + `projectCode` 字段
  - `UserRoleService.enrich()` 富化（reads 严格，由 DanglingProjectIdCleanup 保证无悬空引用）
  - 扩展 `UserRoleControllerCreateTest` / `QueryTest` 加 TC（projectId 校验 + 富化 + 启动后脏数据已清）

- `UserRepository` **不动**（不加 findByLoginName；owner 默认在前端解析）

### C. 前端（约 10 文件）

- 新 `api/project.ts`：interfaces + 5 functions（含 ProjectStatus 类型）
- 新 `/pm/projects` → `ProjectsPage` CRUD（与 Position/Role 同款模式）
  - 「负责人」下拉异步加载用户池
  - 新建时默认选中**当前登录用户**：`users.find(u => u.loginName === useAuthStore.user.username)?.id || ''`
  - 编辑现有项目时**不 disabled**，可改成任意其他用户
  - 当前登录用户在 DB 无 User 行时下拉默认空，admin 手动选
- 改造 `RequirementEditDrawer`：projectId 从「数字输入框」→「Project 下拉」（异步 listProjects；编辑时回显）
- 改造 `UserRolesPage`：projectId 从「数字输入框」→「Project 下拉」（留白 = 公司级 hat 保留）
- `RequirementsPage` + `UserRolesPage` 列表新增「项目」列（render `projectName (projectCode)`）
- Sider「需求管理」菜单组追加「项目」一项（**插在「诉求」之前** — 项目是上位概念）
- `AppRoutes` 加 `/pm/projects` 路由
- vitest：ProjectsPage 默认 owner = current user 断言 + Sider「项目」可见 + AppRoutes 路由 mount + RequirementEditDrawer 含 project 下拉

### D. 显式排除（v0.0.9+ 或更晚）

- ❌ **PMO 看板 query 端点**（`GET /api/users?roleCode=PMO&projectId=X` 富化列表）— v0.0.9 单独
- ❌ **Project 成员 M2M**（角色实际上已能表达：项目成员 = 任何在该项目持有角色的人）
- ❌ **Project 阶段 / 里程碑 / 甘特图**
- ❌ **Project 预算 / 资源分配**
- ❌ **Project 子项目（树形结构）**
- ❌ **项目状态自动转换**（如所有 Requirement DELIVERED → Project DELIVERED）
- ❌ **V4 SQL 历史档**（Flyway 仍禁用）
- ❌ **强制 projectId 非空**（保留 NULL 表示"未指派项目" / 公司级 hat）
- ❌ **GET /api/auth/me 增强**（前端通过 listUsers 匹配 loginName 解析 default owner）

## Capabilities

### Modified Capabilities

- `entity-requirement` — projectId 激活校验 + Detail 富化 projectName/projectCode + ownerUserId **改为可改**（v0.0.6 不可改的对内修订）
- `entity-user-role` — projectId 激活校验 + Detail 富化（保留 NULL = 公司级 hat 语义）
- `frontend-scaffold` — Sider 加「项目」菜单项 + `/pm/projects` 路由 + 改 RequirementEditDrawer / UserRolesPage 的 projectId 输入控件 + RequirementEditDrawer 加 ownerUserId 可改下拉

### New Capabilities

- `entity-project` — Project CRUD + FK 保护（Requirement / UserRole 引用 → 409）+ owner 可改

## Impact

**代码层面**：

- 后端约 +22 文件（10 Project 链 + 5 Requirement 改造 + 4 UserRole 改造 + 3 测试扩展）
- 前端约 +10 文件（1 api + 1 page + 2 改造 + 路由/菜单 + 测试）
- 修改：Requirement / UserRole entity 不动；只动 service + dto + 测试 + 富化 join
- archive/* 全不动；v0.0.5/v0.0.6/v0.0.7 baseline 全保留

**配置层面**：无（不动 `application.yml` / `docker-compose.yml` / `pom.xml`）

**基础设施**：

- **不 down -v**；Hibernate `ddl-auto=update` 自动加 `rainier_project` 表
- 现有手测数据（alice / lili / 后台开发 / PMO / YFM / user-role 2 行含脏 projectId=42）保留
- frontend 容器单独 rebuild + recreate（mysql 卷不动）

## Success Criteria

- [ ] `mvn -ntp test` 全绿；后端测试 ≥ v0.0.7 baseline (125) + 15 新增 ≈ 140+
- [ ] `mvn -ntp spotless:check checkstyle:check` 0 违规
- [ ] `npm test -- --run` 全绿；前端测试 ≥ v0.0.7 baseline (25) + 4 新增 ≈ 29+
- [ ] `npm run build` 无 type error；`npm run lint` 0 错误
- [ ] `docker exec rainier-mysql mysql -e "SHOW TABLES"` 含 10 张表（v0.0.7 的 9 + 本变更 1 = `rainier_project`）
- [ ] `DESCRIBE rainier_project` 含 id / code / name / description / status / owner_user_id / start_date / end_date / enabled + 6 审计 + del_flag
- [ ] `POST /api/projects` 最小 payload（`code + name + ownerUserId`）→ 201 + body.id 数字 + 默认 `status=PLANNING` / `enabled=true`
- [ ] `POST /api/projects` body 缺 `ownerUserId` → 400 + fieldErrors[*].field="ownerUserId"
- [ ] `POST /api/projects` `ownerUserId` 不存在 → 400 "owner user not found"
- [ ] `POST /api/projects` code 重复 → 409 "code already exists"
- [ ] `POST /api/projects` 非法 status → 400 "invalid status"
- [ ] `POST /api/projects` 创建后 body.createBy = 当前登录用户 loginName（AuditorAwareImpl 自动）
- [ ] `PUT /api/projects/{id}` body 含 `ownerUserId` = 另一存在用户 id → 200 + body.ownerUserId 更新（owner 可改）
- [ ] `PUT /api/projects/{id}` body 含 `ownerUserId` 不存在 → 400 "owner user not found"
- [ ] `GET /api/projects/{id}` 返回 body 含富化 `ownerName` + `ownerLoginName`
- [ ] `DELETE /api/projects/{id}` 被 Requirement 引用 → 409 "project has linked requirements"
- [ ] `DELETE /api/projects/{id}` 被 UserRole 引用 → 409 "project has assigned user-roles"
- [ ] `DELETE /api/projects/{id}` 无引用 → 204
- [ ] `POST /api/requirements` 含 `projectId` 不存在 → 400 "project not found"
- [ ] `POST /api/requirements` 含 `projectId` 存在 → 201 + body 富化 `projectName` / `projectCode`
- [ ] `POST /api/requirements` 含 `projectId=null` → 201（兼容保持）
- [ ] **`PUT /api/requirements/{id}` body 含 `ownerUserId` = 另一存在用户 id → 200 + body.ownerUserId 更新（owner 现在可改）**
- [ ] **`PUT /api/requirements/{id}` body 含 `ownerUserId` 不存在 → 400 "owner user not found"**
- [ ] `POST /api/user-roles` 含 `projectId` 不存在 → 400 "project not found"
- [ ] `POST /api/user-roles` 含 `projectId=null` → 201（公司级 hat 仍允许）
- [ ] **应用启动后日志含 "cleaned dangling project_id from rainier_user_role.2"（自愈 user_role.id=2 projectId=42 → null）**
- [ ] **应用启动后 `GET /api/user-roles/2` → 200 + body.projectId=null + projectName=null（已自愈，无脏数据）**
- [ ] 浏览器 `/pm/projects` 列表 / 新建 / 编辑 / 删除 全功能可用
- [ ] Sider「需求管理」组新增「项目」菜单项可点击，位于「诉求」之前
- [ ] `ProjectsPage` 编辑抽屉「负责人」下拉异步加载，新建时默认选中当前登录用户
- [ ] `ProjectsPage` 编辑现有项目时「负责人」下拉**不 disabled**，可改成任意其他用户
- [ ] `RequirementEditDrawer` 「项目」下拉异步加载并保存
- [ ] `UserRolesPage` 「项目」下拉异步加载，留白 → null 保存为公司级 hat
- [ ] `RequirementsPage` 列表新增「项目」列；`UserRolesPage` 列表新增「项目」列
- [ ] `grep -rn 'is_pmo\|isPmo' backend/src/main/java frontend/src` 仍 0 行（v0.0.5 baseline）
- [ ] `grep -rn 'BaseAutoIdEntity' backend/src` 仍 0 行（v0.0.6 baseline）
- [ ] v0.0.7 baseline 保持：岗位 / 角色 / 用户角色 3 页全功能 + UsersPage 岗位下拉
- [ ] v0.0.6 baseline 保持：诉求 / 需求 / 关联 3 页全功能（注意：本次会改 RequirementEditDrawer 的 projectId 控件 + 加列）
