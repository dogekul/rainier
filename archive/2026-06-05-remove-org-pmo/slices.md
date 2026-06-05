# v0.0.5-remove-org-pmo 切片执行计划

| # | 优先级 | TC 覆盖 | 实现目标 | 依赖 |
|---|---|---|---|---|
| M01 | P0 | (前置) | 后端字段移除：`Organization.java` 删 `isPmo` 字段 + `@Column` + getter/setter；`OrganizationCreateRequest` / `OrganizationUpdateRequest` / `OrganizationDetail` 各删 `isPmo` 字段 + getter/setter；`OrganizationService` 删 3 处赋值（create 1 处 + update 2 处）；mvn compile 通过 | 无 |
| M02 | P0 | TC-RMP-001, TC-RMP-002, TC-RMP-003 | 后端测试：`OrganizationControllerCreateTest:67` 改为 `jsonPath("$.isPmo").doesNotExist()`；`OrganizationControllerQueryTest` 新增 1 个 TC-RMP-002 GET doesNotExist + 1 个 TC-RMP-003 PUT 容错忽略 isPmo；mvn test 全绿 | M01 |
| M03 | P0 | (前置) | 前端类型：`frontend/src/api/organization.ts` 中 `Organization` / `OrganizationCreate` / `OrganizationUpdate` 三个 interface 删除 `isPmo` 字段；tsc-b 通过 | 无 |
| M04 | P0 | TC-RMP-FE-001 | 前端 EditDrawer：`EditDrawer.tsx` 删除 PMO 复选框 + `isPmo` state + submit body 中 `isPmo`；新建 `EditDrawer.test.tsx` 断言 `queryByLabelText('PMO 团队') === null`；vitest 全绿 | M03 |
| M05 | P0 | TC-RMP-FE-002 | 前端 OrganizationsPage：`OrganizationsPage.tsx` 删除 PMO 列；删除 `onSubmit` 中传给 update 的 `isPmo`；新建 `OrganizationsPage.test.tsx` 断言表头不含 'PMO'；vitest 全绿 | M03 |
| M06 | P0 | (主规范) | `specs/entity-organization/spec.md` 中 3 处 isPmo 提及（line 55 / 73 / 96）in-place 删除 | 无 |
| M07 | P0 | TC-RMP-FE-003, TC-RMP-E2E-001 | E2E 验证：`docker compose down -v && up -d --build` 起栈；`docker exec mysql DESCRIBE rainier_organization` 无 `is_pmo`；`curl POST` body 带 isPmo 返回 200 + response 无 isPmo；`grep -rn 'isPmo' frontend/src backend/src/main` 0 命中（V1__init_org.sql 排除） | M01, M02, M03, M04, M05 |

## 执行顺序

```
M01 (backend src)     M03 (frontend types)        M06 (spec)
   │                     │                            │
   ├─ M02 (backend test) ├─ M04 (EditDrawer test)     │
   │                     ├─ M05 (OrgPage test)        │
   │                     │                            │
   └─────────┬───────────┘                            │
             │                                        │
             ▼                                        │
            M07 (E2E + DESCRIBE + curl) ←─────────────┘
```

**并行机会**：
- M01 / M03 / M06 互无依赖，可并行
- M02 等 M01；M04 / M05 等 M03
- M07 是同步点，需要所有前置完成

**长程模式下顺序执行**：build skill 会按 M01 → M02 → M03 → M04 → M05 → M06 → M07 串行 RED/GREEN/REFACTOR；并行只在脑海里成立，实际执行按 topological order。
