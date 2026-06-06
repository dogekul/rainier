# v0.0.7-position-role 任务清单

## 1. entity-position Capability（P0）

### 1.1 Position 链路 + 测试（M01）

- [x] 1.1.1 常量类 `com.rainier.position.domain.PositionCategory` — `TECH / BIZ / PM / MGMT / OTHER` + `Set<String> ALL`（Collections.unmodifiableSet 写法）
- [x] 1.1.2 `Position` entity：继承 BaseEntity；字段 code / name / description / category / enabled；@SQLDelete + @Where("del_flag = 0")
- [x] 1.1.3 `PositionRepository` extends JpaRepository<Position, Long>, JpaSpecificationExecutor<Position>；含 `existsByCode`
- [x] 1.1.4 DTO：`PositionCreateRequest` / `PositionUpdateRequest`（**无 code 字段**）/ `PositionDetail`
- [x] 1.1.5 `PositionService`：create（校验 code 唯一 + category 集合 + 默认值 enabled=true）/ findById / list（含 category + enabled + search）/ update（不改 code）/ delete（FK 保护：注入 UserRepository.countByPositionId > 0 → 409）
- [x] 1.1.6 `PositionController`：5 endpoint
- [x] 1.1.7 测试 `PositionControllerCreateTest`：TC-POS-001/002/003/004 (4 case)
- [x] 1.1.8 测试 `PositionControllerQueryTest`：TC-POS-005/006/007/008 (4 case)
- [x] 1.1.9 测试 `PositionControllerDeleteTest`：TC-POS-009/010 (2 case)
- [x] 1.1.10 mvn compile + test 全绿（+10）

## 2. entity-role Capability（P0）

### 2.1 Role 链路 + 测试（M02）

- [x] 2.1.1 `Role` entity：继承 BaseEntity；字段 code / name / description / enabled；@SQLDelete + @Where
- [x] 2.1.2 `RoleRepository` extends JpaRepository<Role, Long>, JpaSpecificationExecutor<Role>；含 `existsByCode`
- [x] 2.1.3 DTO：`RoleCreateRequest` / `RoleUpdateRequest`（**无 code**）/ `RoleDetail`
- [x] 2.1.4 `RoleService`：create（校验 code 唯一）/ findById / list（含 enabled + search）/ update（不改 code）/ delete（FK 保护：注入 UserRoleRepository.countByRoleId > 0 → 409；此 repo 在 M03 才存在 — 因此 RoleService 构造在 M02 暂时 stub，M03 再补回真实 count）
- [x] 2.1.5 `RoleController`：5 endpoint
- [x] 2.1.6 测试 `RoleControllerCreateTest`：TC-ROL-001/002/003 (3 case)
- [x] 2.1.7 测试 `RoleControllerQueryTest`：TC-ROL-004/005/006 (3 case)
- [x] 2.1.8 测试 `RoleControllerDeleteTest`：TC-ROL-007/008 (2 case)
- [x] 2.1.9 mvn test 全绿（+8）

## 3. entity-user-role Capability（P0）

### 3.1 UserRole 链路 + 测试（M03）— 依赖 #2

- [x] 3.1.1 `UserRole` entity：继承 BaseEntity 但**不**加 @SQLDelete；字段 user_id / role_id / project_id；@Table(uniqueConstraints = @UniqueConstraint(name="uk_user_role_user_role_project", columnNames = {"user_id", "role_id", "project_id"}))
- [x] 3.1.2 `UserRoleRepository`：`countByRoleId(Long)` / `countByUserId(Long)`（备用）/ `existsByUserIdAndRoleIdAndProjectId(Long, Long, Long)` / `existsByUserIdAndRoleIdAndProjectIdIsNull(Long, Long)` / `findAll` 配 Specification
- [x] 3.1.3 DTO：`UserRoleCreateRequest` / `UserRoleDetail`（富化字段 userName/userLoginName/roleName/roleCode）
- [x] 3.1.4 `UserRoleService`：create（校验 user FK + role FK + 双层唯一性 + try/catch DataIntegrityViolationException → ConflictException）/ findById / list（按 userId/roleId/projectId 过滤 + 富化 join user/role）/ delete（硬删）
- [x] 3.1.5 `UserRoleController`：4 endpoint（无 PUT）
- [x] 3.1.6 回补 `RoleService.delete` 真实 FK 保护（用 UserRoleRepository.countByRoleId）
- [x] 3.1.7 测试 `UserRoleControllerCreateTest`：TC-UROL-001..007 (7 case)
- [x] 3.1.8 测试 `UserRoleControllerQueryTest`：TC-UROL-008 (1 case)
- [x] 3.1.9 测试 `UserRoleControllerDeleteTest`：TC-UROL-009 (1 case)
- [x] 3.1.10 mvn test 全绿（+9）

