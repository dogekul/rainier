# 岗位（Position）+ 角色（Role）双轴建模 — 技术设计

## Context

- **代码基线**：v0.0.6-demand-requirement（commit 8edab14）— 6 张表（org/user/uo/demand/requirement/demand_requirement）+ 单 BaseEntity + BIGINT IDENTITY 标准已稳定
- **技术栈**：Spring Boot 2.7.18（Java 8）+ MySQL 8 + Hibernate JPA（ddl-auto=update，**Flyway 已禁用**）+ React 18 + Vite + TypeScript + Zustand + Axios
- **已就绪复用资产**：
  - `BaseEntity`（Long id + 5 audit + del_flag）
  - 异常体系：`BadRequestException` / `ConflictException` / `NotFoundException` / `GlobalExceptionHandler`
  - `PageParams` / `PageResponse`
  - v0.0.6 已建立的 enum 字符串模式（`Set<String> ALL` + Service.contains 校验）
  - v0.0.6 已建立的 M2M 硬删 + 唯一约束模式（`DemandRequirementLink`）
  - v0.0.6 已建立的 `project_id` 占位 FK 模式（`Requirement.project_id`）
  - 前端组件库 / `usePaginated` / `client.ts` axios
- **约束**：
  - 不引入新 Maven / npm 依赖
  - 不动 v0.0.3 / v0.0.6 任何已交付 entity（UserOrganization.role=HEAD/MEMBER 保持原义）
  - 不写 V3 SQL 历史档（沿用 v0.0.5/v0.0.6 教训）

## Decisions

### 1. 岗位归属 — Position 单字段挂 User，不引入 user_position M2M

**方案**：`rainier_user` 加 `position_id BIGINT NULL FK rainier_position(id)`；一个用户最多 1 个岗位（可空 = 未定级）。`User` entity / DTO / Service 改造接收 positionId；Detail 富化 `positionName` + `positionCategory`。

**为什么**：
- v0 业务场景下"一人多岗"罕见；单字段够用
- 写路径简单：UserCreateRequest / UserUpdateRequest 加一个字段即可；不引入新表
- 读路径已用 v0.0.3 风格的 enrichment（参考 `SourceDemandView`）— Service 在返回 UserDetail 时按 positionId 查 Position 并富化两字段
- 未来"一人多岗"需求真出现时再扩为 `user_position` M2M，配 migration 拆字段；代价可控

**备选方案及排除原因**：
- 备选 A — `rainier_user_position` M2M：v0 无需求，且 PMO 用例只需要 Role；过度设计
- 备选 B — 把 Position 挂在 `UserOrganization.position_id`：违背"岗位是人的标签"语义（人在不同组织里同一职位的本质不变）；且 UO 是 M2M 表，会让"同一人在多个组织里岗位不同"成为常态，引入数据漂移风险

### 2. 角色作用域 — UserRole 通过 project_id 占位 FK 关联 Project（无 service 校验）

**方案**：`rainier_user_role` M2M 表含 `user_id BIGINT NN + role_id BIGINT NN + project_id BIGINT NULL`；`project_id` 是占位字段，**无 FK 约束、无 Service 不存在性校验**，v0 阶段任意 BIGINT 值都接受；`project_id IS NULL` 表示"未指派项目 / 跨项目通用 hat"。

**为什么**：
- 沿用 v0.0.6 `Requirement.project_id` 同款占位模式 — 项目内已有先例，认知一致
- Project 实体引入是 v0.0.8 的工作；本变更先建好关系骨架，等 Project 落地后做数据清理 + 给 Service 加校验
- NULL allowed 让 v0 用户能立即测试 user-role 流程（不需要先建 Project）

**备选方案及排除原因**：
- 备选 A — 用 `organization_id` 作 Role 作用域：与"PMO of Project X"语义不匹配；项目和组织是正交维度
- 备选 B — 直接引入 Project 实体：scope 飞溅；Project 是大块（生命周期 / 成员 / 状态机），独立 change 更清晰

