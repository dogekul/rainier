# 设计调整说明 — 诉求 + 需求 + M2M 关联

> 原始设计基线：Phase 2 修订版 design.md（10 决策）+ 5 个 spec + test-plan.md
> 调整来源：Phase 4 实现期 + Phase 5 多路并行 Review

## 调整汇总

| # | 调整类型 | 涉及文档/代码 | 严重程度 | 调整阶段 | 用户已知 |
|---|---|---|---|---|---|
| 1 | Java 8 兼容（Set.of → Collections.unmodifiableSet） | 6 常量类 | Minor | Phase 4 M01 编译期发现 | 是（实现期立即修） |
| 2 | Requirement.code 取消 DB unique 约束，仅 Service 层校验 | `Requirement.java` `@Column` | Minor | Phase 4 M03 测试期发现 | 是（与 Organization.code 模式对齐） |
| 3 | 主规范 path 修正 frontend/src/router/AppRoutes → frontend/src/AppRoutes | 4 处文档（spec/design/test-plan/tasks） | Minor | Phase 5 Step 0 Review 命中 | 是 |
| 4 | DemandRequirementLinkService.create 加 DataIntegrityViolationException 兜底 | `DemandRequirementLinkService.java` | Minor (race-safety) | Phase 5 Step 0 Review 命中 | 是 |
| 5 | RequirementService.create sourceDemandIds 去重（LinkedHashSet） | `RequirementService.java` | Minor (idempotency) | Phase 5 Step 0 Review 命中 | 是 |
| 6 | TC-DRL-004 断言强化（isArray → contains） | `DemandRequirementLinkControllerTest.java` | Minor | Phase 5 Step 0 Review 命中 | 是 |

无设计层或行为契约偏离 — 所有 spec Scenario 的可观察行为与原稿一致。

## 调整详细说明

### 调整 1：Java 8 兼容（M01 编译期）

- **原始**：`Set.of(...)` 即时常量集合
- **问题**：`Set.of` 是 Java 9+ API；项目 pom 锁定 Java 8
- **修复**：`Collections.unmodifiableSet(new HashSet<>(Arrays.asList(...)))`
- **触发**：Phase 4 M01 编译失败
- **影响**：6 文件，仅初始化语法变化；可观察行为完全一致
- **预防**：Phase 6 deliver 后给开发规范追加"Java 8 集合 API 白名单"提示

### 调整 2：Requirement.code 取消 DB unique 约束

- **原始**：design 决策 1 未明确说 DB 是否加 `unique = true`；初稿写了 `@Column(unique = true)`
- **问题**：soft-delete 残留（del_flag=1 行）会让"复用同 code"在 @Where 过滤前无感知 + DB 层硬挡 → 测试间数据污染
- **修复**：去掉 `unique = true`，与 v0.0.3 `Organization.code` 完全一致 — Service `existsByCode` 校验是唯一真相源
- **触发**：Phase 4 M03 测试 `post_duplicateCode_returns409` 首次失败
- **影响**：DB schema 略弱（无 DB-level 兜底），但 Service 校验完备；与 v0.0.3 Organization 一致 → 减少认知负担
- **trade-off 记录**：极端并发下两个相同 code 的 POST 可能同时通过 existsByCode 都插入成功（race），但 v0 阶段可接受

### 调整 3：主规范 path 修正

- **原始**：4 处文档引用 `frontend/src/router/AppRoutes.tsx`
- **问题**：项目实际用扁平结构，`frontend/src/AppRoutes.tsx`（无 `router/` 子目录）；spec 的 grep 校验会返 0 → Phase 5 自我验证失败
- **修复**：4 处文档全部更正为 `frontend/src/AppRoutes.tsx`；spec scenario AND 子句的实际执行 `grep -c "/pm/demands" frontend/src/AppRoutes.tsx` 返回 2 ≥ 1 ✓
- **触发**：Phase 5 Step 0 Docs + Test 双路 review 命中（H 级 1 票）
- **影响**：仅文档；不动代码
- **追溯**：这是 Phase 2 设计时对项目结构的认知偏差；今后 STDD 流程应在 design.md 写路径前先 `find` 一次

