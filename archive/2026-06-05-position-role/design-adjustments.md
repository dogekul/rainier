# 设计调整说明 — 岗位 + 角色双轴建模 v0.0.7

> 原始设计基线：Phase 2 design.md（10 决策）+ 5 个 spec + test-plan.md
> 调整来源：Phase 5 多路并行 Review

## 调整汇总

| # | 调整类型 | 涉及文件 | 严重程度 | 调整阶段 | 用户已知 |
|---|---|---|---|---|---|
| 1 | 测试卫生（review 命中） | `UserControllerTest.cleanDb` | Minor | Phase 5 Step 0 | 是 |
| 2 | v0.0.6-preserved bug 顺手修复（Gate 3 手测命中） | 5 业务页 + 2 测试 mock 中的 `size: 200` → `size: 100` | Minor | Phase 5 Gate 3 | 是 |

无任何设计层或行为契约偏离 — 所有 spec Scenario 的可观察行为与原稿一致。

## 调整详细说明

### 调整 1：UserControllerTest.cleanDb 加 positionRepo.deleteAll（Minor）

- **原始**：`@BeforeEach cleanDb` 只清 user 表，TC-USR-001..004 创建的 Position 行（code=BE_ENG/PO）会跨 test 残留
- **问题**：Position.code 仅 service 层唯一（无 DB UNIQUE，与设计决策一致），残留行不会让重复 POST 失败但会污染测试数据视图
- **修复**：注入 PositionRepository 并加 deleteAll；保持与 PositionControllerDeleteTest cleanDb 模式一致
- **触发**：Phase 5 Step 0 Test/Config 代理报 M-1
- **影响**：仅测试卫生；125/125 测试仍绿；不动行为或 spec

### 调整 2：v0.0.6-preserved size 上限 bug 顺手修复（Minor，Gate 3 手测命中）

- **原始问题**：前端"拉池"调用统一用 `size: 200`，但 `PageParams` 后端校验 `size <= 100` → 所有依赖下拉返 400 `"Validation failed: size must be <= 100"` → 下拉列表只有 placeholder option，**无法选择实际数据**
- **历史**：bug 实际是 v0.0.6 引入的 — Demand/Requirement/Link 三页 + RequirementEditDrawer 都用了 size: 200，但 v0.0.6 测试用 vi.mock 绕过了真后端校验，整个变更期未暴露；v0.0.7 复制了同款模式（UsersPage / UserRolesPage）；用户在 Gate 3 手测时命中
- **症状**：
  - UsersPage 编辑抽屉「岗位」下拉只有"（未定级）" — 无法选择已建岗位
  - UserRolesPage 新建抽屉「用户」「角色」下拉只有"请选择" — 无法选已建数据
  - DemandsPage 新建抽屉「提交人」下拉只有"请选择"
  - RequirementEditDrawer「负责 PO」+「源诉求」分区无法加载
  - LinksPage 新建关联抽屉「诉求」「需求」下拉无法选
- **修复**：7 处业务调用 `size: 200` → `size: 100`；2 处测试 mock 元数据同步改 100（不影响行为，仅为一致性）
- **触达文件**：
  - `frontend/src/pages/User/UsersPage.tsx` (1)
  - `frontend/src/pages/UserRole/UserRolesPage.tsx` (2)
  - `frontend/src/pages/Demand/DemandsPage.tsx` (1, v0.0.6 既存)
  - `frontend/src/pages/DemandRequirement/LinksPage.tsx` (2, v0.0.6 既存)
  - `frontend/src/pages/Requirement/RequirementEditDrawer.tsx` (1, v0.0.6 既存)
  - `frontend/src/pages/User/UsersPage.test.tsx` (1, mock meta)
  - `frontend/src/pages/Requirement/RequirementEditDrawer.test.tsx` (1, mock meta, v0.0.6 既存)
- **触发**：Phase 5 Gate 3 用户手测："新建或编辑用户的时候，岗位选择下拉列表无法选择岗位；新建用户角色关系时，下拉列表无法选择已有用户和已有角色"
- **影响**：纯前端代码 4 处新（v0.0.7）+ 3 处旧（v0.0.6）合并修；25/25 frontend 测试仍绿；ESLint 干净；DB 数据零变更；docker 容器 frontend 单独 rebuild + recreate，mysql 卷不动
- **范围合规**：v0.0.6 既存 3 处文件在本次 spec 中未列入 Impact，但跟 v0.0.7 新引入的 4 处是**同一类全栈 bug**；按 v0.0.4 Adjustment #3 同款"Gate 3 手测命中即时修"原则合并；不另开 hotfix change

