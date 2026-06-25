/* (C) 2026 Rainier — internal use only. */
package com.rainier.capability.repository;

import com.rainier.capability.domain.UserCapability;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserCapabilityRepository extends JpaRepository<UserCapability, Long> {
  List<UserCapability> findByUserId(Long userId);

  Optional<UserCapability> findByUserIdAndCapabilityTagId(Long userId, Long capabilityTagId);
}
