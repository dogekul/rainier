# v0.0.8-project 任务清单

## 1. entity-project Capability（P0）

### 1.1 Project 链路（M01）

- [ ] 1.1.1 常量类 `com.rainier.project.domain.ProjectStatus`：5 状态 PLANNING/ACTIVE/ON_HOLD/DELIVERED/ARCHIVED + Set<String> ALL
- [ ] 1.1.2 `Project` entity（继承 BaseEntity，@SQLDelete + @Where）：字段 code / name / description / status / owner_user_id / start_date / end_date / enabled
- [ ] 1.1.3 `ProjectRepository` extends JpaRepository<Project,Long>, JpaSpecificationExecutor<Project>；含 `existsByCode`
- [ ] 1.1.4 DTO：`ProjectCreateRequest`（含 ownerUserId @NotNull）/ `ProjectUpdateRequest`（含 ownerUserId @NotNull 可改，无 code）/ `ProjectDetail`（含 ownerName/ownerLoginName 富化字段）
- [ ] 1.1.5 `ProjectService`：create（code/status/owner 校验 + 默认值 enabled=true + 富化）/ findById（富化）/ list（filter status/enabled + search by code/name）/ update（含 owner 校验 + 富化）/ delete stub（M02 加 FK 保护）
- [ ] 1.1.6 `ProjectController`：5 endpoint
- [ ] 1.1.7 测试 `ProjectControllerCreateTest`：TC-PRJ-001/002/003/004/005/006 (6 case)
- [ ] 1.1.8 测试 `ProjectControllerQueryTest`：TC-PRJ-007/008/009/010 (4 case)
- [ ] 1.1.9 mvn compile + test 全绿（+10 cases）

### 1.2 Project delete FK 保护（M02）— 依赖 #1.1

- [ ] 1.2.1 `RequirementRepository` 加 `countByProjectId(Long projectId)` 派生查询
- [ ] 1.2.2 `UserRoleRepository` 加 `countByProjectId(Long projectId)` 派生查询
- [ ] 1.2.3 `ProjectService` 注入 RequirementRepository + UserRoleRepository；`delete` 检查双向引用 → 409
- [ ] 1.2.4 测试 `ProjectControllerDeleteTest`：TC-PRJ-011/012/013 (3 case)
- [ ] 1.2.5 mvn test 全绿（+3 cases，累计 +13）

## 2. entity-requirement MODIFIED Capability（P0）

### 2.1 Requirement projectId 激活 + owner 可改（M03）— 依赖 #1.1

- [ ] 2.1.1 `RequirementUpdateRequest` 加 `@NotNull private Long ownerUserId;` + getter/setter
- [ ] 2.1.2 `RequirementService` 注入 ProjectRepository
- [ ] 2.1.3 `RequirementService.create`：若 `req.getProjectId() != null` → `projectRepo.existsById` 校验，不存在 → BadRequestException "project not found"
- [ ] 2.1.4 `RequirementService.update`：接收 ownerUserId；若与原 owner 不同 → 校验 user 存在 → 赋值；projectId 校验同 create
- [ ] 2.1.5 `RequirementDetail` 加 `projectName` / `projectCode` 字段 + getter/setter
- [ ] 2.1.6 `RequirementService.enrich` 富化 projectName/projectCode（findById；找到 → set；找不到 → 留 null，理论不会触发因 M05 自愈）
- [ ] 2.1.7 扩展 `RequirementControllerCreateTest`：TC-REQP-001/002/003 (3 case)
- [ ] 2.1.8 扩展 `RequirementControllerQueryTest`：TC-REQP-005/006 (2 case)；修改 v0.0.6 既有 TC-REQ-007（PUT ownerUserId 静默忽略）→ 新语义"PUT 可改 ownerUserId 成功"
- [ ] 2.1.9 mvn test 全绿（+5 cases）

## 3. entity-user-role MODIFIED Capability（P0）

### 3.1 UserRole projectId 激活（M04）— 依赖 #1.1

