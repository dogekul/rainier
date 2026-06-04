# v0 Bootstrap 测试方案与详细案例

> 版本：v0.1
> 创建日期：2026-06-02
> 对应 Phase 2 Spec：`specs/{health-check, auth-placeholder, backend-scaffold, frontend-scaffold, dev-runtime, test-runtime}/spec.md`

## 一、测试策略

### 1.1 测试金字塔

- **单元（70%）**：后端 Controller / Service WebMvcTest 与纯函数；前端 RTL + Vitest 组件单测
- **集成（25%）**：后端 `@SpringBootTest` 启动完整 context + MockMvc；前端 MSW 拦截 `/api`
- **E2E / 系统（5%）**：`docker compose up` 后用 `curl` 探活；浏览器手工烟测

### 1.2 测试原则

- 测试行为而非实现（断言响应、断言渲染，不断言内部状态）
- 后端 `test` profile 用 H2，不依赖本地 docker
- 飞书风格 tokens 通过 computed style 断言，不耦合 className

### 1.3 已有测试资产

| 测试文件 | 用例数 | 类型 | 覆盖范围 |
|---|---|---|---|
| (无) | — | — | 项目无既有代码 |

## 二、详细测试案例

### 功能 1：health-check

对应 Requirement：后端健康检查接口

#### 案例 1.1 — 健康探针返回 UP

| 字段 | 内容 |
|---|---|
| **ID** | TC-HLT-001 |
| **对应 Spec** | `health-check/spec.md` → Scenario: 服务正常运行时返回 UP |
| **优先级** | P0 |
| **预置条件** | Spring Boot 后端已启动 |
| **输入** | `GET /api/health` |
| **预期结果** | HTTP 200；Content-Type `application/json`；body 含 `"status":"UP"` |
| **当前状态** | ❌ 测试缺 |

### 功能 2：auth-placeholder

对应 Requirement：用户登录（mock JWT）+ 凭 token 查询当前用户

#### 案例 2.1 — 登录成功返回 JWT

| 字段 | 内容 |
|---|---|
| **ID** | TC-AUT-001 |
| **对应 Spec** | `auth-placeholder/spec.md` → Scenario: 凭证非空时登录成功 |
| **优先级** | P0 |
| **预置条件** | 后端运行 |
| **输入** | `POST /api/auth/login` body `{"username":"alice","password":"any"}` |
| **预期结果** | HTTP 200；body.token 为 HS256 JWT 三段；body.user.username = `"alice"`；JWT sub=`"alice"`；exp ≈ now+24h |
| **当前状态** | ❌ 测试缺 |

#### 案例 2.2 — 缺字段登录被拒

| 字段 | 内容 |
|---|---|
| **ID** | TC-AUT-002 |
| **对应 Spec** | `auth-placeholder/spec.md` → Scenario: 缺少必填字段时拒绝 |
| **优先级** | P0 |
| **预置条件** | 后端运行 |
| **输入** | `POST /api/auth/login` body `{"password":"x"}` 或 `username` 为空字符串 |
| **预期结果** | HTTP 400；JSON；body.message 非空 |
| **当前状态** | ❌ 测试缺 |

#### 案例 2.3 — /me 查询当前用户

| 字段 | 内容 |
|---|---|
| **ID** | TC-AUT-003 |
| **对应 Spec** | `auth-placeholder/spec.md` → Scenario: 携带有效 token 查询成功 |
| **优先级** | P0 |
| **预置条件** | 已通过 TC-AUT-001 取得 token `T`（username=alice） |
| **输入** | `GET /api/auth/me` Header `Authorization: Bearer T` |
| **预期结果** | HTTP 200；body.username = `"alice"` |
| **当前状态** | ❌ 测试缺 |

#### 案例 2.4 — 无效 token 拒绝

| 字段 | 内容 |
|---|---|
| **ID** | TC-AUT-004 |
| **对应 Spec** | `auth-placeholder/spec.md` → Scenario: 缺失或非法 token 时拒绝 |
| **优先级** | P0 |
| **预置条件** | 后端运行 |
| **输入** | `GET /api/auth/me` 无 Authorization / Bearer invalid / Bearer expired |
| **预期结果** | HTTP 401；JSON；body.message 非空；无 stack trace |
| **当前状态** | ❌ 测试缺 |

### 功能 3：backend-scaffold

#### 案例 3.1 — contextLoads

