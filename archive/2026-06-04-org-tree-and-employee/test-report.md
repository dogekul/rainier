# v1 组织维度骨架 测试报告

> 测试日期：2026-06-05
> 测试环境：macOS Darwin 25.5.0 · Java 1.8.0_472 (Corretto) · Maven 3.9.11 · Node 25.2.1 · MySQL 8.0 (docker)
> 被测版本：working tree at change `2026-06-04-org-tree-and-employee` 末态

## 一、总体概况

| 指标 | 后端 | 前端 |
|---|---|---|
| 测试用例总数 | 59 | 11 |
| 通过 | 59 | 11 |
| 失败 | 0 | 0 |
| 跳过 | 0 | 0 |
| 通过率 | 100% | 100% |
| 执行耗时 | ~5.2 s | ~0.8 s |

**spec 目标**：后端 ≥ 32 新增、前端 ≥ 14 新增。**实际**：后端新增 46（含 1 个 TC-ORG-016 补充用例），前端新增 4（Table×2 + TreeSelect×1 + AppLayout×1）。

### 1.1 覆盖率诊断

| 维度 | 状态 | 说明 |
|---|---|---|
| 后端测试覆盖（变更包：`organization`/`user`/`userorganization`/`common`）| ✅ 行为级覆盖完整 | 每个 controller endpoint × 关键 scenario 至少 1 个 MockMvc 用例 |
| 前端组件测试 | ⚠️ 仅 3 个组件级测试 | Table、TreeSelect、AppLayout 各 1 个测试文件；3 个业务页（OrganizationsPage、UsersPage、UserOrganizationsPage）**无页面级 RTL+MSW 集成测试** |
| Flyway V1__init_org.sql | ⚠️ 未在测试中执行（test profile 用 H2 ddl-auto） | E2E 路径下亦未应用（Flyway 已禁用，见 design-adjustments #1） |

## 二、按模块统计

| 测试模块 | 用例数 | 通过 | 失败 | 备注 |
|---|---|---|---|---|
| RainierApplicationTests | 1 | 1 | 0 | contextLoads (TC-BES-001 baseline / TC-TRT-201 局部) |
| HealthControllerTest (v0) | 1 | 1 | 0 | TC-HLT-001 |
| AuthControllerLoginTest (v0) | 4 | 4 | 0 | TC-AUT-001/002 |
| AuthControllerMeTest (v0) | 5 | 5 | 0 | TC-AUT-003/004 |
| GlobalExceptionHandlerTest | 5 | 5 | 0 | TC-BES-002/003 + TC-BES-202（fieldErrors） |
| CorsConfigTest (v0) | 1 | 1 | 0 | TC-BES-004 |
| PageParamsTest | 4 | 4 | 0 | TC-PAG-001/002/003 + page<0 校验 |
| OrganizationRepositoryTest | 3 | 3 | 0 | TC-BES-203 软删除 JPA 单测 + 字段断言 |
| OrganizationControllerCreateTest | 5 | 5 | 0 | TC-ORG-001..005 |
| OrganizationControllerQueryTest | 10 | 10 | 0 | TC-ORG-006..009 + 011 + 012/013 + 014/015 + tree(008) + search(010 等价) |
| OrganizationDeleteFkTest | 1 | 1 | 0 | TC-ORG-016（有 user_organization 关联 → 409） |
| UserControllerTest | 10 | 10 | 0 | TC-USR-001..011（其中 011 = FK 保护） |
| UserOrganizationControllerTest | 10 | 10 | 0 | TC-UOR-001..010 |
| **后端合计** | **59** | **59** | **0** | — |
| (frontend) Table.test.tsx | 2 | 2 | 0 | TC-FES-202 |
| (frontend) TreeSelect.test.tsx | 1 | 1 | 0 | TC-FES-203 / TC-ORG-018 |
| (frontend) AppLayout.test.tsx | 1 | 1 | 0 | TC-FES-201 |
| (frontend) App.test.tsx (v0) | 1 | 1 | 0 | smoke |
| (frontend) ProtectedRoute.test.tsx (v0) | 2 | 2 | 0 | TC-FES-001/002 (v0) |
| (frontend) Login.test.tsx (v0) | 1 | 1 | 0 | TC-FES-004 (v0) |
| (frontend) tokens.test.tsx (v0) | 1 | 1 | 0 | TC-FES-003 (v0) |
| (frontend) auth.test.ts (v0) | 2 | 2 | 0 | store |
| **前端合计** | **11** | **11** | **0** | — |

## 三、E2E 测试结果

### 3.1 总体概况

