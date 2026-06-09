# Capability: entity-product

> NEW capability from v0.0.12-product (2026-06-09).
> Product 属一个 ProductCategory（NN FK）。4-state machine (PLANNING/ACTIVE/SUNSET/ARCHIVED).
> categoryId 创建后 immutable（v0.0.11 Decision 11 sibling）。
> code service-级唯一；owner 可改；FK 保护（被 Module 引用时不可删）。

## ADDED Requirements

### Requirement: 创建产品

后端 SHALL 通过 `POST /api/products` 接受 `code` + `name` + `categoryId` + `ownerUserId`（必填）；持久化并返回 201。

#### Scenario: 最小 payload + categoryName 富化

- **GIVEN** Category id=1 (code=CAT-FIN, name="金融产品") + User id=1 ("Alice")
- **WHEN** `POST /api/products` body `{"code":"PROD-PAY","name":"支付平台","categoryId":1,"ownerUserId":1}`
- **THEN** SHALL 返回 201
- **AND** body.status SHALL 为 "PLANNING"（默认）
- **AND** body SHALL 富化 categoryName="金融产品" / categoryCode="CAT-FIN" / ownerName="Alice"

#### Scenario: categoryId 不存在 → 400

- **GIVEN** 数据库无 Category id=999
- **WHEN** POST body 含 categoryId=999
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "category not found"

#### Scenario: code 重复 → 409

- **GIVEN** 已存在 code="PROD-DUP" Product
- **WHEN** 再 POST 同 code
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "code already exists"

#### Scenario: 非法 status → 400

- **WHEN** POST body 含 status="UNKNOWN"
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "invalid status"

#### Scenario: 缺必填字段 → 400

- **WHEN** POST body `{"code":"PROD-X"}`（缺 name/categoryId/ownerUserId）
- **THEN** SHALL 返回 400
- **AND** body.fieldErrors[*].field SHALL 同时含 `"name"` / `"categoryId"` / `"ownerUserId"`

### Requirement: 查询产品

后端 SHALL 通过 `GET /api/products/{id}` 返回单 Product 详情（含富化）；通过 `GET /api/products?categoryId=&status=&search=&page=&size=` 返回 PageResponse。

#### Scenario: GET 详情完整字段集

- **GIVEN** Product id=1 关联 Category 1 + User 1
- **WHEN** `GET /api/products/1`
- **THEN** SHALL 返回 200
- **AND** body 字段集 SHALL 含 `[id, code, name, description, status, categoryId, categoryCode, categoryName, ownerUserId, ownerName, ownerLoginName, createTime, updateTime, createBy, updateBy]`

#### Scenario: 按 categoryId 过滤列表

- **GIVEN** Category A 下 2 个 Product, Category B 下 1 个
- **WHEN** `GET /api/products?categoryId={A.id}`
- **THEN** body.total SHALL 为 2
- **AND** body.content[*].categoryId SHALL 全为 A.id

### Requirement: 更新产品（含 owner 可改；categoryId 不可改）

后端 SHALL 通过 `PUT /api/products/{id}` 修改 code / name / description / status / ownerUserId；`categoryId` 创建后不可改（请求体不接受）。

#### Scenario: 更新 status + owner 转移

- **GIVEN** Product id=1 status=PLANNING; User id=2 存在
- **WHEN** PUT body 含 `status="ACTIVE", ownerUserId=2`
- **THEN** SHALL 返回 200
- **AND** body.status SHALL 为 "ACTIVE"
- **AND** body.ownerName SHALL 为 User id=2 的 name

### Requirement: 软删产品（FK 保护）

后端 SHALL 通过 `DELETE /api/products/{id}` 标记 `del_flag=1`；若有 Module 引用 → 409。

#### Scenario: 无引用软删成功

- **GIVEN** Product id=1 无 Module 引用
- **WHEN** `DELETE /api/products/1`
- **THEN** SHALL 返回 204
- **AND** 后续 GET SHALL 返回 404

#### Scenario: 有 Module 引用 → 409

- **GIVEN** Product id=1 在 `rainier_product_module` 中有 ≥ 1 行 productId=1 AND del_flag=0
- **WHEN** `DELETE /api/products/1`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "product has linked modules"
