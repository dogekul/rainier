# v0.0.8-project 切片执行计划

| # | 优先级 | TC 覆盖 | 实现目标 | 依赖 |
|---|---|---|---|---|
| M01 | P0 | TC-PRJ-001..010 (10) | Project 链路完整：`ProjectStatus` 常量类 + `Project` entity（继承 BaseEntity，@SQLDelete 软删）+ 4 DTO（Create/Update/Detail + ownerName/ownerLoginName 富化字段）+ Repository（含 `existsByCode`）+ Service（create + 校验 code 唯一 + status enum + ownerUserId 存在性 + 富化 enrich + update + list）+ Controller（5 endpoint）+ MockMvc 测试 10 case（不含 delete FK 保护） | 无 |
| M02 | P0 | TC-PRJ-011..013 (3) | ProjectService.delete FK 保护：注入 RequirementRepository + UserRoleRepository；加 countByProjectId 派生查询；Delete tests 3 case（无 ref / Req ref / UserRole ref） | M01 |
| M03 | P0 | TC-REQP-001..003, TC-REQP-005..006 (5) | Requirement 改造：注入 ProjectRepository；Service.create / update 加 projectId 校验；RequirementUpdateRequest 加 ownerUserId（@NotNull）；Service.update 接收新 owner 校验存在 + 赋值；RequirementDetail 加 projectName / projectCode；Service.enrich 富化（reads 严格 — 由后续 M05 自愈保证）；扩展 RequirementControllerCreateTest 加 P-001/002/003；扩展 QueryTest 加 P-005/006（含 owner 反转） | M01 |
| M04 | P0 | TC-URLP-001..003 (3) | UserRole 改造：注入 ProjectRepository；Service.create 加 projectId 校验（NULL 公司级 hat）；UserRoleDetail 加 projectName / projectCode；Service.enrich 富化；扩展 UserRoleControllerCreateTest 加 P-001/002/003 | M01 |
| M05 | P0 | TC-REQP-004, TC-URLP-004 (2) | DanglingProjectIdCleanup：`com.rainier.project.bootstrap.DanglingProjectIdCleanup` @Component + CommandLineRunner；native UPDATE SET NULL where project_id not in (select id from rainier_project where del_flag=0)；log WARN per row；新建 `DanglingProjectIdCleanupTest`：@SpringBootTest，pre-insert dangling Requirement + UserRole 行后调 run()，验证 NULL 化 + 后续 GET 返 projectName=null | M03, M04 |
| M06 | P0 | (前置) | 前端 API 类型层：`frontend/src/api/project.ts` — Project / ProjectCreate / ProjectUpdate（含 ownerUserId）/ ProjectStatus 联合类型；listProjects / get / create / update / delete | 无 |
| M07 | P0 | TC-FES-P03, TC-FES-P04 | 前端 ProjectsPage：`/pm/projects` CRUD（与 Position/Role 同款模式）+「负责人」select 异步 listUsers + 默认选中当前登录 user（loginName 匹配）+ 编辑时回显 + **不 disabled**（可改）；新建 `ProjectsPage.test.tsx`：mock auth store + mock listUsers + mock createProject/updateProject → 验证默认选中 + 编辑可改 | M06 |
| M08 | P0 | TC-FES-P01, TC-FES-P02 | Sider 改造 + AppRoutes 改造 + 测试扩展：navGroups「需求管理」items 前插「项目」；AppRoutes 注册 `/pm/projects` → ProjectsPage；扩展 AppLayout.test 加 TC-FES-P01（项目位于诉求之前）；扩展 AppRoutes.test 加 TC-FES-P02（mount /pm/projects → positions-new-btn 存在）；grep 校验 | M07 |
| M09 | P0 | (UI 改造) | RequirementEditDrawer 改造：projectId 控件 数字输入 → Project 下拉（异步 listProjects，留白 = null）；编辑时 owner select 的 `disabled={editing !== null}` 改为 `disabled={false}`（解锁 owner 可改）；RequirementsPage 列表加「项目」列（render projectName + code） | M06 |
| M10 | P0 | TC-FES-P05 | UserRolesPage 改造：projectId 控件 数字输入 → Project 下拉（留白 = 公司级 hat null）；列表加「项目」列；扩展或新建 `UserRolesPage.test.tsx`：mock listProjects + 用户/角色已选 + 项目下拉留白 → 保存 → assert createUserRole body.projectId === null | M06 |
| M11 | P0 | (E2E) | E2E 验证：`docker compose build backend frontend && docker compose up -d --no-deps --force-recreate backend frontend`（**不 down -v**）；后端启动日志含 "cleaned dangling project_id from rainier_user_role.2"；docker exec mysql SHOW TABLES 含 10 张表；DESCRIBE rainier_project；curl 端到端（建 Project → 建 Requirement w/ projectId → 富化检查 → 建 UserRole w/ projectId 校验 → 试 POST Requirement w/ 不存在 projectId → 400 → 试删 Project 在用 → 409 → 改 Requirement.ownerUserId → 200 + 富化跟随）；grep is_pmo / BaseAutoIdEntity 仍 0 | M01..M10 |

