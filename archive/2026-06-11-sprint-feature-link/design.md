# v0.0.14-sprint-feature-link — 技术设计

## Context

**Baseline**: tag `v0.0.13-product-restructure` / commit `952e320`。269 backend + 51 frontend tests green，16 张表。

**现状两域分离**：
```
project 域:   project → requirement → sprint → story → task   (+ demand —DemandRequirementLink— requirement)
product 域:   product → product_module(self-tree) → feature
```
Sprint 已挂 Requirement（`sprint.requirementId` NN）。Feature 经 `feature.moduleId → module.productId` 间接属一个 Product。两域之间无连接。

**先例资产**（本版直接照搬）：
- `com.rainier.demandrequirement.*` —— M:N 硬删链接 capability 的完整模板：`DemandRequirementLink`(BaseEntity, `@Table uniqueConstraints`, 硬删无 `@SQLDelete`) + `*CreateRequest` + `*Detail` + 2 个 View DTO(`SourceDemandView`/`DerivedRequirementView`) + Repository(`existsBy.../findBy.../countBy...`) + Service(create 校验 + `DataIntegrityViolationException`→409 兜底 + `findSourceDemands`/`findDerivedRequirements` 反查) + Controller `@RequestMapping("/api/demand-requirements")`。
- 反查端点挂**拥有方** controller：`RequirementController` 有 `@GetMapping("/{id}/source-demands")`，`DemandController` 有 `@GetMapping("/{id}/derived-requirements")`，均注入 `DemandRequirementLinkService` 委托。

**约束**：
- BaseEntity（Long id / 4 audit / del_flag）；Java 8（无 `Set.of`/`List.of`/无参 `orElseThrow()`）。
- ddl-auto=update（dev/docker MySQL）/ create-drop（H2 test）。docker MySQL 有存量 sprint 数据。
- **standing 约束：测试和修复不删改已有数据** —— 直接决定 productId 用 nullable。

---

## Decisions

### 1. Sprint.productId — nullable 列（不用 NN）

**方案**：`rainier_sprint` 加 `product_id BIGINT NULL`。

**为什么**：docker MySQL 有存量 sprint，ddl-auto=update 给已有行加 NN 列会失败；回填违反 standing 约束且存量 sprint 无自然产品归属。nullable 让存量行留 null、零改动。

**备选**：NN 必填 — 破坏存量数据，排除。

### 2. productId 惰性建立 —— 首个 feature 反推

**方案**：link 创建时，若 `sprint.productId == null`，解析待挂 feature 的产品（`feature.moduleId → module.productId`）并写入 `sprint.productId`（同一事务）。此后该 sprint 的 productId 非空。

**为什么**：满足"sprint = 产品迭代"语义而不强制创建时填；产品在 sprint 真正挂功能那刻自然确定。仿 v0.0.13 "首个确定、后续必须匹配" 模式。

**备选**：创建时必填 productId — 与 Decision 1 nullable 矛盾，且多数 sprint 建时还没想好挂哪些功能。

### 3. productId 创建时可选填 + 一旦非空即不可变

**方案**：`SprintCreateRequest` 加可选 `productId`（建时可预绑）。`SprintUpdateRequest` **不含** productId（Jackson 静默丢弃，仿 requirementId 不可变）。Service 内部仅 Decision 2 的惰性写入可设置它，且仅当当前为 null。

**为什么**：给"建时就知道产品"的用户预绑入口，同时锁死后续变更——productId 变了会让已挂 features 的产品一致性失效。

**备选**：可改 productId — 破坏已建立的 link 一致性，排除。

### 4. 产品一致性校验 —— feature→module→product 2 跳解析

**方案**：link 创建时，解析 feature 的产品：
```java
Feature f = featureRepo.findById(featureId).orElseThrow(400 "feature not found");
ProductModule m = moduleRepo.findById(f.getModuleId()).orElseThrow(...);
Long featureProductId = m.getProductId();
```
若 `sprint.productId != null && !sprint.productId.equals(featureProductId)` → 400 `"feature must belong to the sprint's product"`。若 `sprint.productId == null` → 走 Decision 2 写入。

