# test-report — crm-full-link-visualization (v0.0.94)

## Backend
- 命令：`cd backend && mvn test`
- 结果：**Tests run: 835, Failures: 0, Errors: 0, Skipped: 0**
- 新增：`FullLinkServiceTest` 3 个场景全绿
  - Scenario A — complete chain（ACCEPTANCE + project + operation + customer + activities + artifact）
  - Scenario B — early chain（LEAD only，project/operation/customer 皆 null）
  - Scenario C — 未知 opportunity → NotFoundException

## Frontend
- 命令：`cd frontend && npm test -- --run`
- 结果：**Test Files 56 passed, Tests 275 passed**
- `npx tsc --noEmit` 通过（FullLinkPanel + opportunity.ts 新 API 无类型错）

## 风险/缺口
- artifactCount 当前只把「opportunity 维度全量产出物」挂到 current stage 上（OpportunityArtifact 无 stage 列），可接受
- 暂未为 FullLinkPanel 加 component-level 单测（依赖 react-router + axios mock，后续 E 批补）
