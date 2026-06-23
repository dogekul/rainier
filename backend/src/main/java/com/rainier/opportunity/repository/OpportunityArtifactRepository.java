/* (C) 2026 Rainier — internal use only. */
package com.rainier.opportunity.repository;

import com.rainier.opportunity.domain.OpportunityArtifact;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpportunityArtifactRepository extends JpaRepository<OpportunityArtifact, Long> {

  /** All artifacts for an opportunity, most-recent first. */
  List<OpportunityArtifact> findByOpportunityIdOrderByIdDesc(Long opportunityId);
}
