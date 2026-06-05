# Capability: entity-organization

## ADDED Requirements

### Requirement: 创建组织节点

后端 SHALL 接受合法的 `POST /api/organizations`，含 `parent_id`(可空) / `type` / `code` / `name`，持久化并返回 201。

#### Scenario: 合法 payload 创建根节点

- **GIVEN** backend 已启动，数据库为空
- **WHEN** 客户端发起 `POST /api/organizations`，body `{"parentId":null,"type":"COMPANY","code":"HQ","name":"总公司"}`
- **THEN** 系统 SHALL 返回 HTTP 201
- **AND** 响应头 `Location` SHALL 形如 `/api/organizations/{uuid32}`
- **AND** 响应 body SHALL 含 `id`、`parentId=null`、`type="COMPANY"`、`path` 等于 `/{id}`、`wholeName="总公司"`、`createTime`

#### Scenario: 合法 payload 创建子节点 + 路径派生

- **GIVEN** 已存在节点 `{id:"abc", type:"COMPANY", path:"/abc", wholeName:"总公司"}`
- **WHEN** `POST /api/organizations` body `{"parentId":"abc","type":"DEPARTMENT","code":"RD","name":"研发部"}`
- **THEN** SHALL 返回 201
- **AND** body.path SHALL 为 `/abc/{newId}`
- **AND** body.wholeName SHALL 为 `总公司/研发部`

#### Scenario: 必填字段缺失被拒

- **GIVEN** backend 已启动
- **WHEN** `POST /api/organizations` body 缺 `name`
- **THEN** SHALL 返回 400
- **AND** body SHALL 含 `message="Validation failed"` 与 `fieldErrors[*].field="name"`

#### Scenario: (parent_id, code) 唯一性冲突

- **GIVEN** parent_id=`abc` 下已存在 `code="RD"` 节点
- **WHEN** 再 POST 同 `parentId="abc"` 同 `code="RD"`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "code already exists"

#### Scenario: parent_id 不存在

- **GIVEN** 数据库无 id="ghost" 节点
- **WHEN** POST body `{"parentId":"ghost","type":"TEAM","code":"X","name":"X"}`
- **THEN** SHALL 返回 400
- **AND** body.message SHALL 含 "parent organization not found"

### Requirement: 查询组织节点详情

后端 SHALL 通过 `GET /api/organizations/{id}` 返回单节点完整信息或 404。

#### Scenario: id 存在

- **GIVEN** 数据库存在节点 id=`abc`
- **WHEN** `GET /api/organizations/abc`
- **THEN** SHALL 返回 200
- **AND** body SHALL 含 id / parentId / type / code / name / description / path / wholeName / isPmo / enabled / createTime / updateTime

#### Scenario: id 已被软删

- **GIVEN** 节点 id=`abc` 的 `del_flag=1`
- **WHEN** `GET /api/organizations/abc`
- **THEN** SHALL 返回 404 JSON 含 `message`（软删行对外不可见）

### Requirement: 查询组织树

后端 SHALL 通过 `GET /api/organizations/tree` 返回扁平 list，前端 O(n) 装配树。

#### Scenario: tree 端点返回所有非软删节点

- **GIVEN** 数据库 5 个节点（1 COMPANY + 2 DEPARTMENT + 2 TEAM），其中 1 个 TEAM 已软删
- **WHEN** `GET /api/organizations/tree`
- **THEN** SHALL 返回 200
- **AND** body SHALL 为数组，长度 = 4
- **AND** 每项 SHALL 含 `id`、`parentId`、`type`、`code`、`name`、`isPmo`、`path`、`wholeName`
- **AND** 数组 SHALL 按 path 字典序排列

### Requirement: 组织列表带筛选分页

后端 SHALL 通过 `GET /api/organizations?type=&parentId=&search=&page=&size=` 返回 PageResponse。

#### Scenario: 按 type 筛选

- **GIVEN** 数据库 1 COMPANY + 3 DEPARTMENT + 5 TEAM
- **WHEN** `GET /api/organizations?type=DEPARTMENT`
- **THEN** body.total SHALL 为 3
- **AND** body.content 全部 `type="DEPARTMENT"`

#### Scenario: search 模糊匹配 whole_name

- **GIVEN** 数据库存在 whole_name="总公司/研发部"、"总公司/测试部"、"总公司/销售部"
- **WHEN** `GET /api/organizations?search=研发`
- **THEN** body.total SHALL 为 1
- **AND** body.content[0].wholeName SHALL 含 "研发"

### Requirement: 更新组织名

后端 SHALL 通过 `PUT /api/organizations/{id}` 修改 name / description / is_pmo / enabled；改 name 时级联更新子孙 whole_name。

#### Scenario: 修改 name 后子孙 whole_name 级联

- **GIVEN** 节点树：A(总公司) → B(研发部) → C(后端组)；whole_name 分别为 `总公司`、`总公司/研发部`、`总公司/研发部/后端组`
- **WHEN** `PUT /api/organizations/{B.id}` body `{"name":"研发中心"}`
- **THEN** SHALL 返回 200
- **AND** B.wholeName SHALL 为 `总公司/研发中心`
- **AND** C.wholeName SHALL 为 `总公司/研发中心/后端组`
- **AND** A.wholeName SHALL 不变

### Requirement: 移动组织节点

后端 SHALL 通过 `PUT /api/organizations/{id}/parent` 改 parent_id；级联重算自身及全部子孙的 path / whole_name。

#### Scenario: 移动节点级联

- **GIVEN** 树：A → B → C；B.path="/A/B"；C.path="/A/B/C"
- **WHEN** `PUT /api/organizations/{B.id}/parent` body `{"parentId":"A2"}`（A2 是另一个根）
- **THEN** SHALL 返回 200
- **AND** B.path SHALL 为 `/A2/B`
- **AND** C.path SHALL 为 `/A2/B/C`
- **AND** B.wholeName / C.wholeName SHALL 按新链重算

#### Scenario: 移动到自己的子孙被拒（防环）

- **GIVEN** 树：A → B → C
- **WHEN** `PUT /api/organizations/{A.id}/parent` body `{"parentId":"C"}`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "cannot move to descendant"

### Requirement: 软删组织节点（FK 保护）

后端 SHALL 通过 `DELETE /api/organizations/{id}` 标记 `del_flag=1`；若有子节点或关联 user_organization → 409。

#### Scenario: 无子节点 + 无关联用户 → 软删成功

- **GIVEN** 节点 id=`leaf`，无子节点，无 user_organization
- **WHEN** `DELETE /api/organizations/leaf`
- **THEN** SHALL 返回 204
- **AND** 后续 `GET /api/organizations/leaf` SHALL 返回 404
- **AND** DB 中该行 SHALL `del_flag=1` 仍存在

#### Scenario: 有子节点 → 拒绝软删

- **GIVEN** 节点 id=`parent` 下有 ≥1 个未软删子节点
- **WHEN** `DELETE /api/organizations/parent`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "has child organizations"

#### Scenario: 有关联用户 → 拒绝软删

- **GIVEN** 节点 id=`team1` 在 user_organization 表中有 ≥1 行 left_at IS NULL
- **WHEN** `DELETE /api/organizations/team1`
- **THEN** SHALL 返回 409
- **AND** body.message SHALL 含 "has assigned users"



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