## 4. entity-user MODIFIED Capability（P0）

### 4.1 User 改造 + 测试（M04）— 依赖 #1

- [x] 4.1.1 `User` entity 加 `@Column(name = "position_id") private Long positionId;` + getter/setter
- [x] 4.1.2 `UserRepository` 加 `countByPositionId(Long positionId)`（供 Position FK 保护）
- [x] 4.1.3 `UserCreateRequest` 加 `private Long positionId;` + getter/setter
- [x] 4.1.4 `UserUpdateRequest` 加 `private Long positionId;`（**注意**：本字段允许显式 null → 用包装 Long 自动支持）
- [x] 4.1.5 `UserDetail` 加 `positionId` / `positionName` / `positionCategory` 字段 + getters + 修改静态工厂 `from(User u)` 与新 `from(User u, Position p)` 重载（或在 service 中 setter 富化）
- [x] 4.1.6 `UserService` 注入 `PositionRepository`；create + update 接受 `positionId`（非空 → 校验 `positionRepo.existsById` → 不存在 → BadRequestException "position not found"；可空 → 设 null 清空）；findById + list 富化 positionName/positionCategory
- [x] 4.1.7 `UserController` 无需改（透传 DTO）
- [x] 4.1.8 回补 `PositionService.delete` 用 UserRepository.countByPositionId 真实校验
- [x] 4.1.9 扩展 `UserControllerTest`：TC-USR-001/002/003/004 (4 case，含富化 + null 清空)
- [x] 4.1.10 mvn test 全绿（+4）

## 5. frontend-scaffold MODIFIED Capability（P0）

### 5.1 API 类型（M05）

- [x] 5.1.1 `frontend/src/api/position.ts`：interfaces Position / PositionCreate / PositionUpdate；functions listPositions / getPosition / createPosition / updatePosition / deletePosition
- [x] 5.1.2 `frontend/src/api/role.ts`：interfaces Role / RoleCreate / RoleUpdate；functions list/get/create/update/delete
- [x] 5.1.3 `frontend/src/api/userRole.ts`：interfaces UserRoleLink / UserRoleLinkCreate（含可选 projectId: number | null）；functions listUserRoles / getUserRole / createUserRole / deleteUserRole
- [x] 5.1.4 tsc -b 通过

### 5.2 PositionsPage（M06）— 依赖 #5.1

- [x] 5.2.1 `frontend/src/pages/Position/PositionsPage.tsx`：列表 + 新建/编辑/删除抽屉
- [x] 5.2.2 `PositionEditDrawer` 表单：code / name / description / category（select）/ enabled（checkbox）
- [x] 5.2.3 `frontend/src/pages/Position/index.tsx` 导出

### 5.3 RolesPage（M07）— 依赖 #5.1

- [x] 5.3.1 `frontend/src/pages/Role/RolesPage.tsx`：列表 + CRUD（无 category）
- [x] 5.3.2 `RoleEditDrawer`：code / name / description / enabled
- [x] 5.3.3 `frontend/src/pages/Role/index.tsx`

### 5.4 UserRolesPage（M08）— 依赖 #5.1

- [x] 5.4.1 `frontend/src/pages/UserRole/UserRolesPage.tsx`：列表富化展示 + 新建抽屉
- [x] 5.4.2 抽屉表单：user select + role select + projectId 数字输入框（留白时 POST 传 null，非空时传 number）
- [x] 5.4.3 `frontend/src/pages/UserRole/index.tsx`

### 5.5 UsersPage 改造 + 测试（M09）— 依赖 #4, #5.1

