# Pending Design Adjustments (Phase 4 BUILD)

> 长程模式下，实现过程中发现的小/大设计偏离自动记录于此；Phase 5 VERIFY 时汇总到 `design-adjustments.md`。

---

## Adjustment #1 — 后端不依赖任何 JDBC / DataSource（涉及 design.md §8、§9，spec backend-scaffold）

**原始设计**：
- design.md §8：`spring.datasource` 仅留配置占位；`spring.sql.init.mode=never`
- design.md §9：test profile 用 H2 内存库替代 MySQL
- specs/backend-scaffold/spec.md → contextLoads scenario：GIVEN 已正确配置 `application-test.yml` 使用 H2 内存库

**实际实现（SLICE-B01）**：
- pom.xml 不引入 `spring-boot-starter-jdbc` / `mysql-connector-java` / `h2`
- `application.yml` 不配置 `spring.datasource`
- `application-test.yml` 不配置 H2

**原因**：v0 后端无任何 JPA / MyBatis 调用，无表无 DDL；省去 JDBC 依赖后 Spring Boot 不会触发 `DataSourceAutoConfiguration`，contextLoads 仍可在无 docker 环境通过；空 DataSource 占位会被读取但 Spring Boot 不会主动建立连接 → 在我们的场景里不引入也没有副作用，反而省一份不可见依赖。

**影响范围**：
- 不影响任何 TC 的可验证结果（TC-BES-001 仍能在无 docker 环境跑过）
- TC-DRT-001 中 MySQL service 仍由 docker compose 启动并 healthy；backend 不连接它，但这与 spec scenario "AND backend 启动日志 SHALL 不包含 `Communications link failure`" 一致（不连就不会失败）
- 后续首个业务能力切片（引入 User/Project 等实体）需重新加入 jdbc/JPA + MySQL 驱动 + 真实迁移工具

**分类**：小偏离（不改变接口与可观测行为，只移除一份未使用的依赖）

---

## Adjustment #3 — TC-FES-004 jsdom 不解析 CSS 变量

**原始 Scenario**（specs/frontend-scaffold/spec.md → 登录按钮使用主色）：
- THEN getComputedStyle 返回 `rgb(51, 112, 255)`

**实际实现（SLICE-F04 Login.test.tsx）**：
- jsdom 25 在 getComputedStyle 中不解析 `var(--rainier-color-primary)`，返回字面量字符串
- 测试改为断言 `style.backgroundColor === 'var(--rainier-color-primary)'`（证明按钮引用 token，未硬编码颜色）

**原因**：jsdom 的 CSSOM 实现不展开 CSS 自定义属性引用；这是 jsdom 已知限制，非 Rainier 代码问题。

**补偿**：
- TC-FES-003（tokens.test.tsx）已断言 `--rainier-color-primary` 解析为 `#3370FF`
- TC-DRT-001 浏览器端 docker compose 烟测会在真实浏览器中渲染按钮，可视觉验证最终颜色
- TC-FES-004 优先级 P2，端到端覆盖不打折

**分类**：小偏离（验证方式改变，行为契约未变）

---

## Adjustment #5 — Docker Compose 主机端口可被环境变量覆盖

**原始设计**（design.md §11 / docker-compose.yml）：
- backend 固定映射 `8080:8080`、frontend 固定 `80:80`

**实际实现（Phase 5 VERIFY 中追加）**：
- backend 端口改为 `"${RAINIER_BACKEND_HOST_PORT:-8080}:8080"`
- frontend 端口改为 `"${RAINIER_FRONTEND_HOST_PORT:-80}:80"`
- 默认值与原设计一致；脚本 `scripts/test-compose-up.sh` 同步支持环境变量覆盖

**原因**：在本机 Phase 5 验证时发现 8080 端口被用户另一个项目（`prdws/backend`）长期占用（持续 5 天+，非本变更可解决）。需要支持端口覆盖以使 E2E 在共享环境下可执行。默认值不变，对干净环境无影响。

**影响范围**：
- 不影响任何 TC 的 spec 文本（spec 仍以 8080 / 80 为默认）
- 增强了本地开发的环境兼容性
- TC-DRT-001 验证：`RAINIER_BACKEND_HOST_PORT=18080 bash scripts/test-compose-up.sh` 全绿
- TC-DRT-002 验证：backend 在 mysql healthy 后启动，日志无 `Communications link failure`

**分类**：小偏离（向后兼容的灵活性增强，默认行为不变）

---

## Adjustment #4 — CORS 测试改用 /api/health 而非 /api/auth/login

**原始 Scenario**（specs/backend-scaffold/spec.md → CORS 预检请求）：
- WHEN 浏览器发起 `OPTIONS /api/auth/login`

**实际实现（SLICE-B04 CorsConfigTest）**：
- 改用 `OPTIONS /api/health`

**原因**：Spring CORS preflight 只对至少注册了一个 handler 的路径生效；测试切片 B04 早于 B05（/api/auth/login 还未实现），此外 CORS 配置是全局的（覆盖 `/api/**`），任何已注册的 `/api/*` 路径都能等价验证。

**补偿**：CORS 配置一处，任意 `/api/**` path 均使用相同策略；docker compose 浏览器烟测会用真实 POST `/api/auth/login` 端到端验证。

**分类**：小偏离（验证路径变更，验证语义不变）

---

## Adjustment #2 — 标准缺失：`.stdd/standards/{java,typescript}.md` 不存在

**原始要求**（STDD.md / stdd-build skill Step 0）：Phase 4 BUILD 之前必须读取语言开发规范。

**实际情况**：`.stdd/standards/` 只含 `python.md`，本变更涉及 Java 与 TypeScript 均无对应规范文件。

**Workaround**：
- Java：采用 design.md §10 锁定的 Spotless google-java-format AOSP + Checkstyle google_checks 作为风格规范；命名约定按 Google Java Style Guide。
- TypeScript：采用 design.md §10 锁定的 ESLint（@typescript-eslint + react + react-hooks 推荐配置）+ Prettier 作为风格规范；命名约定按 TS 社区默认（PascalCase 类/类型、camelCase 函数/变量、UPPER_SNAKE 常量）。

**影响范围**：
- 不影响功能行为，仅影响风格统一
- 建议作为独立 STDD 变更补 `java.md` / `typescript.md` 两份规范（已在 design.md Risks 表中提示）

**分类**：小偏离（流程规范本身缺位，用工具链兜底）

---
