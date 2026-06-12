# v0.0.18-workbench 测试方案

> 版本：v0.0.18-workbench ｜ 2026-06-12 ｜ Spec: auth-placeholder(MOD) + entity-story(MOD) + frontend-scaffold(MOD)

## 一、策略

- 后端集成(MockMvc + H2):me 上下文(含降级 + org 级角色) + story owner 过滤。
- 前端组件(vitest+RTL):WorkbenchPage 渲染 + 携 id 查询 + 状态快改。
- E2E(docker):me 真返回上下文、story owner 过滤、存量不变。
- standing:纯扩展(me 加字段 / story 加 param / 前端换首页);既有 auth/story 测试保持绿。

## 二、详细测试案例

### 功能 1：me 当前用户上下文（auth-placeholder）

| ID | 对应 Scenario | 优先级 | 预置/输入 → 预期 |
|----|---------------|--------|------------------|
| TC-ME-001 | 已知用户返回 id/name/roles/projects | P0 | seed user alice + user-role(role PMO, project P) → GET /me → 200; id/name; roles[0].roleCode="PMO"; projects 含 {id:P} |
| TC-ME-002 | 组织级角色 projectId null | P0 | alice 有 user-role projectId=null → roles 含该项 projectId=null; projects 不含 null 项目 |
| TC-ME-003 | username 无对应 User 降级 | P0 | token sub="system" 无 User → 200; id=null; roles=[]; projects=[] |
| TC-ME-004 | 无 token 401（保留） | P0 | GET /me 无 token → 401 |

### 功能 2：story owner 过滤（entity-story）

| ID | 对应 Scenario | 优先级 | 预置/输入 → 预期 |
|----|---------------|--------|------------------|
| TC-STORY-OWN-001 | 按 ownerUserId 过滤 | P0 | 2 story owner=1 + 1 story owner=2 → GET /stories?ownerUserId=1 → total 2; content 全 owner=1 |

### 功能 3：WorkbenchPage（frontend-scaffold）

| ID | 对应 Scenario | 优先级 | 预置/输入 → 预期 |
|----|---------------|--------|------------------|
| TC-FES-WB-001 | 渲染问候+角色+三块 | P0 | mock me/listTasks/listStories → / 渲染 → 含 "Alice" + 角色 chip "PMO" + 三块数据 |
| TC-FES-WB-002 | 携当前用户 id 查询 | P0 | me.id=5 → listTasks 调用含 assigneeUserId:5; listStories 含 ownerUserId:5 |
| TC-FES-WB-003 | 状态快改 updateTask | P0 | 任务 id=11 status 下拉选 IN_PROGRESS → updateTask(11, {…status:"IN_PROGRESS"}) |

### 功能 3.5：Sider 导航壳增强 + 工作台可跳转（Gate 3 反馈，frontend-scaffold）

| ID | 对应 Scenario | 优先级 | 预期 |
|----|---------------|--------|------|
| TC-FES-WB-101 | 工作台菜单组居首 + 我的工作台→/ | P0 | Sider 首组「工作台」；`appshell-nav-/` href=/；我的工作台 在 组织节点 之前 |
| TC-FES-WB-102 | 品牌链接 | P0 | `appshell-brand` href=/ |
| TC-FES-WB-103 | 组折叠/展开 | P0 | 点「系统」组标题 → 审计日志 移除；再点 → 恢复 |
| TC-FES-WB-104 | 收起整个 Sider | P0 | 点 `appshell-sider-toggle` → sider 移除；再点 → 恢复 |
| TC-FES-WB-105 | 工作台条目可跳转 | P0 | 我的项目条目是链接（已含于 WB-001 渲染；href /pm/projects 等） |

### 功能 4：E2E

| ID | 对应 | 优先级 | 预期 |
|----|------|--------|------|
| TC-E2E-WB-001 | me + story owner + standing | P0 | docker 重建;GET /me 返回 {id,username,name,roles,projects};建 story owner=U → ?ownerUserId=U 命中;19 表不变;存量 3 项目未改 |

## 三、回归风险矩阵

| 区域 | 改动 | 回归保护 | 等级 |
|------|------|---------|------|
| me() 扩字段 | 加 id/name/roles/projects | 既有 auth me 测试(username 仍在) + 新 TC-ME | 🟡中(改既有端点返回) |
| story list | 加 ownerUserId 可选 param | 既有 StoryController 测试 + 新 TC | 🟢低 |
| 首页替换 | Home→WorkbenchPage | 既有 App/AppRoutes 测试 | 🟡中(换路由组件) |
| AuthUser 扩展 | 加可选字段 | 既有 auth store 测试 | 🟢低 |

## 四、建议补充顺序

1. P0 全部:TC-ME-001..004 + TC-STORY-OWN-001 + TC-FES-WB-001..003 + TC-E2E-WB-001
