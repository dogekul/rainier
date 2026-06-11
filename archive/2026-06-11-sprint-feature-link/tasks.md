# v0.0.14-sprint-feature-link 任务清单

## 1. entity-sprint：productId（P0）

- [x] 1.1 Sprint entity 加 `productId`（nullable）+ getter/setter
- [x] 1.2 `SprintCreateRequest` 加可选 `productId`；`SprintUpdateRequest` 不加（不可变）（依赖 #1.1）
- [x] 1.3 `SprintDetail` 加 `productId` + `productName` + from() 映射（依赖 #1.1）
- [x] 1.4 `SprintService.create` 接受可选 productId（非空时校验 Product 存在，注入 ProductRepository）（依赖 #1.2）
- [x] 1.5 `SprintService.update` 不碰 productId（不可变）（依赖 #1.2）
- [x] 1.6 `SprintService` enrich/list 加 product batch join（≥3∧≤6 预算，复用既有 4 维 batch）（依赖 #1.3）

## 2. entity-sprint-feature 骨架（P0）

- [x] 2.1 `SprintFeatureLink`（BaseEntity, `@Table uk_sprint_feature(sprint_id,feature_id)`, 硬删无 @SQLDelete）
- [x] 2.2 `SprintFeatureLinkCreateRequest{sprintId, featureId}` + `SprintFeatureLinkDetail`（依赖 #2.1）
- [x] 2.3 `SprintFeatureView`（feature 富化：featureId/code/name/moduleId/status）+ `FeatureSprintView`（sprint 富化：sprintId/code/name/status/requirementId/productId）（依赖 #2.1）
- [x] 2.4 `SprintFeatureLinkRepository`（existsBySprintIdAndFeatureId / findBySprintId / findByFeatureId / countBySprintId / countByFeatureId）（依赖 #2.1）

## 3. entity-sprint-feature service + controller（P0）

- [x] 3.1 `SprintFeatureLinkService.create`：校验链 sprint 存在→feature 存在→feature.moduleId→module.productId 解析→productId 惰性写入 OR 一致性校验→唯一性→save；注入 SprintRepo+FeatureRepo+ProductModuleRepo+SprintFeatureLinkRepo；DataIntegrityViolation→409（依赖 #1.1, #2.4）
- [x] 3.2 `SprintFeatureLinkService.delete` 硬删 + list（依赖 #3.1）
- [x] 3.3 `findFeaturesBySprint`（sprint 存在校验 + feature 富化）（依赖 #3.1）
- [x] 3.4 `findSprintsByFeature`（feature 存在校验 + sprint 富化）（依赖 #3.1）
- [x] 3.5 `findFeaturesByRequirement`（2 跳 LinkedHashSet 去重 + 注入 SprintRepo + feature batch 富化）（依赖 #3.1）
- [x] 3.6 `SprintFeatureLinkController` /api/sprint-features POST/GET/{id}/GET list/DELETE（依赖 #3.2）

## 4. 反查端点挂拥有方（P0）

- [x] 4.1 `SprintController` +GET /{id}/features → linkService.findFeaturesBySprint（依赖 #3.3）
- [x] 4.2 `FeatureController` +GET /{id}/sprints → linkService.findSprintsByFeature（依赖 #3.4）
- [x] 4.3 `RequirementController` +GET /{id}/features → linkService.findFeaturesByRequirement（依赖 #3.5）

## 5. backend 测试（P0）

- [x] 5.1 `SprintFeatureLinkControllerCreateTest`（TC-SF-001..006 惰性/匹配/跨产品/唯一/sprint404/feature404）（依赖 #3.6）
- [x] 5.2 `SprintFeatureLinkControllerDeleteTest`（TC-SF-008..009 硬删 + productId 不回退）+ Query（TC-SF-007）（依赖 #3.6）
- [x] 5.3 `SprintControllerProductIdTest`（TC-SPR-PF-001..004 null/预绑/Update 忽略/详情富化）（依赖 #1.6）
- [x] 5.4 反查测试 3 端点（TC-SF-REV-001..008）（依赖 #4.1, #4.2, #4.3）
- [x] 5.5 perf 2（TC-PERF-SPR-PF-001 sprint list ≥3∧≤6 / TC-PERF-SF-REV-001 requirement→features ≥2∧≤8）（依赖 #1.6, #3.5）

## 6. 前端（P0）

- [x] 6.1 `api/sprintFeature.ts`（create/delete/list + getSprintFeatures(反查) / getFeatureSprints(反查)）+ `api/sprint.ts` 加 productId/productName + `api/requirement.ts` 加 getRequirementFeatures（依赖 #3.6, #4.x）
- [x] 6.2 Sprint 关联功能面板（挂载/解绑 + feature 下拉按 productId 过滤）（依赖 #6.1）
- [x] 6.3 Feature 所在迭代显示（依赖 #6.1）
- [x] 6.4 前端测试（TC-FES-SF-001..003）（依赖 #6.2, #6.3）

## 7. 测试与验证（P0）

- [x] 7.1 全量 backend `mvn test` 通过（预期 ≈ 292）
- [x] 7.2 全量 frontend `npx vitest run` + `tsc --noEmit` 通过（预期 ≈ 54）
- [x] 7.3 E2E：docker compose 重建 + SHOW TABLES=17 + 存量 sprint product_id 仍 null（TC-E2E-SF-001）
- [x] 7.4 E2E curl 全链（挂 feature 锁产品 → 同产品 → 跨产品 400 → 反查 3 端点 → 解绑 204）（TC-E2E-SF-002）
