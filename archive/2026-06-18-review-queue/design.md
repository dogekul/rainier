# Design — v0.0.39-review-queue

> Baseline: tag `v0.0.38-real-auth` / commit 4c9fd46. Gate 1 decisions R1–R4 locked.

## Context

Story today has `ownerUserId` but no reviewer concept. The role-chain audit (#7) found 「谁等我评审」
不存在于数据层 → 架构师 0%、测试无放行门、PM/PO 无评审队列。`ddl-auto=update` (Flyway off) means new
nullable columns are auto-created by Hibernate — zero migration, zero backfill, existing rows get `null`
review fields (out of every queue). All-users `/api/me/*` substrate already exists (PortfolioController
pattern) and is the read-model to mirror.

## Decisions

### D1 — Reviewer fields on Story (additive, nullable)
`Story` gains `reviewerUserId` (`BIGINT NULL`, soft FK→user) + `reviewStatus` (`VARCHAR(16) NULL`).
`ReviewStatus` constants class (Java-8 in-memory `ALL` set, mirroring `StoryStatus`): `PENDING / APPROVED /
REJECTED`; `null` = 无评审需求. A `DECISIONS` set = `{APPROVED, REJECTED}` gates the review action.
- *Why nullable*: existing stories must stay valid + out of all queues; ddl-auto adds nullable cleanly.
- *Why String not enum*: 柔性优于 enum — same rationale as Priority/StoryStatus (no migration on new values).

### D2 — Assignment via create/update (full-replace); decision via dedicated endpoint (R1)
`StoryCreateRequest` / `StoryUpdateRequest` gain optional `reviewerUserId` + `reviewStatus`. Service
validates: `reviewerUserId != null → userRepo.existsById else 400`; `reviewStatus != null →
ReviewStatus.ALL else 400`. On create/update these are set (update = full-replace, consistent with the
rest of the Story PUT representation). The **decision** is a separate one-click action:
`POST /api/stories/{id}/review {decision: "APPROVED"|"REJECTED"}` → `StoryService.review(id, decision)`
sets `reviewStatus`, keeps `reviewerUserId`, returns enriched `StoryDetail`. Unknown id→404, invalid
decision→400.
- *Why dedicated decision endpoint*: the architect board approves/rejects without re-sending the whole
  StoryUpdateRequest (which forces code/title/status/priority/owner). Matches R1「极简」.
- *Note*: PUT full-replace means a Story edit that omits reviewer fields clears them. Acceptable for
  v0.0.39 (no Story-edit UI is in scope); a future Story edit UI must surface reviewer fields. Existing
  Story-update tests stay green (null→null no-op on stories without a reviewer).

### D3 — `GET /api/me/pending-reviews` self-scoped read-model (R2 all-users)
New `MePendingReviewsController` (`@RequestMapping("/api/me")`, mirrors PortfolioController:
`currentUsername(request)` from `AuthController.ATTR_USERNAME`, 401 when missing). New `MeReviewService`:
`findByLoginName(username)` → `null → []`; else `StoryRepository.findByReviewerUserIdAndReviewStatus(me.id,
PENDING)` (`@Where del_flag=0` auto-applies) → batch-enrich (owner/project/sprint, mirroring
`StoryService.list`) → sort. **Not** in `AdminPaths` → all-users (token-gated, non-admin).
- *Sort*: priority rank (URGENT<HIGH<MEDIUM<LOW<LOWEST, unknown last) then `createTime` ascending
  (oldest-waiting first).

### D4 — `PendingReview` DTO
`from(Story)` populates storyId/code/title/status/priority/reviewStatus/projectId/sprintId/ownerUserId/
createTime; setters for enriched projectName/sprintName/ownerName/ownerLoginName (mirrors StoryDetail).

### D5 — Frontend「我的评审」landing page (R4)
`api/reviews.ts`: `getPendingReviews()` → `client.get('/me/pending-reviews')`; `submitReview(storyId,
decision)` → `client.post('/stories/{id}/review', {decision})`. `ReviewsPage` (title「我的评审」, route
`/reviews`): `StatTiles`(待评数) + `DashboardCard` list (each row: priority `StatusChip` + `OwnerChip`
提交人 + title link + 通过/打回 buttons → submitReview → refetch) + `EmptyState`. Nav item「评审看板」(icon
`check`) added to the all-users「数据看板」group; route added to `AppRoutes`; **not** in `isAdminPath`
(navGuardConsistency auto-verifies all-users).

## Architecture / Data flow

```
assign:  POST/PUT /api/stories          → StoryService.create/update → reviewerUserId + reviewStatus=PENDING
decide:  POST /api/stories/{id}/review  → StoryService.review        → reviewStatus=APPROVED|REJECTED
read:    GET  /api/me/pending-reviews   → MeReviewService            → [reviewerUserId=me ∧ PENDING], enriched, sorted
UI:      ReviewsPage  → getPendingReviews / submitReview → board-kit
```

## Risks / Trade-offs

| Risk | Mitigation |
|---|---|
| PUT full-replace wipes reviewer fields | Documented (D2); no Story-edit UI in scope; existing tests null→null no-op |
| 所有用户可记录评审决定 (no fine-grained authz) | R2 accepted — consistent with all-users CRUD; fine-grained review-perms = follow-up |
| pending-reviews enrich N+1 | Batch findAllById (owner/project/sprint), mirrors StoryService.list Code-M1 fix |
| 存量 Story 误入队列 | reviewStatus=null ≠ PENDING → never returned; standing 约束 honored |
| Java 8 trap | ReviewStatus uses Collections.unmodifiableSet(new HashSet<>(Arrays.asList(...))); no Set.of / no-arg orElseThrow; Docker temurin-8 is the gate |
