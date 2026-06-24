/* (C) 2026 Rainier — internal use only. */
package com.rainier.operation.repository;

import com.rainier.operation.domain.Operation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/** Repository for {@link Operation} (v0.0.44). */
@Repository
public interface OperationRepository
    extends JpaRepository<Operation, Long>, JpaSpecificationExecutor<Operation> {

  /** v0.0.58 — find the Operation auto-created from a given opportunity (idempotency check). */
  Optional<Operation> findFirstByOpportunityId(Long opportunityId);
}
