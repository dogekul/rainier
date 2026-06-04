# 设计调整说明

> 原始设计基线：Phase 2 产出的 `design.md` + `specs/*/spec.md` + `test-plan.md`
> 调整来源：Phase 3-5 实现过程中的发现，聚合自 `pending-adjustments.md`

## 调整汇总

| # | 调整类型 | 涉及文档 | 严重程度 | 调整阶段 | 用户已知 |
|---|---|---|---|---|---|
| 1 | design 技术方案变更 | design.md §8 / §9、specs/backend-scaffold | Minor | Phase 4 (SLICE-B01) | 是（test-report Gate 3） |
| 2 | 流程标准缺失（兜底方案） | STDD.md / .stdd/standards/ | Minor | Phase 4 (SLICE-B01) | 是（test-report Gate 3） |
| 3 | spec 验证方式变更（工具限制） | specs/frontend-scaffold | Minor | Phase 4 (SLICE-F04) | 是（test-report Gate 3） |
| 4 | spec 验证路径变更（切片顺序） | specs/backend-scaffold | Minor | Phase 4 (SLICE-B04) | 是（test-report Gate 3） |
| 5 | design 实现增强（环境兼容性） | design.md §11 / docker-compose.yml | Minor | Phase 5 (VERIFY) | 是（test-report Gate 3） |

> 所有 5 项均为 Minor（未改变可观测行为契约或对外接口语义）；零 Major 调整。

## 调整详细说明

### 调整 1：后端不依赖任何 JDBC / DataSource

- **原始设计**：
  - design.md §8：`spring.datasource` 仅留配置占位；`spring.sql.init.mode=never`
  - design.md §9：test profile 用 H2 内存库替代 MySQL
  - specs/backend-scaffold contextLoads scenario：GIVEN `application-test.yml` 使用 H2 内存库
- **调整内容**：
  - `backend/pom.xml` 不引入 `spring-boot-starter-jdbc` / `mysql-connector-java` / `h2`
  - `backend/src/main/resources/application.yml` 不配置 `spring.datasource`
  - `backend/src/test/resources/application-test.yml` 不配置 H2
- **调整原因**：v0 后端无任何 JPA / MyBatis 调用，无表无 DDL；省去 JDBC 依赖后 Spring Boot 不会触发 `DataSourceAutoConfiguration`；contextLoads 仍可在无 docker 环境通过；不引入未使用依赖反而更纯净。
- **影响范围**：
  - TC-BES-001（contextLoads）：✅ 通过，断言"该用例 SHALL 不依赖外部 MySQL" 成立
  - TC-DRT-001/002：✅ 通过，mysql 服务仍由 docker compose 启动并 healthy，"backend 日志无 Communications link failure" 自然成立（不连就不会失败）
  - 后续首个业务能力切片需重新加入 jdbc/JPA + MySQL 驱动 + Flyway 等迁移工具
- **调整阶段**：Phase 4 (SLICE-B01)
- **用户已知**：Gate 3 等待确认

### 调整 2：标准缺失采用工具链规范兜底

- **原始要求**（STDD.md / stdd-build skill Step 0）：Phase 4 BUILD 之前必须读取 `.stdd/standards/<language>.md`
- **调整内容**：`.stdd/standards/` 仅含 `python.md`，本变更涉及的 Java 与 TypeScript 均无对应规范文件；fallback 到 design.md §10 锁定的 Spotless google-java-format AOSP + Checkstyle google_checks（Java）/ ESLint @typescript-eslint + react + react-hooks 推荐 + Prettier（TypeScript）。
- **调整原因**：流程规范文件本身不存在；用工具链强约束作为等价兜底。
- **影响范围**：不影响功能行为；风格统一由 `mvn spotless:check checkstyle:check` 与 `npm run lint` 验证（均 0 告警）。建议作为独立 STDD 变更补 `java.md` / `typescript.md` 规范文档（已记入 design.md Risks）。
- **调整阶段**：Phase 4 (SLICE-B01)
- **用户已知**：Gate 3 等待确认

### 调整 3：TC-FES-004 jsdom 不解析 CSS 变量

- **原始 Scenario**（specs/frontend-scaffold → 登录按钮使用主色）：
  - THEN `getComputedStyle` 返回 `rgb(51, 112, 255)`
- **调整内容**：
  - jsdom 25 在 `getComputedStyle` 中不解析 `var(--rainier-color-primary)`，返回字面量字符串
  - Login.test.tsx 改为断言 `style.backgroundColor === 'var(--rainier-color-primary)'`（证明按钮引用 token，未硬编码颜色）
- **调整原因**：jsdom 的 CSSOM 实现已知不展开 CSS 自定义属性引用，非 Rainier 代码问题。
- **补偿**：
  - TC-FES-003（tokens.test.tsx）已断言 `--rainier-color-primary` 解析为 `#3370FF`
  - TC-DRT-001 浏览器端 docker compose 烟测在真实浏览器中渲染按钮（已通过）
  - TC-FES-004 优先级 P2，端到端覆盖未打折
- **调整阶段**：Phase 4 (SLICE-F04)
- **用户已知**：Gate 3 等待确认

### 调整 4：CORS 测试用 /api/health 而非 /api/auth/login

- **原始 Scenario**（specs/backend-scaffold → CORS 预检请求）：
  - WHEN 浏览器发起 `OPTIONS /api/auth/login`
- **调整内容**：CorsConfigTest 改用 `OPTIONS /api/health`
- **调整原因**：Spring CORS preflight 只对至少注册了一个 handler 的路径生效；测试切片 B04 早于 B05（`/api/auth/login` 还未实现），且 CORS 配置全局覆盖 `/api/**`，任何已注册的 `/api/*` 路径都等价。
- **补偿**：CORS 配置一处，任意 `/api/**` path 均使用相同策略；docker compose 浏览器烟测会用真实 `POST /api/auth/login` 端到端验证 CORS。
- **调整阶段**：Phase 4 (SLICE-B04)
- **用户已知**：Gate 3 等待确认

### 调整 5：Docker Compose 主机端口可被环境变量覆盖

- **原始设计**（design.md §11 / 原 docker-compose.yml）：
  - backend 固定映射 `8080:8080`、frontend 固定 `80:80`
- **调整内容**：
  - `docker-compose.yml` 改为 `"${RAINIER_BACKEND_HOST_PORT:-8080}:8080"` / `"${RAINIER_FRONTEND_HOST_PORT:-80}:80"`
  - `scripts/test-compose-up.sh` 同步支持环境变量覆盖、probe URL 跟随端口变化
- **调整原因**：Phase 5 VERIFY 中发现本机 8080 端口被用户的另一项目（`prdws/backend` Spring Boot，连续运行 5+ 天）长期占用，需要支持端口覆盖以使 E2E 在共享环境下可执行。
- **补偿**：
  - 默认值与原设计一致（8080 / 80），干净环境行为不变
  - TC-DRT-001 验证：`RAINIER_BACKEND_HOST_PORT=18080 bash scripts/test-compose-up.sh` 全绿
  - TC-DRT-002 验证：backend 在 mysql healthy 后启动，日志无 `Communications link failure`
- **调整阶段**：Phase 5 (VERIFY)
- **用户已知**：Gate 3 等待确认
