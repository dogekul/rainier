# Java 开发规范

> 适用版本：Java 8（兼容 11/17 时本规范仍适用，新语言特性按需评估再加节）
> 适用框架：Spring Boot 2.7.x（3.x 升级时同步评估）
> 最后更新：2026-06-04
> 参考实现：[`backend/`](../../backend/)（Rainier v0 bootstrap）

## 一、代码风格

### 1.1 格式化

- **格式化器**：Spotless + Google Java Format（**GOOGLE** style，2 空格缩进）
- **静态检查**：自定义 Checkstyle，最小卫生规则（无 tab、文件结尾换行、行长度 ≤ 120、禁星号 import、禁未用 import）
- **命令**：
  - 自动修复：`mvn -ntp spotless:apply`
  - 校验：`mvn -ntp spotless:check checkstyle:check`
- **行宽**：120 字符（含注释、Javadoc）
- **缩进**：2 空格（禁止 Tab）
- **文件编码**：UTF-8 / LF（统一由根 `.editorconfig` 约束）
- **文件末尾**：必须以 LF 结尾的空行（Checkstyle `NewlineAtEndOfFile`）

### 1.2 命名约定

| 类型 | 规范 | 示例 |
|---|---|---|
| 包 | 全小写、无下划线，按"功能 × 分层"组织 | `com.rainier.auth.controller` |
| 类 / 接口 / 枚举 | PascalCase | `AuthController`、`GlobalExceptionHandler` |
| 方法 / 变量 | camelCase | `issueToken()`、`expirationMillis` |
| 常量 / static final | UPPER_SNAKE_CASE | `TOKEN_STORAGE_KEY`、`ATTR_USERNAME`、`BEARER_PREFIX` |
| 测试类 | 被测类名 + `Test` | `AuthControllerLoginTest`、`HealthControllerTest` |
| DTO 后缀 | `Request` / `Response` / `Dto` | `LoginRequest`、`LoginResponse`、`UserDto` |
| 自定义异常 | `XxxException` | `BadRequestException`、`UnauthorizedException` |

### 1.3 Import 顺序

由 Google Java Format 强制执行：
1. 静态导入（`import static …`）
2. 普通导入（按字母序）

禁止通配符 `import com.xxx.*`（Checkstyle `AvoidStarImport`）。

## 二、包结构与分层

### 2.1 根包

`com.rainier`，按"功能 × 分层"双维度组织：先按 feature 拆子包，再在每个子包内分层。

```
com.rainier
├── RainierApplication.java
├── <feature>
│   ├── controller/<Feature>Controller.java       // HTTP 入口，薄
│   ├── service/<Feature>Service.java             // 业务逻辑
│   ├── repository/<Feature>Repository.java       // JPA / MyBatis（如需）
│   ├── domain/<Entity>.java                       // 领域对象
│   └── dto/{Create,Update,Detail,ListItem,...}Dto.java
├── config
│   ├── CorsConfig.java
│   ├── SecurityFilter.java
│   └── JacksonConfig.java
└── common
    └── exception/{BadRequestException,UnauthorizedException,GlobalExceptionHandler}.java
```

**实例**：`com.rainier.auth` 见 [`backend/src/main/java/com/rainier/auth/`](../../backend/src/main/java/com/rainier/auth/)。

### 2.2 分层规则

- **controller** 只做 HTTP ↔ DTO 转换 + 调 service；禁止直接访问 repository / 出现业务规则
- **service** 持有业务规则；可注入多个 repository / 其他 service；返回 domain 或 dto
- **repository** 只做持久化；返回 domain
- **domain** 是富/贫领域对象（v0 默认贫，未来按需富化），无 framework 注解
- **dto** 是 HTTP 边界对象，含 getter/setter + Jackson 默认序列化；禁止携带业务方法

## 三、并发与异步

### 3.1 规则

- 优先复用 Spring 的 `@Async` + `ThreadPoolTaskExecutor`，不自建 `new Thread()`
- IO 调用必须有超时（`HttpClient` / `RestTemplate` 显式 `setConnectTimeout` / `setReadTimeout`）
- 数据库调用走连接池（HikariCP，Spring Boot 默认）

### 3.2 并发安全

- 共享可变状态用 `java.util.concurrent.locks` 或 `AtomicReference`，禁止 `synchronized(this)`（粒度难控）
- 不要在锁内做 IO（数据库 / HTTP / 文件）
- v0 暂无并发场景；引入时本节扩展

## 四、错误处理

### 4.1 规则

- **系统边界统一处理**：所有未捕获异常由 `@RestControllerAdvice`（[`GlobalExceptionHandler`](../../backend/src/main/java/com/rainier/common/exception/GlobalExceptionHandler.java)）拦截
- **业务异常用自定义类**：继承 `RuntimeException`，命名 `XxxException`，构造函数只接收 `String message`
  - 已有：`BadRequestException` → 400、`UnauthorizedException` → 401
  - 新增业务能力时在 `common/exception/` 下添加，并在 `GlobalExceptionHandler` 加 `@ExceptionHandler`
- **响应体格式统一**：`{"message": "..."}`，禁止暴露 stack trace 或服务器内部细节
- **禁止裸 `catch (Exception e)` 然后吞掉**；如必须吞，加 `LOG.debug("ignored: {}", e.getMessage())` 标注理由

### 4.2 输入校验

