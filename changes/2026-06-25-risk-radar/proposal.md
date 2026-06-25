# A6 风险雷达（规则版，0 AI） (v0.0.70)

## What
- 新增 `com.rainier.risk` 包：RiskRule 接口、RiskFinding/RiskContext 数据载体。
- 三条规则 @Component：OverdueTaskRule（过期未 DONE → WARN）、BlockedStoryRule（Story 状态 BLOCKED → CRIT）、SprintEndingNoDoneRule（Sprint 3 天内结束 + 无 DONE task → WARN）。
- RiskService 注入 List<RiskRule>，按 ScopeService 解析项目范围后聚合所有 finding。
- 新增端点 `GET /api/me/risks?scope=mine`（all-users / token-gated）。

## Why
飞轮 A6：风险雷达是「让 AI/Agent 发现并提示」之前最简单可信的版本——纯规则、可解释、零黑盒。先把规则引擎与扫描骨架搭好，后续 A7/A8 把规则替换或叠加为 AI 推断时，service 接口与端点 contract 不必动。

## Scope
- backend: risk 包（RiskRule, RiskFinding, RiskContext, RiskService, 3 个规则, RiskController）。
- 测试: 每个 Rule 单测 @SpringBootTest（H2 seed 真实 Task/Story/Sprint）+ RiskServiceTest 集成。

## OutOfScope
- 真实 ML/AI 异常检测。
- 风险驳回 / 忽略反馈（前端展示后续做）。
- 主动推送（A8 才耦合）。
- 前端 UI（仅暴露 API）。

## Decisions
- RiskFinding.level 用 String 常量 INFO/WARN/CRIT（与现有 status 风格一致），避免引入 enum 类。
- RiskContext 只携带 userId + projectIds + now，规则自行查 Repository——不预先加载实体，规则之间互不耦合。
- "无 DONE task" 的 sprint 在没有任何 task 时也算（保守告警），与有 1+ task 但无 DONE 同等处理。
- 端点路径用 `/api/me/risks`，与现有 `/api/me/inbox` 等家族对齐；scope 参数复用 ScopeService 三档语义。
