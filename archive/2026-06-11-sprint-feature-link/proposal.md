# v0.0.14-sprint-feature-link — 经 Sprint 桥接产品域与需求域

## Why

v0.0.13 后，产品域（product → module → feature）仍是孤岛。团队答不出三个常见追溯问题：「这个功能为哪些需求建？」「这个需求落到哪些功能？」「这个诉求最终被哪些功能满足？」本版用 **Sprint（产品迭代）当桥**把两域接通——Sprint 已经挂在 Requirement 上（需求侧），只要再让它挂上 Feature（产品侧），整条「诉求→需求→迭代→功能」追溯链就通了。

链路：
```
诉求 ──DemandRequirement── 需求 ──Sprint.requirementId(已有)── Sprint{+productId} ──SprintFeatureLink(新建)── 功能
Demand                    Requirement    一需求 N sprint           一 sprint M feature          Feature
```

## What Changes

- **C1.** **Sprint 加 `productId`（nullable）** —— sprint 成为"产品迭代"，显式持有所属产品，成为 project 域↔product 域的桥。采用 nullable 而非 NN（理由见 Q1）。
- **C2.** **新建 `sprint-feature` capability** —— `SprintFeatureLink(sprintId, featureId)` M:N 链接表，仿 `DemandRequirementLink`：硬删、`(sprint_id, feature_id)` 唯一约束、配反查端点。
- **C3.** **link-create 校验链**：sprint 存在 + feature 存在 + **产品一致性**——feature 的产品（`feature.moduleId → module.productId`）必须等于 sprint.productId；不一致 → 400 `"feature must belong to the sprint's product"`。
- **C4.** **productId 惰性建立 + 不可变**：sprint.productId 为 null 时，挂入的**第一个 feature 反推确定** sprint 的产品并写入；此后所有 link 必须同产品；productId 一旦非空即不可变（仿 requirementId 不可变）。
- **C5.** **反查端点**：`GET /api/sprints/{id}/features`（SprintController）、`GET /api/features/{id}/sprints`（FeatureController）、`GET /api/requirements/{id}/features`（RequirementController，2 跳汇总：需求→其 sprints→features，去重）。
- **C6.** **解绑**：`DELETE /api/sprint-features/{id}` 硬删 link（解绑最后一个 feature 后，sprint.productId 保持不变——已建立即锁定）。
- **C7.** **前端**：Sprint 详情/抽屉加「关联功能」面板（选 feature 挂载 + 已挂列表 + 解绑）；Feature 详情/列表显示「所在迭代」。挂 feature 的下拉按 sprint.productId 过滤（产品确定后只显示该产品的 feature）。

### Q1 决策（Gate 1 已确认）：Sprint.productId = **nullable + 惰性锁定**

- productId 可空，创建时可选填；挂第一个 feature 时若仍空则由该 feature 反推写入，此后不可变。
- 存量 sprint 留 null 不动（满足 standing 约束「不改已有数据」）；新行为惰性强制；"产品迭代"语义在 sprint 真正挂功能时成立。
- 排除 NN 必填：docker MySQL 已有存量 sprint，ddl-auto=update 加 NN 列会失败，回填又违反 standing 约束且无自然产品归属。

## 显式排除（推到后续版本）

- Feature↔Requirement / Feature↔Demand 直连（本版只走 Sprint 桥）
- linkType 元数据（sprint-feature 是纯连接，无类型）
- Story↔Feature / Task↔Feature 关联
- 关联面板 attachment 字段（已决定 field-not-table 且暂缓）
- events / AI 抽取 / 自动关联
- 跨 sprint 批量 reparent feature

## Capabilities

### New Capabilities

- `entity-sprint-feature` —— Sprint↔Feature M:N 链接 capability（硬删 + 产品一致性校验 + 反查）

### Modified Capabilities

- `entity-sprint` —— 加 `productId`（nullable）+ 惰性锁定 + `/{id}/features` 反查端点
- `entity-feature` —— 加 `/{id}/sprints` 反查端点
- `entity-requirement` —— 加 `/{id}/features` 2 跳汇总端点
- `frontend-scaffold` —— Sprint「关联功能」面板 + Feature「所在迭代」显示

## Impact

**代码层面**：
- 新增 `com.rainier.sprintfeature.*`（domain/2 DTO/2 view/repo/service/controller ≈ 8 文件）+ 测试（create/query/delete/perf ≈ 4 类）。
- 改 Sprint（entity/DTO/Service/Controller + 反查 + productId 惰性逻辑 + 不可变校验）。
- 改 Feature / Requirement controller（反查端点）。
- 前端 `api/sprintFeature.ts` + Sprint/Feature 页关联面板。
- 约 **25~30 文件**。

**配置层面**：无新配置。

**基础设施**：16 → **17 张表**（+`rainier_sprint_feature`）；`rainier_sprint` 加 `product_id BIGINT NULL` 列（nullable，存量行不受影响）。

## Success Criteria

- [ ] `rainier_sprint` 新增 `product_id` 列（nullable）；存量 sprint 行 product_id 保持 null，无数据被改。
- [ ] `SHOW TABLES` 含 `rainier_sprint_feature`，共 17 张表。
- [ ] `POST /api/sprint-features {sprintId, featureId}` 首次挂载 → 201，且 sprint.productId 被反推设为该 feature 的产品。
- [ ] 第二个 feature 同产品 → 201；跨产品 feature → 400 `"feature must belong to the sprint's product"`。
- [ ] 重复 `(sprintId, featureId)` → 409。
- [ ] sprint 或 feature 不存在 → 400。
- [ ] `GET /api/sprints/{id}/features` 返回该 sprint 的功能列表（含 feature 富化字段）。
- [ ] `GET /api/features/{id}/sprints` 返回该 feature 所在的迭代列表。
- [ ] `GET /api/requirements/{id}/features` 返回 2 跳汇总（需求→sprints→features，去重）。
- [ ] `DELETE /api/sprint-features/{id}` → 204 硬删，行物理消失。
- [ ] productId 一旦建立，后续 PUT sprint 不能改它（不可变校验）。
- [ ] 前端 Sprint 详情有「关联功能」面板，挂 feature 的下拉按 sprint 产品过滤。
- [ ] 前端 Feature 详情显示「所在迭代」。
- [ ] 全量 backend + frontend 测试 green；E2E 全链（建 product→module→feature + requirement→sprint → 挂 feature 锁产品 → 跨产品拒绝 → 反查三端点 → 解绑）。
