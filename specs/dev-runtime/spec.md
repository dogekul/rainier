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

---

<!-- Appended from change 2026-06-30-user-profile-route (v0.0.118) -->

## MODIFIED Requirements

### Requirement: 本地 Vite 代理支持 127.0.0.1 预览登录

Vite dev server 通过 `/api` 代理访问 backend 时 SHALL 不因浏览器 Origin 为本地临时端口而触发后端 CORS 拦截。

#### Scenario: 127.0.0.1 dev 端口登录可用

- **GIVEN** Rainier frontend dev server 运行在 `http://127.0.0.1:5174`
- **AND** backend 运行在 `http://localhost:8080`
- **WHEN** 浏览器从 `http://127.0.0.1:5174/login` 提交 `POST /api/auth/login`
- **THEN** Vite proxy SHALL 将该请求转发到 backend
- **AND** backend SHALL NOT 返回 `403 Invalid CORS request`
- **AND** 使用有效账号密码 SHALL 成功登录进入工作台

---

<!-- Appended from change 2026-06-04-org-tree-and-employee -->


## MODIFIED Requirements

### Requirement: Compose 启动后 MySQL 含组织表

`docker compose up` 后 MySQL 中 SHALL 存在 3 业务表 + flyway_schema_history。

#### Scenario: Flyway 在 backend healthy 前完成

- **GIVEN** 干净环境 + 干净 docker volume
- **WHEN** `docker compose up -d --build` 后等待全部 healthy（最长 3 分钟）
- **THEN** MySQL 内 `rainier` 库 SHALL 含 `rainier_organization`、`rainier_user`、`rainier_user_organization`、`flyway_schema_history` 四张表
- **AND** 三业务表 SHALL 含本设计文档（design.md §10、§字段表）定义的全部列与 FK 约束
- **AND** `curl http://localhost:18080/api/health` SHALL 返回 200
