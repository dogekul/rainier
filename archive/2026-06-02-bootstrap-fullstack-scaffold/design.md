# 初始化 Rainier 前后端系统骨架 (v0) — 技术设计

## Context

仓库 stage 0：除 STDD 流程文档与产品方案（`A-角色意图卡片.md` / `archive/B-驱动飞轮.md`）外无产品代码。需为 17 角色双维度 PM 系统建立可扩展容器。

约束：

- 后端：Java 8 / Spring Boot 2.7.x（Java 8 末班车，3.x 需 17）/ MySQL 8.0
- 前端：React 18 / Vite / TypeScript / 飞书项目（Lark Project）风格
- 部署：仅本地 Docker Compose
- 不引入：LLM、Figma、钉钉、Outline、GitLab、禅道等外部集成

## Decisions

### 1. 仓库顶层布局

**方案**：`backend/` + `frontend/` + `docker-compose.yml` + 根 `README.md`，**无 monorepo 工具**。

**为什么**：v0 仅两个工件无共享代码，独立工具链各自运维即可；引入 monorepo 工具是过早抽象。

**备选**：

- pnpm workspace + Maven 模块：v0 无共享代码，工具链跨语言收益为零
- 单仓多 Maven module：Maven 模块对 frontend 不适用

### 2. 后端 Spring Boot 版本

**方案**：**Spring Boot 2.7.18**（2.7 line 末版）。

**为什么**：用户锁定 Java 8；Spring Boot 3.x 需 Java 17；2.7 line 兼容性最好且生态文档最全。

**备选**：

- Spring Boot 2.6.x / 2.5.x：补丁不再
- Quarkus / Micronaut：脱离主流，团队学习成本

### 3. 后端包结构

**方案**：根包 `com.rainier`，按 "功能 × 分层" 双维度组织：

```
com.rainier
├── RainierApplication.java
├── auth
│   ├── controller/AuthController.java
│   ├── service/AuthService.java
│   ├── domain/User.java
│   └── dto/{LoginRequest, LoginResponse, MeResponse}.java
├── health
│   └── controller/HealthController.java
├── config
│   ├── CorsConfig.java
│   ├── SecurityFilter.java       (mock JWT 校验过滤器，仅 /api/auth/me 用)
│   └── JacksonConfig.java
└── common
    └── exception/{BadRequestException, UnauthorizedException, GlobalExceptionHandler}.java
```

**为什么**：后续加 Project / Story / Task 等业务模块时只需平行新增子包；避免扁平化 controller 长成 "上帝目录"。

**备选**：纯分层（controller/service/...） — 模块多了会失控。

### 4. 鉴权方案（占位）

**方案**：自实现 `OncePerRequestFilter` 校验 `Authorization: Bearer <token>`，使用 **jjwt 0.11.x** 库 HS256 签发；secret 从 `application.yml` 读取（dev 用硬编码字符串，注 `DO NOT USE IN PROD`）；exp 24h；claims 仅 `sub=username`。**不引入 Spring Security**。

**为什么**：Spring Security 配置量与 v0 占位定位严重不匹配；自实现 ~30 行可控。

**备选**：

- Spring Security + 内存 UserDetailsService：维护成本高，与占位定位不符
- HttpSession：与前后端分离 + 多实例（未来）冲突

### 5. 前端栈与包结构

**方案**：React 18 + Vite 5 + TypeScript 5 + React Router 6 + Zustand + Axios。

```
frontend/src
├── main.tsx
├── App.tsx                       // 路由树
├── api/{client, auth}.ts         // Axios 实例 + auth 接口
├── store/auth.ts                 // Zustand store
├── pages/Login/index.tsx
├── pages/Home/index.tsx
├── components/AppLayout.tsx
├── components/ProtectedRoute.tsx
├── components/ui/{Button, Input, Card}.tsx   // 最少自研基础组件
├── styles/tokens.ts              // 飞书 design tokens
└── styles/global.css
```

**为什么**：Zustand 轻量；Axios 拦截器统一注入 Bearer；**不引入 Ant Design / Arco** —— v0 写最少自研组件，避免视觉风格被组件库反向锁定（飞书风格 ≠ Ant）。

**备选**：

- Redux Toolkit：v0 状态极少，杀鸡用牛刀
- TanStack Query：仅 /me 一个 query，引入成本 > 收益
- Ant Design / Arco：引入即与飞书风格冲突

### 6. 飞书设计风格 tokens

**方案**：自定义 `--rainier-*` CSS variables，取值参考 Lark Project 公开视觉规范，定义于 `frontend/src/styles/tokens.ts` 并注入 `:root`：