| 字段 | 内容 |
|---|---|
| **ID** | TC-BES-001 |
| **对应 Spec** | `backend-scaffold/spec.md` → Scenario: contextLoads 用例通过 |
| **优先级** | P0 |
| **预置条件** | `application-test.yml` 配 H2 |
| **输入** | `mvn -ntp test` |
| **预期结果** | `contextLoads` 用例通过；不依赖 MySQL |
| **当前状态** | ❌ 测试缺 |

#### 案例 3.2 — 未知 endpoint JSON 404

| 字段 | 内容 |
|---|---|
| **ID** | TC-BES-002 |
| **对应 Spec** | `backend-scaffold/spec.md` → Scenario: 访问未知 endpoint |
| **优先级** | P1 |
| **预置条件** | 后端运行 |
| **输入** | `GET /api/this-does-not-exist` |
| **预期结果** | HTTP 404；Content-Type `application/json`；body.message 非空 |
| **当前状态** | ❌ 测试缺 |

#### 案例 3.3 — 异常 → JSON 500

| 字段 | 内容 |
|---|---|
| **ID** | TC-BES-003 |
| **对应 Spec** | `backend-scaffold/spec.md` → Scenario: 控制器抛出 RuntimeException |
| **优先级** | P1 |
| **预置条件** | `test` profile 注册 `GET /api/_diag/boom` |
| **输入** | `GET /api/_diag/boom` |
| **预期结果** | HTTP 500；`application/json`；body.message 非空；无 stack trace |
| **当前状态** | ❌ 测试缺 |

#### 案例 3.4 — CORS 预检

| 字段 | 内容 |
|---|---|
| **ID** | TC-BES-004 |
| **对应 Spec** | `backend-scaffold/spec.md` → Scenario: CORS 预检请求 |
| **优先级** | P1 |
| **预置条件** | 后端运行 |
| **输入** | `OPTIONS /api/auth/login` 含 `Origin: http://localhost:5173`、`Access-Control-Request-Method: POST` |
| **预期结果** | HTTP 204 或 200；`Access-Control-Allow-Origin = http://localhost:5173`；`Access-Control-Allow-Methods` 含 `POST`；`Access-Control-Allow-Headers` 含 `Authorization` |
| **当前状态** | ❌ 测试缺 |

### 功能 4：frontend-scaffold

#### 案例 4.1 — 路由守卫（未登录重定向）

| 字段 | 内容 |
|---|---|
| **ID** | TC-FES-001 |
| **对应 Spec** | `frontend-scaffold/spec.md` → Scenario: 未登录访问首页时重定向 |
| **优先级** | P0 |
| **预置条件** | RTL render App with MemoryRouter `initialEntries=['/']`；localStorage 空 |
| **输入** | (mount) |
| **预期结果** | 渲染 Login 页；`location.pathname === '/login'` |
| **当前状态** | ❌ 测试缺 |

#### 案例 4.2 — 路由守卫（已登录通过）

| 字段 | 内容 |
|---|---|
| **ID** | TC-FES-002 |
| **对应 Spec** | `frontend-scaffold/spec.md` → Scenario: 登录后访问受保护路由通过 |
| **优先级** | P0 |
| **预置条件** | localStorage 写入有效 token；Zustand store 设 `user.username = 'alice'` |
| **输入** | RTL render App with MemoryRouter `['/']` |
| **预期结果** | 渲染 Home；右上角 text 包含 `alice`；主区域文本 `Hello, alice` |
| **当前状态** | ❌ 测试缺 |

#### 案例 4.3 — 主题 tokens 注入

| 字段 | 内容 |
|---|---|
| **ID** | TC-FES-003 |
| **对应 Spec** | `frontend-scaffold/spec.md` → Scenario: 主题 tokens 在 DOM 上可见 |
| **优先级** | P1 |
| **预置条件** | App 挂载到 `#root` |
| **输入** | `getComputedStyle(documentElement).getPropertyValue('--rainier-color-primary')` 等 |
| **预期结果** | `--rainier-color-primary = #3370FF`；`--rainier-radius-button = 6px`；`--rainier-radius-card = 8px` |
| **当前状态** | ❌ 测试缺 |

#### 案例 4.4 — 登录按钮主色

| 字段 | 内容 |
|---|---|
| **ID** | TC-FES-004 |
| **对应 Spec** | `frontend-scaffold/spec.md` → Scenario: 登录按钮使用主色 |
| **优先级** | P2 |
| **预置条件** | 渲染 `/login` |
| **输入** | `getComputedStyle(loginButton).backgroundColor` |
| **预期结果** | `rgb(51, 112, 255)` |
| **当前状态** | ❌ 测试缺 |