## 执行顺序图

```
M01 (Project 链 — entity / dto / service / controller 不含 delete FK)
   │
   ├── M02 (Project.delete FK 保护)
   ├── M03 (Requirement projectId 激活 + owner 可改)
   └── M04 (UserRole projectId 激活)
           │
           └── M05 (DanglingProjectIdCleanup + 测试)
   
M06 (前端 API 类型) — 可与 M01-M05 并行
   │
   ├── M07 (ProjectsPage + test)
   │       │
   │       └── M08 (Sider + AppRoutes + 测试)
   │
   ├── M09 (RequirementEditDrawer 改造 + owner 解锁 + 列表项目列)
   │
   └── M10 (UserRolesPage 改造 + 列表项目列 + test)

M11 (E2E + DESCRIBE + curl + 启动 log) ← 同步点
```

**长程模式下顺序执行**：M01 → M02 → M03 → M04 → M05 → M06 → M07 → M08 → M09 → M10 → M11

## 关键技术 checklist

- M01：Project entity FK 列 `owner_user_id BIGINT NN`；start_date / end_date `@Column(...) java.time.LocalDate`
- M01：ProjectStatus.ALL 用 `Collections.unmodifiableSet(new HashSet<>(Arrays.asList(...)))`（Java 8）
- M01：service 富化 enrich(Project) 注入 UserRepository（同 v0.0.7 UserRole 富化模式）
- M01：code 不加 DB UNIQUE（与 Position/Role/Requirement 决策一致）；service `existsByCode` only
- M02：RequirementRepository.countByProjectId / UserRoleRepository.countByProjectId 派生查询
- M03：RequirementService 现有 @Transactional(rollbackFor=Exception.class) create() 内加 projectId 校验；update() 内加 ownerUserId 校验 + 赋值；DTO 加字段记得 getter/setter
- M03：v0.0.6 TC-REQ-007（PUT body ownerUserId 静默忽略）将自然失败（owner 现在会被改）— 需要修改这个既有测试为新语义"PUT owner 可改"
- M04：UserRoleService 现有 service 富化 enrich 加 project join
- M05：CommandLineRunner native query：用 `@PersistenceContext EntityManager em` + `em.createNativeQuery(...).executeUpdate()`；事务用 @Transactional
- M05：DanglingProjectIdCleanupTest 用 JdbcTemplate insert dangling 行（绕过 entity 校验，模拟既有脏数据）
- M07：默认 owner 解析：`useAuthStore.getState().user?.username` + `users.find(u => u.loginName === username)?.id`
- M07：listUsers 用 size: 100（v0.0.7 修复后的上限）
- M08：navGroups items 数组前插 — 用 `[...prefix, ...originalItems]`
- M09：RequirementEditDrawer 现有 `disabled={editing !== null}` 移除；保持必填校验
- M11：测试环境的 mysql 卷已有 v0.0.7 末态数据；启动后 user_role.id=2 的 projectId=42 必然被清；可用 `docker exec mysql ... SELECT project_id FROM rainier_user_role WHERE id=2` 验证返 NULL
