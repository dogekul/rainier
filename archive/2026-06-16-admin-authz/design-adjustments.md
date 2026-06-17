# Design Adjustments — v0.0.21-admin-authz

Phase 4-5 期间相对 Phase 2 设计的调整。来自 Step 0 双代理评审。

## AD-1 (评审 C1, 必修): 按 Spring lookup path 匹配，防 matrix-param 绕过
- **原实现**：拦截器用 `request.getRequestURI()`（原始 URI）匹配 AdminPaths。
- **问题**：Tomcat 路由前剥 `;...` 段，`getRequestURI()` 保留 → `/api/roles;x=1` 不命中 gate 但仍路由到控制器
  → 未认证绕过（Tier A 读写 + Tier B 写）。实测确认。
- **调整**：改用 `UrlPathHelper.getLookupPathForRequest(request)`（默认 removeSemicolonContent + urlDecode），
  与 DispatcherServlet 路由所用 lookup path 一致。同时关闭大小写/normalization 类。加 TC-AUTHZ-014 回归。
- **接口/语义影响**：无（仍 401/403）；纯修正匹配口径。

## AD-2 (评审 M3, 安全): ElevationService 排除被禁用用户
- **原实现**：`findByLoginName` 只排软删，不排 `enabled=false`。
- **调整**：加载 user 后 `if (user == null || !Boolean.TRUE.equals(user.getEnabled())) return false;`。被禁用账号即时失权。

## AD-3 (评审 M2/M3): 补 sibling-prefix + Tier B features 写测试
- TC-AUTHZ-012：无 token `GET /api/products`→401 vs 非管理员 `GET /api/product-modules`→200（钉死 matches 边界）。
- TC-AUTHZ-013：非管理员 `PUT /api/features/1`→403（Tier B 写不止 users）。

## AD-4 (评审 L1): OPTIONS 断言加强为 `isOk()`（真实放行而非 404 也算过）。

## AD-5 (评审 M1, docs): 注册机制措辞纠正
- design.md/slices.md 原写「逐路径 addPathPatterns」；实际 `WebMvcConfig` 广注册 `/api/**` + `AdminPaths.requiresAdmin`
  单一来源决策（实现更优）。已更正文档。

## 未调整（评审记录可接受）
- M4 bootstrap 可提升多个同 code 角色：`RoleService.create` 应用层 `existsByCode` 已防 code 重复，正常路径不可能；接受。
- L5 每 gated 请求 3 查询：当前规模可接受。
- L6/L2/L3/L4：文字 nit / 单代表性端点 / TC-005/BOOT 已证非假绿。