### 功能 5：dev-runtime

#### 案例 5.1 — Compose 一键起全部 healthy

| 字段 | 内容 |
|---|---|
| **ID** | TC-DRT-001 |
| **对应 Spec** | `dev-runtime/spec.md` → Scenario: 干净环境一次启动成功 |
| **优先级** | P0 |
| **预置条件** | 干净 docker；checkout 本变更 commit |
| **输入** | `docker compose up -d --build` |
| **预期结果** | 3 service 在 180s 内全 `healthy`；`curl /api/health` 200；`curl -I /` 200 |
| **当前状态** | ❌ 测试缺（半自动：bash 脚本 + 轮询） |

#### 案例 5.2 — depends_on healthy 顺序

| 字段 | 内容 |
|---|---|
| **ID** | TC-DRT-002 |
| **对应 Spec** | `dev-runtime/spec.md` → Scenario: backend 等待 mysql healthy 才启动 |
| **优先级** | P1 |
| **预置条件** | compose `depends_on.condition=service_healthy` |
| **输入** | `docker compose up -d` |
| **预期结果** | backend 容器 created 时间 > mysql healthy 时间；backend 日志无 `Communications link failure` |
| **当前状态** | ❌ 测试缺（半自动） |

### 功能 6：test-runtime

| ID | 对应 Spec Scenario | P | 输入 | 预期 |
|---|---|---|---|---|
| TC-TRT-001 | mvn test 全绿 | P0 | `mvn -ntp test` in `backend/` | exit 0；≥1 通过；失败=0 |
| TC-TRT-002 | 后端 lint 零告警 | P1 | `mvn -ntp spotless:check checkstyle:check` | exit 0；无 `BUILD FAILURE` |
| TC-TRT-003 | npm test 全绿 | P0 | `npm test -- --run` in `frontend/` | exit 0；≥1 通过 |
| TC-TRT-004 | 生产构建无 type error | P0 | `npm run build` | exit 0；无 `error TS`；`dist/index.html` 存在 |
| TC-TRT-005 | 前端 lint 零告警 | P1 | `npm run lint` | exit 0；`0 errors, 0 warnings` |

## 三、测试执行矩阵

| 功能模块 | 单元 | 集成 | E2E / 手工 | 状态 |
|---|---|---|---|---|
| health-check | — | TC-HLT-001 (MockMvc) | TC-DRT-001 中 curl | 🔴 待补 |
| auth-placeholder | TC-AUT-001 / 002 / 003 / 004 (MockMvc) | — | TC-DRT-001 浏览器登录烟测 | 🔴 待补 |
| backend-scaffold | TC-BES-001 / 002 / 003 / 004 | — | — | 🔴 待补 |
| frontend-scaffold | TC-FES-001 / 002 / 003 / 004 (RTL + jsdom) | — | TC-DRT-001 浏览器烟测 | 🔴 待补 |
| dev-runtime | — | — | TC-DRT-001 / 002 (半自动) | 🔴 待补 |
| test-runtime | — | TC-TRT-001..005 (CI 命令) | — | 🔴 待补 |

## 四、回归风险矩阵

| 风险区域 | v0 改动 | 已有回归保护 | 风险等级 |
|---|---|---|---|
| 后端 contextLoads 启动 | 全新 | 无 | 🟡 中（SB 2.7.18 配置错可炸） |
| mock JWT 签发与校验 | 全新 | 无 | 🟡 中（secret 误改会全部失效） |
| CORS 配置 | 全新 | 无 | 🟢 低（仅 5173 / 80 白名单） |
| 全局异常 → JSON 序列化 | 全新 | 无 | 🟢 低 |
| 前端路由守卫 | 全新 | 无 | 🟡 中（守卫缺失会泄露受保护页） |
| Vite + nginx 双反代一致性 | 全新 | 无 | 🟡 中（路径分歧会现 "dev 通 prod 挂"） |
| Docker Compose 依赖顺序 | 全新 | 无 | 🟢 低 |

## 五、建议补充顺序

1. **第一优先 P0（v0 上线必补）**：
   TC-HLT-001、TC-AUT-001 / 002 / 003 / 004、TC-BES-001、TC-FES-001 / 002、TC-DRT-001、TC-TRT-001 / 003 / 004
2. **第二优先 P1（提交前补齐）**：
   TC-BES-002 / 003 / 004、TC-FES-003、TC-DRT-002、TC-TRT-002 / 005
3. **第三优先 P2（可灰度补）**：
   TC-FES-004
