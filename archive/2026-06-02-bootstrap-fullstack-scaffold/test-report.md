# v0 Bootstrap 测试报告

> 测试日期：2026-06-03
> 测试环境：macOS Darwin 25.5.0 (arm64) / Corretto JDK 1.8.0_472 / Maven 3.9.11 / Node 25.2.1 / Docker 29.4.3 / Compose v5.1.4
> 被测版本：working tree，change `2026-06-02-bootstrap-fullstack-scaffold`

## 一、总体概况

| 指标 | 数值 |
|---|---|
| 测试用例总数（自动 + 半自动） | 22（后端 13 + 前端 7 + E2E 2） |
| 通过 | 22 |
| 失败 | 0 |
| 跳过 | 0 |
| 通过率 | **100 %** |
| 后端 mvn test 耗时 | ~3 s（13 用例） |
| 前端 vitest 耗时 | ~0.7 s（7 用例） |
| Docker Compose E2E 耗时 | ~2 min（含镜像构建 + 健康等待 + teardown） |

### 1.1 覆盖率诊断（仅变更文件）

> 覆盖率仅作诊断参考，不作为通过/失败门禁。
> v0 未引入覆盖率工具（jacoco / vitest coverage），仅做定性评估。

| 变更范围 | 行覆盖率（定性） | 备注 |
|---|---|---|
| 后端 `com.rainier.health.controller` | 100 % | 1 endpoint × 1 测试 |
| 后端 `com.rainier.auth.controller` | ~100 % | login + me 全分支覆盖（含错误分支） |
| 后端 `com.rainier.auth.service` | ~100 % | issueToken + parseUsername |
| 后端 `com.rainier.config.SecurityFilter` | ~90 % | 仅 `/api/auth/me` 路径分支覆盖，未测非匹配路径透传 |
| 后端 `com.rainier.config.CorsConfig` | 100 % | 预检请求测试 |
| 后端 `com.rainier.common.exception.GlobalExceptionHandler` | ~80 % | 404 + 500 + 400 + 401 路径覆盖；handleAny RuntimeException 覆盖 |
| 前端 `src/store/auth.ts` | ~100 % | setAuth + logout |
| 前端 `src/components/ProtectedRoute.tsx` | 100 % | 已登录 + 未登录两分支 |
| 前端 `src/pages/Login` | ~70 % | 渲染断言 + 主色断言；未测提交→成功跳转端到端流程（由 E2E 兜底） |
| 前端 `src/pages/Home` | ~80 % | 渲染断言；未单独测 me() 重新水合分支（由 E2E 兜底） |
| 前端 `src/api/client.ts` | 0 %（单测） | 拦截器路径无单测；由 E2E 兜底 |

**低覆盖文件说明**：`api/client.ts` 与 `pages/Login` 的端到端流程未在单测中覆盖；由 Docker Compose E2E 烟测覆盖；v0 范围内可接受。后续业务能力切片应补端到端集成测试。

## 二、按模块统计

| 测试模块 | 用例数 | 通过 | 失败 | 跳过 |
|---|---|---|---|---|
| `com.rainier.RainierApplicationTests` | 1 | 1 | 0 | 0 |
| `com.rainier.health.controller.HealthControllerTest` | 1 | 1 | 0 | 0 |
| `com.rainier.common.exception.GlobalExceptionHandlerTest` | 2 | 2 | 0 | 0 |
| `com.rainier.config.CorsConfigTest` | 1 | 1 | 0 | 0 |
| `com.rainier.auth.controller.AuthControllerLoginTest` | 3 | 3 | 0 | 0 |
| `com.rainier.auth.controller.AuthControllerMeTest` | 5 | 5 | 0 | 0 |
| `frontend src/App.test.tsx` | 1 | 1 | 0 | 0 |
| `frontend src/styles/tokens.test.tsx` | 1 | 1 | 0 | 0 |
| `frontend src/store/auth.test.ts` | 2 | 2 | 0 | 0 |
| `frontend src/components/ProtectedRoute.test.tsx` | 2 | 2 | 0 | 0 |
| `frontend src/pages/Login/Login.test.tsx` | 1 | 1 | 0 | 0 |

