/* (C) 2026 Rainier — internal use only. */
package com.rainier.capability.repository;

import com.rainier.capability.domain.CapabilityTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CapabilityTagRepository extends JpaRepository<CapabilityTag, Long> {
  boolean existsByName(String name);
}