#### 数据保留确认

修复**只触前端代码** + frontend 容器单独 rebuild + recreate。用户在 verify 期间手动操作的所有数据完整保留：
- positions: id=1 BE_ENG / 后台开发 / TECH（name 由用户编辑过）
- roles: id=1 PMO / id=2 YFM 研发负责人（用户新建）
- users: id=1 Alice / id=2 lili 黎立（用户新建）
- user-roles: id=1 (alice, PMO, null) / id=2 (alice, PMO, 42)

## 不构成调整的 review 命中

### Known Limitations / 接受不修（v0 单管理员场景）

| # | 来源 | 严重 | 描述 | 接受理由 |
|---|---|---|---|---|
| KL-1 | Code H-1 | H | PositionService.create / RoleService.create 仅靠 `existsByCode` 单点判重，无 DB UNIQUE 兜底 → TOCTOU 竞态可产生重复 code 行 | v0 阶段仅 admin 单用户操作，并发场景不现实；DB UNIQUE 会与"复用 soft-deleted code"语义冲突（同 v0.0.6 Requirement.code 决策）；记录 KL 等 v1 引入分布式锁 |
| KL-2 | Code M-1 | M | `UserService.update` 在 body 缺 positionId 字段时静默清空 positionId（DTO comment 已声明） | DTO comment 明示了行为；test TC-USR-004 显式覆盖 null path；只缺 "absent key" 测试。语义就是"set or clear"；前端始终显式传值，不会触发歧义 |
| KL-3 | Code M-2 | M | Position/Role delete 检查 + delete 之间的窗口，并发可让 user create 或 user_role create 引用刚删的 entity → orphan + enrichment 显示 null | v0 admin 单用户；与 v0.0.6 已接受的 N+1 / TOCTOU KL 同性质；不影响数据一致性（FK 关系存在于 application 层） |
| KL-4 | Code L-1 | L | UserRoleService.enrich 每行查 user/role 仓库 (2N+1 query) | 沿用 v0.0.6 N+1 enrichment KL；v0 数据量小 |
| KL-5 | Code L-2 | L | Position/Role update 的 description 字段 `if != null` 跳过，无法显式清空 | 与 v0.0.3 User.update / v0.0.6 Demand.update 同款不一致；项目级 KL，下次统一 PATCH 语义时一并修 |
| KL-6 | Code L-3 | L | UserRoleService.projectId 不校验（占位语义） | proposal explicitly_excluded 明示；test TC-UROL-007 显式验证；按 spec |
| KL-7 | Code L-4 | L | PositionService.list category 参数无校验，乱码 ?category=GARBAGE 返空页 | 宽容查询语义；下次统一时跟 update 一起重构 |
| KL-8 | Test L-1 | L | TC-FES-H02 grep 校验文档化但未自动化执行 | 沿用 v0.0.6 同款做法；交付阶段 verify 时已人工跑过 grep 返 2 |
| KL-9 | Test L-2 | L | UserControllerTest 类 Javadoc "Covers TC-USR-001..011" 是 v0.0.3 注释，与 v0.0.7 新增 TC-USR-001..004 ID 重号 | Java 方法名唯一不影响运行；纯文档；下次 deliver 阶段顺手修 |
| KL-10 | Test L-3 | L | UsersPage.test.tsx 没显式 `findByText('Backend Engineer')` 等待 listPositions resolve | `setPositionId(1)` 走 raw onChange 事件，state 立即更新；测试通过；防御性可加但当前已足 |
| KL-11 | Test L-4 | L | TC-UROL-009 spec 写 "repo.count(id=1)=0"，实际测试断言 `repo.count()==0` 全表 | cleanDb 已隔离；语义等价；文档措辞 nit |
| KL-12 | Docs L-1 | L | design.md 决策 6 承诺"在 Spec 显式说明 UserOrgRole vs Role 并存"，但 change-local entity-user-role spec 无声明 | 交付时合并到主 specs 可补 1-2 行 disambiguation；本次先 KL |
| KL-13 | Docs L-2 | L | test-plan.md 第 164 行脚注措辞略迷糊（"非对称的 4 个"） | 纯文案；TC 计数本身正确 |

## 结论

1 项 Minor 调整就地修复；13 项 KL 接受不动并全部记录到 test-report。无任何设计层或行为契约偏离。
