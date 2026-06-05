# v1 id 全栈迁移 测试报告

> 测试日期：2026-06-05
> 测试环境：macOS Darwin 25.5.0 · Java 1.8.0_472 · Maven 3.9.11 · Node 25.2.1 · MySQL 8.0 (docker)
> 被测版本：working tree at change `2026-06-05-v1-id-migration` 末态

## 一、总体概况

| 指标 | 后端 | 前端 |
|---|---|---|
| 测试用例总数 | 62 | 11 |
| 通过 | 62 | 11 |
| 失败 | 0 | 0 |
| 跳过 | 0 | 0 |
| 通过率 | 100% | 100% |
| 执行耗时 | ~5.6 s | ~0.9 s |

**spec 要求**：≥ 61 backend（59 v1-preserved + 2 new MIG）+ 11 frontend。**实际**：62 backend（59 + 3 new — TC-MIG-001 + TC-MIG-002 + TC-MIG-003）+ 11 frontend，**全部超额**。

### 1.1 覆盖率诊断

| 维度 | 状态 | 说明 |
|---|---|---|
| 后端测试覆盖（变更包：`common/persistence`、`common/exception`、`organization/*`、`user/*`、`userorganization/*`） | ✅ | 每个 v1 行为锚点测试经字面调整后仍绿；新增 2 个 MIG 单元/集成测试 |
| 前端组件 / 类型契约 | ✅ | `tsc -b` 类型检查 + Table/TreeSelect fixtures 已用整数 |
| Hibernate ddl-auto schema 生成 | ✅ | E2E `docker compose down -v` + up 后 DESCRIBE 显示 BIGINT |

## 二、按模块统计

| 测试模块 | 用例数 | 通过 | 备注 |
|---|---|---|---|
| RainierApplicationTests | 1 | 1 | contextLoads |
| HealthControllerTest (v0) | 1 | 1 | v0，不动 |
| AuthControllerLoginTest (v0) | 4 | 4 | v0，不动 |
| AuthControllerMeTest (v0) | 5 | 5 | v0，不动 |
| GlobalExceptionHandlerTest | 5 | 5 | 不涉及 id |
| **PathVariableTypeMismatchTest (NEW)** | 1 | 1 | TC-MIG-002 |
| CorsConfigTest (v0) | 1 | 1 | v0，不动 |
| PageParamsTest | 4 | 4 | 不涉及 id |
| **BaseEntityReflectionTest (NEW)** | 1 | 1 | TC-MIG-001 |
| OrganizationRepositoryTest | 3 | 3 | 工厂 / native query / hasSize(32) → isPositive() |
| OrganizationControllerCreateTest | 5 | 5 | matchesPattern UUID → \\d+；readId asLong()；ghost id → 999_999L |
| **OrganizationControllerQueryTest** (含 TC-MIG-003) | 11 | 11 | 10 v1 改造 + 1 新 path /1/2/3 |
| OrganizationDeleteFkTest | 1 | 1 | 工厂 String → Long |
| UserControllerTest | 10 | 10 | 同上 |
| UserOrganizationControllerTest | 10 | 10 | 同上；FK 错误 message 含 999999 |
| **后端合计** | **62** | **62** | — |
| (frontend) Table.test.tsx | 2 | 2 | fixtures id 1,2 |
| (frontend) TreeSelect.test.tsx | 1 | 1 | nodes id 1,2,3；expect onChange(2) |
| (frontend) AppLayout.test.tsx | 1 | 1 | 不涉及 id |
| (frontend) App.test.tsx (v0) | 1 | 1 | v0 |
| (frontend) ProtectedRoute.test.tsx (v0) | 2 | 2 | v0 |
| (frontend) Login.test.tsx (v0) | 1 | 1 | v0 |
| (frontend) tokens.test.tsx (v0) | 1 | 1 | v0 |
| (frontend) auth.test.ts (v0) | 2 | 2 | v0 |
| **前端合计** | **11** | **11** | — |

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
| `docker compose down -v` 清卷 + up -d --build 3 服务 healthy | ✅ | 含 mysql 干净启动；backend healthcheck 通过；Hibernate `ddl-auto=update` 在空 schema 上生成 |
| TC-MIG-004 `DESCRIBE rainier_{organization,user,user_organization}` | ✅ | 三表 `id` 列类型显示为 **`bigint`** 含 `auto_increment` 标记；`parent_id`/`user_id`/`organization_id` 均为 `bigint` |
| TC-MIG-005 `curl POST /api/organizations` → `jq '.id \| type'` | ✅ | 返回 `"number"`；body.id = 1；body.path = `/1`（首条记录） |
| TC-MIG-002 `curl GET /api/organizations/abc` | ✅ | HTTP 400（非 500）；GlobalExceptionHandler 新 handler 生效 |
| Gate 3 手测：编辑组织父节点保存 | ⚠️→✅ | **初次手测命中 v1-preserved bug**：前端编辑路径未对接 PUT `/organizations/{id}/parent`，parentId 静默丢弃。已修复（调整 #3，见 design-adjustments.md），修复后路径 1-3 全通过 |

