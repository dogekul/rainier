# 诉求 + 需求 + M2M 关联 — 技术设计

## Context

- **代码基线**：v0.0.5-remove-org-pmo（commit 9f7be69）— 单 BaseEntity 世界、全栈 BIGINT 自增、无 Org.isPmo
- **技术栈**：Spring Boot 2.7.18（Java 8）+ MySQL 8 + Hibernate JPA（ddl-auto=update，**Flyway 已禁用**）+ React 18 + Vite + TypeScript + Zustand + Axios
- **已就绪复用资产**：
  - `com.rainier.common.persistence.BaseEntity`（Long id + audit + del_flag）
  - `com.rainier.common.exception.{BadRequestException, ConflictException, NotFoundException, GlobalExceptionHandler}`
  - `com.rainier.common.web.{PageParams, PageResponse}`
  - `com.rainier.userorganization.repository.UserOrganizationRepository`（FK 删除保护参考实现）
  - 前端 `components/ui/{Table, Pagination, Drawer, ConfirmDialog, TreeSelect}` + `hooks/usePaginated`
- **约束**：
  - 不引入新 Maven 依赖
  - 不动 v1 / v0.0.5 任何 entity
  - 不写 SQL 历史档（避免 v0.0.5 H-finding 同类问题）
  - 全栈 id / FK 必须 BIGINT（验收时 grep BaseAutoIdEntity 应 0 行）

## Decisions

### 1. 表结构 + 基类继承 — 沿用 BaseEntity，3 entity 各自加 @Entity + @Table + @SQLDelete + @Where

**方案**：3 个 entity 类（`Demand`、`Requirement`、`DemandRequirementLink`）全部继承 `BaseEntity`，复用其 `Long id` + 5 个审计字段。`Demand` 和 `Requirement` 加 `@SQLDelete` + `@Where("del_flag = 0")`（软删）；`DemandRequirementLink` 不加（硬删）。

**为什么**：v0.0.4 已经把 BaseEntity 改成 Long id + IDENTITY 策略，本次新表是它的天然消费者。@SQLDelete + @Where 是 v0.0.3 验证过的软删模式，复用零成本。M2M 表硬删是因为链接本身没有业务历史价值（断链 = 不再相关），保留 del_flag 列但永远为 0 是为了 BaseEntity 接口一致性。

**备选方案及排除原因**：
- 备选 A：M2M 表也用软删 → 会让"重新挂同一对"需要先查询是否软删存在再恢复，复杂度无收益
- 备选 B：M2M 表不继承 BaseEntity，自定义最小 entity → 违反"复用 BaseEntity"标准，且失去 audit 字段

### 2. 状态 enum 表达 — String 列 + Service 层显式校验集合 + 默认值

**方案**：`status` / `priority` / `source` / `link_type` 列均为 `VARCHAR(16)` 存字符串字面量；Service 在 create/update 时校验值在允许集合内，否则 throw BadRequestException。不在 Java 层定义 `enum` class（保留为字符串常量类 `DemandStatus` / `RequirementStatus` / `LinkType`）。

**为什么**：
- v0.0.3 `OrganizationType` 用 `@Enumerated(EnumType.STRING)` 已有先例，但本次状态机更复杂（5/6 状态），且未来 PO 可能加自定义状态。字符串 + 集合校验是更柔性的设计
- Jackson 反序列化字符串无歧义；前端 TS union type 直接写枚举值字面量即可
- 防止 enum 类增删值导致的迁移负担

**备选方案及排除原因**：
- 备选 A：Java `enum` + `@Enumerated(EnumType.STRING)` —— 同 Org 风格；但 enum 类未来加值要改代码 + 迁移，柔性差
- 备选 B：DB CHECK 约束 —— MySQL 8 支持但 Hibernate ddl-auto 不会生成；与现有"无 DB 约束"风格冲突
- 备选 C：单独 enum table —— 过度设计

### 3. AI 字段策略 — 仅占位，Service 不写 / 不读

**方案**：`Demand.ai_classification` 和 `Demand.ai_duplicate_hint` 列存在；Service 的 create/update 不接受这两个字段（DTO 中也不暴露 setter）；GET response 包含这两个字段（前端只读展示），可空。

**为什么**：
- 字段位预留是 proposal Why 段的核心动机（避免后期 schema 大改）
- v0 不接 LLM，不让 Service 写也防止 admin 误填脏数据
- Read 路径暴露是为了将来 AI worker 后台跑完写回后，前端能立即看到

**备选方案及排除原因**：
- 备选 A：Service 接受 admin 手填这两个字段 —— 与"AI 推断"语义冲突
- 备选 B：列暂不加，等真接 AI 时再加 —— 违反 proposal 的"现在就预留"决策

### 4. M2M 关联 — `POST /api/demand-requirements` + `POST /api/requirements` 内嵌 sourceDemandIds[]

