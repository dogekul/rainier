# Spec — pmo-company-map (H3, v0.0.110)

## Scenarios

### TC-PMO-01: groupBy=organization 按组织切片 + RYG 计数
Given 3 个项目: P1 挂 OrgA (RED 1 blocked task)、P2 挂 OrgA (GREEN clean)、P3 挂 OrgB (YELLOW 1 overdue)
When 调用 GET `/api/pmo/portfolio?groupBy=organization` (alice 已认证)
Then 返回 2 个 group: OrgA(rygCount.red=1, green=1, projects.size=2) 和 OrgB(rygCount.yellow=1, projects.size=1)
And group.name 来自 Organization 表

### TC-PMO-02: groupBy=owner 按负责人切片
Given alice 拥有 P1, bob 拥有 P2 P3
When 调用 GET `/api/pmo/portfolio?groupBy=owner`
Then 返回 2 个 group: alice 包 1 个项目, bob 包 2 个项目
And group.id = ownerUserId, group.name = user.name

### TC-PMO-03: organizationId NULL 项目落入 "未归属"
Given 1 个项目 organizationId=null
When 调用 GET `/api/pmo/portfolio?groupBy=organization`
Then 至少有 1 个 group.id=null name="未归属" 包此项目

### TC-PMO-04: groupBy=none 返回单个全公司 group
Given 2 个项目
When 调用 GET `/api/pmo/portfolio?groupBy=none`
Then 返回 1 个 group name="全公司", projects.size=2

### TC-PMO-05: no token → 401
When 调用 GET `/api/pmo/portfolio` 无 Authorization
Then 401

### TC-PMOFE-01: PmoPortfolioPage 默认 render + group cards 显示
Given mock getPmoPortfolio 返回 2 个 group, 每个含 1 project
When 渲染 `<PmoPortfolioPage />`
Then 看到 2 张 DashboardCard, 每张含 RYG chip 和项目链接

### TC-PMOFE-02: toggle groupBy 触发 refetch
Given 默认 organization, mock 切换 owner 时返回不同 group
When 用户切换 groupBy select 到 owner
Then getPmoPortfolio 被以 'owner' 再次调用
