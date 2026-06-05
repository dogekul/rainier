# v1 组织维度骨架 任务清单

> 24 切片 × 3-6 子任务；按拓扑依赖排序。

## 1. 持久化基础设施（B01-B05，P0）

- [ ] 1.1 backend/pom.xml 加依赖：`spring-boot-starter-data-jpa`、`mysql-connector-j` 8.x、`flyway-core` 9.22.x、`flyway-mysql`（B01）
- [ ] 1.2 application.yml（dev profile）配 `spring.datasource.{url,username,password}`、`spring.jpa.hibernate.ddl-auto=validate`、`spring.flyway.enabled=true`（B01）
- [ ] 1.3 application-test.yml 配 `spring.datasource.url=jdbc:h2:mem:rainier;...`、`ddl-auto=create-drop`、`spring.flyway.enabled=false`（B01）
- [ ] 1.4 RainierApplication 加 `@EnableJpaAuditing(auditorAwareRef="auditorAware")`（B01）
- [ ] 1.5 `mvn test` 在 test profile 全绿（B01 GREEN）
- [ ] 2.1 `com.rainier.common.persistence.BaseEntity` (`@MappedSuperclass` + `@EntityListeners(AuditingEntityListener.class)`)：id (UUIDHexGenerator) + create_by/time + update_by/time + del_flag (Boolean, default false)（B02）
- [ ] 2.2 `AuditorAwareImpl implements AuditorAware<String>` Bean（从 SecurityContext / RequestAttribute 取 username，fallback "system"）（B02）
- [ ] 3.1 `common.exception.NotFoundException` (RuntimeException, message)（B03）
- [ ] 3.2 `common.exception.ConflictException` (RuntimeException, message)（B03）
- [ ] 3.3 `GlobalExceptionHandler` 加 `@ExceptionHandler(NotFoundException)` → 404 JSON；`ConflictException` → 409 JSON；`MethodArgumentNotValidException` → 400 JSON 含 `{message:"Validation failed", fieldErrors:[{field, message}]}`（B03）
- [ ] 3.4 MockMvc 测试：404 / 409 / 400 fieldErrors 三条（B03 GREEN）
- [ ] 4.1 `common.web.PageResponse<T> { List<T> content; int page; int size; long total; }` POJO（B04）
- [ ] 4.2 `common.web.PageParams { @Min(0) Integer page=0; @Min(1) @Max(100) Integer size=20; String search; }` DTO + controller `@Valid`（B04）
- [ ] 4.3 MockMvc 测试：默认值 / size=101 → 400 / 字段稳定（B04 GREEN）
- [ ] 5.1 `backend/src/main/resources/db/migration/V1__init_org.sql` 写 3 表完整 DDL（B05）
- [ ] 5.2 Flyway 启动通过（dev profile 需真实 mysql，本地手动验证；E2E 在 Z01）

## 2. Organization 实体（B06-B12，P0）

