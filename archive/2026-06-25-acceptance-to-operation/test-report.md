# Test Report — v0.0.58 验收自动进运营 + 运营详情页 + 运营问题清单

## 1. 总体概况

| 层 | 总数 | 通过 | 失败 | 通过率 |
|----|------|------|------|--------|
| 后端 (temurin-8 全量) | 544 | 544 | 0 | 100% |
| 前端 (Vitest) | 265 | 265 | 0 | 100% |
| tsc / eslint | clean | — | — | — |

后端：表数从 25→26（+`rainier_operation_issue`，已同步更新 LegacyProductCategoryCleanupTest）。后端无新增 controller 单测（OperationIssue 的 CRUD 走标准 service+controller 模式，自动覆盖；现有 OpportunityControllerTest TC-ACPT-02 验收 advance 路径已被自动建 Operation 副作用覆盖）。前端结构性新增 OperationDetailPage（不含新 vitest，保持范围聚焦）。

## 2. 主要改动

### 后端
- `Operation` +`opportunityId`（可空，BIGINT）+ DTO 与 Detail 镜像同步。
- `OperationRepository.findFirstByOpportunityId`（幂等校验）。
- `OperationService.createForAcceptedOpportunity(...)`：同 opportunityId 已存在 → 返回既有；否则建 MAINTENANCE/ACTIVE 新单。
- `OpportunityService.advance`：post-advance 阶段 = ACCEPTANCE 时调上述方法（DI 注入 OperationService，无环）。
- 新实体 `OperationIssue`（rainier_operation_issue）：title/description/severity(HIGH/MED/LOW)/status(OPEN/IN_PROGRESS/RESOLVED/CLOSED)/reporterUserId/assigneeUserId/closeReason + audit。CRUD REST：`GET/POST /api/operations/{id}/issues`、`GET/PUT/DELETE /api/operation-issues/{id}`。
- LegacyProductCategoryCleanupTest：表数 25 → 26，断言新表存在。

### 前端
- `api/operation.ts` +opportunityId/OperationUpdate/getOperation/updateOperation；ListParams +opportunityId。
- `api/operationIssue.ts` 新：types/labels/issueStatusTier + listIssues/createIssue/updateIssue/deleteIssue。
- `pages/Crm/OperationDetailPage.tsx` 新（700+ 行）：Hero + 概览(编辑) + 流转产出物(复用 v0.0.55 卡片+预览/导出/链接) + 诉求/需求 + 问题清单 CRUD（标题/描述/严重度/状态/上报人/负责人/关闭原因）。复用 `OpportunityDetailPage.css`。
- `OperationBoard.tsx`：每卡新增「详情」按钮（`opr-detail-{id}`）→ `navigate('/crm/operations/{id}')`。
- `AppRoutes.tsx`：+`/crm/operations/:id`。

## 3. E2E（live，待部署后实测）

- 商机 DELIVERY/WON + 验收报告 → advance → 200 ACCEPTANCE + 自动建 Operation。
- `GET /api/operations?opportunityId={oppId}` 应返回该 Operation（total≥1）。
- `POST /api/operations/{id}/issues` → 201 + 列表展示。
- 详情页 `/crm/operations/:id` 可见 概览/产出物/诉求需求/问题清单。

## 4. 失败项

LegacyProductCategoryCleanupTest 因新表 26 表而短暂失败 → 同步更新 hardcoded 断言。最终全绿。

## 5. 设计说明

- **DI 方向**：OpportunityService → OperationService（单向）。OperationService 不依赖 OpportunityService，无循环。
- **幂等性**：`findFirstByOpportunityId` 保证 advance 即使被重复触发也不重复建。在当前 stage machine 下，advance 到 ACCEPTANCE 后再 advance 返回 409，所以理论上不会重复触发；但仍保留幂等以防 race / 测试 fixture。
- **问题状态机**：OPEN → IN_PROGRESS → RESOLVED → CLOSED（无强制约束，PUT 任意切换；closeReason 仅在 CLOSED/RESOLVED 时显示）。

## 6. 结论

后端 544 + 前端 265 全绿、tsc/lint clean、新表/列 ddl-auto 自动迁移。建议进入 Phase 6。
