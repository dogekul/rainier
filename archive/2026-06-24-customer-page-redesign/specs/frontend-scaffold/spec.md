# Capability: frontend-scaffold — v0.0.51 customer-page-redesign delta (MODIFIED)

> 合并入 canonical `specs/frontend-scaffold/spec.md`（Phase 6）。客户管理页改为卡片网格视觉。

## MODIFIED Requirements (from change 2026-06-24-customer-page-redesign / v0.0.51)

### Requirement: 客户管理页卡片网格视觉

客户页 SHALL 以响应式卡片网格呈现客户：每卡含 首字母头像（按名字取色）+ 客户名 + 行业标签（缺省「未填行业」）+ 联系人 + 备注 + 编辑/删除。
SHALL 在标题旁显示「共 N 家」计数、空态用 EmptyState。搜索/新建/编辑/删除的行为与 testid SHALL 保持不变（纯展示层改版）。

#### Scenario: 卡片网格渲染客户

- **GIVEN** 若干客户
- **WHEN** 客户页渲染
- **THEN** SHALL 出现 `customers-grid`，每个客户一张 `customer-card-{id}`（含名称文本）

#### Scenario: CRUD 入口不回归

- **WHEN** 客户页渲染
- **THEN** `customers-new-btn` SHALL 在；卡片 SHALL 含 `customer-edit-{id}` / `customer-delete-{id}`
- **AND** 新建/编辑抽屉 SHALL 保留 `customer-name`/`customer-save` 等输入与提交
