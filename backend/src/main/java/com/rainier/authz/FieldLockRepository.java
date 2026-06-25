/* (C) 2026 Rainier — internal use only. */
package com.rainier.authz;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for {@link FieldLock} (v0.0.69, A5). */
@Repository
public interface FieldLockRepository extends JpaRepository<FieldLock, Long> {

  List<FieldLock> findByEntityTypeAndEntityId(String entityType, Long entityId);

  boolean existsByEntityTypeAndEntityIdAndFieldName(
      String entityType, Long entityId, String fieldName);

  Optional<FieldLock> findByEntityTypeAndEntityIdAndFieldName(
      String entityType, Long entityId, String fieldName);

  long deleteByEntityTypeAndEntityIdAndFieldName(String entityType, Long entityId, String fieldName);
}