**方案**：M2M 表有两条独立的写入路径：
- 路径 A（标准）：`POST /api/demand-requirements` body `{demandId, requirementId, linkType}` → 创建单条链接
- 路径 B（转化语义）：`POST /api/requirements` body 含可选 `sourceDemandIds: [1, 2]` → 在同一事务内创建 requirement + N 行 demand_requirement 链接，linkType 全部为 `DERIVED`

**为什么**：
- 路径 A 满足"后期手动补关联"的常规需求
- 路径 B 是 PO 转化诉求时的高频原子操作，避免"先建 requirement，再两次 POST link"的三次往返 + 中间状态
- 路径 B 在 service 层用 `@Transactional` 包裹；任一 demand 不存在 → 整体回滚 → requirement 不会留下

**备选方案及排除原因**：
- 备选 A：仅路径 A，前端 do-3-calls —— PO 体验差且中间态可见
- 备选 B：仅路径 B —— "手动补关联"场景缺路径

### 5. FK 删除保护 — 软删 demand/requirement 若有未硬删 link → 409

**方案**：
- `DELETE /api/demands/{id}`：查 `count(*) from rainier_demand_requirement where demand_id=? and del_flag=0` > 0 → 409 "demand has linked requirements"
- `DELETE /api/requirements/{id}`：对称 → 409 "requirement has linked demands"
- `DELETE /api/demand-requirements/{id}`：无下游 → 直接硬删

**为什么**：与 v0.0.3 user-organization 的 `countByOrganizationIdAndLeftAtIsNull` FK 保护风格一致。M2M 表是叶子，自由删。

**备选方案及排除原因**：
- 备选 A：级联软删 —— 危险，可能误删长期价值的 requirement
- 备选 B：不保护 —— 留 orphan link 行污染查询

### 6. 关联查询辅助端点 — 2 个 GET 在 link controller 之外

**方案**：
- `GET /api/requirements/{id}/source-demands` —— Requirement controller 提供；返回 List<DemandDetail with linkType>
- `GET /api/demands/{id}/derived-requirements` —— Demand controller 提供；返回 List<RequirementDetail with linkType>
- 返回 dto 富化 demand/requirement 字段 + `linkType` 标签 + `linkCreatedAt`

**为什么**：
- 端点挂在"主"实体下符合 REST 习惯（拥有关系的一方）
- 富化字段让前端单次请求得到 PO 需要的全部信息（不用再用 demandId 去查 demand 详情 N 次）

**备选方案及排除原因**：
- 备选 A：在 link controller 下提供 `/api/demand-requirements?demandId=` —— 通用但需前端额外 join；不省往返

### 7. 包结构 + 命名 — `com.rainier.{demand, requirement, demandrequirement}`

**方案**：
- `com.rainier.demand.{domain, dto, repository, service, controller}`
- `com.rainier.requirement.{domain, dto, repository, service, controller}`
- `com.rainier.demandrequirement.{domain, dto, repository, service, controller}`（包名无连字符）
- Entity 类名：`Demand`、`Requirement`、`DemandRequirementLink`（避免与表名混淆）

**为什么**：与 v0.0.3 `com.rainier.{organization, user, userorganization}` 风格一致；包名无连字符是 Java 约定

**备选方案及排除原因**：
- 备选 A：合并到 `com.rainier.pm` 单包 —— 文件数 30+ 同包过挤
- 备选 B：`com.rainier.demandrequirementlink` —— 啰嗦

### 8. 前端路由 + Sider 菜单插入位置

**方案**：
- 路由前缀 `/pm/*`：`/pm` → 重定向 `/pm/demands`；`/pm/demands` / `/pm/requirements` / `/pm/demand-requirements`
- Sider 菜单组「**需求管理**」插入在「组织」之后；展开后含 3 项

**为什么**：
- `/pm` prefix 为未来 project / story / task 留命名空间
- 「需求管理」在 PM 主线先于 "组织管理"（PMO 看板等），按业务优先级排序

**备选方案及排除原因**：
- 备选 A：扁平路由 `/demands` `/requirements` —— 命名空间被占，未来无法扩展
- 备选 B：菜单组放最上 —— 喧宾夺主，组织维度是依赖前置

### 9. requirement 编辑抽屉 — sourceDemandIds 多选用「TreeSelect-like」 列表选择器

**方案**：抽屉内嵌一个 demand 列表（带搜索 + 复选框）；保存时把 checked id 收集为 `sourceDemandIds: number[]` 并 POST。编辑 existing requirement 时也支持：从当前已 link 的 demand 列表回填勾选状态；保存 = 删原 link + 加新 link（service 层封装 diff 计算）。

**为什么**：
- demand 数量可能多（几十到几百），下拉选择不够，需要分页 + 搜索
- 复用 `usePaginated` hook + Table 组件，零新增组件

**备选方案及排除原因**：
- 备选 A：复用 TreeSelect —— demand 没有树结构，不适合
- 备选 B：弹出独立"管理关联"模态 —— 与新建路径割裂，UX 差

### 10. 测试策略 — 集成 MockMvc 优先；service 单元仅在有计算逻辑时补

