# Pending Adjustments — v0.0.16-project-type

记录 Phase 3-5 期间相对 Phase 2 设计的偏离与评审修复。

## PA-1 (Build) — 既有 detail 字段集测试为 presence-loop（非 exact-equality），加 projectType 无破坏

design.md Risk 表预估「既有 detail 字段集 exact-equality 测试」需同步。实测 `ProjectControllerQueryTest.get_existingId_returnsFullDetailAndEnriched`（TC-PRJ-007）是 `for(f: expected) assertTrue(body.has(f))` 的**存在性循环**，非 exact-equality（不断言「无多余字段」）。故加 `projectType` 到 `ProjectDetail` **不会**使其变红;仍主动把 `"projectType"` 追加进 expected[] 数组（诚实覆盖）。风险等级实际为 🟢 而非预估的 🟡。

## PA-2 (Build) — 前端类型过滤需专用 refetch effect（usePaginated 不监听 fetcher 身份）

`usePaginated` 的 `useEffect` 仅依赖 `[page,size,search]`（刻意，防闭包无限循环），不监听 fetcher 身份。故 typeFilter 改变不会自动重查。实现加 ref-guard 的 `useEffect(()=>{...list.refetch()},[typeFilter])`（首挂载跳过，避免双取）。TC-FES-PROJTYPE-003 验证过滤改变触发带 projectType 的查询。

## PA-3 (Verify Step 0 Code-M1) — update() projectType set 提前于 owner 校验 → 重排为「校验早/写入晚」

评审发现 `update()` 原实现把 `p.setProjectType(...)` 放在 owner-existence 校验之前（虽 @Transactional 回滚保证无半写持久化，但结构上偏离「先校验后写入」）。修复：拆为早期仅校验 `projectType ∈ ALL`（与 status 校验同处），实际 `p.setProjectType` 下移到 setter 区（`p.setStatus` 之后），完全镜像 status 的「早校验/晚写入」。行为不变，27 项 project 测试 + checkstyle 重跑全绿。

## PA-4 (Verify Step 0 Test-L1) — TC-PROJTYPE-008 去冗余断言

`get_detail_includesProjectType` 原同时 `jsonPath("$.projectType").value("FORMAL")` + `assertTrue(body.has("projectType"))`，后者冗余（前者通过即蕴含存在）。去掉后半 + 删除随之 unused 的 `JsonNode` import（避 checkstyle UnusedImports）。

## PA-5 (Verify Step 0 Docs-M1) — test-plan 执行矩阵 TC-ID 用规范全名

§三 执行矩阵原用缩写 `TC-001`/`TC-FES-001`，与 §二 规范 ID `TC-PROJTYPE-001`/`TC-FES-PROJTYPE-001` 不一致。已改矩阵为全名，E2E 列填 `TC-E2E-PROJTYPE-001`。

## 未修复（评审 M/L，阈值内，记录不阻塞）

- Code-L: `list()` 的 `projectType` 过滤不校验枚举合法性（非法值静默匹配 0 行）——与既有 `status`/`enabled` 过滤行为一致（均不校验），为有意的全局模式一致性，非回归。不改。
- Test-L3: 无「类型过滤清回全部」的前端负路径测试（`projectType: undefined` 分支）——test-plan 未要求，覆盖观察项，价值低。不补。
- Spec-L2: entity-project 过滤 scenario 的 `AND body.content 全部 projectType="FORMAL"` 无 SHALL ——逐字镜像既有 status 过滤 scenario 的同款 house style，非回归。不改。
