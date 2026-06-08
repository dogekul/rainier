/* (C) 2026 Rainier — internal use only. */
package com.rainier.sprint.repository;

import com.rainier.sprint.domain.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/** Repository for {@link Sprint}. */
@Repository
public interface SprintRepository
    extends JpaRepository<Sprint, Long>, JpaSpecificationExecutor<Sprint> {

  boolean existsByCode(String code);

  long countByRequirementId(Long requirementId);

  long countByOwnerUserId(Long ownerUserId);
}
