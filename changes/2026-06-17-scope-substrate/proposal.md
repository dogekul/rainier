# v0.0.28-scope-substrate — 作用域聚合底座 (project↔org edge + scope resolver + portfolio RYG)

> Baseline: tag `v0.0.27-auth-baseline-polish`. 结构层第二波 cycle #1 — 路线图 #1-#4 的底座
> (见 [C-角色链路审计与建设路线图](../../C-角色链路审计与建设路线图.md))。

## Why

角色链路审计的最大发现:8 个仪表盘角色(PM/开发负责人/团队/领域/部门/高管/PMO/小组)全死在同一面墙——
没有跨项目/跨组织的作用域汇总,只有单项目 Cockpit 和单团队 TeamLead。每个面板都在前端重写同一套
「解析作用域 → fan-out tasks/stories → 按项目分组 → 计数 → RYG」。建一次成为所有仪表盘+将来 AI 的读模型。

## What Changes (backend; 1 nullable column, 1 endpoint, 0 删存量)

- **项目↔组织 边**:`Project` 加可空 `organizationId` 列(+ Create/Update/Detail + service);projects 可挂到
  department/domain/team 节点供组合作用域。
- **作用域解析器** `ScopeService.resolveProjectIds(username, scope)`:
  - `mine` = 我有 user_role 的项目 ∪ 我 own 的项目;
  - `led` = 我作为 HEAD 的组织 **及其子树**(BFS over parentId)下挂的项目;
  - `all` = 全部项目(PMO/高管)。
- **组合汇总** `PortfolioService.portfolio(projectIds)`:fan-out task/milestone(findByProjectIdIn 各一次,内存分组)
  → 每项目 {openTasks, overdueTasks, blockedTasks, overdueMilestones, ryg},按 RYG worst-first 排序。
- **后端 RYG** `Ryg.tier(open,overdue,blocked)`:与前端 ryg.ts 同规则(GRAY 无开放 / RED 阻塞或逾期比>0.3 /
  YELLOW 少量逾期 / GREEN 干净),前后端一致。
- **端点** `GET /api/me/portfolio?scope=mine|led|all`(token-gated,身份走 SecurityFilter,默认 mine)。
- 前端 `api/portfolio.ts`(PortfolioRow 类型 + getPortfolio)——下一周期消费。

## Capabilities

- New: `entity-portfolio` (NEW capability — scope resolver + portfolio rollup) + `entity-project` MOD (organizationId).

## Impact

- Backend: `Project`(+organizationId 列 + DTOs + service),`ProjectRepository`(+findByOwnerUserId/findByOrganizationIdIn),
  `TaskRepository`/`MilestoneRepository`(+findByProjectIdIn),新 `com.rainier.portfolio.*`(Ryg/ScopeService/
  PortfolioService/PortfolioRow/PortfolioController)。`RygTest`(5)+`PortfolioControllerTest`(6)。
- 前端:`api/portfolio.ts`(未消费,留给 v0.0.29)。
- 可空列 ddl-auto 安全;存量项目 organizationId=NULL,不删改存量。

## Success Criteria

- [ ] Project 有可空 organizationId,Create/Update/Detail 透传。
- [ ] scope=mine 返回我有 role/own 的项目;led 递归组织子树;all 全部;默认 mine。
- [ ] 每项目 open/overdue/blocked/overdueMilestone 计数正确 + RYG 与前端一致;worst-first 排序。
- [ ] 无 token → 401。backend 384→395 green,checkstyle/tsc/eslint clean。
