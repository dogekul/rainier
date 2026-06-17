# Pending Adjustments — v0.0.21-admin-authz

## Step 0 双代理评审结果（C:1 H:0 M:4 L:多）

C=1 超阈值（C=0），必修。以下为采纳修复。

### PA-1 (code-C1, 必修): matrix-param (`;`) 鉴权绕过
- **发现**：拦截器用 `request.getRequestURI()`（原始 URI）匹配，但 Tomcat 在路由前剥掉 `;...` 段。
  `GET /api/roles;x=1` → getRequestURI 含 `;x=1` → `AdminPaths.matches` 不命中 → 放行，但 DispatcherServlet
  仍路由到 RoleController → **未鉴权到达控制器**。经实测：`/api/roles;x=1`→200、`POST /api/user-roles;a=b`→400(过鉴权)。
  对所有 gated 端点（Tier A 读写 + Tier B 写）成立的未认证绕过。
- **修复**：拦截器改用 Spring 解析后的 lookup path（`UrlPathHelper.getLookupPathForRequest`，默认
  removeSemicolonContent=true + urlDecode=true）匹配 —— 与 DispatcherServlet 路由所用 path 一致，
  同时关闭 H2 类（大小写/`.`/`%2E`/normalization）。加回归测试 `/api/roles;x=1` 无 token → 401。

### PA-2 (code-M3, 安全): 被禁用(未删)用户仍保留提升态
- **发现**：`findByLoginName` 的 `@Where(del_flag=0)` 只排软删，不排 `enabled=false`。被禁用用户持有效 token
  仍 isElevated → admin（至 token 过期，无吊销）。
- **修复**：ElevationService 加载 user 后 `if (!Boolean.TRUE.equals(user.getEnabled())) return false;`。

### PA-3 (test-M2/M3): 补 sibling-prefix + Tier B features/modules 写 测试
- **发现**：`AdminPaths.matches()` 的 products/product-modules、users/user-roles 边界是承重逻辑，但无 TC 钉死；
  Tier B 写 403 只测了 users，未测 features/product-modules。
- **修复**：AdminAuthorizationTest 加：无 token `GET /api/products`→401（Tier A 读收口）vs 非管理员
  `GET /api/product-modules`→200（Tier B 读放行，证明 sibling 不串味）；非管理员 `PUT /api/features/1`→403。

### PA-4 (test-L1): OPTIONS 断言加强
- **修复**：TC-AUTHZ-010 由「非 401/403」加强为断言 `isOk()`（真实放行而非 404）。

### PA-5 (docs-M1): design.md/slices.md 注册机制陈述与实现不符
- **发现**：design.md:15/23/84 + slices.md 写「addPathPatterns 到各 Tier 具体路径」，实际
  `WebMvcConfig` 用 `/api/**` 广注册 + `AdminPaths.requiresAdmin` 单一来源决策（实现更优）。
- **修复**：更正 design.md/slices.md 措辞为「广注册 /api/**，分级由 AdminPaths.requiresAdmin 决策」。

### 未修复（记录可接受）
- code-M4（bootstrap 可提升多个同 code=PMO 角色）：`RoleService.create` 的 `existsByCode` 已在应用层防 code 重复，
  正常路径不可能有重复 PMO；仅直连 DB 造数据才触发，**接受**（低实际风险）。
- code-L5（每 gated 请求 3 查询无缓存）：当前规模可接受，记录。
- code-L6/test-L2/L3/L4：doc 措辞「deadlock→lockout」纯文字 / 单代表性 all-users 端点 / TC-005/BOOT 已证非假绿（评审确认）。
