/* (C) 2026 Rainier — internal use only. */
package com.rainier.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Pure unit test for {@link LocalFsFileStorage} — no Spring context; ctor accepts the root dir
 * directly so we can point at a {@code @TempDir}.
 */
class LocalFsFileStorageTest {

  @Test
  void putThenExistsAndGetReturnsSameBytes(@TempDir Path tmp) throws IOException {
    LocalFsFileStorage storage = new LocalFsFileStorage(tmp.toString());
    byte[] payload = "hello".getBytes(StandardCharsets.UTF_8);

    FileStorage.PutResult put = storage.put(null, payload, "text/plain");

    assertThat(put.getStorageType()).isEqualTo("LOCAL");
    assertThat(put.getStoredKey()).isNotBlank();
    assertThat(put.getAccessUrl()).isEqualTo("/api/files/" + put.getStoredKey());
    assertThat(storage.exists(put.getStoredKey())).isTrue();

    FileStorage.GetResult got = storage.get(put.getStoredKey());
    assertThat(got.getContent()).isEqualTo(payload);
    assertThat(got.getContentType()).isEqualTo("text/plain");
  }

  @Test
  void putWithExplicitKeyHonoursIt(@TempDir Path tmp) throws IOException {
    LocalFsFileStorage storage = new LocalFsFileStorage(tmp.toString());
    FileStorage.PutResult put =
        storage.put("custom/path/note.md", "x".getBytes(StandardCharsets.UTF_8), "text/markdown");
    assertThat(put.getStoredKey()).isEqualTo("custom/path/note.md");
    assertThat(storage.exists("custom/path/note.md")).isTrue();
  }

  @Test
  void getMissingKeyThrows(@TempDir Path tmp) throws IOException {
    LocalFsFileStorage storage = new LocalFsFileStorage(tmp.toString());
    assertThat(storage.exists("does/not/exist.bin")).isFalse();
    assertThatThrownBy(() -> storage.get("does/not/exist.bin")).isInstanceOf(IOException.class);
  }

  @Test
  void putNullContentTypeFallsBackToOctetStream(@TempDir Path tmp) throws IOException {
    LocalFsFileStorage storage = new LocalFsFileStorage(tmp.toString());
    FileStorage.PutResult put = storage.put(null, new byte[] {1, 2, 3}, null);
    FileStorage.GetResult got = storage.get(put.getStoredKey());
    assertThat(got.getContentType()).isEqualTo("application/octet-stream");
  }

  @Test
  void pathTraversalIsRejected(@TempDir Path tmp) throws IOException {
    LocalFsFileStorage storage = new LocalFsFileStorage(tmp.toString());
    // sanitize strips leading slashes; .. resolves outside root → IOException.
    assertThatThrownBy(() -> storage.put("../escape.txt", new byte[] {0}, "text/plain"))
        .isInstanceOf(IOException.class);
  }
}
