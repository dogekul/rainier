/* (C) 2026 Rainier — internal use only. */
package com.rainier.aiworklog.repository;

import com.rainier.aiworklog.domain.AiWorkLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/** Repository for {@link AiWorkLog} (v0.0.43). */
@Repository
public interface AiWorkLogRepository
    extends JpaRepository<AiWorkLog, Long>, JpaSpecificationExecutor<AiWorkLog> {

  long countByStatus(String status);
}
