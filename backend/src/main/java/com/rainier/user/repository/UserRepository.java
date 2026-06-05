/* (C) 2026 Rainier — internal use only. */
package com.rainier.user.repository;

import com.rainier.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/** Repository for {@link User}. Derived queries inherit {@code @Where("del_flag = 0")}. */
@Repository
public interface UserRepository
    extends JpaRepository<User, String>, JpaSpecificationExecutor<User> {

  boolean existsByLoginName(String loginName);

  boolean existsByCode(String code);

  boolean existsByEmailAddress(String emailAddress);
}
