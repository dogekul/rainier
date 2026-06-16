# v0.0.20-role-nav — 角色分级导航（管理员 vs 普通用户）

> Baseline: tag `v0.0.19-requirement-enrich` / commit d706864. backend 348 + frontend 77 测试 green, 19 表.
> 来源: 角色旅程 pivot —— 「思考最原始的一个普通用户，会如何使用本系统」。用户选定从 **A = 角色分级导航** 起步。

## Why

系统已经能识别「我是谁」（v0.0.18 `me()` → id/roles/projects），但**从不据此约束「我能看到/能做什么」**。登录后无论
什么角色，看到的都是同一套 admin 控制台（组织/产品/需求管理/人事配置/系统全部 6 组菜单）。对一个最原始的普通业务用户/
一线执行者来说，「组织节点 CRUD」「用户-组织关系」「岗位/角色/用户角色」「审计日志」这些都是噪音——他只需要**我的工作台 +
需求管理**。系统缺一个「权限可见性」概念：谁是管理员、谁只是普通用户。本版补这个最小分级：给 `Role` 一个 `adminAccess`
标记，`me()` 带出该标记，前端据此**裁剪导航 + 守卫路由**。纯结构层，不依赖 AI，0 新表。

## What Changes

- 后端 `Role` 实体加 `adminAccess` 布尔标记（可空列、Java 默认 FALSE、读时 null→false 兜底），经 `/api/roles`
  create/update 的 checkbox 维护；`RoleDetail`/`RoleCreateRequest`/`RoleUpdateRequest` 同步。
- 后端 `GET /api/auth/me` 的每个 `roles[]` 元素加 `adminAccess` 字段（读 `Role.adminAccess` 兜底 null→false）。
- 前端 `me()` 类型 `MeRole` 加 `adminAccess`；新增 `isElevated(user)` 助手 = `roles.some(r => r.adminAccess)`。
- 前端 `ProtectedRoute` 提升为 app 级 me() 注水点：挂载时调 me() → 写入 store（让角色信息全局可用），并加路由守卫：
  非管理员直接敲 admin 路由（/org、/pm/products、/pm/product-modules、/pm/features、/hr、/sys）→ 跳回 `/`。
- 前端 `AppLayout` 导航组加 `requiresAdmin` 标记（org/product/hr/sys = admin；workbench/pm = 全员），按 `isElevated`
  过滤可见菜单组。
- 前端 `/hr/roles` 角色编辑表单加「管理员权限(adminAccess)」复选框。
- 前端 `WorkbenchPage` 改为从 store 读当前用户上下文（由 ProtectedRoute 注水），去掉它自身重复的 me() 调用。

## Capabilities

### Modified Capabilities

- `entity-role`：`Role` 加 `adminAccess` 字段 + create/update/detail 透传。
- `auth-placeholder`：`GET /api/auth/me` 的 `roles[].adminAccess` 字段。
- `frontend-scaffold`：me() 全局注水（ProtectedRoute）+ `isElevated` 助手 + 导航分级过滤 + admin 路由守卫 +
  RolesPage adminAccess 复选框 + WorkbenchPage 改读 store。

### New Capabilities

- 无（纯扩展；0 新表 / 0 新包 / 0 新端点）。

## Impact

**代码层面**:
- 后端：`Role`(+adminAccess 可空列 + getter/setter) / `RoleCreateRequest`/`RoleUpdateRequest`(+adminAccess) /
  `RoleDetail`(+adminAccess 读兜底) / `RoleService`(create/update set adminAccess，默认 false) /
  `MeResponse.MeRole`(+adminAccess) / `MeService`(组装 MeRole 时读 role.adminAccess 兜底 null→false)。
- 前端：`api/auth.ts`(MeRole +adminAccess) / `api/role.ts`(Role/RoleCreate/RoleUpdate +adminAccess) /
  `store/auth.ts`(`isElevated` 助手，AuthUser 不变) / `components/ProtectedRoute.tsx`(me() 注水 + 守卫) /
  `components/AppLayout.tsx`(NavGroup +requiresAdmin + 过滤) / `pages/Role/RolesPage.tsx`(+adminAccess 复选框) /
  `pages/Workbench/WorkbenchPage.tsx`(改读 store，去 me() 重复调用)。
- **配置/基础设施**：无新依赖、无新表、无新端点。`adminAccess` 为新加可空列（ddl-auto=update 安全，存量行该列为 NULL，
  读时兜底 false）。

## 关键决策（Gate 1 已锁定）

- **D1c**：分级机制 = `Role.adminAccess` 布尔（可空、默认 false、读 null→false 兜底）。不引入独立权限表/权限点系统
  （那是后续 B 的事）；当前用户的提升态 = 「任一所属角色 adminAccess=true」。
- **D2**：普通用户只看 **工作台 + 需求管理**；管理员看全 6 组。组级 `requiresAdmin` 标记：org/product/hr/sys = admin，
  workbench/pm = 全员。
- **D3**：A 仅做**前端 UX 收口**（隐藏菜单 + 前端路由守卫 redirect 非管理员从 admin 路由跳回 /）；**不做 API 安全**
  （后端鉴权收口是后续独立的 B，本版显式不做）。
- **D4**：me() 注水提升到 `ProtectedRoute`（app 级，全局可用）→ 写 store；`WorkbenchPage` 改读 store，去掉它自身重复
  的 me() 调用。

## 重要后果（存量数据）

当前 docker 仅有 **PMO / YFM** 两个角色，`adminAccess` 上线后默认/兜底为 **false** → 既有 PMO 用户**变为普通视图**，
直到有人在 `/hr/roles` 勾选 PMO 的 adminAccess。E2E 必须验证：未勾选 = 普通视图（仅工作台+需求管理）/ 勾选 = 全 6 组控制台。
（不删改存量数据：adminAccess 是纯新增可空列，存量行保持 NULL；勾选是用户后续显式动作。）

## 显式排除（往后）

- **后端 API 鉴权收口**（admin 端点真正拒绝非管理员）—— 后续 B（D3 明确本版只做前端 UX）。
- 细粒度权限点 / 权限表 / 角色-权限多对多 —— 不引入，adminAccess 单布尔足够当前。
- 按角色定制的差异化工作台内容（飞轮层 / 后续角色卡逐个落地）。
- 项目级（而非全局级）的 adminAccess 区分 —— 当前提升态是全局「任一角色 admin」，不分项目。

## Success Criteria

- [ ] `Role` 有 `adminAccess` 可空布尔列；create 默认 false，update 可改，detail 透出；存量行读为 false。
- [ ] `GET /api/auth/me` 的每个 `roles[]` 带 `adminAccess`（存量角色读为 false）。
- [ ] 前端 `isElevated(user)` = 任一角色 adminAccess 为真。
- [ ] 普通用户（无 admin 角色）登录后侧边栏只见 **工作台 + 需求管理** 两组。
- [ ] 管理员（任一角色 adminAccess=true）登录后侧边栏见全 6 组。
- [ ] 普通用户直接敲 admin 路由（/org、/pm/products、/hr、/sys 等）→ 被 redirect 回 `/`。
- [ ] `/hr/roles` 编辑表单有「管理员权限」复选框，保存后 adminAccess 持久化。
- [ ] `WorkbenchPage` 从 store 读上下文，应用内只有 `ProtectedRoute` 调一次 me()。
- [ ] E2E：PMO 未勾选 adminAccess → 普通视图；勾选后 → 全控制台。
- [ ] 全量回归 green（backend ≥348 / frontend ≥77 + 新增），0 新表（仍 19 表）。
