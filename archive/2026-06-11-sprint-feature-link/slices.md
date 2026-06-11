# v0.0.14-sprint-feature-link 切片执行计划

> 10 切片全 P0。M01→M02 链式（Sprint productId 先于 enrich）；M03→M04→M05 链式（骨架→service→controller）；M06 依赖 M04（反查委托 link service）；M04 依赖 M01（惰性写 sprint.productId）。

| # | 优先级 | TC 覆盖 | 实现目标 | 依赖 |
|---|--------|---------|---------|------|
| M01 | P0 | (结构) | Sprint entity +productId(nullable) + Create/Detail DTO（Update 不含） | 无 |
| M02 | P0 | TC-SPR-PF-001..004 | SprintService: create 预绑校验 + update 不碰 productId + list/enrich product batch | M01 |
| M03 | P0 | (结构) | sprintfeature 骨架: SprintFeatureLink + 2 DTO + 2 View + Repository | 无 |
| M04 | P0 | (服务) | SprintFeatureLinkService: create 校验链(惰性锁定+产品一致性+唯一) + delete 硬删 + 3 反查方法(2 跳去重) | M01, M03 |
| M05 | P0 | (端点) | SprintFeatureLinkController /api/sprint-features POST/GET/{id}/list/DELETE | M04 |
| M06 | P0 | (端点) | 反查挂拥有方: Sprint/Feature/Requirement controller +GET 委托 link service | M04 |
| M07 | P0 | TC-SF-001..009 / TC-SPR-PF-001..004 / TC-SF-REV-001..008 / TC-PERF×2 | backend 全测试(4 测试类 + 2 perf) | M05, M06 |
| M08 | P0 | (api) | 前端 api/sprintFeature.ts + sprint.ts(+product) + requirement.ts(+features 反查) | M05, M06 |
| M09 | P0 | TC-FES-SF-001..003 | Sprint 关联功能面板(挂/解绑) + Feature 所在迭代 + 测试 | M08 |
| M10 | P0 | TC-E2E-SF-001..002 | docker compose + SHOW TABLES=17 + 存量 product_id null + curl 全链 | M01..M09 |

## 执行批次（拓扑序）

```
批次 1（可并行）: M01, M03
批次 2: M02 (← M01), M04 (← M01,M03)
批次 3: M05 (← M04), M06 (← M04)
批次 4: M07 (← M05,M06), M08 (← M05,M06)
批次 5: M09 (← M08)
批次 6: M10 (← 全部)
```

## 隐藏陷阱备忘（from Phase 2 + 经验）

- **A** Java 8: 无 `Set.of`/`List.of`/无参 `orElseThrow()`。
- **B** 硬删链接: SprintFeatureLink 不加 `@SQLDelete`/`@Where`，del_flag 列继承但不用（仿 DemandRequirementLink）。
- **C** productId 惰性写入与 link insert 同一 `@Transactional`，失败一起回滚。
- **D** feature→product 是 2 跳: `feature.moduleId → ProductModule.productId`，feature 无直接 productId。
- **E** SprintUpdateRequest 不含 productId（不可变，仿 requirementId）；中文 JSON 断言用 `getContentAsString(StandardCharsets.UTF_8)`（v0.0.13 陷阱）。
- **F** requirement 2 跳去重用 `LinkedHashSet` 保序。
- **G** 反查端点注入 link service 到 3 controller — link service 只依赖 repo 不依赖业务 service，无循环依赖。
- **H** Sprint list perf 预算 v0.0.10.1 ≤5 提到 ≤6（多 product join）；范围断言防假绿。
- **I** 测试 seed 直接 `repo.saveAndFlush` 构造 product→module→feature 链 + requirement→sprint。
- **J** SprintFeatureLinkService.create 注入 `ProductModuleRepository`（解析 feature 产品）；`findById` JpaRepository 自带。
- **K** SprintService 既有 enrich 已 join user/requirement/project/storyCount（v0.0.10.1）—— 加 product 维度时复用既有 batch 框架，别破坏既有 4 维富化。
