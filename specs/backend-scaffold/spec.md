# Capability: backend-scaffold

## ADDED Requirements

### Requirement: Spring Boot 应用 context 可加载

后端 SHALL 包含 `@SpringBootTest` 冒烟用例，验证 application context 在 `test` profile 下可成功加载。

#### Scenario: contextLoads 用例通过

- **GIVEN** 已正确配置 `application-test.yml` 使用 H2 内存库
- **WHEN** 执行 `mvn -ntp test`
- **THEN** 系统 SHALL 报告至少一个 `@SpringBootTest` 用例（名为 `contextLoads`）通过
- **AND** 该用例 SHALL 不依赖外部 MySQL

### Requirement: 未知 endpoint 返回结构化 JSON 404

后端 SHALL 对未注册的路由返回 JSON 格式的 404，禁止返回 Spring Boot 默认的 Whitelabel HTML 错误页。

#### Scenario: 访问未知 endpoint

- **GIVEN** 后端处于运行状态
- **WHEN** 客户端发起 `GET /api/this-does-not-exist`
- **THEN** 系统 SHALL 返回 HTTP 404
- **AND** 响应 Content-Type SHALL 为 `application/json`
- **AND** 响应 body SHALL 包含字段 `message`

### Requirement: 未捕获异常返回结构化 JSON 500

后端 SHALL 通过 `@RestControllerAdvice` 拦截未捕获异常，返回 JSON 500，不暴露 stack trace 给客户端。

#### Scenario: 控制器抛出 RuntimeException

- **GIVEN** 后端在 `test` profile 下注册了诊断用 endpoint `GET /api/_diag/boom`，其实现固定抛出 `RuntimeException("boom")`
- **WHEN** 客户端调用 `GET /api/_diag/boom`
- **THEN** 系统 SHALL 返回 HTTP 500
- **AND** 响应 Content-Type SHALL 为 `application/json`
- **AND** 响应 body SHALL 包含字段 `message`
- **AND** 响应 body SHALL 不包含 stack trace 字符串

### Requirement: 允许前端开发源的 CORS

后端 SHALL 允许来自 `http://localhost:5173`（vite dev）与 `http://localhost`（nginx prod）的跨源 GET / POST 请求。

#### Scenario: CORS 预检请求

- **GIVEN** 后端处于运行状态
- **WHEN** 浏览器发起 `OPTIONS /api/auth/login`，请求头包含 `Origin: http://localhost:5173`、`Access-Control-Request-Method: POST`
- **THEN** 系统 SHALL 返回 HTTP 204 或 200
- **AND** 响应头 `Access-Control-Allow-Origin` SHALL 为 `http://localhost:5173`
- **AND** 响应头 `Access-Control-Allow-Methods` SHALL 包含 `POST`
- **AND** 响应头 `Access-Control-Allow-Headers` SHALL 包含 `Authorization`

---

<!-- Appended from change 2026-06-04-org-tree-and-employee -->


## MODIFIED Requirements

### Requirement: 持久化层 Flyway 自动迁移

后端 SHALL 在 dev profile 启动时自动应用 `db/migration/V<n>__*.sql`，并写入 `flyway_schema_history`。

#### Scenario: 启动日志显示迁移已应用

- **GIVEN** 干净的 MySQL 数据库 `rainier`
- **WHEN** backend 启动（dev profile）
- **THEN** 启动日志 SHALL 含 `Successfully applied 1 migration to schema "rainier"`
- **AND** `flyway_schema_history` 表 SHALL 含一行 `version="1"`、`success=true`

### Requirement: Bean Validation 错误 → 400 JSON 含 fieldErrors

后端 SHALL 在 `@Valid` 校验失败时返回结构化 400。

#### Scenario: 缺必填字段

- **GIVEN** 任一 POST 端点，DTO 含 `@NotBlank` 字段
- **WHEN** 提交缺该字段的 body
- **THEN** SHALL 返回 400
- **AND** body SHALL 含 `message="Validation failed"`
- **AND** body SHALL 含 `fieldErrors[*]` 数组，每项含 `field`、`message`

### Requirement: 软删除全局模式

后端所有 entity SHALL 通过 `@SQLDelete` + `@Where(clause="del_flag=0")` 实现"DELETE 操作转 UPDATE，查询自动过滤"。

#### Scenario: DELETE 实际是 UPDATE

- **GIVEN** rainier_user 中存在 id=`u1`，del_flag=0
- **WHEN** 调用 `userRepository.delete(user)` 或 `DELETE /api/users/u1`
- **THEN** 该行 SHALL 仍存在于 DB
- **AND** 该行 `del_flag` SHALL 为 1
- **AND** 该行 `update_time` SHALL 被更新

#### Scenario: findById 不返回软删行

- **GIVEN** rainier_user 中 id=`u1` 的 del_flag=1
- **WHEN** 调用 `userRepository.findById("u1")`
- **THEN** SHALL 返回 `Optional.empty()`
