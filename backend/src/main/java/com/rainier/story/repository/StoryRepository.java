/* (C) 2026 Rainier — internal use only. */
package com.rainier.story.repository;

import com.rainier.story.domain.Story;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/** Repository for {@link Story}. */
@Repository
public interface StoryRepository
    extends JpaRepository<Story, Long>, JpaSpecificationExecutor<Story> {

  boolean existsByCode(String code);

  /**
   * v0.0.10: replaces v0.0.9 countByRequirementId — Story belongs to Sprint, not directly to
   * Requirement.
   */
  long countBySprintId(Long sprintId);

  long countByOwnerUserId(Long ownerUserId);
}
