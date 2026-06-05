# Capability: entity-organization

## MODIFIED Requirements (from change 2026-06-05-v1-id-migration)

### Requirement: Organization id 与 parent_id 为数字

后端 SHALL 在 `POST /api/organizations` 返回 body 中 `id` 与 `parentId` 为 JSON 数字而非字符串；`Location` header 路径段亦为数字。

#### Scenario: 根节点 id 为数字

- **GIVEN** 数据库为空
- **WHEN** 客户端发起 `POST /api/organizations` body `{"type":"COMPANY","code":"HQ","name":"X"}`
- **THEN** 系统 SHALL 返回 HTTP 201
- **AND** body.id SHALL 为正整数（JSON number 类型）
- **AND** body.parentId SHALL 为 null
- **AND** `Location` header SHALL 形如 `/api/organizations/\d+`

### Requirement: 树缓存 path 内容为数字段

`Organization.path` 字段格式 SHALL 为 `/<id>(/<childId>)*`，其中每个段为十进制数字。

#### Scenario: 三层级路径串为 /1/2/3 形式

- **GIVEN** 三层组织 A → B → C 按序创建，三者 id 分别为 1、2、3（依赖 IDENTITY 顺序生成）
- **WHEN** 客户端 `GET /api/organizations/3`
- **THEN** body.path SHALL 完全等于 `/1/2/3`
- **AND** body.wholeName SHALL 不受 id 类型变化影响（仍由 name 字符串拼接，与 v1 一致）
- **AND** path 中 SHALL 不出现任何 32 字符 hex 段
