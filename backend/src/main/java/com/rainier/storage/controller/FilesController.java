/* (C) 2026 Rainier — internal use only. */
package com.rainier.storage.controller;

import com.rainier.storage.FileStorage;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;

/**
 * 通用文件上传/下载 endpoint（v0.0.91 D3）。token-optional（与既有 CRM 链路 endpoint 一致），
 * 暂无 ACL — 谁拿到 storedKey 谁能读。
 */
@RestController
@RequestMapping("/api/files")
public class FilesController {

  private final FileStorage fileStorage;

  public FilesController(FileStorage fileStorage) {
    this.fileStorage = fileStorage;
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<FileStorage.PutResult> upload(
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "key", required = false) String key)
      throws IOException {
    if (file == null || file.isEmpty()) {
      return ResponseEntity.badRequest().build();
    }
    String effectiveKey =
        (key == null || key.trim().isEmpty()) ? file.getOriginalFilename() : key;
    String contentType = file.getContentType();
    FileStorage.PutResult result =
        fileStorage.put(effectiveKey, file.getBytes(), contentType);
    return ResponseEntity.ok(result);
  }

  @GetMapping("/**")
  public ResponseEntity<byte[]> download(HttpServletRequest request) {
    String full =
        (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
    // full = "/api/files/<key...>" — strip the prefix to recover the storedKey (which may contain "/")
    String prefix = "/api/files/";
    if (full == null || !full.startsWith(prefix)) {
      return ResponseEntity.notFound().build();
    }
    String key = full.substring(prefix.length());
    try {
      FileStorage.GetResult got = fileStorage.get(key);
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.parseMediaType(got.getContentType()));
      headers.setContentLength(got.getContent().length);
      return new ResponseEntity<byte[]>(got.getContent(), headers, org.springframework.http.HttpStatus.OK);
    } catch (IOException e) {
      return ResponseEntity.notFound().build();
    }
  }
}
