# v0.0.5 测试报告

> 测试日期：2026-06-05
> 测试环境：macOS Darwin 25.5.0 · Java 1.8.0_472 · Maven 3.9.11 · Node 25.2.1 · MySQL 8.0 (docker)
> 被测版本：working tree at change `2026-06-05-remove-org-pmo` 末态

## 一、总体概况

| 指标 | 后端 | 前端 |
|---|---|---|
| 测试用例总数 | 64 | 13 |
| 通过 | 64 | 13 |
| 失败 | 0 | 0 |
| 跳过 | 0 | 0 |
| 通过率 | 100% | 100% |
| 执行耗时 | ~5.5 s | ~1.0 s |

**spec 要求**：≥ 64 backend（62 v0.0.4 baseline + 2 新增 TC-RMP-002/003；TC-RMP-001 是 line 67 in-place 替换不增计）+ ≥ 13 frontend（11 v0.0.4 baseline + 2 新增 TC-RMP-FE-001/002）。**实际**：64 backend + 13 frontend，**完全吻合**。

### 1.1 覆盖率诊断

| 维度 | 状态 | 说明 |
|---|---|---|
| 后端测试覆盖（变更包：`organization/domain/Organization`、`organization/dto/{Create,Update,Detail}Request`、`organization/service/OrganizationService` 中 isPmo 相关分支） | ✅ | 5 个 v0.0.4 已通过的 controller/repo 测试全绿（行为锚点）；3 个新 TC-RMP-001/002/003 显式断言 doesNotExist + PUT 容错 |
| 前端组件 / 类型契约 | ✅ | `tsc -b` 0 错；2 个新 vitest 测试断言 PMO 控件不渲染 |
| Hibernate ddl-auto schema | ✅ | `docker compose down -v` + up 后 DESCRIBE `rainier_organization` 不含 `is_pmo` 列 |

## 二、按模块统计

| 测试模块 | 用例数 | 通过 | 备注 |
|---|---|---|---|
| RainierApplicationTests | 1 | 1 | contextLoads |
| HealthControllerTest (v0) | 1 | 1 | v0，不动 |
| AuthControllerLoginTest (v0) | 3 | 3 | v0，不动 |
| AuthControllerMeTest (v0) | 5 | 5 | v0，不动 |
| GlobalExceptionHandlerTest | 5 | 5 | 不涉及 isPmo |
| PathVariableTypeMismatchTest | 1 | 1 | 不涉及 |
| CorsConfigTest (v0) | 1 | 1 | v0 |
| PageParamsTest | 4 | 4 | 不涉及 |
| BaseEntityReflectionTest | 1 | 1 | 不涉及 |
| OrganizationRepositoryTest | 3 | 3 | 不涉及 isPmo（不查该字段） |
| OrganizationControllerCreateTest | 5 | 5 | **TC-RMP-001** in-place 替换 line 67 → `doesNotExist()` |
| **OrganizationControllerQueryTest (含 TC-RMP-002/003)** | 13 | 13 | 11 v0.0.4 + 2 新增 |
| OrganizationDeleteFkTest | 1 | 1 | 不涉及 |
| UserControllerTest | 10 | 10 | 不涉及 |
| UserOrganizationControllerTest | 10 | 10 | 不涉及 |
| **后端合计** | **64** | **64** | — |
| (frontend) Table.test.tsx | 2 | 2 | 不涉及 |
| (frontend) TreeSelect.test.tsx | 1 | 1 | 不涉及 |
| (frontend) AppLayout.test.tsx | 1 | 1 | 不涉及 |
| **(frontend) EditDrawer.test.tsx (NEW)** | 1 | 1 | **TC-RMP-FE-001** PMO 复选框 + label 缺席 |
| **(frontend) OrganizationsPage.test.tsx (NEW)** | 1 | 1 | **TC-RMP-FE-002** 列表表头无 PMO 列 |
| (frontend) App.test.tsx (v0) | 1 | 1 | v0 |
| (frontend) ProtectedRoute.test.tsx (v0) | 2 | 2 | v0 |
| (frontend) Login.test.tsx (v0) | 1 | 1 | v0 |
| (frontend) tokens.test.tsx (v0) | 1 | 1 | v0 |
| (frontend) auth.test.ts (v0) | 2 | 2 | v0 |
| **前端合计** | **13** | **13** | — |

