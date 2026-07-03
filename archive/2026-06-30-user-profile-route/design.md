# user-profile-route - 技术设计

## Context

Rainier 已有两个互补能力：

- C3 后端 `GET /api/users/{id}/profile`：返回与 `/api/me/profile` 相同结构的 `ProfileResponse`，并由服务端校验 self / 直接上级权限。
- H4 前端 `SubordinatesPage`：表格中的「查看档案」链接已经生成 `/users/{id}/profile`。

当前缺口是前端 `AppRoutes` 未注册 `/users/:id/profile`，且 `ProfilePage` 只调用 `getMyProfile()`。本变更只补前端路由与读取逻辑，不改后端权限模型。

## Decisions

### 1. 抽取 `ProfileView` 作为纯展示组件

**方案**：把 `ProfilePage` 中的档案 JSX 抽到 `ProfileView`，由 `ProfilePage` 与新 `UserProfilePage` 共享。

**为什么**：档案 UI 已经包含身份、贡献、能力标签和组织身份。复制这段 JSX 会让后续能力标签或贡献指标改动需要维护两份。

**备选方案及排除原因**：
- 备选 A：直接复制 `ProfilePage` 为 `UserProfilePage`。排除：重复代码太多，容易漂移。
- 备选 B：让 `ProfilePage` 根据路由参数切换 API。排除：`/profile` 是稳定的自档案入口，保持无参数页面更清晰。

### 2. API 层新增 `getUserProfile(userId)`

**方案**：在 `frontend/src/api/profile.ts` 增加 `getUserProfile(userId)`，返回同一个 `UserProfile` 类型。

**为什么**：后端响应结构与 `/api/me/profile` 一致，复用类型能避免新增重复 DTO。

**备选方案及排除原因**：
- 备选 A：在页面里直接调用 `client.get`。排除：违反前端分层规则，页面不直接依赖 axios/client。
- 备选 B：新增 `userProfile.ts` API 文件。排除：实体仍是 profile 能力，拆文件收益不高。

### 3. `/users/:id/profile` 只做前端入口，鉴权留给后端

**方案**：路由注册在现有 `ProtectedRoute` + `AppLayout` 下，页面读取 path param 后调用后端接口。

**为什么**：C3 后端已经定义完整授权矩阵，前端不应该复制上下级权限判断。

**备选方案及排除原因**：
- 备选 A：前端根据 led teams 判断是否允许访问。排除：会产生前后端权限漂移。
- 备选 B：新增 HR/Admin 直通策略。排除：超出本次“补路由断点”范围。

## Architecture

```text
SubordinatesPage
  Link /users/{id}/profile
        |
        v
AppRoutes /users/:id/profile
        |
        v
UserProfilePage
  useParams().id -> getUserProfile(id)
        |
        v
GET /api/users/{id}/profile
        |
        v
ProfileView(profile, title="成员档案")
```

`/profile` 保持：

```text
ProfilePage -> getMyProfile() -> ProfileView(profile, title="我的档案")
```

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|----------|
| 共享展示组件抽取时破坏 `/profile` 既有页面 | 保留 `ProfilePage.test.tsx` 既有断言，并新增路由测试 |
| `/users/:id/profile` 无效 id 导致 API 请求异常 | 页面在 id 非正整数时显示空态，不发请求 |
| 前端权限判断与后端不一致 | 前端不实现业务鉴权，只依赖服务端 401/403/404 |
