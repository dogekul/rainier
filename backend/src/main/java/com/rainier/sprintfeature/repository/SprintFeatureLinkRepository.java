/* (C) 2026 Rainier — internal use only. */
package com.rainier.sprintfeature.repository;

import com.rainier.sprintfeature.domain.SprintFeatureLink;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/** Repository for {@link SprintFeatureLink}. Hard delete; del_flag unused. */
@Repository
public interface SprintFeatureLinkRepository
    extends JpaRepository<SprintFeatureLink, Long>,
        JpaSpecificationExecutor<SprintFeatureLink> {

  boolean existsBySprintIdAndFeatureId(Long sprintId, Long featureId);

  List<SprintFeatureLink> findBySprintId(Long sprintId);

  List<SprintFeatureLink> findByFeatureId(Long featureId);

  long countBySprintId(Long sprintId);

  long countByFeatureId(Long featureId);
}
