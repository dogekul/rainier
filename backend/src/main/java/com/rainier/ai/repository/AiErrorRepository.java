/* (C) 2026 Rainier — internal use only. */
package com.rainier.ai.repository;

import com.rainier.ai.domain.AiError;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/** Repository for {@link AiError} (v0.0.68, A4). */
@Repository
public interface AiErrorRepository
    extends JpaRepository<AiError, Long>, JpaSpecificationExecutor<AiError> {

  long countByStatus(String status);
}
