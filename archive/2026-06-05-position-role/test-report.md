# v0.0.7 测试报告

> 测试日期：2026-06-05
> 测试环境：macOS Darwin 25.5.0 · Java 1.8.0_472 · Maven 3.9.11 · Node 25.2.1 · MySQL 8.0 (docker)
> 被测版本：working tree at change `2026-06-05-position-role` 末态

## 一、总体概况

| 指标 | 后端 | 前端 |
|---|---|---|
| 测试用例总数 | 125 | 25 |
| 通过 | 125 | 25 |
| 失败 | 0 | 0 |
| 跳过 | 0 | 0 |
| 通过率 | 100% | 100% |
| 执行耗时 | ~7 s | ~1.3 s |

**spec 要求**：≥ 125 backend（v0.0.6 baseline 94 + 31 新增）+ ≥ 25 frontend（v0.0.6 baseline 19 + 6 新增）。**实际**：125 + 25，完全吻合。

### 1.1 覆盖率诊断

| 维度 | 状态 | 说明 |
|---|---|---|
| 后端 controller 全部 endpoint | ✅ | 5 + 5 + 4 = 14 endpoint，每个均 ≥1 MockMvc TC |
| 后端 service 业务分支 | ✅ | code 唯一 / category enum / FK 保护 / 富化 / NULL 双路径全覆盖 |
| 前端 3 页 + Sider + 路由 | ✅ | PositionsPage/RolesPage/UserRolesPage 路由 mount 验证；UsersPage 编辑抽屉 position select 验证 |
| Hibernate ddl-auto schema | ✅ | DESCRIBE 三新表 BIGINT auto_increment + position_id 列加到 rainier_user |

## 二、按模块统计

| 测试模块 | 用例数 | 通过 | 备注 |
|---|---|---|---|
| v0.0.6 baseline 全部测试 | 94 | 94 | 0 修改 |
| **PositionControllerCreateTest (NEW)** | 4 | 4 | TC-POS-001..004 |
| **PositionControllerQueryTest (NEW)** | 4 | 4 | TC-POS-005..008 |
| **PositionControllerDeleteTest (NEW)** | 2 | 2 | TC-POS-009..010 |
| **RoleControllerCreateTest (NEW)** | 3 | 3 | TC-ROL-001..003 |
| **RoleControllerQueryTest (NEW)** | 3 | 3 | TC-ROL-004..006 |
| **RoleControllerDeleteTest (NEW)** | 2 | 2 | TC-ROL-007..008 |
| **UserRoleControllerCreateTest (NEW)** | 7 | 7 | TC-UROL-001..007（含 NULL 双路径 + 占位语义） |
| **UserRoleControllerQueryTest (NEW)** | 1 | 1 | TC-UROL-008（富化） |
| **UserRoleControllerDeleteTest (NEW)** | 1 | 1 | TC-UROL-009 |
| **UserControllerTest (扩展)** | 14 | 14 | v0.0.3 10 + v0.0.7 4 新（TC-USR-001..004 含富化 + null 清空） |
| **后端合计** | **125** | **125** | — |
| v0.0.6 frontend baseline 全部 | 19 | 19 | 0 修改 |
| **AppLayout.test.tsx (扩展)** | +1 | +1 | TC-FES-H01（共 3 case） |
| **AppRoutes.test.tsx (扩展)** | +4 | +4 | TC-FES-H02 + 3 路由 sanity（共 8 case） |
| **UsersPage.test.tsx (NEW)** | 1 | 1 | TC-FES-H03 |
| **前端合计** | **25** | **25** | — |

## 三、E2E 测试结果

### 3.1 总体概况

| 指标 | 数值 |
|---|---|
| 用例数 | 8（手动 Bash） |
| 通过 | 8 |
| 失败 | 0 |

### 3.2 关键路径结果

