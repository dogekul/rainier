# v1 组织维度骨架 切片执行计划

> 共 24 个切片，长程模式串行执行；每个 = 1 个垂直切片（RED → GREEN → REFACTOR）。

| # | ID | 优先级 | TC 覆盖 | 实现目标 | 依赖 |
|---|---|---|---|---|---|
| 1 | B01 | P0 | (contextLoads 仍绿) | 持久化基础设施：pom 加 `spring-boot-starter-data-jpa` / `mysql-connector-j 8.x` / `flyway-core` / `flyway-mysql`；`application.yml` 配 dev datasource + `jpa.hibernate.ddl-auto=validate` + flyway 启用；`application-test.yml` 配 H2 + `ddl-auto=create-drop` + flyway 禁用；`RainierApplication` 加 `@EnableJpaAuditing` | 无 |
| 2 | B02 | P0 | (BaseEntity 单测) | `common.persistence.BaseEntity` (UUID id + 4 个 audit 字段 + `del_flag`) + `AuditorAwareImpl` Bean（读 SecurityContext 或 fallback "system"） | 1 |
| 3 | B03 | P0 | TC-BES-202 | `NotFoundException`(404) + `ConflictException`(409) + `GlobalExceptionHandler` 加 3 个 handler（NotFound / Conflict / `MethodArgumentNotValidException` 含 `fieldErrors[]`） | 1 |
| 4 | B04 | P0 | TC-PAG-001/002/003 | `common.web.PageResponse<T>` + `PageParams`（`@Min(0) page`、`@Min(1) @Max(100) size`，默认 0/20）+ controller @Valid 触发 400 | 3 |
| 5 | B05 | P0 | TC-BES-201（B05+Z01 联合） | `backend/src/main/resources/db/migration/V1__init_org.sql` 完整 DDL（3 表 + 自引用 FK + (parent_id,code) unique + utf8mb4_0900_ai_ci） | 1 |
| 6 | B06 | P0 | TC-BES-203 | `Organization` JPA entity (`@SQLDelete` + `@Where("del_flag=0")`) + `OrganizationRepository` + JPA 单测：persist→delete→`findById` empty 且 DB `del_flag=1` | 2, 5 |
| 7 | B07 | P0 | TC-ORG-001..005 | `OrganizationService.create()`：校验 parent 存在 / `(parent_id, code)` 唯一 / 派生 `path` + `whole_name` / 持久化；`OrganizationController POST /api/organizations` | 4, 6, 3 |
| 8 | B08 | P0 | TC-ORG-006..008 | `GET /api/organizations/{id}` + `GET /api/organizations/tree`（软删过滤 + path 字典序） | 7 |
| 9 | B09 | P0 | TC-ORG-009/010 | `GET /api/organizations`（按 `type` / `parentId` / `search` 过滤 + 分页 + 返回 PageResponse） | 4, 7 |
| 10 | B10 | P0 | TC-ORG-011 | `PUT /api/organizations/{id}` 改 `name` → 服务层级联 `UPDATE` 子孙 `whole_name`（基于 `path LIKE 'self.path%'`） | 7 |
| 11 | B11 | P0 | TC-ORG-012/013 | `PUT /api/organizations/{id}/parent` 改 `parent_id`：防环（newParent.path 不能含 id）→ 自身重算 `path`/`whole_name` → 级联 `UPDATE` 子孙 `REPLACE(path, oldPrefix, newPrefix)` | 7 |
| 12 | B12 | P0 | TC-ORG-014..016 | `DELETE /api/organizations/{id}`：有子节点 → 409；有 user_organization `left_at IS NULL` → 409；否则软删 | 7 |
| 13 | B13 | P0 | (User 软删单测) | `User` JPA entity (`@SQLDelete` + `@Where`) + `UserRepository` + JPA 软删单测 | 2, 5 |
| 14 | B14 | P0 | TC-USR-001..004 | `UserService.create()` + `UserController POST`：uniqueness (login_name / code / email_address) + `@Email` 校验 + 默认 `is_internal=true`、`enabled=true` | 4, 13, 3 |
| 15 | B15 | P0 | TC-USR-005..011 | User CRUD 余下：`GET /{id}` / `GET list`（search + `isInternal` 过滤）/ `PUT`（`login_name` 不可改）/ `DELETE`（FK 保护：user_organization `left_at IS NULL`） | 14 |
| 16 | B16 | P0 | (UO 加载) | `UserOrganization` JPA entity（**不**用 `@SQLDelete`，硬删）+ Repository + 基础测试 | 2, 5 |
| 17 | B17 | P0 | TC-UOR-001..004 | `UserOrganizationService.create()`：uniqueness `(user_id, organization_id)`；FK 校验；**`is_primary=true` 时先 UPDATE demote 当前 user 其他 primary 为 false**；`POST /api/user-organizations` | 4, 12, 15, 16 |
| 18 | B18 | P0 | TC-UOR-005..010 | UO 余下：`GET list`（按 `userId` / `organizationId` 过滤 + enrichment user.name + org.name/type）/ `PUT`（改 role/is_primary/left_at；设新 primary 也触发 demote）/ `DELETE`（硬删） | 17 |
| 19 | F01 | P0 | TC-FES-202/203 | `components/ui/{Table, Pagination, Drawer, ConfirmDialog, TreeSelect}` 5 个通用组件（飞书风格）+ `hooks/usePaginated<T>()` 通用列表态 hook | 无（前端独立） |
| 20 | F02 | P0 | TC-FES-201 | `AppLayout` 加左侧 Sider + 菜单组「组织」（组织节点 / 用户 / 用户-组织关系）+ 在 `AppRoutes` 注册 `/org/*` 占位路由 | 19 |
| 21 | F03 | P0 | TC-ORG-017..019 | `pages/Organization/{index, EditDrawer}.tsx` + `api/organization.ts` + TreeSelect 选父 + 列表/编辑/软删二次确认 | 12, 19, 20 |
| 22 | F04 | P0 | TC-USR-012/013 | `pages/User/{index, EditDrawer}.tsx` + `api/user.ts`（编辑表单**无** password 字段） | 15, 19, 20 |
| 23 | F05 | P0 | TC-UOR-011/012 | `pages/UserOrganization/{index, EditDrawer}.tsx` + `api/userOrganization.ts` + 设 `is_primary=true` 时前端文案提示"将自动 demote 旧主属" | 18, 19, 20 |
| 24 | Z01 | P0 | TC-DRT-201 / TC-TRT-201 / TC-BES-201（联合验证） | E2E 验证：docker compose 重启 → 4 张表存在（含 flyway_schema_history）→ curl /api/health → mvn test 在无 docker 通过；关闭 `archive/2026-06-02-.../pending-adjustments.md` Adjustment #1 | 12, 15, 18, 21, 22, 23 |

