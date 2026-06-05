# Capability: entity-organization

## MODIFIED Requirements (from change 2026-06-05-remove-org-pmo)

### Requirement: Organization 实体与 API 契约不再含 isPmo 字段

后端 SHALL 在持久层 (entity + DB 列) 与 API 契约 (request body 接受字段集 + response body 输出字段集) 全栈层面移除 `isPmo` / `is_pmo`；旧客户端发送 `isPmo` 时 SHALL 静默忽略而非拒绝。

#### Scenario: POST /api/organizations 响应不含 isPmo

- **GIVEN** 数据库为空
- **WHEN** 客户端 `POST /api/organizations` body `{"type":"COMPANY","code":"HQ","name":"X"}`
- **THEN** 系统 SHALL 返回 HTTP 201
- **AND** body SHALL 含 `id`、`type`、`code`、`name`、`path`、`wholeName`、`enabled`、`createTime`
- **AND** body SHALL **不含** `isPmo` key（`jsonPath("$.isPmo").doesNotExist()` 成立）

#### Scenario: GET /api/organizations/{id} 响应不含 isPmo

- **GIVEN** 数据库存在节点 id=1
- **WHEN** 客户端 `GET /api/organizations/1`
- **THEN** 系统 SHALL 返回 HTTP 200
- **AND** body SHALL **不含** `isPmo` key
- **AND** body 字段集 SHALL 等于 [id, parentId, type, code, name, description, path, wholeName, enabled, createTime, updateTime]

#### Scenario: PUT /api/organizations/{id} body 带 isPmo 静默忽略

- **GIVEN** 数据库存在节点 id=1，code="HQ"，name="X"
- **WHEN** 客户端 `PUT /api/organizations/1` body `{"code":"HQ","name":"Y","isPmo":true}`
- **THEN** 系统 SHALL 返回 HTTP 200（**非 400**）
- **AND** 响应 body.name SHALL 为 "Y"
- **AND** 响应 body SHALL 不含 `isPmo` key
- **AND** DB 中该行 SHALL 无 `is_pmo` 列（DESCRIBE 验证）

### Requirement: rainier_organization 表 schema 不含 is_pmo 列

数据库 `rainier_organization` 表 SHALL 不含 `is_pmo` 列；通过 `docker compose down -v && up --build` 重生 schema 时 Hibernate ddl-auto SHALL 不生成该列。

#### Scenario: DESCRIBE 表结构无 is_pmo 列

- **GIVEN** 执行 `docker compose down -v && docker compose up -d --build` 起栈完成、所有服务 healthy
- **WHEN** 执行 `docker exec rainier-mysql mysql -u rainier -prainier rainier -e "DESCRIBE rainier_organization"`
- **THEN** 输出 SHALL 不含字段名 `is_pmo` 的行
- **AND** 字段集 SHALL 与 Organization.java 中标注 `@Column` 的字段一一对应
