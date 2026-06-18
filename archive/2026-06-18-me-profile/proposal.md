# v0.0.40-me-profile — 个人贡献/能力档案（路线图 #9）

> Baseline: tag `v0.0.39-review-queue` / commit 6e7d049。来自 [C-角色链路审计路线图](../../C-角色链路审计与建设路线图.md) #9（M/中，服务 团队成员/团队/小组/领域 4 角色）。

## Why

团队成员钩子缺一半：`GET /api/auth/me` 只知 roles/projects（项目级），**无组织身份、无岗位、无直接上级、无贡献量**。
本版建一层自助 `GET /api/me/profile` read-model + 独立「我的档案」页，聚合既有 UserOrganization/Position/Story/Task
数据，给团队成员一个成长档案落点，并为「团队绩效输入 / 小组带教 / 领域人才梯队」预置底座。延续「建一次自助
read-model、多角色消费」撬动模式（继 scope-substrate / portfolio / pending-reviews）。

## What Changes

**后端（NEW capability `me-profile`）**

- `GET /api/me/profile`（all-users，token-gated，非 admin）→ `ProfileResponse`：
  - 身份：`userId` / `loginName` / `name` / `positionName` / `positionCategory`（岗位富化，同 UserService 范式）。
  - 组织身份 `memberships[]`：`{organizationId, organizationName, organizationType, role(HEAD|MEMBER), isPrimary}`
    （当前在岗 = `leftAt IS NULL`）。
  - 直接上级 `manager`：从 primary org 沿组织树（parentId）上溯，取首个**非本人**的在岗 HEAD →
    `{userId, name, loginName}`；无则 null。
  - 贡献 `ownedStoryCount`（`countByOwnerUserId`）+ `assignedTaskCount`（新增 `countByAssigneeUserId`）。
- 新增 repo 方法：`TaskRepository.countByAssigneeUserId(Long)`、`UserOrganizationRepository.findByUserIdAndLeftAtIsNull(Long)`。
  富化批量（org 一次 `findAllById`、manager 单查），无 N+1。

**已确认子决策**：P1 = 仅本人 `/api/me/profile`（下属视图后续）；P2 = 独立「我的档案」/profile 页（同 /reviews 范式）；
P3 = 直接上级沿组织树上溯首个非本人 HEAD；P4 = 精简 2 贡献计数（Story/Task）。

**前端（capability frontend-scaffold MOD）**

- `api/profile.ts`：`getMyProfile()`。
- `ProfilePage`「我的档案」`/profile`：身份 DashboardCard（姓名/岗位/登录名）+ 贡献 StatTiles（我负责的 Story 数 /
  分配给我的任务数）+ 组织身份列表（org 名 + 类型 + 角色 chip + primary 标记）+ 直接上级 OwnerChip + EmptyState（无组织时）。
- 工作台组加「我的档案」入口（icon `badge`，end:true）+ `AppRoutes` 加 `/profile`，**不入 isAdminPath**（navGuardConsistency 自动钉）。

## Capabilities

### Modified Capabilities

- `frontend-scaffold`：新增 ProfilePage「我的档案」+ 工作台导航入口 + /profile 路由（all-users）。

### New Capabilities

- `me-profile`：`GET /api/me/profile` 自助个人档案 read-model（org 身份 + 岗位 + 直接上级 + 贡献计数）。

## Impact

**代码层面**：
- 后端 ~5 文件：新 `ProfileResponse`（含嵌套 Membership/Manager）+ `MeProfileService` + `MeProfileController`；
  `TaskRepository` + `UserOrganizationRepository` 各加 1 查询方法。新测试 1 类。
- 前端 ~5 文件：`api/profile.ts` / `ProfilePage.tsx` + `index.tsx` / `AppRoutes.tsx` / `AppLayout.tsx`。新测试 1-2。

**配置层面**：无。

**基础设施**：无新服务、无新表、无新列、0 AI、0 新依赖。新增 1 个 all-users API `GET /api/me/profile`。

## Success Criteria

- [ ] `GET /api/me/profile` 返回当前用户 身份 + 岗位 + 在岗组织关系 + 直接上级 + 贡献计数；无 token→401。
- [ ] 直接上级 = primary org 上溯首个非本人在岗 HEAD；无 primary/无上级 → null（不报错）。
- [ ] 贡献计数准确（ownedStoryCount = 我负责的未软删 Story 数；assignedTaskCount = 分配给我的未软删 Task 数）。
- [ ] 富化无 N+1（org 批量 findAllById）。
- [ ] token 的 sub 无对应 User（如 "system"）→ 降级返回（loginName=sub、空 memberships、null manager、计数 0），不报错。
- [ ] ProfilePage 渲染 身份卡 + 贡献磁贴 + 组织列表 + 上级；/profile all-users 可达且 `isAdminPath('/profile')===false`。
- [ ] backend 全绿（435 baseline + 新增）+ frontend 全绿（162 baseline + 新增）+ E2E（真实用户档案）+ 存量数据零改。