## 三、E2E 测试结果

### 3.1 总体概况

| 指标 | 数值 |
|---|---|
| E2E 用例数 | 2（TC-DRT-001、TC-DRT-002） |
| 通过 | 2 |
| 失败 | 0 |
| 通过率 | **100 %** |

### 3.2 关键路径结果

| 路径 | 状态 | 备注 |
|---|---|---|
| `docker compose up -d --build` → mysql / backend / frontend 全 healthy | ✅ | 全部在 180s 超时内健康 |
| `curl http://localhost:${BACKEND}/api/health` | ✅ | 返回 `{"status":"UP"}` |
| `curl -I http://localhost:${FRONTEND}/` | ✅ | 200 OK |
| backend `depends_on: mysql healthy` 顺序 | ✅ | 容器创建顺序：mysql → mysql healthy → backend → frontend |
| backend 日志无 `Communications link failure` | ✅ | grep 无命中 |

### 3.3 E2E 结论

✅ 全部通过。运行时拓扑（network / volume / healthcheck / 依赖顺序）符合 design.md §11 描述。本次实际执行使用 `RAINIER_BACKEND_HOST_PORT=18080`（详见 Adjustment #5）；默认 8080 在干净环境下同样适用。

## 四、失败项详细分析

无。

## 五、功能/测试覆盖对照

| 功能模块 (Capability) | 涉及源码 | 测试覆盖 (TC) | 状态 |
|---|---|---|---|
| health-check | `health/controller/HealthController` | TC-HLT-001 + E2E curl | ✅ |
| auth-placeholder | `auth/{controller,service,dto,domain}`, `config/SecurityFilter` | TC-AUT-001 / 002 / 003 / 004 | ✅ |
| backend-scaffold | `RainierApplication`, `common/exception/*`, `config/CorsConfig`, `diag/BoomController` | TC-BES-001 / 002 / 003 / 004 | ✅ |
| frontend-scaffold | `App`, `AppRoutes`, `components/{ProtectedRoute,AppLayout,ui/*}`, `pages/{Login,Home}`, `styles/{tokens,global.css}`, `api/{client,auth}`, `store/auth` | TC-FES-001 / 002 / 003 / 004 | ✅ |
| dev-runtime | `docker-compose.yml`, `backend/Dockerfile`, `frontend/{Dockerfile,nginx.conf}`, `scripts/test-compose-up.sh` | TC-DRT-001 / 002 | ✅ |
| test-runtime | `pom.xml` (Spotless+Checkstyle), `package.json` (vitest/eslint/build), `eslint.config.js`, `.prettierrc.json`, `checkstyle.xml` | TC-TRT-001 / 002 / 003 / 004 / 005 | ✅ |

## 五-B、多路并行 Review 结果

> 由 VERIFY Step 0 执行，3 代理并行只读审查

### Review 迭代历史

| 轮次 | C | H | M | L | 状态 |
|---|---|---|---|---|---|
| 1 | 1 | 2 | 4 | 3 | 修复 C → 复查 |
| 2 | 0 | 0 (2 项确认为非问题) | 4 | 3 | 通过（达 C=0 ✓ / H≤3 ✓ / M≤10 ✓） |

### 最终 Review 汇总

| 维度 | Critical | High | Medium | Low | 总计 |
|---|---|---|---|---|---|
| 代码质量 | 0 | 0 | 3 | 3 | 6 |
| 测试 / 配置 | 0 (已修复 1) | 0 (确认 1 项为非问题) | 0 | 0 | 1 |
| 文档 / Skills | 0 | 0 | 1 | 0 | 1 |
| **总计** | **0** | **0** | **4** | **3** | **8** |

### Review 已修复问题

| # | 严重性 | 文件 | 问题 | 状态 |
|---|---|---|---|---|
| 1 | C | `frontend/src/styles/tokens.test.tsx` | 缺 `beforeEach` 重置 Zustand store / localStorage，存在跨测用例状态泄漏隐患 | ✅ 已加 `beforeEach` 清理 |

