# v0.0.9-story 任务清单

> 9 切片 → ~50 颗粒任务。所有 P0。

## 1. M01 — Story 后端基础（create path）

### 1.1 entity / repository

- [ ] 1.1.1 创建 `backend/src/main/java/com/rainier/story/domain/Story.java`（@Entity + @Table("rainier_story") + @SQLDelete + @Where + extends BaseEntity）字段：code / title / description / acceptanceCriteria / status / priority / complexity / requirementId / projectId / ownerUserId / closeReason，全部 column + getter/setter
- [ ] 1.1.2 创建 `backend/src/main/java/com/rainier/story/domain/StoryStatus.java`：6 项常量 (DRAFT/READY/IN_PROGRESS/DONE/BLOCKED/CANCELLED) + `Set<String> ALL`（Java 8 `Collections.unmodifiableSet(new HashSet<>(Arrays.asList(...)))`）
- [ ] 1.1.3 创建 `backend/src/main/java/com/rainier/story/repository/StoryRepository.java`：extends JpaRepository<Story, Long>, JpaSpecificationExecutor<Story>；方法：existsByCode / countByRequirementId / countByOwnerUserId

### 1.2 dto

- [ ] 1.2.1 创建 `StoryCreateRequest.java`：@NotBlank code/title；@NotNull requirementId/ownerUserId；可选 description / acceptanceCriteria / status / priority / complexity；**不含 projectId**（继承）
- [ ] 1.2.2 创建 `StoryUpdateRequest.java`：@NotBlank code/title；@NotNull status/priority/ownerUserId；可选 description / acceptanceCriteria / complexity / closeReason；**不含 requirementId / projectId**
- [ ] 1.2.3 创建 `StoryDetail.java`：business 字段 + 富化（ownerName / ownerLoginName / requirementCode / requirementTitle / projectName / projectCode）+ `from(Story)` 静态工厂

### 1.3 service + controller (create path)

- [ ] 1.3.1 创建 `StoryService.java`：注入 StoryRepository + RequirementRepository + UserRepository + ProjectRepository
- [ ] 1.3.2 实现 `create(StoryCreateRequest)`：校验 requirementId/ownerUserId 存在、code 唯一、status/priority/complexity 合法、enum 默认值；**从 parent Requirement 复制 projectId** 填入；持久化；调 enrich 返回 detail
- [ ] 1.3.3 实现私有 `enrich(Story r)`：join User + Requirement + Project（projectId 非空时）注入富化字段；防御性 null 处理
- [ ] 1.3.4 创建 `StoryController.java`：`@RestController @RequestMapping("/api/stories")` + POST endpoint 返 201
- [ ] 1.3.5 测试：TC-STR-001..009 在 `StoryControllerCreateTest.java`（9 用例）— 含 createBy 弱断言

## 2. M02 — Story 后端 read path

- [ ] 2.1 实现 `Service.findById(Long id)`：getOrThrow + enrich
- [ ] 2.2 实现 `Service.list(Long requirementId, String status, String priority, PageParams page)`：Specification 拼 requirementId/status/priority/search 过滤 + Sort.by("createTime").descending() + map enrich
- [ ] 2.3 Controller GET 单 (`@GetMapping("/{id}")`) + GET 列表 (`@GetMapping`)
- [ ] 2.4 测试：TC-STR-010..012 在 `StoryControllerQueryTest.java`（3 用例）

## 3. M03 — Story 后端 update path

- [ ] 3.1 实现 `Service.update(Long id, StoryUpdateRequest)`：getOrThrow + 校验 status/priority/complexity 合法 + owner 变更 → 校验存在 → setOwnerUserId（沿用 v0.0.8 Decision 6b）+ code 变更 → 重检唯一性 + 其它字段 set + saveAndFlush + enrich
- [ ] 3.2 Controller PUT (`@PutMapping("/{id}")`)
- [ ] 3.3 测试：TC-STR-013..015 在 `StoryControllerQueryTest.java`（追加 3 用例）

## 4. M04 — Story 后端 delete path

- [ ] 4.1 实现 `Service.delete(Long id)`：getOrThrow + `repo.delete(s)`（@SQLDelete 自动 SET del_flag=1）
- [ ] 4.2 Controller DELETE (`@DeleteMapping("/{id}")`) 返 204
- [ ] 4.3 测试：TC-STR-016 在 `StoryControllerDeleteTest.java`（1 用例）

## 5. M05 — Requirement 改造（FK + storyCount）

