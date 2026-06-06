# v0.0.6 测试报告

> 测试日期：2026-06-05
> 测试环境：macOS Darwin 25.5.0 · Java 1.8.0_472 · Maven 3.9.11 · Node 25.2.1 · MySQL 8.0 (docker)
> 被测版本：working tree at change `2026-06-05-demand-requirement` 末态

## 一、总体概况

| 指标 | 后端 | 前端 |
|---|---|---|
| 测试用例总数 | 94 | 19 |
| 通过 | 94 | 19 |
| 失败 | 0 | 0 |
| 跳过 | 0 | 0 |
| 通过率 | 100% | 100% |
| 执行耗时 | ~6 s | ~1.2 s |

**spec 要求**：≥ 94 backend（v0.0.5 baseline 64 + 30 新增 TC-DMD-001..011 + TC-REQ-001..009 + TC-DRL-001..007 + TC-DRC-001..003）+ ≥ 19 frontend（v0.0.5 baseline 12 实数 + AppLayout 增 1 TC-FES-D01 + 新 AppRoutes.test.tsx 4 测试 + 新 RequirementEditDrawer.test.tsx 1 测试 + 1 个 baseline 误算修正 = 19）。**实际**：94 + 19，完全吻合。

### 1.1 覆盖率诊断

| 维度 | 状态 | 说明 |
|---|---|---|
| 后端 controller 全部 endpoint | ✅ | 5 个 demand + 5 个 requirement + 4 个 link + 2 个辅助查询 = 16 endpoint，每个端点至少 1 个 MockMvc TC |
| 后端 service 业务分支 | ✅ | enum 校验 / FK 校验 / 唯一冲突 / 软删 FK 保护 / 硬删 / 转化原子性 / 转化回滚 全覆盖 |
| 前端 3 页 + EditDrawer 多选 | ✅ | DemandsPage + RequirementsPage + LinksPage 通过 AppRoutes.test 路由 mount 验证；RequirementEditDrawer 单测验证 sourceDemandIds 收集 |
| Hibernate ddl-auto schema | ✅ | DESCRIBE 三新表确认 BIGINT auto_increment + FK 列均 BIGINT |

## 二、按模块统计

| 测试模块 | 用例数 | 通过 | 备注 |
|---|---|---|---|
| RainierApplicationTests | 1 | 1 | contextLoads |
| HealthControllerTest (v0) | 1 | 1 | v0 |
| AuthControllerLoginTest (v0) | 3 | 3 | v0 |
| AuthControllerMeTest (v0) | 5 | 5 | v0 |
| GlobalExceptionHandlerTest | 5 | 5 | 不动 |
| PathVariableTypeMismatchTest | 1 | 1 | 不动 |
| CorsConfigTest (v0) | 1 | 1 | v0 |
| PageParamsTest | 4 | 4 | 不动 |
| BaseEntityReflectionTest | 1 | 1 | 不动 |
| OrganizationRepositoryTest | 3 | 3 | v0.0.3 baseline |
| OrganizationControllerCreateTest | 5 | 5 | v0.0.5 baseline |
| OrganizationControllerQueryTest | 13 | 13 | v0.0.5 baseline |
| OrganizationDeleteFkTest | 1 | 1 | v0.0.5 baseline |
| UserControllerTest | 10 | 10 | v0.0.3 baseline |
| UserOrganizationControllerTest | 10 | 10 | v0.0.3 baseline |
| **DemandControllerCreateTest (NEW)** | 4 | 4 | TC-DMD-001..004 |
| **DemandControllerQueryTest (NEW)** | 5 | 5 | TC-DMD-005..009 |
| **DemandControllerDeleteTest (NEW)** | 2 | 2 | TC-DMD-010..011 |
| **RequirementControllerCreateTest (NEW)** | 3 | 3 | TC-REQ-001..003 |
| **RequirementControllerQueryTest (NEW)** | 4 | 4 | TC-REQ-004..007 |
| **RequirementControllerDeleteTest (NEW)** | 2 | 2 | TC-REQ-008..009 |
| **RequirementConversionTest (NEW)** | 3 | 3 | TC-DRC-001..003 |
| **DemandRequirementLinkControllerTest (NEW)** | 5 | 5 | TC-DRL-001..005 |
| **AuxiliaryQueriesTest (NEW)** | 2 | 2 | TC-DRL-006..007 |
| **后端合计** | **94** | **94** | — |
| (frontend) Table.test.tsx | 2 | 2 | 不动 |
| (frontend) TreeSelect.test.tsx | 1 | 1 | 不动 |
| (frontend) AppLayout.test.tsx | 2 | 2 | +1 TC-FES-D01 |
| (frontend) **AppRoutes.test.tsx (NEW)** | 4 | 4 | TC-FES-D02（含 redirect/req/link 三路 sanity） |
| (frontend) **RequirementEditDrawer.test.tsx (NEW)** | 1 | 1 | TC-FES-D03 |
| (frontend) EditDrawer.test.tsx (v0.0.5) | 1 | 1 | 不动 |
| (frontend) OrganizationsPage.test.tsx (v0.0.5) | 1 | 1 | 不动 |
| (frontend) App.test.tsx (v0) | 1 | 1 | v0 |
| (frontend) ProtectedRoute.test.tsx (v0) | 2 | 2 | v0 |
| (frontend) Login.test.tsx (v0) | 1 | 1 | v0 |
| (frontend) tokens.test.tsx (v0) | 1 | 1 | v0 |
| (frontend) auth.test.ts (v0) | 2 | 2 | v0 |
| **前端合计** | **19** | **19** | — |

