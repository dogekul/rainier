# v0.0.14-sprint-feature-link 测试方案与详细案例

> 版本：v0.0.14
> 创建日期：2026-06-11
> 对应 Phase 2 Spec：entity-sprint-feature(NEW) / entity-sprint(MOD) / entity-feature(MOD) / entity-requirement(MOD) / frontend-scaffold(MOD)
> Baseline：v0.0.13-product-restructure / commit 952e320 / 269 backend + 51 frontend tests

## 一、测试策略

### 1.1 测试金字塔

- **集成层**（主战场）：MockMvc + H2，覆盖 SprintFeatureLinkController（create/delete/list）+ 3 反查端点 + Sprint productId 惰性锁定。
- **单元层**（轻）：产品一致性解析（feature→module→product）+ 2 跳汇总去重逻辑。
- **性能层**：sprint list product enrich + requirement→features 2 跳 ≤ 预算。
- **前端**：Vitest + RTL，Sprint 关联功能面板挂载/解绑 + Feature 所在迭代。
- **E2E**：docker compose + curl，17 表 + 存量 sprint product_id 仍 null + 全链。

### 1.2 测试原则

- 硬删链接断言行物理消失（`SELECT COUNT(*)`=0）。
- productId 惰性锁定的"首个建立 / 后续匹配 / 跨产品拒绝 / 不回退"四态都要独立覆盖。
- 反查端点的"存在/空/404"三态覆盖。
- standing 约束：E2E 验证存量 sprint product_id 未被改。
- perf 用范围断言（≥N ∧ ≤M）防 statistics 假绿。

### 1.3 已有测试资产（可复用 seeding 模式）

| 来源 | 复用点 |
|---|---|
| DemandRequirementLink 测试 | 硬删链接 create/conflict/delete 断言模式 |
| v0.0.13 ProductModule 测试 | product→module→feature seeding 链 |
| Sprint 既有测试 | requirement→sprint seeding |

## 二、详细测试案例

### 功能 1 — SprintFeatureLink 创建（含产品一致性 + 惰性锁定）

#### 案例 1.1 — 首个 feature 挂载触发 productId 惰性建立

| 字段 | 内容 |
|------|------|
| **ID** | TC-SF-001 |
| **对应 Spec** | entity-sprint-feature → Scenario: 首个 feature 挂载触发 productId 惰性建立 |
| **优先级** | P0 |
| **预置条件** | Sprint S(productId=null) + Feature F(经 module 属 Product P) |
| **输入** | `POST /api/sprint-features {sprintId:S, featureId:F}` |
| **预期结果** | 201；body 含 sprintId/featureId/id/createTime；GET /api/sprints/S 的 productId == P |
| **当前状态** | ❌ |

#### 案例 1.2 — 第二个同产品 feature 挂载成功

| 字段 | 内容 |
|------|------|
| **ID** | TC-SF-002 |
| **对应 Spec** | entity-sprint-feature → Scenario: 第二个同产品 feature 挂载成功 |
| **优先级** | P0 |
| **预置条件** | Sprint S 已锁 Product P；Feature F2 属 P |
| **输入** | `POST /api/sprint-features {sprintId:S, featureId:F2}` |
| **预期结果** | 201 |
| **当前状态** | ❌ |

#### 案例 1.3 — 跨产品 feature 挂载被拒

| 字段 | 内容 |
|------|------|
| **ID** | TC-SF-003 |
| **对应 Spec** | entity-sprint-feature → Scenario: 跨产品 feature 挂载被拒 |
| **优先级** | P0 |
| **预置条件** | Sprint S 已锁 Product P；Feature G 属 Product Q |
| **输入** | `POST /api/sprint-features {sprintId:S, featureId:G}` |
| **预期结果** | 400；message 含 "feature must belong to the sprint's product" |
| **当前状态** | ❌ |

#### 案例 1.4 — 唯一性冲突 409

| 字段 | 内容 |
|------|------|
| **ID** | TC-SF-004 |
| **对应 Spec** | entity-sprint-feature → Scenario: 唯一性冲突 |
| **优先级** | P0 |
| **预置条件** | 已存在链接 (S, F) |
| **输入** | 再 POST 同 (S, F) |
| **预期结果** | 409；message 含 "link already exists" |
| **当前状态** | ❌ |

#### 案例 1.5 — sprintId 不存在 400

| 字段 | 内容 |
|------|------|
| **ID** | TC-SF-005 |
| **对应 Spec** | entity-sprint-feature → Scenario: sprintId 不存在被拒 |
| **优先级** | P0 |
| **输入** | POST sprintId=999999 |
| **预期结果** | 400；message 含 "sprint not found" |
| **当前状态** | ❌ |

#### 案例 1.6 — featureId 不存在 400

