# 初始化 Rainier 前后端系统骨架 (v0)

## Why

[A-角色意图卡片.md](../../A-角色意图卡片.md) 已定义了一个 17 角色、双维度（组织 / 项目）、4 飞轮的 AI 原生企业 PM 系统。仓库目前只有 STDD 流程文档与产品方案文档，没有任何产品代码——后续每一张角色卡片的能力（项目驾驶舱、关联面板、AI 工作日志、需求收件箱……）都没有"落地容器"。

本次变更不实现任何角色能力，只搭"能跑通、能扩展、能演示流程"的最小骨架，让后续每一张卡片可以作为一个 STDD 变更逐步切片落进来。

## What Changes

- 新建 `backend/` Spring Boot 2.7.x（Java 8 兼容）+ Maven + MySQL 8 工程
- 新建 `frontend/` React 18 + Vite + TypeScript 工程，UI 主题贴近飞书项目风格（圆角卡片、`#3370FF` 主色、简洁中后台密度）
- 后端：`GET /api/health` 健康检查、`POST /api/auth/login`（账号密码任意 → 返回固定 mock JWT）、`GET /api/auth/me`（凭 token 返回当前用户）
- 前端：登录页 + 首页（显示当前用户 + "Hello, <name>" 占位）+ 路由守卫 + 全局 API 客户端
- 测试脚手架：后端 JUnit 5 + Mockito，前端 Vitest + React Testing Library，各含 1 个冒烟用例（确保 STDD Phase 4 可用）
- 本地开发：`docker-compose.yml` 一键起 mysql + backend + frontend
- 工程规范：后端 Spotless + Checkstyle，前端 ESLint + Prettier，根 `.editorconfig`
- 根目录新增 `README.md`，写清启动步骤、目录约定、新接口 / 新页面的扩展位置

## Capabilities

### Modified Capabilities

- （无 — 项目尚无既有产品代码）

### New Capabilities

- `backend-scaffold`：Spring Boot 后端工程骨架（分层包结构 `controller/service/repository/domain/config`、统一异常处理、CORS、JSON 序列化约定）
- `frontend-scaffold`：React + Vite 前端工程骨架（路由、状态、Axios 客户端、飞书风格主题 token、布局组件）
- `health-check`：后端 liveness 探针接口
- `auth-placeholder`：登录占位流程（mock JWT、本地 token 存储、`/me` 接口、路由守卫）—— 仅打通端到端，**不接真实 IdP**
- `dev-runtime`：Docker Compose 本地开发环境（MySQL 8 + backend + frontend，含 healthcheck 与等待依赖）
- `test-runtime`：前后端单测脚手架与首个冒烟用例，保证 Phase 4 起步即可 RED→GREEN

## Impact

**代码层面**：

- 新增 `backend/`：约 20-30 个文件（`pom.xml`、`RainierApplication.java`、`AuthController` / `HealthController`、`AuthService` + mock 实现、`User` domain、统一异常 handler、CORS 配置、`application-{dev,test}.yml`、1 个 `@SpringBootTest` 冒烟用例）
- 新增 `frontend/`：约 20-30 个文件（`package.json`、`vite.config.ts`、`tsconfig.json`、`main.tsx`、路由、`pages/Login`、`pages/Home`、`api/client.ts`、`api/auth.ts`、`store/auth.ts`、飞书风格 theme tokens、1 个 Vitest 冒烟用例）
- 根目录新增：`docker-compose.yml`、`Makefile`（可选）、`README.md`、`.editorconfig`、`.gitignore` 增补

**配置层面**：

- 后端 `application.yml`（profile：`dev` 用本地 MySQL；`test` 用 H2 内存库）
- 前端 `.env.development` / `.env.production`（API base URL）
- Lint：Checkstyle / Spotless、ESLint、Prettier 配置文件
- 不修改 `.stdd/` 任何文件（STDD 工具配置 ≠ 产品配置）

**基础设施**：

- 本地：Docker Compose 起 MySQL 8.0（持久化 named volume）
- 远端：本次不涉及部署
- 第三方服务：本次**不**引入 LLM、Figma、钉钉、Outline、GitLab、禅道任何集成（留待后续按角色卡片切片）

## Success Criteria

- [ ] 干净环境下执行 `docker compose up` 能启动 mysql、backend、frontend 三个服务且 backend healthcheck 通过
- [ ] `curl http://localhost:8080/api/health` 返回 200 且 body 含 `"status":"UP"`
- [ ] 浏览器访问 `http://localhost:5173` 显示登录页，视觉风格符合飞书项目调性（主色 `#3370FF`、圆角 6px、卡片化布局）
- [ ] 在登录页输入任意 username / password 提交后跳转首页，首页右上角显示登录用户名，主区域显示 `Hello, <username>`
- [ ] 后端执行 `mvn test` 全部通过（至少 1 个 `@SpringBootTest contextLoads` 用例）
- [ ] 前端执行 `npm run build && npm test` 均通过（生产构建无 type error、Vitest 冒烟用例通过）
- [ ] 后端 `mvn spotless:check checkstyle:check` 与前端 `npm run lint` 均零告警
- [ ] `README.md` 包含：技术栈版本、3 步启动指令、目录结构图、"如何加一个后端接口 / 前端页面" 扩展指引
- [ ] 本次变更**不**新增任何业务领域实体（User / Team / Project / Story / Task 等留待后续 STDD 变更）
