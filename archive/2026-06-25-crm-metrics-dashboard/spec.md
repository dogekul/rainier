# crm-metrics-dashboard spec (v0.0.93, D5)

## Scenario MET-001 — winRate 计算
GIVEN 区间内有 3 个 WON、1 个 LOST、2 个 OPEN 商机
WHEN 调用 `MetricsService.winRate(start, end, null)`
THEN 返回 0.75（3 / (3+1)）

## Scenario MET-002 — dealRate 计算
GIVEN 区间内 6 个商机（WON=2，LOST=1，OPEN=3）
WHEN 调用 `MetricsService.dealRate(start, end, null)`
THEN 返回 2/6 ≈ 0.333

## Scenario MET-003 — winRate 无样本
GIVEN 区间内仅有 OPEN 商机
WHEN 调用 `MetricsService.winRate(...)`
THEN 返回 null（拒绝除零）

## Scenario MET-004 — avgDeliveryCycleDays
GIVEN 2 个 DELIVERED 项目：(start=2026-01-01, end=2026-01-11, 10d) 与 (start=2026-02-01, end=2026-02-21, 20d)
WHEN 调用 `MetricsService.avgDeliveryCycleDays(...)`
THEN 返回 15.0

## Scenario MET-005 — overdueProjects
GIVEN 3 个项目：A endDate=昨天 status=ACTIVE / B endDate=明天 status=ACTIVE / C endDate=昨天 status=DELIVERED
WHEN 调用 `MetricsService.overdueProjects(null)`
THEN 仅返回 A（B 未到期；C 已交付）

## Scenario MET-006 — `GET /api/metrics/crm` 返回 200 并含全部字段
GIVEN seed 数据
WHEN GET `/api/metrics/crm`
THEN 200，body 含 `winRate / dealRate / avgDeliveryCycleDays / overdueProjects[]`
