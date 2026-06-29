# Proposal: Milestone 状态机 (C7)

> v0.0.87, 2026-06-25 — 给 Milestone.status 加状态机：PLANNED / IN_PROGRESS / DONE / CANCELLED，
> 禁止 PLANNED → DONE 直接跳过 IN_PROGRESS；保留 DONE→IN_PROGRESS、CANCELLED→PLANNED 的"撤销"路径。

## 背景

v0.0.17 Milestone 只用 3 个状态（PLANNED / REACHED / MISSED），且**自由改**——前端可以
随意把 PLANNED 直接打成 REACHED，没有"开始进行中"的过渡，也没有任何防呆。

## 范围

1. NEW 4 个 canonical 状态常量：`PLANNED`, `IN_PROGRESS`, `DONE`, `CANCELLED`
   - 旧 `REACHED` / `MISSED` 保留为 legacy 输入别名（normalize → DONE / CANCELLED）
2. NEW `MilestoneStatusMachine.validateTransition(from, to)` —— 非法跳转抛 `BadRequestException`
3. 允许的转换（其余皆禁）：
   - `PLANNED → IN_PROGRESS`
   - `PLANNED → CANCELLED`
   - `IN_PROGRESS → DONE`
   - `IN_PROGRESS → CANCELLED`
   - `DONE → IN_PROGRESS`（撤销 done）
   - `CANCELLED → PLANNED`（撤销取消）
   - 同状态 → 同状态 always ok（保存其他字段）
4. `MilestoneService.update` 在落库前调状态机校验
5. NEW `POST /api/milestones/{id}/transition` `{to, reason?}` —— 显式语义端点，
   只改 status（与 actualDate 联动：进入 DONE 时若 actualDate 为空 → 自动填 today）
6. `MilestoneService.create` & `update` 接收 legacy `REACHED`/`MISSED` 时 normalize 后再校验/存储

## 兼容

- 老接口 PUT `/api/milestones/{id}` 仍可改 status——但现在受状态机约束
- DB 中既有 `REACHED`/`MISSED` 行：读时透传；下次 update 时 normalize 写回 canonical
- TC-MILE-005 旧用例（"DONE" → 400）改为发明确无效的 `"XXX"`
- TC-MILE-010 旧用例（PLANNED → REACHED）改走两步过渡，并断言 normalize 结果为 `DONE`

## OutOfScope

- 状态变化通知（A8 PushChannel 未联动）
- 状态变化审计（已被 AuditAspect 自动捕获，无需新代码）
- 前端 UI 显式按钮（按需走旧 PUT 或新 transition 端点，UI 后续打磨）

## commit

`feat(milestone-status-machine): C7 里程碑状态机 (v0.0.87)`
