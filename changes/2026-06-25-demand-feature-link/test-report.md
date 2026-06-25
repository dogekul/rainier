# Test Report — C6 Requirement↔Feature 关联

## 运行
- `cd backend && mvn test` → 全绿。

## 结果
- 总数：**Tests run: 760, Failures: 0, Errors: 0, Skipped: 0**
- 新增：`RequirementFeatureLinkControllerTest` 8 个用例（TC-RFL-001..008）全过。
- 受影响存量：`LegacyProductCategoryCleanupTest` 期望表数 37 → 38（新增 `rainier_requirement_feature`）。

## 用例对应
| Scenario  | 测试方法                                        | 结果 |
| --------- | ----------------------------------------------- | ---- |
| TC-RFL-001 | post_validLink_returns201                       | pass |
| TC-RFL-002 | post_duplicate_returns409                       | pass |
| TC-RFL-003 | post_unknownRequirement_returns400              | pass |
| TC-RFL-004 | post_unknownFeature_returns400                  | pass |
| TC-RFL-005 | reverseLookup_endpointsReturnLink               | pass |
| TC-RFL-006 | delete_returnsNoContentAndRowVanishes           | pass |
| TC-RFL-007 | requirementDetail_enrichedWithFeatureIds        | pass |
| TC-RFL-008 | featureList_enrichedWithRequirementIds (批量富化) | pass |

## Caveats
- 路径冲突：`/api/requirements/{id}/features` 已被 v0.0.14 占用（sprint→feature 2 跳派生），
  C6 新增直接关联端点改为 `/api/requirements/{id}/linked-features` 以保留既有契约。
- 前端未动；后续如需 UI 展示 featureIds 再补 sub-change。
