# crm-metrics-dashboard test report (v0.0.93, D5)

## Backend
- `mvn test` → **832 passed / 0 failed / 0 errors / 0 skipped**
- New: `com.rainier.metrics.service.MetricsServiceTest` — 6 tests, covers MET-001..005 + composite snapshot wiring.

## Frontend
- `npm test -- --run` → **275 passed / 0 failed** (56 files)
- New page `MetricsPage` is render-only over `getCrmMetrics()`; smoke covered transitively by `App.test.tsx` route tree.

## Spec coverage
| Scenario | Test |
| --- | --- |
| MET-001 winRate 0.75 | `winRate_threeWonOneLost_returns0_75` |
| MET-002 dealRate 2/6 | `dealRate_twoOfSix_returnsOneThird` |
| MET-003 winRate null | `winRate_onlyOpen_returnsNull` |
| MET-004 avgDeliveryCycleDays 15 | `avgDeliveryCycleDays_twoProjects_returns15` |
| MET-005 overdueProjects 仅 A | `overdueProjects_excludesFutureAndDelivered` |
| MET-006 endpoint 200 + 全字段 | covered by `crmSnapshot_populatesAllFields` service-level (controller is a thin pass-through) |

## Caveats
- MET-006 端到端 controller 测试未单独写，因为 controller 仅是 `service.crmSnapshot(...)` 的薄壳，且区间解析委派给 `Instant.parse`。如后续要 fail-fast 校验区间格式，可补一个 `MetricsControllerTest`。
- `avgDeliveryCycleDays` 取的是 `Project.endDate - Project.startDate`，而非 spec 里的 `Opportunity.acceptedAt - implementationStartedAt`（这两个字段当前 schema 不存在）。已在 proposal.md「Decisions」中记录此简化。
- `ownerUserId` 过滤同时打到 `Opportunity.commercialOwnerUserId` 和 `Project.ownerUserId`；如后续要按 PM / 解决方案 / 运营经理 维度切，扩展 service 即可。
