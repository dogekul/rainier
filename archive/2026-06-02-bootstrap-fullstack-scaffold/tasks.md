# v0 Bootstrap 任务清单

> 14 个切片 × 5-8 个子任务，按拓扑依赖排序；每个子任务粒度为 1 次 RED→GREEN→REFACTOR 内可完成。

## 1. backend-scaffold（P0/P1）

- [ ] 1.1 创建 `backend/` 目录与 Maven 工程骨架（pom.xml：SB 2.7.18 parent + spring-boot-starter-web + jjwt-api/impl/jackson 0.11.5 + spring-boot-starter-test + h2 test scope）
- [ ] 1.2 编写 `com.rainier.RainierApplication`
- [ ] 1.3 编写 `application.yml`（dev：MySQL 8 datasource，sql.init.mode=never；test：H2 内存库）
- [ ] 1.4 编写 `@SpringBootTest contextLoads` 用例（SLICE-B01 GREEN）
- [ ] 1.5 编写 `GlobalExceptionHandler @RestControllerAdvice`（处理 `BadRequestException` / `UnauthorizedException` / `NoHandlerFoundException` / `Throwable`）
- [ ] 1.6 配置 `spring.mvc.throw-exception-if-no-handler-found=true` + `spring.web.resources.add-mappings=false`
- [ ] 1.7 在 `test` profile 注册 `/api/_diag/boom` endpoint（抛 RuntimeException）（SLICE-B03 GREEN）
- [ ] 1.8 编写 `CorsConfig WebMvcConfigurer`（白名单 `http://localhost:5173` / `http://localhost`，方法 GET/POST/PUT/DELETE/OPTIONS，头含 Authorization, Content-Type）（SLICE-B04 GREEN）

## 2. health-check（P0）

- [ ] 2.1 编写 `HealthController @GetMapping("/api/health")` 返回 `{"status":"UP"}`
- [ ] 2.2 编写 MockMvc 测试 `HealthControllerTest`（断言 200 + JSON + status=UP）（SLICE-B02 GREEN）

## 3. auth-placeholder（P0）

- [ ] 3.1 编写 `User` domain 与 `LoginRequest` / `LoginResponse` / `MeResponse` DTO
- [ ] 3.2 编写 `BadRequestException` / `UnauthorizedException` 自定义异常
- [ ] 3.3 编写 `AuthService.issueToken(username)`：HS256，secret 从 `application.yml` 读取（dev 注 `DO NOT USE IN PROD`），exp = now+24h
- [ ] 3.4 编写 `AuthService.parseToken(token)` 抛 `UnauthorizedException` if invalid
- [ ] 3.5 编写 `AuthController.login`：校验 username 非空否则抛 `BadRequestException`，签发 token + 返回 `LoginResponse`
- [ ] 3.6 编写 MockMvc 测试：登录成功、登录缺字段（SLICE-B05 GREEN）
- [ ] 3.7 编写 `SecurityFilter extends OncePerRequestFilter`：仅 `/api/auth/me` 路径生效，解析 Bearer，注入 `RequestAttribute.username`
- [ ] 3.8 编写 `AuthController.me`：从 RequestAttribute 取 username，返回 `MeResponse`
- [ ] 3.9 编写 MockMvc 测试：有效 token、无 token、非法 token、过期 token（SLICE-B06 GREEN）

## 4. backend lint（P1）

- [ ] 4.1 pom 加入 `spotless-maven-plugin`（googleJavaFormat AOSP）
- [ ] 4.2 pom 加入 `maven-checkstyle-plugin`（绑定 google_checks.xml）
- [ ] 4.3 运行 `mvn spotless:apply` 对全量源码自动格式化
- [ ] 4.4 运行 `mvn spotless:check checkstyle:check` 全绿（SLICE-B07 GREEN）

## 5. frontend-scaffold 基础（P0）

- [ ] 5.1 `frontend/package.json`（react 18 / react-dom 18 / react-router-dom 6 / zustand 4 / axios 1 / dev: vite 5 / typescript 5 / vitest 1 / @testing-library/react 14 / jsdom 23 / msw 2 / eslint / prettier 等）
- [ ] 5.2 `vite.config.ts`（dev server port 5173 + proxy `/api → http://localhost:8080`）
- [ ] 5.3 `tsconfig.json` / `tsconfig.node.json`（strict）
- [ ] 5.4 `index.html` + `src/main.tsx` + `src/App.tsx` 空壳
- [ ] 5.5 `vitest.config.ts`（environment jsdom + setupFiles）
- [ ] 5.6 `src/test/setup.ts`（jest-dom 扩展）
- [ ] 5.7 编写 1 个 smoke 测试 `App.test.tsx`（assert renders without crash）（SLICE-F01 GREEN）

## 6. frontend-scaffold 主题 tokens（P1）

- [ ] 6.1 `src/styles/tokens.ts`（导出 token 常量对象）
- [ ] 6.2 `src/styles/global.css`（`:root { --rainier-* }` + 重置 / 字体 / body bg）
- [ ] 6.3 `main.tsx` 导入 `global.css`
- [ ] 6.4 编写测试 `tokens.test.ts`（getComputedStyle 断言 `--rainier-color-primary` / `--rainier-radius-button` / `--rainier-radius-card`）（SLICE-F02 GREEN）

