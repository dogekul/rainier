# Capability: frontend-scaffold — v0.0.64 增量

## MODIFIED Requirements

### Requirement: ProjectEditDrawer +负责团队 +项目PMO 字段联动

`ProjectEditDrawer` SHALL 在 owner 字段下加「负责团队」TreeSelect 和「项目 PMO」`<select>`；team 切换 SHALL 触发 effective-PMOs 重列并重置 pmo 默认。

#### Scenario: 创建项目 选 owner 后团队字段自动填默认

- **GIVEN** owner=lina 的主组织=研发中心 (id=2)
- **WHEN** 用户打开 「新建项目」抽屉 + 选 owner=lina
- **THEN** 「负责团队」TreeSelect SHALL 自动展示 "研发中心"
- **AND** 「项目PMO」`<select>` SHALL 默认选 "黎立（继承自...）"或首条
- **AND** 用户保留默认 + 保存 SHALL 提交 organizationId=2 / pmoUserId=2

#### Scenario: 切换团队 PMO 候选刷新

- **GIVEN** 抽屉打开，team=研发中心，pmo=黎立
- **WHEN** 用户切换 team 到 "招商银行" (id=4) 假设无 PMO
- **THEN** `<select>` SHALL 显示 loading 一瞬
- **AND** 候选刷新为 [] 或新 team 的 effective-PMOs
- **AND** pmo 默认重置（清空或选首条）

### Requirement: ProjectDetailPage Hero / 基本信息 / 成员 Tab

ProjectDetailPage SHALL 在 Hero 加 团队/PMO chips；基本信息 grid SHALL 加 负责团队 / 项目PMO 行；SHALL 新增「成员」Tab（位于 里程碑 与 需求 之间）含 owner + pmo 合成行 + 真实成员行。

#### Scenario: 访问详情页 成员 Tab 渲染

- **GIVEN** 项目 3 owner=lina / pmo=黎立 / project_member: user6=DEV, user7=QA
- **WHEN** 用户访问 /pm/projects/3 + 点 「成员」Tab
- **THEN** SHALL 渲染 4 行
- **AND** 第 1 行 SHALL 含 lina 的 OwnerChip + 「负责人」标签
- **AND** 第 2 行 SHALL 含 黎立 + 「项目PMO」标签
- **AND** 后续行按 joined_at DESC 列 user7/user6 + role 中文标签

#### Scenario: owner / pmo / admin 看到管理按钮

- **GIVEN** 项目 3 owner=lina / 当前用户=lina
- **WHEN** 用户在 「成员」Tab
- **THEN** SHALL 显示 「添加成员」按钮
- **AND** 每条真实 member 行右侧 SHALL 显示 「移除」按钮
- **AND** owner / pmo 合成行右侧 SHALL NOT 显示 「移除」按钮

#### Scenario: 非授权用户看不到管理按钮

- **GIVEN** 项目 3 当前用户=陈敏 (普通用户)
- **WHEN** 用户访问 「成员」Tab
- **THEN** SHALL NOT 显示 「添加成员」按钮
- **AND** SHALL NOT 显示 「移除」按钮
- **AND** SHALL 仍渲染完整成员列表（只读）

### Requirement: OrganizationEditDrawer +PMO 管理段

`OrganizationEditDrawer` SHALL 底部增 PMO 管理段（仅 admin 可改）：own PMO chips + 「添加 PMO」按钮 + 继承的 PMO 显示为禁用 chip 含「继承自 XX」注脚。

#### Scenario: admin 加 PMO

- **GIVEN** 当前用户=admin / 编辑组织 2 (研发中心)
- **AND** 当前 own PMO = []
- **WHEN** 用户点 「添加 PMO」+ 选 黎立 + 确认
- **THEN** SHALL 调 `POST /api/organizations/2/pmos {userId:2}`
- **AND** PMO 段 SHALL 出现一个 chip "黎立"

#### Scenario: 非 admin 只读

- **GIVEN** 当前用户=lina (非 admin) / 编辑组织 2
- **WHEN** 用户打开 EditDrawer
- **THEN** PMO 段 SHALL 显示当前 PMO 列表
- **AND** SHALL NOT 显示 「添加 PMO」按钮
- **AND** 每个 chip SHALL NOT 含删除 X

### Requirement: ProjectsPage +团队列

`ProjectsPage` 表格 SHALL 在「负责人」列后加「团队」列，显示 organizationName 或 "—"。

#### Scenario: 列表渲染团队列

- **GIVEN** 项目 3 organizationName="研发中心"
- **WHEN** 用户访问 /pm/projects
- **THEN** 项目 3 行的「团队」cell SHALL 显示 "研发中心"
