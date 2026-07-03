# v0.0.118-user-profile-route 测试方案与详细案例

> 版本：v0.0.118
> 创建日期：2026-06-30
> 对应 Phase 2 Spec：`changes/2026-06-30-user-profile-route/specs/user-profile-route/spec.md`

## 一、测试策略

### 1.1 测试金字塔

本变更只改前端路由与页面消费，测试以 Vitest + React Testing Library 为主：

- API 单元测试：验证 `getUserProfile(42)` 走 `/users/42/profile`。
- 页面组件测试：验证 `UserProfilePage` 使用 path param 调指定用户档案 API 并渲染内容。
- 路由集成测试：验证 `AppRoutes` 注册 `/users/:id/profile`，不会落到兜底。
- 回归测试：保留 `/profile` 既有 `ProfilePage` 测试，确保仍走 `getMyProfile()`。

### 1.2 测试原则

- 先写 RED 测试，再做最小实现。
- 不 mock React Router 的 `useParams`，使用真实 `MemoryRouter` 路由。
- 不在前端复制后端鉴权逻辑，401/403/404 仍由 API 层和全局拦截器处理。

### 1.3 已有测试资产

| 测试文件 | 用例数 | 类型 | 覆盖范围 |
|----------|--------|------|----------|
| `frontend/src/pages/Profile/ProfilePage.test.tsx` | 5 | 页面组件 | `/profile` 自档案渲染、能力标签 |
| `frontend/src/pages/Subordinates/SubordinatesPage.test.tsx` | 3 | 页面组件 | 下属列表与 `/users/{id}/profile` 链接 |
| `frontend/src/AppRoutes.test.tsx` | 既有多例 | 路由集成 | App route tree 注册与挂载 |

## 二、详细测试案例

### 功能 1：指定用户档案前端路由

对应 spec Requirement：指定用户档案前端路由

#### 案例 1.1 — 路由注册并渲染指定用户档案页

| 字段 | 内容 |
|------|------|
| **ID** | TC-UPROF-001 |
| **对应 Spec** | `user-profile-route/spec.md` → Scenario: 下属档案路由已注册 |
| **优先级** | P0 |
| **预置条件** | Auth store 有 token，`AppRoutes` 在 `MemoryRouter` 中挂载 |
| **输入** | 访问 `/users/42/profile` |
| **预期结果** | 渲染 `profile-identity`，不回到 Workbench |
| **当前状态** | 已通过：`AppRoutes.test.tsx` TC-UPROF-001 |

#### 案例 1.2 — 指定用户页面调用 `/users/{id}/profile`

| 字段 | 内容 |
|------|------|
| **ID** | TC-UPROF-002 |
| **对应 Spec** | `user-profile-route/spec.md` → Scenario: 指定用户档案调用正确 API |
| **优先级** | P0 |
| **预置条件** | `getUserProfile` mock 返回 Bob 的 profile |
| **输入** | 渲染 `/users/42/profile` |
| **预期结果** | `getUserProfile(42)` 被调用，页面展示 Bob 的身份、贡献和组织身份 |
| **当前状态** | 已通过：`UserProfilePage.test.tsx` TC-UPROF-002 + `profile.test.ts` API 断言 |

#### 案例 1.3 — `/profile` 行为不变

| 字段 | 内容 |
|------|------|
| **ID** | TC-UPROF-003 |
| **对应 Spec** | `user-profile-route/spec.md` → Scenario: 我的档案行为不变 |
| **优先级** | P0 |
| **预置条件** | `getMyProfile` mock 返回 Alice 的 profile |
| **输入** | 渲染 `/profile` |
| **预期结果** | `getMyProfile()` 被调用，仍展示 Alice 的档案 |
| **当前状态** | 已通过：`ProfilePage.test.tsx` 回归 + `profile.test.ts` TC-UPROF-003 |

### 功能 2：下属查看档案链路闭环

对应 spec Requirement：下属查看档案链路闭环

#### 案例 2.1 — 下属链接目标被真实路由消费

| 字段 | 内容 |
|------|------|
| **ID** | TC-UPROF-004 |
| **对应 Spec** | `user-profile-route/spec.md` → Scenario: 下属链接进入真实档案页 |
| **优先级** | P1 |
| **预置条件** | 下属列表返回 `id=42` |
| **输入** | 点击「查看档案」链接 |
| **预期结果** | 导航到 `/users/42/profile`，由真实 route 渲染档案页 |
| **当前状态** | 已通过：`SubordinatesPage.test.tsx` 链接契约 + `AppRoutes.test.tsx` route 消费 |

## 三、测试执行矩阵

| 功能模块 | 单元测试 | 集成测试 | E2E | 状态 |
|----------|---------|----------|-----|------|
| `api/profile.ts` | `getUserProfile` endpoint | N/A | N/A | ✅ |
| `UserProfilePage` | 页面渲染 + API 参数 | N/A | N/A | ✅ |
| `AppRoutes` | N/A | `/users/:id/profile` mount | N/A | ✅ |
| `ProfilePage` | `/profile` 回归 | N/A | N/A | ✅ |

## 四、回归风险矩阵

| 风险区域 | V0.0.118 改动 | 已有回归保护 | 风险等级 |
|----------|-------------|-------------|---------|
| `/profile` 我的档案 | 抽取共享展示组件 | `ProfilePage.test.tsx` | 🟡 |
| 路由兜底 | 新增 `/users/:id/profile` | `AppRoutes.test.tsx` | 🟢 |
| 下属页面链接 | 不改链接，仅补目标 route | `SubordinatesPage.test.tsx` | 🟢 |

## 五、建议补充顺序

1. **第一优先**（部署前必补）：TC-UPROF-001、TC-UPROF-002、TC-UPROF-003
2. **第二优先**（部署后尽快补）：TC-UPROF-004
3. **第三优先**（后续补）：真实浏览器 E2E 点击链路
