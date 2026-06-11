/* (C) 2026 Rainier — internal use only. */
package com.rainier.auditlog.repository;

import com.rainier.auditlog.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/** Repository for {@link AuditLog}. Append-only; no delete in the application flow. */
@Repository
public interface AuditLogRepository
    extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {}
