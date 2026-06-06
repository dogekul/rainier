# v0.0.6-demand-requirement 任务清单

## 1. 后端基础（P0）

### 1.1 常量类（M01）

- [x] 1.1.1 `com.rainier.demand.domain.DemandStatus` — `PENDING / IN_REVIEW / CONVERTED / DONE / CLOSED` 常量集合 + `Set<String> ALL`
- [x] 1.1.2 `com.rainier.demand.domain.Priority` — `URGENT / HIGH / MEDIUM / LOW`
- [x] 1.1.3 `com.rainier.demand.domain.Source` — `WEB / WECHAT / EMAIL / DINGTALK / OTHER`
- [x] 1.1.4 `com.rainier.requirement.domain.RequirementStatus` — `DRAFT / IN_REVIEW / APPROVED / IN_DEV / DELIVERED / DEPRECATED`
- [x] 1.1.5 `com.rainier.requirement.domain.Complexity` — `XS / S / M / L / XL`
- [x] 1.1.6 `com.rainier.demandrequirement.domain.LinkType` — `DERIVED / RELATED`
- [x] 1.1.7 mvn compile 通过

## 2. entity-demand Capability（P0）

### 2.1 Demand 链路 + 测试（M02）— 依赖 #1

- [x] 2.1.1 `Demand` entity：继承 BaseEntity，字段 title / description / submitter_user_id (BIGINT) / status / priority / source / ai_classification / ai_duplicate_hint / close_reason；@SQLDelete + @Where("del_flag = 0")
- [x] 2.1.2 `DemandRepository` extends JpaRepository<Demand, Long>, JpaSpecificationExecutor<Demand>
- [x] 2.1.3 DTO：`DemandCreateRequest` / `DemandUpdateRequest` / `DemandDetail`（注意：Create/Update 不含 aiClassification/aiDuplicateHint）
- [x] 2.1.4 `DemandService`：create（校验 user FK + status/priority/source 集合 + 默认值）/ findById / list (filter status/priority + search) / update / delete (FK 保护)
- [x] 2.1.5 `DemandController` 5 endpoint：POST / GET-id / GET-list / PUT / DELETE
- [x] 2.1.6 测试 `DemandControllerCreateTest`：TC-DMD-001/002/003/004 (4 case)
- [x] 2.1.7 测试 `DemandControllerQueryTest`：TC-DMD-005/006/007/008/009 (5 case)
- [x] 2.1.8 测试 `DemandControllerDeleteTest`：TC-DMD-010/011 (2 case)
- [x] 2.1.9 mvn test 全绿（+ 11 cases）

## 3. entity-requirement Capability（P0）

### 3.1 Requirement 链路 + 测试（M03）— 依赖 #1

- [x] 3.1.1 `Requirement` entity：继承 BaseEntity，字段 code (UNIQUE) / title / description / owner_user_id (BIGINT) / status / priority / complexity / project_id (BIGINT, nullable) / close_reason；@SQLDelete + @Where
- [x] 3.1.2 `RequirementRepository` extends JpaRepository<Requirement, Long>, JpaSpecificationExecutor<Requirement>
- [x] 3.1.3 DTO：`RequirementCreateRequest`（含可选 `sourceDemandIds: List<Long>`） / `RequirementUpdateRequest`（不含 ownerUserId 与 sourceDemandIds） / `RequirementDetail`
- [x] 3.1.4 `RequirementService`：create（校验 user FK + code 唯一 + 默认值；M03 阶段先不实现 sourceDemandIds 逻辑，M05 补） / findById / list（含 projectId 过滤） / update（不允许改 ownerUserId） / delete（FK 保护）
- [x] 3.1.5 `RequirementController` 5 endpoint
- [x] 3.1.6 测试 `RequirementControllerCreateTest`：TC-REQ-001/002/003 (3 case)
- [x] 3.1.7 测试 `RequirementControllerQueryTest`：TC-REQ-004/005/006/007 (4 case)
- [x] 3.1.8 测试 `RequirementControllerDeleteTest`：TC-REQ-008/009 (2 case)
- [x] 3.1.9 mvn test 全绿（+ 9 cases）

## 4. entity-demand-requirement Capability（P0）

### 4.1 Link 链路 + 测试（M04）— 依赖 #2, #3

