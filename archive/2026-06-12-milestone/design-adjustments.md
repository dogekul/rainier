# 设计调整说明 — v0.0.17-milestone

> 原始设计基线：Phase 2 design.md + specs/*.md + test-plan.md
> 调整来源：Phase 3-5 实现与评审

## 调整汇总

| # | 调整类型 | 涉及文档 | 严重程度 | 调整阶段 | 用户已知 |
|---|---------|---------|---------|---------|---------|
| 1 | spec/proposal 表述修正（code 必填） | spec.md / proposal.md | Minor | Phase 5 (Review Docs-M) | 是（本报告） |
| 2 | 实现不变量文档化（sortOrder） | design.md Decision 1 | Minor | Phase 5 (Review Code-M1) | 是 |
| 3 | 测试补强（缺 code / 项目已删） | test-plan.md | Minor | Phase 5 | 是 |

## 调整详细说明

### 调整 1: code 由「默认值」修正为「必填」

- **原始设计**: entity-milestone「创建里程碑」Requirement 与 proposal Success Criteria 把 `code` 归入「其余用默认值」。
- **调整内容**: `MilestoneCreateRequest.code` 实为 `@NotBlank`（必填，无默认）。修正 Requirement/proposal 必填字段为 `projectId + code + name + targetDate`；新增「缺 code 被拒」scenario + TC-MILE-014。
- **调整原因**: 评审 Docs-M——文档表述与代码契约不符，且 `@NotBlank` 约束无测试覆盖。
- **影响范围**: entity-milestone spec / proposal / test-plan / MilestoneControllerTest（+1 测试）;scenarios 19→20, tc 20→21。
- **调整阶段**: Phase 5 / **用户已知**: 是

### 调整 2: sortOrder 非空不变量文档化

- **原始设计**: design.md Decision 1 述 sortOrder 默认 0;Decision 4/5 述 create 默认 0、list 按 sortOrder 排序。
- **调整内容**: sort_order 列 nullable 但 API 路径恒非空（create 强制 0、update 省略保留）。实体字段加默认 `= 0`，update 加注释明确「keep-current（不清空 null）」。不改列约束（无 null 行、ddl-auto=update 不回填 NOT NULL）。
- **调整原因**: 评审 Code-M1——列可空与非空不变量的潜在不一致（latent，无 API 路径产生 null）。
- **影响范围**: Milestone.java（字段默认）/ MilestoneService.update（注释）。行为不变。
- **调整阶段**: Phase 5 / **用户已知**: 是

### 调整 3: 测试补强

- **原始设计**: TC-MILE-CAS-001 断言里程碑级联软删。
- **调整内容**: CAS-001 补 `GET /api/projects/{id} → 404`（防「级联跑了但项目漏删」假绿）;新增 TC-MILE-014（缺 code→400）。
- **调整原因**: 评审加固建议。
- **影响范围**: ProjectMilestoneCascadeTest / MilestoneControllerTest。
- **调整阶段**: Phase 5 / **用户已知**: 是