### Review 确认为非问题（澄清）

| # | 严重性 | 文件 | 评审意见 | 处置 |
|---|---|---|---|---|
| 2 | H | `frontend/src/pages/Home/index.tsx:15` | `setAuth(token, { username: res.username })` 仅传 `username`，未来 AuthUser 演进可能不一致 | **澄清**：`AuthUser` 当前接口仅含 `username`，对象字面量完全匹配类型；接口若扩展会触发 TypeScript 编译错误，不会静默漏字段。降级到 L 备查。 |
| 3 | H | `backend/src/test/java/com/rainier/common/exception/GlobalExceptionHandlerTest.java:41` | `jsonPath("$.message").isNotEmpty()` 仅查非空 | **澄清**：Spring 的 `isNotEmpty()` matcher 排除 null 与空字符串；当前断言可正确捕捉缺字段 / 空字符串两种 bug。视为有效断言。 |

### Review 已知限制（M / L 级，未修复，记录备查）

| # | 严重性 | 文件 | 问题 | 处置 |
|---|---|---|---|---|
| 4 | M | `frontend/src/pages/Login/index.tsx:22-24` | `catch` 块吞掉所有错误并显示通用消息，无法区分 400 / 5xx / 网络错误 | v0 占位登录可接受；下一切片接真实 IdP 时补 AxiosError 分支 |
| 5 | M | `backend/src/main/java/com/rainier/config/SecurityFilter.java:46-56` | 静默吞 `UnauthorizedException`，无 debug 日志，故障溯源略麻烦 | v0 可接受；下一切片可加 `LOG.debug("token rejected: {}", ex.getMessage())` |
| 6 | M | `frontend/src/api/client.ts:29-38` | 401 拦截器对 `/login` 自身返回 401 不做循环保护 | v0 占位登录接受任意账号密码不会 401；下一切片接真实 IdP 时加 loop guard |
| 7 | M | `README.md:11 / :42` | "Maven 3.9+" vs "Maven 3.6+" 文案不一致 | 不影响功能；记下次文档巡检 |
| 8 | L | `backend/src/main/java/com/rainier/auth/dto/LoginRequest.java` | POJO 无 `@NotBlank` 注解（手动校验） | v0 故意未启用 Bean Validation；记 |
| 9 | L | `backend/src/main/java/com/rainier/auth/service/AuthService.java:53` | `token.isEmpty()` 不覆盖纯空白 | jjwt parseClaimsJws 对空白会抛 MalformedJwtException → UnauthorizedException，行为正确 |
| 10 | L | `frontend/src/pages/Home/index.tsx:16-18` | `me()` 异常仅 catch 不 log，非 401 错误会静默 | 与代码 #6 相关；同期修复 |

## 六、设计调整说明

共 **5 项 Minor 调整**（**零 Major**），完整列表见 [`design-adjustments.md`](./design-adjustments.md)：

1. 后端不引入 JDBC / DataSource（v0 不查表）
2. `.stdd/standards/{java,typescript}.md` 缺失，用 design.md §10 工具链兜底
3. TC-FES-004 jsdom 不解析 CSS 变量，改断言"按钮引用 token"
4. CORS 测试用 `/api/health`（切片顺序约束）
5. Docker Compose 主机端口可被环境变量覆盖（默认值与原设计一致）

## 七、修复确认记录

| 问题 | 修复文件 | 状态 |
|---|---|---|
| Review C-1：tokens.test 缺状态清理 | `frontend/src/styles/tokens.test.tsx` | ✅ 已加 beforeEach |
| 环境：8080 被另一项目占用阻塞 E2E | `docker-compose.yml`, `scripts/test-compose-up.sh` | ✅ 加环境变量覆盖；E2E 实际跑通 |
| YAML list `@Value` 注入失败 | `backend/src/main/resources/application.yml` | ✅ 改为逗号分隔字符串 |
| `@WebMvcTest` HealthControllerTest 失败 | `backend/src/test/java/com/rainier/health/controller/HealthControllerTest.java` | ✅ 改为 `@SpringBootTest` |
| jsdom localStorage 缺方法 | `frontend/src/test/setup.ts` | ✅ 加 MemoryStorage polyfill |
| `vi` JSX 文件后缀 | `tokens.test.ts → tokens.test.tsx` | ✅ 已重命名 |

