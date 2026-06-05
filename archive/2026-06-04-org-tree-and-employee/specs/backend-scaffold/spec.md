# Capability: backend-scaffold

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
