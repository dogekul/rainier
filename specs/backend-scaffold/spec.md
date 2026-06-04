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
