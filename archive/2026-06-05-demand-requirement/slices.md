# v0.0.6-demand-requirement 切片执行计划

| # | 优先级 | TC 覆盖 | 实现目标 | 依赖 |
|---|---|---|---|---|
| M01 | P0 | (前置) | 常量类：`com.rainier.demand.domain.{DemandStatus, Priority, Source}`、`com.rainier.requirement.domain.{RequirementStatus, Complexity}`、`com.rainier.demandrequirement.domain.LinkType` 各 String 常量集合（用于 service 校验） | 无 |
| M02 | P0 | TC-DMD-001..011 (11) | Demand 链路完整：`Demand` entity（继承 BaseEntity，软删 @SQLDelete + @Where）+ DTO（Create / Update / Detail）+ Repository + Service（create/get/list/update/delete + FK 保护）+ Controller（POST/GET-detail/GET-list/PUT/DELETE）+ MockMvc 测试 11 case | M01 |
| M03 | P0 | TC-REQ-001..009 (9) | Requirement 链路完整：`Requirement` entity + 4 DTO + Repository + Service（含 code 全局唯一性 + projectId 字段位 + ownerUserId 不可变）+ Controller + MockMvc 测试 9 case | M01 |
| M04 | P0 | TC-DRL-001..007 (7) | DemandRequirementLink 链路：`DemandRequirementLink` entity（继承 BaseEntity 但**不**加 @SQLDelete，硬删）+ DTO + Repository（含 `countByDemandId`、`countByRequirementId`，供 FK 保护）+ Service + Controller（5 CRUD）+ **2 个辅助端点**（在 Requirement / Demand controller 中追加 `GET /{id}/source-demands` + `GET /{id}/derived-requirements`）+ MockMvc 测试 7 case | M02, M03 |
| M05 | P0 | TC-DRC-001..003 (3) | Workflow conversion：扩展 `RequirementService.create()` 接受可选 `sourceDemandIds`；在同一 `@Transactional` 内创建 requirement + N 行 link；任一 demand 不存在 → 抛 BadRequestException → 整体回滚；MockMvc 测试 3 case（含回滚验证） | M02, M03, M04 |
| M06 | P0 | (前置) | 前端 API 类型层：`frontend/src/api/{demand,requirement,demandRequirement}.ts` —— 3 个 module 提供 list / get / create / update / delete / 2 辅助查询 + 完整 TS 接口（Demand / DemandCreate / DemandUpdate；Requirement / RequirementCreate（含可选 sourceDemandIds: number[]）/ RequirementUpdate；DemandRequirementLink / DemandRequirementLinkCreate） | 无 |
| M07 | P0 | (UI) | 前端 DemandsPage：`/pm/demands` 列表 + 新建 + 编辑 + 删除抽屉 + 状态枚举下拉；复用 v1 Table/Pagination/Drawer/ConfirmDialog/usePaginated | M06 |
| M08 | P0 | (UI) | 前端 RequirementsPage：`/pm/requirements` 列表 + 编辑抽屉（含 sourceDemandIds **分页 + 搜索 + 复选** 子区）；保存时 POST 含 sourceDemandIds[] | M06 |
| M09 | P0 | (UI) | 前端 LinksPage：`/pm/demand-requirements` 列表 + 新建（双 select demand/requirement）+ 编辑 linkType + 删除（硬删） | M06 |
| M10 | P0 | TC-FES-D01, TC-FES-D02 | 前端 AppLayout：Sider 新增菜单组「需求管理」+ 3 子项（位于「组织」之后）；`AppRoutes.tsx` 注册 `/pm/demands`、`/pm/requirements`、`/pm/demand-requirements` + `/pm` 重定向 `/pm/demands`；改造现有 `AppLayout.test.tsx` 覆盖 TC-FES-D01；新建 `AppRoutes.test.tsx` 覆盖 TC-FES-D02（含 `grep -c "/pm/demands" AppRoutes.tsx` 兜底） | M07, M08, M09 |
| M11 | P0 | TC-FES-D03 | 新建 `frontend/src/pages/Requirement/RequirementEditDrawer.test.tsx`：mount 抽屉 + mock listDemands 返回 2 条 + 复选 + 点保存 → assert mock createRequirement 收到 `body.sourceDemandIds: [10, 20]` | M08 |
| M12 | P0 | TC-E2E-001..003 | E2E 验证：`docker compose down -v && up -d --build`；`docker exec mysql -e "SHOW TABLES"` 含 `rainier_{demand, requirement, demand_requirement}` 三张新表；`DESCRIBE` 各表确认 id 列 BIGINT auto_increment + FK 列 BIGINT；curl POST 三套端点（demand / requirement / link）含验证；curl POST /requirements w/ sourceDemandIds=[1,2] 验证关联落库；`grep -rn 'BaseAutoIdEntity' backend/src` 0 命中 | M01..M11 |

## 执行顺序图

```
M01 (常量)
   │
   ├── M02 (Demand 链)
   │     │
   │     └── M03 (Requirement 链)  ← 也可与 M02 并行（但保持串行简化）
   │           │
   │           └── M04 (Link 链 + 2 辅助端点)
   │                 │
   │                 └── M05 (workflow conversion)
   │
M06 (前端 API 类型) — 与 M01 同时可启动，但 build skill 选串行
   │
   ├── M07 (DemandsPage)
   ├── M08 (RequirementsPage)
   ├── M09 (LinksPage)
   │       │
   │       └── M10 (AppLayout + AppRoutes + TC-FES-D01/D02)
   │             │
   │             └── M11 (RequirementEditDrawer.test TC-FES-D03)
   │
M12 (E2E + DESCRIBE + curl) ← 同步点，等全部前置完成
```

**长程模式下顺序执行**：M01 → M02 → M03 → M04 → M05 → M06 → M07 → M08 → M09 → M10 → M11 → M12（串行 RED/GREEN/REFACTOR）。

## 关键技术 checklist（实现期常见陷阱预防）

- M02/M03/M04：`@Column` 必须显式标 nullable，避免 Hibernate ddl-auto 推断错；enum 字符串列 length=16 与 spec 一致
- M02/M03：service 校验 enum 集合时用 `Set<String>` 静态常量，BadRequestException 给清晰 message
- M04：`@SQLDelete` 不加在 link entity；但仍继承 BaseEntity（含 del_flag 列），del_flag 永远 0 — entity 类不暴露 setDelFlag 给 service
- M05：`@Transactional`(rollbackFor=Exception.class) 确保 BadRequestException 触发回滚（默认只回滚 RuntimeException —— BadRequestException 是 RuntimeException 子类则可，但需复查）
- M06：TS 类型 `sourceDemandIds?: number[]` 必须 optional；`RequirementUpdate` 不应含 sourceDemandIds（避免 PUT 路径误用）
- M08：RequirementEditDrawer 「源诉求」分区用 `usePaginated` 加载 demand 列表；checkbox state 用 `Set<number>` 管理
- M10：`AppRoutes.tsx` 添加路由时确保 import 4 个新页面组件；保存前 grep 校验防 linter 回退
- M12：DESCRIBE 三表的 FK 列均应为 `bigint`，非 `bigint(20)` 或 `varchar(32)`