- [ ] 3.1.1 `UserRoleService` 注入 ProjectRepository
- [ ] 3.1.2 `UserRoleService.create`：若 `req.getProjectId() != null` → `projectRepo.existsById` 校验，不存在 → BadRequestException "project not found"
- [ ] 3.1.3 `UserRoleDetail` 加 `projectName` / `projectCode` 字段 + getter/setter
- [ ] 3.1.4 `UserRoleService.enrich` 富化 projectName/projectCode（理论 M05 自愈后无 dangling）
- [ ] 3.1.5 扩展 `UserRoleControllerCreateTest`：TC-URLP-001/002/003 (3 case)
- [ ] 3.1.6 mvn test 全绿（+3 cases）

## 4. DanglingProjectIdCleanup 启动自愈（M05）— 依赖 #2, #3

- [ ] 4.1 新建 `com.rainier.project.bootstrap.DanglingProjectIdCleanup` @Component implements CommandLineRunner
- [ ] 4.2 `@Transactional` `run()` 方法：注入 EntityManager；执行 2 条 native UPDATE：
  - `UPDATE rainier_requirement SET project_id = NULL WHERE project_id IS NOT NULL AND project_id NOT IN (SELECT id FROM rainier_project WHERE del_flag = 0)`
  - 同样 for rainier_user_role
  - 用 SLF4J Logger `org.slf4j.LoggerFactory` log WARN 每张表清理的行数
- [ ] 4.3 新建 `backend/src/test/java/com/rainier/project/bootstrap/DanglingProjectIdCleanupTest.java`：
  - @SpringBootTest @ActiveProfiles("test")
  - @Autowired DanglingProjectIdCleanup cleanup, RequirementRepository, UserRoleRepository, JdbcTemplate
  - @BeforeEach：直接 JdbcTemplate insert：1 user + 1 role + 1 demand + 1 requirement w/ projectId=999（无对应 Project）+ 1 user_role w/ projectId=888（无对应 Project）
  - @Test1 TC-REQP-004：cleanup.run() → assert requirement.projectId 变为 null
  - @Test2 TC-URLP-004：cleanup.run() → assert user_role.projectId 变为 null
- [ ] 4.4 mvn test 全绿（+2 cases）

## 5. frontend-scaffold MODIFIED Capability（P0）

### 5.1 API 类型（M06）

- [ ] 5.1.1 `frontend/src/api/project.ts`：interfaces Project / ProjectCreate / ProjectUpdate / ProjectStatus type + listProjects / getProject / createProject / updateProject / deleteProject

### 5.2 ProjectsPage（M07）— 依赖 #5.1

- [ ] 5.2.1 `frontend/src/pages/Project/ProjectsPage.tsx`：列表 + 新建/编辑/删除抽屉 + Sider 测试用 data-testid="projects-new-btn"
- [ ] 5.2.2 「负责人」select 异步 listUsers({ size: 100 })；新建模式默认选中 = users.find(u => u.loginName === useAuthStore.user.username)?.id || ''；编辑模式默认 = editing.ownerUserId；**不 disabled**
- [ ] 5.2.3 `frontend/src/pages/Project/index.tsx` 默认导出
- [ ] 5.2.4 新建 `frontend/src/pages/Project/ProjectsPage.test.tsx`：
  - mock auth store user.username="alice" + mock listUsers 返 [{id:1, loginName:"alice"}, {id:2, loginName:"lili"}] + mock createProject/updateProject
  - TC-FES-P03：新建 → 抽屉「负责人」select value === "1"
  - TC-FES-P04：编辑（mock getProject 返 ownerUserId=1）→ 切换到 2 → 保存 → assert updateProject body.ownerUserId === 2 + select disabled === false

### 5.3 Sider + AppRoutes 改造（M08）— 依赖 #5.2

- [ ] 5.3.1 `AppLayout.tsx`：navGroups「需求管理」items 前插 `{ to: '/pm/projects', label: '项目' }`
- [ ] 5.3.2 `AppRoutes.tsx`：注册 `/pm/projects` → ProjectsPage（import）
- [ ] 5.3.3 扩展 `AppLayout.test.tsx`：TC-FES-P01（项目位于诉求之前 + 跳转 /pm/projects）
- [ ] 5.3.4 扩展 `AppRoutes.test.tsx`：TC-FES-P02（mount /pm/projects → screen.getByTestId('projects-new-btn')）+ mock listProjects vi.mock
- [ ] 5.3.5 grep 校验 `grep -c "/pm/projects" frontend/src/AppRoutes.tsx >= 1`

