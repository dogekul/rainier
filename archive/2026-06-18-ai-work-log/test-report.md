# v0.0.43-ai-work-log — 测试报告 (Phase 5 VERIFY)

> Baseline: tag `v0.0.42-po-inbox` / commit d08cefb。路线图 §3/§4 飞轮层启动（AI 工作日志，种子驱动壳）。

## 1. 总体概况

| 维度 | 结果 |
|------|------|
| 后端单元/集成 | **470 / 470** ✅（459 baseline + 11 new；0 fail/error/skip） |
| 前端组件/路由 | **180 / 180** ✅（175 baseline + 5 new；46 files）+ tsc clean + eslint 0 warn |
| 新增后端测试 | AiWorkLogControllerTest **9/9** + AiWorkLogSeedTest **2/2** |
| 新增前端测试 | AiWorkLogsPage **3/3** + AppRoutes /ai/work-logs **1** + navGuardConsistency 自动 +1 |
| E2E（Docker 真 MySQL，种子启动） | 多链路全绿 ✅ |
| 多路评审 (Step 0) | 3 reviewers / 10 findings / **C:0 H:0** / confirmed-real **0** |
| 表数 | 20 → **21**（rainier_ai_work_log；LegacyProductCategoryCleanupTest 已更新） |

## 2. 新增测试

**ai-work-log（AiWorkLogControllerTest，9）**：create→201+PROPOSED+evidence / 缺 evidence→400 / list 按 status 过滤 /
采纳→200+ACCEPTED+decidedBy / 驳回无 reason→400 / 驳回带 reason→200+rejectReason / 重复裁决→409 / 非法 decision→400 /
未知 id→404。**状态机顺序**（400→404→409→reason）全覆盖。

**种子（AiWorkLogSeedTest，2，@TestPropertySource flag=true）**：表空→种入（全 PROPOSED + evidence 非空）/ 幂等（再跑 count 不变）。

**前端（AiWorkLogsPage 3 + AppRoutes）**：渲染提议 + 采纳/驳回按钮 / 采纳→decide(id,ACCEPTED)+refetch+离队 / 空态；
/ai/work-logs 挂载 + literal；navGuardConsistency 自动钉 `isAdminPath('/ai/work-logs')===false`（新 AI all-users 组）。

## 3. E2E（live stack — Docker，真 MySQL，dev profile 种子开）

| # | 验证 | 结果 |
|---|------|------|
| 1 | 种子启动 → `GET /api/ai-work-logs` | total 4，4 条 PROPOSED（ASSIGNMENT/WEEKLY_REPORT/RISK_RADAR/STATUS_SYNC），均带 evidence，倒序 ✅ |
| 2 | 采纳 PROPOSED | 200；status=ACCEPTED；**decidedBy=alice**（token 身份实链捕获）✅ |
| 3 | 重复裁决→409 / 驳回无 reason→400 / 非法→400 / 无 token→401 | 409 / 400 / 400 / 401 ✅ |
| 4 | 表数 + 存量数据 | rainier_tables=**21**、ai_work_logs=4（种子）、users=7 不变 ✅ |

> 仅新增 rainier_ai_work_log 表 + 种子数据；存量业务数据零改（standing 约束）。

## 4. 多路评审（Step 0）+ 11 类失败模式

**3 reviewers**：10 findings，**C:0 H:0**，对抗式 verify 后 **confirmed-real = 0**。核心 confirmations：
- **状态机正确有序**：null/非法 decision→400（DB 读之前）→ 404 未知 → 409 非 PROPOSED（防重复裁决）→ REJECTED 缺 reason→400；ACCEPT 显式清空 rejectReason。
- **evidence 非空两层强制**：实体 @Column(nullable=false) + 请求 @NotBlank。
- **种子安全**：flag-gated（test false 不污染）、幂等（count==0）、@Transactional、无 ddl-auto 时序风险（schema 在 EMF 初始化建好，先于 CommandLineRunner）。
- **Java-8 clean**；decidedBy 鲁棒（token 或 "system"）；表数 20→21 正确；navGuardConsistency 自动钉 all-users。
- **飞轮框架表述如实**：明确是「种子驱动壳，0 真实 AI/推断/外部集成」，未夸大。

**11 类失败模式**：无幻觉；范围聚焦（ai-work-log + frontend-scaffold）；契约 (k) 前后端 DTO 对齐；(d) design D1-D6 与代码吻合；无覆盖真空（含种子 + 状态机）。

## 5. 已知取舍（记录，不阻塞）

- **种子驱动壳，0 真实 AI**：本版只建提议-证据-裁决底座；真实推断/状态自动同步/风险雷达/AI 周报需「外部集成」前置（飞轮后续）。
- **all-users 裁决**：任意已认证用户可采纳/驳回；分级授权（谁能裁决哪个 agent）= 飞轮后续步骤。
- **前端驳回理由用 window.prompt**：v1 壳够用；后续可换内联输入。

## 6. 结论

| 信号 | 状态 |
|------|------|
| 后端 470/470 + 前端 180/180 + tsc/lint | ✅ |
| 新增 11 后端 + 5 前端测试全绿 | ✅ |
| E2E 种子 + 列表 + 裁决状态机（采纳/重复/驳回/非法/无token）| ✅ |
| Docker 真 JDK-8 构建 + 表数 21 | ✅ |
| 多路评审 C:0 H:0 confirmed-real:0 | ✅ |
| 存量业务数据零改 | ✅ |
| **飞轮层底座落地**（AI 提议-证据-裁决） | ✅ |

**部署建议**：可交付（待用户审阅后 push）。后续飞轮：外部集成（GitLab/钉钉…）→ Event 抽取 → 状态自动同步 → 分级授权 → 风险雷达 → AI 周报 → 主动推送。
