/* (C) 2026 Rainier — internal use only. */
package com.rainier.auditlog.repository;

import com.rainier.auditlog.domain.AuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/** Repository for {@link AuditLog}. Append-only; no delete in the application flow. */
@Repository
public interface AuditLogRepository
    extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

  /** v0.0.41 compliance — audit row counts grouped by action, busiest first. row[0]=action, row[1]=count. */
  @Query("SELECT a.action, COUNT(a) FROM AuditLog a GROUP BY a.action ORDER BY COUNT(a) DESC")
  List<Object[]> countGroupedByAction();

  /** v0.0.41 compliance — audit row counts grouped by entityType, busiest first. row[0]=type, row[1]=count. */
  @Query("SELECT a.entityType, COUNT(a) FROM AuditLog a GROUP BY a.entityType ORDER BY COUNT(a) DESC")
  List<Object[]> countGroupedByEntityType();

  /** v0.0.41 compliance — the 20 most recent audit rows for the dashboard activity feed. */
  List<AuditLog> findTop20ByOrderByCreateTimeDescIdDesc();
}
