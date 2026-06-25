/* (C) 2026 Rainier — internal use only. */
package com.rainier.story.repository;

import com.rainier.story.domain.Story;
import java.util.Collection;
import java.util.List;
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

  /**
   * v0.0.39: stories awaiting review by a given user (the "我的待评审队列" read-model). {@code @Where(del_flag
   * = 0)} auto-applies, so soft-deleted stories are excluded.
   */
  List<Story> findByReviewerUserIdAndReviewStatus(Long reviewerUserId, String reviewStatus);

  /** v0.0.70 risk-radar — stories scoped to a portfolio of projects (del_flag=0 via @Where). */
  List<Story> findByProjectIdIn(Collection<Long> projectIds);
}
