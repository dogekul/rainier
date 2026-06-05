# Capability: test-runtime

## MODIFIED Requirements

### Requirement: 测试 profile 用 H2 + ddl-auto

测试运行 SHALL 用 H2 内存库，不依赖外部 MySQL，不应用 Flyway 迁移。

#### Scenario: mvn test 在无 docker 环境通过

- **GIVEN** 已安装 JDK 8 + Maven，无 docker 运行
- **WHEN** 在 `backend/` 执行 `mvn -ntp test`
- **THEN** SHALL 退出码 0
- **AND** Surefire 报告 SHALL 显示 ≥ 32 测试通过、0 失败
- **AND** 测试日志 SHALL 含 H2 启动标志（如 `H2 console available` 或 `jdbc:h2:mem:`）
- **AND** 测试日志 SHALL 不含 `flyway` 启动 / migration 条目
