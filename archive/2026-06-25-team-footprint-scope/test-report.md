# Test Report — team-footprint-scope (H2, v0.0.109)

## Backend (mvn test)
- 891 tests, 0 failures, 0 errors, 0 skipped
- 新增 `ScopeServiceFootprintTest` — 5 tests，全部通过
  - `teamFootprintProjects_includesSubtreeMembersOwnedAndRoledProjects` (Scenario A)
  - `resolveProjectIds_footprintDelegates` (Scenario B)
  - `teamFootprintProjects_nonHeadReturnsEmpty` (Scenario C)
  - `teamFootprintProjects_excludesLeftMembers` (Scenario D)
  - `teamFootprintProjects_nullLeaderReturnsEmpty`（防御性 null guard）
- `PortfolioControllerTest` 加 1 test — `portfolioFootprint_includesUntaggedMemberOwnedProjects` (Scenario E)
  - 验证 `scope=footprint` 能捞出 `organizationId IS NULL` 的成员项目

## Frontend (vitest --run)
- 63 files, 309 tests, 全绿
- `TeamLeadPage.test.tsx` 加 2 tests:
  - `requests footprint scope by default (TC-TL-FP-01)` — Scenario F
  - `refetches with led scope when toggled (TC-TL-FP-02)` — Scenario G

## Coverage map
| Scenario | Test |
| --- | --- |
| A — footprint walks subtree | `ScopeServiceFootprintTest#teamFootprintProjects_includesSubtreeMembersOwnedAndRoledProjects` |
| B — resolveProjectIds 桥接 | `ScopeServiceFootprintTest#resolveProjectIds_footprintDelegates` |
| C — 非 HEAD 返回空 | `ScopeServiceFootprintTest#teamFootprintProjects_nonHeadReturnsEmpty` |
| D — leftAt 排除 | `ScopeServiceFootprintTest#teamFootprintProjects_excludesLeftMembers` |
| E — PortfolioController footprint | `PortfolioControllerTest#portfolioFootprint_includesUntaggedMemberOwnedProjects` |
| F — 前端默认 footprint | `TeamLeadPage.test.tsx#TC-TL-FP-01` |
| G — toggle 切到 led | `TeamLeadPage.test.tsx#TC-TL-FP-02` |

## Caveats
- 同一人 HEAD 多个独立树 → 已 distinct 处理，但未单测覆盖
- 项目活跃度过滤、子团队分组 — OutOfScope，留给 H3+