**方案**：
- 后端测试以 `@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")` 集成测试为主，覆盖所有 endpoint 行为
- service 单元测试仅在需要时补（如 demand 转 requirement 时的事务回滚验证 — 可能需要 mock repository）
- repository 测试仅在自定义 native query 时补（如 `countByDemandIdAndDelFlag` 类）
- 前端测试：3 页 + 2 path edge case = ≥ 6 vitest，新建 + 编辑 + 删除路径覆盖

**为什么**：v0.0.3-0.0.5 已证明 MockMvc 集成测试 ROI 最高（一个测试同时验 controller + service + repository + DB），且避开 Mockito 的脆弱性。

## Architecture

```
┌─ 写入链路 ──────────────────────────────────────────────────────────┐
│                                                                      │
│  POST /api/demands ─▶ DemandController ─▶ DemandService.create()     │
│                          │                  · validate enum strings  │
│                          │                  · save → BIGINT id 自增  │
│                          ▼                                            │
│                       DemandRepository (extends JpaRepository)        │
│                                                                       │
│  POST /api/requirements ─▶ RequirementController ─▶ RequirementSvc.   │
│       (含可选 sourceDemandIds[])     │              createWithLinks() │
│                                       │              · @Transactional │
│                                       │              · save Req       │
│                                       │              · for each id:   │
│                                       │                resolve demand │
│                                       │                or throw → 回滚│
│                                       │                save Link      │
│                                       ▼                               │
│                                  RequirementRepository                │
│                                  DemandRequirementLinkRepository      │
└──────────────────────────────────────────────────────────────────────┘
                                  
┌─ 关联查询链路 ─────────────────────────────────────────────────────┐
│                                                                      │
│  GET /api/requirements/{id}/source-demands ─▶ RequirementController  │
│                                                · service.findSource  │
│                                                  Demands(id)         │
│                                                · JOIN demand x link  │
│                                                · 返回富化 dto         │
└──────────────────────────────────────────────────────────────────────┘

┌─ 删除保护链路 ────────────────────────────────────────────────────┐
│                                                                      │
│  DELETE /api/demands/{id} ─▶ DemandController ─▶ DemandService.del() │
│                                  · count link 行                     │
│                                  · > 0 → throw ConflictException 409 │
│                                  · = 0 → softDelete (@SQLDelete)     │
└──────────────────────────────────────────────────────────────────────┘

┌─ 前端 ──────────────────────────────────────────────────────────────┐
│                                                                      │
│  AppLayout (Sider) ─▶ 「需求管理」菜单组                              │
│       ├─ 诉求         ─▶ /pm/demands         DemandsPage            │
│       ├─ 需求         ─▶ /pm/requirements    RequirementsPage        │
│       └─ 诉求-需求关联 ─▶ /pm/demand-requirements  LinksPage          │
│                                                                      │
│  RequirementsPage EditDrawer:                                        │
│       · 主表单（code/title/description/...）                         │
│       · 「源诉求」分区：可分页搜索 demand 列表 + 复选                │
│       · 保存 → POST 或 PUT，sourceDemandIds = checked ids            │
└──────────────────────────────────────────────────────────────────────┘
```

## Risks / Trade-offs

| 风险 | 缓解措施 |
|---|---|
| Status enum 用字符串后，前端可能传非法值导致 service 抛错 → 用户体验差 | TS 类型 + 后端 BadRequestException 双兜底；前端 Sider/Drawer 列出有效值 hardcoded list |
| sourceDemandIds[] 含 unknown demand id 时 requirement 创建回滚 → 前端要能识别 | service 抛 BadRequestException("demand not found: id=N")，前端 Drawer 直接展示后端 message |
| AI 字段位现在留但未来 LLM 选型改变可能字段不够用 | 字段名通用（classification 是聚合概念，duplicate_hint 是简单单值），未来扩展为 JSON 字段可平滑迁移 |
| Hibernate ddl-auto 不删旧 enum 值导致 DB 出现已废弃状态遗留 | 不依赖 ddl-auto 管 enum 集合；service 校验是唯一真相源 |
| Phase 4 工作量大（30+ 后端测试 + 6 前端测试） | 切片 12-14 个，沿用 v0.0.3 build skill 的并行节奏 |
| 主规范 specs/ 现已 11 个 capability，加 4 个 NEW + 1 个 MODIFIED 后 16 个；导航成本上升 | 主 specs/ 文件继续按 capability 名字典排序；archive 提供历史回溯 |
| frontend `AppRoutes.tsx` 可能像 v0.0.3 那样被 linter "改回"，丢失 /pm 路由 | 在 build 验证步骤中显式 `grep -c "/pm/demands" frontend/src/AppRoutes.tsx`（注：项目用扁平结构 `frontend/src/AppRoutes.tsx`，无 `router/` 子目录）|
| 关联查询辅助端点（2 个） vs 标准 RESTful 风格略有偏离 | 决策 6 已论证 ROI；在 spec 中显式说明这是 v0 一次性优化、不影响主路径 |
