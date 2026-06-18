# Test Plan — v0.0.40-me-profile

> Baseline backend 435 green / frontend 162 green. New TCs below; all P0.

## 测试策略

- 后端集成（@SpringBootTest + MockMvc，H2）= profile 聚合（身份/岗位/组织/上级上溯/计数）+ 降级 + 401。
- 前端组件（Vitest + RTL，mock api/profile）= ProfilePage 渲染；AppRoutes /profile；navGuardConsistency 自动。
- E2E（Docker 真 MySQL）= 真实用户档案 + 存量数据零改。
- seed：直接 saveAndFlush（User/Organization/UserOrganization/Position/Story/Task），无需 service 链。

## 详细测试案例

### me-profile（后端，MeProfileControllerTest）

| TC-ID | Scenario | 预期 |
|---|---|---|
| TC-PROF-001 | 身份 + 岗位 + 组织关系 | 200；name/positionName/positionCategory + memberships 含 primary MEMBER 项 |
| TC-PROF-002 | 直接上级 = primary org 在岗 HEAD（非本人） | manager = bob |
| TC-PROF-003 | 团队 HEAD 上级取父组织 HEAD（上溯跳过本人） | manager = carol |
| TC-PROF-004 | 无上级 → manager null | 200；manager=null |
| TC-PROF-005 | 贡献计数（owned Story / assigned Task） | ownedStoryCount=3 / assignedTaskCount=5 |
| TC-PROF-006 | 无 token | 401 |
| TC-PROF-007 | token sub 无对应用户 → 降级 | 200；loginName=sub、memberships=[]、manager=null、计数 0 |
| TC-PROF-008 | 软删 Story/Task 不计入 | 计数仅未软删 |

### frontend-scaffold（前端，ProfilePage.test + AppRoutes）

| TC-ID | Scenario | 预期 |
|---|---|---|
| TC-PROFP-01 | 渲染身份 + 贡献 + 上级 | name/position 可见、Story=3/Task=5 磁贴、Bob 上级 |
| TC-PROFP-02 | 组织关系列表 | 采购小队 + 角色标记可见 |
| TC-PROFP-03 | 无组织 → EmptyState | profile-orgs-empty 可见 |
| TC-FES-PROF-01 | /profile 路由挂载 ProfilePage | profile 容器可见 |
| TC-FES-PROF-02 | AppRoutes.tsx 含 /profile literal | grep ≥1 |
| TC-FES-PROF-03 | isAdminPath('/profile')===false（navGuardConsistency 自动） | all-users |

### E2E

| TC-ID | Scenario | 预期 |
|---|---|---|
| TC-E2E-PROF-001 | 真实用户 token → GET /api/me/profile 返回 身份/组织/计数 | 链路通 |
| TC-E2E-PROF-002 | 存量业务数据不变（纯读） | 数据零改 |

## 回归风险矩阵

| 区域 | 风险 | 缓解 |
|---|---|---|
| 新 /api/me/profile | 🟢低 | 纯新增只读端点，复用 MeTeamService/Portfolio 范式 |
| TaskRepository/UserOrgRepository +方法 | 🟢低 | 纯新增派生查询，不动既有 |
| manager 上溯 | 🟡中 | depth cap 8 防环；无 primary/无 HEAD → null |
| 前端导航守卫 | 🟢低 | navGuardConsistency 自动钉 /profile all-users |
