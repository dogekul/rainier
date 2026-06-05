# v0.0.5-remove-org-pmo 任务清单

## 1. entity-organization Capability（P0）

### 1.1 后端字段移除（M01）

- [x] 1.1.1 `backend/src/main/java/com/rainier/organization/domain/Organization.java`：删除第 51-52 行 `@Column(name = "is_pmo", nullable = false)` + `private Boolean isPmo = Boolean.FALSE;`；删除 line 114 getter `getIsPmo()`；删除 line 117-119 setter `setIsPmo(Boolean)`
- [x] 1.1.2 `backend/src/main/java/com/rainier/organization/dto/OrganizationCreateRequest.java`：删除 line 27 `private Boolean isPmo;`；line 72 getter；line 75-77 setter
- [x] 1.1.3 `backend/src/main/java/com/rainier/organization/dto/OrganizationUpdateRequest.java`：删除 line 24 字段；line 53 getter；line 56-58 setter
- [x] 1.1.4 `backend/src/main/java/com/rainier/organization/dto/OrganizationDetail.java`：删除 line 19 字段；line 36 `dto.isPmo = o.getIsPmo()` 赋值行；line 78 getter
- [x] 1.1.5 `backend/src/main/java/com/rainier/organization/service/OrganizationService.java`：删除 line 74 `o.setIsPmo(Boolean.TRUE.equals(req.getIsPmo()));`；删除 line 157-159 `if (req.getIsPmo() != null) { o.setIsPmo(req.getIsPmo()); }`
- [x] 1.1.6 mvn compile 通过；spotless + checkstyle 0 违规

### 1.2 后端测试改造（M02）— 依赖 #1.1

- [x] 1.2.1 `backend/src/test/java/com/rainier/organization/controller/OrganizationControllerCreateTest.java:67`：将 `.andExpect(jsonPath("$.isPmo").value(false))` 改为 `.andExpect(jsonPath("$.isPmo").doesNotExist())` → **TC-RMP-001**
- [x] 1.2.2 `backend/src/test/java/com/rainier/organization/controller/OrganizationControllerQueryTest.java`：新增 `get_byId_responseDoesNotContainIsPmo()` MockMvc 测试 → **TC-RMP-002**
- [x] 1.2.3 `backend/src/test/java/com/rainier/organization/controller/OrganizationControllerQueryTest.java`：新增 `put_withIsPmoInBody_silentlyIgnored_returns200()` MockMvc 测试 → **TC-RMP-003**
- [x] 1.2.4 mvn test 全绿（64 个测试 — v0.0.4 baseline 62 + 2 新增 TC-RMP-002/003；TC-RMP-001 是 line 67 in-place 替换不增计）

## 2. frontend-scaffold Capability（P0）

### 2.1 前端类型移除（M03）

- [x] 2.1.1 `frontend/src/api/organization.ts:15`：删除 `Organization.isPmo: boolean;`
- [x] 2.1.2 `frontend/src/api/organization.ts:27`：删除 `OrganizationCreate.isPmo?: boolean;`
- [x] 2.1.3 `frontend/src/api/organization.ts:35`：删除 `OrganizationUpdate.isPmo?: boolean;`
- [x] 2.1.4 tsc -b 通过

### 2.2 EditDrawer 改造 + 测试（M04）— 依赖 #2.1

- [x] 2.2.1 `frontend/src/pages/Organization/EditDrawer.tsx:33`：删除 `const [isPmo, setIsPmo] = useState(false);`
- [x] 2.2.2 同文件 line 48：删除 `setIsPmo(editing.isPmo);`
- [x] 2.2.3 同文件 line 68：从 submit body 中删除 `isPmo,`
- [x] 2.2.4 同文件 line 98-101：删除 PMO label + checkbox（保留"启用"checkbox）
- [x] 2.2.5 新建 `frontend/src/pages/Organization/EditDrawer.test.tsx`：mount with mock `getOrganizationTree` returning `[]`；assert `queryByText('PMO 团队') === null` + `queryByLabelText('PMO 团队') === null` → **TC-RMP-FE-001**
- [x] 2.2.6 vitest 该 test 通过

### 2.3 OrganizationsPage 改造 + 测试（M05）— 依赖 #2.1

- [x] 2.3.1 `frontend/src/pages/Organization/OrganizationsPage.tsx:42-46`：删除 `{ key: 'isPmo', title: 'PMO', render: (r) => (r.isPmo ? '是' : '—') },`
- [x] 2.3.2 同文件 line 113：删除 `onSubmit` 内 `isPmo: req.isPmo,`
- [x] 2.3.3 新建 `frontend/src/pages/Organization/OrganizationsPage.test.tsx`：mount with mock `listOrganizations` returning 1 item；assert `screen.queryAllByRole('columnheader').map(h => h.textContent)` 不含 'PMO' + 含 ['编码','名称','类型','全路径','操作'] → **TC-RMP-FE-002**
- [x] 2.3.4 vitest 全绿（≥ 13 个测试）

## 3. 主规范修订（P0）

### 3.1 specs/entity-organization/spec.md（M06）

- [x] 3.1.1 line 55：去掉 `isPmo /` 部分（response body 字段清单中）
- [x] 3.1.2 line 73：去掉 `、isPmo`（tree 节点字段清单中）
- [x] 3.1.3 line 96：去掉 `/ is_pmo`（update 允许字段集中）

## 4. 测试与验证（M07）

### 4.1 E2E 验证 — 依赖 #1, #2, #3 全部完成

- [x] 4.1.1 `docker compose down -v && docker compose up -d --build`（环境变量 `RAINIER_BACKEND_HOST_PORT=18080`）
- [x] 4.1.2 等待 3 服务 healthy；curl `http://localhost:18080/api/health` = 200；curl `http://localhost/` = 200
- [x] 4.1.3 `docker exec rainier-mysql mysql -urainier -prainier rainier -e "DESCRIBE rainier_organization"` → 输出不含 `is_pmo` → **TC-RMP-E2E-001**
- [x] 4.1.4 `curl -X POST http://localhost:18080/api/organizations -H 'Content-Type: application/json' -d '{"type":"COMPANY","code":"X","name":"X","isPmo":true}'` → HTTP 201；`jq '.isPmo'` 返回 null（不存在）
- [x] 4.1.5 `grep -rn 'isPmo\|is_pmo' backend/src/main/java backend/src/main/resources/application*.yml frontend/src specs/entity-organization` 返回 0 行 → **TC-RMP-FE-003**
- [x] 4.1.6 全量 `mvn test` ≥ 63 全绿；`npm test -- --run` ≥ 13 全绿；`npm run build` 退出码 0；`npm run lint` 0 错误

## 5. 切片完成度对照

| 切片 | TC 覆盖 | 任务编号 |
|---|---|---|
| M01 | (前置) | 1.1.1-1.1.6 |
| M02 | TC-RMP-001, 002, 003 | 1.2.1-1.2.4 |
| M03 | (前置) | 2.1.1-2.1.4 |
| M04 | TC-RMP-FE-001 | 2.2.1-2.2.6 |
| M05 | TC-RMP-FE-002 | 2.3.1-2.3.4 |
| M06 | (主规范) | 3.1.1-3.1.3 |
| M07 | TC-RMP-FE-003, TC-RMP-E2E-001 | 4.1.1-4.1.6 |
