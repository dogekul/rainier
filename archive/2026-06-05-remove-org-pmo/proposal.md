# 删除 Organization 的 isPmo 字段

## Why

v1（archive/2026-06-04-org-tree-and-employee）把 PMO 建模为"组织节点的布尔属性"（`Organization.is_pmo`）。这是领域错位：

**PMO 是"人在某个组织内承担的岗位/角色"，应由"员工 × 岗位"维度判断，不是组织本体属性。**

下一个变更（2026-06-05-demand-requirement 恢复后）将引入岗位/角色实体，到那时 PMO 看板等需求都会按"有 PMO 岗位的人"过滤而非按"isPmo=true 的组织"过滤。

先把这条错位字段清掉，避免在错误数据模型上继续叠加 UI + 业务逻辑。

本变更是**纯字段移除**，不引入岗位/角色实体（那是 demand-requirement 的事），只是为它扫清地基。

## What Changes

1. Drop SQL 列 `rainier_organization.is_pmo`
2. 移除 Java 字段：`Organization.isPmo`（+ `@Column` + getter/setter）；`OrganizationCreateRequest.isPmo`（+ getter/setter）；`OrganizationUpdateRequest.isPmo`（+ getter/setter）；`OrganizationDetail.isPmo`（+ `initFromEntity` 赋值 + getter）
3. 移除 Service 赋值：`OrganizationService.create()` 第 74 行 `setIsPmo`；`update()` 第 157-158 行 `if + setIsPmo`（共 3 处赋值，原描述"4 处"是估算偏多）
4. 移除测试断言：`OrganizationControllerCreateTest` 第 67 行 `jsonPath("$.isPmo")`
5. 移除前端 TS 类型：`api/organization.ts` 中 `Organization.isPmo` / `OrganizationCreate.isPmo` / `OrganizationUpdate.isPmo`
6. 移除前端 UI：`OrganizationsPage.tsx` 列表的 "PMO" 列；`EditDrawer.tsx` 的 PMO 复选框 + `isPmo` state + submit body 中的 `isPmo`
7. 修订主规范 `specs/entity-organization/spec.md`：删除 3 处提到 isPmo 的 Scenario AND 子句（创建/列表/编辑场景的 body 字段断言）
8. 历史档 `backend/src/main/resources/db/migration/V1__init_org.sql` 不动（Flyway 已禁用，文件是 v1 历史档，无运行时效果）

## Capabilities

### Modified Capabilities

- `entity-organization`：移除 `isPmo` 字段 + 相关 Scenario（创建响应 body、列表响应 body、编辑允许字段集均不再含 `isPmo`）
- `frontend-scaffold`：org 编辑抽屉删除 PMO 复选框 + 列表删除 PMO 列

### New Capabilities

（无）

## Impact

**代码层面（10 个文件）**：

- 后端：
  - `backend/src/main/java/com/rainier/organization/domain/Organization.java`
  - `backend/src/main/java/com/rainier/organization/dto/OrganizationCreateRequest.java`
  - `backend/src/main/java/com/rainier/organization/dto/OrganizationUpdateRequest.java`
  - `backend/src/main/java/com/rainier/organization/dto/OrganizationDetail.java`
  - `backend/src/main/java/com/rainier/organization/service/OrganizationService.java`
- 后端测试：
  - `backend/src/test/java/com/rainier/organization/controller/OrganizationControllerCreateTest.java`
- 前端：
  - `frontend/src/api/organization.ts`
  - `frontend/src/pages/Organization/OrganizationsPage.tsx`
  - `frontend/src/pages/Organization/EditDrawer.tsx`
- 规范：
  - `specs/entity-organization/spec.md`

**配置层面**：

- 无（Flyway 已禁用；Hibernate `ddl-auto=update` 不会主动 drop 列）

**基础设施层面**：

- 需要选择数据清理策略（**留待 Gate 2 design 阶段定**）：
  - 方案 A：`docker compose down -v` 清卷重生（沿用 id-migration 路线）— 简单清洁，符合"v0 可丢数据"前提，但已建组织/用户数据全清
  - 方案 B：一次性 `ALTER TABLE rainier_organization DROP COLUMN is_pmo`（通过 `docker exec rainier-mysql ...`）— 保数据但需手动 SQL
  - 方案 C：临时启用 `data.sql` 写 ALTER 语句 — 污染启动流程，不推荐

## Success Criteria

- [ ] `grep -rn 'isPmo\|is_pmo' backend/src/main/java backend/src/main/resources/application*.yml frontend/src specs/entity-organization` 在 source path 内返回 0 行命中（V1__init_org.sql 中的历史档行排除）
- [ ] `mvn test` 全绿（≥ 61 个测试，含原 `CreateTest` 删 `isPmo` 断言后的版本）
- [ ] `npm test` 全绿（≥ 11 个测试）
- [ ] `tsc -b` + ESLint + Spotless + Checkstyle 0 错误
- [ ] `DESCRIBE rainier_organization` 不含 `is_pmo` 列
- [ ] 浏览器编辑组织抽屉：PMO 复选框消失（只剩 父节点 / 类型 / 编码 / 名称 / 描述 / 启用）
- [ ] 浏览器组织列表：表头无 "PMO" 列
- [ ] `curl POST /api/organizations` 请求 body 即使带 `isPmo` 也不报错（Jackson 默认忽略未知字段），响应 body 也不含 `isPmo`
- [ ] `curl GET /api/organizations/{id}` 响应 body 无 `isPmo` 字段
- [ ] v0.0.4 基线保持：组织树 CRUD（含父节点变更）+ 用户 CRUD + 人员-组织归属（含 isPrimary demote）全部仍可用
- [ ] `specs/entity-organization/spec.md` 中 3 处含 `isPmo` 的 AND 子句已删除，主规范不再要求 body 含 `isPmo`
