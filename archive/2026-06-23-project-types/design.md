# Design: v0.0.48 — 项目类型拓展 + 立项创建/关联对外-交付

基线 tag v0.0.47-board-redesign / commit 5c20dab。

## Context

- `ProjectType`（`domain/ProjectType.java`）= 字符串常量 + `ALL` Set，刻意 VARCHAR 存储以便加值免 DB 迁移；当前 {CASUAL, FORMAL}。无后端标签 map（标签仅前端 `ProjectsPage.tsx`）。
- `ProjectService.create`（`:65-93`）校验 owner 存在、code 唯一、`ProjectType.ALL.contains(type)`、默认 CASUAL；`@Transactional`，返回有 `getId()` 的 ProjectDetail。
- `OpportunityService.initiate`（`:259-278`）：WON 校验 → decision 校验 → `projectId` existsById → PASS 设 projectId。`OpportunityInitiateRequest` = `{@NotNull projectId, decision, note}`。
- `ProjectTypeBackfill`：native `UPDATE ... = 'CASUAL' WHERE project_type IS NULL`，幂等。
- 前端 DeliveryFlow 立项抽屉 = 纯 `<select>` 选已有项目（`listProjects({size:100})`）→ `initiateOpportunity(id, projectId, 'PASS')`；无项目时死路文案。
- 客户「选或建」范式靠后端 create-on-write；项目无此后端路径 → 本版改为后端原子扩展 initiate。

## Decisions

### D1 类型常量拓展（entity-project）
- `ProjectType`：保留 `CASUAL`；新增 `CORE_FEATURE`/`CORE_TECH`/`EXTERNAL_DELIVERY`；`ALL` = 这 4 个。
- 保留 `LEGACY_FORMAL = "FORMAL"` 常量（**不入 ALL**，仅供 backfill 迁移引用 + 文档化退役）。默认值仍 CASUAL（create service 与 ProjectDetail 读路径兜底不变）。
- 标签仍仅前端（`api/project.ts` 共享 `PROJECT_TYPE_LABELS`）。

### D2 FORMAL 迁移（backfill）
- `ProjectTypeBackfill` 追加幂等 native：`UPDATE rainier_project SET project_type='CORE_FEATURE' WHERE project_type='FORMAL'`，保留既有 NULL→CASUAL。用户确认的"正式→主业-功能建设"语义迁移；仅改 project_type 值、不动其它字段。

### D3 initiate 原子「创建或关联」（opportunity）
- `OpportunityInitiateRequest`：`projectId` 去 `@NotNull`（可空）；新增可空 `projectCode`/`projectName`/`projectOwnerUserId`（内联新建用）。保留 decision/note。
- `OpportunityService.initiate` 重构签名为 `initiate(Long id, OpportunityInitiateRequest req, String decidedBy)`；注入 `ProjectService`。流程：
  - WON 校验、decision 校验不变。
  - **PASS**：解析交付项目——
    - 若 `projectId != null`：existsById（404/400）+ **校验其 projectType == EXTERNAL_DELIVERY**（否则 400，贴合"关联对外-交付项目"），link。
    - 否则（内联新建）：要求 `projectName`/`projectCode` 非空（缺→400）；`ownerUserId = projectOwnerUserId ?? opp.pmUserId`（仍为空→交由 ProjectService 校验报 400）；构造 `ProjectCreateRequest{code,name,ownerUserId,projectType=EXTERNAL_DELIVERY}` → `projectService.create` → 取 id link。
    - 二者皆空 → 400「需提供 projectId 或新建项目信息」。
  - **REJECT**：仅记录 decidedBy，不要求项目、不创建、不 link（放宽既有"REJECT 也强校验 projectId"）。
- 两边 create 均 `@Transactional`，initiate 自身 `@Transactional` → 项目插入与商机 link 原子提交。
- **备选**（纯前端两步 createProject→initiate）已排除：非原子，失败留孤立项目（Gate-1 决策）。

### D4 前端
- `api/project.ts`：`ProjectType` 联合改 4 值；导出 `PROJECT_TYPE_OPTIONS`/`PROJECT_TYPE_LABELS`（轻量/主业-功能建设/主业-技术改造/对外-交付）。`ProjectsPage` 改用共享常量。
- `api/opportunity.ts`：`initiateOpportunity(id, req)` —— req `{projectId?|(projectCode,projectName,projectOwnerUserId?), decision, note?}`，POST 同端点扩展体。
- `DeliveryFlow.tsx` 立项抽屉：模式切换「关联已有 / 新建」。关联：`listProjects({size:100, projectType:'EXTERNAL_DELIVERY'})` 下拉（空则提示可切到新建，不再死路）。新建：code+name 输入（owner 默认商机 pmUserId，可不填由后端兜底），类型固定 对外-交付（展示只读）。提交按模式发 `initiateOpportunity`。

## Architecture / 数据流

立项 PASS → initiate(req)：req 带 projectId（校验 EXTERNAL_DELIVERY）→ link；或带新建字段 → ProjectService.create(type=EXTERNAL_DELIVERY) → link。同事务。前端 DeliveryFlow 仅根据「关联/新建」模式组装 req。

## Risks / Trade-offs

| 风险 | 缓解 |
|---|---|
| backfill 改既有 FORMAL 项目类型值 | 用户 Gate-1 明确确认"正式分为"迁移；仅改 project_type，幂等，仅 FORMAL 行 |
| initiate 签名重构波及既有测试 | 控制器测试走 HTTP（请求体），仅服务签名内部变；同步更新调用 + 测试 |
| 关联非 EXTERNAL_DELIVERY 项目被拒可能影响既有立项数据 | 仅校验"新关联"操作；既有已 link 的 projectId 不回溯校验 |
| 内联新建 owner 为空（pmUserId 未设）→ create 400 | 前端可填 owner；后端 ProjectService 报清晰 400；DeliveryFlow 优雅显示 |
| FORMAL 退役但仍可能残留 | backfill 启动迁移；ALL 不含 FORMAL 防新建；ProjectDetail 读路径 NULL→CASUAL（FORMAL 残留显示原值，但 backfill 已清） |