```
--rainier-color-primary: #3370FF       (Lark Project 主蓝)
--rainier-color-text-1:  #1F2329
--rainier-color-text-2:  #646A73
--rainier-color-text-3:  #8F959E
--rainier-bg-page:       #F5F6F7
--rainier-bg-card:       #FFFFFF
--rainier-radius-button: 6px
--rainier-radius-card:   8px
--rainier-shadow-card:   0 4px 16px 0 rgba(0, 0, 0, 0.08)
--rainier-font:          PingFang SC, -apple-system, "Helvetica Neue", Arial, sans-serif
```

**为什么**：tokens 集中可让后续整套 UI 切换风格成本可控；用 `--rainier-*` 命名前缀避免声称 Lark brand。

### 7. 前端鉴权处理

**方案**：登录成功 → token 写 `localStorage["rainier.token"]` + Zustand `useAuthStore`；Axios request 拦截器自动注入 `Authorization: Bearer`；response 拦截器 401 → 清 store + `navigate("/login")`。

**为什么**：v0 不引入 silent refresh / SSO；localStorage 足够；后续可平滑切到 httpOnly cookie。

**备选**：sessionStorage（刷新即丢，UX 差）。

### 8. MySQL 与持久化

**方案**：MySQL 8.0 仅作 "已部署" 演示对象，**v0 不建任何业务表**；后端不引入 JPA / MyBatis / Flyway；`spring.datasource` 仅留配置占位；`spring.sql.init.mode=never`。

**为什么**：proposal 明示 "不新增业务领域实体"；引入 ORM / 迁移会越界。

**备选**：彻底去掉 MySQL — 不符合 "Docker Compose 三服务" SC。

### 9. 测试栈

**方案**：

- 后端：JUnit 5 + AssertJ + Mockito + Spring Boot Test；**`test` profile 用 H2 内存库**替代 MySQL，使 `mvn test` 在无 docker 环境可跑。
- 前端：Vitest + React Testing Library + jsdom + MSW（mock /api）。

**为什么**：H2 让 contextLoads 在无 docker 环境也能通过；MSW 是 RTL 生态标准。

**备选**：Testcontainers — 启动慢、与 H2 等价收益但成本高。

### 10. Lint / 格式化

**方案**：

- 后端：Spotless（google-java-format AOSP）+ Checkstyle（google_checks）
- 前端：ESLint（@typescript-eslint + react + react-hooks 推荐配置）+ Prettier

**为什么**：均为社区主流；CI 可作 PR 门禁。

### 11. Docker Compose 拓扑

**方案**：3 服务（mysql / backend / frontend），自定义网络 `rainier-net`，named volume `rainier-mysql-data`；frontend 容器用 nginx 提供 `dist` 并反代 `/api → backend:8080`。

healthcheck：

- mysql：`mysqladmin ping -h localhost`
- backend：`curl -f http://localhost:8080/api/health`
- frontend：`curl -f http://localhost/`

backend `depends_on: { mysql: { condition: service_healthy } }`。

**为什么**：nginx 反代避免 dev / prod 不同源 CORS 配置差异；healthcheck 让 `docker compose up` 一次成功。

## Architecture

```
[Browser]
  │
  ├── npm run dev   ──>  [Vite Dev Server :5173]  ──(proxy /api)──┐
  │                                                                │
  └── docker compose ─>  [Frontend container :80 (nginx)] ─(proxy)─┤
                                                                   │
                                                                   ▼
                                                       ┌──────────────────────┐
                                                       │ Backend :8080        │
                                                       │  ├─ SecurityFilter   │
                                                       │  │   (mock JWT)      │
                                                       │  ├─ /api/health      │
                                                       │  └─ /api/auth/*      │
                                                       └──────────┬───────────┘
                                                                  │ jdbc (lazy)
                                                                  ▼
                                                       ┌──────────────────────┐
                                                       │ MySQL 8.0  (空库)    │
                                                       └──────────────────────┘
```

## Risks / Trade-offs

| 风险 | 缓解措施 |
|---|---|
| Java 8 + Spring Boot 2.7 已过 OSS 免费安全更新 | v0 接受；首个业务能力 STDD 变更中评估升级到 Java 17 + Spring Boot 3.x |
| 自实现 JWT 过滤器（非 Spring Security） | 显式标注 mock；secret 不入 git；引入真实 IdP 的变更中替换 |
| H2 ↔ MySQL 行为差异 | `test` profile 不依赖任何 DDL / DML，`contextLoads` 不查表 |
| nginx 反代 vs vite dev 双前端入口 | 同一 `/api` 前缀语义；dev 用 vite.config 反代，prod 用 nginx |
| 飞书 tokens 取自公开观察非授权 | tokens 用 `--rainier-*` 命名，不声称 Lark brand |
| 不引入 UI 组件库 → 后续可能重做组件 | v0 仅 Button / Input / Card 三个自研组件，重做成本可控 |
| `.stdd/standards/` 缺 `java.md` / `typescript.md` | Phase 4 BUILD 前需补两份开发规范，否则团队风格难统一 |