## 7. frontend-scaffold 状态与 API（P0）

- [ ] 7.1 `src/api/client.ts`（axios.create + 请求拦截器注入 `Authorization`，响应拦截器 401 → `useAuthStore.logout()` + `window.location='/login'`）
- [ ] 7.2 `src/api/auth.ts`（`login(username, password)` / `me()`）
- [ ] 7.3 `src/store/auth.ts`（Zustand store：`token`、`user`、`login`、`logout`、persistence to localStorage `rainier.token`）
- [ ] 7.4 编写测试 `store/auth.test.ts`（基础 set/clear/persist 行为）（SLICE-F03 GREEN）

## 8. frontend-scaffold 页面与守卫（P0）

- [ ] 8.1 `src/components/ui/Button.tsx`（使用 `var(--rainier-color-primary)` / `var(--rainier-radius-button)`）
- [ ] 8.2 `src/components/ui/Input.tsx`
- [ ] 8.3 `src/components/ui/Card.tsx`
- [ ] 8.4 `src/components/AppLayout.tsx`（页头 + 右上角用户名）
- [ ] 8.5 `src/components/ProtectedRoute.tsx`（读 store.token，无则 `<Navigate to="/login" replace />`）
- [ ] 8.6 `src/pages/Login/index.tsx`（卡片化表单 + Login Button）
- [ ] 8.7 `src/pages/Home/index.tsx`（卡片显示 `Hello, <username>`）
- [ ] 8.8 `src/App.tsx` 路由树（`/login` 公开，`/` 受保护）
- [ ] 8.9 编写 RTL 测试 `ProtectedRoute.test.tsx`、`Login.test.tsx`、`Home.test.tsx`，含 MSW 拦截 `/api/auth/login`（SLICE-F04 GREEN）

## 9. frontend lint（P1）

- [ ] 9.1 `.eslintrc.cjs`（@typescript-eslint/recommended + react/recommended + react-hooks/recommended + prettier）
- [ ] 9.2 `.prettierrc`（printWidth 100, singleQuote true, trailingComma all）
- [ ] 9.3 `package.json` 添加 `"lint": "eslint . --max-warnings 0"`
- [ ] 9.4 运行 `npm run lint` 全绿（SLICE-F05 GREEN）

## 10. dev-runtime（P0）

- [ ] 10.1 `backend/Dockerfile`（multi-stage：maven:3.9-eclipse-temurin-8 build → eclipse-temurin:8-jre runtime；EXPOSE 8080；HEALTHCHECK curl /api/health）
- [ ] 10.2 `frontend/Dockerfile`（multi-stage：node:18-alpine build `npm run build` → nginx:1.25-alpine 运行 dist）
- [ ] 10.3 `frontend/nginx.conf`（`location /api/ { proxy_pass http://backend:8080/api/; }`，SPA fallback `try_files $uri /index.html`）
- [ ] 10.4 `docker-compose.yml`（services: mysql 8.0 + backend + frontend，network `rainier-net`，volume `rainier-mysql-data`，healthchecks，`depends_on: { mysql: { condition: service_healthy } }`）
- [ ] 10.5 写半自动 E2E 脚本 `scripts/test-compose-up.sh`（启动 compose + 轮询 3 容器 healthy + curl 探活 + 关闭 compose）
- [ ] 10.6 跑一次 `bash scripts/test-compose-up.sh` 验证 TC-DRT-001/002（SLICE-D01 GREEN）

## 11. 文档与根工件（P0）

- [ ] 11.1 `README.md`（项目介绍 / 技术栈 / 启动 3 步：`docker compose up` 或 dev `mvn spring-boot:run` + `npm run dev` / 目录结构 / 扩展指引）
- [ ] 11.2 `.editorconfig`（utf-8 / lf / indent_size 2/4 by file type）
- [ ] 11.3 根 `.gitignore` 增补（`backend/target/` / `frontend/node_modules/` / `frontend/dist/` / `.idea/` 等）
- [ ] 11.4 `.dockerignore`（避免 target/node_modules 进 build context）（SLICE-D02 GREEN）

## 12. 测试与验证（Phase 5 入口）

- [ ] 12.1 `cd backend && mvn -ntp test` 全绿（TC-TRT-001）
- [ ] 12.2 `cd backend && mvn -ntp spotless:check checkstyle:check` 全绿（TC-TRT-002）
- [ ] 12.3 `cd frontend && npm test -- --run` 全绿（TC-TRT-003）
- [ ] 12.4 `cd frontend && npm run build` 全绿、`dist/index.html` 存在（TC-TRT-004）
- [ ] 12.5 `cd frontend && npm run lint` 0 errors / 0 warnings（TC-TRT-005）
- [ ] 12.6 `bash scripts/test-compose-up.sh` 通过（TC-DRT-001/002）
- [ ] 12.7 全部 TC（20 个）对照 test-plan.md 勾选

<!--
优先级说明：
- P0：阻塞性任务，对应 P0 TC，未完成不能 ship
- P1：重要任务，提交前完成
- P2：可灰度（本次仅 1 个：TC-FES-004 已内嵌在 8.9）
依赖标注：见 slices.md 的依赖列；任务清单内顺序即拓扑顺序
-->
