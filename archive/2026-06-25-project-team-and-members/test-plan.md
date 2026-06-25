# Test Plan — Project Team / PMO / Members (v0.0.64)

## 测试策略

### 金字塔
- **Unit (后端)**：service + repository 层；快速；占比 ~60%
- **Integration (后端)**：controller via MockMvc + 真实 H2 DB；占比 ~30%
- **E2E**：Docker 后端 + curl + mysql verify；占比 ~10%
- **Frontend**：vitest + @testing-library/react + MemoryRouter；mock API

### 原则
- 每个 spec Scenario 至少 1 个 TC；TC 名标 GIVEN-WHEN-THEN 摘要
- TC 跨 capability 复用通过 helper（如 `seedOrgWithPmo(orgId, userId)`）
- 边界场景 + 失败路径与正常路径并重
- AuditAspect 自动审计 → 顺便断言 audit_log 有相应行（spot check 2 处）

### 已有资产
- `JwtTokenProviderTest` / `AuthorizationFilterTest` 等已稳定
- `AuditAspectIntegrationTest` 已就位（v0.0.15）
- `LegacyProductCategoryCleanupTest` 表数断言 → 需同步改 26 → 28
- `OrganizationServiceTest` 已有 getOrganizationTree 等 → 增 getAncestorIds 测试

## 详细测试案例

### entity-organization-pmo (8 TCs)

| TC-ID | 优先级 | 关联 Scenario | 描述 |
|---|---|---|---|
| TC-OPMO-001 | P0 | 创建 PMO 关系 | admin POST 加 PMO → 201 + DB 行 + enrichment |
| TC-OPMO-002 | P0 | 重复添加 | 同 org + 同 user 第二次 POST → 409 "PMO 已存在" |
| TC-OPMO-003 | P0 | 删自身 PMO | admin DELETE own PMO → 200 + del_flag=1 |
| TC-OPMO-004 | P0 | 子组织继承父 PMO | 三级 org，root 配 alice，孙级查 effective → 含 alice 标 inheritedFromOrgId=root |
| TC-OPMO-005 | P0 | 顶级组织无祖先 | root org 查 effective → 仅含 own PMO inheritedFromOrgId=self |
| TC-OPMO-006 | P0 | 删继承 PMO 400 | 子 org DELETE inherited user → 400 "请到上级组织 XX 操作" |
| TC-OPMO-007 | P0 | 非 admin POST 被拒 | 普通用户 POST → 403 |
| TC-OPMO-008 | P1 | effective-PMOs 顺序 | own 优先于 inherited（按祖先深度排序） |

### entity-project-member (12 TCs)

| TC-ID | 优先级 | 关联 Scenario | 描述 |
|---|---|---|---|
| TC-PMEM-001 | P0 | 添加成员 | owner POST member 成功 → 201 + DB + joined_by |
| TC-PMEM-002 | P0 | 非法 role | role=FAKE → 400 "invalid role" |
| TC-PMEM-003 | P0 | 重复添加 409 | 同 project + 同 user 再 POST → 409 "已是项目成员" |
| TC-PMEM-004 | P0 | 加 owner 为成员 | userId == ownerUserId → 400 "该用户已是项目负责人" |
| TC-PMEM-005 | P0 | 非授权 add | 普通用户 POST → 403 |
| TC-PMEM-006 | P0 | 项目 PMO 可加 | 当前 user = project.pmo_user_id → POST 成功 201 |
| TC-PMEM-007 | P0 | admin 可加 | adminAccess=true 用户 POST → 成功 |
| TC-PMEM-008 | P0 | 改 role | PUT 改 role 字段 → 200 + DB 更新 |
| TC-PMEM-009 | P0 | 删成员 | DELETE 普通成员 → 200 + del_flag=1 |
| TC-PMEM-010 | P0 | 删 owner 拒绝 | DELETE userId=ownerUserId → 400 "不可移除负责人" |
| TC-PMEM-011 | P0 | 列表 UNION | GET members 返回 owner 行 + pmo 行 + 真实行 顺序+role 标签 |
| TC-PMEM-012 | P0 | pmo == owner 去重 | 同人时仅返 OWNER 一行（不合成 PMO 行） |

### entity-project (5 TCs，增量)

| TC-ID | 优先级 | 关联 Scenario | 描述 |
|---|---|---|---|
| TC-PROJ-MOD-001 | P0 | create 带 pmoUserId | POST 带 pmoUserId → 201 + pmoName enriched |
| TC-PROJ-MOD-002 | P0 | create 带 organizationId | POST 带 organizationId → 201 + organizationName enriched |
| TC-PROJ-MOD-003 | P0 | 默认 organizationId 注入 | 缺 organizationId + owner 有主组织 → 自动填 |
| TC-PROJ-MOD-004 | P0 | 默认 pmoUserId 注入 | 缺 pmoUserId + 有 team → 自动填 effective-PMOs 首条 |
| TC-PROJ-MOD-005 | P0 | owner 无主组织 | organizationId 留 null + pmoUserId 留 null |

### frontend-scaffold (6 TCs)

