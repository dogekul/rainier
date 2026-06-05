# Capability: test-runtime

## ADDED Requirements

### Requirement: 后端测试与静态检查全部通过

后端工程 SHALL 提供可独立运行的测试与静态检查命令，全部以 0 退出码结束。

#### Scenario: mvn test 全绿

- **GIVEN** 已安装 JDK 8 + Maven 3.6+
- **AND** `backend/` 目录已 checkout 至本变更对应 commit
- **WHEN** 在 `backend/` 执行 `mvn -ntp test`
- **THEN** 系统 SHALL 以退出码 0 结束
- **AND** Surefire 报告 SHALL 显示至少 1 个测试用例通过
- **AND** Surefire 报告 SHALL 显示失败数为 0

#### Scenario: 后端 lint 零告警

- **GIVEN** 已安装 JDK 8 + Maven 3.6+
- **WHEN** 在 `backend/` 执行 `mvn -ntp spotless:check checkstyle:check`
- **THEN** 系统 SHALL 以退出码 0 结束
- **AND** 输出 SHALL 不包含 `BUILD FAILURE`

### Requirement: 前端测试、构建与静态检查全部通过

前端工程 SHALL 提供可独立运行的测试 / 构建 / 静态检查命令，全部以 0 退出码结束。

#### Scenario: npm test 全绿

- **GIVEN** 已安装 Node.js ≥ 18 + npm ≥ 9
- **AND** `frontend/` 目录已执行 `npm ci`
- **WHEN** 在 `frontend/` 执行 `npm test -- --run`
- **THEN** 系统 SHALL 以退出码 0 结束
- **AND** Vitest 输出 SHALL 显示至少 1 个测试用例通过

#### Scenario: 生产构建无 type error

- **GIVEN** 已安装 Node.js ≥ 18，且 `frontend/` 已 `npm ci`
- **WHEN** 在 `frontend/` 执行 `npm run build`
- **THEN** 系统 SHALL 以退出码 0 结束
- **AND** 输出 SHALL 不包含 `error TS`
- **AND** 构建产物 SHALL 出现在 `frontend/dist/index.html`

#### Scenario: 前端 lint 零告警

- **GIVEN** 已安装 Node.js ≥ 18，且 `frontend/` 已 `npm ci`
- **WHEN** 在 `frontend/` 执行 `npm run lint`
- **THEN** 系统 SHALL 以退出码 0 结束
- **AND** ESLint 输出 SHALL 显示 `0 errors, 0 warnings`

---

<!-- Appended from change 2026-06-04-org-tree-and-employee -->


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