### 调整 4：DemandRequirementLinkService.create 加竞态兜底（H 级 review 命中）

- **原始**：`existsByDemandIdAndRequirementId` 检查 + `saveAndFlush` 顺序执行，无 TOCTOU 保护
- **问题**：并发 POST 同 `(demandId, requirementId)` 时第一次 check 都返 false，第二次 saveAndFlush 撞 DB 唯一约束 → 500 而非 409
- **修复**：包 try/catch `DataIntegrityViolationException` → throw `ConflictException("link already exists")`，与 `RequirementService.create` 处理 code 唯一性同模式
- **触发**：Phase 5 Step 0 Code Review H-1
- **影响**：可观察行为：并发场景下 409 取代 500，对客户端语义不变（已存在）
- **测试**：现有 TC-DRL-002（顺序 dup）仍绿；竞态测试 v0 不补，记入 known-limitations

### 调整 5：sourceDemandIds 去重（M 级 review 命中）

- **原始**：`for (Long demandId : sources)` 直接遍历 — 客户端传 `[d1, d1, d2]` 会在第二次 d1 撞 (demand_id, requirement_id) 唯一约束
- **修复**：`new LinkedHashSet<>(sources)` 保序去重
- **触发**：Phase 5 Step 0 Code Review M-2
- **影响**：客户端等价语义不变（同 demand 派生同 requirement 本就只该有 1 行 link）；防御性 + 幂等性提升
- **测试**：现有 TC-DRC-001 / 002 / 003 仍绿；v0 不补 dedup 专项

### 调整 6：TC-DRL-004 断言强化（H 级 review 命中）

- **原始**：`jsonPath("$.content[*].demandId").isArray()` —— 只检查字段是数组，不查内容
- **修复**：`contains(demandId1.intValue(), demandId1.intValue())` —— 严格断言所有行都是过滤值
- **触发**：Phase 5 Step 0 Test Review H-1
- **影响**：测试强度提升；现有过滤实现仍通过

## 不构成调整的 review 命中

### Known Limitations / 接受不修

| # | 来源 | 描述 |
|---|---|---|
| KL-1 | Code M-1 | 辅助查询 N+1 query — v0 数据量小（一个需求关联诉求数预计 < 10），未来加索引/缓存时再优化 |
| KL-2 | Code L-1 ~ L-5 | 文档注释陈旧、Repository.hardDeleteAll 未用、@Transactional rollbackFor 冗余、update 不可清空字段、前端 delete 错误不弹消息 — 全部接受 |
| KL-3 | Test M-1 | TC-DMD-009 PUT body silent-ignore 起始 aiClassification=null —— 测试强度可加强，但对外契约已被 TC-DRC-003 + curl POST E2E 间接覆盖 |
| KL-4 | Test L-1 ~ L-3 | DTO 层 Jackson drop 隐性、cleanDb 顺序冗余、vi.mock 未覆盖全 endpoint — 全部接受 |
| KL-5 | Docs M-1 | tasks.md 6.6.2 "13 + 3 = 16" 算术错（应为 19）— 已在 test-report 修正描述 |
| KL-6 | Docs M-2 | baseline 13 vs 实际 12 frontend 测试 — 沿用上版本 metadata 误差，不阻塞 |
| KL-7 | Docs L-1 ~ L-2 | capability count 12 vs prose 11、slice range 12-14 vs 实际 12 — 文案漂移，trivial |

## 结论

6 项 Minor 调整全部已就地修复；7 类 KL 接受不动。无任何设计层或行为契约偏离。Phase 4 build 完全按 Phase 2 spec 实施；3 entity + 1 workflow + 1 frontend MODIFIED 整条链路通过 94 backend + 19 frontend + 7 E2E 端到端验证。
