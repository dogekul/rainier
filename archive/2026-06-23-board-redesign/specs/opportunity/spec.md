# Capability: opportunity — v0.0.47 board-redesign delta (MODIFIED)

> 合并入 canonical `specs/opportunity/spec.md`（Phase 6）。新增「进入阶段时间」`stageEnteredAt`（停留时长预警的后端基础）。见 [[frontend-scaffold]]。

## MODIFIED / ADDED Requirements (from change 2026-06-23-board-redesign / v0.0.47)

### Requirement: 商机记录「进入当前阶段时间」stageEnteredAt

`Opportunity` SHALL 持有 `stageEnteredAt`（进入当前阶段的时刻，nullable）。`create()` SHALL 将其设为创建时刻；
`advance()` 在**实际发生阶段变更**（PASS / 非关口推进）时 SHALL 刷新为当下；关口 REJECT（阶段不变）SHALL NOT 刷新。
`OpportunityDetail` SHALL 回传 `stageEnteredAt`。既有 null 行 SHALL 由启动 backfill 以 `update_time` 兜底（仅填空值、不改既有业务字段）。

#### Scenario: 创建即记录进入阶段时间

- **GIVEN** 一次合法的 `POST /api/opportunities`
- **WHEN** 创建成功
- **THEN** `OpportunityDetail.stageEnteredAt` SHALL 非空

#### Scenario: 推进刷新进入阶段时间

- **GIVEN** 商机在某非终态阶段、记录了 `stageEnteredAt = t0`
- **WHEN** `POST /{id}/advance` 触发阶段前进（PASS 或非关口）
- **THEN** `stageEnteredAt` SHALL 刷新为不早于 t0 的新时刻

#### Scenario: 关口否决不刷新进入阶段时间

- **GIVEN** 商机在售前关口（如 BIDDING）、记录了 `stageEnteredAt = t0`
- **WHEN** `POST /{id}/advance` body `{decision:"REJECT"}`（阶段不变、丢单）
- **THEN** `stageEnteredAt` SHALL 仍为 t0（不刷新）

#### Scenario: 既有空值经 backfill 兜底

- **GIVEN** 升级前已存在、`stage_entered_at` 为 NULL 的商机行
- **WHEN** 应用启动执行 backfill
- **THEN** 该行 `stage_entered_at` SHALL 被填为其 `update_time`
- **AND** 该行其它业务字段（客户/阶段/金额/状态）SHALL 不变
