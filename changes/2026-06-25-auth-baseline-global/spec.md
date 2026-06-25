# Spec — auth-baseline-global (B1, v0.0.74)

> 强化 v0.0.27 引入的 `app.security.require-all-users-token` 全员 token 基线：常量化白名单 +
> 真实 servlet 链路 end-to-end 回归。

## MODIFIED Requirements

### Requirement: 白名单常量化

后端 SHALL 提供 `com.rainier.config.SecurityWhitelistPaths` 公共常量类，列举所有跳过 token 检查的端点
（POST /api/auth/login、GET /api/health、CORS preflight），`SecurityFilter` SHALL 通过该常量类判定白名单。

#### Scenario: SecurityFilter 引用常量

- **GIVEN** `app.security.require-all-users-token.enabled=true`
- **WHEN** 请求 GET /api/health
- **THEN** SHALL 返回 200（不需 token）
- **AND** 常量值由 `SecurityWhitelistPaths` 暴露给测试与下游引用方

## ADDED Requirements

### Requirement: 全员 token 基线 end-to-end 回归

后端 SHALL 提供 `GlobalAuthBaselineTest`（@SpringBootTest webEnvironment=RANDOM_PORT，flag enabled=true）
通过真实 TestRestTemplate 走完整 servlet 链路验证基线。

#### Scenario: 无 token 访问保护端点

- **GIVEN** `app.security.require-all-users-token.enabled=true`
- **WHEN** GET /api/me/inbox 无 Authorization
- **THEN** SHALL 返回 401
- **AND** body.message 包含 "Missing or invalid token"

#### Scenario: 有 token 访问保护端点

- **GIVEN** 同上
- **WHEN** GET /api/me/inbox 带合法 Bearer
- **THEN** SHALL 返回 200

#### Scenario: 白名单 login 端点未被门挡

- **WHEN** POST /api/auth/login 无 Authorization 体合法
- **THEN** SHALL 返回 200（非 SecurityFilter 401）

#### Scenario: matrix-param 不能绕过

- **WHEN** GET /api/me/inbox;x=1 无 Authorization
- **THEN** SHALL 返回 401（PATH_HELPER 已剥离 ;x=1）