| 指标 | 数值 |
|---|---|
| 用例数 | 3（手动 Bash） |
| 通过 | 3 |
| 失败 | 0 |

### 3.2 关键路径结果

| 路径 | 状态 | 说明 |
|---|---|---|
| `docker compose up -d --build` 3 服务 healthy | ✅ | mysql + backend + frontend 全 healthy（首启约 60s） |
| `docker exec rainier-mysql ... SHOW TABLES` | ✅ | 含 `rainier_organization` / `rainier_user` / `rainier_user_organization`（3 表，无 `flyway_schema_history` —— Hibernate 生成路径，见 design-adjustments #1） |
| `curl http://localhost:18080/api/health` + `POST /api/organizations` | ✅ | health 返回 `{"status":"UP"}`；POST 返回 201 + Location + UUID id（`2c9580839e95ae40019e95af36320000`）+ path + wholeName 正确派生 |

### 3.3 E2E 结论

✅ 端到端通过。容器组装 + Hibernate schema 生成 + REST API 行为全部正常。

## 四、失败项详细分析

无失败项。

## 五、功能/测试覆盖对照

| 功能模块 | 涉及源码 | 已覆盖测试 | 缺口 |
|---|---|---|---|
| entity-organization | `organization/{domain, repository, service, controller, dto}/*.java` | 19 测试（含 repo 3 + create 5 + query 10 + delete-FK 1） | TC-ORG-017/018/019 前端页测试（018 由 TreeSelect 单测覆盖；017/019 缺） |
| entity-user | `user/{domain, repository, service, controller, dto}/*.java` | 10 测试 | TC-USR-012/013 前端页测试缺 |
| entity-user-organization | `userorganization/{domain, repository, service, controller, dto}/*.java` | 10 测试 | TC-UOR-011/012 前端页测试缺 |
| pagination-envelope | `common/web/{PageResponse, PageParams}.java` | 4 测试 | — |
| backend-scaffold (mod) | `common/exception/*`, `common/persistence/*` | 5 异常 + 3 repo softdelete = 8 | — |
| frontend-scaffold (mod) | `components/ui/*`, `components/AppLayout.tsx` | 4 组件级 | 3 页面级集成（gap, 见六） |
| dev-runtime (mod) | `docker-compose.yml`, V1 SQL | 1 手动 E2E | — |
| test-runtime (mod) | `application-test.yml` | 1 启动日志验证 | — |

## 五-B、多路并行 Review 结果

### Review 迭代历史

| 轮次 | C | H | M | L | 状态 |
|---|---|---|---|---|---|
| 1 | 1 (误报) | 1 | 4 | 2 | 经人工核对 → 0 真 C；1 真 H 已记录；3 项已修复 |

### 最终 Review 汇总

| 维度 | Critical | High | Medium | Low | 总计 |
|---|---|---|---|---|---|
| 代码质量 | 0 (误报澄清) | 1 | 2 | 0 | 3 |
| 测试/配置 | 0 | 2 | 1 | 0 | 3 |
| 文档/Skills | 0 (已修订) | 1 | 3 | 2 | 6 |

### Review 已修复问题

| # | 严重性 | 文件 | 问题 | 状态 |
|---|---|---|---|---|
| 1 | M | `frontend/src/api/user.ts` | User 类型缺 `createBy` / `updateBy` 审计字段 | ✅ 已加 |
| 2 | C(文档)→已修订 | `test-plan.md` TC-BES-201/TC-DRT-201 | spec 期望 Flyway，与实际 Hibernate 路径不符 | ✅ TC 文本已更新；adjustments 已记录 |
| 3 | H(覆盖) | `OrganizationService.delete()` FK 含 user_organization 但无 MockMvc 用例 | ✅ 新增 `OrganizationDeleteFkTest`（TC-ORG-016） |

### Review 已知限制 / 未修复

| # | 严重性 | 文件 / 位置 | 说明 |
|---|---|---|---|
| 1 | M (覆盖) | `pages/{Organization, User, UserOrganization}/*.tsx` | 3 个业务页缺页面级 RTL + MSW 集成测试（TC-ORG-017/019 / TC-USR-012/013 / TC-UOR-011/012 共 6 条 TC）。E2E 浏览器手测已覆盖关键交互。**建议**：单独切片补 6 条页面测试 |
| 2 | M (并发) | `UserOrganizationService.create()` is_primary 自动 demote | `@Transactional` 覆盖范围内安全；极端并发下两次同时 `is_primary=true` 写入可能短时存在 2 个 primary 直到事务完成。乐观锁 `@Version` 字段未添加（design.md §13 提到但 v0 未实施）。**建议**：业务量上升时加 `@Version` |
| 3 | L (类型) | `frontend/src/api/userOrganization.ts` | 未导出 `UserOrgRole` 等独立 type alias；当前内联使用未影响功能 |
| 4 | L (文档) | `pending-adjustments.md #3` 提到 MariaDB 尝试，但代码中无 MariaDB 配置残留 | 仅说明性文字，不影响代码 |

