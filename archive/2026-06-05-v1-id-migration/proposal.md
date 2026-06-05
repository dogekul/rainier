# v1 表 id 全栈迁移 (VARCHAR(32) UUID → BIGINT AUTO_INCREMENT)

## Why

用户在做"诉求/需求"变更（[changes/2026-06-05-demand-requirement](../2026-06-05-demand-requirement/)）的 Phase 1 中决定"未来所有新表 id 用自增整数"，**随后又决定历史表也同步迁移**，避免两代际 schema 长期并存带来的隐性维护成本：

- 双基类 `BaseEntity (String UUID)` + `BaseAutoIdEntity (Long)` 长期共存 → 团队心智成本
- 跨代 FK 类型异构（`varchar(32)` ↔ `bigint`） → join 性能差 + 文档/ORM 复杂度上升
- 业务码 / URL / 日志中 32 字符 UUID 难记 → 运维体验差
- 现在 v0 阶段无生产数据，迁移代价最低；越晚做越贵

本变更**只做迁移**，不引入新业务能力。迁移交付后即解锁 demand-requirement 变更（其 proposal 也会被简化为单一 `BaseEntity` 世界观）。

## What Changes

### A. 数据库

- 数据策略：`docker compose down -v` 清掉 `rainier-mysql-data` 卷；重启后 Hibernate `ddl-auto=update` 生成新 schema
- 不写 SQL 迁移脚本（v0 阶段无生产数据，可接受丢失 E2E + 手工测试创建的行）
- 3 张 v1 表的物理变化：
  - `rainier_organization.id` `VARCHAR(32)` → `BIGINT AUTO_INCREMENT`
  - `rainier_organization.parent_id` `VARCHAR(32)` → `BIGINT NULL`
  - `rainier_user.id` 同上
  - `rainier_user_organization.id` / `user_id` / `organization_id` 同上
  - 所有 unique 约束 / FK 约束自动跟随
  - `rainier_organization.path` 列长度不变（仍 `VARCHAR(200)`），但内容从 `/uuid32/uuid32` 变成 `/1/2/3`

### B. 后端公共基类

- `com.rainier.common.persistence.BaseEntity`：
  - 字段类型：`private String id` → `private Long id`
  - 生成器：`@GenericGenerator(strategy="uuid")` → 删；用 `@GeneratedValue(strategy = GenerationType.IDENTITY)`
  - `@Column(length=32, ...)` → 删长度声明（BIGINT 不需要 length）
- 不新建 `BaseAutoIdEntity`（之前 demand-requirement 提议的双基类设想直接废弃）

### C. 后端 3 套实体链路（全部类型替换）

- **Organization**：`id: Long` / `parentId: Long`；service/controller `@PathVariable String id` → `@PathVariable Long id`；树缓存逻辑（path/whole_name 派生与级联）逻辑不变，只是 path 内容变短
- **User**：`id: Long`；service/controller 同上
- **UserOrganization**：`id: Long` / `userId: Long` / `organizationId: Long`；service 中 `existsByUserIdAndOrganizationId(String,String)` → `(Long,Long)` 等所有 Repository 派生方法签名
- **Repository 泛型**：`JpaRepository<T, String>` → `JpaRepository<T, Long>`（含 `JpaSpecificationExecutor<T>`）
- **DTO**：`OrganizationDetail.id/parentId`、`UserDetail.id`、`UserOrgDetail.id/userId/organizationId` 全部 `String` → `Long`
- **请求体**：`OrganizationCreateRequest.parentId: String` → `Long`；`OrganizationMoveRequest.parentId: String` → `Long`；`UserOrgCreateRequest.userId/organizationId: String` → `Long`；`UserOrgUpdateRequest.organizationId: String` → `Long`
- **Native query**：`OrganizationRepository.countRawById(String) / rawDelFlag(String)` 参数 `String` → `Long`
- **Audit auditor**：`createBy` / `updateBy` 仍是 `String`（v1 mock JWT 注入的 username，不是数字 id）—— **不变**

### D. 后端测试（全部断言修正）

- 9 个测试类涉及 id 断言：
  - `OrganizationControllerCreateTest` / `OrganizationControllerQueryTest` / `OrganizationDeleteFkTest`
  - `UserControllerTest`
  - `UserOrganizationControllerTest`
  - `OrganizationRepositoryTest`
  - `GlobalExceptionHandlerTest`（NotFound message 含 id 字符串）
- 改造点：
  - `matchesPattern("[0-9a-f]{32}")` → `matchesPattern("\\d+")`
  - `body("id", matchesPattern(...))` 改为数字断言
  - `header().string("Location", matchesPattern("/api/.../[0-9a-f]{32}"))` → `/\\d+`
  - 工厂方法返回类型 `String createRoot(...)` → `Long createRoot(...)`
  - `readId()` 解析 `id` 时 `JsonNode.asText()` → `asLong()`
  - 测试中"不存在 id"占位由 `"ghost00000000000000000000000000"` → `999_999L`
- 测试**总数不变**（59 用例），只改类型/断言；不增不减

### E. 前端 TS 类型 + 调用链

