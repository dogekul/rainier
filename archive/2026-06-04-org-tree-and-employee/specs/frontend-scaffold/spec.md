# Capability: frontend-scaffold

## MODIFIED Requirements

### Requirement: 左侧导航 Sider

前端 SHALL 在登录后页面（AppLayout 下）显示左侧 Sider 导航。

#### Scenario: Sider 含组织菜单组

- **GIVEN** 用户已登录访问 `/`
- **WHEN** 页面渲染完成
- **THEN** 左侧 Sider SHALL 渲染
- **AND** Sider SHALL 含菜单组 "组织"
- **AND** 该组展开后 SHALL 含 3 项：组织节点 / 用户 / 用户-组织关系
- **AND** 点击 "组织节点" SHALL 跳转 `/org/organizations`

### Requirement: 通用 CRUD 组件

前端 SHALL 在 `components/ui/` 提供 `Table` / `Pagination` / `Drawer` / `ConfirmDialog` / `TreeSelect` 5 个通用组件。

#### Scenario: Table 渲染 columns + rows

- **GIVEN** 测试 mount `<Table columns={[{key:'name',title:'名称',render:(r)=>r.name}]} dataSource={[{id:'1',name:'A'}]} rowKey="id" />`
- **WHEN** 渲染完成
- **THEN** SHALL 渲染 `<table>` 元素
- **AND** 表头 SHALL 含文本 "名称"
- **AND** 表体 SHALL 渲染 1 行，含文本 "A"

#### Scenario: TreeSelect 父节点选择器

- **GIVEN** 用户在 organizations 新建表单
- **WHEN** TreeSelect 被点击且 `GET /api/organizations/tree` 返回 3 节点（含层级）
- **THEN** SHALL 弹出树形面板
- **AND** 面板 SHALL 渲染 3 个节点，按 parent_id 嵌套
- **AND** 选择一个节点后 SHALL 关闭面板并填入 `parentId`
