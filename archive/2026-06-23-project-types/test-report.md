# Test Report: v0.0.48 — 项目类型拓展 + 立项创建/关联对外-交付

> 范围：ProjectType 轻量 + 三正式子类（主业-功能建设/主业-技术改造/对外-交付）、FORMAL 退役迁移；立项 initiate 原子「创建或关联」对外-交付项目。
> 基线 v0.0.47-board-redesign / 5c20dab。

## 1. 总体概况

| 维度 | 结果 |
|---|---|
| 后端单元/集成 (temurin-8) | **524 / 524** ✅（515 v0.0.47 + 本版净 +9：TC-INI-01..07 / TC-PROJTYPE 迁移+新类型 / backfill 迁移；0 fail/error/skip） |
| 前端组件/路由 | **248 / 248** ✅（54 files；DeliveryFlow TC-FDH-01/01b/02/03/04 + ProjectsPage 类型四项 + 项目类型测试更新）+ tsc clean + eslint 0 warn |
| E2E（docker 真 MySQL，前后端重建部署） | green（含两处真栈 bug 修复见下） |
| 多代理评审 (Step 0) | 4 维度 + 对抗式 verify：**14 raw → 确认 8 全 real（C:0 H:2 M:4 L:6）**（工作流因会话切换两次中断，结果自 journal 恢复）；确认项已处置 |
| 数据 | 既有 FORMAL→CORE_FEATURE 迁移（用户确认）；误改的真实商机 30/33 已还原；5 个 __E2E 测试项目待用户授权清理 |

## 2. 改动

- 后端：`ProjectType` +CORE_FEATURE/CORE_TECH/EXTERNAL_DELIVERY（ALL 含 4 类），FORMAL → `LEGACY_FORMAL`（不入 ALL）；`Project.projectType` + DTO `@Size` 16→**32**（容纳 EXTERNAL_DELIVERY=17）；NEW `ProjectTypeColumnWiden` 启动 ALTER 加宽既有 MySQL 列；`ProjectTypeBackfill` +FORMAL→CORE_FEATURE 迁移；`OpportunityInitiateRequest` projectId 可空 +projectCode/projectName/projectOwnerUserId；`OpportunityService.initiate(req)` 注入 ProjectService、resolveDeliveryProject（关联校验 EXTERNAL_DELIVERY / 内联建 + owner 默认 pmUserId / 二选一互斥 / REJECT 不要求项目）。
- 前端：`api/project.ts` ProjectType 4 值 + 共享 OPTIONS/LABELS；ProjectsPage 用共享常量（下拉 4 项）；`initiateOpportunity(id, body)` 新签名；DeliveryFlow 立项 关联/新建模式 + **项目负责人下拉**（默认当前用户）+ 后端 message 透出。
- 无新表/依赖/API。

## 3. E2E（live stack — Docker 真 MySQL + JDK-8，前后端均重建）

| # | 步骤 | 结果 |
|---|---|---|
| A | 创建 CASUAL/CORE_FEATURE/CORE_TECH/EXTERNAL_DELIVERY 项目 | 各 201 ✅（EXTERNAL_DELIVERY 经列加宽后通过） |
| B | 创建 FORMAL（退役） | 400 ✅ |
| C | 立项 关联 EXTERNAL_DELIVERY 项目 | 200，opp.projectId 链入 ✅ |
| D | 立项 内联新建（带 owner） | 200，新项目 type=EXTERNAL_DELIVERY 并关联 ✅ |
| E | 立项 内联新建（商机无 pm、无 owner） | 400「需指定负责人」✅（用户实测命中的分支） |
| F | 前端 bundle | delivery-mode-create / delivery-new-owner / 主业-功能建设 / 对外-交付 均在 ✅ |

