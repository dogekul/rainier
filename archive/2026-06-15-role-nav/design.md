# Design — v0.0.20-role-nav

> Baseline: tag `v0.0.19-requirement-enrich` / commit d706864. backend 348 + frontend 77 green, 19 表.

## Context

v0.0.18 给了「我是谁」（`me()` → id/roles/projects），但应用没有「我能看到什么」的概念：`AppLayout` 把全 6 个导航组无条件
渲染给每个登录用户，`AppRoutes` 对每条 admin 路由零守卫，`me()` 只在 `WorkbenchPage` 内部调用（其它页面拿不到角色）。
本版加最小分级：`Role.adminAccess` 布尔 → `me().roles[].adminAccess` → 前端 `isElevated` → 裁剪菜单 + 守卫路由。

约束：
- Java 8 / Spring Boot 2.7，MySQL 严格模式，`ddl-auto=update`（新列必须可空，否则 ALTER NOT NULL 在有存量行时报错）。
- standing：不删改存量业务数据。当前仅 PMO/YFM 两角色，adminAccess 默认/兜底 false。
- D3：仅前端 UX 收口，不动后端鉴权（admin 端点本版仍对任何 token 开放——这是后续 B）。

## Decisions

### D1 — `Role.adminAccess`：可空布尔 + 读时兜底 null→false
- **方案**：`@Column(name = "admin_access")  private Boolean adminAccess = Boolean.FALSE;`（**不加 `nullable=false`**）。
  getter 读时兜底：`return adminAccess == null ? Boolean.FALSE : adminAccess;`（与 v0.0.16 projectType / v0.0.19
  状态列同模式——可空列 + 读 coalesce，避免对存量行做 ALTER NOT NULL）。
- **为什么**：`ddl-auto=update` 对已存在的 `rainier_role` 表加 NOT NULL 列会失败（存量行无默认）。可空 + Java 默认值
  保证新建行写 false、存量行 NULL 读为 false。
- **备选（排除）**：`nullable=false` + bootstrap backfill runner（如 ProjectTypeBackfill）。排除理由：单布尔标记语义上
  「无 = 非管理员」天然成立，读兜底比一条 backfill runner 更省；无需 native UPDATE 改存量。

### D2 — 提升态 = 「任一所属角色 adminAccess=true」（全局，不分项目）
- **方案**：`isElevated(user) = (user?.roles ?? []).some(r => r.adminAccess === true)`。
- **为什么**：当前需求是「管理员 vs 普通用户」的二元分级，全局判定足够；项目级差异留到显式排除。
- **备选（排除）**：按 projectId 分项目判定 admin。排除理由：导航是全局壳（非项目内），分项目无意义；超范围。

### D3 — `me().roles[].adminAccess`：MeService 读 Role.adminAccess 兜底
- **方案**：`MeResponse.MeRole` 加 `Boolean adminAccess`；`MeService` 组装时
  `r == null ? Boolean.FALSE : (r.getAdminAccess() == null ? false : r.getAdminAccess())`（getter 已兜底，这里再防
  roleMap 缺失 role 的 null）。
- **为什么**：前端 elevation 完全依赖 me() 返回，必须非空。

### D4 — 导航分级：NavGroup 加 `requiresAdmin`，按 isElevated 过滤
- **方案**：`NavGroup` 加 `requiresAdmin?: boolean`；org/product/hr/sys 标 `requiresAdmin: true`，workbench/pm 不标。
  渲染前 `navGroups.filter(g => !g.requiresAdmin || elevated)`。
- **为什么**：组级标记最小改动，顺序/结构不变，测试可断言「普通用户只见 2 组」。
- **备选（排除）**：item 级权限。排除理由：当前粒度到组即可，pm 组对全员开放、内部不再细分。

### D5 — me() 注水提升到 ProtectedRoute（app 级）+ admin 路由守卫
- **方案**：`ProtectedRoute` 挂载时若 store.user 无 roles 则调 `me()` → `setAuth(token, {...})`；同时对 admin 路由前缀
  做守卫：用 `useLocation()` 判断当前 path 命中 admin 前缀且 `!isElevated` → `<Navigate to="/" replace/>`。
  守卫在 me() 注水**完成前**（roles 未知）放行（避免管理员被误踢），注水后再生效。
- **守卫触发时机**：用 `hydrated` 本地态（me() resolve 或失败后置 true）。`hydrated=false` 时不守卫（渲染 Outlet），
  `hydrated=true` 且非 admin 命中 admin 前缀时 Navigate。这样首帧不闪错跳。
- **为什么**：ProtectedRoute 包裹所有受保护路由，是唯一的 app 级注水点；WorkbenchPage 不再重复调 me()。
- **备选（排除）**：每个 admin 页面各自守卫。排除理由：N 处重复，集中在 ProtectedRoute 更内聚（deep module）。
- **admin 路由前缀**：`/org`、`/hr`、`/sys`、以及 product 组的 `/pm/products`/`/pm/product-modules`/`/pm/features`
  （注意 product 组与 pm 组共享 `/pm` 前缀，故守卫用**精确前缀集**而非单纯 `/pm`，否则会误伤 pm 组的全员路由）。

### D6 — RolesPage adminAccess 复选框
- **方案**：编辑/新建抽屉加一个「管理员权限」checkbox，绑定 `adminAccess` state；create/update body 带上。
- **为什么**：adminAccess 必须有 UI 维护入口，否则永远没人能成为管理员。

## Architecture

```
登录 → token in store
  ↓
ProtectedRoute (app 级)
  ├─ token? 无 → /login
  ├─ 挂载: store.user.roles 缺 → me() → setAuth(token, {id,name,roles[adminAccess],projects})  [注水]
  ├─ hydrated=true & 当前 path ∈ adminPrefixes & !isElevated(user) → Navigate "/"   [守卫]
  └─ Outlet
       └─ AppLayout: navGroups.filter(!requiresAdmin || isElevated(user))   [裁剪]
            └─ WorkbenchPage: 读 store.user（不再自调 me()）

后端: me() → MeService → MeRole.adminAccess = Role.adminAccess(兜底 false)
      Role.adminAccess ← RolesPage checkbox → POST/PUT /api/roles
```

## Risks / Trade-offs

| 风险 | 等级 | 缓解 |
|---|---|---|
| 加 NOT NULL 列致 ddl 失败 | 🔴 | D1 可空列 + 读兜底，存量行 NULL→false |
| 守卫首帧把管理员误踢回 / (me() 未注水时 roles 未知) | 🟡 | D5 hydrated 门控：注水完成前不守卫 |
| product 组与 pm 组共享 `/pm` 前缀，守卫误伤全员 pm 路由 | 🟡 | D5 用精确 admin 前缀集（含 3 条 /pm/product* 路径），不用裸 `/pm` |
| 存量 PMO 用户上线后突然变普通视图 | 🟡（预期行为）| 文档+E2E 明示：勾选 PMO adminAccess 即恢复；不是 bug |
| me() 从 WorkbenchPage 移走破坏既有 WB 测试 | 🟡 | WorkbenchPage 测试改为 seed store.user；WorkbenchPage 完全不再调 me()（让 proposal「应用内只有 ProtectedRoute 调一次 me()」严格成立） |
| D3 仅前端：admin API 仍对普通用户开放 | 🟢（有意，已排除）| proposal 显式排除，后续 B 收口 |
