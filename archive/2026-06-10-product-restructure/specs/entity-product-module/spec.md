# Capability: entity-product-module

> MODIFIED in v0.0.13-product-restructure (2026-06-10).
> ProductModule 仍属一个 Product（NN FK），新增 **`parentId` 自引用 FK（nullable）** 形成 2~3 层树。
> 3-state machine 保留；productId 仍创建后 immutable；parentId 创建后**可改**（含严格 reparent 验证）。
> code 唯一性从 service-级全局改为 **`(parentId, code)` 复合唯一** + 顶层 `(productId, parentId IS NULL, code)` 应用层校验。
> 删除链双向 — 同时检查 child Module 与 child Feature。

## MODIFIED Requirements

### Requirement: 创建产品模块（可选 parentId）

后端 SHALL 通过 `POST /api/product-modules` 接受 `code` + `name` + `productId` + `ownerUserId`（必填）+ `parentId`（可选）；持久化并返回 201。当 parentId 非空时，强制 cross-product + depth 校验。

#### Scenario: 顶层 module 创建（parentId 为 null）+ 默认富化

- **GIVEN** Product id=1 (code=PROD-PAY, name="支付平台") + User id=1 ("Alice")
- **WHEN** `POST /api/product-modules` body `{"code":"MOD-WALLET","name":"钱包","productId":1,"ownerUserId":1}`（不传 parentId）
- **THEN** SHALL 返回 201
- **AND** body.parentId SHALL 为 null
- **AND** body.pathName SHALL 为 "钱包"
- **AND** body.pathCodes SHALL 为 "MOD-WALLET"
- **AND** body.status SHALL 为 "PLANNING"

#### Scenario: 子 module 创建（parentId 指向同 product 顶层）

- **GIVEN** Product id=1 下已存在顶层 Module M1 (id=10, code=MOD-WALLET, name="钱包")
- **WHEN** `POST /api/product-modules` body `{"code":"MOD-BALANCE","name":"余额","productId":1,"parentId":10,"ownerUserId":1}`
- **THEN** SHALL 返回 201
- **AND** body.parentId SHALL 为 10
- **AND** body.pathName SHALL 为 "钱包 / 余额"
- **AND** body.pathCodes SHALL 为 "MOD-WALLET / MOD-BALANCE"

#### Scenario: parentId 不存在 → 400

- **GIVEN** 数据库无 Module id=999
- **WHEN** POST body 含 parentId=999
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "parent module not found"

#### Scenario: productId 不存在 → 400

- **GIVEN** 数据库无 Product id=999
- **WHEN** POST body 含 productId=999
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "product not found"

#### Scenario: 非法 status → 400

- **WHEN** POST body 含 status="UNKNOWN"
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "invalid status"

#### Scenario: 缺必填字段 → 400

- **WHEN** POST body `{"code":"MOD-X"}`（缺 name/productId/ownerUserId）
- **THEN** SHALL 返回 400
- **AND** body.fieldErrors[*].field SHALL 同时含 `"name"` / `"productId"` / `"ownerUserId"`

### Requirement: code 唯一性 — 复合 (parentId, code)

后端 SHALL 在 DB 加 `uk_product_module_parent_code(parent_id, code)` 复合唯一约束；当 parentId 为 null 时，应用层 Service SHALL 检查同 `productId` 内所有顶层模块的 code 唯一性。

#### Scenario: 同 parent 下 code 重复 → 409

- **GIVEN** Module M1 (parentId=10, code="MOD-DUP") 已存在
- **WHEN** POST body `{code:"MOD-DUP", parentId:10, productId:1, name:"X", ownerUserId:1}`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "code already exists under parent"

#### Scenario: 不同 parent 下 code 可重复（不冲突）

- **GIVEN** Module M1 (parentId=10, code="MOD-CFG") 已存在
- **WHEN** POST body `{code:"MOD-CFG", parentId:20, productId:1, name:"另一个配置", ownerUserId:1}`（同 productId 不同 parent）
- **THEN** SHALL 返回 201

#### Scenario: 同 product 顶层 code 重复 → 409（应用层）

- **GIVEN** Product 1 下顶层 Module (parentId=null, code="MOD-TOP") 已存在
- **WHEN** POST body `{code:"MOD-TOP", productId:1, name:"X", ownerUserId:1}`（不传 parentId）
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "code already exists in top-level"

### Requirement: 跨产品父级拒绝

后端 SHALL 在 create/update 时检查：若 parentId 非空，对应 parent module 的 `productId` 必须与请求/当前 module 的 `productId` 相同；否则 → 400。

#### Scenario: parent 属另一 product → 400

- **GIVEN** Product P1, Product P2; Module M (id=20, productId=P2)
- **WHEN** POST body `{code:"MOD-X", name:"X", productId:P1.id, parentId:20, ownerUserId:1}`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "parent module must belong to the same product"

### Requirement: 深度上限 — 可配置（默认 3）

后端 SHALL 注入配置项 `rainier.product-module.depth.max`（默认 3）；create/update 时 walk parent chain 校验「自身层数 ≤ max」；超出 → 400。

#### Scenario: 创建第 4 层（超 max=3）→ 400

