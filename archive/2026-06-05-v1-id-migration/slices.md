# v1 id 全栈迁移 切片执行计划

> 共 7 个切片，长程模式串行执行。
> 注意 M01 完成后 ~50 个测试会编译失败 / 红，由 M02-M04 逐层修复。

| # | ID | 优先级 | TC 覆盖 | 实现目标 | 依赖 |
|---|---|---|---|---|---|
| 1 | M01 | P0 | (M02-M07 的前置) | `BaseEntity` 改 `Long id` + `@GeneratedValue(IDENTITY)`；`GlobalExceptionHandler` 加 `MethodArgumentTypeMismatchException` → 400 JSON handler；编译期会大量报错，正常 | 无 |
| 2 | M02 | P0 | TC-ORG-001..019 + TC-MIG-003 | Organization 全链：entity.parentId → Long；Repository<Organization, Long>；service 所有 String id 参数改 Long；controller @PathVariable Long；4 个 DTO id/parentId 改 Long；树 path 内容自动变 `/1/2/3`；3 个 Org 测试类断言 / 工厂 / readId / matchesPattern 改造 + 新增 TC-MIG-003 | 1 |
| 3 | M03 | P0 | TC-USR-001..013 | User 全链：Repository<User, Long>；service Long getOrThrow；controller @PathVariable Long；3 个 User DTO id 改 Long；UserControllerTest 断言改造（含 ghost id 占位换 `999_999L`） | 1 |
| 4 | M04 | P0 | TC-UOR-001..012 + TC-ORG-016（FK 删保护）| UserOrganization 全链：UO entity userId / organizationId 改 Long；Repository<UO, Long> + existsByUserIdAndOrganizationId(Long,Long)；service 所有 String id 改 Long；controller @PathVariable Long；4 个 UO DTO id/userId/organizationId 改 Long；UOControllerTest + OrganizationDeleteFkTest 改造；service 错误消息含数字 id（"organization not found: id=999999"） | 1 |
| 5 | M05 | P0 | TC-MIG-001 + TC-MIG-002 + TC-PAG-001..003 + TC-BES-201..203 | 单元：新建 `BaseEntityReflectionTest`（TC-MIG-001：反射断言 id 类型 / @GeneratedValue / IDENTITY）；新建 `PathVariableTypeMismatchTest`（TC-MIG-002）；PageParamsTest、GlobalExceptionHandlerTest、OrganizationRepositoryTest（含 native query）扫一遍断言改造 | 1, 2 |
| 6 | M06 | P0 | TC-FES-201..203 + 前端 build/lint | 前端类型 sweep：`api/{organization, user, userOrganization}.ts` 所有 id 字段 string → number；`TreeSelect.tsx` TreeNode 类型；3 个 page 文件 + EditDrawer state 类型；Table.test.tsx / TreeSelect.test.tsx fixture 用整数；npm test + npm run build + npm run lint 全绿 | 2, 3, 4 |
| 7 | M07 | P0 | TC-DRT-201 + TC-MIG-004 + TC-MIG-005 + TC-TRT-201 | E2E：`docker compose down -v` 清卷 → `docker compose up -d --build` → 等 healthy；docker exec mysql -e "DESCRIBE rainier_organization" 等 3 表显示 BIGINT；curl POST /api/organizations 返回 body.id 为 JSON 数字；浏览器三页烟测；最后 `mvn test` + `npm test` + lint 全绿（无 docker 也通） | 1-6 |

## 推荐串行执行

```
M01 (BaseEntity + handler, 编译会断)
  ↓
M02 Organization 全链 (含级联 path test 验证 /1/2/3)
  ↓
M03 User 全链
  ↓
M04 UserOrganization 全链 + Org delete FK test
  ↓
M05 新增 MIG 单元测试 + 余下 v1 测试断言扫尾
  ↓
M06 前端 TS 类型 sweep + 测试 + build
  ↓
M07 E2E verify (docker down -v / up -d --build / curl / 浏览器烟测)
```

## 风险点

- **M01 后大面积红**：BaseEntity 改完，所有 v1 测试在 M02-M04 修复前都不通；正常情况；不触发降级（这是预期的重构空窗）
- **path /1/2/3 与并发 IDENTITY**：测试断言 `path == "/1/2/3"` 依赖 IDENTITY 顺序生成；若测试用 `@Transactional` 回滚后再插入可能不连续；解决：用相对断言（`path =~ /\d+\/\d+\/\d+/`）+ 端到端断言（C 的 path 必以 A.id 开头、B.id 居中、C.id 结尾）
- **`@PathVariable Long` 异常归类**：默认 `MethodArgumentTypeMismatchException` 被 `Throwable` handler 兜底成 500；M01 显式新增 handler 后归 400；M05 / TC-MIG-002 显式断言
- **前端 TS strict 模式**：`number | null` 与之前 `string | null` 在 `??` / `?.` 行为相同；但 `JSON.parse(...).id === '...'` 类比较风险；不影响（API 端已数字化）
- **demand-requirement 文档**：仍 paused；本变更交付后由该变更解锁，本变更不动它的文件

## 并行机会

理论上 M02 / M03 / M04 可并行（无交叉依赖），但长程串行执行避免 commit 噪声。
