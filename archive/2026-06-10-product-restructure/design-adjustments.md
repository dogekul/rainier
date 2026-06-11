# v0.0.13-product-restructure — 设计调整说明（Phase 5 汇总）

> 汇总 Phase 4 pending-adjustments.md（PA-1..PA-5）+ Phase 5 Step 0 三路评审发现与修复。

## A. Phase 4 BUILD 期间的偏离（已记录于 pending-adjustments.md）

| # | 类型 | 内容 | 处置 |
|---|------|------|------|
| PA-1 | 包位置 | LegacyCleanup 放 `com.rainier.product.bootstrap`（domain-following）而非 design 草图的 change-named 包 | design.md Decision 1 + Architecture 图已在 Phase 5 更正 |
| PA-2 | test-only | UTF-8 charset fix `getContentAsString(UTF_8)` | 无设计影响 |
| PA-3 | 计数 | 测试数 269/51 vs 预估 271/53 — 计数法差异，64 P0 TC 全落地 | test-report 第五节对照 |
| PA-4 | 亮点 | LegacyCleanup 真实 MySQL 双分支验证（drop + idempotent） | 非偏离，记录 |
| PA-5 | UI 实现 | ProductModulesPage 完全替换 Table 为嵌套 UL/LI 树 | design.md Decision 11 标题已更正 |

## B. Phase 5 Step 0 评审发现与修复（Round 0）

初始汇总：**C:0 / H:3 / M:4 / L:7**（代码 0/0/1/4 · 测试 0/1/2/1 · 文档 0/2/1/2）

### 已修复（C/H 全部 + 低成本 M）

| # | 来源 | 发现 | 修复 |
|---|------|------|------|
| H-1 | 测试评审 | application-test.yml 缺显式 depth.max（依赖 @Value 默认） | 显式加 `rainier.product-module.depth.max: 3` + 同步注释 |
| H-2 | 文档评审 | design.md Decision 1 + Architecture 图仍写 `com.rainier.productrestructure.bootstrap` | 双处更正为 `com.rainier.product.bootstrap` |
| H-3 | 文档评审 | spec Scenario 标题「reparent 到自己的子孙 → 400 cycle」与正文 THEN 200 矛盾（实为祖先合法 case） | 改名「reparent 到自己的祖先 → 200 合法（非 cycle）」 |
| M-2 | 测试评审 | FeatureEditDrawer.test.tsx Product mock 残留废弃 categoryId 字段 | 已删除 2 处 |
| M-4 | 文档评审 | design.md Decision 11 标题未反映「UL 完全替换 Table」 | 标题补注 + 引用 PA-5 |
| L-a | 文档评审 | .stdd.yaml `phase.current: spec` 过期 | 改 `verify` |

### 记录不修（M/L 级，附理由）

| # | 来源 | 发现 | 理由 |
|---|------|------|------|
| M-1 | 代码评审 | reparent 到 null（顶层）时跳过 depth 校验 | 逻辑正确（任何能存在的子树挂顶层必不超限）；维护性注释已有 |
| M-3 | 测试评审 | paddingLeft 像素断言较脆 | 缩进是 spec 行为（8+24×depth 确定性公式），断言即规格 |
| L-1..7 | 三路 | buildPath 环防御静默截断 / batchAncestors 轮次可再省 / pathName fallback 视觉差 / ancestor-reparent 专项测试缺（隐式覆盖）/ DDL-in-@Transactional 语义 / 其余文档措辞 | 全部低危且不影响行为；ancestor-reparent 专项测试列入 v0.0.14 候选 |

### Round 1 复核

修复后复跑：backend 269/269 ✓ / frontend 51/51 ✓ / tsc ✓ / eslint 0 errors（1 个 v0.0.8 遗留 warning 在 RequirementEditDrawer.test.tsx，不在本 diff，不修）。
聚合降至 **C:0 / H:0 / M:2 / L:7**（均为记录项）→ 阈值内，评审通过。

## C. spec/test-plan 修订项（Phase 6 合并 canonical 时生效）

1. `specs/entity-product-module/spec.md`：Scenario 改名（见 H-3）— 已在 change-local spec 修正，Phase 6 合并以修正版为准。
2. `design.md`：Decision 1 / Decision 11 / Architecture 图 — 已修正（归档版即正确版）。
3. test-plan TC-FES-FEAT-001 的「关键断言」示例文案与实现 mock 数据有措辞差（"MOD-WALLET / 钱包" vs 实际 "钱包"）— 行为断言一致，文案不改（test-plan 为 Phase 2 锁定文档）。

## D. Phase 6 DELIVER TODO checklist

- [ ] Archive `changes/2026-06-10-product-restructure/` → `archive/`
- [ ] **DELETE** canonical `specs/entity-product-category/`（整目录）
- [ ] Merge canonical `specs/entity-product/spec.md`（去 categoryId 版本替换 + 保留变更注记）
- [ ] Merge canonical `specs/entity-product-module/spec.md`（parentId 树版本替换）
- [ ] Merge canonical `specs/frontend-scaffold/spec.md`：替换 v0.0.12 的两条「产品 4 项 / 4 路由」Requirement 为 v0.0.13 的「3 项 / 3 路由」+ 新增树形/级联/pathName 3 条 Requirement + changelog
- [ ] Commit + tag `v0.0.13-product-restructure`
- [ ] No push（待用户指令）
