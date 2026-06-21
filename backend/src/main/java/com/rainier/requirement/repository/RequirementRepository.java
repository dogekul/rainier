/* (C) 2026 Rainier — internal use only. */
package com.rainier.requirement.repository;

import com.rainier.requirement.domain.Requirement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/** Repository for {@link Requirement}. */
@Repository
public interface RequirementRepository
    extends JpaRepository<Requirement, Long>, JpaSpecificationExecutor<Requirement> {

  boolean existsByCode(String code);

  /** v0.0.42 me-inbox — requirements owned by a user (del_flag=0 via @Where). */
  List<Requirement> findByOwnerUserId(Long ownerUserId);

  /** v0.0.8: used by ProjectService.delete to enforce FK protection. */
  long countByProjectId(Long projectId);

  /** Hard delete for test cleanup — see DemandRepository.hardDeleteAll() rationale. */
  @org.springframework.data.jpa.repository.Modifying
  @org.springframework.data.jpa.repository.Query(
      value = "DELETE FROM rainier_requirement",
      nativeQuery = true)
  void hardDeleteAll();
}