| 字段 | 内容 |
|------|------|
| **ID** | TC-SF-006 |
| **对应 Spec** | entity-sprint-feature → Scenario: featureId 不存在被拒 |
| **优先级** | P0 |
| **输入** | POST featureId=999999 |
| **预期结果** | 400；message 含 "feature not found" |
| **当前状态** | ❌ |

#### 案例 1.7 — 按 sprintId 过滤列表

| 字段 | 内容 |
|------|------|
| **ID** | TC-SF-007 |
| **对应 Spec** | entity-sprint-feature → Scenario: 按 sprintId 过滤 |
| **优先级** | P0 |
| **预置条件** | 3 个链接行，2 行 sprintId=S |
| **输入** | `GET /api/sprint-features?sprintId=S` |
| **预期结果** | total=2；content[*].sprintId 全为 S |
| **当前状态** | ❌ |

### 功能 2 — 解绑（硬删）

#### 案例 2.1 — 硬删成功行物理消失

| 字段 | 内容 |
|------|------|
| **ID** | TC-SF-008 |
| **对应 Spec** | entity-sprint-feature → Scenario: 硬删成功 |
| **优先级** | P0 |
| **预置条件** | 链接 id=L |
| **输入** | `DELETE /api/sprint-features/L` |
| **预期结果** | 204；SELECT COUNT(*) for id=L = 0 |
| **当前状态** | ❌ |

#### 案例 2.2 — 解绑最后一个 feature 后 productId 不回退

| 字段 | 内容 |
|------|------|
| **ID** | TC-SF-009 |
| **对应 Spec** | entity-sprint-feature → Scenario: 解绑最后一个 feature 后 sprint.productId 不变 |
| **优先级** | P0 |
| **预置条件** | Sprint S 仅挂 Feature F（productId 锁为 P） |
| **输入** | DELETE 该链接 |
| **预期结果** | 204；Sprint S 的 productId 仍为 P |
| **当前状态** | ❌ |

### 功能 3 — Sprint productId（惰性 + 不可变 + 富化）

#### 案例 3.1 — 创建 Sprint 不传 productId → null

| 字段 | 内容 |
|------|------|
| **ID** | TC-SPR-PF-001 |
| **对应 Spec** | entity-sprint → Scenario: 创建 Sprint 不传 productId → productId 为 null |
| **优先级** | P0 |
| **输入** | POST /api/sprints 含 requirementId/ownerUserId，不含 productId |
| **预期结果** | 201；body.productId == null |
| **当前状态** | ❌ |

#### 案例 3.2 — 创建 Sprint 预绑 productId

| 字段 | 内容 |
|------|------|
| **ID** | TC-SPR-PF-002 |
| **对应 Spec** | entity-sprint → Scenario: 创建 Sprint 预绑 productId |
| **优先级** | P0 |
| **预置条件** | Product P |
| **输入** | POST 含 productId=P |
| **预期结果** | 201；body.productId==P；body.productName==P.name |
| **当前状态** | ❌ |

#### 案例 3.3 — Update 携带 productId 被静默忽略

| 字段 | 内容 |
|------|------|
| **ID** | TC-SPR-PF-003 |
| **对应 Spec** | entity-sprint → Scenario: 更新 Sprint 携带 productId 被静默忽略 |
| **优先级** | P0 |
| **预置条件** | Sprint S 的 productId=P |
| **输入** | PUT /api/sprints/S body 含 productId=Q |
| **预期结果** | 200；Sprint S 的 productId 仍为 P |
| **当前状态** | ❌ |

#### 案例 3.4 — GET 详情含 productId + productName

| 字段 | 内容 |
|------|------|
| **ID** | TC-SPR-PF-004 |
| **对应 Spec** | entity-sprint → Scenario: GET 详情含 productId 与 productName |
| **优先级** | P0 |
| **预置条件** | Sprint S 的 productId=P |
| **输入** | GET /api/sprints/S |
| **预期结果** | 200；body 含 productId 与 productName==P.name |
| **当前状态** | ❌ |

### 功能 4 — 反查端点

#### 案例 4.1 — sprint→features 列表

| 字段 | 内容 |
|------|------|
| **ID** | TC-SF-REV-001 |
| **对应 Spec** | entity-sprint → Scenario: 列出 sprint 的功能 |
| **优先级** | P0 |
| **预置条件** | Sprint S 挂 F1、F2 |
| **输入** | `GET /api/sprints/S/features` |
| **预期结果** | 200；长度 2；每项含 featureId/code/name/moduleId |
| **当前状态** | ❌ |

#### 案例 4.2 — sprint→features sprint 不存在 404

| 字段 | 内容 |
|------|------|
| **ID** | TC-SF-REV-002 |
| **对应 Spec** | entity-sprint → Scenario: sprint 不存在 → 404 |
| **优先级** | P0 |
| **输入** | `GET /api/sprints/999999/features` |
| **预期结果** | 404；message 含 "sprint not found" |
| **当前状态** | ❌ |

