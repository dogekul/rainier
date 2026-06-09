# Capability: entity-product-module

> NEW capability from v0.0.12-product (2026-06-09).
> ProductModule 属一个 Product（NN FK）。3-state machine (PLANNING/ACTIVE/DEPRECATED).
> productId 创建后 immutable。
> FK 保护（被 Feature 引用时不可删）。

## ADDED Requirements

### Requirement: 创建产品模块

后端 SHALL 通过 `POST /api/product-modules` 接受 `code` + `name` + `productId` + `ownerUserId`（必填）；持久化并返回 201。

#### Scenario: 最小 payload + productName 富化

- **GIVEN** Product id=1 (code=PROD-PAY, name="支付平台") + User id=1 ("Alice")
- **WHEN** `POST /api/product-modules` body `{"code":"MOD-WALLET","name":"钱包","productId":1,"ownerUserId":1}`
- **THEN** SHALL 返回 201
- **AND** body.status SHALL 为 "PLANNING"
- **AND** body SHALL 富化 productCode="PROD-PAY" / productName="支付平台" / ownerName="Alice"

#### Scenario: productId 不存在 → 400

- **GIVEN** 数据库无 Product id=999
- **WHEN** POST body 含 productId=999
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "product not found"

#### Scenario: code 重复 → 409

- **WHEN** 重复 POST 同 code
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "code already exists"

#### Scenario: 非法 status → 400

- **WHEN** POST body 含 status="UNKNOWN"
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "invalid status"

#### Scenario: 缺必填字段 → 400

- **WHEN** POST body `{"code":"MOD-X"}`（缺 name/productId/ownerUserId）
- **THEN** SHALL 返回 400
- **AND** body.fieldErrors[*].field SHALL 同时含 `"name"` / `"productId"` / `"ownerUserId"`

### Requirement: 查询产品模块

后端 SHALL 通过 `GET /api/product-modules/{id}` 返回单 Module 详情；通过 `GET /api/product-modules?productId=&status=&search=&page=&size=` 返回 PageResponse。

#### Scenario: GET 详情完整字段集

- **GIVEN** Module id=1 关联 Product 1 + User 1
- **WHEN** `GET /api/product-modules/1`
- **THEN** SHALL 返回 200
- **AND** body 字段集 SHALL 含 `[id, code, name, description, status, productId, productCode, productName, ownerUserId, ownerName, ownerLoginName, createTime, updateTime, createBy, updateBy]`

#### Scenario: 按 productId 过滤列表

- **GIVEN** Product A 下 2 Module, Product B 下 1 Module
- **WHEN** `GET /api/product-modules?productId={A.id}`
- **THEN** body.total SHALL 为 2
- **AND** body.content[*].productId SHALL 全为 A.id

### Requirement: 更新产品模块（含 owner 可改；productId 不可改）

后端 SHALL 通过 `PUT /api/product-modules/{id}` 修改 code / name / description / status / ownerUserId；`productId` 不可改。

#### Scenario: 更新 status + owner 转移

- **GIVEN** Module id=1 status=PLANNING; User id=2 存在
- **WHEN** PUT body 含 status="ACTIVE", ownerUserId=2
- **THEN** SHALL 返回 200
- **AND** body.status SHALL 为 "ACTIVE"
- **AND** body.ownerName SHALL 跟随至 User id=2 的 name

### Requirement: 软删产品模块（FK 保护）

后端 SHALL 通过 `DELETE /api/product-modules/{id}` 标记 `del_flag=1`；若有 Feature 引用 → 409。

#### Scenario: 无引用软删成功

- **GIVEN** Module id=1 无 Feature 引用
- **WHEN** `DELETE /api/product-modules/1`
- **THEN** SHALL 返回 204
- **AND** 后续 GET SHALL 返回 404

#### Scenario: 有 Feature 引用 → 409

- **GIVEN** Module id=1 在 `rainier_feature` 中有 ≥ 1 行 moduleId=1 AND del_flag=0
- **WHEN** `DELETE /api/product-modules/1`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "module has linked features"
