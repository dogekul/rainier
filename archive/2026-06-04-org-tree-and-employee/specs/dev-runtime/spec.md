# Capability: dev-runtime

## MODIFIED Requirements

### Requirement: Compose 启动后 MySQL 含组织表

`docker compose up` 后 MySQL 中 SHALL 存在 3 业务表 + flyway_schema_history。

#### Scenario: Flyway 在 backend healthy 前完成

- **GIVEN** 干净环境 + 干净 docker volume
- **WHEN** `docker compose up -d --build` 后等待全部 healthy（最长 3 分钟）
- **THEN** MySQL 内 `rainier` 库 SHALL 含 `rainier_organization`、`rainier_user`、`rainier_user_organization`、`flyway_schema_history` 四张表
- **AND** 三业务表 SHALL 含本设计文档（design.md §10、§字段表）定义的全部列与 FK 约束
- **AND** `curl http://localhost:18080/api/health` SHALL 返回 200
