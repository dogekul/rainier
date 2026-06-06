# v0.0.7-position-role 切片执行计划

| # | 优先级 | TC 覆盖 | 实现目标 | 依赖 |
|---|---|---|---|---|
| M01 | P0 | TC-POS-001..010 (10) | Position 链路完整：`PositionCategory` 常量类 + `Position` entity（继承 BaseEntity，@SQLDelete 软删）+ 4 DTO（Create/Update/Detail + 默认值处理）+ Repository（含 `existsByCode` + 测试 cleanup 工具）+ Service（create + 校验 enum + 校验 code 唯一 / update / list / FK 保护 delete）+ Controller（5 endpoint）+ MockMvc 测试 10 case；mvn compile + test 通过 | 无 |
| M02 | P0 | TC-ROL-001..008 (8) | Role 链路完整：`Role` entity（@SQLDelete 软删）+ 4 DTO + Repository + Service（无 enum，但保留 code 唯一性 + FK 保护 delete）+ Controller + MockMvc 测试 8 case | 无 |
| M03 | P0 | TC-UROL-001..009 (9) | UserRole 链路：`UserRole` entity（继承 BaseEntity，**不**加 @SQLDelete，硬删）+ `@UniqueConstraint(user_id, role_id, project_id)` + 3 DTO + Repository（含 `existsByUserIdAndRoleIdAndProjectId` 与 `existsByUserIdAndRoleIdAndProjectIdIsNull` 两路径 + `countByRoleId`）+ Service（双层防御：service IS NULL 兜底 + DB UNIQUE + try/catch DataIntegrityViolationException + 富化 list join user/role）+ Controller（4 endpoint） + MockMvc 测试 9 case（含 NULL 双路径 + 占位字段语义） | M02（Role 实体） |
| M04 | P0 | TC-USR-001..004 (4) | User MODIFIED：`User` entity 加 `position_id` 字段 + getter/setter；UserCreateRequest / UserUpdateRequest 加 `positionId`；UserDetail 富化 `positionName` / `positionCategory`；UserService.create/update 接 positionId（可空校验 Position 存在）；UserService.findById/list enrichment（按 positionId 富化）；UserController 透传；扩展 UserControllerTest 加 4 case；UserService 注入 PositionRepository；UserRepository 加 `countByPositionId` 供 Position FK 保护使用 | M01（Position 实体） |
| M05 | P0 | (前置) | 前端 API 类型层：`frontend/src/api/{position,role,userRole}.ts` 三 module — Position/PositionCreate/PositionUpdate + Role/RoleCreate/RoleUpdate + UserRoleLink/UserRoleLinkCreate；所有 functions list/get/create/update/delete；UserRoleLinkCreate 含可选 `projectId: number \| null`；UserRoleLink 富化字段 userName/userLoginName/roleName/roleCode | 无 |
| M06 | P0 | (UI) | 前端 PositionsPage：`/hr/positions` 列表 + 新建/编辑/删除抽屉 + category 下拉；列：code/name/category/enabled/createTime；复用 v1 Table/Pagination/Drawer/ConfirmDialog/usePaginated | M05 |
| M07 | P0 | (UI) | 前端 RolesPage：`/hr/roles` 列表 + CRUD（无 category） | M05 |
| M08 | P0 | (UI) | 前端 UserRolesPage：`/hr/user-roles` 列表（富化 userName/roleName 展示）+ 新建抽屉（双 select user/role + projectId 数字输入框，留白=null）+ 删除（硬删） | M05 |
| M09 | P0 | TC-FES-H03 | 前端 UsersPage 改造 + 测试：UsersPage 编辑抽屉新增「岗位」select（异步 listPositions）→ 保存 body 含 positionId；UsersPage 列表新增「岗位」列（render positionName + category）；新建 `frontend/src/pages/User/UsersPage.test.tsx` 验证 TC-FES-H03（mock listPositions 返回 2 条 → 选 id=1 → 保存 → mock createUser 收到 body.positionId=1） | M04, M05 |
| M10 | P0 | TC-FES-H01, TC-FES-H02 | 前端 AppLayout 改造 + AppRoutes 改造 + 测试：AppLayout `navGroups` 末尾 push `{key:'hr', title:'人事配置', items:[岗位/角色/用户角色]}`；AppRoutes 加 4 路由（`/hr` redirect 到 `/hr/positions`、`/hr/positions`、`/hr/roles`、`/hr/user-roles`）；扩展 AppLayout.test.tsx 加 TC-FES-H01；扩展 AppRoutes.test.tsx 加 TC-FES-H02（含 grep `/hr/positions` AppRoutes.tsx ≥ 1 bash 兜底）；vitest 19+5=24 全绿 | M06, M07, M08, M09 |
| M11 | P0 | (E2E) | E2E 验证：`docker compose down -v && up -d --build` 起栈；`SHOW TABLES` 9 张（v0.0.6 的 6 + 本变更 3 = position/role/user_role）；DESCRIBE 3 新表 + rainier_user 含 position_id BIGINT NULL；curl 端到端（建 Position → 建 Role → POST User w/ positionId → 富化检查 → POST UserRole projectId=null → POST 同对 → 409 → POST projectId=42 → 201 → DELETE Position w/ User 引用 → 409 → DELETE UserRole → 204）；grep `is_pmo\|isPmo` / `BaseAutoIdEntity` 仍 0 行 | M01..M10 |

