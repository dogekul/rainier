# v1 id 全栈迁移 任务清单

> 7 切片 × 3-5 子任务；M01 完成后短暂红期到 M04 结束，符合预期。

## 1. BaseEntity 重写 + handler 新增（M01，P0）

- [ ] 1.1 `BaseEntity`：`String id` → `Long id`；删 `@GenericGenerator(strategy="uuid")`；加 `@GeneratedValue(strategy = GenerationType.IDENTITY)`；删 `@Column(length=32)`
- [ ] 1.2 `GlobalExceptionHandler` 加 `@ExceptionHandler(MethodArgumentTypeMismatchException.class)` → 400 JSON `{message:"Invalid id format"}`
- [ ] 1.3 `import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException`
- [ ] 1.4 此时 `mvn compile` 大量报错，正常；继续 M02

## 2. Organization 全链（M02，P0）

- [ ] 2.1 `Organization` entity：`parentId` String → Long（含 @Column(name="parent_id")）
- [ ] 2.2 `OrganizationRepository extends JpaRepository<Organization, Long>, JpaSpecificationExecutor<Organization>`；派生方法 `existsByParentIdAndCode(Long, String)`、`existsByParentIdIsNullAndCode` 等；`countRawById(Long)` + `rawDelFlag(Long)`
- [ ] 2.3 `OrganizationService`：所有 `String id` → `Long id`；`getOrThrow(Long)`；`computeWholeName`、`cascade*` 内部 Long 处理
- [ ] 2.4 `OrganizationController`：所有 `@PathVariable String id` → `Long id`；`URI.create("/api/organizations/" + id)` 字符串拼接对 Long 自动 toString
- [ ] 2.5 DTO 类型：`OrganizationDetail.id/parentId`、`OrganizationCreateRequest.parentId`、`OrganizationUpdateRequest`（无 id 字段）、`OrganizationMoveRequest.parentId` 全部 String → Long；@Size 注解删（Long 不支持）
- [ ] 2.6 `OrganizationControllerCreateTest`：5 个测试 `matchesPattern("[0-9a-f]{32}")` → `\\d+`；`readId(MvcResult)` 用 `asLong()`；非数字 parentId 测试用 `"99999999999"` 字符串经 JSON 转 Long（仍 400 但因 type-mismatch）
- [ ] 2.7 `OrganizationControllerQueryTest`：10 个测试断言改造；3 层级联用 `body.path =~ ^/\d+/\d+/\d+$` 正则；明确"含 A、B、C 三 id 顺序"
- [ ] 2.8 `OrganizationDeleteFkTest`：3 工厂方法返回 Long；`createOrg`、`createUser`、`createUserOrg` 改返值
- [ ] 2.9 `OrganizationRepositoryTest`：3 用例的 `repo.countRawById(Long)` / `rawDelFlag(Long)` 调用；ROOT-1/2/3 工厂返 Long
- [ ] 2.10 新增 TC-MIG-003：3 层级联场景用 hamcrest `matchesPattern("^/\\d+/\\d+/\\d+$")` 断言（加到 OrganizationControllerQueryTest 或独立类）

## 3. User 全链（M03，P0）

- [ ] 3.1 `User` entity 不动（id 在 BaseEntity）；只确认 @Column 无 length=32
- [ ] 3.2 `UserRepository<User, Long>`；`existsByLoginName(String)`、`existsByCode(String)`、`existsByEmailAddress(String)` 不动（参数本来就 String）
- [ ] 3.3 `UserService`：`getOrThrow(Long)`；`delete(Long)`；`update(Long, req)`；FK 检查 `userOrgRepo.countByUserIdAndLeftAtIsNull(Long)`
- [ ] 3.4 `UserController` @PathVariable Long
- [ ] 3.5 DTO：`UserDetail.id` String → Long；`UserCreateRequest` / `UserUpdateRequest` 无 id 字段
- [ ] 3.6 `UserControllerTest`：10 用例 matchesPattern / readId 改 Long；ghost id 占位换 `999_999L`

## 4. UserOrganization 全链（M04，P0）

- [ ] 4.1 `UserOrganization` entity：`userId` / `organizationId` String → Long；@Column(name="user_id" / "organization_id") 保留
- [ ] 4.2 `UserOrganizationRepository<UO, Long>`；`existsByUserIdAndOrganizationId(Long, Long)`；`findByUserIdAndIsPrimaryTrue(Long)`；`countByOrganizationIdAndLeftAtIsNull(Long)`；`countByUserIdAndLeftAtIsNull(Long)`；`demoteOthersForUser(Long, Long)`（含 @Query 参数类型）
- [ ] 4.3 `UserOrganizationService`：所有 String id 改 Long；`create(req)` 内 `repo.demoteOthersForUser(user.getId(), uo.getId())` 不变（语义对）；错误消息 "user not found: id=" + id 自动数字
- [ ] 4.4 `UserOrganizationController` @PathVariable Long；query param `userId` / `organizationId` 从 String 改 Long
- [ ] 4.5 DTO：`UserOrgDetail.id/userId/organizationId` Long；`UserOrgCreateRequest.userId/organizationId` Long（删 @NotBlank，改 @NotNull）；`UserOrgUpdateRequest.organizationId` Long
- [ ] 4.6 `UserOrganizationControllerTest`：10 用例改造；含 `post_userNotFound_returns400` 用 `999999L`；`expect.body.message.value("user not found: id=999999")`

