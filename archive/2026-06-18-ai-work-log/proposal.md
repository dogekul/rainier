# v0.0.43-ai-work-log — AI 工作日志（飞轮层启动）

> Baseline: tag `v0.0.42-po-inbox` / commit d08cefb。来自 [C-角色链路审计路线图](../../C-角色链路审计与建设路线图.md)
> §3 飞轮层 + §4 AI Agent（M/高）。**这是飞轮层的第一锤**——种子驱动先建壳，不依赖外部集成。

## Why

AI Agent 角色目前纯 0；飞轮层（AI/事件/集成）整条轨道前置于「外部集成」，但其**结构地基**可以先建：一条
append-style 的「AI 工作日志」——AI 每个动作都是一条带**证据(evidence)**的**提议(PROPOSED)**，人类**可见 / 可采纳 /
可驳回**（PROPOSED→ACCEPTED/REJECTED 状态机）。**驳回数据 = KPI 金矿**（衡量 AI 质量）。本版种子驱动建壳：实体 +
状态机 + 列表/裁决页，为后续「状态自动同步 / 风险雷达 / AI 周报 / 分级授权」提供统一的提议-审阅底座。

## What Changes

**后端（NEW capability `ai-work-log`，新表 rainier_ai_work_log，all-users）**

- `AiWorkLog` 实体（extends BaseEntity）：`agentType` / `action` / `targetType?` / `targetId?` / `summary` /
  `evidence`(**非空** —— AI 提议必须带证据) / `status`(PROPOSED/ACCEPTED/REJECTED，默认 PROPOSED) / `decidedBy?` /
  `decidedAt?` / `rejectReason?`。`AiWorkLogStatus` 常量类（ALL + DECISIONS={ACCEPTED,REJECTED}）。
- `GET /api/ai-work-logs?agentType=&status=&page=&size=`（分页倒序，Specification 过滤）+ `GET /{id}`。
- `POST /api/ai-work-logs`（创建提议；evidence/agentType/action/summary 必填，status=PROPOSED）。
- `POST /api/ai-work-logs/{id}/decision` body `{decision: ACCEPTED|REJECTED, reason?}`：**状态机** —— 仅
  PROPOSED 可裁决（已裁决→409）；REJECTED 必带 reason（否则 400）；记 decidedBy（token 身份，缺则 system）+ decidedAt。
- **种子运行器** `AiWorkLogSeed`（@Order HIGHEST CommandLineRunner，gated `app.demo.ai-work-log-seed.enabled`
  默认 true / test false）：表空时种入若干 PROPOSED 样例（让页面有数据，无需真实 AI），幂等（count==0 守卫）。

**前端（capability frontend-scaffold MOD，all-users）**

- `api/aiWorkLog.ts`：listAiWorkLogs / decideAiWorkLog。
- `AiWorkLogsPage`「AI 工作日志」`/ai/work-logs`：StatTiles（待裁决/已采纳/已驳回）+ 状态过滤 + 列表（agentType +
  action + summary + evidence + 状态 chip；PROPOSED 行带 采纳/驳回 按钮 → decision 后刷新）+ EmptyState。
- 新顶级「AI」导航组（all-users）+「AI 工作日志」+ `/ai/work-logs` 路由，**不入 isAdminPath**。

## Capabilities

### Modified Capabilities

- `frontend-scaffold`：新增 AiWorkLogsPage + 新「AI」导航组 + /ai/work-logs 路由（all-users）。

### New Capabilities

- `ai-work-log`：AI 工作日志（提议-证据-裁决状态机）+ 读 API + 裁决端点 + 种子运行器。飞轮层底座。

## Impact

**代码层面**：后端 ~9 文件（AiWorkLog 实体 + AiWorkLogStatus + repo + 3 DTO（Detail/CreateRequest/DecisionRequest）+
service + controller + seed runner）。新测试 1-2 类。前端 ~5 文件（api + page + index + AppRoutes + AppLayout）。新测试 1-2。
**表数 20 → 21**（更新 LegacyProductCategoryCleanupTest 断言）。

**配置层面**：`application.yml` 加 `app.demo.ai-work-log-seed.enabled: true`；`application-test.yml` 加 `false`。

**基础设施**：**+1 新表** rainier_ai_work_log（ddl-auto 自动建）、0 AI（种子驱动壳，无真实推断）、0 新依赖。新增 4 个 all-users API。

## Success Criteria

- [ ] `AiWorkLog` 持久化；create 强制 evidence/agentType/action/summary 非空（缺→400）。
- [ ] `GET /api/ai-work-logs` 分页倒序，支持 agentType/status 过滤。
- [ ] `POST /{id}/decision`：PROPOSED→ACCEPTED/REJECTED；非 PROPOSED→409；非法 decision→400；REJECTED 无 reason→400；记 decidedBy/decidedAt。
- [ ] 种子运行器表空时种入样例、幂等、test profile 不跑（不污染测试）。
- [ ] 表数 21（LegacyProductCategoryCleanupTest 更新）。
- [ ] AiWorkLogsPage 渲染列表 + 采纳/驳回交互 + 空态；/ai/work-logs all-users 可达且 `isAdminPath('/ai/work-logs')===false`。
- [ ] backend 全绿（459 baseline + 新增）+ frontend 全绿（175 baseline + 新增）+ E2E（种子 + 列表 + 裁决链）+ 存量数据零改。
