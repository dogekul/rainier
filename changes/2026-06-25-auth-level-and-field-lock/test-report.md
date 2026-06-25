# test-report: auth-level-and-field-lock (v0.0.69, A5)

## 新增类
- `backend/src/main/java/com/rainier/authz/AiAuthLevel.java` — 常量集 BASIC / INTERMEDIATE / DEPTH。
- `backend/src/main/java/com/rainier/authz/FieldLock.java` — `@Entity rainier_field_lock`，唯一约束 (entity_type, entity_id, field_name)，`@CreationTimestamp locked_at`。
- `backend/src/main/java/com/rainier/authz/FieldLockRepository.java` — `findByEntityTypeAndEntityId` / `existsBy...` / `findBy...` / `deleteBy...`。
- `backend/src/main/java/com/rainier/authz/FieldLockService.java` — `lock` (幂等) / `unlock` (幂等) / `listFor` / `isLocked`。
- `backend/src/main/java/com/rainier/authz/FieldLockDto.java` — 读 DTO。
- `backend/src/main/java/com/rainier/authz/FieldLockCreateRequest.java` — 写请求体。
- `backend/src/main/java/com/rainier/authz/FieldLockController.java` — `/api/field-locks` POST/GET/DELETE (all-users)。
- `backend/src/main/java/com/rainier/me/controller/MeAiAuthLevelController.java` — `POST /api/me/ai-auth-level`（含内部 Request/Response 类）。
- `backend/src/test/java/com/rainier/authz/FieldLockServiceTest.java` — 9 tests。
- `backend/src/test/java/com/rainier/me/controller/MeAiAuthLevelControllerTest.java` — 4 tests。

## 修改类
- `backend/src/main/java/com/rainier/user/domain/User.java` — +`ai_auth_level` nullable column；getter null→"BASIC"。
- `backend/src/main/java/com/rainier/auth/dto/MeResponse.java` — +`aiAuthLevel` 字段（含 6-arg / 5-arg 重载构造器，保持向后兼容）。
- `backend/src/main/java/com/rainier/auth/service/MeService.java` — `forUsername` 用 6-arg 构造器传入 `user.getAiAuthLevel()`。
- `backend/src/test/java/com/rainier/product/bootstrap/LegacyProductCategoryCleanupTest.java` — schema 计数 30 → 31，含 `rainier_field_lock` 断言。

## 新增/修改表
- NEW `rainier_field_lock` (id, entity_type VARCHAR(32) NN, entity_id BIGINT NN, field_name VARCHAR(64) NN, locked_by VARCHAR(16) NN, locked_at DATETIME NN, UNIQUE uk_field_lock_entity_field(entity_type, entity_id, field_name))。
- MODIFIED `rainier_user` + `ai_auth_level VARCHAR(16)` nullable。

## 测试通过数
- backend: **624 tests pass, 0 failures, 0 errors, 0 skipped** (`mvn test` 全量绿)。
  - 新增 13 tests: FieldLockServiceTest (9) + MeAiAuthLevelControllerTest (4)。
- frontend: 未改动。

## Caveats
- 本版仅落库锁，**未在 AI 写路径强制查锁**（按 OutOfScope 规划，留给 A6/A7）。
- field-locks 端点对所有登录用户开放，未做所有者校验（任何用户可锁/解锁任意字段）；若需收敛由后续提案决定。
- FieldLock 不软删，unlock 直接物理 delete（无审计需求）。
