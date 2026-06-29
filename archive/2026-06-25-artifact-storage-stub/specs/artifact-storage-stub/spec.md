# Capability: artifact-storage-stub

> NEW capability (v0.0.91-artifact-storage-stub, 2026-06-25) — 为产出物附件引入存储抽象
> `FileStorage` 接口 + `LocalFsFileStorage` 默认实现 + `S3FileStorageStub` 占位 + 通用
> `/api/files` 上传/下载 endpoint。**仍无权限控制 / 真实 S3 集成 — 仅切换 impl 的接口**。

## ADDED Requirements

### Requirement: FileStorage 接口契约

系统 SHALL 提供 `FileStorage` 接口，定义三个方法：

- `PutResult put(String key, byte[] content, String contentType)` — 写入，`key` 为 null 时由
  实现自动生成；返回含 `storageType` / `storedKey` / `accessUrl` 的 `PutResult`
- `GetResult get(String key)` — 读取，返回含 `content` / `contentType` 的 `GetResult`；不存在抛 `IOException`
- `boolean exists(String key)` — 存在性检查；不抛异常

#### Scenario: 默认装配 LocalFsFileStorage

- **GIVEN** `app.storage.kind` 未配置（或 `=local`）
- **WHEN** Spring 容器启动
- **THEN** `FileStorage` bean 解析为 `LocalFsFileStorage`

#### Scenario: 切换 s3 后装配 stub

- **GIVEN** `app.storage.kind=s3`
- **WHEN** Spring 容器启动
- **THEN** `FileStorage` bean 解析为 `S3FileStorageStub`，且调用 `put` 抛 `UnsupportedOperationException`

### Requirement: LocalFsFileStorage 落盘读写

`LocalFsFileStorage` SHALL 将文件写入 `app.storage.local.root` 目录下；`put` 时若调用方
未提供 `key`，SHALL 生成 `yyyyMM/<uuid>-<原 key 或 "file">` 形式的 storedKey；
`accessUrl` SHALL 为 `/api/files/<storedKey>`；`storageType` SHALL 为 `"LOCAL"`。

#### Scenario: put 后 exists/get 内容一致

- **GIVEN** LocalFsFileStorage 指向临时目录
- **WHEN** put(null, "hello".getBytes(), "text/plain")
- **THEN** PutResult.storedKey 非空
- **AND** exists(storedKey) 为 true
- **AND** get(storedKey).content 等于 "hello".getBytes()
- **AND** get(storedKey).contentType 等于 "text/plain"

#### Scenario: get 不存在的 key 抛 IOException

- **GIVEN** LocalFsFileStorage 指向临时目录
- **WHEN** get("does/not/exist.bin")
- **THEN** 抛 IOException

### Requirement: POST /api/files 上传

系统 SHALL 暴露 `POST /api/files`（multipart）endpoint，接收 `file` 字段（必填）+ 可选
`key` 字段；返回 `PutResult` JSON（storageType / storedKey / accessUrl）。

#### Scenario: 上传成功返回 200 + PutResult

- **GIVEN** 一个 multipart 请求带 file="hi.txt" 内容="bye"
- **WHEN** POST /api/files
- **THEN** HTTP 200，返回 JSON 含 `storageType="LOCAL"` / 非空 `storedKey` / `accessUrl` 以 `/api/files/` 开头

### Requirement: GET /api/files/{key} 下载

系统 SHALL 暴露 `GET /api/files/{key:.+}` endpoint，返回文件字节流 + 上传时记录的
Content-Type；key 不存在 SHALL 返回 HTTP 404。

#### Scenario: 上传后下载字节一致

- **GIVEN** 先 POST /api/files 上传 "bye"
- **WHEN** GET 返回的 accessUrl
- **THEN** HTTP 200，响应体字节等于 "bye"

#### Scenario: 不存在的 key 返回 404

- **WHEN** GET /api/files/does/not/exist.bin
- **THEN** HTTP 404

### Requirement: OpportunityArtifact.storageKey 字段

`OpportunityArtifact` SHALL 新增 `storageKey` 字段（VARCHAR(500), nullable），与既有
`link` 字段并存：新流程上传文件后写 `storageKey`，老的 URL 字段保留向后兼容；不强制
迁移现有数据。

#### Scenario: 新字段默认 null 向后兼容

- **GIVEN** v0.0.90 创建的 artifact 记录（只有 link, 没有 storageKey）
- **WHEN** GET /api/opportunity-artifacts/{id}
- **THEN** 仍可正常返回，storageKey 为 null