**为什么**：feature 无直接 productId 字段（v0.0.13 决定经 module 间接），故 2 跳解析。SprintFeatureLinkService 注入 `FeatureRepository` + `ProductModuleRepository`。

### 5. SprintFeatureLink 链接表 —— 仿 DemandRequirementLink 硬删

**方案**：`entity-sprint-feature` capability，`SprintFeatureLink extends BaseEntity`，`@Table(name="rainier_sprint_feature", uniqueConstraints=@UniqueConstraint(name="uk_sprint_feature", columnNames={"sprint_id","feature_id"}))`，字段 `sprintId` + `featureId`，**无 `@SQLDelete`/`@Where`**（硬删，del_flag 列继承但不用）。

**为什么**：M:N 纯连接，硬删语义与 DemandRequirementLink 一致——解绑就是物理删行，不需要软删历史。

### 6. (sprintId, featureId) 唯一 + 并发兜底

**方案**：Service create 先 `existsBySprintIdAndFeatureId` → 409 `"link already exists"`；DB UQ 兜底，捕获 `DataIntegrityViolationException` 再转 409（仿 DemandRequirementLink 第 67-73 行）。

**为什么**：友好错误 + 并发竞态防护，照搬已验证模式。

### 7. 校验顺序固定：sprint 存在 → feature 存在 → 产品一致性 → 唯一性

**方案**：create 校验严格按此序，每步失败给最具体错误（404/400/400/409）。

**为什么**：早 short-circuit + 错误信息精确。

### 8. 三个反查端点挂拥有方 controller

**方案**：
- `GET /api/sprints/{id}/features` → `SprintController` 注入 `SprintFeatureLinkService.findFeaturesBySprint(sprintId)` → `List<SprintFeatureView>`（feature 富化：code/name/moduleId/moduleName/status）。
- `GET /api/features/{id}/sprints` → `FeatureController` → `findSprintsByFeature(featureId)` → `List<FeatureSprintView>`（sprint 富化：code/name/status/requirementId/productId）。
- `GET /api/requirements/{id}/features` → `RequirementController` → `findFeaturesByRequirement(requirementId)`（Decision 9）。

**为什么**：照搬 DemandRequirementLink 反查挂 Requirement/Demand controller 的先例，拥有方语义自然。

### 9. Requirement → features 2 跳汇总去重

**方案**：
```java
List<Sprint> sprints = sprintRepo.findByRequirementId(requirementId);   // 1 跳
Set<Long> featureIds = new LinkedHashSet<>();
for (Sprint s : sprints)
    for (SprintFeatureLink l : repo.findBySprintId(s.getId()))           // 2 跳
        featureIds.add(l.getFeatureId());
// 批量取 feature 富化，LinkedHashSet 保序去重
```

**为什么**：一个需求多个 sprint、不同 sprint 可能挂同一 feature，`LinkedHashSet` 去重保序。批量 `findAllById(featureIds)` 富化（≤ 3 SQL）。

### 10. Sprint enrich 加 productName

**方案**：`SprintDetail` 加 `productId` + `productName`（productId 非空时 join Product 取 name）。list 端点 batch enrich 增 product 维度（v0.0.10.1 batch 模式，预算仍 ≤ 5+1）。

**为什么**：前端 Sprint 列表/详情要显示所属产品。

### 11. 前端 feature 下拉按 sprint.productId 过滤

**方案**：Sprint「关联功能」面板挂 feature 时，下拉用 `listFeatures()` 后客户端按"feature 的产品 == sprint.productId"过滤；若 sprint.productId 仍 null（还没挂过），下拉显示全部 feature（首个挂载将确定产品）。feature 的产品前端经已挂 enrich 或 moduleId→module 解析——本版简化：复用后端反查/挂载校验，前端下拉先不严格过滤 null 情形，挂错由后端 400 兜底。

**为什么**：产品确定后只让选同产品 feature，UX 防错；null 情形交后端校验，避免前端做 2 跳解析。