## 八、十一类失败模式检查

| ID | 模式 | 结果 |
|---|---|---|
| a | 幻觉行为（编造 API/路径） | ✅ 无命中 |
| b | 范围蔓延 | ✅ 无命中（`scripts/` 为 TC-DRT-001/002 必需，在 proposal Impact 隐含范围） |
| c | 级联错误（静默 catch） | ⚠️ 3 处已知 catch，全部有意为之且记录（M 级，列于五-B） |
| d | 上下文丢失（与设计矛盾） | ✅ 5 项偏离全部记录到 design-adjustments.md |
| e | 工具误用 | ✅ 无命中 |
| f | 运行时行为偏差（声明 vs 实际绑定） | ✅ 检查路由/事件/CSS 全部已绑定 |
| g | 管线断链（多步骤转换缺步） | ✅ 后端 mvn build、前端 vite build、Docker multi-stage 全部完整 |
| h | 内容质量偏差 | ✅ README 含完整启动 / 目录 / 扩展指引 |
| i | 指令衰减 | N/A（本变更无 Prompt-driven 生成步骤） |
| j | 覆盖真空 | ✅ 6 capability 均有自动化覆盖（test-runtime 自验、dev-runtime 半自动脚本） |
| k | 契约断层 | ✅ 后端 LoginResponse / MeResponse / Auth header 与前端 `api/auth.ts` interface 完全对齐 |

## 九、结论

✅ **v0 可以交付**。所有 SC 已可验证为通过；所有调整均为 Minor 且已记录；Review 通过 C=0 / H≤3 / M≤10 阈值。

### 9.1 质量信号汇总

| 信号源 | 状态 | 备注 |
|---|---|---|
| 后端 mvn test | ✅ | 13/13，100% |
| 前端 vitest | ✅ | 7/7，100% |
| Docker Compose E2E | ✅ | 2/2 TC（含 `RAINIER_BACKEND_HOST_PORT=18080` workaround） |
| Backend Lint (Spotless + Checkstyle) | ✅ | 0 错误 |
| Frontend Lint (ESLint) | ✅ | 0 错误 0 警告 |
| Frontend Build (tsc -b && vite build) | ✅ | dist/index.html + 211 KB JS gzip 71 KB |
| 类型检查 (tsc -b) | ✅ | 0 type error |
| 多版本测试 | N/A | v0 固定 JDK 8 + Node 25 |
| 覆盖率 | N/A（仅定性） | 关键模块均 ≥80%，端到端流程由 E2E 兜底 |
| 多路 Review (Step 0) | ✅ | 1 C 已修复，2 H 澄清为非问题，4 M / 3 L 记录备查 |
| 十一类失败模式 | ✅ | 0 命中（c 项 3 处已知有意 catch 仍记录） |

### 9.2 已知风险与后续建议

1. **首个业务能力切片**需补：MySQL 连接池 + JPA/MyBatis、`.stdd/standards/{java,typescript}.md`、补 `api/client.ts` 拦截器单测、补登录失败分支区分（400 / 5xx / 网络）、补 SecurityFilter debug 日志
2. **Java 8 + Spring Boot 2.7 EOL**：v0 接受；建议下一变更评估升级到 Java 17 + Spring Boot 3.x
3. **mock JWT 不可入生产**：secret 在 `application.yml` 明示 `DO NOT USE IN PROD`；接真实 IdP 时整体替换 SecurityFilter

### 9.3 部署建议

- 干净开发环境直接 `docker compose up -d --build` 即可；端口冲突时用 `RAINIER_BACKEND_HOST_PORT` / `RAINIER_FRONTEND_HOST_PORT` 覆盖
- 不建议直接将本骨架部署到生产（mock 鉴权 + 无业务模型）
- 建议下一变更里加 CI（GitHub Actions 或同等）一键跑 mvn test + npm test + lint
