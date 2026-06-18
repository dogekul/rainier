# v0.0.40-me-profile — 测试报告 (Phase 5 VERIFY)

> Baseline: tag `v0.0.39-review-queue` / commit 6e7d049。路线图 #9 个人贡献/能力档案。

## 1. 总体概况

| 维度 | 结果 |
|------|------|
| 后端单元/集成 | **442 / 442** ✅（435 baseline + 7 new；0 fail/error/skip） |
| 前端组件/路由 | **167 / 167** ✅（162 baseline + 5 new；43 files）+ tsc clean + eslint 0 warn |
| 新增后端测试 | MeProfileControllerTest **7/7**（TC-PROF-001..008） |
| 新增前端测试 | ProfilePage.test **3/3** + AppRoutes /profile **1** + navGuardConsistency 自动 +1 |
| E2E（Docker 真 MySQL，real-auth on） | profile 链 + 401 + 数据零改 ✅ |
| 多路评审 (Step 0) | 3 reviewers / 8 findings / **C:0 H:0** / confirmed-real **0** |

## 2. 新增测试（MeProfileControllerTest）

- TC-PROF-001 身份+岗位+组织关系 / TC-PROF-002 直接上级=primary org 在岗 HEAD / **TC-PROF-003 团队 HEAD 上级上溯父组织 HEAD**（2 层组织，可证伪：去掉上溯→null、去掉 skip-self→alice）/ TC-PROF-004 无上级→null / TC-PROF-005+008 贡献计数（他人 Story/Task 不计入，软删经 @Where 排除）/ TC-PROF-006 无 token→401 / TC-PROF-007 token sub 无对应用户→降级 200。

前端：TC-PROFP-01 身份+贡献磁贴+上级 / TC-PROFP-02 组织列表 / TC-PROFP-03 空态+无上级。navGuardConsistency 自动断言 `isAdminPath('/profile')===false`。

## 3. E2E（live stack — Docker，真 MySQL，real-auth on）

| # | 验证 | 结果 |
|---|------|------|
| 1 | `GET /api/me/profile`(alice) | 200；name="Alice"、positionName="后台开发"/TECH、2 个 TEAM HEAD membership（primary 标记）、ownedStoryCount=3、assignedTaskCount=4 ✅ |
| 2 | 无 token | **401** ✅ |
| 3 | manager 推导 | alice 为其团队唯一 HEAD 且无父组织 HEAD → manager=null（正确）✅ |
| 4 | 存量业务数据（纯读） | users 7 / projects 5 / stories 13 / user_org 8 不变 ✅ |

> standing 约束：本端点纯只读聚合，零写；存量数据零改。

## 4. 多路并行技术评审（Step 0）+ 11 类失败模式

**3 reviewers（code / test-config / docs-spec）**：8 findings，**C:0 H:0**，对抗式 verify 后 **confirmed-real = 0**。

核心 confirmations：
- **manager 上溯可证安全**：`while(depth<8)` + `depth++`，环形 parentId 至多 8 跳终止；skip-self + first-non-self-HEAD + parentId 上爬语义与 TC-002/003/004 吻合。
- **软删跨两种模型正确**：Story/Task 经 `@Where(del_flag=0)` 计数仅活行；UserOrganization 是硬删（`leftAt`）→ `findBy...LeftAtIsNull` 是正确的在岗谓词（非 del_flag）。
- **org `@Where` 一致**：软删 org 经 `findAllById` 的 `o==null` continue 丢弃；上爬中软删父 org→`findById` 空→`orgId=null`→干净停止。
- **null-safe 全面**：me==null 降级、positionId 守卫、`getType()/getRole()` 的 `.name()` 守卫、Objects::nonNull 过滤。
- **Java-8 clean**：无 var/Set.of/无参 orElseThrow/Stream.toList；仅 `Collectors.toList()`（Docker temurin-8 已编译验证）。
- **DTO 契约对齐**：ProfileResponse.memberships 初始化为空 list（线上永不 null），嵌套 Membership/Manager 与 `api/profile.ts` 字段逐一对齐。
- **测试非重言式**：TC-003 上溯是关键判别用例（可证伪）；TC-005 他人 Task 排除使计数断言有意义；degrade 钉 me==null 分支。

**11 类失败模式**：无幻觉；范围聚焦（NEW me-profile + frontend-scaffold MOD）；契约 (k) 前后端字段对齐；无覆盖真空 (j)；(d) design.md D1–D7 与代码逐条吻合。

## 5. 设计取舍（记录，不阻塞）

- **P1 仅本人**：`/api/me/profile` 自助；下属视图（`/api/users/{id}/profile` 限本人+直接上级）后续。
- **P4 精简 2 计数**：ownedStoryCount + assignedTaskCount；按状态分布/本周等更丰富指标后续。
- manager 上溯仅取首个非本人 HEAD（depth cap 8）；多上级/矩阵汇报不建模。

## 6. 结论

| 信号 | 状态 |
|------|------|
| 后端 442/442 + 前端 167/167 + tsc/lint | ✅ |
| 新增 7 后端 + 5 前端测试全绿 | ✅ |
| E2E（真实用户档案 + 401 + 数据零改） | ✅ |
| Docker 真 JDK-8 构建 | ✅ |
| 多路评审 C:0 H:0 confirmed-real:0 | ✅ |
| 团队成员成长档案钩子补齐（org 身份+岗位+上级+贡献） | ✅ |

**部署建议**：可交付。后续候选：下属视图（本人+直接上级）、更丰富贡献指标（按状态/本周/趋势）、岗位能力标签。
