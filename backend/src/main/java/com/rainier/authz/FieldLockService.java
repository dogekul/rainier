/* (C) 2026 Rainier — internal use only. */
package com.rainier.authz;

import com.rainier.common.exception.BadRequestException;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business ops for {@link FieldLock} (v0.0.69, A5). lock 是幂等的；unlock 也是幂等的。 */
@Service
@Transactional(readOnly = true)
public class FieldLockService {

  private final FieldLockRepository repo;

  public FieldLockService(FieldLockRepository repo) {
    this.repo = repo;
  }

  /** 登记一个字段锁；若已存在则直接返回现有行（幂等，不抛错也不更新 lockedBy）。 */
  @Transactional
  public FieldLock lock(String entityType, Long entityId, String fieldName, String lockedBy) {
    requireNonBlank(entityType, "entityType");
    if (entityId == null) {
      throw new BadRequestException("entityId is required");
    }
    requireNonBlank(fieldName, "fieldName");
    requireNonBlank(lockedBy, "lockedBy");

    Optional<FieldLock> existing =
        repo.findByEntityTypeAndEntityIdAndFieldName(entityType, entityId, fieldName);
    if (existing.isPresent()) {
      return existing.get();
    }
    FieldLock l = new FieldLock();
    l.setEntityType(entityType);
    l.setEntityId(entityId);
    l.setFieldName(fieldName);
    l.setLockedBy(lockedBy);
    return repo.saveAndFlush(l);
  }

  /** 解除字段锁；不存在不抛错（幂等）。 */
  @Transactional
  public void unlock(String entityType, Long entityId, String fieldName) {
    requireNonBlank(entityType, "entityType");
    if (entityId == null) {
      throw new BadRequestException("entityId is required");
    }
    requireNonBlank(fieldName, "fieldName");
    repo.deleteByEntityTypeAndEntityIdAndFieldName(entityType, entityId, fieldName);
  }

  public List<FieldLock> listFor(String entityType, Long entityId) {
    requireNonBlank(entityType, "entityType");
    if (entityId == null) {
      throw new BadRequestException("entityId is required");
    }
    return repo.findByEntityTypeAndEntityId(entityType, entityId);
  }

  /** Whether (entityType, entityId, fieldName) is locked — exposed for future A6/A7 write path. */
  public boolean isLocked(String entityType, Long entityId, String fieldName) {
    if (entityType == null || entityId == null || fieldName == null) {
      return false;
    }
    return repo.existsByEntityTypeAndEntityIdAndFieldName(entityType, entityId, fieldName);
  }

  private static void requireNonBlank(String value, String name) {
    if (value == null || value.trim().isEmpty()) {
      throw new BadRequestException(name + " is required");
    }
  }
}