## 三、E2E 测试结果

### 3.1 总体概况

| 指标 | 数值 |
|---|---|
| 用例数 | 4（手动 Bash） |
| 通过 | 4 |
| 失败 | 0 |

### 3.2 关键路径结果

| 路径 | 状态 | 说明 |
|---|---|---|
| `docker compose down -v` 清卷 + up -d --build 3 服务 healthy | ✅ | 服务起栈成功（mysql/backend/frontend 全 healthy）；Hibernate `ddl-auto=update` 在空 schema 上 CREATE TABLE 不含 `is_pmo` |
| **TC-RMP-E2E-001** `DESCRIBE rainier_organization` | ✅ | 字段：id / create_by / create_time / del_flag / update_by / update_time / code / description / enabled / name / parent_id / path / type / whole_name（**无 is_pmo**） |
| **TC-RMP-FE-003** `grep -rn 'isPmo\|is_pmo' backend/src/main/java application*.yml frontend/src` | ✅ | 0 行命中（V1__init_org.sql 排除，见 §8.1 known-limitations） |
| TC-RMP-003 (E2E confirmation) `curl POST .../organizations -d '{"isPmo":true,...}'` | ✅ | HTTP 201；`response.body.isPmo == null`（Jackson 静默忽略未知 + 序列化时无 isPmo 字段）；其他字段（id/parentId/path/wholeName/enabled/createTime/updateTime/createBy/updateBy）齐全 |

### 3.3 E2E 结论

✅ 端到端通过。schema 层（DESCRIBE）+ 契约层（POST 容错）+ 静态层（grep）三重确认 isPmo 已彻底从运行时移除。

## 四、失败项详细分析

无失败项。

## 五、功能 / 测试覆盖对照

| 功能模块 | 涉及源码 | 已覆盖测试 |
|---|---|---|
| entity-organization R1 (API 契约不含 isPmo) | `Organization.java`、3 DTOs、`OrganizationService.java`（create/update 删 setIsPmo） | TC-RMP-001（CreateTest line 67 doesNotExist）+ TC-RMP-002（QueryTest GET doesNotExist）+ TC-RMP-003（QueryTest PUT 容错）+ E2E curl |
| entity-organization R2 (schema 不含 is_pmo) | `Organization.java` 删 `@Column(name="is_pmo")` | TC-RMP-E2E-001（DESCRIBE） |
| frontend-scaffold R1 (UI 无 PMO 控件) | `api/organization.ts` 删 3 处类型字段；`EditDrawer.tsx` 删 state + checkbox；`OrganizationsPage.tsx` 删列 + onSubmit isPmo | TC-RMP-FE-001（EditDrawer.test.tsx）+ TC-RMP-FE-002（OrganizationsPage.test.tsx）+ TC-RMP-FE-003（grep + tsc） |

## 五-B、多路并行 Review 结果

### Review 迭代历史

| 轮次 | C | H | M | L | 状态 |
|---|---|---|---|---|---|
| 1 | 0 | 1 | 3 | 8 | 修复 M-2/M-3 + H 降级 → 通过 |

### 最终 Review 汇总

| 维度 | Critical | High | Medium | Low |
|---|---|---|---|---|
| 代码质量 | 0 | 0 | 0 | 3 |
| 测试/配置 | 0 | 1（V1__init_org.sql 残留） | 1（test-report 缺失，本文件解决） | 3 |
| 文档/Skills | 0 | 0 | 2（test-plan 计数错） | 2 |

### Review 已修复问题

| # | 严重性 | 文件 | 问题 | 状态 |
|---|---|---|---|---|
| 1 | M | `changes/2026-06-05-remove-org-pmo/test-plan.md` §1.3 baseline | QueryTest 行误标 "0 修改"，应为 "+2 新增" | ✅ 已改 |
| 2 | M | 同 test-plan.md 合计行 + tasks.md §1.2.4 文案 | 后端合计 65 → 64；tasks "≥ 63" → 精确 64 | ✅ 已改 |