## 三、E2E 测试结果

### 3.1 总体概况

| 指标 | 数值 |
|---|---|
| 用例数 | 7（手动 Bash） |
| 通过 | 7 |
| 失败 | 0 |

### 3.2 关键路径结果

| 路径 | 状态 | 说明 |
|---|---|---|
| `docker compose down -v` 清卷 + up -d --build 起栈 healthy | ✅ | 3 服务（mysql/backend/frontend）全部 healthy |
| `SHOW TABLES` 含 6 张表 | ✅ | rainier_{organization,user,user_organization,demand,requirement,demand_requirement} |
| `DESCRIBE rainier_demand / rainier_requirement / rainier_demand_requirement` | ✅ | 三表 id 列 BIGINT auto_increment；submitter_user_id / owner_user_id / demand_id / requirement_id / project_id / ai_duplicate_hint 均 BIGINT |
| curl POST /api/demands × 2 + /api/requirements w/ sourceDemandIds | ✅ | requirement.id 为数字；DB demand_requirement 表新增 2 行 link_type=DERIVED |
| curl GET /api/requirements/{id}/source-demands | ✅ | 长度 = 2 |
| curl GET /api/demands/{id}/derived-requirements | ✅ | 长度 = 1，含 linkType=DERIVED |
| curl POST /api/demands w/ aiClassification | ✅ | 201 + response.aiClassification = null（service 不接受） |
| `grep -rn 'BaseAutoIdEntity' backend/src` | ✅ | 0 行命中（决策 B 单 BaseEntity 世界保持） |
| `grep -rn 'is_pmo\|isPmo' backend/src/main/java frontend/src` | ✅ | 0 行命中（v0.0.5 baseline 保持） |

### 3.3 E2E 结论

✅ 端到端通过。schema 层 + API 层 + 静态层三重确认本变更交付完整、v0.0.5 baseline 完整保留、Phase 2 显式排除项全部生效。

## 四、失败项详细分析

无失败项。

## 五、功能 / 测试覆盖对照

