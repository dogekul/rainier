# crm-metrics-dashboard proposal (v0.0.93, D5)

## What
新增 **CRM 度量看板** —— 一个 `MetricsService` 聚合 4 个核心指标（成单率 / 中标率 / 平均交付周期 / 逾期项目督办清单），通过 `GET /api/metrics/crm` 暴露，前端在导航「数据看板」下新增 `/metrics` 页用纯数字卡片 + 表格呈现。

## Why
售前→交付→运营全链路已落地（Opportunity / Project / Operation），但缺一处「整体健康度」视图。本版给团队/管理者一个最小可用的飞轮 KPI 仪表盘：知道当前赢单率多少、合同签约率多少、平均交付多少天、哪些项目逾期。

## Scope
- NEW package `com.rainier.metrics`
- NEW `MetricsService`：
  - `winRate(periodStart, periodEnd, ownerUserId?)` —— 中标率 = WON / (WON + LOST)（区间内 createTime 落在 [start, end) 的商机）
  - `dealRate(periodStart, periodEnd, ownerUserId?)` —— 成单率 = WON / 总商机数（同区间）
  - `avgDeliveryCycleDays(periodStart, periodEnd, ownerUserId?)` —— Project.status=DELIVERED 且 endDate 落在区间的项目的 (endDate - startDate) 平均值；无样本返回 null
  - `overdueProjects(ownerUserId?)` —— Project.endDate < today 且 status NOT IN (DELIVERED, ARCHIVED)
- NEW DTO `CrmMetrics` `OverdueProjectRow`
- NEW `MetricsController` `GET /api/metrics/crm?periodStart=&periodEnd=&ownerUserId=&scope=` —— all-users（token-optional）
- NEW frontend page `frontend/src/pages/MetricsPage.tsx` —— 卡片展示三比率 + 表格列逾期项目
- NEW route `/metrics`，在导航「数据看板」组下挂入口
- 测试：`MetricsServiceTest` seed 多个 opportunity/project → 校验比率与逾期清单

## OutOfScope
- 真实图表（仅纯数字卡片 + 表格）
- 自动督办通知（A8 NotificationService 已具，本版不联动）
- ownerUserId 过滤的精细权限（只按 owner 字段简单 where）

## Decisions
- 接口区间用 `Instant` ISO-8601 字符串接收（如 `2026-06-01T00:00:00Z`），缺省时间为 [近 90 天, 现在)；都缺省也允许
- `winRate` 与 `dealRate` 用 long count 查询，避免加载实体
- `avgDeliveryCycleDays` 返回 `Double`，null 表示无样本（前端展示「—」）
- 逾期项目用 `Project.endDate` 作为「expectedEndDate」（schema 复用既有列）
- ownerUserId 同时匹配 `Opportunity.commercialOwnerUserId` 与 `Project.ownerUserId`（最简策略）
- 不引入新依赖；纯 JPA Specification / count query
