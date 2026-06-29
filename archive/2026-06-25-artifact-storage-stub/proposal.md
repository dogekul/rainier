# D3: artifact-storage-stub

## What
为产出物（OpportunityArtifact 等）的附件存储引入抽象层 `FileStorage`，并提供 LocalFS
默认实现 + S3 占位 stub + 通用 `/api/files` 上传/下载 endpoint。同时给
`OpportunityArtifact` 增加 `storageKey` 字段（nullable，向后兼容现有 `link` 字段）。

## Why
当前所有产出物（OpportunityArtifact、CRM 链路其他附件）只能用 URL 字符串占位，没有真正的
文件落地。D3 引入存储抽象 + 本地 fallback，让前端可以真实上传文件 / 下载附件，后续接
S3/OSS 只换 impl，不动业务代码。

## Scope
- NEW `com.rainier.storage.FileStorage` 接口 + `PutResult` / `GetResult` 值对象
- NEW `com.rainier.storage.LocalFsFileStorage` (`@Primary`, `app.storage.kind=local` 或默认)
- NEW `com.rainier.storage.S3FileStorageStub` (`app.storage.kind=s3`)，put/get 抛 `UnsupportedOperationException`
- NEW `com.rainier.storage.controller.FilesController`：
  - `POST /api/files` (multipart: `file`, optional `key`) → `PutResult` JSON
  - `GET /api/files/{key:.+}` → 字节流 + Content-Type
- `OpportunityArtifact` 增 `storageKey` 字段（nullable）+ getter/setter
- `application.yml` 增 `app.storage.kind=local` / `app.storage.local.root=./.storage/files`
  + `spring.servlet.multipart.max-file-size=10MB` / `max-request-size=10MB`
- `application-test.yml` 增 `app.storage.local.root=./target/.storage-test`
- NEW `changes/2026-06-25-artifact-storage-stub/spec.md`
- 测试：
  - `LocalFsFileStorageTest`（@TempDir + 直接构造 impl，覆盖 put/get/exists/不存在）
  - `FilesControllerIntegrationTest`（mockMvc multipart 上传 + GET 下载字节比对）

## OutOfScope
- 真实 S3/OSS SDK 集成（仅 stub）
- 文件级权限 / ACL（暂全员可读写）
- 病毒扫描 / mime sniff / 大小限额业务校验
- 把现有 OpportunityArtifact.link 数据迁移到 storageKey
- 其他实体（如 Operation/Project 附件）批量加字段（保留接口可复用）

## Decisions
- key 生成：若调用方未传 `key`，由 LocalFs 生成 `yyyyMM/<uuid>-<原文件名>`
- accessUrl：本地实现返回 `/api/files/<storedKey>`（前端拼 origin 即可下载）
- storageType：`LOCAL` / `S3`
- 落盘根目录 `./.storage/files` 用 git-ignored（运行期产物）
- LocalFs `get` 找不到 key → 抛 `IOException`；controller 转 404

## commit
"feat(artifact-storage-stub): D3 文件存储抽象 + LocalFS impl (v0.0.91)"