- [x] 4.1.1 `DemandRequirementLink` entity：继承 BaseEntity 但**不**加 @SQLDelete / @Where；字段 demand_id (BIGINT NN) / requirement_id (BIGINT NN) / link_type；UNIQUE (demand_id, requirement_id)
- [x] 4.1.2 `DemandRequirementLinkRepository`：含 `countByDemandId(Long)`、`countByRequirementId(Long)`、`existsByDemandIdAndRequirementId`、`findByDemandId`、`findByRequirementId`
- [x] 4.1.3 DTO：`DemandRequirementLinkCreateRequest` / `DemandRequirementLinkDetail` / `SourceDemandView`（含 demand 字段 + linkType + linkId） / `DerivedRequirementView`
- [x] 4.1.4 `DemandRequirementLinkService`：create（校验 demand + requirement 都存在 + 唯一） / findById / list (filter demandId/requirementId) / delete（硬删）
- [x] 4.1.5 `DemandRequirementLinkController` 4 endpoint：POST / GET-id / GET-list / DELETE
- [x] 4.1.6 **辅助端点**：`RequirementController.getSourceDemands(Long id)` → `GET /api/requirements/{id}/source-demands`；`DemandController.getDerivedRequirements(Long id)` → `GET /api/demands/{id}/derived-requirements`
- [x] 4.1.7 同时给 demand / requirement service 增加 link 查询能力（注入 DemandRequirementLinkRepository）
- [x] 4.1.8 修改 `DemandService.delete()` 与 `RequirementService.delete()` 实际使用 `linkRepo.countByDemandId/RequirementId` 做 FK 保护（替换 M02/M03 阶段的 stub 实现）
- [x] 4.1.9 测试 `DemandRequirementControllerTest`：TC-DRL-001/002/003/004/005 (5 case)
- [x] 4.1.10 测试 `RequirementSourceDemandsTest`：TC-DRL-006 (1 case)
- [x] 4.1.11 测试 `DemandDerivedRequirementsTest`：TC-DRL-007 (1 case)
- [x] 4.1.12 mvn test 全绿（+ 7 cases）

## 5. workflow-demand-conversion Capability（P0）

### 5.1 转化语义 + 测试（M05）— 依赖 #2, #3, #4

- [x] 5.1.1 修改 `RequirementService.create()`：检测 `req.getSourceDemandIds() != null && !empty` → 在已存在的 `@Transactional` 中循环调用 `linkRepo.save(new DemandRequirementLink(demandId, requirement.id, "DERIVED"))`；任一 demand 不存在 → throw BadRequestException("demand not found: id=N") → @Transactional 触发回滚
- [x] 5.1.2 注意：`@Transactional(rollbackFor = RuntimeException.class)` —— 默认即回滚 RuntimeException，BadRequestException 应是 RuntimeException 子类；确认 GlobalExceptionHandler 体系
- [x] 5.1.3 测试 `RequirementConversionTest`：TC-DRC-001 (含 sourceDemandIds=[10,20] 成功) / TC-DRC-002 (999999 触发回滚) / TC-DRC-003 (空数组 = 普通创建) — 3 case
- [x] 5.1.4 mvn test 全绿（+ 3 cases，累计 +30 backend）

## 6. frontend-scaffold MODIFIED Capability（P0）

### 6.1 API 类型（M06）

- [x] 6.1.1 `frontend/src/api/demand.ts`：interfaces Demand / DemandCreate / DemandUpdate；functions listDemands / getDemand / createDemand / updateDemand / deleteDemand / getDerivedRequirements
- [x] 6.1.2 `frontend/src/api/requirement.ts`：interfaces Requirement / RequirementCreate（含 `sourceDemandIds?: number[]`） / RequirementUpdate；functions listRequirements / getRequirement / createRequirement / updateRequirement / deleteRequirement / getSourceDemands
- [x] 6.1.3 `frontend/src/api/demandRequirement.ts`：interfaces DemandRequirementLink / DemandRequirementLinkCreate；functions listDemandRequirements / createDemandRequirement / deleteDemandRequirement
- [x] 6.1.4 tsc -b 通过

### 6.2 DemandsPage（M07）— 依赖 #6.1

- [x] 6.2.1 `frontend/src/pages/Demand/DemandsPage.tsx`：列表 + 新建/编辑/删除抽屉；用 select 下拉 status/priority/source；列：title / submitterUserId / status / priority / createTime
- [x] 6.2.2 `DemandEditDrawer.tsx`：form 含 title / description / submitterUserId (User 下拉，listUsers) / status / priority / source / closeReason
- [x] 6.2.3 `frontend/src/pages/Demand/index.tsx` 导出 page
- [x] 6.2.4 vitest 不动（M11 单独写 EditDrawer 测试不在此切片）

### 6.3 RequirementsPage（M08）— 依赖 #6.1

