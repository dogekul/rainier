/* (C) 2026 Rainier — internal use only. */
package com.rainier.requirementfeature.repository;

import com.rainier.requirementfeature.domain.RequirementFeatureLink;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for {@link RequirementFeatureLink}. Hard delete; del_flag unused. */
@Repository
public interface RequirementFeatureLinkRepository
    extends JpaRepository<RequirementFeatureLink, Long> {

  boolean existsByRequirementIdAndFeatureId(Long requirementId, Long featureId);

  List<RequirementFeatureLink> findByRequirementId(Long requirementId);

  List<RequirementFeatureLink> findByFeatureId(Long featureId);

  List<RequirementFeatureLink> findByRequirementIdIn(Collection<Long> requirementIds);

  List<RequirementFeatureLink> findByFeatureIdIn(Collection<Long> featureIds);
}
