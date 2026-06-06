/* (C) 2026 Rainier — internal use only. */
package com.rainier.demandrequirement.repository;

import com.rainier.demandrequirement.domain.DemandRequirementLink;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/** Repository for {@link DemandRequirementLink}. Hard delete; del_flag unused. */
@Repository
public interface DemandRequirementLinkRepository
    extends JpaRepository<DemandRequirementLink, Long>,
        JpaSpecificationExecutor<DemandRequirementLink> {

  long countByDemandId(Long demandId);

  long countByRequirementId(Long requirementId);

  boolean existsByDemandIdAndRequirementId(Long demandId, Long requirementId);

  List<DemandRequirementLink> findByDemandId(Long demandId);

  List<DemandRequirementLink> findByRequirementId(Long requirementId);
}
