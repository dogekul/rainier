# test report — push-channel (v0.0.72, A8)

## 新增类清单
- `backend/src/main/java/com/rainier/notification/domain/Notification.java`
- `backend/src/main/java/com/rainier/notification/repository/NotificationRepository.java`
- `backend/src/main/java/com/rainier/notification/service/NotificationService.java`
- `backend/src/main/java/com/rainier/notification/controller/MeNotificationsController.java`
- `backend/src/main/java/com/rainier/notification/dto/NotificationDetail.java`
- `backend/src/test/java/com/rainier/notification/NotificationServiceTest.java`
- `backend/src/test/java/com/rainier/notification/RiskServicePushIntegrationTest.java`

## 修改清单
- `backend/src/main/java/com/rainier/risk/RiskService.java` — 注入 `NotificationService`；`runAll` 末尾对每条 CRIT finding 调 `notificationService.send`，标记 `@Transactional`（默认含写）
- `backend/src/test/java/com/rainier/product/bootstrap/LegacyProductCategoryCleanupTest.java` — 表数 32 → 33，新增 `rainier_notification` 断言

## 新增表 (DDL)

### rainier_notification
- `id` BIGINT PK
- `user_id` BIGINT NOT NULL
- `title` VARCHAR(256) NOT NULL
- `body` CLOB / LONGTEXT (nullable)
- `level` VARCHAR(16) NOT NULL (INFO/WARN/CRIT)
- `entity_type` VARCHAR(64) (nullable)
- `entity_id` BIGINT (nullable)
- `created_at` TIMESTAMP NOT NULL
- `read_at` TIMESTAMP (nullable)
- BaseEntity 公共列：create_by/create_time/update_by/update_time/del_flag
- INDEX `idx_notif_user` (user_id), `idx_notif_user_read` (user_id, read_at)

## 新增端点
- `GET  /api/me/notifications?onlyUnread={bool}&page=&size=` → `PageResponse<NotificationDetail>`，token-gated
- `POST /api/me/notifications/{id}/read` → `NotificationDetail`（幂等）
- `POST /api/me/notifications/read-all` → `{"updated": N}`

## 测试结果
- backend: **642 tests pass / 0 failed / 0 errors**
- 新增 5 个测试：4 个 `NotificationServiceTest` + 1 个 `RiskServicePushIntegrationTest`
- 受影响：`RiskRulesIntegrationTest`（6 tests）+ `LegacyProductCategoryCleanupTest`（schema 表数）全部通过

## Caveats
- **重复推送**：本版每次 `runAll` 命中 CRIT 都写新通知，无去重窗口；OutOfScope 收敛
- **仅站内通道**：邮件 / IM 走后续 stub
- **userId 解析**：controller 内 `UserRepository.findByLoginName`，未抽 helper；与 `MeInboxController` 形态一致
- **markRead 跨用户**：当 id 存在但 userId 不匹配时返回 404（NotFound），不暴露存在性
