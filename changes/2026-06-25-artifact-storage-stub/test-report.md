# Test Report — artifact-storage-stub (D3, v0.0.91)

## 新增类

- `backend/src/main/java/com/rainier/storage/FileStorage.java`
- `backend/src/main/java/com/rainier/storage/LocalFsFileStorage.java`
- `backend/src/main/java/com/rainier/storage/S3FileStorageStub.java`
- `backend/src/main/java/com/rainier/storage/controller/FilesController.java`

## 修改

- `backend/src/main/java/com/rainier/opportunity/domain/OpportunityArtifact.java` — 新增
  `storageKey` 字段（VARCHAR(500), nullable, 向后兼容）+ getter/setter
- `backend/src/main/resources/application.yml` — `spring.servlet.multipart.{max-file-size,
  max-request-size}=10MB` + `app.storage.kind=local` / `app.storage.local.root=./.storage/files`
- `backend/src/test/resources/application-test.yml` — `app.storage.local.root=./target/.storage-test`
- `.gitignore` — 忽略 `.storage/` / `backend/.storage/` / `backend/target/.storage-test/`

## 新增测试

- `backend/src/test/java/com/rainier/storage/LocalFsFileStorageTest.java` (5 cases)
  - put → exists → get 字节一致 (含 content-type 回填)
  - 显式 key 被尊重
  - get 不存在 key 抛 IOException
  - null content-type 默认 application/octet-stream
  - 路径穿越 (`../`) 被拒
- `backend/src/test/java/com/rainier/storage/controller/FilesControllerIntegrationTest.java` (3 cases)
  - multipart 上传 → 用 accessUrl 下载，字节一致 + content-type 正确
  - GET 不存在 key → 404
  - POST 空 file → 400

## 测试结果

```
Tests run: 817, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

较上一 sub-change (809) 新增 8 tests，全部通过。

## 数据库 DDL

- `rainier_opportunity_artifact` 新增 `storage_key VARCHAR(500) NULL`，nullable + JPA `ddl-auto=update`
  自动加列；老数据 storageKey=null，与 v0.0.90 完全向后兼容

## Caveats

- 没有 ACL — 任何人拿到 storedKey 都能 GET（与 token-optional endpoint 一致）；权限留给后续
- S3FileStorageStub 仅占位；put/get 抛 `UnsupportedOperationException`，接 SDK 时换 impl
- LocalFs 落到 `./.storage/files`，是 Docker 容器内路径；要持久化需 mount volume（本次未改 compose）
- `OpportunityArtifact.link` 字段保留，未做数据迁移；新前端走 storageKey，老 URL 链路不受影响
- 其他实体（Operation 附件、Project 附件等）暂未加 storageKey；接口已抽象，复用只是加字段
- `LocalFsFileStorage` 的 `@ConditionalOnProperty(matchIfMissing=true)` 让默认 profile 装配它；
  S3 stub 仅当显式 `app.storage.kind=s3` 时启用
- multipart 上限 10MB（含 request 总大小），超出 Spring 抛 `MaxUploadSizeExceededException`，
  此 sub-change 未自定义 ExceptionHandler，按默认 500/413 行为返回
- FilesController 的 download 用 `HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE` 解析全
  路径（含 `/`），支持 yyyyMM 分桶子目录
