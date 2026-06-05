/* (C) 2026 Rainier — internal use only. */
package com.rainier.userorganization.repository;

import com.rainier.userorganization.domain.UserOrganization;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repository for {@link UserOrganization}. */
@Repository
public interface UserOrganizationRepository
    extends JpaRepository<UserOrganization, Long>, JpaSpecificationExecutor<UserOrganization> {

  boolean existsByUserIdAndOrganizationId(Long userId, Long organizationId);

  List<UserOrganization> findByUserIdAndIsPrimaryTrue(Long userId);

  long countByOrganizationIdAndLeftAtIsNull(Long organizationId);

  long countByUserIdAndLeftAtIsNull(Long userId);

  /** Demote all primary assignments for a user except a given keepId. */
  @Modifying
  @Query(
      "UPDATE UserOrganization uo SET uo.isPrimary = false "
          + "WHERE uo.userId = :userId AND uo.isPrimary = true AND uo.id <> :keepId")
  int demoteOthersForUser(@Param("userId") Long userId, @Param("keepId") Long keepId);
}