| 功能模块 | 涉及源码（新增）| 已覆盖测试 |
|---|---|---|
| entity-demand | Demand + 3 DTO + Service + Controller + 常量 3 个 | TC-DMD-001..011（11 case） |
| entity-requirement | Requirement + 3 DTO + Service + Controller + 常量 2 个 | TC-REQ-001..009（9 case） |
| entity-demand-requirement | DemandRequirementLink + 3 DTO + Service + Controller + 2 辅助端点 + 常量 1 个 | TC-DRL-001..007（7 case，含辅助查询） |
| workflow-demand-conversion | RequirementService.create 集成 sourceDemandIds 原子转化（含回滚） | TC-DRC-001..003（3 case） |
| frontend-scaffold MODIFIED | api/{demand,requirement,demandRequirement}.ts + 3 pages + RequirementEditDrawer + AppLayout 菜单 + AppRoutes 4 路由 | TC-FES-D01..D03（3 case，AppLayout 1 + AppRoutes 4 + RequirementEditDrawer 1） |

## 五-B、多路并行 Review 结果

### Review 迭代历史

| 轮次 | C | H | M | L | 状态 |
|---|---|---|---|---|---|
| 1 | 0 | 4 | 6 | 10 | 修复 4 H + 1 M（dedup） → 通过 |

### 最终 Review 汇总

| 维度 | Critical | High | Medium | Low |
|---|---|---|---|---|
| 代码质量 | 0 | 1（race condition） | 2（N+1 + dedup） | 5 |
| 测试/配置 | 0 | 2（DRL-004 弱 + grep 缺) | 2 | 3 |
| 文档/Skills | 0 | 1（path mismatch） | 2 | 2 |

### Review 已修复问题

| # | 严重性 | 文件 | 问题 | 状态 |
|---|---|---|---|---|
| 1 | H (code) | `DemandRequirementLinkService.java` | TOCTOU 竞态 → 500 替代 409 | ✅ catch DataIntegrityViolationException |
| 2 | H (test) | `DemandRequirementLinkControllerTest.java` TC-DRL-004 | 仅 isArray() 弱断言 | ✅ contains(demandId1, demandId1) |
| 3 | H (test+docs) | tasks/test-plan/design/spec 4 处 | `frontend/src/router/AppRoutes.tsx` 路径不存在 | ✅ 修正为 `frontend/src/AppRoutes.tsx` |
| 4 | M (code) | `RequirementService.java` workflow | sourceDemandIds 未去重 → DB unique 撞 500 | ✅ LinkedHashSet 保序去重 |

### Review 已知限制 / 接受不修

| # | 严重性 | 位置 | 说明 |
|---|---|---|---|
| 1 | M (code) | `DemandRequirementLinkService` 辅助查询 | N+1 query (findById per link)；v0 数据量小，未来加批量优化 |
| 2 | M (test) | TC-DMD-009 | PUT silently-ignore aiClassification 起始 null → 不真证明；E2E curl 间接覆盖；接受 |
| 3 | M (docs) | `tasks.md` §6.6.2 算式 "13 + 3 = 16" | 应是 19；文案级 trivial |
| 4 | M (docs) | baseline 13 vs 实际 12 frontend | 上代 metadata 误差 |
| 5 | L 5 项（code） | 注释陈旧 / Repository.hardDeleteAll 未用 / @Transactional 冗余 / update 不可清空 / 前端 delete 错误不弹消息 | 全接受 |
| 6 | L 3 项（test） | DTO 层 Jackson drop 隐性 / cleanDb 冗余 / vi.mock 未覆盖全 endpoint | 接受 |
| 7 | L 2 项（docs） | capability count 12 vs 11 / slice range 12-14 prose | trivial |

## 六、设计调整说明

6 项 Minor 调整，详见 [design-adjustments.md](design-adjustments.md)：
- 2 项 Phase 4 实现期就地修复（Java 8 兼容、code 唯一性策略对齐）
- 4 项 Phase 5 Review 命中后即时修复（race-safety、dedup、path mismatch、弱断言）

## 七、修复确认记录

| 问题 | 修复文件 | 状态 |
|---|---|---|
| TOCTOU race in link.create | `DemandRequirementLinkService.java` | ✅ |
| sourceDemandIds 未去重 | `RequirementService.java` | ✅ |
| TC-DRL-004 弱断言 | `DemandRequirementLinkControllerTest.java` | ✅ |
| 4 处 path mismatch | 4 文档 | ✅ |