### 3. UserRole 唯一性 — DB UNIQUE + Service NULL 兜底双保险

**方案**：
- DB 层：`@UniqueConstraint(columnNames = {"user_id", "role_id", "project_id"})` — 防 non-NULL 重复
- Service 层：create 时，若 `project_id IS NULL`，先查 `existsByUserIdAndRoleIdAndProjectIdIsNull(userId, roleId)` → true 则抛 `ConflictException("user-role already exists")`；若 non-NULL，先查 `existsByUserIdAndRoleIdAndProjectId(...)` → true 抛 409
- 再 `saveAndFlush` 用 try/catch `DataIntegrityViolationException` 兜底竞态（同 v0.0.6 `DemandRequirementLink` H-1 修复模式）

**为什么**：
- MySQL UNIQUE 约束允许多个 NULL 行（标准行为）→ 对 `(alice, PMO, NULL)` 重复无保护
- Service 层 IS NULL 查询 + sed 校验是唯一可靠的 NULL 兜底
- DB UNIQUE 仍然保留：non-NULL 路径享 DB 原子性 + 防服务竞态
- 双保险冗余但每层独立、明确

**备选方案及排除原因**：
- 备选 A — 用 0 作"未指派"哨兵替代 NULL：丑陋；和占位 FK 的"允许任意 BIGINT 包括 0"语义打架
- 备选 B — MySQL 8 functional index `(user_id, role_id, COALESCE(project_id, 0))`：Hibernate ddl-auto 不生成；放弃

### 4. Position / Role 删除保护 — Position 看 User.position_id；Role 看 UserRole.role_id

**方案**：
- `DELETE /api/positions/{id}`：`userRepo.countByPositionId(id) > 0` → 409 `"position has assigned users"`
- `DELETE /api/roles/{id}`：`userRoleRepo.countByRoleId(id) > 0` → 409 `"role has assignments"`
- `DELETE /api/user-roles/{id}`：M2M 叶子，无下游，直接硬删（参考 v0.0.6 `DemandRequirementLink.delete`）

**为什么**：与 v0.0.3 Organization → UserOrganization、v0.0.6 Demand/Requirement → DemandRequirementLink 的 FK 保护模式完全对齐 — 一种模式贯穿全栈，认知零负担。

### 5. UserDetail 富化 — Service.findById 时按 positionId 查 Position，附 positionName + positionCategory

**方案**：`UserService.findById` 内部：
```java
User u = repo.findById(id).orElseThrow(...);
UserDetail dto = UserDetail.from(u);
if (u.getPositionId() != null) {
  positionRepo.findById(u.getPositionId()).ifPresent(p -> {
    dto.setPositionName(p.getName());
    dto.setPositionCategory(p.getCategory());
  });
}
```
列表场景同样在 `list()` 内做（每条 enrich；可接受 N+1，v0 数据量小，N+1 已是 v0.0.6 既定 KL）。

**为什么**：
- 一次查询能给前端足够信息渲染列表 / 详情，不让前端做 join
- 与 v0.0.6 `SourceDemandView` / `DerivedRequirementView` 富化模式一致

### 6. UserOrganization.role 命名冲突 — 显式保留 + 文档澄清，不重命名

**方案**：v0.0.3 `UserOrganization.role: UserOrgRole(HEAD/MEMBER)` 字段保持原样不动；新引入 `com.rainier.role.domain.Role` 实体是完全独立的概念。两者不共享类名（`UserOrgRole` 是 enum；`Role` 是 entity），不在同一包，导入时无歧义。

**为什么**：
- 重命名 v0.0.3 字段会扩大 scope，并触发 4 个文件 + 10 个测试 + 1 个 frontend 类型同步改动
- 命名虽然容易混淆，但语义不同（层级位 vs 功能 hat）反而值得保留两个概念的独立性
- 在 Spec / Design / 主规范中显式说明并存关系

**备选方案及排除原因**：
- 备选 A — 重命名 `UserOrgRole` → `OrgHierarchyPosition` 或 `UserOrgRank`：破坏 v0.0.6 baseline；ROI 不高

