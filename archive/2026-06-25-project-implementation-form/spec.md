# spec — Project Implementation Form (D1, v0.0.89)

## Scenarios

### PIF-001 PUT 首次创建（projectId 合法 + scopeMarkdown 非空）→ 200，返回新记录 id
- GIVEN 一个已存在的 Project
- WHEN `PUT /api/projects/{id}/implementation` with `{scopeMarkdown:"…", estimatedManDays:60}`
- THEN 200，body 含 `id`, `projectId`, `scopeMarkdown`, `estimatedManDays=60`
- 数据库新增 1 条 `rainier_project_implementation`

### PIF-002 PUT 重复（同 projectId）→ 幂等，id 不变（upsert 语义）
- GIVEN PIF-001 已成立
- WHEN 同 projectId 再次 PUT 不同 scope
- THEN 200，返回 id 与第一次相同；scope 已更新

### PIF-003 GET 不存在 → 404
- WHEN `GET /api/projects/{id}/implementation` 该 projectId 从未 PUT
- THEN 404

### PIF-004 GET 存在 → 200
- GIVEN PIF-001 已成立
- WHEN `GET`
- THEN 200，字段完整

### PIF-005 PUT projectId 不存在 → 400
- WHEN `PUT /api/projects/{99999}/implementation`
- THEN 400 (`BadRequestException`: project not found)

### PIF-006 PUT scopeMarkdown 空 → 400
- WHEN body 中 `scopeMarkdown` 为 null 或空白
- THEN 400 (Bean Validation `@NotBlank`)
