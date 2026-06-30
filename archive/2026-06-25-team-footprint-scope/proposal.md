# Proposal — team-footprint-scope (H2, v0.0.109)

## 命门
C-路线图 #5：团队/小组负责人 RYG 当前用 `scope=led` —
按「项目的 `organizationId` 属于负责人 org-subtree」过滤。
这意味着只有"打了团队标签的项目"才计入。

现实里大量项目 `organizationId=NULL`（v0.0.64 之前创建）或挂在跨团队组织，
导致 TeamLead 面板要么空，要么只看到一小撮项目。
而四个组织角色（团队/小组/领域/部门）面板都复用同一个 scope。

H2 引入一个真正"以人为中心"的作用域 — **footprint** —
让 RYG 按团队成员的"工作落点"展开，而不是项目的组织标签。

## 范围
1. **新方法** `ScopeService.teamFootprintProjects(Long leaderId)`
   - 找 `leaderId` 是 HEAD 的所有活跃 org (`leftAt IS NULL, role=HEAD`)
   - BFS org-tree 收集所有后代 org（复用既有 `descendantOrgIds`）
   - 收集 org-tree 内所有活跃成员 `UserOrganization.userId` (`leftAt IS NULL`)
   - 这些 user 作为 `Project.ownerUserId` 或持有 `UserRole.projectId` 的所有项目，去重返回
2. **`ScopeService.resolveProjectIds`** 增加 `scope=footprint` 分支 → 调上面方法
3. **PortfolioController** 透明承接：`GET /api/me/portfolio?scope=footprint`
4. **前端**
   - `frontend/src/api/portfolio.ts` `PortfolioScope` 增加 `'footprint'`
   - `TeamLeadPage.tsx` 默认 `footprint`，并加 toggle：「团队足迹 / 我直管项目」
5. **测试**
   - `ScopeServiceFootprintTest`（SpringBootTest，复用 PortfolioControllerTest 的 seeding 风格）
     - alice HEAD of DEPT, DEPT 有 child TEAM1，TEAM1 含 bob/charlie
     - alice 的 footprint 包含 bob/charlie 拥有/有角色的项目
     - 不包含 david（不在子树）
   - `PortfolioControllerTest` 加 TC-PF-FP-001：`scope=footprint` 返回足迹项目
   - `TeamLeadPage.test.tsx` 默认请求 `footprint`，toggle 切到 `led` 时改请求

## OutOfScope
- 跨 leadership（同一人 HEAD 多个独立树）合并 — distinct 处理即可
- TEAM HEAD 维度分组（按子团队 group）
- 项目活跃度过滤
- 历史成员（leftAt != null）穿透

## commit
- `feat(team-footprint-scope): H2 团队足迹作用域 + Team RYG 改 (v0.0.109)`