### 7. 路由前缀 — `/hr/*` 独立 namespace

**方案**：
- 新菜单组「人事配置」+ 路由前缀 `/hr/*`：`/hr` → 重定向 `/hr/positions`；`/hr/positions` / `/hr/roles` / `/hr/user-roles`
- AppLayout `navGroups` 末尾新增 `{key: 'hr', title: '人事配置', items: [...]}`，位于「需求管理」之后

**为什么**：
- `/pm/*` 已被需求管理（3 项）占住，再塞会让"PM = Project Management"语义被稀释
- `/hr/*`（Human Resources）和岗位/角色概念完美匹配
- Sider 视觉分组按业务领域划分更直观

**备选方案及排除原因**：
- 备选 A — 复用 `/pm/*`：语义混淆
- 备选 B — 把 positions 放 `/org/*`（视为组织/HR 配置）：UO 已在 /org，再加会和"组织结构"分组混乱

### 8. 常量类 — 仅 PositionCategory；Role / UserRole 无 enum

**方案**：
- `com.rainier.position.domain.PositionCategory`：`TECH / BIZ / PM / MGMT / OTHER` + `Set<String> ALL`，Service.create/update 校验
- Role 无 category enum（功能 hat 自由命名）
- UserRole 无任何状态字段（仅 user_id / role_id / project_id）

**为什么**：
- v0 Position 分类已能覆盖 5 大常见职能；多了反而决策疲劳
- Role 的"hat 类型"难以预先穷举（业务自由定义 PMO/TechLead/Reviewer/…）

### 9. 包结构 — `com.rainier.{position, role, userrole}`

**方案**：
- `com.rainier.position.{domain, dto, repository, service, controller}`
- `com.rainier.role.{domain, dto, repository, service, controller}`
- `com.rainier.userrole.{domain, dto, repository, service, controller}`（包名无连字符，沿用 v0.0.6 `demandrequirement` 命名）
- Entity 类名：`Position` / `Role` / `UserRole`

**为什么**：与 v0.0.6 `com.rainier.{demand, requirement, demandrequirement}` 风格一致；包名无连字符是 Java 约定。

### 10. 测试策略 — 集成 MockMvc 为主；UserRole 唯一性测试两路径（NULL / non-NULL）

**方案**：
- 后端测试 `@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")` 集成 MockMvc 风格（沿用 v0.0.6）
- 关键非 trivial 测试：
  - UserRole 唯一性：`(alice, PMO, NULL)` 重复 → 409；`(alice, PMO, 42)` 重复 → 409；`(alice, PMO, NULL)` 和 `(alice, PMO, 42)` 共存 → OK
  - Position 删除保护：被任意 User 引用 → 409；无引用 → 204
  - User 富化：GET 返回 positionName / positionCategory
- 前端 vitest：3 页表头 + UsersPage 编辑抽屉岗位下拉断言

## Architecture

