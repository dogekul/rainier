/* (C) 2026 Rainier — internal use only. */
package com.rainier.userrole.repository;

import com.rainier.userrole.domain.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link UserRole}. Hard delete; del_flag unused.
 *
 * <p>NULL safety: MySQL UNIQUE allows multiple NULL rows on a nullable column, so service layer
 * uses {@link #existsByUserIdAndRoleIdAndProjectIdIsNull} when {@code projectId == null}.
 */
@Repository
public interface UserRoleRepository
    extends JpaRepository<UserRole, Long>, JpaSpecificationExecutor<UserRole> {

  long countByRoleId(Long roleId);

  long countByUserId(Long userId);

  boolean existsByUserIdAndRoleIdAndProjectId(Long userId, Long roleId, Long projectId);

  boolean existsByUserIdAndRoleIdAndProjectIdIsNull(Long userId, Long roleId);
}