- `frontend/src/api/organization.ts`：`Organization.id/parentId: string` → `number`；`OrganizationCreate.parentId?: string | null` → `number | null`；函数签名 `getOrganization(id: string)` → `number` 等所有 5 + 2 接口
- `frontend/src/api/user.ts`：`User.id: string` → `number`；函数签名同步
- `frontend/src/api/userOrganization.ts`：`UserOrganization.id/userId/organizationId: string` → `number`；含 enrichment 字段保留
- `frontend/src/hooks/usePaginated.ts`：泛型类型不变（不锁 id 字段）
- `frontend/src/components/ui/TreeSelect.tsx`：`TreeNode.id/parentId: string` → `number | null`（影响 OrganizationsPage、UserOrganizationsPage）
- `frontend/src/pages/Organization/{index, EditDrawer, OrganizationsPage}.tsx`：parentId 状态从 `string | null` → `number | null`；删除 confirm 等用到 id 的地方
- `frontend/src/pages/User/UsersPage.tsx`：编辑/删除路径
- `frontend/src/pages/UserOrganization/UserOrganizationsPage.tsx`：userId / organizationId 状态
- `frontend/src/AppRoutes.tsx`：路由不含 `:id`（v1 没用 URL 路径参数），不动

### F. 前端测试

- v1 前端 4 个新增测试（Table / TreeSelect / AppLayout / Login）：
  - `Table.test.tsx`：fixtures `id: '1', '2'` → `id: 1, 2`
  - `TreeSelect.test.tsx`：nodes `id: 'a', 'b', 'c'` → `id: 1, 2, 3`；`parentId: 'a'` → `1`
  - 其余不动
- v0 测试（`ProtectedRoute / Login / tokens / auth`）不动 —— 不涉及业务实体

### G. 不在本变更（显式排除）

- ❌ 任何新业务能力或新实体
- ❌ 数据迁移脚本（用户已确认 wipe）
- ❌ 修改 v1 archive 文档（archive immutable）
- ❌ 修改 repo specs/* 文件（已合并的规范保留作为该时刻的快照；本变更不更新 specs 文本，让 git 历史 + 本 proposal 作为现实状态来源）
- ❌ Project / demand / requirement 等下游实体
- ❌ demand-requirement 变更恢复（独立 follow-up）

## Capabilities

### Modified Capabilities

- `backend-scaffold`：`BaseEntity` id 类型与生成策略变更
- `entity-organization`：id / parentId / path 内容类型变更
- `entity-user`：id 类型变更
- `entity-user-organization`：id / userId / organizationId 类型变更
- `frontend-scaffold`：TS 类型约定变更（全局 id 改 number）

### New Capabilities

- （无 —— 纯重构）

## Impact

**代码层面**：

- 后端约修改 25 个文件（BaseEntity + 3 entity + 3 repo + 3 service + 3 controller + ~10 DTO + 6 测试类）
- 前端约修改 12 个文件（3 api/*.ts + 3 page/index.tsx + 3 EditDrawer + TreeSelect + 2 测试）
- 不新增公共抽象（不引入 BaseAutoIdEntity，废弃 demand-requirement 中的设想）
- 不动 `frontend/src/api/client.ts` / `store/auth.ts` / v0 entity（auth/health/Login/Home 全部无业务 id）
- 不动 `backend/pom.xml` / `application*.yml` / `docker-compose.yml`
- 不动 `archive/2026-06-04-org-tree-and-employee/` 任何文件（已交付的历史记录）

**配置层面**：

- 无依赖、无 YAML、无环境变量变化

**基础设施**：

- 必须执行 `docker compose down -v`（删 `rainier-mysql-data` volume）→ 接受所有 dev 数据丢失
- 重启后 `mysql:8.0` 容器以新 schema（BIGINT id）重生

## Success Criteria

- [ ] `mvn -ntp test` 全绿；总数 ≥ 59（数量与 v1 verify 时相同；不增不减）
- [ ] `mvn -ntp spotless:check checkstyle:check` 0 违规
- [ ] `npm test -- --run` 全绿；总数 ≥ 11
- [ ] `npm run build` 无 type error；`npm run lint` 0 错误
- [ ] `git grep "private String id"` 在 `backend/src/main/java/com/rainier/common/persistence/BaseEntity.java` 中无匹配
- [ ] `git grep "JpaRepository<.*, String>"` 在 backend 主代码中无匹配（必须是 `<T, Long>`）
- [ ] `git grep '@PathVariable String id'` 在 backend 主代码中无匹配
- [ ] `git grep -E '\bid:\s*string\b'` 在 `frontend/src/api/` 中无匹配
- [ ] `git grep '\[0-9a-f\]{32}'` 在 backend 测试中无匹配
- [ ] `docker compose down -v && docker compose up -d --build` 后，`docker exec rainier-mysql mysql ... -e "DESCRIBE rainier_organization"` 显示 `id BIGINT` 而非 `varchar(32)`
- [ ] 同上对 `rainier_user` / `rainier_user_organization` 也成立
- [ ] `curl -X POST /api/organizations -d '{"type":"COMPANY","code":"X","name":"X"}'` 返回 201 + body.id 是数字（不是 32 字符 hex）
- [ ] 浏览器 `/org/organizations` / `/org/users` / `/org/user-organizations` 三页可见列表 + 新建 + 编辑 + 删除（含 TreeSelect 选父）
- [ ] `changes/2026-06-05-demand-requirement/.stdd.yaml` 中 `phase.current: suspended` 与 `blocks_on: 2026-06-05-v1-id-migration` 标注保留不动（本变更交付后由那个变更自己解除阻塞）