## 执行顺序图

```
M01 (Position 链)
   │
   ├── M04 (User MODIFIED) ← 也依赖 M01
   │
M02 (Role 链)
   │
   └── M03 (UserRole 链)
   
M05 (前端 API 类型) — 可与 M01/M02/M03/M04 并行
   │
   ├── M06 (PositionsPage)
   ├── M07 (RolesPage)
   ├── M08 (UserRolesPage)
   └── M09 (UsersPage 改造 + 测试) ← 依赖 M04, M05
         │
         └── M10 (AppLayout + AppRoutes + 测试) ← 依赖 M06, M07, M08, M09
               │
               └── M11 (E2E + DESCRIBE + curl) ← 同步点
```

**长程模式下顺序执行**：M01 → M02 → M03 → M04 → M05 → M06 → M07 → M08 → M09 → M10 → M11（串行 RED/GREEN/REFACTOR）。

## 关键技术 checklist（实现期常见陷阱预防）

- M01：PositionCategory 用 `Collections.unmodifiableSet(new HashSet<>(Arrays.asList(...)))`（Java 8 兼容；沿用 v0.0.6 教训）
- M01/M02：`code` 字段 `@Column(nullable = false, length = 64)` — **不**加 `unique = true`（沿用 v0.0.6 Requirement.code 决策，避免 soft-delete 残留挡 reuse）
- M01：DemandRepository.hardDeleteAll 是 v0.0.6 引入但未实际用上的模式 — 本次同样**不引入** hardDeleteAll；测试 cleanDb 直接用 deleteAll（业务测试不复用 code）
- M03：UserRole `@UniqueConstraint(name = "uk_user_role_user_role_project", columnNames = {"user_id", "role_id", "project_id"})` — 但 MySQL NULL 多重允许，需 service 配 IS NULL 路径
- M03：`existsByUserIdAndRoleIdAndProjectIdIsNull` 是 Spring Data JPA 派生查询 — 命名严格按 PropertyExpression（注意 `IsNull` 后缀大小写）
- M03：DataIntegrityViolationException 包 try/catch 时 **抛 ConflictException** 不要重新包 — 沿用 v0.0.6 link 修复
- M04：UserDetail 已有 `from(User)` 静态工厂；本次扩展时**加** `from(User, Position position)` 重载或者直接在 UserService 中 set
- M04：UserRepository 加 `countByPositionId` 为 M01 的 FK 保护使用 — **可能引入循环依赖** Position → User → Position；用法是 Position service 注入 UserRepository（v0.0.6 同款模式）
- M05：TS interface 中 `projectId?: number | null`（**注意 nullable 与 optional 双兼容**，避免 axios POST 时把 null 序列化成 undefined）
- M08：UserRolesPage 的 projectId 输入框留白时 POST body 传 null（不传 undefined）
- M09：UsersPage 改造时**保留** v0.0.3 + v0.0.6 既有字段；只**追加** position 下拉与列；不动 v0.0.5 删 isPmo 的事实
- M10：`AppRoutes.tsx` 添加路由前 import 4 个新页面组件；保存前 grep 兜底（v0.0.3 历史 linter 回退教训）
- M11：DESCRIBE 三张表的 FK 列均应为 `bigint`；rainier_user 表新加 position_id 应是 `bigint`/Null=YES
