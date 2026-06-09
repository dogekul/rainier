/* (C) 2026 Rainier — internal use only. */
package com.rainier.feature.repository;

import com.rainier.feature.domain.Feature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface FeatureRepository
    extends JpaRepository<Feature, Long>, JpaSpecificationExecutor<Feature> {

  boolean existsByCode(String code);

  /** Used by ProductModuleService.delete (M09) — only counts del_flag=0 rows (via @Where). */
  long countByModuleId(Long moduleId);
}
