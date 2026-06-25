# Change Proposal — Project Team / PMO / Members (v0.0.64)

## Why

项目当前只有 owner_user_id 一个"人"字段，缺三类关键属性：

1. **负责团队** — 项目客观上属于某团队/组织，缺这层归属导致团队级看板/统计/权限做不出来。后端 schema 上其实已有 `rainier_project.organization_id` 列，从未在 UI 启用。
2. **项目 PMO** — 项目交付的关键角色（节奏跟进/风险/复盘）。今天没字段；PMO 也没有和组织挂钩，分配靠口头。
3. **项目成员清单** — owner + PMO 之外，谁是这个项目的 DEV/QA/PD/DESIGN/BIZ/OPS。今天靠 user_role 表凑（重，且承载权限语义），表达"该人参与本项目"很别扭。

用户原文（2026-06-25）：

> 项目里需要添加上负责团队，默认为负责人所在团队，可修改；项目成员信息，可以由项目负责人 或 PMO 随时添加；PMO 与组织关联；项目PMO默认设置为项目所属团队的PMO，可修改。

后续澄清：
- PMO 与组织关联是 **1:N**（一组织可多 PMO）；
- **owner 不冗余写入 project_member 表**（read 时 UNION）；
- 上级组织配 PMO 后 **下级自动继承**（读时计算，不写入下级行）；
- 默认值在 **后端注入**（避免前端覆盖漂移）；
- 项目成员要有 **项目内角色** 字段（与 rainier_role 解耦）。

## What Changes

### Backend
1. 新表 `rainier_organization_pmo`：(organization_id, user_id) 多对多，承载"谁是某 org 的 PMO"。
2. 新表 `rainier_project_member`：(project_id, user_id, role) 项目成员，role ∈ {PD, DEV, QA, DESIGN, BIZ, OPS, OTHER}；UNIQUE(project_id, user_id, del_flag) 一人一项目一行。
3. 改表 `rainier_project += pmo_user_id BIGINT NULL`（单值；默认取团队 effective-PMOs 首条）。
4. `ProjectService.create` 注入默认值：缺 organizationId 时取 owner 主组织；缺 pmoUserId 时取该 org 的 effective-PMOs 首条。
5. `OrganizationService.getAncestorIds(orgId)` helper：沿 parent_id 链向上回溯。
6. `OrganizationPmoService.findEffectivePmos(orgId)` helper：自身 PMOs UNION 所有祖先 PMOs，标 inheritedFromOrgId。
7. `MeService.listMyProjects` 改 UNION project_member（"我作为成员的项目"也算"我的项目"）。
8. 新 endpoints:
   - `GET/POST/DELETE /api/organizations/{id}/pmos` —— 仅 admin
   - `GET /api/organizations/{id}/effective-pmos` —— 含 inherited
   - `GET /api/projects/{id}/members` —— 登录用户
   - `POST /api/projects/{id}/members` body `{userId, role}` —— owner / project pmo / admin
   - `PUT /api/projects/{id}/members/{userId}` body `{role}` —— 同上（只改 role）
   - `DELETE /api/projects/{id}/members/{userId}` —— 同上；owner 不可删
9. `Project` / `ProjectDetail` enrich：+ pmoUserId / pmoName / pmoLoginName / organizationId / organizationName / organizationType
10. `LegacyProductCategoryCleanupTest` 表数 26 → 28。

### Frontend
1. 新文件 `api/organizationPmo.ts`、`api/projectMember.ts`、`constants/labels.ts +PROJECT_MEMBER_ROLE_LABELS`。
2. `ProjectEditDrawer.tsx`：
   - 加 「负责团队」TreeSelect（org 树）；
   - 加 「项目 PMO」`<select>`，候选 = 所选团队的 effective-PMOs；
   - owner 切换 → 后端注入新默认；前端切换 team → 查 effective-pmos 重列 + 重置 PMO 为首条。
3. `ProjectDetailPage.tsx`：
   - Hero 加 2 个 chip：`🏢 团队名` + `👤PMO 名`；
   - 基本信息 grid 加两行：负责团队 / 项目 PMO；
   - 新 Tab「成员」（在 里程碑 与 需求 之间）：UNION owner+pmo+members；添加/移除按钮按权限隐藏；行内 role select 可改。
