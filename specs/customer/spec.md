# Capability: customer — v0.0.45 (NEW)

> NEW capability（fold-in of change `2026-06-23-gate-artifacts`）。客户实体：商机的甲方主体，支持 CRUD + 客户管理页 +
> 导航；商机创建时可选既有客户或以 customerName 新建/复用（同名去重）。新表 rainier_customer（软删除）。
> 见 [[opportunity]] / [[frontend-scaffold]]。完整实现与测试见 `archive/2026-06-23-gate-artifacts/test-report.md`。

## ADDED Requirements

### Requirement: 客户 CRUD

后端 SHALL 提供 `/api/customers` 的 CRUD：`POST`（必填 `name`；可选 `industry`/`contactName`/`notes`）、`GET`（分页）、
`GET /{id}`、`PUT /{id}`、`DELETE /{id}`（软删除）。`name` SHALL 非空。客户为 all-users 资源。

#### Scenario: 最小创建客户

- **WHEN** `POST /api/customers` body `{name:"X 集团"}`
- **THEN** SHALL 返回 201，body.name SHALL 为 "X 集团"

#### Scenario: 缺名称被拒

- **WHEN** `POST /api/customers` body `{}`（无 name）
- **THEN** SHALL 返回 400

#### Scenario: 软删除

- **GIVEN** 存在客户 C
- **WHEN** `DELETE /api/customers/{C}`
- **THEN** SHALL 返回 204
- **AND** 后续 `GET /api/customers` SHALL NOT 含 C

### Requirement: 商机关联客户（选或建）

创建商机时，`customerId` 非空 SHALL 关联既有客户；为空 SHALL 以 `customerName` 创建或复用同名客户（忽略大小写去重，
`findFirstByNameIgnoreCase`）。详见 [[opportunity]]。

#### Scenario: 同名客户复用

- **GIVEN** 已存在客户「中信集团」
- **WHEN** `POST /api/opportunities` body `{customerName:"中信集团", title:"…"}`（无 customerId）
- **THEN** SHALL 复用既有「中信集团」而非新建重复客户
