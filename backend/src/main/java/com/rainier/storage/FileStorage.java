/* (C) 2026 Rainier — internal use only. */
package com.rainier.storage;

import java.io.IOException;

/**
 * 产出物附件存储抽象（v0.0.91 D3）。LocalFsFileStorage 是默认实现；S3FileStorageStub 占位，
 * 后续接 SDK 时换 impl，不动业务代码。
 *
 * <p>暂无 ACL / 权限控制 — 全员可读写（与 token-optional endpoint 保持一致）。
 */
public interface FileStorage {

  /**
   * 写入文件。若 {@code key} 为 null/blank，由实现自动生成（含时间分桶 + UUID 防重）。
   *
   * @param key 调用方可选指定的 storedKey；null/blank 时自动生成
   * @param content 文件字节
   * @param contentType MIME，例如 "image/png"；null/blank 时实现写 "application/octet-stream"
   * @return 写入结果（含 storageType / storedKey / accessUrl）
   */
  PutResult put(String key, byte[] content, String contentType) throws IOException;

  /** 读取文件。key 不存在 SHALL 抛 {@link IOException}。 */
  GetResult get(String key) throws IOException;

  /** 存在性检查，不抛异常。 */
  boolean exists(String key);

  /** put 返回值。 */
  final class PutResult {
    private final String storageType;
    private final String storedKey;
    private final String accessUrl;

    public PutResult(String storageType, String storedKey, String accessUrl) {
      this.storageType = storageType;
      this.storedKey = storedKey;
      this.accessUrl = accessUrl;
    }

    public String getStorageType() {
      return storageType;
    }

    public String getStoredKey() {
      return storedKey;
    }

    public String getAccessUrl() {
      return accessUrl;
    }
  }

  /** get 返回值。 */
  final class GetResult {
    private final byte[] content;
    private final String contentType;

    public GetResult(byte[] content, String contentType) {
      this.content = content;
      this.contentType = contentType;
    }

    public byte[] getContent() {
      return content;
    }

    public String getContentType() {
      return contentType;
    }
  }
}
