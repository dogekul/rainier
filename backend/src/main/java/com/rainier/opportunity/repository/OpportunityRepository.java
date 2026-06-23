/* (C) 2026 Rainier — internal use only. */
package com.rainier.opportunity.repository;

import com.rainier.opportunity.domain.Opportunity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/** Repository for {@link Opportunity} (v0.0.44). */
@Repository
public interface OpportunityRepository
    extends JpaRepository<Opportunity, Long>, JpaSpecificationExecutor<Opportunity> {

  long countByStatus(String status);
}
