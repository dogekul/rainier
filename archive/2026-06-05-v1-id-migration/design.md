# v1 id 全栈迁移 — 技术设计

## Context

v1（`archive/2026-06-04-org-tree-and-employee`）shipped with `VARCHAR(32)` UUID hex 主键 + Hibernate `UUIDHexGenerator`。User 决定切到 `BIGINT AUTO_INCREMENT` 后，需要将三表 entity 全链路类型迁移；data 直接 wipe 重生（dev 阶段，无生产数据）。

约束：保持 v1 所有行为可观测结果不变（API 路径 / 状态码 / FK 删保护 / 软删除 / 树缓存 / is_primary demote 全部不动）；只换 id 类型。

## Decisions

### 1. BaseEntity 重写

**方案**：`String id (UUIDHexGenerator)` → `Long id (@GeneratedValue IDENTITY)`；删 `@Column(length=32)`；删除 `@GenericGenerator(strategy="uuid")` 注解。

**为什么**：用户决定 + Hibernate IDENTITY 与 MySQL BIGINT AUTO_INCREMENT 是 1:1 映射；插入返回 generated key 同样工作。

**备选**：`@SequenceGenerator`（MySQL 不支持原生 sequence；Hibernate 模拟开销大）；`@TableGenerator`（性能差）。

### 2. Repository 泛型

**方案**：所有 `JpaRepository<T, String>` / `JpaSpecificationExecutor<T>` → `<T, Long>`；派生方法签名（`existsByLoginName`、`existsByUserIdAndOrganizationId` 等）参数类型跟随。

**为什么**：Spring Data 自动从泛型推断 id 类型。

### 3. 实体字段 + 关联列类型

- `Organization.parentId` String → Long
- `UserOrganization.userId` / `organizationId` String → Long
- 所有 service `getOrThrow(String)` → `(Long)`
- Controller `@PathVariable String id` → `@PathVariable Long id`

Spring MVC 自动 `String → Long` 转换；非数字 path 自动抛 `MethodArgumentTypeMismatchException`（见 §6 新增 handler）。

### 4. DTO 类型

所有 `Detail` / `CreateRequest` / `UpdateRequest` / `MoveRequest` 中的 id 类字段全部 `String` → `Long`；JSON 序列化为数字。

**前端契约影响**：JSON 数字而非字符串；前端 TS 类型同步改 `number`（见 §8）。

### 5. 树缓存 path 内容格式

- 旧：`/2c9580839e95ae40019e95af36320000/...`（每段 32 char）
- 新：`/1/2/3`（每段 1-N digits）
- 服务层 `setPath(parent.getPath() + "/" + o.getId())` 逻辑不变；`o.getId()` 现在是 `Long`，`+` 自动调 `Long.toString()`
- `path LIKE 'prefix/%'` 查询语义不变
- 列长度 VARCHAR(200) 保持，新格式实际只用 100 字符内（千级节点无压力）

### 6. MethodArgumentTypeMismatchException handler 新增

**方案**：`GlobalExceptionHandler` 新增 `@ExceptionHandler(MethodArgumentTypeMismatchException.class)` → 400 JSON `{message: "Invalid id format"}`。

**为什么**：原本通配 `Throwable` handler 会返 500；既然有数字 path 校验需求，把非数字 path 归 400 更语义清晰。

### 7. createBy / updateBy 保持 String

v1 `SecurityFilter` 把 username（mock JWT 的 sub）写到 `RequestAttribute`；`AuditorAware` 取的是 username 字符串，不是用户表主键。

**保持 String**：迁移与 user identity 独立；接真实 IdP 时再评估是否切到 `user.id`。

### 8. 测试断言策略

- `matchesPattern("[0-9a-f]{32}")` → `matchesPattern("\\d+")`
- 工厂方法 `String createRoot(...)` → `Long createRoot(...)`
- `jsonPath("$.id")` 期望类型从 String 切到 Number；`.value(123L)` 形式
- AssertJ 断言 `.isNotNull().hasSize(32)` → `.isPositive()` 或 `.isGreaterThan(0L)`
- "不存在 id" 占位常量从 `"ghost00000000000000000000000000"` → `999_999L`
- `readId(MvcResult)` 内部 `JsonNode.asText()` → `asLong()`

### 9. 前端 TS 类型 sweep

- 所有 `api/*.ts` 中 `id: string` / `parentId: string | null` / `userId: string` 等 → `number` / `number | null`
- `TreeNode.id` / `parentId` → `number | null`
- 函数签名 `getOrganization(id: string)` → `(id: number)`
- store 层无 id（仅 token + user.username），不动
- 前端测试 fixtures id 用整数 `1, 2, 3`

### 10. 数据策略

`docker compose down -v` → 删 named volume `rainier-mysql-data` → `docker compose up -d --build` → Hibernate `ddl-auto=update` 在空 schema 上生成新表；BIGINT id 自动应用。

**为什么**：dev 无生产数据；user 已确认 wipe；省去复杂 SQL 迁移脚本。

**风险**：手动 UI 测试数据丢失（接受）。

## Architecture

```
[v1 path]                                      [migrated path]
String UUID id (32 char hex)                   Long id (BIGINT AUTO_INCREMENT)
        │                                              │
        ▼                                              ▼
 BaseEntity<String>           →            BaseEntity<Long>
 JpaRepository<T,String>      →            JpaRepository<T,Long>
 @PathVariable String id      →            @PathVariable Long id
 DTO.id: String               →            DTO.id: Long
 path "/uuid/uuid"            →            path "/1/2/3"
 (JSON) "id": "abc...32char"  →            "id": 1
 (TS) id: string              →            id: number
```

## Risks / Trade-offs

| 风险 | 缓解 |
|---|---|
| 测试断言遗漏（漏改某处 `[0-9a-f]{32}` 正则） | Phase 5 加 `git grep` SC + 全量 mvn test |
| frontend TS 编译错误连锁（漏改某处 `id: string`） | `npm run build` 全编译；TypeScript strict 模式找出全部 |
| Hibernate ddl-auto=update 在已有 schema 上 ALTER 失败 | 用户已确认 wipe；不在已有 schema 上 ALTER |
| path 内容变短可能让某些字符串断言失败 | Phase 5 跑 E2E + 浏览器三页烟测 |
| 前端 axios baseURL `/api/${id}` 旧时常 string，新时 number 自动 toString | JS 字符串模板对 number 隐式转换；无需修改 |
| URL 路径参数 `@PathVariable Long` 非数字时 400 而非 500 | 决策 §6 新增 `MethodArgumentTypeMismatchException` handler |
| demand-requirement 解冻时需要更新 proposal | 不在本变更范围；那个变更自己负责 |
