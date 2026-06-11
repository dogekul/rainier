# Capability: frontend-scaffold

> MODIFIED in v0.0.14-sprint-feature-link (2026-06-11).
> 新增：Sprint「关联功能」面板（挂载/解绑 feature + 已挂列表）；Feature 列表/详情显示「所在迭代」。
> 其余 v0.0.8–v0.0.13 Requirement 不变。

## ADDED Requirements

### Requirement: Sprint「关联功能」面板

前端 SHALL 在 Sprint 的行级 drilldown 或详情中提供「关联功能」面板：展示该 sprint 已挂的 Feature 列表，提供「挂载功能」（feature 下拉 + 提交）与每行「解绑」。挂载下拉在 sprint.productId 非空时 SHALL 仅显示该产品的 feature。

#### Scenario: 展示已挂功能并支持挂载

- **GIVEN** Sprint S 已挂 Feature F1
- **WHEN** 用户展开 S 的「关联功能」面板
- **THEN** 面板 SHALL 显示 F1
- **AND** SHALL 提供「挂载功能」下拉与提交按钮
- **AND** 提交后 SHALL 调用 `POST /api/sprint-features` 并刷新列表

#### Scenario: 解绑功能

- **GIVEN** Sprint S 已挂 Feature F1
- **WHEN** 用户点击 F1 行的「解绑」
- **THEN** SHALL 调用 `DELETE /api/sprint-features/{id}`
- **AND** 刷新后 F1 SHALL 从面板消失

### Requirement: Feature 列表/详情显示「所在迭代」

前端 SHALL 在 Feature 页对每个 feature 提供查看其所在迭代的能力（调用 `GET /api/features/{id}/sprints`），展示 sprint code/name/status。

#### Scenario: 查看 feature 的所在迭代

- **GIVEN** Feature F 被挂到 Sprint S1
- **WHEN** 用户在 Feature 页查看 F 的「所在迭代」
- **THEN** SHALL 调用 `GET /api/features/{F}/sprints`
- **AND** SHALL 显示 S1 的 code 与 name
