# 设计调整说明 — v0.0.16-project-type

> 原始设计基线：Phase 2 design.md + specs/*.md + test-plan.md
> 调整来源：Phase 3-5 实现与评审

## 调整汇总

| # | 调整类型 | 涉及文档 | 严重程度 | 调整阶段 | 用户已知 |
|---|---------|---------|---------|---------|---------|
| 1 | 实现结构（update 校验/写入顺序） | design.md Decision 4 | Minor | Phase 5 (Review Code-M1) | 是（本报告） |
| 2 | 风险等级修正（字段集测试） | design.md Risks | Minor | Phase 4 | 是 |
| 3 | 实现细节补充（前端过滤 refetch） | design.md Decision 8 | Minor | Phase 4 | 是 |

## 调整详细说明

### 调整 1: update() projectType 改为「校验早 / 写入晚」

- **原始设计**: design.md Decision 4 描述 update 语义为「present→校验+set / absent→保留」，未规定校验与写入的代码顺序。首版实现把「校验+set」合并在一个早期块，导致 `p.setProjectType` 发生在 owner-existence 校验之前。
- **调整内容**: 拆分为——早期仅做 `projectType ∈ ALL` 合法性校验（与 status 校验同处）；实际 `p.setProjectType` 下移到 setter 区（`p.setStatus` 之后），完全镜像既有 status 的「早校验/晚写入」。
- **调整原因**: 评审 Code-M1。原结构虽因 `@Transactional` 回滚不会持久化半写，但偏离「先全部校验、再全部写入」的结构惯例（与 status 不一致）。
- **影响范围**: `ProjectService.update`（仅顺序，行为不变）;无 spec/TC 改动;TC-PROJTYPE-005（保留）/006（非法）重跑仍绿。
- **调整阶段**: Phase 5 / **用户已知**: 是

### 调整 2: 既有 detail 字段集测试风险从 🟡 降为 🟢

- **原始设计**: design.md Risks 预估「既有『GET 详情返完整字段』测试为 exact-equality，加 projectType 须同步改测试（🟡 中）」。
- **调整内容**: 实测该测试（TC-PRJ-007）是 presence-loop（`assertTrue(body.has(f))`），非 exact-equality;加字段不会破断言。仍主动追加 `"projectType"` 到 expected[]（诚实覆盖）。
- **调整原因**: Phase 4 实读测试源发现。
- **影响范围**: `ProjectControllerQueryTest`（追加一行）;风险等级 🟡→🟢。
- **调整阶段**: Phase 4 / **用户已知**: 是

### 调整 3: 前端类型过滤需专用 refetch effect

- **原始设计**: design.md Decision 8 述「类型过滤下拉扩展 fetcher」。
- **调整内容**: `usePaginated` 的 effect 仅依赖 `[page,size,search]`（刻意，防闭包循环），不监听 fetcher 身份 → typeFilter 改变不自动重查。补 ref-guard 的 `useEffect(...[typeFilter])` 调 `list.refetch()`（首挂载跳过防双取）。
- **调整原因**: Phase 4 实现发现 hook 行为约束。
- **影响范围**: `ProjectsPage.tsx`;TC-FES-PROJTYPE-003 验证。
- **调整阶段**: Phase 4 / **用户已知**: 是
