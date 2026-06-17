# Test Plan — v0.0.21-admin-authz

> Baseline backend 354 / frontend 106 / 19 表. Scope: 1 NEW capability (backend-authz), 0 表/列, 后端 only.

## 1. 测试策略

- 门控关（test profile）下既有 354 测试零改动全绿（回归保护）。
- 鉴权真实路径由 **AdminAuthorizationTest**（`@TestPropertySource` 置 `admin-authz.enabled=true`，独立 context）覆盖。
- bootstrap 由 **AdminAuthzBootstrapTest** 覆盖（提升 + 幂等）。
- E2E 真实 profile（enabled=true）验证 401/403/200 三态 + bootstrap 生效 + 全员端点不被误伤。
- 不删改存量数据（除 bootstrap 合法 seed）。

## 2. 详细测试案例

### AdminAuthorizationTest（enabled=true，MockMvc）

| TC-ID | 场景 | Act | Assert |
|---|---|---|---|
| TC-AUTHZ-001 | 无 token Tier A 写 | POST /api/roles 无 Authorization | 401 |
| TC-AUTHZ-002 | 无 token Tier A 读 | GET /api/audit-logs 无 token | 401 |
| TC-AUTHZ-003 | 非管理员 Tier A 写 | bob token POST /api/roles | 403 |
| TC-AUTHZ-004 | 非管理员 Tier A 删 | bob token DELETE /api/organizations/1 | 403 |
| TC-AUTHZ-005 | 管理员 Tier A 写放行 | alice(admin) token POST /api/roles 合法 body | 非 401/403（2xx） |
| TC-AUTHZ-006 | 非管理员 Tier B 读放行 | bob token GET /api/users | 200 |
| TC-AUTHZ-007 | 非管理员 Tier B 读放行（features/modules）| bob token GET /api/features、/api/product-modules | 200 |
| TC-AUTHZ-008 | 非管理员 Tier B 写被拒 | bob token POST /api/users | 403 |
| TC-AUTHZ-009 | 全员端点不被误伤 | bob token GET /api/projects | 200 |
| TC-AUTHZ-010 | OPTIONS 预检放行 | OPTIONS /api/roles | 非 401/403 |
| TC-AUTHZ-011 | 无效 token Tier A | 乱码 Bearer GET /api/roles | 401 |

### AdminAuthzBootstrapTest（enabled=true）

| TC-ID | 场景 | Arrange | Act | Assert |
|---|---|---|---|---|
| TC-BOOT-001 | 无 admin 提升 PMO | seed role code=PMO adminAccess=false，无其它 admin | run() | PMO.adminAccess=true + INFO 日志 |
| TC-BOOT-002 | 已有 admin 幂等 no-op | seed 一 admin 角色 + PMO(false) | run() | PMO 仍 false（未被动） |
| TC-BOOT-003 | 无 PMO 角色安全 no-op | 无 code=PMO 角色 | run() | 不抛异常，无变更 |

### 门控关回归

| TC-ID | 场景 | Assert |
|---|---|---|
| TC-AUTHZ-GATE-OFF | test profile（false）既有 354 测试 | 全绿零改动（含 RoleControllerCreateTest 无 token POST 仍 201） |

### E2E（docker，真实 profile enabled=true）

| TC-ID | 场景 | 预期 |
|---|---|---|
| TC-E2E-AUTHZ-001 | bootstrap | 重建后 PMO.admin_access=true（日志/SQL 可见），无新表(19) |
| TC-E2E-AUTHZ-002 | 无 token Tier A | curl POST /api/roles 无 token → 401 |
| TC-E2E-AUTHZ-003 | 管理员放行 | alice(=PMO 已 bootstrap) token POST /api/roles → 2xx；GET /api/audit-logs → 200 |
| TC-E2E-AUTHZ-004 | 非管理员 403 + Tier B GET 200 | 造一个非 admin 用户/角色 token：POST /api/roles→403；GET /api/users→200 |
| TC-E2E-AUTHZ-005 | 全员端点 | 任意（含无 token 既有行为）GET /api/projects → 不被拦 |
| TC-E2E-AUTHZ-006 | 存量数据 | 除 PMO bootstrap seed 外行数/数据不变 |

## 3. 测试执行矩阵

| 功能 | 专用集成(enabled=true) | 门控关回归 | E2E |
|---|---|---|---|
| Tier A 鉴权 401/403/200 | TC-AUTHZ-001..005/011 | — | TC-E2E-AUTHZ-002/003 |
| Tier B 写收口/读放行 | TC-AUTHZ-006/007/008 | — | TC-E2E-AUTHZ-004 |
| 全员端点/OPTIONS 不误伤 | TC-AUTHZ-009/010 | — | TC-E2E-AUTHZ-005 |
| 门控关 no-op | — | TC-AUTHZ-GATE-OFF (354) | — |
| bootstrap | TC-BOOT-001..003 | — | TC-E2E-AUTHZ-001/006 |

## 4. 回归风险矩阵

| 区域 | 风险 | 缓解 |
|---|---|---|
| 既有 39 admin 控制器测试 | 🔴 鉴权全红 | 门控关(test profile false) → 零改动 |
| 全员端点误伤 | 🔴 | Tier B GET 放行 + addPathPatterns 只注册 admin 路径 + TC-AUTHZ-009 |
| 拦截器异常未走 GlobalExceptionHandler | 🟡 | TC-AUTHZ-001/003 断言 401/403 JSON；若不走则改 preHandle 直写 response |
| /api/users vs /api/user-roles 前缀混淆 | 🟡 | 精确 path（`/api/users`+`/api/users/**` 独立注册），TC-AUTHZ-006/008 |
| bootstrap 改存量 PMO | 🟡（有意）| 仅无 admin 时、幂等、日志、E2E 说明 |
| 覆盖真空（门控关时鉴权零覆盖）| 🟡 | AdminAuthorizationTest(enabled=true) + E2E 真实 profile |

## 5. 建议补充顺序

P0：TC-BOOT-001..003 → TC-AUTHZ-001..011 → 门控关全量回归 → E2E TC-E2E-AUTHZ-001..006。
