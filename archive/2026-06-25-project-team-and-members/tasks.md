# Tasks — v0.0.64 Project Team / PMO / Members

按 capability 分组的实现任务清单。

## Capability: entity-organization-pmo (NEW)

- [ ] **M01-a** 创建 `backend/src/main/java/com/rainier/organizationpmo/domain/OrganizationPmo.java` (extends BaseEntity; fields: organizationId, userId; @Table rainier_organization_pmo)
- [ ] **M01-b** 创建 `OrganizationPmoRepository.java` (JpaSpecificationExecutor + findByOrganizationIdInAndDelFlagFalse 等)
- [ ] **M01-c** 创建 DTOs：`OrganizationPmoDetail`, `OrganizationPmoCreateRequest`, `EffectivePmoDetail`
- [ ] **M01-d** 创建 `OrganizationPmoService.java`：record(orgId,userId)→Detail, query(orgId)→List<Detail>, delete(id)，findEffectivePmos(orgId)→List<EffectivePmoDetail> 含继承计算 + enrich userName/orgName，校验非 admin 抛 403
- [ ] **M01-e** 创建 `OrganizationPmoController.java`：`GET/POST/DELETE /api/organizations/{id}/pmos`、`GET /api/organizations/{id}/effective-pmos`
- [ ] **M01-f** 在 `OrganizationService` 加 `getAncestorIds(orgId)`：沿 parent_id 链向上，返回 [self, parent, grandparent, ...]，停在 NULL
- [ ] **M05** 测试：OrganizationPmoServiceTest + OrganizationPmoControllerTest + EffectivePmosInheritanceTest（TC-OPMO-001~008，含 3 层 org 继承）+ perf TC-PERF-PEM-001

## Capability: entity-project-member (NEW)

- [ ] **M02-a** 创建 `backend/src/main/java/com/rainier/projectmember/domain/ProjectMember.java` (extends BaseEntity; fields: projectId, userId, role, joinedAt, joinedBy)
- [ ] **M02-b** 创建 `domain/ProjectMemberRole.java` 常量类 + ALL set (用 `new HashSet<>(Arrays.asList(PD,DEV,QA,DESIGN,BIZ,OPS,OTHER))`)
- [ ] **M02-c** 创建 `ProjectMemberRepository.java`：findByProjectIdAndDelFlagFalseOrderByJoinedAtDesc, findByProjectIdAndUserIdAndDelFlagFalse, countByUserIdAndDelFlagFalse
- [ ] **M02-d** 创建 DTOs：`ProjectMemberDetail` (含 displayLabel)、`ProjectMemberCreateRequest`、`ProjectMemberUpdateRequest`
- [ ] **M02-e** 创建 `ProjectMemberService.java`：record(projId,userId,role,joinedBy), updateRole(memberId, role), delete(memberId), listMembers(projectId) 含合成 owner+pmo 行 UNION 真实行
- [ ] **M02-f** 创建 `ProjectMemberController.java`：`GET/POST /api/projects/{id}/members`、`PUT /api/projects/{id}/members/{userId}`、`DELETE /api/projects/{id}/members/{userId}`
- [ ] **M06** 测试：ProjectMemberServiceTest + ControllerTest + UnionListMembersTest（TC-PMEM-001~012）+ perf TC-PERF-PEM-002

## Capability: entity-project (MODIFIED)

- [ ] **M03a-a** `Project.java` +`pmoUserId BIGINT NULL` 字段 + getter/setter
- [ ] **M03a-b** `ProjectCreateRequest.java` +pmoUserId / organizationId nullable 字段
- [ ] **M03a-c** `ProjectUpdateRequest.java` +pmoUserId / organizationId 字段
- [ ] **M03a-d** `ProjectDetail.java` + from()  enrich：pmoUserId / pmoName / pmoLoginName / organizationId / organizationName / organizationType（join user + organization）
- [ ] **M03b-a** `ProjectService.create` 默认值注入：缺 organizationId 时查 user_organization (is_primary=1) 取 owner 主组织；缺 pmoUserId 时调 OrganizationPmoService.findEffectivePmos(orgId) 取首条
- [ ] **M03b-b** `ProjectService.update` 接受 organizationId / pmoUserId 改动（不重新注入默认）
- [ ] **M03b-c** `ProjectService.toDetail` enrich Project → ProjectDetail 含 organization / pmo 字段
- [ ] **M07** 测试：ProjectServiceCreateDefaultInjectionTest（TC-PROJ-MOD-001~005）+ LegacyProductCategoryCleanupTest 表数 26→28

## Capability: backend-authz (MODIFIED)

