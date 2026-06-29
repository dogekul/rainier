# Spec: C4 richer-contribution-metrics

## DTO

`ProfileResponse` 新增字段：
- `Contribution contribution`（nested）

`ProfileResponse.Contribution`：
```
Map<String, Long> tasksByStatus;        // keys: TaskStatus.ALL (含 0 计数)
Map<String, Long> storiesByStatus;      // keys: StoryStatus.ALL (含 0 计数)
long tasksThisWeek;
long tasksDoneThisWeek;
List<WeekBucket> weeklyTrend;           // 4 项,按周升序
```

`ProfileResponse.WeekBucket`：
```
String week;          // ISO-8601 周字符串如 "2026-W26"
long tasksDone;
long storiesDone;
```

> 兼容旧字段：`ownedStoryCount` / `assignedTaskCount` 不动。

## Week 划分
- 周一 00:00:00 UTC 为周起点（ISO-8601 周）
- `weekStart` = 本周一 00:00:00 UTC（Instant）
- `weeklyTrend` 包含最近 4 个周（含本周），按时间升序
- 周字符串格式：`YYYY-'W'ww`（基于 ISO 周年），如 `2026-W26`

## Service

`ContributionMetricsService`（`@Service`, `@Transactional(readOnly=true)`）：
- `Contribution computeFor(Long userId)`：组装所有字段
- 内部 helper `Instant weekStartUtc(Instant now, int weeksBack)` —— 纯函数，便于单测

`MeProfileService.aggregate(User me)`：末尾追加 `out.setContribution(metricsSvc.computeFor(me.getId()))`。

## 鉴权
- 不变 — 走 `/api/me/profile` 与 C3 `/api/users/{id}/profile` 既有 gate

## Repos

`TaskRepository`：
- `long countByAssigneeUserIdAndStatus(Long, String)`
- `long countByAssigneeUserIdAndCreateTimeGreaterThanEqual(Long, java.time.Instant)`
- `long countByAssigneeUserIdAndStatusAndUpdateTimeGreaterThanEqual(Long, String, Instant)`
- `long countByAssigneeUserIdAndStatusAndUpdateTimeBetween(Long, String, Instant, Instant)`

`StoryRepository`：
- `long countByOwnerUserIdAndStatus(Long, String)`
- `long countByOwnerUserIdAndStatusAndUpdateTimeBetween(Long, String, Instant, Instant)`

## Test

`ContributionMetricsServiceTest`（`@SpringBootTest`, `@ActiveProfiles("test")`, 测试同库清理）：
- `byStatus_grouped`：seed alice 5 task 不同 status → tasksByStatus 计数符合，未涉及的 status 键为 0
- `thisWeek_counts`：seed 本周新增 + 上周新增 → tasksThisWeek 仅算本周；DONE 且本周完成 → tasksDoneThisWeek 准确
- `weeklyTrend_fourWeeks`：trend 长度 4 严格升序

`MeProfileControllerTest`（已有 8 用例）：保持现行 jsonPath 不动，新加 1 用例：
- `profile_contributionPayload`：alice 有 task 与 story → 响应包含 contribution.tasksByStatus / weeklyTrend (size 4)

## OutOfScope
按上面 proposal Non-Goals。
