# Rainier

> AI 原生企业项目管理系统 — v0 全栈骨架。
> 产品设计见 [`A-角色意图卡片.md`](./A-角色意图卡片.md) / [`archive/B-驱动飞轮.md`](./archive/B-驱动飞轮.md)。
> 研发流程使用 [STDD](./STDD.md)。

## 技术栈

| 层 | 技术 | 版本 |
|---|---|---|
| 后端 | Java + Spring Boot + Maven | 1.8 / 2.7.18 / 3.9+ |
| 数据库 | MySQL（v0 未建业务表） | 8.0 |
| 鉴权 | jjwt（mock JWT，HS256） | 0.11.5 |
| 前端 | React + Vite + TypeScript | 18.3 / 5.4 / 5.6 |
| 路由 / 状态 / HTTP | React Router / Zustand / Axios | 6 / 4 / 1 |
| UI | 自研最小组件（Button/Input/Card）+ 飞书风格 design tokens | — |
| 后端测试 | JUnit 5 + Mockito + Spring Boot Test | 5.x |
| 前端测试 | Vitest + React Testing Library + jsdom | 2.x |
| 后端 lint | Spotless（Google Java Format）+ Checkstyle | 2.30 / 3.3 |
| 前端 lint | ESLint 9 + typescript-eslint 8 + Prettier | 9 / 8 / 3 |

## 3 步启动

> 任选一种：A 「一键 Docker」 或 B 「本地开发」

### A. 一键 Docker（推荐演示）

```bash
docker compose up -d --build      # 起 mysql + backend + frontend
open http://localhost              # 浏览器访问；任意账号密码登录
docker compose down -v             # 拆掉（含数据卷）
```

健康检查：
```bash
curl http://localhost:8080/api/health   # {"status":"UP"}
curl -I http://localhost/                # 200 OK
```

### B. 本地开发（前后端各自跑）

需先安装 JDK 8（推荐 Corretto 8）、Maven 3.6+、Node 18+。

```bash
# Terminal 1 — backend
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) mvn spring-boot:run

# Terminal 2 — frontend（vite dev server 通过 proxy 转发 /api → :8080）
cd frontend
npm install
npm run dev
```

浏览器打开 <http://localhost:5173/>，任意账号密码登录后进入首页。

## 目录结构

```
.
├── A-角色意图卡片.md           # 产品方案 Step A
├── STDD.md / AGENTS.md          # 研发流程
├── archive/ / changes/ / specs/ # STDD 变更归档与活跃变更
├── backend/                     # Spring Boot 后端
│   ├── pom.xml
│   ├── checkstyle.xml
│   ├── Dockerfile
│   └── src/{main,test}/{java,resources}
├── frontend/                    # React + Vite 前端
│   ├── package.json
│   ├── vite.config.ts
│   ├── eslint.config.js
│   ├── Dockerfile
│   ├── nginx.conf
│   └── src/{api,store,components,pages,styles,test}
├── scripts/
│   └── test-compose-up.sh       # 半自动 E2E：TC-DRT-001 / TC-DRT-002
└── docker-compose.yml
```

## 扩展指引

### 加一个后端接口

1. 在 `backend/src/main/java/com/rainier/<module>/` 下按 `controller / service / domain / dto` 分层添加文件
2. 业务异常继承 `com.rainier.common.exception.{BadRequestException,UnauthorizedException}`，会被 `GlobalExceptionHandler` 自动包成 JSON
3. 单元测试放 `backend/src/test/java/com/rainier/<module>/`，使用 `@SpringBootTest + @AutoConfigureMockMvc + @ActiveProfiles("test")`
4. `mvn -ntp test` 验证；`mvn -ntp spotless:apply spotless:check checkstyle:check` 通过 lint

### 加一个前端页面

1. 在 `frontend/src/pages/<Feature>/index.tsx` 写页面，引用 `components/ui/{Button,Input,Card}`
2. 在 `frontend/src/AppRoutes.tsx` 注册路由（默认在 `<ProtectedRoute>` 下，受登录守护）
3. 接口调用：`frontend/src/api/<feature>.ts` 用 `client` 实例（已自动注入 Bearer + 401 跳登录）
4. RTL 测试用 `<MemoryRouter initialEntries={...}>` 包 `<AppRoutes />`；样式用 `var(--rainier-*)` token，不硬编码颜色
5. `npm test -- --run`、`npm run build`、`npm run lint` 三件套全绿

## 测试与质量

```bash
# 后端
cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 1.8) mvn -ntp test
cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 1.8) mvn -ntp spotless:check checkstyle:check

# 前端
cd frontend && npm test -- --run
cd frontend && npm run build
cd frontend && npm run lint

# E2E (半自动)
bash scripts/test-compose-up.sh
```

## 设计与规格

- 技术决策：[`changes/2026-06-02-bootstrap-fullstack-scaffold/design.md`](./changes/2026-06-02-bootstrap-fullstack-scaffold/design.md)
- 行为规格：[`changes/2026-06-02-bootstrap-fullstack-scaffold/specs/`](./changes/2026-06-02-bootstrap-fullstack-scaffold/specs/)
- 测试方案：[`changes/2026-06-02-bootstrap-fullstack-scaffold/test-plan.md`](./changes/2026-06-02-bootstrap-fullstack-scaffold/test-plan.md)
- 实现偏离记录：[`changes/2026-06-02-bootstrap-fullstack-scaffold/pending-adjustments.md`](./changes/2026-06-02-bootstrap-fullstack-scaffold/pending-adjustments.md)

## 已知边界（v0 占位，留待后续 STDD 变更）

- 登录是 mock JWT（任意账号密码都通过），未接真实 IdP
- MySQL 仅启动作演示，后端不查询任何表
- 不含业务领域实体（Project / Story / Task 等）
- 不集成 LLM / GitLab / 钉钉 / Outline / Figma / 禅道
- 不含 CI 配置
