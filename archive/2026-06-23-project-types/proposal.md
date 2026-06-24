# Proposal: v0.0.48 — 项目类型拓展 + 立项创建/关联对外-交付项目

## Why

现项目类型仅 `CASUAL`(轻量) / `FORMAL`(正式)。用户要求：轻量一如既往；正式细分为 **主业-功能建设 / 主业-技术改造 / 对外-交付**。
并且实施流转「立项」时应能**创建或关联一个「对外-交付」项目**（现状只能从既有项目下拉里选，无法新建，无项目时是死路）。

## What Changes

- 后端 `ProjectType` 拓展：保留 `CASUAL`(轻量)；新增 `CORE_FEATURE`(主业-功能建设) / `CORE_TECH`(主业-技术改造) / `EXTERNAL_DELIVERY`(对外-交付)；`FORMAL` 退役（迁移）。
- 启动 backfill 追加一条：既有 `FORMAL` 项目 → `CORE_FEATURE`（主业-功能建设，默认正式桶）。保留既有 NULL→CASUAL。
- 后端 `initiate` 原子扩展：请求体支持「已有 `projectId`」**或**「内联新建项目字段(code/name/owner)」二选一；新建项目强制 `EXTERNAL_DELIVERY`、复用 `ProjectService.create`、与商机关联同事务提交。关联已有项目时校验其类型为 `EXTERNAL_DELIVERY`。
- 前端 ProjectsPage 类型下拉 → 4 项（轻量/主业-功能建设/主业-技术改造/对外-交付）；`PROJECT_TYPE_OPTIONS/LABELS` 上提到 `api/project.ts` 共享。
- 前端 DeliveryFlow 立项抽屉：从「纯选已有」改为「关联已有(仅对外-交付) 或 新建对外-交付项目」。

## Capabilities

- Modified: `entity-project`（类型拓展 + backfill）、`opportunity`（initiate 创建或关联）、`frontend-scaffold`（ProjectsPage 类型 + DeliveryFlow 立项）。
- New: 无。

## Impact

- 代码：后端 `ProjectType` / `ProjectTypeBackfill` / `OpportunityInitiateRequest` / `OpportunityService.initiate`(+注入 `ProjectService`) / `OpportunityController` + 测试；前端 `api/project.ts` / `ProjectsPage.tsx` / `api/opportunity.ts` / `pages/Crm/DeliveryFlow.tsx` + 测试。
- 配置/基建：无新表/列/依赖；复用 `POST /{id}/initiate`（请求体扩展）、`ProjectService.create`、`GET /api/projects?projectType=`。
- 数据：backfill 将既有 `FORMAL` 项目类型值改为 `CORE_FEATURE`（用户确认的"正式分为"语义迁移）；NULL→CASUAL 保留；其它项目字段不动。

## Success Criteria

- [ ] `ProjectType.ALL` = {CASUAL, CORE_FEATURE, CORE_TECH, EXTERNAL_DELIVERY}；create/update 校验接受这 4 类、拒绝未知；默认仍 CASUAL。
- [ ] 启动后既有 `FORMAL` 项目类型变为 `CORE_FEATURE`；NULL 仍→CASUAL；幂等。
- [ ] ProjectsPage 类型下拉显示 4 个中文选项；列表/过滤标签正确。
- [ ] `initiate` 传 `projectId`（须为 EXTERNAL_DELIVERY）→ 关联；传新建字段 → 建一个 EXTERNAL_DELIVERY 项目并关联（同事务）；二者缺一/兼有 → 400。
- [ ] DeliveryFlow 立项可「关联已有对外-交付项目」或「新建对外-交付项目」，无项目不再是死路。
- [ ] 后端 temurin-8 全绿 + 前端全绿 + E2E 绿；除 FORMAL→CORE_FEATURE 迁移外不改既有业务数据。
