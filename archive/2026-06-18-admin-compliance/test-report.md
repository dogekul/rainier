# v0.0.41-admin-compliance — 测试报告 (Phase 5 VERIFY)

> Baseline: tag `v0.0.40-me-profile` / commit 75b4fa1。路线图 #10 审计聚合 + 合规仪表盘。

## 1. 总体概况

| 维度 | 结果 |
|------|------|
| 后端单元/集成 | **453 / 453** ✅（442 baseline + 11 new；0 fail / 0 error / 0 skip） |
| 前端组件/路由 | **171 / 171** ✅（167 baseline + 4 new；44 files）+ tsc clean + eslint 0 warn |
| 新增后端测试 | ComplianceControllerTest **5/5**（功能）、ComplianceAuthzTest **6/6**（门控，含残留 401） |
| 新增前端测试 | CompliancePage **2/2** + AppRoutes /sys/compliance **1** + navGuardConsistency 自动 +1 |
| E2E（Docker 真 MySQL，admin-authz on） | 4/4 ✅ |
| 多路评审 (Step 0) | 3 reviewers / 7 findings / **C:0 H:0** / confirmed-real **0** |

## 2. 新增测试

**功能（ComplianceControllerTest，admin-authz off）**：audit-summary 总量 + byAction 最频在前 + recent / byEntityType 聚合 / 空表零值；residual 仅停用且有角色（seed alice 启用有角色 + empty 停用无角色 双负样本，断言仅 ghost）/ 无残留空数组。

**门控（ComplianceAuthzTest，admin-authz on）**：audit-summary 无 token→401 / 非 admin→403 / admin→200；residual 无 token→401 / 非 admin→403 / admin→200（双端点完整 401/403/200 矩阵，elevation 真实校验）。

**前端（CompliancePage.test + AppRoutes）**：渲染总量+残留表+按动作+最近 / 无残留空态 / /sys/compliance 路由挂载 + literal。navGuardConsistency 自动断言 `isAdminPath('/sys/compliance')===true`（系统 admin 组）。

## 3. E2E（live stack — Docker，真 MySQL，admin-authz on）

| # | 验证 | 结果 |
|---|------|------|
| 1 | admin(alice) `GET /api/compliance/audit-summary` | 200；total 98、byAction `[(CREATE,85),(UPDATE,9),(DELETE,4)]` 最频在前、byEntityType top `(TASK,26)`、recent 20 ✅ |
| 2 | admin `GET /api/compliance/residual-permissions` | 200；count 0（seed 无停用用户 → 有效空集；停用-有角色路径由单测 TC-COMP-004 覆盖）✅ |
| 3 | 非 admin(lili) 双端点 | 403 / 403 ✅ |
| 4 | 无 token | 401 ✅ |

> 端点纯只读 → 存量业务数据零改（standing 约束）；E2E 仅登录+读取，未写任何行。

## 4. 多路并行技术评审（Step 0）+ 11 类失败模式

**3 reviewers（code / test-config / docs-spec）**：7 findings，**C:0 H:0**，对抗式 verify 后 **confirmed-real = 0**。

核心 confirmations：
- **GROUP BY 正确**：`countGroupedByAction/EntityType` JPQL `GROUP BY x ORDER BY COUNT(a) DESC`（最频在前），单测 + E2E 双证；`Object[]→LabelCount` 映射 null-safe + `((Number)row[1]).longValue()`。
- **软删一致**：`findByEnabledFalse` 经 User `@Where(del_flag=0)` 自动排除软删用户。
- **残留逻辑真测**：seed 双负样本（启用有角色 + 停用无角色），断言仅停用-有角色返回。
- **AdminPaths 无 sibling 误伤**：`/api/compliance` base-or-prefix 匹配，仅两子路径，无 `/api/compliance-x`。
- **门控真实**：admin/非 admin 由 adminAccess true/false 角色区分，403 vs 200 走真实 elevation。
- **Java-8 clean**、**安全表述未夸大**（残留授权 inert：停用用户永不提升 + 不能登录 → 卫生缺口非活跃越权，与 ElevationService 一致）、**docs 与代码逐条吻合**。

**评审 LOW 项已闭合**：residual-permissions 缺 no-token→401 显式测试 → 已补 TC-COMP-AUTHZ-006（ComplianceAuthzTest 5→6）。

**11 类失败模式**：无幻觉；范围聚焦（admin-compliance + frontend-scaffold）；契约 (k) — 前后端 DTO 字段逐一对齐 + recent 复用 AuditLog 类型；(d) 上下文一致 design D1–D4 与代码吻合；无覆盖真空 (j)。

## 5. 已知取舍（记录，不阻塞）

- **E2E residual 为空集**：seed 无停用用户；不在 E2E 中停用真实用户（违背 standing 约束），停用-有角色路径由单测 TC-COMP-004（ghost→DEV）覆盖。
- **残留授权 inert**：停用用户永不提升 + 不能登录，残留授权是 de-provisioning 卫生缺口（建议回收），非活跃越权 —— 页面/DTO 如实表述。
- **C2 范围**：仅停用-有角色；软删用户残留（已消失）不纳入。

## 6. 结论

| 信号 | 状态 |
|------|------|
| 后端 453/453 + 前端 171/171 + tsc/lint | ✅ |
| 新增 11 后端 + 4 前端测试全绿 | ✅ |
| E2E 审计聚合 + 残留对账 + 门控（admin/非admin/无token） | ✅ |
| Docker 真 JDK-8 构建 | ✅ |
| 多路评审 C:0 H:0 confirmed-real:0 | ✅ |
| 存量业务数据零改（纯读） | ✅ |
| 管理员角色钩子（合规仪表盘）补齐 + 安全 track 闭合 | ✅ |

**部署建议**：可交付。后续候选：残留权限一键回收动作、审计 actor/时间趋势、停用用户自动 de-provision 流程。
