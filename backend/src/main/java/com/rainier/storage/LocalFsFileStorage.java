/* (C) 2026 Rainier — internal use only. */
package com.rainier.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 默认 {@link FileStorage} 实现：把文件直接落到本地磁盘。dev/test 都用这个 — 路径由
 * {@code app.storage.local.root} 控制（dev 默认 {@code ./.storage/files}）。
 *
 * <p>{@code @Primary} + {@code matchIfMissing=true}：只要没显式 set 为 s3 就是它生效。
 *
 * <p>storedKey 形如 {@code 202606/<uuid>-<原文件名>}（按年月分桶 + uuid 防重）。content-type 存
 * 同名 {@code .meta} 边车文件（一行纯文本），下载时回填到响应头。
 */
@Component
@Primary
@ConditionalOnProperty(
    prefix = "app.storage",
    name = "kind",
    havingValue = "local",
    matchIfMissing = true)
public class LocalFsFileStorage implements FileStorage {

  private static final DateTimeFormatter MONTH_BUCKET = DateTimeFormatter.ofPattern("yyyyMM");
  private static final String DEFAULT_CT = "application/octet-stream";

  private final Path root;

  public LocalFsFileStorage(
      @Value("${app.storage.local.root:./.storage/files}") String root) throws IOException {
    this.root = Paths.get(root).toAbsolutePath().normalize();
    Files.createDirectories(this.root);
  }

  @Override
  public PutResult put(String key, byte[] content, String contentType) throws IOException {
    if (content == null) {
      throw new IOException("content is null");
    }
    String storedKey = (key == null || key.trim().isEmpty()) ? generateKey("file") : sanitize(key);
    Path target = resolveSafe(storedKey);
    Files.createDirectories(target.getParent());
    Files.write(
        target,
        content,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.WRITE);
    String ct =
        (contentType == null || contentType.trim().isEmpty()) ? DEFAULT_CT : contentType.trim();
    Files.write(
        sidecar(target),
        ct.getBytes(StandardCharsets.UTF_8),
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING);
    return new PutResult("LOCAL", storedKey, "/api/files/" + storedKey);
  }

  @Override
  public GetResult get(String key) throws IOException {
    if (key == null || key.trim().isEmpty()) {
      throw new IOException("key is blank");
    }
    Path target = resolveSafe(sanitize(key));
    if (!Files.isRegularFile(target)) {
      throw new IOException("file not found: " + key);
    }
    byte[] bytes = Files.readAllBytes(target);
    String ct = DEFAULT_CT;
    Path meta = sidecar(target);
    if (Files.isRegularFile(meta)) {
      String line = new String(Files.readAllBytes(meta), StandardCharsets.UTF_8).trim();
      if (!line.isEmpty()) {
        ct = line;
      }
    }
    return new GetResult(bytes, ct);
  }

  @Override
  public boolean exists(String key) {
    if (key == null || key.trim().isEmpty()) {
      return false;
    }
    try {
      return Files.isRegularFile(resolveSafe(sanitize(key)));
    } catch (IOException e) {
      return false;
    }
  }

  /** Defends against {@code ../} traversal — resolved path MUST stay inside {@code root}. */
  private Path resolveSafe(String storedKey) throws IOException {
    Path candidate = root.resolve(storedKey).toAbsolutePath().normalize();
    if (!candidate.startsWith(root)) {
      throw new IOException("path escapes storage root: " + storedKey);
    }
    return candidate;
  }

  private Path sidecar(Path target) {
    return target.resolveSibling(target.getFileName().toString() + ".meta");
  }

  private static String sanitize(String key) {
    // Strip leading slashes / windows drive letters; collapse backslashes.
    String k = key.replace('\\', '/').trim();
    while (k.startsWith("/")) {
      k = k.substring(1);
    }
    return k;
  }

  private static String generateKey(String originalName) {
    String bucket = LocalDate.now().format(MONTH_BUCKET);
    String safeName = (originalName == null ? "file" : originalName).replace('/', '_');
    return bucket + "/" + UUID.randomUUID() + "-" + safeName;
  }
}
