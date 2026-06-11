# Capability: entity-audit-log

> NEW capability from v0.0.15-audit-log (2026-06-11).
> Append-only 变更流水：Spring AOP 切面在每个 entity service 的 create/update/delete 成功返回后，
> 同事务写入一条审计行（actor / entityType / entityId / action / summary / createTime）。
> 不可改不可删（无 POST/PUT/DELETE 端点）。读 API 分页倒序 + 多过滤。

## ADDED Requirements

### Requirement: 自动审计实体写操作

后端 SHALL 通过 AOP 切面在任意 `*Service.create/update/delete` **成功返回后**，于同一事务内写入一条 `rainier_audit_log` 行，记录 actor / entityType / entityId / action / summary。

#### Scenario: 创建实体产生 CREATE 审计行

- **GIVEN** 已登录用户 actor="alice"，且数据库无相关审计行
- **WHEN** `POST /api/requirements` 成功创建 Requirement（返回 id=N）
- **THEN** SHALL 新增 1 条 audit_log
- **AND** 该行 action SHALL 为 "CREATE"
- **AND** 该行 entityType SHALL 为 "REQUIREMENT"
- **AND** 该行 entityId SHALL 为 N
- **AND** 该行 actor SHALL 为 "alice"

#### Scenario: 更新实体产生 UPDATE 审计行

- **GIVEN** 已存在 Requirement id=N
- **WHEN** `PUT /api/requirements/N` 成功更新
- **THEN** SHALL 新增 1 条 audit_log
- **AND** 该行 action SHALL 为 "UPDATE"
- **AND** 该行 entityId SHALL 为 N

#### Scenario: 删除实体产生 DELETE 审计行

- **GIVEN** 已存在 Requirement id=N
- **WHEN** `DELETE /api/requirements/N` 成功删除
- **THEN** SHALL 新增 1 条 audit_log
- **AND** 该行 action SHALL 为 "DELETE"
- **AND** 该行 entityId SHALL 为 N

#### Scenario: summary 文本格式

- **GIVEN** 已登录用户
- **WHEN** 成功创建 Requirement id=N
- **THEN** 对应审计行 summary SHALL 为 `"CREATE REQUIREMENT#" + N`

### Requirement: 失败的写操作不记审计

后端 SHALL 仅在业务写方法**正常返回**时记录审计；方法抛异常（校验失败 / 冲突等）时 SHALL **不**产生审计行。

#### Scenario: 业务校验失败不记审计

- **GIVEN** 已存在 code="REQ-DUP" 的 Requirement
- **WHEN** 再次 `POST /api/requirements` 用同 code（业务抛 409）
- **THEN** SHALL 返回 409
- **AND** SHALL **不**新增任何 action="CREATE" 且该 code 对应的 audit_log 行
- **AND** audit_log 总行数 SHALL 与请求前相同

### Requirement: 审计写入与业务同事务

后端审计写入 SHALL 与被审计的业务操作处于同一事务；业务事务回滚时审计行 SHALL 一并回滚。

#### Scenario: 业务方法异常导致事务回滚时无审计残留

- **GIVEN** 一个被审计的 service 写方法在持久化后、返回前抛出运行时异常（测试用注入或既有冲突路径）
- **WHEN** 该写操作触发事务回滚
- **THEN** 业务行 SHALL 不存在
- **AND** 对应审计行 SHALL 不存在（无幽灵审计）

### Requirement: entityType 对复合类名正确推导

后端 SHALL 从 service 声明类名推导 entityType（去 `Service` 后缀 + camelCase 转 SCREAMING_SNAKE）。

#### Scenario: 复合名 service 推导

- **GIVEN** 已登录用户
- **WHEN** `POST /api/sprint-features` 成功创建 SprintFeatureLink
- **THEN** 对应审计行 entityType SHALL 为 "SPRINT_FEATURE_LINK"
- **AND** action SHALL 为 "CREATE"

### Requirement: 审计读操作不被审计（无自递归）

后端查询审计日志的端点 SHALL **不**产生新的审计行。

#### Scenario: 查询审计日志不自增审计

- **GIVEN** audit_log 当前有 K 行
- **WHEN** `GET /api/audit-logs` 查询一次
- **THEN** SHALL 返回 200
- **AND** audit_log 行数 SHALL 仍为 K（查询不写审计）

### Requirement: 查询审计日志（分页倒序 + 多过滤）

后端 SHALL 通过 `GET /api/audit-logs?actor=&entityType=&entityId=&action=&page=&size=` 返回 PageResponse（按 createTime 倒序）；通过 `GET /api/audit-logs/{id}` 返回单条。

#### Scenario: 按 entityType + entityId 过滤

- **GIVEN** 对 Requirement id=5 有 2 条审计行，对其它实体有若干行
- **WHEN** `GET /api/audit-logs?entityType=REQUIREMENT&entityId=5`
- **THEN** body.total SHALL 为 2
- **AND** body.content 每行 SHALL entityType="REQUIREMENT" 且 entityId=5

#### Scenario: 按 actor 过滤

- **GIVEN** actor="alice" 产生 3 条、actor="system" 产生 1 条审计行
- **WHEN** `GET /api/audit-logs?actor=alice`
- **THEN** body.total SHALL 为 3
- **AND** body.content 每行 actor SHALL 为 "alice"

#### Scenario: 倒序返回（最新在前）

- **GIVEN** 先后产生审计行 A、B（B 较新）
- **WHEN** `GET /api/audit-logs`
- **THEN** body.content[0] SHALL 为 B（createTime 较新者在前）

#### Scenario: 单条查询

- **GIVEN** 审计行 id=L 存在
- **WHEN** `GET /api/audit-logs/L`
- **THEN** SHALL 返回 200
- **AND** body 字段集 SHALL 含 `[id, actor, entityType, entityId, action, summary, createTime]`

### Requirement: 审计日志 append-only（无写端点）

后端 SHALL **不**提供创建 / 修改 / 删除审计日志的端点。

#### Scenario: 无写端点

- **GIVEN** 应用已启动
- **WHEN** 对 `/api/audit-logs` 发起 `POST` / `PUT` / `DELETE`
- **THEN** SHALL **不**命中任何写处理器（响应非 2xx；实际为 500，因既有全局 catch-all 把未处理的 405 映射为 500 — 见 PA-1，append-only 契约不受影响）
- **AND** `AuditLogController` 源码 SHALL 不含任何写映射（无 POST/PUT/DELETE handler）
