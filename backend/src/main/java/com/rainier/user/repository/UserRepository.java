/* (C) 2026 Rainier — internal use only. */
package com.rainier.user.repository;

import com.rainier.user.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/** Repository for {@link User}. Derived queries inherit {@code @Where("del_flag = 0")}. */
@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

  boolean existsByLoginName(String loginName);

  /** v0.0.41 compliance — disabled (non-soft-deleted) users, for residual-permission reconciliation. */
  List<User> findByEnabledFalse();

  /** v0.0.18: resolve the current user from the JWT subject (loginName) for GET /api/auth/me. */
  Optional<User> findByLoginName(String loginName);

  boolean existsByCode(String code);

  boolean existsByEmailAddress(String emailAddress);

  /** v0.0.7: used by PositionService.delete to enforce FK protection. */
  long countByPositionId(Long positionId);
}