4. `ProjectsPage.tsx`：加 "团队" 列。
5. `Organization/EditDrawer.tsx`：底部加 PMO 管理段；own chips 可删，inherited chips 灰禁用 +「继承自 XX」注脚；仅 admin 可改。

### Seed
- `scripts/seed-demo.sql` 补：
  - 给现有 6 组织配 own PMO（让继承能看出效果，比如根级 招联金融 配 alice，研发中心 配 lina）；
  - 给 6 项目配 team + pmo_user_id；
  - 每个 EXTERNAL_DELIVERY 项目加 3-5 个 member（不同 role 覆盖）。

## Capabilities

### New
- `entity-project-member` — 项目成员关系（含项目内角色）+ CRUD
- `entity-organization-pmo` — 组织↔PMO 多对多 + 继承计算

### Modified
- `entity-project` — +organizationId 启用 +pmoUserId 字段 + 默认值注入逻辑
- `frontend-scaffold` — ProjectEditDrawer / ProjectDetailPage / ProjectsPage / Organization EditDrawer
- `me-inbox`（或 me-profile）— listMyProjects UNION project_member

## Impact

### 代码层面
- **后端**：~10 新/改 service+controller+dto 文件；2 新 entity + 2 新 repo；改 ProjectService.create / Project / ProjectDetail；新增 OrganizationService.getAncestorIds
- **前端**：~12 改/新文件（2 api 新文件 + 1 constants 改 + 4 page 改 + Drawer 改 + 弹窗组件新增）
- **测试**：~15 backend 测试新增 / ~3 frontend 测试新增

### 配置层面
- 无环境变量改动
- ddl-auto=update 自动建表，无 migration 脚本

### 基础设施
- 无新依赖
- 无新外部服务

## Success Criteria

- [ ] `SHOW TABLES` = 28（含 rainier_organization_pmo + rainier_project_member）
- [ ] `POST /api/projects` 缺 `organizationId` 时，后端从 `user_organization` 取 owner 主组织自动填
- [ ] `POST /api/projects` 缺 `pmoUserId` 但有 `organizationId` 时，从 effective-PMOs 取首条自动填
- [ ] `GET /api/organizations/{id}/effective-pmos` 正确返回 自身+祖先 union，每条带 `inheritedFromOrgId`(null=自己) / `inheritedFromOrgName`
- [ ] 在子 org 上删 inherited PMO 返 400「请到上级 XX 操作」
- [ ] `POST /api/projects/{id}/members` 当前用户 ∉ {owner, project.pmo, admin} 时返 403
- [ ] `DELETE /api/projects/{id}/members/{userId}` 试删 owner 时返 400
- [ ] `GET /api/projects/{id}/members` 返回列表含合成 owner 行（role="OWNER"）+ pmo 行（role="PMO"）+ 显式 project_member 行（按 joined_at DESC）
- [ ] 前端：新建项目，先选 owner → 团队字段自动填该 user 主组织；切换团队 → PMO 候选刷新且默认选首条
- [ ] 前端：ProjectDetailPage 成员 Tab 渲染所有成员 + 权限按钮按当前用户身份显示
- [ ] 前端：Organization EditDrawer 里 PMO 段，own chips 可删、inherited chips 灰禁用带「继承自 XX」注脚
- [ ] 后端：temurin-8 测试 309+~15 → ~324 全绿
- [ ] 前端：vitest 265+~3 → ~268 全绿；tsc clean；eslint clean
- [ ] seed-demo.sql 跑完后，前端 alice 登入即可看到完整 team/pmo/member 信息

## Out of Scope (后续)

- 项目成员一行支持多个 role（暂只 1:1）
- bulk add members（一次只加一个）
- 项目内 role 的权限差异（DEV/QA/etc. 都是只读身份描述，无权限差异）
- 组织详情页（先在 EditDrawer 里维护 PMO；后续可独立成 OrganizationDetailPage）
- PMO 历史变更审计页（AuditAspect 已捕获，但无专门页面）

## Baseline

- tag `v0.0.63-seed-demo` / commit `70624e1`
- 后端测试 309 backend / 前端 265 vitest（v0.0.62 收尾时数）
- 表数 26 → 28（new: organization_pmo + project_member）
