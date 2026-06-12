# Pending Adjustments — v0.0.17-milestone

记录 Phase 3-5 期间相对 Phase 2 设计的偏离与评审修复。

## PA-1 (Build) — 前端面板挂载用 Table isExpanded/renderExpanded

design.md Decision 8 述「照搬 SprintFeaturePanel 内联模式」。实现确认 `Table` 组件已有 `isExpanded(row)`+`renderExpanded(row)` 行展开能力（v0.0.9 起，RequirementsPage/SprintsPage 用）。ProjectsPage 用 `milestonesExpanded: Set<number>` + 行「里程碑」按钮 toggle + Table 的 isExpanded/renderExpanded 挂 `<MilestonesPanel projectId={r.id}/>`。与 SprintsPage 完全同款。

## PA-2 (Build) — 级联软删用 deleteAll(findByProjectId) 触发 @SQLDelete

ProjectService.delete 级联用 `milestoneRepo.deleteAll(milestoneRepo.findByProjectId(id))`。`deleteAll(Iterable)` 逐行 `delete(T)` → 触发 `@SQLDelete` 软删（非 `deleteAllInBatch` 物理删）。findByProjectId 因 `@Where` 只返 active，避免重复处理已软删行。级联置于现有 requirement/userrole/task 的 409 检查之后、repo.delete(p) 之前。

## PA-3 (Build) — 跨版本表数测试 18→19 + rainier_milestone

`LegacyProductCategoryCleanupTest.schema_tableCount_*` 断言 18→19，加 `assertTrue(rows.contains("rainier_milestone"))`，方法名 `is16WithoutProductCategory` → `withoutProductCategory`（去掉过时数字）。跨版本维护项（v0.0.14 16→17 / v0.0.15 17→18 / v0.0.17 18→19）。

## PA-4 (Verify Step 0 Docs-M) — code 必填表述修正 + 补 缺 code→400 用例

评审发现 entity-milestone「创建里程碑」Requirement 与 proposal Success Criteria 把 `code` 列在「其余用默认值」，但 code 实为 `@NotBlank` 必填（无默认）。修正：Requirement/proposal 必填字段列为 `projectId + code + name + targetDate`；新增 spec scenario「缺 code 被拒」+ TC-MILE-014（缺 code→400，断言 fieldErrors 含 code），补上 `@NotBlank` 约束的覆盖缺口。scenarios 19→20 / tc 20→21。

## PA-5 (Verify Step 0 Code-M1) — sortOrder 不变量文档化 + 字段默认 0

评审指出 sort_order 列 nullable 但不变量是非空（create 强制 0、update 省略保留）。修复：实体字段加默认 `= 0`（防直接构造产生 null）+ update 加注释「sortOrder absent → keep current（不清空为 null）」。不改列约束（ddl-auto=update 不会回填 NOT NULL，且无 null 行存在），保持 sortOrder 作为「可选默认 0」语义一致。

## PA-6 (Verify Step 0 hardening) — CAS-001 加 项目已删 断言

TC-MILE-CAS-001 原只断言里程碑级联软删（total 0），未断言项目本身已删。补 `GET /api/projects/{id} → 404`，防「级联跑了但 repo.delete(p) 漏了」的假绿。

## 未修复（评审 L，阈值内，记录不阻塞）

- Code-L2: MilestoneRepository.countByProjectId 暂无调用方（前瞻保留，未来若加「项目有里程碑」提示可用）。不删。
- Code-L3: actualDate 与 status/targetDate 无交叉校验（PLANNED 也可带 actualDate）——D4 无状态机的有意宽松。不改。
- Test-L: TC-MILE-009 status 过滤未独立验 REACHED 可取（弱负路径）；TC-FES-MILE-003 未断言 create 后 refetch/reset。均为可选加固，价值低。不补。