| TC-ID | 优先级 | 关联 Scenario | 描述 |
|---|---|---|---|
| TC-FES-PEM-001 | P0 | ProjectEditDrawer team 自动填 | mock /me/primary-org + 选 owner → team TreeSelect 显示 |
| TC-FES-PEM-002 | P0 | 切 team PMO 候选刷新 | 切 team → mock /effective-pmos → select 选项更新 + 默认重选 |
| TC-FES-PEM-003 | P0 | ProjectDetailPage 成员 Tab 渲染 | mock /members → 渲染 owner 行 + pmo 行 + 真实行 |
| TC-FES-PEM-004 | P0 | 非授权用户不见管理按钮 | 当前用户 != owner/pmo/admin → 添加 + 移除按钮 absent |
| TC-FES-PEM-005 | P0 | OrganizationEditDrawer PMO 段 | mock /pmos → own chip 可删 + inherited chip 灰禁用 |
| TC-FES-PEM-006 | P1 | ProjectsPage 团队列 | mock listProjects 含 organizationName → 表格显示 |

### E2E (4 TCs)

| TC-ID | 优先级 | 描述 |
|---|---|---|
| TC-E2E-PEM-001 | P0 | docker 重建 + SHOW TABLES=28 (含 organization_pmo + project_member) |
| TC-E2E-PEM-002 | P0 | curl: admin POST org_pmo → 子 org GET effective-pmos 含继承条目 |
| TC-E2E-PEM-003 | P0 | curl: owner POST member → owner 查 members 看到完整列表（合成行+真实行） |
| TC-E2E-PEM-004 | P0 | curl: 非授权 POST → 403; admin 创建 project 缺 organizationId → 自动填主组织 |

### Perf (附带，~2 TCs)

| TC-ID | 优先级 | 描述 |
|---|---|---|
| TC-PERF-PEM-001 | P1 | 大组织树 (10 层) 查 effective-pmos 响应 < 50ms |
| TC-PERF-PEM-002 | P1 | 100 成员的 project 查 members < 100ms |

**TC 总数：** 8 + 12 + 5 + 6 + 4 + 2 = **37 TCs (35 P0 + 2 P1)**

## 测试执行矩阵

| 功能 | Unit | Integration | E2E | Frontend |
|---|---|---|---|---|
| organization_pmo CRUD | ✅ TC-OPMO-001~003,008 | ✅ via controller test | ✅ TC-E2E-002 | — |
| effective-pmos 继承 | ✅ TC-OPMO-004,005 | ✅ | ✅ TC-E2E-002 | — |
| 删继承拒绝 | — | ✅ TC-OPMO-006 | — | — |
| project_member CRUD | ✅ TC-PMEM-001~004,008~012 | ✅ | ✅ TC-E2E-003 | — |
| 成员权限 (403) | — | ✅ TC-PMEM-005,007 | ✅ TC-E2E-004 | — |
| project pmoUserId/organizationId | ✅ TC-PROJ-MOD-001,002 | ✅ | ✅ | — |
| 默认值注入 | ✅ TC-PROJ-MOD-003~005 | ✅ | ✅ TC-E2E-004 | — |
| ProjectEditDrawer | — | — | — | ✅ TC-FES-PEM-001,002 |
| ProjectDetailPage 成员 Tab | — | — | — | ✅ TC-FES-PEM-003,004 |
| OrganizationEditDrawer PMO | — | — | — | ✅ TC-FES-PEM-005 |
| ProjectsPage 团队列 | — | — | — | ✅ TC-FES-PEM-006 |

## 回归风险矩阵

| 区域 | 风险 | 说明 |
|---|---|---|
| `ProjectService.create` | 🔴 高 | 新增默认值注入逻辑；既有 createProject 调用必须仍 work；测试集中验证既有签名兼容 |
| `Project / ProjectDetail DTO` | 🟡 中 | 新字段 enrich 不破坏现有响应；既有前端读 ProjectDetail 不应崩；测试覆盖 list/get/create 三接口 |
| `ProjectsPage` | 🟡 中 | 加 team 列 + 编辑按钮 / 行点击行为不变；既有测试用例继续通过 |
| `OrganizationEditDrawer` | 🟡 中 | 加 PMO 段不破坏既有名/描述编辑流程；既有 OrganizationsPage.test 用例继续 |
| `MeService.listMyProjects` | 🟡 中 | UNION 新数据源；既有 workbench "我的项目" 用例继续通过 |
| `LegacyProductCategoryCleanupTest` | 🟢 低 | 表数断言改 26→28，纯机械 |
| AuditAspect | 🟢 低 | 自动捕获新 service；测试 spot-check 2 处即可 |
| Java 8 兼容 | 🔴 高 | Set.of/List.of/var 误用会让 Docker 构建挂；review checklist |

## 建议补充顺序

**P0 (必做，本版核心)：**
1. TC-OPMO-001~007 (org_pmo CRUD + 继承 + 权限)
2. TC-PMEM-001~012 (member CRUD + 权限 + UNION)
3. TC-PROJ-MOD-001~005 (project 字段 + 默认注入)
4. TC-FES-PEM-001~005 (前端核心)
5. TC-E2E-PEM-001~004 (端到端)

**P1 (附带，资源允许)：**
6. TC-OPMO-008 (PMO 排序)
7. TC-FES-PEM-006 (ProjectsPage 团队列)
8. TC-PERF-PEM-001,002 (性能 spot-check)