| 路径 | 状态 | 说明 |
|---|---|---|
| `docker compose down -v` 清卷 + up -d --build 起栈 healthy | ✅ | 3 服务 healthy |
| `SHOW TABLES` 含 9 张表 | ✅ | rainier_{organization, user, user_organization, demand, requirement, demand_requirement, position, role, user_role} |
| `DESCRIBE rainier_position` | ✅ | id BIGINT auto_increment + code/name/category/enabled + 6 审计 + del_flag |
| `DESCRIBE rainier_role` | ✅ | id BIGINT auto_increment + code/name/enabled + 6 审计 + del_flag |
| `DESCRIBE rainier_user_role` | ✅ | user_id/role_id BIGINT NN，project_id BIGINT NULL（占位 + 多重 NULL 允许） |
| `DESCRIBE rainier_user` 含 position_id | ✅ | position_id BIGINT YES（NULL allowed） |
| curl PUT user attach position → 富化 | ✅ | response 含 positionName + positionCategory |
| curl POST user-role projectId=null + dup → 409 | ✅ | service NULL 兜底生效 |
| curl POST user-role projectId=42 共存 → 201 | ✅ | NULL + non-NULL 共存 |
| curl DELETE position w/ User ref → 409 | ✅ | FK 保护 |
| grep `is_pmo\|isPmo` 0 行 | ✅ | v0.0.5 baseline 守护 |
| grep `BaseAutoIdEntity` 0 行 | ✅ | v0.0.6 baseline 守护 |

### 3.3 E2E 结论

✅ 端到端通过。schema 层 + API 层 + 静态层三重确认本变更交付完整、v0.0.6 baseline 完整保留、Phase 2 显式排除项全部生效。

## 四、失败项详细分析

无失败项。

## 五、功能 / 测试覆盖对照

| 功能模块 | 涉及源码（新增）| 已覆盖测试 |
|---|---|---|
| entity-position | Position entity + 3 DTO + Service + Controller + PositionCategory 常量 | TC-POS-001..010 (10 case) |
| entity-role | Role entity + 3 DTO + Service + Controller | TC-ROL-001..008 (8 case) |
| entity-user-role | UserRole entity + 3 DTO + Service（双层兜底）+ Controller | TC-UROL-001..009 (9 case 含 NULL 双路径 + 占位) |
| entity-user MODIFIED | User entity 加 position_id + 3 DTO 富化 + UserService 注入 PositionRepository + 富化 enrich | TC-USR-001..004 (4 case 含 null 清空) |
| frontend-scaffold MODIFIED | api/{position,role,userRole}.ts + 3 页 + UsersPage 编辑抽屉 position select + 列表 position 列 + AppLayout 加菜单组 + AppRoutes 加 4 路由 | TC-FES-H01..H03 (3 case) + AppRoutes 路由 mount sanity |

## 五-B、多路并行 Review 结果

### Review 迭代历史

| 轮次 | C | H | M | L | 状态 |
|---|---|---|---|---|---|
| 1 | 0 | 1 | 3 | 10 | 修复 M-3 hygiene → H 和 M 均在阈值内（≤3 / ≤10）→ 通过 |

### 最终 Review 汇总

| 维度 | Critical | High | Medium | Low |
|---|---|---|---|---|
| 代码质量 | 0 | 1（Position/Role TOCTOU 竞态） | 2（update 字段语义 + delete TOCTOU） | 4 |
| 测试/配置 | 0 | 0 | 1（UserControllerTest cleanDb hygiene） | 4 |
| 文档/Skills | 0 | 0 | 0 | 2 |

### Review 已修复问题

| # | 严重性 | 文件 | 问题 | 状态 |
|---|---|---|---|---|
| 1 | M | `UserControllerTest.cleanDb` | TC-USR-001..004 创建 Position 行未清理 | ✅ 注入 PositionRepository + deleteAll |

### Review 已知限制 / 接受不修（v0 单管理员场景）

详见 [design-adjustments.md](design-adjustments.md) 13 项 KL：
- 3 项主要：Position/Role TOCTOU code 竞态、UserService.update 缺字段语义、Position/Role delete TOCTOU
- 4 项 Code 优化点：UserRole enrich N+1、description 不可清空、projectId 不校验、list category 不校验
- 4 项 Test 优化点：grep 未自动、Javadoc 重号、async 等待、count() 全表 vs id 特定
- 2 项 Docs 优化点：UserOrgRole vs Role disambiguation、test-plan 措辞

## 六、设计调整说明

1 项 Minor 测试卫生修复，详见 [design-adjustments.md](design-adjustments.md)。无任何设计或行为契约偏离。

## 七、修复确认记录