### 5.4 RequirementEditDrawer 改造 + 列表项目列（M09）— 依赖 #5.1

- [ ] 5.4.1 `RequirementEditDrawer.tsx`：projectId 控件从「数字输入」改「Project 下拉」（listProjects size: 100；留白 → null）
- [ ] 5.4.2 同文件 owner select：`disabled={editing !== null}` → `disabled={false}`（owner 现在可改）
- [ ] 5.4.3 `RequirementsPage.tsx` 列表加「项目」列：render `r.projectName ? \`${r.projectName} (${r.projectCode})\` : '—'`

### 5.5 UserRolesPage 改造（M10）— 依赖 #5.1

- [ ] 5.5.1 `UserRolesPage.tsx`：projectId 控件从「数字输入」改「Project 下拉」（留白 → null = 公司级 hat）
- [ ] 5.5.2 同文件列表加「项目」列：render projectName + code 或 "（公司级）"
- [ ] 5.5.3 扩展或新建 `UserRolesPage.test.tsx`：mock listProjects + user/role 已选 + 项目留白 → 保存 → assert createUserRole body.projectId === null → TC-FES-P05
- [ ] 5.5.4 vitest 全绿（前端累计 25 + 5 = 30）

## 6. E2E 验证 + 验收（M11）

- [ ] 6.1 `RAINIER_BACKEND_HOST_PORT=18080 docker compose build backend frontend && docker compose up -d --no-deps --force-recreate backend frontend`（**不 down -v**）
- [ ] 6.2 等服务 healthy；docker logs rainier-backend | grep "cleaned dangling project_id" 含 user_role.2
- [ ] 6.3 `docker exec rainier-mysql mysql -e "SHOW TABLES"` 含 10 张表（v0.0.7 的 9 + rainier_project）
- [ ] 6.4 `DESCRIBE rainier_project`：id BIGINT auto_increment + code/name/description/status/owner_user_id BIGINT + start_date/end_date DATE + enabled + 6 审计 + del_flag
- [ ] 6.5 `docker exec mysql -e "SELECT id, project_id FROM rainier_user_role"` → id=2 行 project_id NULL（自愈生效）
- [ ] 6.6 curl 端到端：
  - POST Project (PROJ-001, "测试项目", ownerUserId=1) → 201 + ownerName="Alice"
  - POST Requirement w/ projectId=1 → 201 + projectName/projectCode 富化
  - POST Requirement w/ projectId=99 (不存在) → 400 "project not found"
  - POST UserRole w/ projectId=1 → 201 + projectName 富化
  - PUT Requirement 改 ownerUserId=2 → 200 + body.ownerUserId=2（owner 可改验证）
  - DELETE Project 1（已被 Requirement 引用）→ 409 "has linked requirements"
- [ ] 6.7 `grep -rn 'is_pmo\|isPmo' backend/src/main/java backend/src/main/resources/application*.yml frontend/src` 仍 0 行（v0.0.5 baseline）
- [ ] 6.8 `grep -rn 'BaseAutoIdEntity' backend/src` 仍 0 行（v0.0.6 baseline）
- [ ] 6.9 全量 mvn test (≥ 143) + npm test (≥ 30) + npm run build + npm run lint + spotless + checkstyle 全绿

## 7. 切片完成度对照

| 切片 | TC 覆盖 | 任务编号 |
|---|---|---|
| M01 | TC-PRJ-001..010 | 1.1.1-1.1.9 |
| M02 | TC-PRJ-011..013 | 1.2.1-1.2.5 |
| M03 | TC-REQP-001..003, 005..006 | 2.1.1-2.1.9 |
| M04 | TC-URLP-001..003 | 3.1.1-3.1.6 |
| M05 | TC-REQP-004, TC-URLP-004 | 4.1-4.4 |
| M06 | (前置) | 5.1.1 |
| M07 | TC-FES-P03, P04 | 5.2.1-5.2.4 |
| M08 | TC-FES-P01, P02 | 5.3.1-5.3.5 |
| M09 | (UI 改造) | 5.4.1-5.4.3 |
| M10 | TC-FES-P05 | 5.5.1-5.5.4 |
| M11 | (E2E) | 6.1-6.9 |
