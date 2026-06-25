# test-report: A6 风险雷达（规则版） (v0.0.70)

## 新增类

- `backend/src/main/java/com/rainier/risk/RiskRule.java` — 规则接口（name + evaluate）。
- `backend/src/main/java/com/rainier/risk/RiskFinding.java` — 不可变 finding POJO (level / message / entityType / entityId / ruleName)。
- `backend/src/main/java/com/rainier/risk/RiskContext.java` — 规则输入载体 (userId / projectIds / now)。
- `backend/src/main/java/com/rainier/risk/RiskService.java` — 注入 List<RiskRule>，按 ScopeService 解析项目范围后聚合。
- `backend/src/main/java/com/rainier/risk/rules/OverdueTaskRule.java` — WARN 过期未 DONE task。
- `backend/src/main/java/com/rainier/risk/rules/BlockedStoryRule.java` — CRIT BLOCKED story。
- `backend/src/main/java/com/rainier/risk/rules/SprintEndingNoDoneRule.java` — WARN 3 天内结束且无 DONE task 的 Sprint。
- `backend/src/main/java/com/rainier/risk/controller/MeRiskController.java` — `GET /api/me/risks?scope=mine`。
- `backend/src/test/java/com/rainier/risk/RiskRulesIntegrationTest.java` — 3 规则单测 + RiskService 聚合 + 端点 + 401。

## 修改类

- `backend/src/main/java/com/rainier/story/repository/StoryRepository.java` — 新增 `findByProjectIdIn(Collection<Long>)`。
- `backend/src/main/java/com/rainier/requirement/repository/RequirementRepository.java` — 新增 `findByProjectIdIn(Collection<Long>)`。
- `backend/src/main/java/com/rainier/sprint/repository/SprintRepository.java` — 新增 `findByRequirementIdIn(Collection<Long>)`。
- `backend/src/main/java/com/rainier/task/repository/TaskRepository.java` — 新增 `findBySprintIdIn(Collection<Long>)`。

## DDL 变化

无新增表 / 无新增字段（纯查询，仅复用现有 entity）。

## 测试

- `RiskRulesIntegrationTest`: 6 tests pass (3 个规则单测 + RiskService.runAll + 端点 200 + 401)。
- 全量后端: **630 tests pass，0 failure / 0 error / 0 skipped**。
- 前端: 本次未改前端，未运行。

## 端点

- `GET /api/me/risks?scope=mine` (all-users，token-gated)：返回 `RiskFinding[]`。

## Caveats

- 规则当前 fetch all 然后内存过滤，规模大时应改用 Specification + DB-side predicate；A8 主动推送对接前再优化。
- `RiskService.runAll` 无去重 / 排序；前端展示后续按 level (CRIT > WARN > INFO) 排序。
- 仅扫描 in-scope projects；scope=all 暴露给所有用户即风险全集，未来需要 RBAC 收紧。