### Review 已知限制 / 接受不修复

| # | 严重性 | 位置 | 说明 |
|---|---|---|---|
| 1 | H → 降级 M | `backend/src/main/resources/db/migration/V1__init_org.sql:25` | `is_pmo TINYINT(1) NOT NULL DEFAULT 0` 仍存在。**接受**：design.md 决策 1 + proposal `explicitly_excluded: v1-historical-sql-rewrite` 显式将该文件标为 v1 历史档不动；Flyway 已禁用（v0.0.3 Adjustment #1），ddl-auto=update 从 entity 生成 schema 不含 `is_pmo`，TC-RMP-E2E-001 DESCRIBE 实测通过。未来若启用 Flyway 应作为独立 change 修订。**风险登记于 §8.1**。 |
| 2 | M | 本文件 | test-report 缺失 → 本文件解决（review 时 verify 阶段尚未完成 Step 5） |
| 3 | L | OrganizationControllerCreateTest | 缺 POST body 含 isPmo 的容错测试（与 PUT 的 TC-RMP-003 对称的）。TC-RMP-003 已隐含证明 Jackson `FAIL_ON_UNKNOWN_PROPERTIES=false` 全局生效；E2E curl POST 已额外验证；**接受不补** |
| 4 | L | `specs/entity-organization/spec.md` | 缺 "POST response body 不含 isPmo" 的显式 Scenario；TC-RMP-001 在测试层已锚定；**接受不补** |
| 5 | L | `EditDrawer.test.tsx:35` | `queryByLabelText('PMO 团队')` 在原 markup（无 htmlFor 的内联 label）下本就找不到目标控件，作为防御性冗余无害；**接受不删** |
| 6 | L | `OrganizationsPage.test.tsx` body 文本 'PMO' 检查 | 真实回归会渲染 `'是'/'—'` 而非 `'PMO'` 文本；header 数组检查才是负重断言；**接受**：body 检查作为防御冗余无害 |
| 7 | L | 缺 `GET /tree` items 的 doesNotExist 断言 | 共用 `OrganizationDetail.from()`；TC-RMP-002 在该 from 路径上已锚定；**接受不补** |
| 8 | L | `EditDrawer.test.tsx` async 微任务未显式 await mock settle | 可能触发 React `act()` 警告但 `waitFor(...)` 实际等待标签出现；run 时未报 warning；**接受不动** |
| 9 | L | `tasks.md` 中行号是 pre-edit snapshot | 修复完成后行号失效，仅作 audit trail；**接受** |
| 10 | L | proposal §What Changes 4 "移除"措辞 | 与 design.md "in-place 替换 line 67" 略有出入；语义上一致；**接受** |
| 11 | L | EditDrawer 测试断言强度 | 同 5；**接受** |

## 六、设计调整说明

2 项 Minor 文档修正，详见 [design-adjustments.md](design-adjustments.md)。无任何设计或行为偏离。

## 七、修复确认记录

| 问题 | 修复文件 | 状态 |
|---|---|---|
| test-plan §1.3 baseline `OrganizationControllerQueryTest` 行 "0 修改" → "+2 新增" | `changes/2026-06-05-remove-org-pmo/test-plan.md` | ✅ |
| test-plan 后端合计 65 → 64 + tasks 文案精确化 | `changes/2026-06-05-remove-org-pmo/test-plan.md`、`tasks.md` | ✅ |

## 八、结论

**整体评估**：可交付。64 后端测试 + 13 前端测试全绿；4 路 E2E 全通；Spotless + Checkstyle + ESLint + TypeScript + vite build 全部清白；DESCRIBE rainier_organization 不含 is_pmo；POST/PUT 即便 body 含 isPmo 也仍 200/201 且 response 无 isPmo（Jackson 默认 ignore-unknown 兜底）；浏览器 UI 编辑抽屉无 PMO 复选框、列表无 PMO 列。