## 5. 新增 MIG 单元测试 + 余下 v1 扫尾（M05，P0）

- [ ] 5.1 新建 `BaseEntityReflectionTest`（com.rainier.common.persistence 下）：
  - 反射读 `BaseEntity.class.getDeclaredField("id")`
  - 断言 `field.getType() == Long.class`
  - 断言 含 `@GeneratedValue`，`strategy() == IDENTITY`
- [ ] 5.2 新建 `PathVariableTypeMismatchTest`：MockMvc GET /api/organizations/not-a-number → 400 + application/json + body.message 非空 + content 不含 "Exception"
- [ ] 5.3 `PageParamsTest`：无 id 字段，不动
- [ ] 5.4 `GlobalExceptionHandlerTest`：5 用例不涉及 id，不动；BoomController 不动
- [ ] 5.5 `mvn -ntp test` 全绿确认（M01-M05 累积）

## 6. 前端 TS 类型 sweep（M06，P0）

- [ ] 6.1 `frontend/src/api/organization.ts`：
  - `Organization.id` string → number
  - `Organization.parentId` string | null → number | null
  - `OrganizationCreate.parentId?` string | null → number | null
  - `OrganizationUpdate`（无 id 字段）
  - 函数签名 `getOrganization(id: string)` → `(id: number)`、`updateOrganization(id, body)` 等所有 5 + 2 接口
- [ ] 6.2 `frontend/src/api/user.ts`：`User.id: string` → number；函数签名同步
- [ ] 6.3 `frontend/src/api/userOrganization.ts`：`UserOrganization.id/userId/organizationId: string` → number；enrichment 字段（userLoginName 等）保留 string
- [ ] 6.4 `components/ui/TreeSelect.tsx`：`TreeNode.id: string` → number；`parentId: string | null` → number | null；onChange 签名 `(id: number | null)`
- [ ] 6.5 `pages/Organization/index.tsx + EditDrawer.tsx + OrganizationsPage.tsx`：state `parentId: string | null` → number | null；删除 confirm 用到 id 的地方；TreeSelect 传值改
- [ ] 6.6 `pages/User/UsersPage.tsx`：editing.id、delete confirm 等用到 id 的类型
- [ ] 6.7 `pages/UserOrganization/UserOrganizationsPage.tsx`：userId state string → number；organizationId state string | null → number | null
- [ ] 6.8 `components/ui/Table.test.tsx`：fixtures id 用 `1, 2`
- [ ] 6.9 `components/ui/TreeSelect.test.tsx`：nodes id 用 `1, 2, 3`；parentId 用 `1`、`2`、`null`；onChange 期望 `2`
- [ ] 6.10 `npm test -- --run` + `npm run build` + `npm run lint` 全绿

## 7. E2E verify + cleanup（M07，P0）

- [ ] 7.1 `docker compose down -v` 清掉 rainier-mysql-data 卷
- [ ] 7.2 `RAINIER_BACKEND_HOST_PORT=18080 docker compose up -d --build`
- [ ] 7.3 等 3 容器 healthy（最长 3 分钟）
- [ ] 7.4 `docker exec rainier-mysql mysql -uroot -prainier_root rainier -e "DESCRIBE rainier_organization;"` 输出含 `id  bigint` 与 `auto_increment`（TC-MIG-004）
- [ ] 7.5 同上对 rainier_user / rainier_user_organization
- [ ] 7.6 `curl -fsS http://localhost:18080/api/health` → 200 + status:UP
- [ ] 7.7 `curl -X POST http://localhost/api/organizations -H "Content-Type: application/json" -d '{"type":"COMPANY","code":"X","name":"X"}' | jq '.id | type'` → 输出 `"number"`（TC-MIG-005）
- [ ] 7.8 浏览器 `/org/organizations` / `/org/users` / `/org/user-organizations` 三页可用（人工烟测，本变更长程自动跑可省略，留 Gate 3 时用户确认）
- [ ] 7.9 `mvn -ntp test` + `mvn spotless:check checkstyle:check` 全绿
- [ ] 7.10 `npm test -- --run` + `npm run build` + `npm run lint` 全绿

## 8. 测试与验证（Phase 5 入口）

- [ ] 8.1 后端测试 ≥ 59 + 2（TC-MIG-001/002）= 61 全绿；不增不减 v1 用例
- [ ] 8.2 前端测试 ≥ 11 全绿
- [ ] 8.3 `git grep "[0-9a-f]{32}"` 在 backend 测试中 0 匹配
- [ ] 8.4 `git grep "private String id"` 在 BaseEntity.java 中 0 匹配
- [ ] 8.5 `git grep "JpaRepository<.*, String>"` 在 backend 主代码中 0 匹配
- [ ] 8.6 `git grep "id: string"` 在 frontend/src/api/ 中 0 匹配