```
┌─ 写入链路 ──────────────────────────────────────────────────────────┐
│                                                                      │
│  POST /api/positions ─▶ PositionController ─▶ PositionService.create │
│                                                · code 唯一校验 (svc) │
│                                                · category 集合校验  │
│                                                · saveAndFlush         │
│                                                                       │
│  POST /api/user-roles ─▶ UserRoleController ─▶ UserRoleService.create│
│                                                · user/role FK 存在性  │
│                                                · NULL 路径: IS NULL  │
│                                                  查询 + service 兜底  │
│                                                · non-NULL: DB UNIQUE │
│                                                  + try/catch         │
│                                                  DataIntegrity..     │
│                                                                       │
│  PUT /api/users/{id} ─▶ UserController ─▶ UserService.update         │
│       (含可选 positionId)              · 若 positionId != null →     │
│                                          positionRepo.existsById     │
│                                          (位置不存在 → 400)           │
│                                        · u.setPositionId(...)        │
└──────────────────────────────────────────────────────────────────────┘

┌─ 读取链路（富化） ─────────────────────────────────────────────────┐
│                                                                      │
│  GET /api/users/{id} ─▶ UserController ─▶ UserService.findById       │
│                                            · UserDetail.from(u)      │
│                                            · if (u.positionId != null)│
│                                              positionRepo.findById   │
│                                              → set positionName /    │
│                                                 positionCategory     │
│                                                                       │
│  GET /api/user-roles ─▶ UserRoleController ─▶ UserRoleService.list   │
│                          (filter: userId/roleId/projectId)            │
│                          · 每行 enrich → UserRoleDetail with         │
│                            userName / userLoginName /                │
│                            roleName / roleCode                       │
└──────────────────────────────────────────────────────────────────────┘

┌─ 删除保护链路 ────────────────────────────────────────────────────┐
│                                                                      │
│  DELETE /api/positions/{id} ─▶ PositionController ─▶ Service.delete   │
│                                  · count User where position_id=id   │
│                                  · > 0 → 409                         │
│                                  · = 0 → softDelete (@SQLDelete)     │
│                                                                       │
│  DELETE /api/roles/{id} ─▶ RoleController ─▶ Service.delete           │
│                              · count UserRole where role_id=id        │
│                              · > 0 → 409                              │
│                                                                       │
│  DELETE /api/user-roles/{id} ─▶ Controller ─▶ Service.delete         │
│                                   · 硬删 repo.delete                  │
└──────────────────────────────────────────────────────────────────────┘

┌─ 前端 ──────────────────────────────────────────────────────────────┐
│                                                                      │
│  AppLayout (Sider) ─▶「人事配置」菜单组                              │
│       ├─ 岗位         ─▶ /hr/positions    PositionsPage              │
│       ├─ 角色         ─▶ /hr/roles        RolesPage                  │
│       └─ 用户角色     ─▶ /hr/user-roles   UserRolesPage              │
│                                                                      │
│  UsersPage (现有，编辑抽屉新增字段):                                │
│       · 「岗位」下拉 (异步 listPositions → 显示 name / code)         │
│       · 列表新增「岗位」列 (render positionName + category)          │
│                                                                      │
│  UserRolesPage:                                                      │
│       · 列表富化 userName / userLoginName / roleName / roleCode       │
│       · 新建抽屉 双 select user + role + projectId (number 输入)     │
└──────────────────────────────────────────────────────────────────────┘
```

## Risks / Trade-offs

| 风险 | 缓解措施 |
|---|---|
| UserRole.project_id 无校验 → 用户可能写入任意脏 BIGINT | 沿用 v0.0.6 `Requirement.project_id` 同款模式；v0.0.8 Project 落地后做数据清理 + 加校验；test-report 显式列入 KL |
| UserRole 唯一性 NULL 兜底依赖 service 单点 → 并发竞态绕过 | 双层防御：service IS NULL 查询 + DB UNIQUE (non-NULL 路径) + try/catch DataIntegrityViolationException；TC-UROL-005 显式覆盖 |
| UserOrgRole(HEAD/MEMBER) vs 新 Role 实体命名混淆 | 显式 design / spec 文档；entity 类放不同包；测试用例命名带前缀（`UserRole...` vs `UserOrganization.role...`） |
| User 富化 N+1 query (list 场景每行查 Position) | 已是 v0.0.6 既定 KL；v0 数据量小；未来引入 `findAllById` 批量优化 |
| Hibernate ddl-auto 加 NOT NULL 列到现有 rainier_user 表时遇到现有数据 | position_id 是 NULL allowed，无影响；现有 alice/bob 等行 position_id 自动为 NULL |
| Phase 4 工作量约 ~60 文件，Phase 5 review 可能命中较多 H/M（参考 v0.0.6 的 4 H） | 沿用长程模式 + 多路 review；预留 1-2 轮自动修复迭代 |
| frontend `AppRoutes.tsx` 加 4 新路由 + Sider 加菜单组 | 仿 v0.0.6 TC-FES-D02 模式：mount 测试 + grep `/hr/positions` 兜底 |
