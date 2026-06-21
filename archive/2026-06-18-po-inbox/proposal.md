# v0.0.42-po-inbox — PO 需求收件箱（路线图 PO 角色）

> Baseline: tag `v0.0.41-admin-compliance` / commit 55bf6a8。来自 [C-角色链路审计路线图](../../C-角色链路审计与建设路线图.md) §4 PO 角色（M/高，最弱角色）。

## Why

PO（产品负责人）是覆盖最差的角色：六段链路只通「我的 Story」一段。业务方提交的诉求堆着、哪些还没转成需求 PO 看不到；
PO 自己负责的需求进展也没有落点。本版给 PO 一个**「待我处理」的收件箱落地页** + 自助 read-model，延续「建一次自助
read-model、角色落地页消费」模式（继 portfolio / pending-reviews / profile）。

## What Changes

**后端（NEW capability `me-inbox`，all-users）**

- `GET /api/me/inbox`（token-gated，非 admin）→ `InboxResponse`：
  - **待处理诉求** `unconvertedDemands`：尚无 demand-requirement 关联、且状态非终态（非 DONE/CLOSED）的 Demand
    `[{id, title, priority, status, createTime}]`（PO 的转化三分诊队列），按优先级高→低排序。
  - **我的需求** `myRequirements`：`ownerUserId = 当前用户` 的 Requirement `[{id, code, title, status, priority,
    expectedDate, projectId, projectName}]`（projectName 批量富化），按优先级高→低排序。
- repo 加：`RequirementRepository.findByOwnerUserId`（派生查询，尊重 `@Where del_flag=0`）。「未转化诉求」用
  `DemandRequirementLinkRepository.findAll()` 取已关联 demandId 集合 + `DemandRepository.findAll()` 过滤（应用量级，无需新查询）。

**已定子决策**：收件箱 = 待处理诉求（全局未转化三分队列，PO 分诊全部进件）+ 我的需求（owner=我）；独立 `/inbox` 页；精简字段。

**前端（capability frontend-scaffold MOD）**

- `api/inbox.ts`：`getInbox()`。
- `InboxPage`「需求收件箱」`/inbox`：StatTiles（待处理诉求数 / 我的需求数）+ 待处理诉求 DashboardCard（标题 + 优先级
  StatusChip + 状态 + 链接到 `/pm/demands`）+ 我的需求 DashboardCard（code+标题 + 状态 chip + 优先级 + 期望日期 + 链接
  到 `/pm/requirements`）+ 各自 EmptyState。
- 工作台组加「需求收件箱」入口（icon `inbox`）+ `/inbox` 路由，**不入 isAdminPath**（navGuardConsistency 自动钉）。

## Capabilities

### Modified Capabilities

- `frontend-scaffold`：新增 InboxPage「需求收件箱」+ 工作台导航 + /inbox 路由（all-users）。

### New Capabilities

- `me-inbox`：`GET /api/me/inbox` 自助 PO 收件箱 read-model（待处理诉求 + 我的需求）。

## Impact

**代码层面**：后端 ~5 文件（InboxResponse + 嵌套 InboxDemand/InboxRequirement + MeInboxService + MeInboxController；
RequirementRepository +1 派生查询）。新测试 1 类。前端 ~5 文件（api/inbox.ts / InboxPage+index / AppRoutes / AppLayout）。新测试 1-2。

**配置层面**：无。

**基础设施**：无新服务、无新表、无新列、0 AI、0 新依赖。新增 1 个 all-users API。

## Success Criteria

- [ ] `GET /api/me/inbox` 返回 待处理诉求（无关联且非终态）+ 我的需求（owner=我），各按优先级排序；无 token→401。
- [ ] 已关联的诉求、已 DONE/CLOSED 的诉求 SHALL NOT 出现在待处理诉求。
- [ ] 我的需求仅含 ownerUserId=当前用户 的；projectName 富化（有项目时）。
- [ ] token sub 无对应 User → 降级返回 **两区皆空**（unconvertedDemands=[]、myRequirements=[]），与其它 me-service 一致，不报错。
- [ ] InboxPage 渲染两区 + 空态；/inbox all-users 可达且 `isAdminPath('/inbox')===false`。
- [ ] backend 全绿（453 baseline + 新增）+ frontend 全绿（171 baseline + 新增）+ E2E + 存量数据零改。
