# v0.0.118-user-profile-route Phase 5 Test Report

> 执行日期：2026-06-30
> 变更：前端补齐 `/users/:id/profile` 下属档案路由
> 范围：frontend only

## 1. 结论

P5 通过。`/users/:id/profile` 已注册为受保护前端路由，页面按 path param 调用 `GET /api/users/{id}/profile`，并复用 `/profile` 的档案展示组件。`/profile` 保持调用 `GET /api/me/profile`。

P6 前补充验证：本地预览登录失败根因是 Vite dev server 跑在 `http://127.0.0.1:5174` 时，浏览器请求携带该 Origin，后端 CORS allowlist 只包含 `http://localhost:5173` / `http://localhost`，导致 proxied `POST /api/auth/login` 返回 `403 Invalid CORS request`。已在 Vite dev proxy 中把 `/api` 转发请求的 Origin 重写为后端允许的 dev origin，本地浏览器已可用 `alice / rainier123` 登录工作台。

## 2. RED 证据

先写失败测试再实现：

```bash
cd frontend && npm test -- --run src/AppRoutes.test.tsx src/pages/Profile/UserProfilePage.test.tsx
```

结果：失败符合预期。

- `AppRoutes` 断言 `/users/:id/profile` 注册数为 0。
- `UserProfilePage.test.tsx` 无法解析 `./UserProfilePage`。

## 3. GREEN 证据

### 3.1 聚焦测试

```bash
cd frontend && npm test -- --run src/api/profile.test.ts src/AppRoutes.test.tsx src/pages/Profile/ProfilePage.test.tsx src/pages/Profile/UserProfilePage.test.tsx src/pages/Subordinates/SubordinatesPage.test.tsx
```

结果：5 个测试文件通过，36 个用例通过。

覆盖：

- TC-UPROF-001：`AppRoutes` 注册 `/users/:id/profile` 并真实渲染页面。
- TC-UPROF-002：`UserProfilePage` 调用 `getUserProfile(42)` 并展示指定用户档案。
- TC-UPROF-003：`getMyProfile()` 继续调用 `/me/profile`。
- TC-UPROF-004：下属列表既有链接契约保持，目标路由已补齐。

### 3.2 前端全量测试

```bash
cd frontend && npm test -- --run
```

结果：69 个测试文件通过，330 个用例通过。

备注：输出中仍有既有 React Router future flag / `act(...)` warning，以及本地 `--localstorage-file` 提示；均非本变更引入的失败项。

### 3.3 Lint

```bash
cd frontend && npm run lint
```

结果：通过，`eslint . --max-warnings 0` 无 warning。

### 3.4 Build

```bash
cd frontend && npm run build
```

结果：通过，`tsc -b && vite build` 成功。

备注：Vite 仍提示部分 chunk 大于 500KB，这是既有构建体积 warning，非本变更引入的失败项。

### 3.5 Diff 检查

```bash
git diff --check
```

结果：通过，无 trailing whitespace / conflict marker。

### 3.6 本地登录回归

```bash
curl -i \
  -H 'Origin: http://127.0.0.1:5174' \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"rainier123"}' \
  http://127.0.0.1:5174/api/auth/login
```

结果：200 OK。浏览器实测 `http://127.0.0.1:5174/login` 使用 `alice / rainier123` 成功进入 Rainier 工作台。

## 4. 未执行项

- 后端测试未执行：本变更不改后端代码，复用 C3 已存在的 `GET /api/users/{id}/profile` 服务端鉴权。
- STDD CLI bridge 未执行：本机 `python` 命令不存在；`python3 bin/stdd --help` 进一步确认项目根目录没有 `bin/stdd` 文件。
- 浏览器 E2E 未执行：当前由 Vitest + MemoryRouter 覆盖前端路由闭环。

## 5. 风险与 Caveats

1. `/users/:id/profile` 的 401/403/404 仍由后端接口与全局 axios 拦截器处理，本页当前只做静默空态，不新增错误页。
2. `ProfileView` 是只读展示组件，已按 self/member 视角区分基础统计与空态文案；更深的成员档案 UX polish 留给后续。
3. `/me/subordinates` 的「查看档案」链接现在有真实目标页；是否只对 HEAD 展示入口仍沿用 H4 的 `listLedTeams()` 运行时显隐策略。
4. Vite proxy Origin 重写只影响本地 dev server，不改变生产 nginx 反代路径。
