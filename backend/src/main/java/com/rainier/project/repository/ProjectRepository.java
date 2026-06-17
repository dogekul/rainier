/* (C) 2026 Rainier — internal use only. */
package com.rainier.project.repository;

import com.rainier.project.domain.Project;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/** Repository for {@link Project}. */
@Repository
public interface ProjectRepository
    extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {

  boolean existsByCode(String code);

  // v0.0.28 portfolio scope resolution.
  List<Project> findByOwnerUserId(Long ownerUserId);

  List<Project> findByOrganizationIdIn(Collection<Long> organizationIds);
}