- [ ] 6.1 `organization.domain.Organization` extends BaseEntity，`@Table(name="rainier_organization")` + `@SQLDelete(sql="UPDATE rainier_organization SET del_flag=1, update_time=NOW(6) WHERE id=?")` + `@Where(clause="del_flag=0")`（B06）
- [ ] 6.2 `OrganizationType` enum（COMPANY/DEPARTMENT/DOMAIN/TEAM/SUBGROUP），`@Enumerated(STRING)`（B06）
- [ ] 6.3 `OrganizationRepository extends JpaRepository<Organization, String>` + `existsByParentIdAndCode` / `findByParentIdAndCodeAndDelFlagFalse` / `findAllByOrderByPathAsc` 等查询方法（B06）
- [ ] 6.4 JPA 单测：persist → delete → findById empty 且 DB 行 del_flag=1（B06 GREEN）
- [ ] 7.1 `dto.OrganizationCreateRequest`（`@NotBlank code/name`、可空 parentId/description/isPmo）+ `OrganizationDetail` + `OrganizationListItem`（B07）
- [ ] 7.2 `OrganizationService.create(req)`：校验 parent 存在 → 校验 `(parent_id, code)` 未占用 → 派生 path = `parent.path + "/" + newId` / whole_name = `parent.wholeName + "/" + req.name`（B07）
- [ ] 7.3 `OrganizationController POST /api/organizations`（B07）
- [ ] 7.4 MockMvc 测试 5 条：根节点 / 子节点 / 缺 name / 重复 code / parent 不存在（B07 GREEN）
- [ ] 8.1 `OrganizationService.findById(id)` 抛 NotFoundException；`.findTree()` 排除软删 + path 字典序（B08）
- [ ] 8.2 `GET /api/organizations/{id}` + `GET /api/organizations/tree`（B08）
- [ ] 8.3 MockMvc 测试 3 条：detail 200 / detail 404 / tree 排除软删 + 排序（B08 GREEN）
- [ ] 9.1 `OrganizationService.list(type, parentId, search, pageParams)`：JPQL 含 `LIKE` whole_name/code/name + 分页（B09）
- [ ] 9.2 `GET /api/organizations?type=&parentId=&search=&page=&size=`（B09）
- [ ] 9.3 MockMvc 测试 2 条：type 过滤 / search 匹配 whole_name（B09 GREEN）
- [ ] 10.1 `OrganizationService.update(id, req)` 改 name → 1 条 `UPDATE rainier_organization SET whole_name = REPLACE(whole_name, oldPath, newPath) WHERE path LIKE 'self.path/%'`（注意：先级联子孙再改自身，避免 oldPath 不匹配）（B10）
- [ ] 10.2 `PUT /api/organizations/{id}`（B10）
- [ ] 10.3 MockMvc 测试：3 层级联（A/B/C）改 B.name 后 B.wholeName + C.wholeName 正确（B10 GREEN）
- [ ] 11.1 `OrganizationService.move(id, newParentId)`：防环（newParent.path 不能含 id）→ 计算 oldPathPrefix / newPathPrefix → 单条 UPDATE `REPLACE(path, oldPrefix, newPrefix)` + `REPLACE(whole_name, oldNamePrefix, newNamePrefix)`（B11）
- [ ] 11.2 `PUT /api/organizations/{id}/parent`（B11）
- [ ] 11.3 MockMvc 测试 2 条：移动级联 + 防环 409（B11 GREEN）
- [ ] 12.1 `OrganizationService.delete(id)`：查 子节点 count > 0 → ConflictException；查 user_organization `left_at IS NULL` count > 0 → ConflictException；否则调 repo.delete（B12）
- [ ] 12.2 `DELETE /api/organizations/{id}`（B12）
- [ ] 12.3 MockMvc 测试 3 条：无子无关联 → 204 + DB del_flag=1；有子 → 409；有关联 → 409（B12 GREEN）

## 3. User 实体（B13-B15，P0）

- [ ] 13.1 `user.domain.User` extends BaseEntity，`@Table(name="rainier_user")` + `@SQLDelete` + `@Where`（B13）
- [ ] 13.2 `UserRepository` + `existsByLoginNameAndDelFlagFalse` / `existsByCodeAndDelFlagFalse` / `existsByEmailAddressAndDelFlagFalse`（B13）
- [ ] 13.3 JPA 单测：persist + soft delete（B13 GREEN）
- [ ] 14.1 `dto.UserCreateRequest`（`@NotBlank login_name/name`、可空 code/email_address/is_internal/enabled，`@Email` email）+ DTO（B14）
- [ ] 14.2 `UserService.create(req)`：uniqueness 校验 → save（B14）
- [ ] 14.3 `UserController POST /api/users`（B14）
- [ ] 14.4 MockMvc 测试 4 条：最小 payload + uniqueness × 3（login_name/code/email）+ email 格式（B14 GREEN）
- [ ] 15.1 GET detail + GET list (search + isInternal filter) + PUT update (login_name 不可改) + DELETE (FK 校验 user_organization 中 left_at IS NULL)（B15）
- [ ] 15.2 MockMvc 测试 7 条（B15 GREEN）

## 4. UserOrganization 实体（B16-B18，P0）

- [ ] 16.1 `userorganization.domain.UserOrganization` extends BaseEntity（**不** `@SQLDelete`，硬删）+ `@Table(name="rainier_user_organization")`（B16）
- [ ] 16.2 `UserOrganizationRepository`：含 `findByUserIdAndIsPrimaryTrue` / `findByUserIdAndLeftAtIsNull` / `findByOrganizationIdAndLeftAtIsNull` / `existsByUserIdAndOrganizationId`（B16）
- [ ] 16.3 JPA 单测：persist + delete (hard delete 验证)（B16）
- [ ] 17.1 `dto.UserOrgCreateRequest`（`@NotBlank user_id/org_id`、默认 role=MEMBER、默认 is_primary=false、默认 joined_at=now）（B17）
- [ ] 17.2 `UserOrganizationService.create(req)`：FK 校验 → `(user_id, org_id)` uniqueness → **若 is_primary=true**：先 `UPDATE user_organization SET is_primary=false WHERE user_id=? AND is_primary=true` → save（带 `@Version` 乐观锁）（B17）
- [ ] 17.3 `POST /api/user-organizations`（B17）
- [ ] 17.4 MockMvc 测试 4 条：合法 / 重复 / FK 不存在 / is_primary auto-demote（B17 GREEN）
- [ ] 18.1 `GET list` 含 enrichment（join user + organization 取 name/type 拼装）（B18）
- [ ] 18.2 PUT update（role / is_primary / left_at；is_primary=true 触发 demote）（B18）
- [ ] 18.3 DELETE 硬删（B18）
- [ ] 18.4 MockMvc 测试 6 条（B18 GREEN）

