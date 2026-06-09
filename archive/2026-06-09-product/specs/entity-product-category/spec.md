# Capability: entity-product-category

> NEW capability from v0.0.12-product (2026-06-09).
> ProductCategory 是产品架构的顶层。Flat 平铺（不做树）。2-state machine (ACTIVE/ARCHIVED).
> code service-级唯一；owner 可改（family Decision 6b）。FK 保护：被 Product 引用时不可删。

## ADDED Requirements

### Requirement: 创建产品分类

后端 SHALL 通过 `POST /api/product-categories` 接受 `code` + `name` + `ownerUserId`（必填），其余字段使用默认值；持久化并返回 201。

#### Scenario: 最小 payload 创建 Category + 默认值 + 富化

- **GIVEN** 数据库存在 User id=1 (loginName="alice", name="Alice")
- **WHEN** `POST /api/product-categories` body `{"code":"CAT-FIN","name":"金融产品","ownerUserId":1}`
- **THEN** SHALL 返回 HTTP 201
- **AND** body.status SHALL 为 "ACTIVE"
- **AND** body SHALL 富化 ownerName="Alice" / ownerLoginName="alice"
- **AND** `Location` header SHALL 形如 `/api/product-categories/\d+`

#### Scenario: code 重复 → 409

- **GIVEN** 数据库已存在 code="CAT-DUP" 的 Category
- **WHEN** 再 POST 同 code
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "code already exists"

#### Scenario: ownerUserId 不存在 → 400

- **GIVEN** 数据库无 User id=999_999
- **WHEN** POST body 含 ownerUserId=999999
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "owner user not found"

#### Scenario: 非法 status → 400

- **GIVEN** User id=1 存在
- **WHEN** POST body 含 status="UNKNOWN"
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "invalid status"

#### Scenario: 缺必填字段 → 400 fieldErrors

- **GIVEN** 后端已启动
- **WHEN** `POST /api/product-categories` body `{"code":"CAT-X"}`（缺 name / ownerUserId）
- **THEN** SHALL 返回 400
- **AND** body.fieldErrors[*].field SHALL 同时含 `"name"` 与 `"ownerUserId"`

### Requirement: 查询产品分类

后端 SHALL 通过 `GET /api/product-categories/{id}` 返回单 Category 详情（含富化）；通过 `GET /api/product-categories?status=&search=&page=&size=` 返回 PageResponse。

#### Scenario: GET 详情完整字段集 + ownerName 富化

- **GIVEN** Category id=1 关联 User id=1 (loginName="alice", name="Alice")
- **WHEN** `GET /api/product-categories/1`
- **THEN** SHALL 返回 200
- **AND** body 字段集 SHALL 含 `[id, code, name, description, status, ownerUserId, ownerName, ownerLoginName, createTime, updateTime, createBy, updateBy]`
- **AND** body.ownerName SHALL 为 "Alice"

#### Scenario: 按 status 过滤列表

- **GIVEN** 数据库存在 2 个 ACTIVE Category + 1 个 ARCHIVED
- **WHEN** `GET /api/product-categories?status=ACTIVE`
- **THEN** body.total SHALL 为 2
- **AND** body.content[*].status SHALL 全为 "ACTIVE"

### Requirement: 更新产品分类（含 owner 可改）

后端 SHALL 通过 `PUT /api/product-categories/{id}` 修改 code / name / description / status / ownerUserId；code 变更须重检唯一性；新 ownerUserId 必须存在。

#### Scenario: 更新 status + owner 转移富化跟随

- **GIVEN** Category id=1 status="ACTIVE", ownerUserId=1; User id=2 (lili, 黎立) 存在
- **WHEN** `PUT /api/product-categories/1` body `{"code":"CAT-X","name":"X","status":"ARCHIVED","ownerUserId":2}`
- **THEN** SHALL 返回 200
- **AND** body.status SHALL 为 "ARCHIVED"
- **AND** body.ownerName SHALL 为 "黎立"
- **AND** body.ownerLoginName SHALL 为 "lili"

#### Scenario: PUT 新 ownerUserId 不存在 → 400

- **GIVEN** Category id=1 存在；User id=999_999 不存在
- **WHEN** PUT body 含 ownerUserId=999999
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "owner user not found"

### Requirement: 软删产品分类（FK 保护）

后端 SHALL 通过 `DELETE /api/product-categories/{id}` 标记 `del_flag=1`；若有未删 Product 引用 → 409。

#### Scenario: 无引用软删成功

- **GIVEN** Category id=1 无 Product 引用
- **WHEN** `DELETE /api/product-categories/1`
- **THEN** SHALL 返回 204
- **AND** 后续 `GET /api/product-categories/1` SHALL 返回 404

#### Scenario: 有 Product 引用 → 409

- **GIVEN** Category id=1 在 `rainier_product` 中有 ≥ 1 行 categoryId=1 AND del_flag=0
- **WHEN** `DELETE /api/product-categories/1`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "category has linked products"
