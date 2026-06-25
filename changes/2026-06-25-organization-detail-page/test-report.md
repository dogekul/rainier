# Test report — organization-detail-page (E3, v0.0.99)

## Backend
- `mvn test` → **845 tests, 0 failures, 0 errors, 0 skipped** (15.1s)
- 新增：`OrganizationAuditEndpointTest` 3 tests
  - `auditLog_filtersByEntityTypeAndId` ✔
  - `auditLog_filtersByAction` ✔
  - `auditLog_unknownOrg_returns404` ✔

## Frontend
- `npm test -- --run Organization` → **3 tests passed**
  - `OrganizationDetailPage.test.tsx`：渲染基本信息 + 6 个 tab ✔
  - `OrganizationsPage.test.tsx`（已有，更新为 MemoryRouter 包裹）✔
  - `EditDrawer.test.tsx`（已有，未变）✔

## Caveats
- 变更历史 tab 仅展示 `entityType=ORGANIZATION` 行（OutOfScope：ORGANIZATION_PMO 行的聚合）。
- 关联项目客户端过滤（list 拉 200 条本地 filter），因 `/api/projects` 暂无 `organizationId` 查询参数；后续若数据膨胀需补后端参数。
- 详情页本版只读，编辑入口仍在 `OrganizationsPage` 行操作中。
