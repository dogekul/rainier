/* (C) 2026 Rainier — internal use only. */
package com.rainier.opportunity.repository;

import com.rainier.opportunity.domain.StageActivity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StageActivityRepository extends JpaRepository<StageActivity, Long> {

  /** All activities for an opportunity within a stage, oldest-first (creation order). */
  List<StageActivity> findByOpportunityIdAndStageCodeOrderByIdAsc(
      Long opportunityId, String stageCode);
}
