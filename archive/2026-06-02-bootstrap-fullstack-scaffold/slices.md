# v0 Bootstrap 切片执行计划

> 共 14 个切片，按拓扑依赖排序；同优先级中可并行的切片以 ⇄ 标注。
> 每个切片 = 1+ Scenario → 1+ 测试 → 1 个可独立 commit 的实现单元。

| # | ID | 优先级 | TC 覆盖 | 实现目标 | 依赖 |
|---|---|---|---|---|---|
| 1 | SLICE-B01 | P0 | TC-BES-001 / TC-TRT-001 | Maven 工程骨架 + `RainierApplication` + `application-{dev,test}.yml`（H2 for test）+ `contextLoads` 冒烟测试 | 无 |
| 2 | SLICE-B02 | P0 | TC-HLT-001 | `HealthController` + `GET /api/health`（返回 `{"status":"UP"}`）+ MockMvc 测试 | 1 |
| 3 | SLICE-B03 | P1 | TC-BES-002 / TC-BES-003 | `GlobalExceptionHandler` + 404 JSON 化（throw-exception-if-no-handler-found）+ test-profile 注册 `/api/_diag/boom` + MockMvc 测试 | 1 |
| 4 | SLICE-B04 | P1 | TC-BES-004 | `CorsConfig` WebMvcConfigurer + MockMvc 预检测试 | 1 |
| 5 | SLICE-B05 | P0 | TC-AUT-001 / TC-AUT-002 | `User` domain / `LoginRequest|Response` DTO / `AuthService.issueToken` (jjwt HS256) / `AuthController.login` / 入参校验 / MockMvc 测试 | 1, 3 |
| 6 | SLICE-B06 | P0 | TC-AUT-003 / TC-AUT-004 | `SecurityFilter` (OncePerRequestFilter, 校验 `/api/auth/me` Bearer) + `AuthController.me` + MockMvc 测试（含无 token / 非法 token / 过期 token） | 5 |
| 7 | SLICE-B07 | P1 | TC-TRT-002 | pom 引入 Spotless + Checkstyle 插件，配置 google_checks，全部源码通过 `mvn spotless:check checkstyle:check` | 2, 3, 4, 5, 6 |
| 8 | SLICE-F01 | P0 | TC-TRT-003 / TC-TRT-004（基线） | `frontend/` 初始化（package.json / vite.config.ts / tsconfig.json / index.html / main.tsx / App.tsx 空壳）+ Vitest + RTL + jsdom 配置 + 1 个 smoke 测试 | 无 |
| 9 | SLICE-F02 | P1 | TC-FES-003 | `styles/tokens.ts` + `styles/global.css`（`:root` 注入 `--rainier-*` 变量）+ computed style 测试 | 8 |
| 10 | SLICE-F03 | P0 | （foundation, 无独立 TC） | `api/client.ts` (Axios + 请求/响应拦截器) + `api/auth.ts` + `store/auth.ts` (Zustand) + 单测 | 8 |
| 11 | SLICE-F04 | P0 | TC-FES-001 / TC-FES-002 / TC-FES-004 | `components/ui/{Button,Input,Card}` + `AppLayout` + `ProtectedRoute` + `pages/Login` + `pages/Home` + 路由守卫 RTL 测试 | 9, 10 |
| 12 | SLICE-F05 | P1 | TC-TRT-005 | ESLint (@typescript-eslint + react + react-hooks) + Prettier 配置，全部源码通过 `npm run lint` | 8, 9, 10, 11 |
| 13 | SLICE-D01 | P0 | TC-DRT-001 / TC-DRT-002 | `backend/Dockerfile` + `frontend/Dockerfile`（multi-stage build + nginx）+ `frontend/nginx.conf`（反代 `/api`）+ `docker-compose.yml`（mysql/backend/frontend + healthchecks + depends_on healthy） | 7, 12 |
| 14 | SLICE-D02 | P0 | （SC 覆盖：README） | `README.md`（启动 3 步 / 技术栈版本 / 目录结构 / 扩展约定）+ `.editorconfig` + `.gitignore` 增补 + `.dockerignore` | 13 |

## 并行机会

- ⇄ **后端 Phase A**：3 ⇄ 4 可并行（均仅依赖 1）
- ⇄ **前端 Phase A**：9 ⇄ 10 可并行（均仅依赖 8）
- ⇄ **后端 vs 前端**：1-7 与 8-12 完全可并行（无交叉依赖）

> 长程模式自动执行采用串行（避免 commit 噪声）；并行机会留作未来手工 review 用。

## 推荐执行顺序（串行）

```
1 → 2 → 3 → 4 → 5 → 6 → 7  (backend 7 slices)
8 → 9 → 10 → 11 → 12       (frontend 5 slices)
13 → 14                     (runtime 2 slices)
```

## 风险点

- **SLICE-B05 与 SLICE-B03 顺序**：B05 抛 `BadRequestException` 需 B03 的 handler 兜底，故 B05 依赖 B03。
- **SLICE-B07 / F05 排在所有源码之后**：lint 配置对存量代码做硬约束，提前加会卡 build。
- **SLICE-D01 在所有代码就绪后**：Dockerfile 复制目标必须已存在；compose 启动要打通端到端。
- **SLICE-F04 的 UI 组件**：必须用 tokens 变量而非硬编码颜色，否则 TC-FES-004 直接红。
