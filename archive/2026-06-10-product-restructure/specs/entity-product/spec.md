# Capability: entity-product

> MODIFIED in v0.0.13-product-restructure (2026-06-10).
> 移除 `categoryId` 字段（v0.0.12 引入的 ProductCategory capability 整层删除）。
> Product 现在直接是顶层产品域实体，不再属于任何分类。
> 4-state machine (PLANNING/ACTIVE/SUNSET/ARCHIVED) 保留。
> code service-级唯一；owner 可改；FK 保护（被 Module 引用时不可删）。

## MODIFIED Requirements

### Requirement: 创建产品（去 categoryId）

后端 SHALL 通过 `POST /api/products` 接受 `code` + `name` + `ownerUserId`（必填）；持久化并返回 201。**不再接受 `categoryId` 字段**。

#### Scenario: 最小 payload + ownerName 富化

- **GIVEN** User id=1 ("Alice") 存在
- **WHEN** `POST /api/products` body `{"code":"PROD-PAY","name":"支付平台","ownerUserId":1}`
- **THEN** SHALL 返回 201
- **AND** body.status SHALL 为 "PLANNING"（默认）
- **AND** body SHALL 富化 ownerName="Alice"
- **AND** body SHALL 不含 categoryId / categoryCode / categoryName 字段

#### Scenario: 请求体含 categoryId 时静默忽略（不报错也不存）

- **GIVEN** User id=1 存在
- **WHEN** `POST /api/products` body 含 `"categoryId":999`（已废弃字段）
- **THEN** SHALL 返回 201
- **AND** body SHALL 不含 categoryId 字段
- **AND** 后续 GET 不显示 categoryId

#### Scenario: code 重复 → 409

- **GIVEN** 已存在 code="PROD-DUP"
- **WHEN** 再 POST 同 code
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "code already exists"

#### Scenario: 非法 status → 400

- **WHEN** POST body 含 status="UNKNOWN"
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "invalid status"

#### Scenario: 缺必填字段 → 400

- **WHEN** POST body `{"code":"PROD-X"}`（缺 name/ownerUserId）
- **THEN** SHALL 返回 400
- **AND** body.fieldErrors[*].field SHALL 同时含 `"name"` / `"ownerUserId"`
- **AND** body.fieldErrors[*].field SHALL **不含** `"categoryId"`（字段已删）

### Requirement: 查询产品（去 categoryId 富化与过滤）

后端 SHALL 通过 `GET /api/products/{id}` 返回单 Product 详情（含 owner 富化，**不含** category 富化）；通过 `GET /api/products?status=&search=&page=&size=` 返回 PageResponse。**不再接受 `categoryId` 查询参数**。

#### Scenario: GET 详情字段集（无 category 字段）

- **GIVEN** Product id=1 关联 User 1
- **WHEN** `GET /api/products/1`
- **THEN** SHALL 返回 200
- **AND** body 字段集 SHALL 含 `[id, code, name, description, status, ownerUserId, ownerName, ownerLoginName, createTime, updateTime, createBy, updateBy]`
- **AND** body SHALL **不含** `categoryId` / `categoryCode` / `categoryName`

#### Scenario: list 端点请求含 categoryId 参数时静默忽略

- **GIVEN** 数据库存在 3 个 Product
- **WHEN** `GET /api/products?categoryId=1`
- **THEN** SHALL 返回 200
- **AND** body.total SHALL 为 3（filter 失效但不报错）

### Requirement: 更新产品（owner 可改；状态 / 描述可改）

后端 SHALL 通过 `PUT /api/products/{id}` 修改 code / name / description / status / ownerUserId。**不再接受 `categoryId`**。

#### Scenario: 更新 status + owner 转移

- **GIVEN** Product id=1 status=PLANNING; User id=2 存在
- **WHEN** PUT body 含 `status="ACTIVE", ownerUserId=2`
- **THEN** SHALL 返回 200
- **AND** body.status SHALL 为 "ACTIVE"
- **AND** body.ownerName SHALL 为 User id=2 的 name

### Requirement: 软删产品（FK 保护 — 仅检查 Module）

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

---

## REMOVED Requirements（v0.0.12 → v0.0.13 删除）

- ~~创建产品（含 categoryId 必填校验）~~ → 见上 Create 修订版
- ~~按 categoryId 过滤列表~~ → 见上 Query 修订版（filter 字段移除）
- ~~更新产品 — categoryId 不可改~~ → 见上 Update 修订版（categoryId 字段已删，约束失效）
