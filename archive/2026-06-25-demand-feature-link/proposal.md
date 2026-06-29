# C6: Requirement ↔ Feature 直接关联

## Why
- Demand 已经能落地为 Requirement（DemandRequirementLink）。
- Requirement 只能通过 Sprint→Feature 二跳引用 Feature；缺一条产品同学常用的"这个需求要落到哪几个 Feature"
  的直接关联。
- 没有直接关联就没法在需求详情里直观展示对应功能、也没法做 Feature 视角的"覆盖了哪些需求"反查。

## What
新增第三张关联表 `rainier_requirement_feature`：

- `requirementId` × `featureId`，唯一约束 `(requirement_id, feature_id)`。
- 记录建立时间 `linkedAt` 与建立人 `linkedByUserId`（可空，沿用 AuthzService.currentUserId 拿当前登录）。
- 硬删除（与 DemandRequirementLink/SprintFeatureLink 一致）。

## API
- `POST /api/requirement-features` body `{requirementId, featureId}` → 201 + Detail
- `DELETE /api/requirement-features/{id}` → 204
- `GET /api/requirements/{id}/linked-features` → 该需求的直接 link 列表
- `GET /api/features/{id}/requirements` → 该 Feature 的直接 link 列表

注：`/api/requirements/{id}/features` 已被 v0.0.14 占用（sprint→feature 2 跳派生），
新增端点用 `linked-features` 区分，避免破坏既有调用方。

## Enrichment
- `RequirementDetail.featureIds: Long[]`
- `FeatureDetail.requirementIds: Long[]`（列表接口走批量富化）

## Out of scope
- 自动推荐 link（机器学习/规则推断）
- Demand→Feature 直接关联（Demand→Requirement 已存在，链路足够）
