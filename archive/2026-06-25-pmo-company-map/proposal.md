# Proposal — pmo-company-map (H3, v0.0.110)

## 命门
PMO / 高管 需要一张「公司项目地图」: 看全公司项目的 RYG, 并能按 组织 / 负责人 分组聚合。
v0.0.30 项目地图 PortfolioPage scope=all 已经能看全公司 flat 列表, 但缺：
1. 按 Project.organizationId 分组的卡片视图（依赖 H1 organizationId 已可寻址）；
2. 按 ownerUserId 分组的负责人视角；
3. 独立 PMO 入口（不和 PortfolioPage 混用 scope toggle）。

## 范围（实际增量）
1. **NEW backend `com.rainier.pmo`**:
   - `PmoPortfolioRow` DTO: { group: {id, name, type}, projects: List<PortfolioRow>, rygCount: {red, yellow, green, gray} }
   - `PmoPortfolioService.companyMap(groupBy)`:
     - 复用 `ScopeService.resolveProjectIds(username, "all")` + `PortfolioService.portfolio(ids)` 得到 List<PortfolioRow>
     - groupBy=organization → 按 row.organizationId 分组；查 Organization 拿 name/type；NULL → group=`{id:null, name:"未归属", type:null}`
     - groupBy=owner → 按 project.ownerUserId 分组；查 User 拿 name；NULL → group=`{id:null, name:"未指定", type:"USER"}`
     - groupBy=none / 其它 → 返回单个 group=`{id:null, name:"全公司", type:null}` 包所有 row
   - `PmoPortfolioController` GET `/api/pmo/portfolio?groupBy=organization|owner|none`：token-gated（all-users 可见; 后端不卡 admin, 与 PortfolioPage scope=all 一致, RYG 数据本就 all-users 可读）
2. **NEW `frontend/src/api/pmoPortfolio.ts`**: getPmoPortfolio(groupBy)
3. **NEW `frontend/src/pages/Pmo/PmoPortfolioPage.tsx`** (路径 `/pmo`)：
   - 顶部 group toggle (按组织 / 按负责人 / 不分组)
   - 每个 group 一张 DashboardCard：标题=group name + RYG 统计 chip + 项目数 + 展开项目列表
   - 复用 board-kit StatusChip / DashboardCard
4. **AppRoutes** 注册 `/pmo`
5. **AppLayout** 加 nav 入口「PMO」组「公司项目地图」, all-users 可见, 排在「数据看板」之后
6. **NEW changes/2026-06-25-pmo-company-map/spec.md**
7. **测试**:
   - `PmoPortfolioControllerTest`: groupBy=organization 返回按 org 切片 + RYG 计数；groupBy=owner 按 owner 切片；no-token → 401。
   - `PmoPortfolioPage.test.tsx`: render + toggle 切换 groupBy 触发 refetch。

## OutOfScope
- 高管定制视图（PMO 用同一页足够）
- 导出 CSV / Excel
- 趋势图 / 历史对比
- 后端 admin/role 卡控（PortfolioPage scope=all 全员可见, 同口径）

## commit
`feat(pmo-company-map): H3 PMO 公司项目地图页 (v0.0.110)`
