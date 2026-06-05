# Capability: backend-scaffold

## MODIFIED Requirements (from change 2026-06-05-v1-id-migration)

### Requirement: 实体主键类型为 BIGINT AUTO_INCREMENT

后端 `BaseEntity` SHALL 用 `Long id` + `GenerationType.IDENTITY` 替代 v1 的 `String id` + `UUIDHexGenerator`。

#### Scenario: BaseEntity 字段反射断言

- **GIVEN** 已编译的 `BaseEntity.class`
- **WHEN** 反射读取 `id` 字段元数据
- **THEN** 系统 SHALL 报告 `Field.getType() == Long.class`
- **AND** Field SHALL 含 `@GeneratedValue` 注解
- **AND** `GeneratedValue.strategy()` SHALL 为 `GenerationType.IDENTITY`

### Requirement: @PathVariable 自动类型转换错误兜底

后端 SHALL 在 controller 接收非数字 path 参数时返回 400 JSON，而不是 500。

#### Scenario: 非数字 id 路径

- **GIVEN** backend 处于运行状态
- **WHEN** 客户端发起 `GET /api/organizations/not-a-number`
- **THEN** 系统 SHALL 返回 HTTP 400
- **AND** 响应 Content-Type SHALL 为 `application/json`
- **AND** body SHALL 包含字段 `message`
- **AND** body SHALL 不包含 stack trace 字符串
