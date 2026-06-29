# Capability: milestone-status-machine

> NEW (v0.0.87, 2026-06-25) — 给 Milestone 状态字段加状态机：PLANNED → IN_PROGRESS → DONE，
> 任意状态可 → CANCELLED；保留 DONE → IN_PROGRESS / CANCELLED → PLANNED 的撤销路径。
> 同时 normalize legacy REACHED/MISSED → DONE/CANCELLED。

## ADDED Requirements

### Requirement: 4 个 canonical 状态常量 + 2 个 legacy 别名

`MilestoneStatus` SHALL 暴露：

- canonical: `PLANNED`, `IN_PROGRESS`, `DONE`, `CANCELLED`
- legacy (仅作为输入接收，会被 normalize): `REACHED`(→DONE), `MISSED`(→CANCELLED)
- `ALL` 包含 4 个 canonical
- `LEGACY_ALL` 包含 2 个 legacy
- `normalize(String)` 把 legacy 映射到 canonical；canonical 透传；其它 → 返回原值（由调用方决定是否抛错）

#### Scenario: normalize legacy 输入

- **GIVEN** `MilestoneStatus.normalize("REACHED")`
- **THEN** 返回 `"DONE"`
- **AND** `normalize("MISSED")` 返回 `"CANCELLED"`
- **AND** `normalize("PLANNED")` 返回 `"PLANNED"`
- **AND** `normalize("XXX")` 返回 `"XXX"`（让调用方校验）

### Requirement: MilestoneStatusMachine.validateTransition 合法表

下列转换合法（其它非法）：

| from | to |
|---|---|
| PLANNED | IN_PROGRESS, CANCELLED |
| IN_PROGRESS | DONE, CANCELLED |
| DONE | IN_PROGRESS |
| CANCELLED | PLANNED |
| 任意 X | X (no-op) |

非法跳转 SHALL 抛 `BadRequestException`，message 含 `"illegal transition"`、`from`、`to`。

#### Scenario: 合法转换通过

- **WHEN** `validateTransition("PLANNED", "IN_PROGRESS")`
- **THEN** 无异常

#### Scenario: PLANNED → DONE 直跳被禁

- **WHEN** `validateTransition("PLANNED", "DONE")`
- **THEN** 抛 `BadRequestException("illegal transition: PLANNED -> DONE")`

#### Scenario: 同状态 ok

- **WHEN** `validateTransition("DONE", "DONE")`
- **THEN** 无异常

#### Scenario: DONE → CANCELLED 禁

- **WHEN** `validateTransition("DONE", "CANCELLED")`
- **THEN** 抛 `BadRequestException`

### Requirement: MilestoneService.update 落库前调状态机校验

`MilestoneService.update(id, req)` SHALL 先 normalize `req.status`，再调
`MilestoneStatusMachine.validateTransition(currentStatus, normalizedStatus)`，
非法 → 400。`create` 也 normalize 输入。

#### Scenario: PUT PLANNED → DONE 直跳 → 400

- **GIVEN** 一个 status=PLANNED 的 Milestone
- **WHEN** PUT 把 status 改成 `"DONE"`
- **THEN** 400, message 含 `"illegal transition"`

#### Scenario: PUT PLANNED → REACHED normalize 为 DONE 但仍非法 → 400

- **GIVEN** 一个 status=PLANNED 的 Milestone
- **WHEN** PUT 把 status 改成 `"REACHED"`
- **THEN** 400（normalize 后是 DONE，PLANNED→DONE 非法）

#### Scenario: PUT PLANNED → IN_PROGRESS → DONE 两步合法

- **GIVEN** PLANNED milestone id=X
- **WHEN** PUT status=IN_PROGRESS，再 PUT status=DONE
- **THEN** 两次都 200，最终 `status=DONE`

### Requirement: NEW POST /api/milestones/{id}/transition

显式 transition 端点：body `{to: STATUS, reason?: String}`。
- normalize `to` → 调状态机校验 → 写库
- 进入 `DONE` 时若 `actualDate` 为空 SHALL 自动填 today
- `reason` 字段本版仅记录到 AuditAspect 自动捕获的 payload；不持久化到 Milestone 实体

#### Scenario: POST transition IN_PROGRESS → DONE 自动填 actualDate

- **GIVEN** milestone status=IN_PROGRESS, actualDate=null
- **WHEN** POST `/api/milestones/{id}/transition` body `{"to":"DONE"}`
- **THEN** 200, response `status="DONE"`, `actualDate=today`

#### Scenario: POST transition 非法 → 400

- **GIVEN** PLANNED milestone
- **WHEN** POST `{"to":"DONE"}`
- **THEN** 400

### Requirement: PortfolioService 把 DONE 视为已达成

`PortfolioService` 的"overdue milestone"判定 SHALL 把 `DONE` 和 legacy `REACHED` 都当作已达成
（不计 overdue）。

## OutOfScope

- 通知（A8）
- 前端 transition 按钮 UI