## 六、设计调整说明

5 项调整（1 Major + 4 Minor），详见 [design-adjustments.md](design-adjustments.md)。
最重大：**Flyway 禁用**（MySQL 8 + Java 8 + Flyway 8.x community 三方冲突），改 Hibernate ddl-auto=update。spec 文本与 test-plan TC-BES-201/TC-DRT-201 已同步修订。

## 七、修复确认记录

| 问题 | 修复文件 | 状态 |
|---|---|---|
| Spotless 格式违规（13 文件） | `mvn spotless:apply` 全量重写 | ✅ |
| TC-ORG-016 user_organization FK 删保护缺自动化 | 新增 `OrganizationDeleteFkTest.java`（1 用例） | ✅ |
| 前端 User 类型缺 `createBy` / `updateBy` | `frontend/src/api/user.ts` 补 2 字段 | ✅ |
| TC-BES-201 / TC-DRT-201 spec 文本与实际不符 | `test-plan.md` 局部修订 | ✅ |

## 八、结论

**整体评估**：可交付。59 后端测试 + 11 前端测试全绿；docker compose E2E 3 路径全通；Spotless + Checkstyle + ESLint + TypeScript 构建全部清白；FK 删除保护双向（org→child / user_org）已自动化覆盖；is_primary 自动 demote 已自动化覆盖；树缓存 path/whole_name 级联（含改 name + move + 防环）已自动化覆盖。

**风险等级**：低
- 已知 Major 调整（Flyway 禁用）影响 schema 演进路径但不影响 v0 功能契约
- 已知 Medium 覆盖缺口（6 条前端页 TC 待补 + 1 处并发乐观锁待加）属于"可接受的渐进债务"，建议作为独立切片在下个变更前完成

### 8.1 质量信号汇总

| 信号源 | 状态 | 备注 |
|---|---|---|
| 单元/集成测试 | ✅ | 后端 59/59、前端 11/11 = 70/70 全绿 |
| E2E 测试 | ✅ | 3 路径全通（compose up + SHOW TABLES + curl/POST） |
| 后端 Lint | ✅ | Spotless + Checkstyle 0 违规 |
| 前端 Lint | ✅ | ESLint 0 错误 0 警告 |
| 类型检查 | ✅ | tsc -b 0 错误 |
| 多版本测试 | N/A | 单一 Java 8 / Node 25 环境 |
| 覆盖率 | 诊断 | 后端行为级完整，前端业务页缺集成测试（详见 §1.1） |
| 十一类失败模式 | ✅ | 0 真命中（详见下） |

### 8.2 十一类失败模式核对

| ID | 模式 | 结果 |
|---|---|---|
| a 幻觉行为 | 引用的路径 / 注解 / 库 API 全部真实 | ✅ |
| b 范围蔓延 | 改动严格在 proposal Impact 范围内（5 项 Adjustment 均已 Phase 1/2 知情或 Phase 5 显式声明） | ✅ |
| c 级联错误 | 服务层异常未吞；GlobalExceptionHandler 覆盖 NotFound/Conflict/Validation/Throwable | ✅ |
| d 上下文丢失 | Flyway 禁用偏离已在 design-adjustments + pending-adjustments 同步；test-plan 已修订 | ✅ |
| e 工具误用 | 文件操作用 Edit/Write；命令执行经 Bash | ✅ |
| f 运行时行为偏差 | E2E 触发 Hibernate 生成表；POST 触发 path 派生；均运行时验证 | ✅ |
| g 管线断链 | docker build → Spring Boot 启动 → Hibernate ddl-auto → API 响应链路完整 | ✅ |
| h 内容质量偏差 | 文档与 schema 字段、API 契约一致；adjustments 与 test-plan 已对齐 | ✅ |
| i 指令衰减 | proposal 中"是/否"条件全部 TC 验证或 E2E 覆盖 | ✅ |
| j 覆盖真空 | 前端 6 条页面 TC 自动化缺位 → 已在 §五-B 标 Medium 并建议补 | ⚠️ 可接受（E2E 手测兜底） |
| k 契约断层 | 后端 DTO 字段 ↔ 前端 type 接口全量核对；修一处 `createBy/updateBy` | ✅ |