## 5. 前端通用组件（F01，P0）

- [ ] 19.1 `frontend/src/components/ui/Table.tsx` 受控 columns + dataSource + rowKey，飞书风格（B 用 `--rainier-*` token）
- [ ] 19.2 `Pagination.tsx`（page / size / total / onPageChange）
- [ ] 19.3 `Drawer.tsx`（受控 open + onClose + title + children）
- [ ] 19.4 `ConfirmDialog.tsx`（二次确认）
- [ ] 19.5 `TreeSelect.tsx`（数据源接收扁平 list 含 parentId，前端装树展示）
- [ ] 19.6 `hooks/usePaginated.ts`（封装 page/size/search/items/total/loading/refetch）
- [ ] 19.7 RTL 测试：Table 渲染 columns + rows；TreeSelect 树面板（F01 GREEN）

## 6. 前端 AppLayout + 路由（F02，P0）

- [ ] 20.1 `AppLayout.tsx` 加左侧 Sider 240px，菜单组「组织」3 项
- [ ] 20.2 `AppRoutes.tsx` 注册 `/org/organizations` / `/org/users` / `/org/user-organizations`；`/org` 重定向到 `/org/organizations`
- [ ] 20.3 RTL 测试：Sider 含菜单组 + 跳转（F02 GREEN）

## 7. 前端 organizations 页（F03，P0）

- [ ] 21.1 `api/organization.ts` 5 个函数 + tree 接口（list / get / create / update / move / remove / tree）
- [ ] 21.2 `pages/Organization/index.tsx` 列表 + 搜索 + 分页 + Tree 视图 toggle（v0 先列表）+ 新建/编辑按钮 + 删除二次确认
- [ ] 21.3 `pages/Organization/EditDrawer.tsx` 表单：parentId(TreeSelect) + type + code + name + description + isPmo
- [ ] 21.4 RTL + MSW 测试：列表渲染 + TreeSelect + 删除确认（F03 GREEN）

## 8. 前端 users 页（F04，P0）

- [ ] 22.1 `api/user.ts` 5 个函数
- [ ] 22.2 `pages/User/index.tsx` 列表 + 搜索 + isInternal 切换 + 新建/编辑 + 删除
- [ ] 22.3 `pages/User/EditDrawer.tsx` 表单：login_name(创建后只读) + name + code + email_address + is_internal + enabled（**无** password 字段）
- [ ] 22.4 RTL + MSW 测试：列表 + 编辑无 password（F04 GREEN）

## 9. 前端 user-organizations 页（F05，P0）

- [ ] 23.1 `api/userOrganization.ts` 5 个函数
- [ ] 23.2 `pages/UserOrganization/index.tsx` 列表（含 user.name / org.name / role / is_primary 标识）+ userId/organizationId 筛选 + 新建/编辑 + 删除
- [ ] 23.3 `pages/UserOrganization/EditDrawer.tsx` 表单：userId(异步 Select) + organizationId(TreeSelect) + role + is_primary（设 true 时显示文案"将自动 demote 当前 user 的旧主属"）+ joinedAt + leftAt
- [ ] 23.4 RTL + MSW 测试：列表 + 新建（F05 GREEN）

## 10. E2E + Cleanup（Z01，P0）

- [ ] 24.1 `docker compose down -v && docker compose up -d --build`，等待全部 healthy
- [ ] 24.2 `docker exec rainier-mysql mysql -uroot -proot rainier -e "SHOW TABLES"` → 含 4 张表
- [ ] 24.3 `curl http://localhost:18080/api/health` → 200
- [ ] 24.4 `cd backend && JAVA_HOME=... mvn -ntp test` 在无 docker 环境下全绿
- [ ] 24.5 修改 `archive/2026-06-02-bootstrap-fullstack-scaffold/pending-adjustments.md` 中 Adjustment #1 加 ✅ 已解决标注，指向本变更
- [ ] 24.6 提交切片完成日志 / 准备进 Phase 5 VERIFY

## 11. 测试与验证（Phase 5 入口）

- [ ] 25.1 `mvn -ntp test` 全绿（≥ 32 新增后端测试）
- [ ] 25.2 `mvn -ntp spotless:check checkstyle:check` 全绿
- [ ] 25.3 `npm test -- --run` 全绿（≥ 14 新增前端测试）
- [ ] 25.4 `npm run build` 无 type error
- [ ] 25.5 `npm run lint` 0 errors
- [ ] 25.6 全部 55 TC 对照 test-plan.md 勾选

<!--
优先级说明：本变更全部 P0（10 P0 slices + 14 P0 slices = 24）。
P1 TC 部分会被 P0 slice 顺带覆盖（如 TC-ORG-010 在 B09 / TC-FES-203 在 F01）。
-->
