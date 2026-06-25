/* (C) 2026 Rainier — internal use only. */
package com.rainier.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * S3/OSS 占位 stub（v0.0.91 D3）。仅当 {@code app.storage.kind=s3} 时装配；put/get 一律抛
 * {@link UnsupportedOperationException}，等待后续接 SDK 时替换。
 */
@Component
@ConditionalOnProperty(prefix = "app.storage", name = "kind", havingValue = "s3")
public class S3FileStorageStub implements FileStorage {

  @Override
  public PutResult put(String key, byte[] content, String contentType) {
    throw new UnsupportedOperationException("S3FileStorageStub — not yet implemented");
  }

  @Override
  public GetResult get(String key) {
    throw new UnsupportedOperationException("S3FileStorageStub — not yet implemented");
  }

  @Override
  public boolean exists(String key) {
    return false;
  }
}
