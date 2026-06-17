# v0.0.38-real-auth — 测试报告 (Phase 5 VERIFY)

> Baseline: tag `v0.0.37-all-pages-polish` / commit 626275b
> Closes CRITICAL: mock login = 自证身份 / 任意冒充 (架空 v0.0.21/24 全部授权)

## 1. 总体概况

| 维度 | 结果 |
|------|------|
| 后端单元/集成 | **421 / 421** (Failures 0, Errors 0, Skipped 0) ✅ |
| 新增测试 | RealAuthLoginTest **6/6** (TC-AUTH-REAL-001..006) |
| 既有 login 测试 (flag off) | AuthControllerLoginTest **3/3** 不变 ✅ |
| 认证基线/授权回归 | AuthBaselineTest 7/7、AdminAuthorizationTest 14/14、AdminAuthzBootstrapTest 3/3 ✅ |
| 前端 | 无改动 (本版纯后端) — 登录契约 `{token, user:{username}}` 未变 |
| E2E (Docker, 真 MySQL, real-auth=true) | 6/6 关键路径通过 ✅ |
| Lint/编译 | Docker `maven:3.9-eclipse-temurin-8` (真 JDK 8) 编译通过 ✅ |

## 2. 新增测试 (RealAuthLoginTest, @TestPropertySource real-auth=true)

| TC | 场景 | 预期 | 结果 |
|----|------|------|------|
| TC-AUTH-REAL-001 | 正确密码 | 200 + token + user.username | ✅ |
| TC-AUTH-REAL-002 | 错误密码 | 401 | ✅ |
| TC-AUTH-REAL-003 | 未知用户 | 401 (与错密码同消息, 不泄露登录名是否存在) | ✅ |
| TC-AUTH-REAL-004 | 禁用用户 (enabled=false) + 正确密码 | 401 | ✅ |
| TC-AUTH-REAL-005 | 缺密码 | 400 (校验先于鉴权) | ✅ |
| TC-AUTH-REAL-006 | 回填无密码用户 → 默认密码可登 | hash 非空 + matches(rainier123) + 200 | ✅ |

## 3. E2E (live stack — `docker compose`, real MySQL, profile dev, real-auth=true)

| # | 验证 | 命令 | 结果 |
|---|------|------|------|
| 1 | alice + 错误密码 → 401 | `POST /api/auth/login` | **HTTP 401** ✅ (冒充洞已封) |
| 2 | alice + rainier123 → 200 | 同上 | **HTTP 200** + token ✅ |
| 3 | 未知用户 ghost → 401 | 同上 | **HTTP 401** ✅ |
| 4 | 缺密码 → 400 | 同上 | **HTTP 400** ✅ |
| 5 | 回填: 7 用户全部有 BCrypt hash | SQL count | 7/7 有 hash, 0 无 hash; alice = `$2a$10$…` ✅ |
| 6 | **存量业务数据未改** (standing 约束) | SQL count | projects 5 / requirements 10 / audit_logs 97 — 全保留 ✅ |

> standing 约束「测试和修复不删改已有业务数据」: 回填**只新增** password_hash 列值, 未删/改任何业务行。

## 4. 多路评审 (Step 0) 与 11 类失败模式

**对抗性评审结论**: 校验逻辑稳固 — 无绕过路径; 未知用户/错密码/禁用/无 hash 四态均等价 401; 响应不泄露 hash; 回填幂等 (仅对 null/空 hash 生效)。

发现项 (已记录, prod 加固为后续):
- **C1** 共享默认密码 `rainier123` + 无改密流程 → 作为 demo/prod 默认随版本发布。**缓解**: `SecurityPostureWarning` 启动 loud WARN。
- **H1** dev JWT 签名密钥可被持仓库者伪造 token。**缓解**: 同上 WARN (含 `changeme`/`dev-only` 检测)。
- **H2** real-auth flag「静默 fail-open」(关闭即任意密码) → **已处理**: `SecurityPostureWarning` 非 test profile 下 real-auth=false 时 loud WARN (不 fail-fast, 保 demo 可启)。
- **M1** login_name TOCTOU 竞态 (软删模型下放弃 DB unique, 仅 app 层 `existsByLoginName`) — 后续 partial-unique index。
- **L3** 未知用户跳过 BCrypt → 计时旁路 — 后续 dummy-hash 恒定时。

11 类失败模式: 无幻觉 (API/类均真实); 范围 — `SecurityPostureWarning` 超出最小实现但属安全范围内、直接处理 H2; 契约 (k) — `LoginResponse` 形状未变, 前端零改; **Java 8 陷阱已命中并修复** (见 §5)。

## 5. 设计调整 / 修复记录

- **D4 调整**: 原计划 `login_name` DB unique 约束 → **放弃**。`@SQLDelete` 软删模型下普通 unique 索引仍见 del_flag=1 行, 会阻止「软删后重建同名登录」且破坏测试 (deleteAll 仅置 del_flag=1 → 重建 alice 撞索引)。改为 app 层唯一 (`existsByLoginName`, 尊重 `@Where del_flag=0`), 经 `@Where` 只返回 active 用户 → `findByLoginName` 无歧义。
- **Java 8 编译修复**: `RealAuthLoginTest:124` 原用无参 `Optional.orElseThrow()` (Java 10+)，本地 JDK 25 通过但 Docker `temurin-8` 真 JDK 8 拒绝 → 改 `orElseThrow(() -> new AssertionError(...))` (Java 8 Supplier 形式)。**根因**: 本地 mvn 用 JDK 25 + source/target 1.8 (非 `--release 8`)，放行 Java 10+ API；Docker JDK-8 构建是真正的 Java-8 门, 正确拦截。

## 6. 结论

| 信号 | 状态 |
|------|------|
| 后端全量 421/421 | ✅ |
| flag-off 既有行为不变 | ✅ |
| flag-on 真实校验 (错密码 401) | ✅ E2E + 单测 |
| 回填幂等 + 存量数据零改 | ✅ |
| Docker 真 JDK-8 构建 | ✅ |
| 头号安全洞 (mock 冒充) | **已关闭** |

**部署建议**: 可交付。prod 上线前需 override `app.security.jwt.secret` (256-bit) + `app.security.default-password` + 补改密流程 (C1/H1, 启动 WARN 已提示, 列为 v0.0.39 候选)。
