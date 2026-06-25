/* (C) 2026 Rainier — internal use only. */
package com.rainier.authz;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * v0.0.69 (A5): field-locks HTTP — all-users (everyone can lock their own fields, no admin gate).
 * 后续 AI 写路径会查 {@link FieldLockService#isLocked} 决定是否拒绝写入。
 */
@RestController
@RequestMapping("/api/field-locks")
public class FieldLockController {

  private final FieldLockService service;

  public FieldLockController(FieldLockService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<FieldLockDto> create(@RequestBody FieldLockCreateRequest req) {
    FieldLock l =
        service.lock(req.getEntityType(), req.getEntityId(), req.getFieldName(), req.getLockedBy());
    FieldLockDto dto = FieldLockDto.from(l);
    return ResponseEntity.created(URI.create("/api/field-locks/" + dto.getId())).body(dto);
  }

  @GetMapping
  public List<FieldLockDto> list(
      @RequestParam String entityType, @RequestParam Long entityId) {
    return service.listFor(entityType, entityId).stream()
        .map(FieldLockDto::from)
        .collect(Collectors.toList());
  }

  @DeleteMapping
  public ResponseEntity<Void> delete(
      @RequestParam String entityType,
      @RequestParam Long entityId,
      @RequestParam String fieldName) {
    service.unlock(entityType, entityId, fieldName);
    return ResponseEntity.noContent().build();
  }
}
