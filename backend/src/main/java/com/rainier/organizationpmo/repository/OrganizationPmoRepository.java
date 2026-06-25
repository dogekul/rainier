/* (C) 2026 Rainier — internal use only. */
package com.rainier.organizationpmo.repository;

import com.rainier.organizationpmo.domain.OrganizationPmo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationPmoRepository
    extends JpaRepository<OrganizationPmo, Long>, JpaSpecificationExecutor<OrganizationPmo> {

  List<OrganizationPmo> findByOrganizationIdOrderById(Long organizationId);

  List<OrganizationPmo> findByOrganizationIdInOrderById(List<Long> organizationIds);

  Optional<OrganizationPmo> findByOrganizationIdAndUserId(Long organizationId, Long userId);

  boolean existsByOrganizationIdAndUserId(Long organizationId, Long userId);
}
