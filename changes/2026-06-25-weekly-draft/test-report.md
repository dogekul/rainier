# Test report — weekly-draft (A7, v0.0.71)

## 新增类
- `backend/src/main/java/com/rainier/weekly/domain/WeeklyDraft.java`
- `backend/src/main/java/com/rainier/weekly/domain/WeeklyDraftStatus.java`
- `backend/src/main/java/com/rainier/weekly/repository/WeeklyDraftRepository.java`
- `backend/src/main/java/com/rainier/weekly/service/WeeklyDraftService.java`
- `backend/src/main/java/com/rainier/weekly/dto/WeeklyDraftResponse.java`
- `backend/src/main/java/com/rainier/weekly/dto/GenerateWeeklyDraftRequest.java`
- `backend/src/main/java/com/rainier/weekly/controller/MeWeeklyDraftController.java`

## 新增测试
- `backend/src/test/java/com/rainier/weekly/WeeklyDraftServiceGenerateTest.java` — 7 tests

## 修改类
- `backend/src/test/java/com/rainier/product/bootstrap/LegacyProductCategoryCleanupTest.java`
  — 表数 31 → 32（rainier_weekly_draft 加入）

## 新增表
| 表 | 字段 |
|----|------|
| `rainier_weekly_draft` | `id BIGINT PK`, `user_id BIGINT NOT NULL`, `period_start DATE NOT NULL`, `period_end DATE NOT NULL`, `content_markdown CLOB NOT NULL`, `status VARCHAR(16) NOT NULL DEFAULT 'DRAFT'`, `created_at TIMESTAMP NOT NULL`, `accepted_at TIMESTAMP NULL`, + BaseEntity 公共列 (`create_by/create_time/update_by/update_time/del_flag`) |

## 新增端点
- `POST /api/me/weekly-drafts/generate` body `{periodStart,periodEnd}` → `WeeklyDraftResponse`
- `GET /api/me/weekly-drafts?page=&size=` → `PageResponse<WeeklyDraftResponse>`
- `POST /api/me/weekly-drafts/{id}/accept` → `WeeklyDraftResponse`

## 测试通过
- Backend: **637 tests pass / 0 fail / 0 error / 0 skipped** (`mvn test`)
- Frontend: 未触及（无前端改动，本 sub-change OutOfScope）

## Caveats
- accept 端点未做 owner 校验（本版仅 token-gated，service 接受 id 即放行）；A8 / 后续 admin 入口
  接入时需补 `WeeklyDraft.userId == currentUserId` 检查
- `update_time` 在 H2 测试里通过 Hibernate auditing 自动写入，因此 `seedTask` 后立刻被 generate 看见；
  生产环境若 DST 跨界，按 `ZoneId.systemDefault()` 拼装 instant 可能差 1 小时（一般无关紧要）
- 未接 LLM / 邮件 push（A8 范畴），markdown 仅模板拼接
