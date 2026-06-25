/* (C) 2026 Rainier — internal use only. */
package com.rainier.userorganization.repository;

import com.rainier.userorganization.domain.UserOrganization;
import com.rainier.userorganization.domain.UserOrgRole;
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

  // v0.0.24 self-scoped team endpoints (active assignment = leftAt IS NULL).
  List<UserOrganization> findByUserIdAndRoleAndLeftAtIsNull(Long userId, UserOrgRole role);

  boolean existsByUserIdAndOrganizationIdAndRoleAndLeftAtIsNull(
      Long userId, Long organizationId, UserOrgRole role);

  List<UserOrganization> findByOrganizationIdAndLeftAtIsNull(Long organizationId);

  /** v0.0.40 me-profile — all active (leftAt IS NULL) org memberships of a user. */
  List<UserOrganization> findByUserIdAndLeftAtIsNull(Long userId);

  /**
   * v0.0.64 — owner 主组织（用于 Project 创建时 organizationId 默认注入）。
   *
   * <p>Filters: is_primary=1 AND left_at IS NULL（即活跃主组织）。
   */
  List<UserOrganization> findByUserIdAndIsPrimaryTrueAndLeftAtIsNull(Long userId);

  /** Demote all primary assignments for a user except a given keepId. */
  @Modifying
  @Query(
      "UPDATE UserOrganization uo SET uo.isPrimary = false "
          + "WHERE uo.userId = :userId AND uo.isPrimary = true AND uo.id <> :keepId")
  int demoteOthersForUser(@Param("userId") Long userId, @Param("keepId") Long keepId);
}
