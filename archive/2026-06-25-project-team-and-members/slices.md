# Slices — v0.0.64 Project Team / PMO / Members

12 slices grouped into 5 batches by dependency. Within a batch, slices are independent and can run in parallel.

## Batch 1 — Backend scaffolding (parallel, no deps)

| # | Pri | TCs | Target | Deps |
|---|---|---|---|---|
| M01 | P0 | TC-OPMO-001~007 | `organizationpmo` 包：OrganizationPmo entity + OrganizationPmoRepository + OrganizationPmoService (含 findEffectivePmos 继承算法 + getAncestorIds helper 加到 OrganizationService) + OrganizationPmoController + DTOs (Detail/CreateRequest/EffectivePmoDetail) | — |
| M02 | P0 | TC-PMEM-001~011 | `projectmember` 包：ProjectMember entity + ProjectMemberRole 常量类 (PD/DEV/QA/DESIGN/BIZ/OPS/OTHER) + ProjectMemberRepository + ProjectMemberService (含 listMembers UNION owner/pmo) + ProjectMemberController + DTOs | — |
| M03a | P0 | TC-PROJ-MOD-001,002 | rainier_project `+pmo_user_id` 列 (Project domain + DTOs ProjectCreateRequest/ProjectUpdateRequest/ProjectDetail 接受/返回 pmoUserId+organizationId+enriched names) | — |

## Batch 2 — Service-layer integration (depends on Batch 1)

| # | Pri | TCs | Target | Deps |
|---|---|---|---|---|
| M03b | P0 | TC-PROJ-MOD-003~005 | ProjectService.create 默认值注入：缺 organizationId 取 owner 主组织; 缺 pmoUserId 取 organizationPmoService.findEffectivePmos(orgId) 首条 | M01, M03a |
| M04 | P0 | TC-PMEM-005,007 / 我的项目 | AuthzService.canManageProjectMembers helper (owner ∥ project.pmo ∥ adminAccess); MeService.listMyProjects UNION project_member.user_id | M02, M03a |

## Batch 3 — Backend tests (depends on Batch 2)

| # | Pri | TCs | Target | Deps |
|---|---|---|---|---|
| M05 | P0 | TC-OPMO-001~008 + TC-PERF-PEM-001 | OrganizationPmoServiceTest + OrganizationPmoControllerTest + EffectivePmosInheritanceTest (3 层 org 链) + perf | M01 |
| M06 | P0 | TC-PMEM-001~012 + TC-PERF-PEM-002 | ProjectMemberServiceTest + ProjectMemberControllerTest + UnionListMembersTest (合成行顺序/pmo==owner 去重) | M02, M04 |
| M07 | P0 | TC-PROJ-MOD-001~005 | ProjectServiceCreateDefaultInjectionTest (5 case: 全 explicit / 缺 org / 缺 pmo / owner 无主组织 / pmo 取 effective 首条); `LegacyProductCategoryCleanupTest` 表数 26→28 | M03b |

## Batch 4 — Frontend (parallel)

| # | Pri | TCs | Target | Deps |
|---|---|---|---|---|
| M08 | P0 | — | api/organizationPmo.ts + api/projectMember.ts + api/project.ts (+pmoUserId/organizationId 字段) + constants/labels.ts +PROJECT_MEMBER_ROLE_LABELS map | — |
| M09 | P0 | TC-FES-PEM-001,002 | ProjectEditDrawer.tsx 加 team TreeSelect + pmo `<select>` + useEffect 联动（owner 变 → team 默认 / team 变 → effective-pmos 候选刷新+pmo 默认） | M08 |
| M10 | P0 | TC-FES-PEM-003,004 | ProjectDetailPage.tsx Hero +team/pmo chips；基本信息 grid +team/pmo 行；新「成员」Tab (位于 里程碑 与 需求 之间) 含 UNION 列表 + 添加弹窗 + 权限按钮 | M08 |
| M11 | P1 | TC-FES-PEM-005,006 | Organization/EditDrawer.tsx 底部 PMO 管理段 (own 可改/inherited 灰禁用); ProjectsPage.tsx +团队列 | M08 |

## Batch 5 — E2E + Seed (last)

| # | Pri | TCs | Target | Deps |
|---|---|---|---|---|
| M12 | P0 | TC-E2E-PEM-001~004 | scripts/seed-demo.sql 补 (6 org PMO + project pmo/team + 项目成员 各 role); docker 重建 + SHOW TABLES=28 + curl 验证链 (admin 加 org_pmo → 子 org effective-pmos 含继承 → owner 加 member → list 看合成行 → 非授权 403 → 缺 organizationId 后端注入主组织) | M01-M11 |

## 拓扑顺序

```
[Batch 1] M01, M02, M03a   (parallel)
              ↓
[Batch 2] M03b, M04         (parallel, depend on Batch 1)
              ↓
[Batch 3] M05, M06, M07     (parallel, depend on Batch 2)
              ↓
[Batch 4] M08, then M09/M10/M11 (M08 first; M09/M10/M11 parallel)
              ↓
[Batch 5] M12               (after all)
```

预期最终：
- 后端测试 309 + ~25 (org_pmo 8 + project_member 12 + project-default 5 + perf 2 - 改 1 cleanup) → **~334 green**
- 前端测试 265 + ~6 (project edit drawer 2 + detail member tab 2 + org PMO 1 + projects col 1) → **~271 green**
- 表数 26 → 28
- E2E SHOW TABLES + 全链路 curl 通过
