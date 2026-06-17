/* (C) 2026 Rainier — internal use only. */
package com.rainier.milestone.repository;

import com.rainier.milestone.domain.Milestone;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/** Repository for {@link Milestone}. */
@Repository
public interface MilestoneRepository
    extends JpaRepository<Milestone, Long>, JpaSpecificationExecutor<Milestone> {

  /** Service-level composite uniqueness — soft-deleted rows excluded by {@code @Where}. */
  boolean existsByProjectIdAndCode(Long projectId, String code);

  /** Active milestones of a project — used by the project-delete cascade soft-delete. */
  List<Milestone> findByProjectId(Long projectId);

  /** v0.0.28 portfolio fan-out — active milestones across a set of projects. */
  List<Milestone> findByProjectIdIn(Collection<Long> projectIds);

  long countByProjectId(Long projectId);
}
