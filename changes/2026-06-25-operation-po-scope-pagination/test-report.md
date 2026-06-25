# Test Report — D7 运营 PO scope + 问题分页 + 转工单 (v0.0.95)

## Backend
- `mvn test` → 842 passed, 0 failed, 0 errors.
- 新增覆盖：
  - `MeInboxControllerTest.inbox_productIdFilter_keepsOnlyMatchingOppRequirements` (TC-INBOX-PROD-001)
  - `MeInboxControllerTest.inbox_noProductId_originalBehaviour` (TC-INBOX-PROD-002)
  - `OperationIssuePaginationConvertTest.list_paged_returnsCorrectSlice` (TC-OPI-PAGE-001)
  - `OperationIssuePaginationConvertTest.list_paged_statusFilter` (TC-OPI-PAGE-002)
  - `OperationIssuePaginationConvertTest.convertToTask_createsTaskAndMarksConverted` (TC-OPI-CONV-001)
  - `OperationIssuePaginationConvertTest.convertToTask_invalidProject_returns400` (TC-OPI-CONV-002)
  - `OperationIssuePaginationConvertTest.convertToTask_unknownIssue_returns404` (TC-OPI-CONV-003)

## Frontend
- `npm test -- --run` → 275 passed.
- `npm run build` → green (492 KB JS / 33 KB CSS).
- 新行为：OperationDetailPage 问题清单加客户端分页 (10/页) + 「转工单」按钮调用 `POST /api/operation-issues/{id}/convert-to-task`。

## Caveats
- 客户端分页基于本地全量 issues (复用既有 listOperationIssues)；服务端 `/issues/page` endpoint 已实现并测试覆盖，前端切换为它属 E 批打磨。
- 「转工单」沿用 `window.prompt/alert` 轻量交互；正规对话框留待 E 批。
- D7 PO product 过滤通过 Opportunity.productId 间接达成 (Demand/Requirement 无 productId 列)；未来若加显式 PO 关联表再演进。