### 12. 解绑硬删 —— productId 不回退

**方案**：`DELETE /api/sprint-features/{id}` 物理删 link。即使删光某 sprint 的所有 feature，`sprint.productId` 保持已建立的值不变。

**为什么**：productId 一旦锁定即稳定（Decision 3 不可变）；回退会让"再挂别的产品 feature"绕过约束。

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│ com.rainier.sprintfeature (NEW capability)                  │
│   domain/SprintFeatureLink (BaseEntity, uk_sprint_feature)  │
│   dto/SprintFeatureLinkCreateRequest {sprintId, featureId}  │
│   dto/SprintFeatureLinkDetail                               │
│   dto/SprintFeatureView   (feature 富化, 用于 sprint→features)│
│   dto/FeatureSprintView   (sprint 富化, 用于 feature→sprints) │
│   repository/SprintFeatureLinkRepository                    │
│     existsBySprintIdAndFeatureId / findBySprintId /         │
│     findByFeatureId / countBySprintId / countByFeatureId    │
│   service/SprintFeatureLinkService                          │
│     create  (Decision 2/4/6/7 — 注入 SprintRepo+FeatureRepo │
│              +ProductModuleRepo, 惰性写 sprint.productId)    │
│     delete  (硬删)                                          │
│     findFeaturesBySprint / findSprintsByFeature /           │
│     findFeaturesByRequirement (2 跳)                        │
│   controller/SprintFeatureLinkController                   │
│     @RequestMapping("/api/sprint-features")  POST / DELETE  │
└─────────────────────────────────────────────────────────────┘

注入反查到拥有方 controller：
  SprintController       + GET /{id}/features    → linkService.findFeaturesBySprint
  FeatureController      + GET /{id}/sprints     → linkService.findSprintsByFeature
  RequirementController  + GET /{id}/features    → linkService.findFeaturesByRequirement

Sprint (MODIFIED):
  domain +productId(nullable)  dto +productId/+productName
  Service: productId 惰性写入由 SprintFeatureLinkService 完成；SprintService.update 不碰 productId
           list/enrich +product join
```

### 数据流：挂第一个 feature（productId 惰性建立）

```
POST /api/sprint-features {sprintId=S, featureId=F}
  1. sprintRepo.findById(S)        → 不存在 400 "sprint not found"
  2. featureRepo.findById(F)       → 不存在 400 "feature not found"
  3. featureProductId = moduleRepo.findById(F.moduleId).productId
  4. if S.productId == null:  S.productId = featureProductId; save(S)   ← 惰性建立
     else if S.productId != featureProductId: 400 "feature must belong to the sprint's product"
  5. existsBySprintIdAndFeatureId(S,F) → 409 "link already exists"
  6. save link  (catch DataIntegrityViolation → 409)
  → 201 SprintFeatureLinkDetail
```

---

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| nullable productId 留下"半成品 sprint"(无产品) | 语义即如此——没挂功能的 sprint 本就无产品归属；反查/前端对 null 友好处理 |
| 惰性写 sprint.productId 与 link 创建同事务，失败回滚一致性 | create 方法 `@Transactional`，productId 写入与 link insert 同事务，要么都成要么都回 |
| feature→module→product 2 跳解析每次 link 2 SELECT | link 创建低频；反查/list 走 batch enrich（Decision 9/10）≤ 预算 |
| productId 不可变 → 用户挂错产品 feature 后想换产品 | 需先解绑所有 feature 再…（productId 仍不回退）——本版接受；跨产品换迭代属重建场景，文档 §排除 |
| docker MySQL 加 product_id 列 | nullable 列 ddl-auto=update 安全；E2E 验证存量 sprint product_id = null 未被改 |
| Requirement→features 2 跳在需求挂很多 sprint 时 N 次 findBySprintId | sprint 数通常 < 20；featureIds 收集后单次 batch 富化；perf 测试覆盖范围断言 |
| 反查端点注入 link service 到 3 个 controller 造成循环依赖 | link service 只依赖 repo 不依赖 SprintService 等，无环（同 DemandRequirementLink 先例） |