- [ ] **M04-a** `AuthzService.canManageProjectMembers(Long userId, Long projectId): boolean` — owner ∥ project.pmo ∥ admin
- [ ] **M04-b** 在 ProjectMemberController 加 service 调用 + 抛 ForbiddenException

## Capability: me-inbox (MODIFIED)

- [ ] **M04-c** `MeService.listMyProjects(userId)` UNION：owner+user_role.project_id+project_member.user_id 三路去重；改既有 query 或新 query 都行
- [ ] **M04-d** 既有 `MeServiceTest` 加 1 case：仅 member 也算我的项目

## Capability: frontend-scaffold (MODIFIED)

- [ ] **M08-a** `frontend/src/api/organizationPmo.ts`：types + listPmos / addPmo / deletePmo / getEffectivePmos
- [ ] **M08-b** `frontend/src/api/projectMember.ts`：types + listMembers / addMember / updateMemberRole / deleteMember + `PROJECT_MEMBER_ROLE_LABELS` Record map
- [ ] **M08-c** `frontend/src/api/project.ts` 类型扩展：Project / ProjectCreate / ProjectUpdate / ProjectDetail +pmoUserId / pmoName / pmoLoginName / organizationId / organizationName / organizationType
- [ ] **M08-d** `frontend/src/constants/labels.ts` 加 `PROJECT_MEMBER_ROLE_LABELS` 中文 map (PD=产品经理/DEV=研发/QA=测试/DESIGN=设计/BIZ=业务/OPS=运维/OTHER=其他)
- [ ] **M09-a** `ProjectEditDrawer.tsx` 加 state：teamOrgId, pmoUserId, pmoCandidates, pmoLoading, lastPmoRequestId
- [ ] **M09-b** TreeSelect 「负责团队」+ useEffect on owner change → 调 /api/me/effective-pmos 或类似 helper 拿 primary-org
- [ ] **M09-c** `<select>` 「项目PMO」+ useEffect on teamOrgId change → getEffectivePmos + 重置 pmo 默认 + AbortController/lastRequestId 防竞态
- [ ] **M09-d** 提交时附 organizationId + pmoUserId
- [ ] **M09-e** 测试：TC-FES-PEM-001 (team 自动填) + TC-FES-PEM-002 (PMO 候选刷新)
- [ ] **M10-a** `ProjectDetailPage.tsx` Hero 加 团队 chip + PMO chip (data from ProjectDetail)
- [ ] **M10-b** 基本信息 grid 加 「负责团队」/「项目PMO」 两行
- [ ] **M10-c** 新「成员」Tab：listMembers + 渲染 owner 行 / pmo 行 / 真实行 + role 中文标签 + OwnerChip
- [ ] **M10-d** 「添加成员」按钮 + 弹窗 (UserSelect + RoleSelect) → addMember；权限决定可见
- [ ] **M10-e** 「移除」按钮（每真实 member 行右侧）+ 「改 role」select → updateMemberRole；owner/pmo 合成行无这些按钮
- [ ] **M10-f** 测试：TC-FES-PEM-003 (Tab 渲染) + TC-FES-PEM-004 (非授权无管理按钮)
- [ ] **M11-a** `Organization/EditDrawer.tsx` 底部 PMO 管理段：own chips（admin 可删 X）+ inherited chips（灰禁用 + 「继承自 XX」注脚）+ 「添加 PMO」按钮（仅 admin）
- [ ] **M11-b** `ProjectsPage.tsx` 表格 +「团队」列 (organizationName 或 "—")
- [ ] **M11-c** 测试：TC-FES-PEM-005 (Org PMO 段) + TC-FES-PEM-006 (Projects 团队列)

## Cap: seed + E2E

- [ ] **M12-a** `scripts/seed-demo.sql` 补 organization_pmo 行 (root=alice, 研发中心=黎立)，project pmo/team 字段，project_member 各 role 覆盖
- [ ] **M12-b** 重建 Docker frontend + 重启 + curl 链路验证 (TC-E2E-PEM-001~004): SHOW TABLES=28 + admin POST org_pmo → 子 org GET effective-pmos 含继承 → owner POST member → list members 含合成行 → 非授权 POST 403 → POST project 缺 organizationId 自动填

## 完成准则

- [ ] backend `mvn test` (temurin-8 Docker) 309+~25 → ~334 全绿
- [ ] frontend `vitest run` 265+~6 → ~271 全绿
- [ ] `tsc --noEmit` clean / `eslint` clean
- [ ] SHOW TABLES = 28
- [ ] curl 链路（M12-b）全通
- [ ] AuditAspect 自动捕获新 service 写操作（spot check audit_log 出现 ORGANIZATION_PMO / PROJECT_MEMBER 行）
- [ ] 既有 309 后端 + 265 前端测试零回归
