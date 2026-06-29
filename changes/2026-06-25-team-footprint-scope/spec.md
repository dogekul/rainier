# Spec — team-footprint-scope (H2)

## Scenario A — ScopeService.teamFootprintProjects 基本展开
Given alice 是 DEPT 的 HEAD（active），DEPT 下有 TEAM1，TEAM1 内有 bob、charlie（active）
And bob 拥有项目 P-BOB
And charlie 在 P-CHARLIE 上有 UserRole
And david 拥有项目 P-DAVID 但不在 DEPT 子树内
When 调用 `scopeService.teamFootprintProjects(alice.id)`
Then 返回的 projectIds 包含 P-BOB 和 P-CHARLIE
And 不包含 P-DAVID
And 不重复（即使 bob 同时 own 和 role-on）

## Scenario B — `resolveProjectIds(username, "footprint")` 桥接
Given 同上 seeding
When `scopeService.resolveProjectIds("alice", "footprint")`
Then 等价于直接调 `teamFootprintProjects(alice.id)`

## Scenario C — 非 HEAD 用户返回空
Given eve 不是任何 org 的 HEAD
When `teamFootprintProjects(eve.id)`
Then 返回空 list

## Scenario D — leftAt 排除
Given alice HEAD of TEAM1, bob 在 TEAM1 但 `leftAt != null`
And bob 拥有项目 P-BOB
When `teamFootprintProjects(alice.id)`
Then P-BOB 不在结果中（bob 已离队）

## Scenario E — PortfolioController scope=footprint
Given alice token，alice 是 DEPT HEAD，bob 在 DEPT 内拥有项目 P-BOB
When `GET /api/me/portfolio?scope=footprint`
Then 200 OK
And 返回的 PortfolioRow 包含 projectCode=P-BOB

## Scenario F — TeamLeadPage 默认请求 footprint
Given 已登录团队负责人
When TeamLeadPage 挂载
Then 调用 `getPortfolio('footprint')`（不是 `'led'`）
And 渲染团队足迹的项目 RYG

## Scenario G — TeamLeadPage 作用域切换 toggle
Given TeamLeadPage 已渲染默认 footprint
When 用户点击 toggle 切到「我直管」
Then 再次调用 `getPortfolio('led')`
And 渲染 led-scope 项目
