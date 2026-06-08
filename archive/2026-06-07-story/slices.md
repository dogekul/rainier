# v0.0.9-story 切片执行计划

> 9 切片（M01-M09）：后端 5 + 前端 3 + E2E 1
> 全部 P0；M01..M05 严格顺序（依赖链）；M06..M08 必须 M05 之后；M09 E2E 收尾

## 切片表

| # | 优先级 | TC 覆盖 | 实现目标 | 依赖 |
|---|---|---|---|---|
| M01 | P0 | TC-STR-001..009 (create path) | `com.rainier.story` 新包：Story / StoryStatus / Repo / DTO ×3 / Service.create + enrich / Controller POST。注入 RequirementRepo + UserRepo + ProjectRepo。projectId 从父 Requirement 继承。 | 无 |
| M02 | P0 | TC-STR-010..012 (read path) | Service.findById + Service.list（含 Specification + filter requirementId/status/priority/search）+ Controller GET 单 / GET 列表 | M01 |
| M03 | P0 | TC-STR-013..015 (update path) | Service.update（含 owner 校验；requirementId/projectId 不可改即使 payload 含也忽略）+ Controller PUT | M01 |
| M04 | P0 | TC-STR-016 (delete path) | Service.delete + Controller DELETE（软删 @SQLDelete 触发） | M01 |
| M05 | P0 | TC-REQS-001 + TC-REQS-002 | RequirementService 改造：注入 StoryRepository；delete 加 Story FK 保护（顺序：先 demand_requirement 后 story）；enrich 加 storyCount；RequirementDetail.storyCount 字段 | M01 |
| M06 | P0 | — (前端 type) | `frontend/src/api/story.ts`：Story TS type + StoryCreate/Update/ListParams + 5 个 CRUD 函数 | M01 (after backend endpoint shape stable) |
| M07 | P0 | TC-FES-S03 + TC-FES-S04 | `StoryEditDrawer.tsx`：抽屉组件 — 标题/描述/AC/状态/优先级/复杂度/负责人下拉；默认 owner 沿用 v0.0.8 模式（auth store.username matches listUsers）；编辑模式 owner 不 disabled；+ 单元测试 | M06 |
| M08 | P0 | TC-FES-S01 + TC-FES-S02 | `StoryListPanel.tsx` 新子组件 + `RequirementsPage.tsx` 改造：行展开按钮 + storyCount 列 + 子区域渲染 StoryListPanel（含新建按钮 + 编辑/删除）；`api/requirement.ts` Requirement type 加 storyCount；+ RequirementsPage.test.tsx 测试（新建如不存在） | M05 + M07 |
| M09 | P0 | E2E | `mvn package` + `docker compose build backend frontend` + `up -d --no-deps --force-recreate`（**不 down -v**）+ docker exec mysql SHOW TABLES = 11 含 rainier_story + DESCRIBE rainier_story + curl flow（创 Story、不存在 requirementId 400、PUT 改 owner、DELETE Requirement 含 Story 409、GET Requirement storyCount） | M05 + M08 |

## 依赖图

```
M01 ── M02
  │
  ├──── M03
  │
  ├──── M04
  │
  ├──── M05 ── M08 ── M09
  │            │
  └──── M06 ── M07 ──┘
```

## 长程模式特别注意（v0.0.8 / v0.0.8.1 经验教训）

- **Java 8 兼容**：`StoryStatus.ALL` 用 `Collections.unmodifiableSet(new HashSet<>(Arrays.asList(...)))` —— **不能用 `Set.of`**（编译失败）
- **无 DB UNIQUE**：Story.code 唯一靠 service `existsByCode`，不要在 `@Column` 上加 `unique=true`（与 v0.0.8 family pattern 一致）
- **前端 listUsers / listProjects 必须 `size: 100`**：v0.0.7 PageParams `size <= 100` 校验是硬约束，否则返 400（v0.0.7 hotfix 教训）
- **frontend index.tsx 默认导出**：所有 page 模块 export default（AppRoutes 用 default import）；不要只 named export（v0.0.8 M07 修复教训）
- **测试隔离**：所有 `@SpringBootTest` 测试类 `@BeforeEach cleanDb()` 删除所有相关 repo（按 FK 顺序：story → requirement → user / project）
- **frontend test mock 全 promise resolve**：`listUsers().then(...)` 需 `await waitFor(...)`
- **测试 createBy 弱断言可接受**：test profile auditor 注入 "system"，断言 `.value("system")` 或 `.isString().isNotEmpty()`
- **删除时 status / del_flag 检查**：软删后 `GET` 必须返 404（@Where 过滤生效）

## 后续 STDD 衔接

完成所有 M01..M09 后，**自动进入 stdd-verify** —— Step 0 三路评审 → Step 1-5 → Gate 3 等用户确认。
