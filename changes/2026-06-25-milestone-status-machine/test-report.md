# Test Report — milestone-status-machine (C7, v0.0.87)

## Backend

`cd backend && mvn test`

- **Total**: 781 tests, 0 failures, 0 errors, 0 skipped → BUILD SUCCESS
- 重点用例：
  - `MilestoneStatusMachineTest` — 14/14（合法/非法转换全覆盖 + normalize 别名）
  - `MilestoneServiceTransitionTest` — 7/7（PUT/POST transition、actualDate 自动填、legacy alias）
  - `MilestoneControllerTest` — 14/14（既有 CRUD + 调整后的 TC-MILE-002/005/010）
  - `ProjectMilestoneCascadeTest` — 2/2
  - `PortfolioControllerTest` — 6/6（DONE 视为已达成）

## 调整既有用例

- `TC-MILE-002`：legacy `REACHED` 创建后 expect `DONE`（normalize 结果）
- `TC-MILE-005`：旧 invalid token 改 `XXX`（"DONE" 现在合法）
- `TC-MILE-010`：拆成两步 PLANNED → IN_PROGRESS → DONE（旧的 PLANNED 直跳 REACHED 不再合法）

## 未跑

- 前端 `npm test`：本批未触前端代码（无前端改动）。