#### 案例 4.3 — feature→sprints 列表

| 字段 | 内容 |
|------|------|
| **ID** | TC-SF-REV-003 |
| **对应 Spec** | entity-feature → Scenario: 列出 feature 所在的迭代 |
| **优先级** | P0 |
| **预置条件** | Feature F 挂到 S1、S2 |
| **输入** | `GET /api/features/F/sprints` |
| **预期结果** | 200；长度 2；每项含 sprintId/code/name/status |
| **当前状态** | ❌ |

#### 案例 4.4 — feature→sprints 空数组

| 字段 | 内容 |
|------|------|
| **ID** | TC-SF-REV-004 |
| **对应 Spec** | entity-feature → Scenario: feature 未挂任何 sprint → 空数组 |
| **优先级** | P0 |
| **输入** | `GET /api/features/F/sprints`（F 未挂） |
| **预期结果** | 200；空数组 |
| **当前状态** | ❌ |

#### 案例 4.5 — feature→sprints feature 不存在 404

| 字段 | 内容 |
|------|------|
| **ID** | TC-SF-REV-005 |
| **对应 Spec** | entity-feature → Scenario: feature 不存在 → 404 |
| **优先级** | P0 |
| **输入** | `GET /api/features/999999/sprints` |
| **预期结果** | 404；message 含 "feature not found" |
| **当前状态** | ❌ |

#### 案例 4.6 — requirement→features 2 跳汇总去重

| 字段 | 内容 |
|------|------|
| **ID** | TC-SF-REV-006 |
| **对应 Spec** | entity-requirement → Scenario: 跨多个 sprint 汇总去重 |
| **优先级** | P0 |
| **预置条件** | Requirement R 下 S1(挂 F1,F2)、S2(挂 F2,F3) |
| **输入** | `GET /api/requirements/R/features` |
| **预期结果** | 200；含且仅含 F1/F2/F3（F2 去重）；每项含 featureId/code/name |
| **当前状态** | ❌ |

#### 案例 4.7 — requirement→features 空数组

| 字段 | 内容 |
|------|------|
| **ID** | TC-SF-REV-007 |
| **对应 Spec** | entity-requirement → Scenario: 需求下的 sprint 均未挂功能 → 空数组 |
| **优先级** | P0 |
| **输入** | `GET /api/requirements/R/features`（R 下 sprint 无链接） |
| **预期结果** | 200；空数组 |
| **当前状态** | ❌ |

#### 案例 4.8 — requirement→features requirement 不存在 404

| 字段 | 内容 |
|------|------|
| **ID** | TC-SF-REV-008 |
| **对应 Spec** | entity-requirement → Scenario: requirement 不存在 → 404 |
| **优先级** | P0 |
| **输入** | `GET /api/requirements/999999/features` |
| **预期结果** | 404；message 含 "requirement not found" |
| **当前状态** | ❌ |

### 功能 5 — 性能

#### 案例 5.1 — Sprint list product enrich SQL 范围

| 字段 | 内容 |
|------|------|
| **ID** | TC-PERF-SPR-PF-001 |
| **对应 Spec** | design.md Decision 10 |
| **优先级** | P0 |
| **预置条件** | 20 sprint（含 product 富化） |
| **输入** | `GET /api/sprints?size=20` 用 Hibernate Statistics |
| **预期结果** | SQL 次数 ≥ 4 ∧ ≤ 7（2 page + user/req/project batch + storyCount + product batch；既有 baseline 锁 6，product enrich +1，见 PA-1） |
| **当前状态** | ❌ |

#### 案例 5.2 — requirement→features 2 跳 SQL 范围

| 字段 | 内容 |
|------|------|
| **ID** | TC-PERF-SF-REV-001 |
| **对应 Spec** | design.md Decision 9 |
| **优先级** | P0 |
| **预置条件** | Requirement R 下 5 sprint，共挂 10 feature |
| **输入** | `GET /api/requirements/R/features` 用 Statistics |
| **预期结果** | SQL 次数 ≥ 2 ∧ ≤ 8（sprints + 各 sprint links + feature batch 富化） |
| **当前状态** | ❌ |

### 功能 6 — 前端

#### 案例 6.1 — Sprint 关联功能面板：展示 + 挂载

| 字段 | 内容 |
|------|------|
| **ID** | TC-FES-SF-001 |
| **对应 Spec** | frontend-scaffold → Scenario: 展示已挂功能并支持挂载 |
| **优先级** | P0 |
| **预置条件** | mock sprint S 已挂 F1；mock listFeatures |
| **输入** | 展开面板 → 选 feature → 提交 |
| **预期结果** | 显示 F1；提交调用 POST /api/sprint-features；刷新 |
| **当前状态** | ❌ |