- [x] 6.3.1 `frontend/src/pages/Requirement/RequirementsPage.tsx`：列表 + 编辑抽屉
- [x] 6.3.2 `RequirementEditDrawer.tsx`：主表单 + **「源诉求」分区**（usePaginated 加载 demand 列表 + checkbox state Set<number> + 保存时 `body.sourceDemandIds = Array.from(set)`）
- [x] 6.3.3 `frontend/src/pages/Requirement/index.tsx` 导出

### 6.4 LinksPage（M09）— 依赖 #6.1

- [x] 6.4.1 `frontend/src/pages/DemandRequirement/LinksPage.tsx`：列表 + 新建抽屉（demand select + requirement select + linkType select）+ 删除（硬删，无软删提示）
- [x] 6.4.2 `frontend/src/pages/DemandRequirement/index.tsx` 导出

### 6.5 AppLayout + AppRoutes + 测试（M10）— 依赖 #6.2, #6.3, #6.4

- [x] 6.5.1 `AppLayout.tsx`：Sider 新增菜单组「需求管理」（位于「组织」之后），3 子项：诉求 / 需求 / 诉求-需求关联
- [x] 6.5.2 `AppRoutes.tsx`：注册 4 个 route：`/pm` → redirect `/pm/demands`；`/pm/demands` → DemandsPage；`/pm/requirements` → RequirementsPage；`/pm/demand-requirements` → LinksPage
- [x] 6.5.3 修改 `AppLayout.test.tsx` 增加 TC-FES-D01 断言：含「需求管理」组 + 3 子项
- [x] 6.5.4 新建 `frontend/src/AppRoutes.test.tsx`：TC-FES-D02 mount Memory Router at `/pm/demands` → 找到 DemandsPage 元素（如查找页面 testid 或顶部标题）
- [x] 6.5.5 grep 校验：Bash `grep -c "/pm/demands" frontend/src/AppRoutes.tsx` ≥ 1

### 6.6 RequirementEditDrawer 测试（M11）— 依赖 #6.3

- [x] 6.6.1 `frontend/src/pages/Requirement/RequirementEditDrawer.test.tsx`：TC-FES-D03 mount drawer + mock listDemands 返回 2 条（id=10/20）+ vi.fn() createRequirement → 用户勾选 + 保存 → assert createRequirement 收到 body.sourceDemandIds = `[10, 20]`
- [x] 6.6.2 vitest 全绿（前端累计 13 + 3 = 16 个测试）

## 7. E2E 验证 + 验收（M12）

- [x] 7.1 `docker compose down -v && RAINIER_BACKEND_HOST_PORT=18080 docker compose up -d --build` 起栈，3 服务 healthy
- [x] 7.2 `docker exec rainier-mysql mysql -urainier -prainier rainier -e "SHOW TABLES"` 含 `rainier_{organization, user, user_organization, demand, requirement, demand_requirement}` 6 张表
- [x] 7.3 `DESCRIBE rainier_demand` / `rainier_requirement` / `rainier_demand_requirement`：id 列均为 `bigint auto_increment`；FK 列 (submitter_user_id, owner_user_id, demand_id, requirement_id) 均为 `bigint`
- [x] 7.4 curl POST `/api/demands`、`/api/requirements`、`/api/demand-requirements` 各 1 次确认 201 + 数字 id
- [x] 7.5 curl POST `/api/requirements` w/ `sourceDemandIds=[<id1>, <id2>]` 确认 201 + DB demand_requirement +2 行
- [x] 7.6 `grep -rn 'BaseAutoIdEntity' backend/src` 0 行命中
- [x] 7.7 `grep -rn 'is_pmo\|isPmo' backend/src/main/java backend/src/main/resources/application*.yml frontend/src` 0 行命中（v0.0.5 baseline 守护）
- [x] 7.8 全量 mvn test (≥ 94)、npm test (≥ 16)、npm run build 全绿

## 8. 切片完成度对照

| 切片 | TC 覆盖 | 任务编号 |
|---|---|---|
| M01 | (前置) | 1.1.1-1.1.7 |
| M02 | TC-DMD-001..011 | 2.1.1-2.1.9 |
| M03 | TC-REQ-001..009 | 3.1.1-3.1.9 |
| M04 | TC-DRL-001..007 | 4.1.1-4.1.12 |
| M05 | TC-DRC-001..003 | 5.1.1-5.1.4 |
| M06 | (前置) | 6.1.1-6.1.4 |
| M07 | (UI) | 6.2.1-6.2.4 |
| M08 | (UI) | 6.3.1-6.3.3 |
| M09 | (UI) | 6.4.1-6.4.2 |
| M10 | TC-FES-D01, TC-FES-D02 | 6.5.1-6.5.5 |
| M11 | TC-FES-D03 | 6.6.1-6.6.2 |
| M12 | (E2E) | 7.1-7.8 |
