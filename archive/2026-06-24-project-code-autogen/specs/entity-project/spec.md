# Capability: entity-project — v0.0.49 project-code-autogen delta (MODIFIED)

> 合并入 canonical `specs/entity-project/spec.md`（Phase 6）。项目编号由「类型前缀 + 自增 id」自动生成。见 [[opportunity]] / [[frontend-scaffold]]。

## MODIFIED Requirements (from change 2026-06-24-project-code-autogen / v0.0.49)

### Requirement: 项目编号自动生成（类型前缀 + 自增 id）

创建项目时 `code` SHALL 由服务端自动生成为 `{类型前缀}-{自增id}`（前缀：CASUAL=`LT` / CORE_FEATURE=`CF` / CORE_TECH=`CT` /
EXTERNAL_DELIVERY=`ED`）。请求体中的 `code` SHALL 被忽略（不再要求、不再查重 409）。编号创建时一次生成、此后不可变（改类型不变）。既有项目 code 不回填。

#### Scenario: 创建对外-交付项目自动编号

- **WHEN** `POST /api/projects` body `{name, ownerUserId, projectType:"EXTERNAL_DELIVERY"}`（无 code）
- **THEN** SHALL 返回 201，code SHALL 匹配 `ED-<id>`

#### Scenario: 轻量项目自动编号

- **WHEN** `POST /api/projects` body `{name, ownerUserId}`（默认 CASUAL，无 code）
- **THEN** SHALL 返回 201，code SHALL 匹配 `LT-<id>`

#### Scenario: 请求 code 被忽略

- **WHEN** `POST /api/projects` body 含 `code:"任意值"`
- **THEN** SHALL 返回 201，code SHALL 仍为自动生成的 `{前缀}-{id}`（非传入值）
