/* (C) 2026 Rainier — internal use only. */
package com.rainier.projectimplementation.repository;

import com.rainier.projectimplementation.domain.ProjectImplementation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for {@link ProjectImplementation} (v0.0.89). */
@Repository
public interface ProjectImplementationRepository
    extends JpaRepository<ProjectImplementation, Long> {

  Optional<ProjectImplementation> findByProjectId(Long projectId);
}