- [x] 5.5.1 `frontend/src/pages/User/UsersPage.tsx` 编辑抽屉新增「岗位」select（异步 listPositions → 显示 name + category）；保存 body 含 positionId
- [x] 5.5.2 同文件列表新增「岗位」列：`render: (r) => r.positionName ? \`${r.positionName} (${r.positionCategory})\` : '—'`
- [x] 5.5.3 新建 `UsersPage.test.tsx`：mock listPositions 返回 2 条 + mock createUser → 选择 id=1 → 保存 → assert createUser 收到 body.positionId=1 → **TC-FES-H03**
- [x] 5.5.4 vitest 通过

### 5.6 AppLayout + AppRoutes + 测试（M10）— 依赖 #5.2, #5.3, #5.4, #5.5

- [x] 5.6.1 `AppLayout.tsx`：navGroups 末尾追加 `{key:'hr', title:'人事配置', items:[岗位/角色/用户角色]}`
- [x] 5.6.2 `AppRoutes.tsx`：注册 4 路由（`/hr` redirect / `/hr/positions` / `/hr/roles` / `/hr/user-roles`）
- [x] 5.6.3 扩展 `AppLayout.test.tsx` 加 TC-FES-H01：含「人事配置」组 + 3 子项 + 点击「岗位」跳 `/hr/positions`
- [x] 5.6.4 扩展 `AppRoutes.test.tsx` 加 TC-FES-H02：mount MemoryRouter at `/hr/positions` → 找 PositionsPage 元素（PositionsPage 加 `data-testid="positions-new-btn"`）
- [x] 5.6.5 grep 校验 `grep -c "/hr/positions" frontend/src/AppRoutes.tsx >= 1`
- [x] 5.6.6 vitest 19+5=24 全绿（含 TC-FES-H01/H02/H03 + AppRoutes 新增 2 mount sanity）

## 6. E2E 验证 + 验收（M11）

- [x] 6.1 `docker compose down -v && RAINIER_BACKEND_HOST_PORT=18080 docker compose up -d --build` 起栈，3 服务 healthy
- [x] 6.2 `docker exec rainier-mysql mysql -urainier -prainier rainier -e "SHOW TABLES"` 含 `rainier_{organization, user, user_organization, demand, requirement, demand_requirement, position, role, user_role}` 9 张表
- [x] 6.3 `DESCRIBE rainier_position` / `rainier_role` / `rainier_user_role`：id 列 `bigint auto_increment`；FK 列 (user_id, role_id) bigint；project_id bigint nullable
- [x] 6.4 `DESCRIBE rainier_user` 含新 `position_id bigint Null=YES` 列
- [x] 6.5 curl 端到端：建 Position（id=1，code="BE_ENG"，category="TECH"）→ 建 Role（id=1，code="PMO"）→ POST User w/ positionId=1 → 确认 GET 返 positionName + positionCategory → POST UserRole projectId=null → 201 → POST 同对（projectId=null）→ 409 → POST UserRole projectId=42 → 201 → DELETE Position 1 → 409 → DELETE UserRole 1 → 204
- [x] 6.6 `grep -rn 'is_pmo\|isPmo' backend/src/main/java backend/src/main/resources/application*.yml frontend/src` 仍 0 行（v0.0.5 baseline 守护）
- [x] 6.7 `grep -rn 'BaseAutoIdEntity' backend/src` 仍 0 行（v0.0.6 baseline 守护）
- [x] 6.8 全量 mvn test (≥ 119) + npm test (≥ 24) + npm run build + npm run lint + spotless + checkstyle 全绿

## 7. 切片完成度对照

| 切片 | TC 覆盖 | 任务编号 |
|---|---|---|
| M01 | TC-POS-001..010 | 1.1.1-1.1.10 |
| M02 | TC-ROL-001..008 | 2.1.1-2.1.9 |
| M03 | TC-UROL-001..009 | 3.1.1-3.1.10 |
| M04 | TC-USR-001..004 | 4.1.1-4.1.10 |
| M05 | (前置) | 5.1.1-5.1.4 |
| M06 | (UI) | 5.2.1-5.2.3 |
| M07 | (UI) | 5.3.1-5.3.3 |
| M08 | (UI) | 5.4.1-5.4.3 |
| M09 | TC-FES-H03 | 5.5.1-5.5.4 |
| M10 | TC-FES-H01, TC-FES-H02 | 5.6.1-5.6.6 |
| M11 | (E2E) | 6.1-6.8 |
