# Test Report — ai-inference-and-error-board (A4, v0.0.68)

## 新增类
- `backend/src/main/java/com/rainier/ai/AiInference.java` — 飞轮 AI 调用统一接口
- `backend/src/main/java/com/rainier/ai/StubAiInference.java` — `@Primary` deterministic stub 实现
- `backend/src/main/java/com/rainier/ai/domain/AiError.java` — 实体 `rainier_ai_error`
- `backend/src/main/java/com/rainier/ai/domain/AiErrorStatus.java` — OPEN / FIXED 常量
- `backend/src/main/java/com/rainier/ai/repository/AiErrorRepository.java`
- `backend/src/main/java/com/rainier/ai/service/AiErrorService.java` — record / markFixed / list
- `backend/src/main/java/com/rainier/ai/controller/AiErrorController.java` — `/api/ai/errors` REST
- `backend/src/main/java/com/rainier/ai/dto/AiErrorDetail.java`
- `backend/src/main/java/com/rainier/ai/dto/AiErrorFixRequest.java`
- 测试：`backend/src/test/java/com/rainier/ai/StubAiInferenceTest.java`，`backend/src/test/java/com/rainier/ai/AiErrorServiceTest.java`

## 修改
- `backend/src/main/java/com/rainier/authz/AdminPaths.java` — TIER_B 新增 `/api/ai/errors`（写 admin，读 all-users）
- `backend/src/test/java/com/rainier/product/bootstrap/LegacyProductCategoryCleanupTest.java` — 表数从 29 → 30，并断言 `rainier_ai_error` 存在

## 新增表
`rainier_ai_error`（JPA ddl-auto=update 自动创建）：
- `id` BIGINT PK
- `occurred_at` TIMESTAMP NOT NULL
- `ai_action` VARCHAR(128) NOT NULL
- `error_desc` VARCHAR(512) NOT NULL
- `affected_entity_type` VARCHAR(64) NULL
- `affected_entity_id` BIGINT NULL
- `root_cause` VARCHAR(16) NULL
- `status` VARCHAR(16) NOT NULL DEFAULT 'OPEN'
- `fix_action` VARCHAR(512) NULL
- `evidence` LOB NULL
- `create_by` / `create_time` / `update_by` / `update_time` / `del_flag` (来自 BaseEntity)

## 测试通过数
- backend: 611 tests pass (全量 mvn test，含本次新增 17 tests: StubAiInferenceTest 8, AiErrorServiceTest 9)
- frontend: 未改动，未运行

## Caveats
- StubAiInference 对没有无参构造器的输出类型会抛 `IllegalArgumentException`（已加 unit test 覆盖）。后续接入真实 LLM 时需替换为基于 prompt 的实现。
- AiError 录入仍是手动调用 `AiErrorService.record(...)`；本版未提供创建 REST 端点（OutOfScope，自动检测后续做）。
- 前端 UI 待 A9 实现。