### 3.1 真栈发现的两处缺陷（H 级，均已修）
1. **VARCHAR(16) 溢出**：`EXTERNAL_DELIVERY`(17) 在既有 MySQL 列（16）上插入 500——`ddl-auto=update` 不会加宽既有列。H2 测试因每次重建 schema 无法发现。**修**：entity/DTO 16→32 + `ProjectTypeColumnWiden` 启动 ALTER（幂等、H2 上 try/catch 跳过）。
2. **立项 400（用户实测）**：内联新建项目默认取商机 pmUserId 作负责人，但既有 INITIATION 商机均无 pm → 400。**修**：DeliveryFlow 新建表单加项目负责人下拉（默认当前用户），提交带 projectOwnerUserId；后端保留 null 守卫 + 清晰 400。

## 4. 多代理评审（Step 0）+ 处置

4 维度（backend/frontend/test-spec/failure-modes）+ 对抗式 verify。工作流两次因会话切换中断，结果自 `journal.jsonl` 恢复（8 条 verdict 全 real）。**14 raw → 确认 8（C:0 H:2 M:4 L:6）**。

| 严重度 | 发现 | 处置 |
|---|---|---|
| H | EXTERNAL_DELIVERY 不入既有 VARCHAR(16) | **已修**（widen 迁移，§3.1-1，真栈验证 201） |
| H | 内联新建缺 owner→400 分支无测试 | **已修** TC-INI-05（+ 真栈 §3-E） |
| M | 同传 projectId+新建字段→400 无测试 | **已修** TC-INI-06 |
| M | projectId 不存在→400 无测试 | **已修** TC-INI-07 |
| M | 立项 error 渲染丢弃后端 message（仅 axios 通用串） | **已修** 改用 `e.response.data.message`（对齐 PresaleFlow）+ TC-FDH-04 |
| M | 前端 error 渲染/校验分支无测试 | **已修** TC-FDH-04 |
| L | TC-PROJTYPE-010 编号跨文件重复 | **已修** 控制器测试改 TC-PROJTYPE-011/012 |
| L | 关联 tab 无项目时禁用未断言 / listProjects 无 catch / REJECT 放宽无后端测试 / backfill 二次幂等未断言 | 记录；低风险、看板/立项只读语义不变（未逐一补，均 < 阈值附录项） |

评审结论：C:0 H:2 M:4 L:6；H/M 全部修复，L 多数修复、其余记录。

### 4.1 评审后 UX 折入（用户反馈，两轮）

- 一轮：立项新建表单加**项目负责人下拉**（默认当前用户/商机 PM）；失败展示后端 message（修用户实测 400）。
- 二轮（修正）：立项抽屉**默认「新建」模式，与项目数量无关**——立项主流动作是为赢单新建交付项目；放弃首版「按项目数量切默认」（项目变多后会埋没新建）。「关联已有」为次要一键选项。TC-FDH-01b 钉住「即使已有项目仍默认新建」，TC-FDH-03 钉住「无项目时新建表单直接可用」。

## 5. 失败模式（a–k）

- (b) 范围：仅改计划内文件 + 1 个新 bootstrap（widen）。
- (d) 一致：实现符合 design.md D1-D4（FORMAL 迁移 / 原子 initiate / 链 EXTERNAL_DELIVERY / 前端共享常量）。
- (c) 吞错：DeliveryFlow 现透出后端 friendly message。
- (k) 契约：前后端 initiate JSON（projectId/projectCode/projectName/projectOwnerUserId/decision）+ ProjectType 值 + list `projectType` 过滤参数一致（真栈 E2E 实测）。
- 数据安全：backfill 仅 FORMAL→CORE_FEATURE；误改的 30/33 已还原；__E2E 测试项目待授权清理。

## 6. 结论

✅ 项目类型拓展为 轻量+三正式子类、FORMAL 迁移；立项可原子「创建或关联」对外-交付项目（关联限对外-交付、新建强制对外-交付 + 指定负责人）。
后端 524/524（temurin-8）+ 前端 247/247 + tsc/lint clean；E2E 全链绿（含两处真栈 bug 修复）。评审 C:0 H:2 M:4 L:6 全处置。建议进入 Gate 3。

> 待用户处置：5 个 `__E2E_*` 测试项目（id 9–13）删除被安全分类器拦截，需用户授权或自行在项目页删除。