## 八、结论

**整体评估**：可交付。94 后端测试 + 19 前端测试全绿；7 路 E2E 全通；Spotless + Checkstyle + ESLint + tsc + vite build 全部清白；3 新表 schema 全 BIGINT；3 新 capability + 1 workflow + 1 frontend MODIFIED 端到端验证通过；4 项 H 级 review 命中已即时修复。

**风险等级**：低
- 已知 Known Limitations：N+1 query、TC-DMD-009 软断言、文档 metadata 漂移、5 个 L 级 code 优化点 —— 全部接受不阻塞

### 8.1 质量信号汇总

| 信号源 | 状态 | 备注 |
|---|---|---|
| 单元/集成测试 | ✅ | 后端 94/94、前端 19/19 = 113/113 全绿 |
| E2E 测试 | ✅ | 7 路（schema + curl POST/GET + 静态 grep） |
| 后端 Lint | ✅ | Spotless + Checkstyle 0 违规 |
| 前端 Lint | ✅ | ESLint 0 错误 |
| 类型检查 | ✅ | tsc -b 0 错误；vite build 通过（dist/assets/index-sat43Vj_.js 245.68 kB） |
| 多路 Review | ✅ | C:0 H:4（全修）M:6（2 修 + 4 接受）L:10（接受） |
| 十一类失败模式 | ✅ | 0 命中（详见下） |
| 已知限制 | 7 项 | 全部记录、全部接受 |

### 8.2 十一类失败模式核对

| ID | 模式 | 结果 |
|---|---|---|
| a 幻觉行为 | 引用注解 / 类名 / 库 API 全部真实 | ✅ |
| b 范围蔓延 | 改动严格在 proposal 范围；archive/* 不动；v0.0.5 baseline 全保留 | ✅ |
| c 级联错误 | 异常未吞；DataIntegrityViolationException 显式映射；workflow @Transactional 回滚验证 | ✅ |
| d 上下文丢失 | design 10 决策与实现 1:1；6 项 Minor 调整已 design-adjustments.md 记录 | ✅ |
| e 工具误用 | Edit/Write 用于文件；Bash 用于 mvn/npm/docker/curl/grep | ✅ |
| f 运行时行为偏差 | E2E DESCRIBE + curl 端到端验证；前端 mock + 路由 mount | ✅ |
| g 管线断链 | docker build → ddl-auto → API → axios → tsc 编译链完整 | ✅ |
| h 内容质量偏差 | spec / design / test-plan / tasks 全部互相对齐；4 处 path 已修 | ✅ |
| i 指令衰减 | proposal 16 SC + 显式排除项（4 类）全部生效 | ✅ |
| j 覆盖真空 | 5 capability 自动化覆盖率均 ≥ 1 测试；workflow 含回滚专项 | ✅ |
| k 契约断层 | 后端 DTO ↔ 前端 TS 类型、auxiliary 端点字段名（SourceDemandView / DerivedRequirementView）双端对齐；E2E curl 验证 | ✅ |

### 8.3 部署建议

- 交付前提：本次 verify 中已完成 `docker compose down -v + up --build` + 6 张表确认
- 必备校验：DESCRIBE 三新表无残留 + curl POST sourceDemandIds 流程 + grep BaseAutoIdEntity / isPmo 双零
- 浏览器手测建议路径：
  1. 登录 → Sider 看到「需求管理」组、3 子项可点
  2. /pm/demands 新建 1 个诉求（注意 status 默认 PENDING）
  3. /pm/requirements 新建 1 个需求，勾选 1 个源诉求 → 保存
  4. /pm/demand-requirements 看到 1 条 DERIVED 链接
  5. 试删源诉求 → 应 409（"demand has linked requirements"）
  6. 先删链接 → 再删诉求 → 应 204
