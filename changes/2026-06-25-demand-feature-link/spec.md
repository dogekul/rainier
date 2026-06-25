# Spec: Requirement ↔ Feature Direct Link

## Entity
`RequirementFeatureLink`（package `com.rainier.requirementfeature`）映射 `rainier_requirement_feature`：

| 字段             | 类型     | 备注                                       |
| ---------------- | -------- | ------------------------------------------ |
| id               | BIGINT   | BaseEntity (auto-increment)                |
| requirement_id   | BIGINT   | NOT NULL                                   |
| feature_id       | BIGINT   | NOT NULL                                   |
| linked_at        | DATETIME | nullable，insert 时由 Service 写入 now()   |
| linked_by_user_id| BIGINT   | nullable，AuthzService.currentUserId       |
| create_by/time, update_by/time, del_flag | BaseEntity 默认；hard delete，del_flag 始终 false |

UniqueConstraint `uk_requirement_feature(requirement_id, feature_id)`。

## Service：`RequirementFeatureLinkService`
- `link(req, currentUserId)`：
  - 校验 requirementId / featureId 存在，否则 400。
  - 已存在 link → 409 `link already exists`。
  - DataIntegrityViolationException → 同样 409（并发场景）。
- `unlink(id)`：not found → 404；成功 204。
- `listByRequirement(requirementId) / listByFeature(featureId)`：父实体缺失 → 404。
- 富化辅助：
  - `findFeatureIdsByRequirement(id) → List<Long>` —— 去重保序。
  - `findRequirementIdsByFeature(id) → List<Long>`。
  - `findFeatureIdsByRequirementIds(ids) / findRequirementIdsByFeatureIds(ids)` —— 批量富化。

## REST
- `POST /api/requirement-features` `{requirementId, featureId}` → 201 RequirementFeatureLinkDetail。
- `DELETE /api/requirement-features/{id}` → 204。
- `GET /api/requirement-features/by-requirement/{requirementId}` → List。
- `GET /api/requirement-features/by-feature/{featureId}` → List。
- `GET /api/requirements/{id}/linked-features` → List。
- `GET /api/features/{id}/requirements` → List。

`POST` 在 controller 里通过 `AuthzService.currentUserId(req)` 拿当前用户 id；匿名请求允许（uid=null）。

## DTO 富化
- `RequirementDetail.featureIds: List<Long>` —— `enrich()` 内调 `findFeatureIdsByRequirement`。
- `FeatureDetail.requirementIds: List<Long>` —— 单条 enrich 走单查；list 接口走
  `findRequirementIdsByFeatureIds` 批量。

## Scenarios（测试覆盖）

### TC-RFL-001 合法创建
POST 带合法 requirementId+featureId → 201；响应里 requirementId/featureId 回写；linkedAt 非空。

### TC-RFL-002 唯一约束
同一 `(requirementId, featureId)` 二次 POST → 409 `link already exists`。

### TC-RFL-003 未知 requirementId
POST featureId 合法、requirementId 不存在 → 400 `requirement not found`。

### TC-RFL-004 未知 featureId
POST requirementId 合法、featureId 不存在 → 400 `feature not found`。

### TC-RFL-005 反查
建 link 后 `GET /api/requirements/{rid}/linked-features` 含该 link；
`GET /api/features/{fid}/requirements` 含该 link。

### TC-RFL-006 硬删
DELETE 后 by-requirement 列表清空；DB 行消失。

### TC-RFL-007 RequirementDetail.featureIds 富化
两条 link 之后 `GET /api/requirements/{id}` 的 `featureIds` 含两个 featureId（去重保序）。

### TC-RFL-008 FeatureDetail.requirementIds 富化（list 端点批量）
两条 link 之后 `GET /api/features?` 列表 item 的 `requirementIds` 含两个 requirementId。

## Out of scope
- 自动推荐、Demand→Feature 直接关联。
- 软删除。