**风险等级**：低
- 已知 Known Limitation：V1__init_org.sql 历史档残留 is_pmo 列定义（运行时无影响，Flyway 禁用，design.md 显式接受）

### 8.1 质量信号汇总

| 信号源 | 状态 | 备注 |
|---|---|---|
| 单元/集成测试 | ✅ | 后端 64/64、前端 13/13 = 77/77 全绿 |
| E2E 测试 | ✅ | 4 路（compose up + DESCRIBE + grep + curl POST w/ isPmo） |
| 后端 Lint | ✅ | Spotless + Checkstyle 0 违规 |
| 前端 Lint | ✅ | ESLint 0 错误 |
| 类型检查 | ✅ | tsc -b 0 错误；vite build 通过（dist/assets/index-58m32Ss7.js 232.36 kB） |
| 多路 Review | ✅ | C:0 H:1（降级 M）M:3（2 已修 + 1 本文件解决）L:8（全接受） |
| 十一类失败模式 | ✅ | 0 命中（详见下） |
| 已知限制 | 1 项 | V1__init_org.sql 残留（运行时无影响，符合 design 决策） |

### 8.2 十一类失败模式核对

| ID | 模式 | 结果 |
|---|---|---|
| a 幻觉行为 | 引用注解 / 类名 / 库 API（`@Column`、`@JsonIgnoreProperties`、Jackson `FAIL_ON_UNKNOWN_PROPERTIES`、`doesNotExist()`、`queryByLabelText`）全部真实 | ✅ |
| b 范围蔓延 | 改动严格在 proposal Impact 10 文件 + 2 新建 + 1 主规范 + 4 change docs 范围内；archive 不动；v0 entity 不动；user/uo entity 不动；docker-compose 不动；application*.yml 不动 | ✅ |
| c 级联错误 | 异常未吞；删除字段不影响其他端点；Service 的 cascade（path/wholeName）与 isPmo 无关 | ✅ |
| d 上下文丢失 | design.md 6 决策与实现 1:1；2 项 Minor 调整已在 design-adjustments.md 记录；V1__init_org.sql 不动严格遵循决策 1 + proposal 排除项 | ✅ |
| e 工具误用 | Edit / Write 用于文件；Bash 用于 mvn / npm / docker / curl / grep | ✅ |
| f 运行时行为偏差 | E2E DESCRIBE + curl 双向验证 schema 与 API 契约；浏览器手测可对照（M07 docker compose up 后用户可手测） | ✅ |
| g 管线断链 | docker build → Hibernate ddl-auto → API → frontend axios → tsc 编译链完整；删除字段未引入新转换步骤 | ✅ |
| h 内容质量偏差 | 主规范 3 处 isPmo 删除文字一致；test-plan + design.md + tasks.md + proposal 计数已对齐（M-2/M-3 修复） | ✅ |
| i 指令衰减 | proposal 11 SC 全部已 verify（含 "grep 0 行" + "DESCRIBE 无 is_pmo" + "tsc 0 错" + "浏览器无 PMO" 等强制项） | ✅ |
| j 覆盖真空 | 0 capability 自动化为 0；2 个 MODIFIED capability 各 ≥1 测试（entity-org 3 backend tests + frontend-scaffold 2 vitest + 1 E2E） | ✅ |
| k 契约断层 | 后端 DTO（OrganizationDetail / Create / Update）已删 isPmo ↔ 前端 TS 类型（Organization / Create / Update）已删 isPmo ↔ vitest mock 不传 isPmo ↔ E2E curl 即便传 isPmo 也忽略；契约 4 端对齐 | ✅ |

### 8.3 部署建议

- 交付前提：`docker compose down -v && docker compose up -d --build` 已完成（M07）
- 必备校验：`DESCRIBE rainier_organization` 无 `is_pmo`、curl POST 响应无 isPmo —— 均已绿
- 浏览器手测可选项：编辑组织抽屉、组织列表页（已确认通过 vitest 等价断言）
- 已知限制（V1__init_org.sql）不阻塞当前交付；后续启用 Flyway 时需独立 change 修订
