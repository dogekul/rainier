# Capability: entity-portfolio

> Change log:
> - 2026-06-17 (v0.0.28-scope-substrate) — NEW. The reusable scope-aggregation substrate behind every
>   dashboard. `ScopeService.resolveProjectIds(username, scope)` resolves a caller's project set for
>   scope `mine` (projects the caller has a UserRole on ∪ owns), `led` (projects tagged to an org the
>   caller HEADs AND its org-subtree via parentId BFS), or `all` (company-wide). `PortfolioService`
>   rolls a project-id set up into per-project open/overdue/blocked task counts + overdue-milestone
>   count + deterministic RYG (`Ryg.tier`, mirrors frontend `ryg.ts`), sorted worst-first. Exposed as
>   `GET /api/me/portfolio?scope=` (token-gated). Reuses the project↔org edge (entity-project
>   `organizationId`) and the existing task/milestone list endpoints; 0 new tables.

## ADDED Requirements

### Requirement: scoped project-health portfolio

后端 SHALL 通过 `GET /api/me/portfolio?scope=mine|led|all` 返回当前用户该作用域下每个项目的健康汇总
`{projectId, projectCode, projectName, projectStatus, organizationId, openTasks, overdueTasks,
blockedTasks, overdueMilestones, ryg}`,按 RYG 红→黄→绿→灰 排序。Token-gated（身份来自 SecurityFilter）。

#### Scenario: scope=mine 返回我有角色或负责的项目

- **GIVEN** 用户在项目 A 有 UserRole、拥有项目 B、与项目 C 无关
- **WHEN** `GET /api/me/portfolio?scope=mine`
- **THEN** 响应 SHALL 含 A、B,不含 C

#### Scenario: scope=led 递归组织子树

- **GIVEN** 用户是部门 D 的 HEAD,D 下有团队 T;项目 P1 挂 T、P2 挂 D、P3 无组织
- **WHEN** `GET /api/me/portfolio?scope=led`
- **THEN** 响应 SHALL 含 P1、P2,不含 P3

#### Scenario: scope=all 返回全部项目

- **GIVEN** 系统有 N 个项目
- **WHEN** `GET /api/me/portfolio?scope=all`
- **THEN** 响应 SHALL 含全部 N 个项目

#### Scenario: 每项目计数与 RYG

- **GIVEN** 某项目有 10 个开放任务,其中 4 个逾期、0 个阻塞
- **WHEN** portfolio 计算
- **THEN** 该项目 `openTasks=10`、`overdueTasks=4`、`ryg="RED"`（逾期比 0.4 > 0.3）;开放任务含阻塞则 RED;
  少量逾期 YELLOW;开放且无逾期/阻塞 GREEN;无开放 GRAY

#### Scenario: 缺 token

- **GIVEN** 无 Authorization 头
- **WHEN** `GET /api/me/portfolio`
- **THEN** 后端 SHALL 返回 401