- [ ] 5.1 `requirement/dto/RequirementDetail.java`：加 `Long storyCount` 字段 + setter
- [ ] 5.2 `requirement/service/RequirementService.java`：注入 `StoryRepository`
- [ ] 5.3 修改 `RequirementService.delete`：在 `linkRepo.count(...) > 0 → 409` 之后追加 `if (storyRepo.countByRequirementId(id) > 0) throw new ConflictException("requirement has linked stories")`
- [ ] 5.4 修改 `RequirementService.enrich`：加 `dto.setStoryCount(storyRepo.countByRequirementId(r.getId()))`
- [ ] 5.5 测试：TC-REQS-001 在 `RequirementControllerDeleteTest.java`（追加 1 用例） + TC-REQS-002 在 `RequirementControllerQueryTest.java`（追加 1 用例：GET 单 + list 都验证 storyCount）

## 6. M06 — 前端 api/story.ts

- [ ] 6.1 创建 `frontend/src/api/story.ts`：Story TS interface + StoryStatus union + StoryCreate/Update/ListParams + 5 个 CRUD 函数（listStories / getStory / createStory / updateStory / deleteStory）
- [ ] 6.2 修改 `frontend/src/api/requirement.ts`：Requirement type 加 `storyCount?: number`

## 7. M07 — StoryEditDrawer + 测试

- [ ] 7.1 创建 `frontend/src/pages/Requirement/StoryEditDrawer.tsx`：Drawer with code/title/description/acceptanceCriteria/状态下拉/优先级下拉/复杂度下拉/负责人下拉；新建时锁定 Requirement 字段（display only）；默认 owner = current loginName matched user；编辑时回显 + owner 不 disabled；formError 状态（沿用 v0.0.8.1 Code-M7 模式）
- [ ] 7.2 创建 `frontend/src/pages/Requirement/StoryEditDrawer.test.tsx`：TC-FES-S03 + TC-FES-S04（默认 owner / owner 可改 → updateStory）+ 必填校验 form error

## 8. M08 — RequirementsPage drilldown + 测试

- [ ] 8.1 创建 `frontend/src/pages/Requirement/StoryListPanel.tsx`：表格（id / code / title / status / priority / owner / actions） + "新建 Story" 按钮（data-testid="stories-new-btn"）+ 每行编辑/删除按钮 + `data-testid="story-list-panel-{requirementId}"`
- [ ] 8.2 修改 `frontend/src/pages/Requirement/RequirementsPage.tsx`：表格加 "Story 数" 列 显示 r.storyCount，行展开按钮 toggle expanded Set<id>，展开时渲染 StoryListPanel
- [ ] 8.3 测试：检查是否已存在 `RequirementsPage.test.tsx`，不存在则新建；加 TC-FES-S01 + TC-FES-S02 用例（Story 数列 + 点开渲染面板）

## 9. M09 — E2E 验证（不 down -v）

- [ ] 9.1 `cd backend && mvn -q spotless:apply test` 全绿（148 + 19 ≥ 167 后端）
- [ ] 9.2 `cd frontend && npx vitest run` 全绿（32 + 4 ≥ 36 前端）
- [ ] 9.3 `npx tsc -p tsconfig.json --noEmit` 0 错误
- [ ] 9.4 `npx vite build` 0 错误
- [ ] 9.5 `cd backend && mvn -q package -DskipTests`
- [ ] 9.6 `docker compose build backend frontend`（不 down -v）
- [ ] 9.7 `docker compose up -d --no-deps --force-recreate backend frontend`
- [ ] 9.8 `docker exec rainier-mysql mysql -uroot -prainier_root rainier -e "SHOW TABLES;"` = 11 张表，含 `rainier_story`
- [ ] 9.9 `docker exec rainier-mysql mysql -uroot -prainier_root rainier -e "DESCRIBE rainier_story;"` 字段集匹配 design.md §Class layout
- [ ] 9.10 curl flow：
  - 登录 admin → 拿 token
  - POST /api/stories 含 requirementId=已有 → 201 + 验证 projectId 自动继承 + ownerName + requirementCode + projectName 富化
  - POST /api/stories 含 requirementId=999 → 400 "requirement not found"
  - PUT /api/stories/{id} 改 ownerUserId → 200 + ownerName 跟随
  - DELETE /api/requirements/{id}（有 Story 引用）→ 409 "requirement has linked stories"
  - GET /api/requirements/{id} → body.storyCount ≥ 1
- [ ] 9.11 `git diff --stat HEAD` 验证范围：仅 v0.0.9 文件 + Requirement 改造 + RequirementsPage 改造
- [ ] 9.12 v0.0.8 测试数据完整（user / project / requirement / user_role 不丢）