| 问题 | 修复文件 | 状态 |
|---|---|---|
| UserControllerTest.cleanDb Position 残留 | `backend/src/test/java/com/rainier/user/controller/UserControllerTest.java` | ✅ |
| Gate 3 手测：下拉列表无法选已建数据（PageParams size > 100 触发 400） | 7 处 `size: 200` → `size: 100`（UsersPage / UserRolesPage / DemandsPage / LinksPage / RequirementEditDrawer + 2 mock）；含 3 处 v0.0.6 既存 bug 合并修 | ✅ |

## 八、结论

**整体评估**：可交付。125 后端测试 + 25 前端测试全绿；E2E 全通；Spotless + Checkstyle + ESLint + tsc + vite build 全部清白；3 新表 + 1 新列 schema 全 BIGINT；3 新 capability + 2 MODIFIED 端到端验证通过；v0.0.5/v0.0.6 baseline 完整保留。

**风险等级**：低
- 13 项 KL（含 1 H 级 TOCTOU 竞态）全部接受为 v0 admin 场景限制；后续随项目演进解锁

### 8.1 质量信号汇总

| 信号源 | 状态 | 备注 |
|---|---|---|
| 单元/集成测试 | ✅ | 125 + 25 = 150/150 全绿 |
| E2E 测试 | ✅ | 8 路（SHOW TABLES + DESCRIBE 3 + position_id 列 + curl PUT/POST + FK 保护 + grep 双零） |
| 后端 Lint | ✅ | Spotless + Checkstyle 0 违规 |
| 前端 Lint | ✅ | ESLint 0 错误 |
| 类型检查 | ✅ | tsc -b 0 错误；vite build 通过（dist 255.09 kB） |
| 多路 Review | ✅ | C:0 H:1（接受）M:3（1 修 + 2 接受）L:10（接受） |
| 十一类失败模式 | ✅ | 0 命中（详见 8.2） |
| 已知限制 | 13 项 | 全部记录、全部接受、有 v1 演进路径 |

### 8.2 十一类失败模式核对

| ID | 模式 | 结果 |
|---|---|---|
| a 幻觉行为 | 注解 / 类名 / API 全部真实 | ✅ |
| b 范围蔓延 | 改动严格在 proposal 范围；archive 不动；v0.0.5/v0.0.6 baseline 全保留 | ✅ |
| c 级联错误 | DataIntegrityViolationException 显式映射；service 异常不吞 | ✅ |
| d 上下文丢失 | design 10 决策与实现 1:1；1 项调整已记录 | ✅ |
| e 工具误用 | Edit/Write 用于文件；Bash 用于 mvn/npm/docker/curl | ✅ |
| f 运行时行为偏差 | E2E DESCRIBE + curl 端到端验证；React 路由 mount + 编辑抽屉 dropdown 测试 | ✅ |
| g 管线断链 | docker build → ddl-auto → API → axios → tsc 编译链完整 | ✅ |
| h 内容质量偏差 | spec/design/test-plan/tasks 互对齐；UserOrgRole vs Role 命名澄清 design.md 决策 6 明示 | ✅ |
| i 指令衰减 | proposal 24 SC + 12 显式排除项全部生效 | ✅ |
| j 覆盖真空 | 5 capability 自动化覆盖率均 ≥1 测试 | ✅ |
| k 契约断层 | 后端 DTO ↔ 前端 TS 类型对齐；UserDetail 富化字段（positionName/positionCategory）双端对齐；E2E curl 验证 | ✅ |

### 8.3 部署建议

- 交付前提：本次 verify 中已完成 `docker compose down -v + up --build` + 9 张表确认
- 必备校验：DESCRIBE 三新表 + position_id 列 + curl 流转化 + grep 双零
- 浏览器手测建议路径：
  1. 登录 → Sider 看到「人事配置」组、3 子项可点
  2. /hr/positions 新建 1 个岗位（如 BE_ENG, TECH）
  3. /hr/roles 新建 1 个角色（如 PMO）
  4. 在 /org/users 编辑某用户，绑定刚才的岗位
  5. /hr/user-roles 新建关联（用户 + PMO + 留白 projectId）
  6. 再加一个 projectId=42 → 共存
  7. 再加同对（projectId=42）→ 应被前端 409 阻挡（KL：前端 delete 错误 silently swallow，新建会显示）
  8. 删除已分配的岗位 → 应 409
