# Capability: frontend-scaffold

## MODIFIED Requirements (from change 2026-06-05-v1-id-migration)

### Requirement: 前端 TS 接口 id 字段统一为 number

前端 `api/*.ts` 中所有实体类型 SHALL 用 `number` 表达 id 类字段；TreeSelect 与 usePaginated 等通用组件类型跟随。

#### Scenario: TypeScript 类型契约

- **GIVEN** `frontend/` 工作目录已 `npm ci`
- **WHEN** 执行 `npm run build`
- **THEN** `tsc -b` 阶段 SHALL 退出码 0
- **AND** vite build 阶段 SHALL 退出码 0
- **AND** `frontend/src/api/{organization,user,userOrganization}.ts` 中所有 `id` / `parentId` / `userId` / `organizationId` 字段类型 SHALL 为 `number` 或 `number | null`
- **AND** `frontend/src/components/ui/TreeSelect.tsx` 中 `TreeNode.id` / `parentId` 字段类型 SHALL 为 `number` / `number | null`