## 并行机会

- ⇄ 后端 / 前端：B01-B18 与 F01-F05 完全可并行（前后端无交叉依赖）
- ⇄ B06 / B13 / B16：3 个 entity 加载切片均仅依赖 B02+B05，可并行
- ⇄ F03 / F04 / F05：3 个前端页均仅依赖 F01+F02 + 对应后端 API，可并行

> 长程模式采用串行（避免 git 噪声 + commit 排序困难）。

## 风险点

- **B07 path/whole_name 派生**：依赖 parent.path（必须先 fetch parent）；服务层强一致性
- **B11 移动节点 + 防环**：必须查 newParent.path 是否含 self.id；level 边界（move 到根 → parent_id=null）
- **B17 is_primary demote 并发**：用乐观锁 `@Version` 字段 + retry；并发 race 接受最终一致
- **软删除 + unique 约束**：服务层 `WHERE del_flag=0` 校验，新建复用旧 code 需要先恢复（v0 暂无恢复接口）
- **B05 Flyway V1 SQL**：测试环境 H2 不验证；仅 E2E 在 Z01 验证
- **F03 TreeSelect**：组织树深度若有 1000+ 节点 UI 会卡，v0 暂不考虑虚拟滚动

## 推荐串行执行顺序

```
B01 → B02 → B03 → B04 → B05         (4+ 基础设施)
↓
B06 → B07 → B08 → B09 → B10 → B11 → B12  (7 Organization)
↓
B13 → B14 → B15                     (3 User)
↓
B16 → B17 → B18                     (3 UserOrganization)
↓
F01 → F02 → F03 → F04 → F05         (5 Frontend)
↓
Z01                                  (1 E2E + Cleanup)
```