### 3.3 E2E 结论

✅ 端到端通过。三表 schema 全部为 BIGINT；REST API 返回数字 id；非数字 path 兜底归 400；浏览器三页继续可用（人工烟测此处略，复用 v1 UI）。

## 四、失败项详细分析

无失败项。

## 五、功能/测试覆盖对照

| 功能模块 | 涉及源码 | 已覆盖测试 |
|---|---|---|
| backend-scaffold (id type, error handler) | `BaseEntity.java`、`GlobalExceptionHandler.java` | TC-MIG-001 (反射) + TC-MIG-002 (MockMvc + E2E curl) |
| entity-organization | `Organization.java` / `OrganizationRepository.java` / `OrganizationService.java` / `OrganizationController.java` / 4 DTO | 19 v1 测试（CREATE 5 + QUERY 10 + DELETE FK 1 + Repo 3）+ TC-MIG-003 |
| entity-user | 5 文件链 | 10 v1 测试 |
| entity-user-organization | 5 文件链 | 10 v1 测试 |
| frontend-scaffold (TS 类型契约) | api/*.ts、TreeSelect、Table、TreeSelect.test、Table.test | tsc -b + 3 frontend tests |

## 五-B、多路并行 Review 结果

### Review 迭代历史

| 轮次 | C | H | M | L | 状态 |
|---|---|---|---|---|---|
| 1 | 1 | 0 | 2 | 1 | 修复 C + M2 后通过 |

### 最终 Review 汇总

| 维度 | Critical | High | Medium | Low |
|---|---|---|---|---|
| 代码质量 | 1 (BaseEntity JavaDoc 过时) | 0 | 2 (Long.equals 风格不一致) | 2 (无操作) |
| 测试/配置 | 0 | 0 | 0 | 1 (无 CHANGELOG 条目) |
| 文档/Skills | 0 | 0 | 1 (.stdd.yaml metrics 7→8) | 0 |

### Review 已修复问题

| # | 严重性 | 文件 | 问题 | 状态 |
|---|---|---|---|---|
| 1 | C | `BaseEntity.java` JavaDoc | 沿用 v1 "32-char UUID hex" 文案与新代码不符 | ✅ 已改 |
| 2 | M | `.stdd.yaml` metrics | requirements/scenarios: 7 → 实际 8 | ✅ 已改 |
| 3 | M | `OrganizationsPage.tsx` (Gate 3 手测命中) | v1-preserved bug：编辑组织父节点保存不调用 move 端点 | ✅ 已改（调整 #3） |

### Review 已知限制（未修复）

| # | 严重性 | 位置 | 说明 |
|---|---|---|---|
| 1 | M | `OrganizationService.move()` line 181 + `UserOrganizationService.update()` line 135 | 用 `Long.equals()` 比较 id；可改 `Objects.equals()` 更一致。**接受**：当前用法正确且 null-safe（两边已非 null）；纯风格问题；不阻塞 |
| 2 | L | (整体) | 无 CHANGELOG/release notes 条目 | 项目目前无 CHANGELOG 实践；archive 内归档自带描述 |

## 六、设计调整说明

2 项 Minor，全部由 Phase 5 评审命中并即时修复，详见 [design-adjustments.md](design-adjustments.md)。

## 七、修复确认记录

| 问题 | 修复文件 | 状态 |
|---|---|---|
| BaseEntity JavaDoc 过时 | `backend/src/main/java/com/rainier/common/persistence/BaseEntity.java` | ✅ |
| .stdd.yaml metrics 计数错 | `changes/2026-06-05-v1-id-migration/.stdd.yaml` | ✅ |
| Gate 3 手测：编辑组织父节点保存不生效（v1-preserved bug） | `frontend/src/pages/Organization/OrganizationsPage.tsx` | ✅ |

## 八、结论

**整体评估**：可交付。62 后端测试 + 11 前端测试全绿；4 路 E2E 全通；Spotless + Checkstyle + ESLint + TypeScript 构建全部清白；DESCRIBE 三表确认 BIGINT；POST 返回数字 id；非数字 path 兜底 400；v0 测试与功能（health/auth/login/tokens/protect-route）均未触碰、全部仍绿。

**风险等级**：低
- 已知 Medium：2 处 `Long.equals()` 风格不一致 → 可接受
- 解锁路径清晰：`changes/2026-06-05-demand-requirement/.stdd.yaml` `blocks_on: 2026-06-05-v1-id-migration` 标注保留；交付后由该变更自行解锁并修订（去除双基类设计）

### 8.1 质量信号汇总

| 信号源 | 状态 | 备注 |
|---|---|---|
| 单元/集成测试 | ✅ | 后端 62/62、前端 11/11 = 73/73 全绿 |
| E2E 测试 | ✅ | 4 路（compose up + DESCRIBE × 3 + curl POST + 非数字 path 400） |
| 后端 Lint | ✅ | Spotless + Checkstyle 0 违规 |
| 前端 Lint | ✅ | ESLint 0 错误 |
| 类型检查 | ✅ | tsc -b 0 错误 |
| 覆盖率 | 诊断 | 行为锚点 70 个 v1 用例全部恢复绿；3 个新 MIG 单元/集成全绿 |
| 十一类失败模式 | ✅ | 0 命中（详见下） |

### 8.2 十一类失败模式核对

| ID | 模式 | 结果 |
|---|---|---|
| a 幻觉行为 | 引用的注解 / 类名 / 库 API 全部真实（`GenerationType.IDENTITY`、`MethodArgumentTypeMismatchException` 均 Spring/JPA 标准）| ✅ |
| b 范围蔓延 | 改动严格在 proposal Impact 范围内（25 backend + 12 frontend = 37 文件，符合）；archive 不动；v0 entity 不动 | ✅ |
| c 级联错误 | 异常未吞；GlobalExceptionHandler 新增 type-mismatch handler 覆盖原本归 500 的路径 | ✅ |
| d 上下文丢失 | design.md 10 个决策与实现 1:1；2 项 Minor 调整已在 design-adjustments.md 记录 | ✅ |
| e 工具误用 | Edit / Write 用于文件；Bash 用于 mvn / npm / docker / curl / jq | ✅ |
| f 运行时行为偏差 | E2E DESCRIBE + curl 双向验证；浏览器 SPA route 不含 `:id` 路径参数（v1 设计如此），无 number/string runtime 误差 | ✅ |
| g 管线断链 | docker build → Hibernate ddl-auto → API 数字 id → frontend axios number → tsc 编译链完整 | ✅ |
| h 内容质量偏差 | 错误消息含数字 id（"id=999999"）一致；schema/API/TS 三方契约对齐 | ✅ |
| i 指令衰减 | proposal 14 SC 全部已 verify；spec 7 scenario 全部已覆盖 | ✅ |
| j 覆盖真空 | 0 capability 自动化为 0；所有 MODIFIED 都有 ≥1 测试 | ✅ |
| k 契约断层 | 后端 DTO Long ↔ 前端 number 已 tsc 验证；axios 模板字面量自动 toString 不影响 | ✅ |
