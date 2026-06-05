# Capability: pagination-envelope

## ADDED Requirements

### Requirement: 所有 list 接口统一返回 PageResponse

后端 SHALL 在 `/api/organizations`、`/api/users`、`/api/user-organizations` list 接口返回相同形态。

#### Scenario: envelope 字段稳定

- **GIVEN** 任一 list 接口
- **WHEN** 客户端发起 GET 请求
- **THEN** 响应 body SHALL 含且仅含字段 `content`、`page`、`size`、`total`
- **AND** `content` SHALL 为数组（可空）
- **AND** `page` SHALL 为非负整数
- **AND** `size` SHALL 为正整数
- **AND** `total` SHALL 为非负长整数

#### Scenario: size 超出上限被拒

- **GIVEN** backend 已启动
- **WHEN** `GET /api/organizations?size=101`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "size must be ≤ 100"

#### Scenario: page=0 size=20 为默认值

- **GIVEN** backend 已启动，相关表空
- **WHEN** `GET /api/users` 无任何参数
- **THEN** body SHALL 为 `{"content":[],"page":0,"size":20,"total":0}`