#### 案例 6.2 — Sprint 关联功能面板：解绑

| 字段 | 内容 |
|------|------|
| **ID** | TC-FES-SF-002 |
| **对应 Spec** | frontend-scaffold → Scenario: 解绑功能 |
| **优先级** | P0 |
| **预置条件** | mock sprint S 已挂 F1 |
| **输入** | 点 F1 行「解绑」 |
| **预期结果** | 调用 DELETE /api/sprint-features/{id}；刷新后 F1 消失 |
| **当前状态** | ❌ |

#### 案例 6.3 — Feature 所在迭代显示

| 字段 | 内容 |
|------|------|
| **ID** | TC-FES-SF-003 |
| **对应 Spec** | frontend-scaffold → Scenario: 查看 feature 的所在迭代 |
| **优先级** | P0 |
| **预置条件** | mock GET /api/features/F/sprints 返回 S1 |
| **输入** | 查看 F 的「所在迭代」 |
| **预期结果** | 调用反查端点；显示 S1 code+name |
| **当前状态** | ❌ |

### 功能 7 — E2E

#### 案例 7.1 — SHOW TABLES = 17 + 存量 sprint product_id 仍 null

| 字段 | 内容 |
|------|------|
| **ID** | TC-E2E-SF-001 |
| **对应 Spec** | proposal Success Criteria + standing 约束 |
| **优先级** | P0 |
| **输入** | docker compose 重建 + SHOW TABLES + 查存量 sprint product_id |
| **预期结果** | 17 表含 rainier_sprint_feature；存量 sprint 行 product_id IS NULL（未被改） |
| **当前状态** | ❌ |

#### 案例 7.2 — curl 全链

| 字段 | 内容 |
|------|------|
| **ID** | TC-E2E-SF-002 |
| **对应 Spec** | proposal Success Criteria |
| **优先级** | P0 |
| **输入** | curl: 建 product→module→feature + requirement→sprint → 挂 feature(锁产品) → 第二同产品 feature → 跨产品 feature 400 → 反查 3 端点 → 解绑 204 |
| **预期结果** | 全链按预期；productId 锁定；跨产品拒绝；3 反查正确；解绑物理删 |
| **当前状态** | ❌ |

## 三、测试执行矩阵

| 功能模块 | 单元 | 集成 | Perf | E2E | 状态 |
|----------|------|------|------|-----|------|
| SprintFeatureLink create/delete/list | — | TC-SF-001..009 (9) | — | TC-E2E-SF-002 | 🟢 |
| Sprint productId 惰性/不可变/富化 | — | TC-SPR-PF-001..004 (4) | TC-PERF-SPR-PF-001 (1) | TC-E2E-SF-001 | 🟢 |
| 反查 3 端点 | — | TC-SF-REV-001..008 (8) | TC-PERF-SF-REV-001 (1) | TC-E2E-SF-002 | 🟢 |
| 前端面板 | — | TC-FES-SF-001..003 (3) | — | manual | 🟢 |

**TC 总数**：9 + 4 + 8 + 2 + 3 + 2 = **28 P0**

## 四、回归风险矩阵

| 风险区域 | v0.0.14 改动 | 已有回归保护 | 风险等级 |
|----------|---------------|-------------|---------|
| Sprint 加 product_id nullable 列 | entity + DTO + Service + Controller | TC-SPR-PF-001..004 + 既有 Sprint 测试不破 + E2E 存量 null | 🟡 |
| productId 惰性锁定 4 态逻辑 | SprintFeatureLinkService.create | TC-SF-001/002/003/009 四态独立覆盖 | 🔴 |
| 新增 sprint_feature 表 + 硬删 | 新 capability 全栈 | TC-SF-001..009 + E2E SHOW TABLES | 🟢 |
| 3 反查端点注入 link service 到拥有方 controller | 改 Sprint/Feature/Requirement controller | TC-SF-REV-001..008；循环依赖风险低（link service 不依赖业务 service） | 🟡 |
| Sprint list product enrich 改 batch | SprintService.list | TC-PERF-SPR-PF-001 + 既有 Sprint 测试 | 🟡 |
| 既有 v0.0.10-13 sprint/feature 测试 | Sprint/Feature 改动 | 269 backend + 51 frontend 回归 | 🟢 |

**总评**：🔴 高: 1（productId 惰性锁定）/ 🟡 中: 4 / 🟢 低: 2

## 五、建议补充顺序

1. **第一优先**（部署前必补）：全部 28 P0（本版无 P1/P2）。
2. **第二优先**：无。
3. **第三优先**（后续版本）：
   - sprint-feature 并发挂载 race（DataIntegrityViolation 兜底已覆盖，未压测）
   - requirement→features 在超大 sprint 树下的分页（本版不分页，返全量）