- v0 使用手动检查（`isBlank` 等），见 [`AuthController.login`](../../backend/src/main/java/com/rainier/auth/controller/AuthController.java)
- 业务规则复杂时可引入 `@Valid` + JSR-380（依赖已含 `spring-boot-starter-validation`），并在 `GlobalExceptionHandler` 添加 `MethodArgumentNotValidException` 处理器

## 五、日志

### 5.1 规则

- 用 `org.slf4j.Logger` + `LoggerFactory.getLogger(MyClass.class)`，**不用** `System.out` / `e.printStackTrace()`
- 异常用 `LOG.error("简短描述", ex)`（自动带 stack trace 到日志，但不进响应）
- 关键业务节点（用户登录、状态变更、对外调用）记 `INFO`；调试细节记 `DEBUG`
- 禁止在循环里打 `DEBUG`（噪音灾难）

### 5.2 日志内容

- 必须包含可定位上下文：`username`、`requestId`、关键参数
- 推荐格式：`"操作 op=xxx user={} duration={}ms", user, ms`（占位符避免无谓字符串拼接）
- **禁止打印**：密码、token、JWT、卡号等敏感信息（即使是 mock token 也不打）

## 六、测试代码规范

### 6.1 测试栈

- **JUnit 5**（`org.junit.jupiter.api.*`）
- **AssertJ**（`org.assertj.core.api.Assertions.assertThat`）— 主断言库
- **Spring 断言**（`MockMvcResultMatchers.*`）— HTTP 层断言
- **Mockito**（`org.mockito.*` + `org.springframework.boot.test.mock.mockito.*`）— 仅在业务逻辑必要时

### 6.2 测试文件组织

- 测试目录镜像源码包：`backend/src/test/java/com/rainier/<feature>/<layer>/<Class>Test.java`
- 仅 test profile 的辅助控制器放 `backend/src/test/java/com/rainier/diag/`（如 [`BoomController`](../../backend/src/test/java/com/rainier/diag/BoomController.java)），用 `@Profile("test")` + 测试用 `@Import` 引入

### 6.3 测试命名

`<被测方法或端点>_<场景>_<预期结果>`，方法体仅 Arrange / Act / Assert，三段清晰。

**示例**（[`AuthControllerLoginTest`](../../backend/src/test/java/com/rainier/auth/controller/AuthControllerLoginTest.java)）：

```java
@Test
void login_withValidCredentials_returnsJwtAndUser() { ... }

@Test
void login_withMissingUsername_returns400Json() { ... }
```

### 6.4 切片选型

- **WebMvcTest 切片**慎用：只加载控制器层；若控制器 / 全局过滤器（如 `SecurityFilter`）依赖 `@Service`，会 NoSuchBeanDefinitionException。**默认采用** `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")` 三件套（见 v0 全部 controller test）
- 单元纯函数（无 Spring 依赖）用 `@Test` 即可，不引 Spring context

### 6.5 断言强度

- 禁止 `assertNotNull(x)` 单独使用——必须断言具体值（`assertThat(x).isEqualTo(...)`）
- HTTP 响应至少断言：`status`、`Content-Type`、关键 JSON path 的具体值
- 错误路径至少断言：`status`、`body.message` 非空、**不含 stack trace**（`content().string(not(containsString("Exception")))`）
- 时间敏感断言用 `isBetween(..., ...)` 给容差，不要 `isEqualTo` 时间戳

### 6.6 测试隔离

- 默认 Spring context 缓存复用；如某测试改动 bean / 应用上下文，加 `@DirtiesContext`
- `test` profile 默认配置见 [`backend/src/test/resources/application-test.yml`](../../backend/src/test/resources/application-test.yml)
- 引入数据库后：单测用 H2 内存库（per-test 清表）或 Testcontainers MySQL（per-class）

## 七、代码审查清单

> 由 STDD Phase 5 多代理 review 的 `code` agent 使用；逐项核查。

- [ ] **Bug 风险**：边界条件 / 空指针 / 数组越界 / 资源未关闭（try-with-resources）/ 整数溢出
- [ ] **死代码**：无 `System.out.println` / 注释掉的代码 / 未用 import / 未用变量 / 未用方法
- [ ] **一致性**：同 module 内 controller / service 错误处理风格一致；DTO 命名一致；测试命名遵循 §6.3
- [ ] **错误处理**：边界输入有校验；外部调用有超时；catch 没吞异常；自定义业务异常已注册到 `GlobalExceptionHandler`
- [ ] **安全**：无硬编码密钥（dev 占位 secret 需注 `DO NOT USE IN PROD`）；无 SQL 拼接（用参数化）；CORS 白名单显式；JWT 校验路径完整（exp / signature / sub）
- [ ] **测试覆盖**：每个 spec Scenario ≥ 1 测试；新增公共方法 ≥ 1 单测；错误路径 ≥ 1 测试
- [ ] **Fixture 质量**：用 `@SpringBootTest` 三件套；test profile 不依赖 docker（如无业务表，连 H2 都不需要）
- [ ] **断言质量**：断言行为而非实现；禁止 `assertNotNull` 单用；时间断言用 `isBetween`
- [ ] **注释**：保留 WHY（非显而易见的不变性、约束、TODO 加 ticket）；删除 WHAT 注释

## 八、依赖与 pom

- 新增依赖**先评估**：是否进 v0 必要范围；优先选 Spring Boot starter 系列
- 版本统一放 `<properties>`，不在 `<dependency>` 直接写死
- 禁止 `<scope>system</scope>` 引本地 jar
- 禁止 SNAPSHOT 依赖进入 release 分支