- **GIVEN** Module 链 L1 (parentId=null) → L2 (parentId=L1) → L3 (parentId=L2)；max=3
- **WHEN** POST body `{code:"MOD-L4", productId:1, parentId:L3.id, name:"X", ownerUserId:1}`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "max module depth exceeded: 4 > 3"

#### Scenario: 创建第 3 层（等于 max=3）→ 201

- **GIVEN** Module 链 L1 → L2；max=3
- **WHEN** POST body `{code:"MOD-L3", productId:1, parentId:L2.id, ...}`
- **THEN** SHALL 返回 201

### Requirement: 更新产品模块 — parentId 可变 + 三检

后端 SHALL 通过 `PUT /api/product-modules/{id}` 修改 code / name / description / status / ownerUserId / **parentId**；productId 仍创建后不可改；reparent 时按顺序校验 cross-product → cycle → depth。

#### Scenario: 同产品内 reparent 成功

- **GIVEN** Module M (id=30, productId=1, parentId=10); Module M2 (id=20, productId=1, parentId=null) 存在
- **WHEN** PUT body 含 parentId=20
- **THEN** SHALL 返回 200
- **AND** body.parentId SHALL 为 20
- **AND** body.pathName SHALL 重新拼接

#### Scenario: reparent 到自己的祖先 → 200 合法（非 cycle）

- **GIVEN** Module 树 A → B → C
- **WHEN** C 被 PUT body 含 `parentId=B.id`（B 是 C 的祖先 — 祖先不在 C 的子孙集，不构成环）
- **THEN** SHALL 返回 200
- **AND** body.parentId SHALL 为 B.id

#### Scenario: reparent 到自身 → 400 cycle

- **GIVEN** Module M (id=30) 存在
- **WHEN** PUT body 含 parentId=30（自指）
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "cycle detected"

#### Scenario: reparent 到自己的真子孙 → 400 cycle

- **GIVEN** Module A → B → C；reparent A 到 C
- **WHEN** PUT body 含 parentId=C.id
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "cycle detected"

#### Scenario: reparent 后总深度超 max → 400

- **GIVEN** max=3; 树 A → B; 另树 X → Y (depth 2); reparent X 到 B
- **WHEN** PUT body 含 parentId=B.id (X 子树深度 2, B 起算 X 在 3 层，X.Y 在 4 层)
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "max module depth exceeded"

#### Scenario: reparent 到 null（移为顶层）

- **GIVEN** Module M (id=30, parentId=10) 存在
- **WHEN** PUT body 含 parentId=null（显式）
- **THEN** SHALL 返回 200
- **AND** body.parentId SHALL 为 null
- **AND** body.pathName SHALL 仅显示自己的 name

### Requirement: 查询产品模块（含 parent 富化与 path）

后端 SHALL 通过 `GET /api/product-modules/{id}` 返回单 Module 详情（含 parent 与 path 字段）；通过 `GET /api/product-modules?productId=&parentId=&status=&search=&page=&size=` 返回 PageResponse。

#### Scenario: GET 详情含 parent 与 path 字段

- **GIVEN** Module L2 (id=20, parentId=10, productId=1) 关联 Product 1
- **WHEN** `GET /api/product-modules/20`
- **THEN** SHALL 返回 200
- **AND** body 字段集 SHALL 含 `[id, code, name, status, productId, productCode, productName, parentId, parentCode, parentName, pathName, pathCodes, ownerUserId, ownerName, ownerLoginName, ...audit]`
- **AND** body.pathName SHALL 含两段（如 "钱包 / 余额"）

#### Scenario: 按 parentId 过滤列表

- **GIVEN** Module M1 顶层；M2、M3 parentId=M1.id；M4 parentId=null
- **WHEN** `GET /api/product-modules?parentId={M1.id}`
- **THEN** body.total SHALL 为 2
- **AND** body.content[*].parentId SHALL 全为 M1.id

#### Scenario: 按 productId 过滤列表（含全树）

- **GIVEN** Product A 下 5 Module（含子孙）, Product B 下 1 Module
- **WHEN** `GET /api/product-modules?productId={A.id}`
- **THEN** body.total SHALL 为 5

### Requirement: 软删产品模块（双向 FK 保护）

后端 SHALL 通过 `DELETE /api/product-modules/{id}` 标记 `del_flag=1`；若有子 Module（parentId=id）或 Feature 引用 → 409。

#### Scenario: 无引用软删成功

- **GIVEN** Module id=1 无 Feature、无子 Module
- **WHEN** `DELETE /api/product-modules/1`
- **THEN** SHALL 返回 204
- **AND** 后续 GET SHALL 返回 404

#### Scenario: 有 Feature 引用 → 409

- **GIVEN** Module id=1 在 `rainier_feature` 中有 ≥ 1 行 moduleId=1 AND del_flag=0
- **WHEN** `DELETE /api/product-modules/1`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "module has linked features"

#### Scenario: 有子 Module → 409

- **GIVEN** Module id=1 在 `rainier_product_module` 中有 ≥ 1 行 parent_id=1 AND del_flag=0
- **WHEN** `DELETE /api/product-modules/1`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "module has linked sub-modules"

#### Scenario: 同时有 Feature 和子 Module（首先报 Feature）

- **GIVEN** Module id=1 同时有 Feature 引用与子 Module
- **WHEN** `DELETE /api/product-modules/1`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "module has linked features"（不是 sub-modules）
