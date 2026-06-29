# test-report — Project Implementation Form (D1, v0.0.89)

## Backend
- `cd backend && mvn test` → **801 passed, 0 failed**, 0 errors.
- New tests:
  - `ProjectImplementationServiceTest` (3 tests)
    - PIF-001/002 `createOrUpdate_idempotent` — 同 projectId 二次 PUT 复用 id；count=1
    - PIF-005 `createOrUpdate_unknownProject_throws` — 不存在 projectId → `BadRequestException`
    - PIF-003 `findByProjectId_missing_throws` → `NotFoundException`
  - `ProjectImplementationControllerTest` (4 tests)
    - PIF-003 `get_missing_returns404`
    - PIF-001+004 `put_then_get` — PUT 200 → GET 200，字段一致
    - PIF-002 `put_twice_isUpsert` — 同 projectId 第二次返回相同 id
    - PIF-006 `put_blankScope_returns400` — `@NotBlank` 触发 400
- Updated `LegacyProductCategoryCleanupTest`: 表总数 39 → 40（含新表 `rainier_project_implementation`）。

## Frontend
- `cd frontend && npm test -- --run` → **274 passed, 0 failed**（56 个 test 文件）。
- 未为新增 `ProjectImplementationPanel` 单独加 vitest（沿用既有 panel 风格 = 仅集成在 ProjectDetailPage 内，受总体 smoke test 覆盖）；后续可补一个 `ProjectImplementationPanel.test.tsx`。

## 验证矩阵
| Scenario | 测试 | 状态 |
|---|---|---|
| PIF-001 | `put_then_get` | ✅ |
| PIF-002 | `put_twice_isUpsert` / `createOrUpdate_idempotent` | ✅ |
| PIF-003 | `get_missing_returns404` / `findByProjectId_missing_throws` | ✅ |
| PIF-004 | `put_then_get` | ✅ |
| PIF-005 | `createOrUpdate_unknownProject_throws` | ✅ |
| PIF-006 | `put_blankScope_returns400` | ✅ |
