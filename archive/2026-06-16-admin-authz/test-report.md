# Test Report — v0.0.21-admin-authz

> Baseline: tag `v0.0.20-role-nav` / commit a15ef66 (backend 354 / frontend 106 / 19 表).

## 1. 总体概况

| 维度 | 总数 | 通过 | 失败 | 跳过 | 通过率 |
|---|---|---|---|---|---|
| Backend (mvn test) | 371 | 371 | 0 | 0 | 100% |
| checkstyle (mvn build) | — | ✅ BUILD SUCCESS | — | — | — |
| Frontend | 106 | 106 | 0 | 0 | 100%（无前端改动，回归保护）|
| E2E (docker, 真实 profile enabled=true) | — | ✅ green | — | — | — |

新增 backend +17（AdminAuthorizationTest 14 + AdminAuthzBootstrapTest 3）。门控关（test profile）下既有 354 测试零改动全绿。

## 2. 按模块统计
- `AdminAuthorizationTest` 14/14：无 token Tier A 读/写→401（001/002）、非管理员 Tier A 写/删→403（003/004）、
  管理员 Tier A 写放行 201（005）、非管理员 Tier B 读 users/features/modules→200（006/007）、Tier B 写 users→403（008）、
  全员端点不被误伤→200（009）、OPTIONS isOk（010）、无效 token→401（011）、sibling-prefix products 收口 vs
  product-modules 放行（012）、Tier B 写 features→403（013）、**matrix-param 不绕过→401（014, PA-1 回归）**。
- `AdminAuthzBootstrapTest` 3/3：无 admin 提升 PMO（001）、已有 admin 幂等 no-op（002）、无 PMO 安全 no-op（003）。

## 3. E2E 测试结果（docker，真实 MySQL，卷保留）
- **bootstrap**：重建后 `PMO.admin_access=1`，`YFM=NULL`（仅 PMO 被合法提升为首个管理员 seed）。✅
- 无 token `POST /api/roles`→**401**；`GET /api/audit-logs`→**401**。✅
- **matrix-param 绕过**：无 token `POST /api/roles;x=1`→**401**（真实 Tomcat 验证 C1 已堵）。✅
- alice(=PMO admin) `GET /api/audit-logs`→**200**（Tier A 读放行）。✅
- ghost(非管理员) `POST /api/roles`→**403**；`GET /api/audit-logs`→**403**；`GET /api/users`→**200**（Tier B 读放行）。✅
- 无 token `GET /api/projects`→**200**（全员端点不被误伤）。✅
- 存量行数 users=2/roles=2/projects=6/user_roles=2 与测前一致（所有写均被拒，无 stray 数据）；唯一变更 = PMO bootstrap seed。✅

## 4. 失败项详细分析
无失败项。

## 5. 功能/测试覆盖对照
| Spec Scenario | 实现 | 测试 |
|---|---|---|
| Tier A 全方法 401/403/200 | AdminAuthorizationInterceptor + AdminPaths.TIER_A | TC-AUTHZ-001..005/011/012 + E2E |
| Tier B 写收口/读放行 | AdminPaths.isMutating + TIER_B | TC-AUTHZ-006/007/008/012/013 + E2E |
| 全员端点不被误伤 / OPTIONS | requiresAdmin=false / OPTIONS 放行 | TC-AUTHZ-009/010 + E2E |
| 门控关 no-op | enabled flag | 354 既有测试（test profile false）|
| bootstrap 提升/幂等/安全 | AdminAuthzBootstrap | TC-BOOT-001..003 + E2E |
| matrix-param 不绕过 | UrlPathHelper lookup path | TC-AUTHZ-014 + E2E |

## 6. 设计调整说明
见 `design-adjustments.md`（AD-1..AD-5，均来自 Step 0 评审）。AD-1（C1 matrix-param）为必修安全修复。

## 7. 修复确认记录（Step 0 双代理评审）
- 评审结果：代码 **C:1** H:0 M:4 L:多 / 测试·文档 C:0 H:0 M:3 L:多。
- **C1（必修，已修）**：matrix-param (`;`) 鉴权绕过 → 改用 `UrlPathHelper.getLookupPathForRequest`，加 TC-AUTHZ-014 回归 + E2E 真实 Tomcat 验证。
- M（已修）：M3-code 被禁用用户失权（ElevationService +enabled 检查）、M2/M3-test sibling-prefix + features 写测试、M1-docs 注册机制措辞、L1 OPTIONS 断言加强。
- M4（接受）：bootstrap 可提升多个同 code 角色 —— 应用层 `existsByCode` 已防重复，正常路径不可能；接受。
- 复评阈值：C=0 ✅、H≤3（0）✅、M≤10（剩 1 接受项）✅。

## 8. 十一类失败模式检查 (a–k)
- (a) 幻觉：无；UrlPathHelper/getLookupPathForRequest、HandlerInterceptor、Role.getAdminAccess 均真实。
- (b) 范围蔓延：限于 authz 包 + ForbiddenException + config + 2 测试；无实体/前端改动。
- (c) 级联错误：拦截器只在 gated 时抛 401/403；ElevationService 边界返回 false（不吞真错误）。
- (d) 上下文丢失：实现匹配 D1-D6（docs 评审逐条 PASS，注册机制措辞已纠正 AD-5）。
- (e) 工具误用：Edit/Write + 专用测试；native UPDATE 同 ProjectTypeBackfill 既有模式。
- (f) 运行时行为偏差：**经 E2E 真实 Tomcat 验证**（matrix-param 绕过在 MockMvc 与真实 Tomcat 均 401；静态匹配口径与 DispatcherServlet 一致）。
- (g) 管线断链：token→username→isElevated→401/403 全链单测 + E2E 打通。
- (h) 内容质量偏差：无。
- (i) 指令衰减：standing 约束执行（E2E 写全被拒、无 stray 数据；唯一变更 = bootstrap 合法 seed）。
- (j) 覆盖真空：门控关时 354 测试不覆盖 authz，但 AdminAuthorizationTest（enabled=true 独立 context）+ E2E 真实 profile 覆盖 gated 路径 → 非真空。
- (k) 契约断层：401/403 JSON 体经 GlobalExceptionHandler 统一（与既有 401 同构）；前端无需改（v0.0.20 已隐藏 admin UI，本版为后端深度防御）。

## 9. 结论
全量 backend 371/371 通过；E2E 三态（401/403/200）+ bootstrap + matrix-param 绕过闭合均在真实 Tomcat 验证；
存量业务数据除 PMO bootstrap 合法 seed 外不变。Step 0 评审 C:1→0（matrix-param 必修已修）。0 新表/列/依赖，0 前端改动。
质量信号全绿，建议进入 Gate 3。
