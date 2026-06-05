# 组织维度骨架 — 技术设计

## Context

v0 bootstrap 不含持久化（[Adjustment #1](../../archive/2026-06-02-bootstrap-fullstack-scaffold/pending-adjustments.md)）。本变更引入 JPA + Flyway + MySQL，并落地 3 张表覆盖角色卡片 §2.2 全部 5 层组织结构。表结构经用户对 legacy `sys_depart` / `sys_user` 字段集逐一筛选，保留必要字段 + 软删除，丢弃 oa 上游溯源 + 重复状态字段 + 个人隐私字段。

约束：Java 8、Spring Boot 2.7.18、MySQL 8、React 18 + Vite + TS；不动 mock JWT；不引入 MyBatis / Lombok / MapStruct / Ant Design / TanStack Query。

## Decisions

### 1. JPA / Hibernate 引入

**方案**：`spring-boot-starter-data-jpa`（Hibernate 5.6 默认） + `mysql-connector-j 8.x`。
**为什么**：SB 2.7 标配；JpaRepository 节省样板；后续 7 个实体复用同套基础设施。
**备选**：MyBatis（手写 SQL）；jOOQ（学习曲线）。

### 2. Flyway 迁移

**方案**：`flyway-core` + `flyway-mysql` 9.x；迁移目录 `backend/src/main/resources/db/migration/`；启动时自动 migrate；`validate-on-migrate=true`。
**为什么**：声明式版本化；MySQL-specific 语法（utf8mb4、ENGINE=InnoDB）由 `flyway-mysql` 支持。

### 3. 测试 profile DB 策略

**方案**：H2 内存库 + `ddl-auto=create-drop` + Flyway **禁用**；端到端真实 MySQL + Flyway 由 docker compose E2E 覆盖。
**为什么**：H2 不识别 MySQL DDL（utf8mb4、AUTO_INCREMENT 语法、tinyint 形态），Flyway 校验会失败；Hibernate 从 entity 生成 schema 在单测里足够。
**备选**：Testcontainers MySQL（启动慢 30s+）；H2 MySQL 兼容模式（边缘语法仍不通）。

### 4. id 类型：`VARCHAR(32)` UUID hex

**方案**：所有表 id 为 `VARCHAR(32)`；Hibernate `UUIDHexGenerator` 生成 32 字符 hex（无连字符）。
```java
@Id
@GeneratedValue(generator = "uuid")
@GenericGenerator(name = "uuid",
    strategy = "org.hibernate.id.UUIDHexGenerator")
@Column(length = 32, nullable = false, updatable = false)
private String id;
```
**为什么**：与 legacy `sys_depart` / `sys_user` 兼容；分布式无中心生成；URL 含 id 时不暴露顺序。
**备选**：`BIGINT AUTO_INCREMENT`（与 legacy 不兼容）；UUID 带连字符 36 位（占空间多 4 byte）。
**代价**：FK 列也是 `VARCHAR(32)`；索引体积比 BIGINT 大 4×；查询 join 性能略低。

### 5. BaseEntity + JPA Auditing

**方案**：`com.rainier.common.persistence.BaseEntity`（`@MappedSuperclass`）含 id、`create_by` (String)、`create_time` (Instant)、`update_by` (String)、`update_time` (Instant)、`del_flag` (Boolean default false)。
- `@EntityListeners(AuditingEntityListener.class)`
- `RainierApplication` 加 `@EnableJpaAuditing(auditorAwareRef="auditorAware")`
- `AuditorAware<String>` bean 从 SecurityFilter 注入的 username 中取（v0 mock JWT 已有），fallback "system"

### 6. 软删除全局模式

**方案**：所有 entity 共享：
```java
@SQLDelete(sql = "UPDATE rainier_<table> SET del_flag = 1, update_time = NOW(6) WHERE id = ?")
@Where(clause = "del_flag = 0")
@Entity public class Foo extends BaseEntity { ... }
```
- Spring Data 自动在所有 query 后追加 `del_flag=0`
- `findById` / `existsById` / `findAll` 都看不到软删行
- service 层 `delete()` 自动转为 UPDATE
**为什么**：组织/用户变动频繁，硬删丢历史；外键约束下硬删要级联难维护。
**代价**：用 native query 时必须自己加 `WHERE del_flag=0`；`unique` 约束包含软删行 → 需 partial unique 或服务层校验。本 v0 选**服务层校验**（创建前查 `WHERE code=? AND del_flag=0`）。

### 7. PageResponse envelope（沿用 v0 决策）

**方案**：`com.rainier.common.web.PageResponse<T> { List<T> content; int page; int size; long total; }`。
所有 list 接口返回它，不暴露 Spring `Page` 内部字段。

### 8. 分页参数

**方案**：`?page=0&size=20`；`size` 上限 100；`page<0` 或 `size>100` → 400；首版固定 `id DESC` 或 `sort_order ASC, id DESC`（如有）。

### 9. 搜索语义

**方案**：
- `rainier_organization`：`?search=` 对 `(code, name, whole_name)` 做 `LIKE %?%`
- `rainier_user`：`?search=` 对 `(login_name, name, code, email_address)` 做 `LIKE %?%`
- `rainier_user_organization`：不支持 `search`（只按 `user_id`、`organization_id`、`role` 过滤）

大小写不敏感（MySQL `utf8mb4_0900_ai_ci` 默认）。

### 10. 组织树缓存（path / whole_name）

**方案**：服务层在写时维护两列：

- **创建子节点**：从父节点取 `parent.path` / `parent.whole_name`，自身 `path = parent.path + "/" + self.id`，`whole_name = parent.whole_name + "/" + self.name`
- **更新 name**：先更新自身，再递归 SQL `UPDATE rainier_organization SET whole_name = REPLACE(whole_name, oldPrefix, newPrefix) WHERE path LIKE 'oldPrefix%'`
- **移动节点（改 parent_id）**：把"老 path 前缀"在子孙的 path / whole_name 中替换为"新 path 前缀"，单条 `UPDATE ... WHERE path LIKE 'oldPrefix/%'`

**为什么**：业务读远多于写（前端展示全路径、过滤子树）；递归 CTE 在 MySQL 8 可用但慢；缓存让所有读为 O(1)。
**代价**：写入时多 1 次 UPDATE；移动深度 N 的节点要级联 N+1 行；可接受（组织变动稀疏）。
**备选**：递归 CTE 即时查（每次读 N 跳）；闭包表（4 张表，过度设计）。

### 11. 组织 type 软约束

**方案**：服务层默认推荐链 `COMPANY → DEPARTMENT → DOMAIN → TEAM → SUBGROUP`；创建子节点时若违反建议，记 WARN 日志但**允许通过**。
**为什么**：角色卡片 §2.1 §5 "角色退化通过项目创建时显式配置实现" —— 小公司可能 `DEPARTMENT → TEAM` 跳级。
**备选**：DB 触发器强约束（运维负担）；服务层硬拒（与角色卡片冲突）。

### 12. user_organization.role 取值

**方案**：仅 `MEMBER` / `HEAD` 两值；具体职务（部门负责人 vs 团队负责人 vs 小组负责人）由所在 `org.type` 派生：
- `org.type=DEPARTMENT` + `role=HEAD` → 部门负责人
- `org.type=DOMAIN` + `role=HEAD` → 领域负责人
- `org.type=TEAM` + `role=HEAD` → 团队负责人 / PMO 主管
- ...
**为什么**：避免 5 种 HEAD_OF_* 枚举冗余；future 加层级 0 改动。

### 13. user_organization.is_primary 唯一性

**方案**：服务层保证 `(user_id, is_primary=true) ≤ 1 行`。设新 primary 时先 UPDATE 旧 primary = false。
**代价**：MySQL 不支持 partial unique index；并发设两个 primary 极端情况靠 `@Transactional(SERIALIZABLE)` 或乐观锁。v0 用乐观锁 + 重试。

### 14. 异常体系扩展

**方案**：新增 `NotFoundException`(404) + `ConflictException`(409)。`GlobalExceptionHandler` 各加 handler。
新增 `@ExceptionHandler(MethodArgumentNotValidException.class)`：
```json
{
  "message": "Validation failed",
  "fieldErrors": [{"field": "name", "message": "must not be blank"}]
}
```

### 15. Service 事务边界

**方案**：类级 `@Transactional(readOnly = true)` + 写方法上 `@Transactional`；只 service 层加事务注解。

### 16. DTO ↔ Entity mapping

**方案**：每个 service 内手写 `toDto` / `toEntity` 静态方法；不引 MapStruct。

### 17. 后端包结构

**方案**：扁平 `com.rainier.{organization, user, userorganization}` 三子包；与 v0 风格一致。
```
com.rainier
├── organization
│   ├── controller / service / repository / domain / dto
├── user
│   └── ...
├── userorganization
│   └── ...
├── common
│   ├── persistence / BaseEntity, AuditorAware, ...
│   ├── web / PageResponse, PageRequest
│   └── exception / NotFoundException, ConflictException, GlobalExceptionHandler (已有)
└── config (已有：CorsConfig, SecurityFilter, JacksonConfig)
```

### 18. 前端通用组件

**方案**：抽 `components/ui/`:
- `Table.tsx`：受控 columns / dataSource / rowKey
- `Pagination.tsx`：受控 page/size + 总数 + 切页
- `Drawer.tsx`：受控 open / onClose + title + 子表单
- `ConfirmDialog.tsx`：二次确认
- `TreeSelect.tsx`：父组织节点选择器（基于 `GET /api/organizations/tree`）

均**飞书风格**（圆角 6px / `#3370FF` 主色 / 卡片化）；不引 Ant / Arco。

### 19. 前端列表态管理

**方案**：`hooks/usePaginated<T>(fetcher)` 通用 hook，封装 `{page, size, search, items, total, loading, refetch}`。
每个 list page 用此 hook + `useState` 管 selected row + `useEffect` 自动 fetch。
**为什么**：3 个 list page 共享逻辑；后续 7 个实体复用；不引 TanStack Query。

### 20. 前端 3 个独立 CRUD

| 路由 | 内容 |
|---|---|
| `/org/organizations` | 列表 + 父节点 TreeSelect + type 切换 + is_pmo + 编辑 + 软删除（二次确认）|
| `/org/users` | 列表 + 编辑（无密码字段；含 is_internal）+ 软删除 |
| `/org/user-organizations` | 列表 + 新建（user / org / role / is_primary）+ 编辑（改 role / 设 is_primary / 填 left_at）+ 删除 |

`AppLayout` 左侧 Sider 菜单组 "组织"：组织节点 / 用户 / 用户-组织关系。

## Architecture

```
[Browser]
  ├─ /org/organizations ──┐
  ├─ /org/users           ├──> [pages/<Entity>/index.tsx] ──axios──┐
  └─ /org/user-organizations ─┘                                      │
                                                                     ▼
                                                       [Backend Spring Boot]
                                                          │
                                                          ├─ @RestController
                                                          │  └─ @Valid → MethodArgumentNotValidException → 400 fieldErrors
                                                          ├─ @Service (@Transactional)
                                                          │  ├─ FK protect / 子节点存在 → ConflictException → 409
                                                          │  ├─ 树 path/whole_name 维护
                                                          │  └─ is_primary 单一性维护
                                                          ├─ JpaRepository
                                                          │  └─ Soft delete: @SQLDelete + @Where
                                                          ▼
                                              ┌──────────────────────┐
                                              │ MySQL 8 (real)       │
                                              │  rainier_organization│
                                              │  rainier_user        │
                                              │  rainier_user_org    │
                                              │  flyway_schema_hist  │
                                              └──────────────────────┘

  (test profile path)
    @SpringBootTest → H2 in-memory + Hibernate ddl-auto=create-drop + Flyway disabled
```

## Risks / Trade-offs

| 风险 | 缓解 |
|---|---|
| H2 ↔ MySQL DDL 漂移（test 通过但 prod 启动炸）| Flyway `validate-on-migrate=true` + E2E 真 MySQL 覆盖；CI 必跑 compose E2E |
| UUID 主键索引体积大 / join 慢 | 接受；3 表组织数据规模有限（百级-千级）；未来如出现性能问题可加 redis 缓存 |
| 软删除导致 unique 约束 collision（删后建同 code）| 服务层 `WHERE del_flag=0` 校验；与 DB unique 共存（软删行仍占 unique slot，需要重新启用旧 code 时手动改 del_flag 而非新建） |
| 组织树 move 操作级联慢 | 单条 UPDATE ... LIKE 前缀；千级节点级联仍 <10ms；超千级再考虑闭包表 |
| `is_primary` 并发设两个 | 乐观锁（BaseEntity 加 `@Version`）+ 重试；极端 race condition 接受最终一致 |
| Flyway 迁移历史污染 | v0 后续切片只追加 `V2__`、`V3__`；改 V1 需 baseline 重置（README 标注）|
| 不引 MapStruct，DTO 手写 mapper 后期工作量 | 实体达 7+ 时再评估 |
| Sider 导航对未登录路由的兼容 | `AppLayout` 包在 `ProtectedRoute` 内层；未登录不渲染 Sider |
