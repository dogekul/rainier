# Capability: dev-runtime

## ADDED Requirements

### Requirement: Docker Compose 一键启动全部服务

`docker compose up` SHALL 启动 mysql / backend / frontend 三个服务，且 backend 在 mysql healthy 之前不进入 `running` 状态。

#### Scenario: 干净环境一次启动成功

- **GIVEN** 干净的本地环境（已安装 Docker Engine ≥ 20.10、未存在 rainier 网络与数据卷）
- **AND** 仓库已 checkout 至本变更对应的 commit
- **WHEN** 执行 `docker compose up -d --build`
- **THEN** 系统 SHALL 在 3 分钟内使 mysql、backend、frontend 三个 service 的 `State.Health.Status` 全部变为 `healthy`
- **AND** `curl http://localhost:8080/api/health` SHALL 返回 HTTP 200 且 body 含 `"status":"UP"`
- **AND** `curl -I http://localhost/` SHALL 返回 HTTP 200

#### Scenario: backend 等待 mysql healthy 才启动

- **GIVEN** `docker-compose.yml` 中 backend service 配置了 `depends_on: { mysql: { condition: service_healthy } }`
- **WHEN** 执行 `docker compose up -d` 且 mysql 启动较慢
- **THEN** 系统 SHALL 直到 mysql healthcheck 通过才创建 backend 容器
- **AND** backend 启动日志 SHALL 不包含 `Communications link failure` 类 MySQL 连接异常
