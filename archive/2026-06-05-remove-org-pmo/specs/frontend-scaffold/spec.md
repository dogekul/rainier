# Capability: frontend-scaffold

## MODIFIED Requirements (from change 2026-06-05-remove-org-pmo)

### Requirement: 组织编辑抽屉与列表不再渲染 PMO 控件

前端 `OrganizationsPage.tsx` 列表 SHALL 不渲染 "PMO" 列；`EditDrawer.tsx` 抽屉表单 SHALL 不渲染 PMO 复选框 + label `PMO 团队`；`api/organization.ts` 中 `Organization` / `OrganizationCreate` / `OrganizationUpdate` 类型 SHALL 不含 `isPmo` 字段。

#### Scenario: EditDrawer 渲染时无 PMO 复选框

- **GIVEN** 测试 mount `<OrganizationEditDrawer open={true} editing={null} onClose={...} onSubmit={...} />`
- **WHEN** 渲染完成且初次 useEffect 已跑（mock `getOrganizationTree` 返回 `[]`）
- **THEN** `screen.queryByLabelText('PMO 团队')` SHALL 为 `null`
- **AND** `screen.queryByText('PMO 团队')` SHALL 为 `null`
- **AND** 抽屉中可见的 label SHALL 仅包含：父节点 / 类型 / 编码 / 名称 / 描述 / 启用

#### Scenario: OrganizationsPage 列表表头无 PMO 列

- **GIVEN** 测试 mount `<OrganizationsPage />`，mock `listOrganizations` 返回 1 条数据
- **WHEN** 渲染完成
- **THEN** `screen.queryAllByRole('columnheader')` 文本数组 SHALL 不含 `PMO`
- **AND** 表头 SHALL 仅含：编码 / 名称 / 类型 / 全路径 / 操作

#### Scenario: TypeScript 类型契约 — Organization 类型无 isPmo

- **GIVEN** `frontend/` 工作目录已 `npm ci`
- **WHEN** 执行 `npm run build`
- **THEN** `tsc -b` 阶段 SHALL 退出码 0
- **AND** `frontend/src/api/organization.ts` 中 `interface Organization` SHALL 不含 `isPmo` 字段
- **AND** `interface OrganizationCreate` 与 `interface OrganizationUpdate` SHALL 不含 `isPmo` 字段
- **AND** `grep -n 'isPmo' frontend/src/**/*.ts frontend/src/**/*.tsx` SHALL 返回 0 行
